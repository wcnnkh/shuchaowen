package scw.sql.orm.support;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import scw.aop.ProxyUtils;
import scw.aop.support.FieldSetterListen;
import scw.core.IteratorCallback;
import scw.core.IteratorCallback.Row;
import scw.core.Pagination;
import scw.core.utils.ClassUtils;
import scw.core.utils.CollectionUtils;
import scw.core.utils.StringUtils;
import scw.io.ResourceUtils;
import scw.sql.ResultSetMapper;
import scw.sql.RowCallback;
import scw.sql.Sql;
import scw.sql.SqlTemplate;
import scw.sql.SqlUtils;
import scw.sql.orm.Column;
import scw.sql.orm.EntityOperations;
import scw.sql.orm.ORMException;
import scw.sql.orm.ResultMapping;
import scw.sql.orm.ResultSet;
import scw.sql.orm.TableChange;
import scw.sql.orm.TableName;
import scw.sql.orm.annotation.Table;
import scw.sql.orm.cache.CacheManager;
import scw.sql.orm.dialect.PaginationSql;
import scw.sql.orm.dialect.SqlDialect;
import scw.sql.orm.enums.OperationType;
import scw.sql.orm.enums.TableStructureResultField;
import scw.sql.orm.support.generation.GeneratorContext;
import scw.sql.orm.support.generation.GeneratorService;
import scw.sql.orm.support.generation.annotation.Generator;

public abstract class AbstractEntityOperations extends SqlTemplate implements EntityOperations {
	public abstract SqlDialect getSqlDialect();

	public abstract CacheManager getCacheManager();

	public abstract GeneratorService getGeneratorService();

	public final String getTableName(Class<?> clazz, Object obj, String tableName) {
		String tName = tableName;
		if (StringUtils.isEmpty(tName)) {
			if (obj instanceof TableName) {
				tName = ((TableName) obj).getTableName();
			}
		}
		return StringUtils.isEmpty(tName) ? getSqlDialect().getObjectRelationalMapping().getTableName(clazz) : tName;
	}

	public final String getTableName(Class<?> clazz, String tableName) {
		return (tableName == null || tableName.length() == 0)
				? getSqlDialect().getObjectRelationalMapping().getTableName(clazz) : tableName;
	}

	public final <T> T getById(Class<? extends T> type, Object... params) {
		return getById(null, type, params);
	}

	public final <T> List<T> getByIdList(Class<? extends T> type, Object... params) {
		return getByIdList(null, type, params);
	}

	public <T> T getById(String tableName, Class<? extends T> type, Object... params) {
		if (type == null) {
			throw new NullPointerException("type is null");
		}
		
		T t = getCacheManager().getById(type, params);
		if(t == null){
			if(getCacheManager().isSearchDB(type, params)){
				String tName = getTableName(type, tableName);
				ResultSet resultSet = select(getSqlDialect().toSelectByIdSql(type, tName, params));
				t = resultSet.getFirst().get(type, tName);
				if (t != null) {
					getCacheManager().save(t);
				}
			}
		}
		return t;
	}

	public <T> List<T> getByIdList(String tableName, Class<? extends T> type, Object... params) {
		if (type == null) {
			throw new NullPointerException("type is null");
		}

		String tName = getTableName(type, tableName);
		ResultSet resultSet = select(getSqlDialect().toSelectByIdSql(type, tName, params));
		return resultSet.getList(type, tName);
	}

	public Object getAutoIncrementLastId(Connection connection, String tableName) throws SQLException {
		return query(getSqlDialect().toLastInsertIdSql(tableName), connection, new ResultSetMapper<Object>() {

			public Object mapper(java.sql.ResultSet resultSet) throws SQLException {
				if (resultSet.next()) {
					return resultSet.getObject(1);
				}
				return null;
			}
		});
	}

	protected void generator(OperationType operationType, Class<?> clazz, Object bean, String tableName) {
		GeneratorContext generatorContext = new GeneratorContext(this, operationType, bean,
				getSqlDialect().getObjectRelationalMapping(), tableName);
		for (Column column : getSqlDialect().getObjectRelationalMapping().getPrimaryKeys(clazz)) {
			Generator generator = column.getAnnotatedElement().getAnnotation(Generator.class);
			if (generator == null) {
				return;
			}

			generatorContext.setColumn(column);
			getGeneratorService().process(generatorContext);
		}
	}

	protected boolean orm(OperationType operationType, Class<?> clazz, Object bean, String tableName) {
		String tName = getTableName(clazz, bean, tableName);
		Sql sql = SqlUtils.toSql(operationType, getSqlDialect(), clazz, bean, tName);
		Connection connection = null;
		try {
			connection = getUserConnection();
			return ormExecute(operationType, clazz, bean, tName, sql, connection);
		} catch (Throwable e) {
			throw new ORMException(SqlUtils.getSqlId(sql), e);
		} finally {
			close(connection);
		}
	}

	protected boolean ormExecute(final OperationType operationType, Class<?> clazz, final Object bean,
			final String tableName, final Sql sql, final Connection connection) throws Throwable {
		final int count = update(sql, connection);
		for (Column column : getSqlDialect().getObjectRelationalMapping().getColumns(clazz)) {
			if (column.isAutoIncrement()) {
				if (operationType == OperationType.SAVE || operationType == OperationType.SAVE_OR_UPDATE) {
					if (count == 0) {
						logger.warn("执行{{}}更新行数为0，无法获取到主键自增编号", SqlUtils.getSqlId(sql));
					} else if (count == 1) {
						try {
							column.set(bean, getAutoIncrementLastId(connection, tableName));
						} catch (SQLException e) {
							throw new ORMException(column.getName(), e);
						}
					}
				}
			}
		}
		return count != 0;
	}

	public boolean save(Object bean, String tableName) {
		Class<?> userClass = ProxyUtils.getProxyFactory().getUserClass(bean.getClass());
		if (orm(OperationType.SAVE, userClass, bean, getTableName(userClass, bean, tableName))) {
			getCacheManager().save(bean);
			return true;
		}
		return false;
	}

	public boolean update(Object bean, String tableName) {
		if (bean instanceof FieldSetterListen) {
			if (((FieldSetterListen) bean).get_field_setter_map() == null) {
				logger.warn("更新对象[{}]不存在数据变更", bean.getClass().getName());
				return false;
			}
		}

		Class<?> userClass = ProxyUtils.getProxyFactory().getUserClass(bean.getClass());
		if (orm(OperationType.UPDATE, userClass, bean, getTableName(userClass, bean, tableName))) {
			getCacheManager().update(bean);
			return true;
		}
		return false;
	}

	public boolean delete(Object bean, String tableName) {
		Class<?> userClass = ProxyUtils.getProxyFactory().getUserClass(bean.getClass());
		if (orm(OperationType.DELETE, userClass, bean, tableName)) {
			getCacheManager().delete(bean);
			return true;
		}
		return false;
	}

	public boolean saveOrUpdate(Object bean, String tableName) {
		Class<?> userClass = ProxyUtils.getProxyFactory().getUserClass(bean.getClass());
		if (orm(OperationType.SAVE_OR_UPDATE, userClass, bean, getTableName(userClass, bean, tableName))) {
			getCacheManager().saveOrUpdate(bean);
			return true;
		}
		return false;
	}

	public boolean deleteById(Class<?> type, Object... params) {
		return deleteById(null, type, params);
	}

	public boolean deleteById(String tableName, Class<?> type, Object... params) {
		Sql sql = getSqlDialect().toDeleteByIdSql(type, getTableName(type, tableName), params);
		if (update(sql) != 0) {
			getCacheManager().deleteById(type, params);
			return true;
		}
		return false;
	}

	public final boolean save(Object bean) {
		return save(bean, null);
	}

	public final boolean update(Object bean) {
		return update(bean, null);
	}
	
	public final boolean delete(Object bean) {
		return delete(bean, null);
	}

	public final boolean saveOrUpdate(Object bean) {
		return saveOrUpdate(bean, null);
	}
	
	protected <K, V> Map<K, V> getInIdListInternal(Class<? extends V> type, String tableName,
			Collection<? extends K> inPrimaryKeys, Object... primaryKeys){
		String tName = getTableName(type, tableName);
		Sql sql = getSqlDialect().toSelectInIdSql(type, tName, primaryKeys, inPrimaryKeys);
		ResultSet resultSet = select(sql);
		List<V> list = resultSet.getList(type, tName);
		if (list == null || list.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<String, K> keyMap = getSqlDialect().getObjectRelationalMapping().getInIdKeyMap(type, inPrimaryKeys,
				primaryKeys);
		Map<K, V> map = new LinkedHashMap<K, V>();
		for (V v : list) {
			String key = getSqlDialect().getObjectRelationalMapping().getObjectKey(type, v);
			map.put(keyMap.get(key), v);
		}
		return map;
	}

	@SuppressWarnings("unchecked")
	public final <K, V> Map<K, V> getInIdList(Class<? extends V> type, String tableName,
			Collection<? extends K> inPrimaryKeys, Object... primaryKeys) {
		if (CollectionUtils.isEmpty(inPrimaryKeys)) {
			return Collections.EMPTY_MAP;
		}

		if (type == null) {
			throw new NullPointerException("type is null");
		}

		if (primaryKeys != null
				&& primaryKeys.length > getSqlDialect().getObjectRelationalMapping().getPrimaryKeys(type).size() - 1) {
			throw new NullPointerException("primaryKeys length  greater than primary key lenght");
		}

		Map<K, V> map = getCacheManager().getInIdList(type, inPrimaryKeys, primaryKeys);
		if (CollectionUtils.isEmpty(map)) {
			Map<K, V> valueMap = getInIdListInternal(type, tableName, inPrimaryKeys,
					primaryKeys);
			if (!CollectionUtils.isEmpty(valueMap)) {
				for (Entry<K, V> entry : valueMap.entrySet()) {
					getCacheManager().save(entry.getValue());
				}
			}
			return valueMap;
		}

		if (map.size() == inPrimaryKeys.size()) {
			return map;
		}

		List<K> notFoundList = new ArrayList<K>(inPrimaryKeys.size());
		for (K k : inPrimaryKeys) {
			if (k == null) {
				continue;
			}

			if (map.containsKey(k)) {
				continue;
			}

			notFoundList.add(k);
		}

		if (!CollectionUtils.isEmpty(notFoundList)) {
			Map<K, V> dbMap = getInIdListInternal(type, tableName, notFoundList,
					primaryKeys);
			if (dbMap == null || dbMap.isEmpty()) {
				return map;
			}

			for (Entry<K, V> entry : dbMap.entrySet()) {
				getCacheManager().save(entry.getValue());
			}
			map.putAll(dbMap);
		}
		return map;
	}

	public final <K, V> Map<K, V> getInIdList(Class<? extends V> type, Collection<? extends K> inIdList, Object... params) {
		return getInIdList(type, null, inIdList, params);
	}

	public ResultSet select(Sql sql) {
		return query(sql, new ResultSetMapper<ResultSet>() {

			public ResultSet mapper(java.sql.ResultSet resultSet) throws SQLException {
				return new DefaultResultSet(resultSet);
			}
		});
	}

	public <T> List<T> select(Class<? extends T> type, Sql sql) {
		return select(sql).getList(type);
	}

	public <T> T selectOne(Class<? extends T> type, Sql sql) {
		return select(sql).getFirst().get(type);
	}

	@SuppressWarnings("unchecked")
	public <T> T selectOne(Class<? extends T> type, Sql sql, T defaultValue) {
		if (type.isPrimitive()) {
			// 如果是基本数据类型
			Object v = selectOne(ClassUtils.resolvePrimitiveIfNecessary(type), sql);
			return (T) (v == null ? defaultValue : v);
		} else {
			T v = selectOne(type, sql);
			return v == null ? defaultValue : v;
		}
	}

	public void createTable(Class<?> tableClass) {
		createTable(tableClass, null);
	}

	public void createTable(Class<?> tableClass, String tableName) {
		execute(getSqlDialect().toCreateTableSql(tableClass, getTableName(tableClass, tableName)));
	}

	public void createTable(String packageName) {
		Collection<Class<?>> list = ResourceUtils.getPackageScan().getClasses(packageName);
		for (Class<?> tableClass : list) {
			Table table = tableClass.getAnnotation(Table.class);
			if (table == null) {
				continue;
			}

			if (table.create()) {
				createTable(tableClass);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public <T> Pagination<List<T>> select(Class<? extends T> type, long page, int limit, Sql sql) {
		PaginationSql paginationSql = getSqlDialect().toPaginationSql(sql, page, limit);
		Long count = select(paginationSql.getCountSql()).getFirst().get(0);
		if (count == null) {
			count = 0L;
		}

		if (count == 0) {
			return new Pagination<List<T>>(0, limit, Collections.EMPTY_LIST);
		}

		return new Pagination<List<T>>(count, limit, select(type, paginationSql.getResultSql()));
	}

	public <T> Pagination<List<T>> select(Class<? extends T> type, int page, int limit, Sql sql) {
		return select(type, (long) page, limit, sql);
	}

	public Pagination<ResultSet> select(long page, int limit, Sql sql) {
		PaginationSql paginationSql = getSqlDialect().toPaginationSql(sql, page, limit);
		Long count = selectOne(Long.class, paginationSql.getCountSql());
		if (count == null) {
			count = 0L;
		}

		if (count == 0) {
			return new Pagination<ResultSet>(0, limit, ResultSet.EMPTY_RESULT_SET);
		}

		return new Pagination<ResultSet>(count, limit, select(paginationSql.getResultSql()));
	}

	public Pagination<ResultSet> select(int page, int limit, Sql sql) {
		return select((long) page, limit, sql);
	}

	/**
	 * 迭代所有的数据
	 * 
	 * @param tableClass
	 * @param iterator
	 */
	public <T> void iterator(final Class<? extends T> tableClass, final IteratorCallback<T> iterator) {
		Sql sql = getSqlDialect().toSelectByIdSql(tableClass,
				getSqlDialect().getObjectRelationalMapping().getTableName(tableClass), null);
		iterator(sql, new IteratorCallback<ResultMapping>() {

			public boolean iteratorCallback(ResultMapping data) {
				T t = data.get(tableClass);
				if (t == null) {
					return true;
				}

				return iterator.iteratorCallback(t);
			}
		});
	}

	public <T> void iterator(Sql sql, final Class<? extends T> type, final IteratorCallback<T> iterator) {
		iterator(sql, new IteratorCallback<ResultMapping>() {

			public boolean iteratorCallback(ResultMapping data) {
				T t = data.get(type);
				if (t == null) {
					return true;
				}

				return iterator.iteratorCallback(t);
			}
		});
	}

	public void iterator(Sql sql, final IteratorCallback<ResultMapping> iterator) {
		query(sql, new RowCallback() {

			public boolean processRow(java.sql.ResultSet rs, int rowNum) throws SQLException {
				return iterator.iteratorCallback(new DefaultResultMapping(rs));
			}
		});
	}

	public <T> void query(final Class<? extends T> tableClass, final IteratorCallback<Row<T>> iterator) {
		Sql sql = getSqlDialect().toSelectByIdSql(tableClass,
				getSqlDialect().getObjectRelationalMapping().getTableName(tableClass), null);
		query(sql, new IteratorCallback<Row<ResultMapping>>() {

			public boolean iteratorCallback(Row<ResultMapping> row) {
				T t = row.getValue().get(tableClass);
				if (t == null) {
					return true;
				}

				return iterator.iteratorCallback(new Row<T>(row.getIndex(), t));
			}
		});
	}

	public <T> void query(Sql sql, final Class<? extends T> type, final IteratorCallback<Row<T>> iterator) {
		query(sql, new IteratorCallback<Row<ResultMapping>>() {

			public boolean iteratorCallback(Row<ResultMapping> row) {
				T t = row.getValue().get(type);
				if (t == null) {
					return true;
				}

				return iterator.iteratorCallback(new Row<T>(row.getIndex(), t));
			}
		});
	}

	public void query(Sql sql, final IteratorCallback<Row<ResultMapping>> iterator) {
		query(sql, new RowCallback() {

			public boolean processRow(java.sql.ResultSet rs, int rowNum) throws SQLException {
				return iterator.iteratorCallback(new Row<ResultMapping>(rowNum, new DefaultResultMapping(rs)));
			}
		});
	}

	public <T> T getMaxValue(Class<? extends T> type, Class<?> tableClass, String tableName, String idField) {
		Sql sql = getSqlDialect().toMaxIdSql(tableClass, getTableName(tableClass, tableName), idField);
		return select(sql).getFirst().get(type, 0);
	}

	public <T> T getMaxValue(Class<? extends T> type, Class<?> tableClass, String idField) {
		return getMaxValue(type, tableClass, null, idField);
	}

	public TableChange getTableChange(Class<?> tableClass) {
		return getTableChange(tableClass, null);
	}

	public TableChange getTableChange(Class<?> tableClass, String tableName) {
		String tName = getTableName(tableClass, tableName);
		Sql sql = getSqlDialect().toTableStructureSql(tableClass, tName, Arrays.asList(TableStructureResultField.NAME));
		List<String[]> list = select(String[].class, sql);
		HashSet<String> hashSet = new HashSet<String>();
		List<String> deleteList = new LinkedList<String>();
		for (String[] names : list) {
			String name = names[0];
			hashSet.add(name);
			Column column = getSqlDialect().getObjectRelationalMapping().getColumn(tableClass, name);
			if (column == null) {// 在现在的表结构中不存在，应该删除
				deleteList.add(name);
			}
		}

		List<Column> addList = new LinkedList<Column>();
		for (Column column : getSqlDialect().getObjectRelationalMapping().getColumns(tableClass)) {
			if (!hashSet.contains(column.getName())) {// 在已有的数据库中不存在，应该添加
				addList.add(column);
			}
		}

		return new TableChange(deleteList, addList);
	}
}