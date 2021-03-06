package scw.orm.sql;

import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import scw.convert.TypeDescriptor;
import scw.lang.Nullable;
import scw.mapper.Field;
import scw.orm.EntityOperations;
import scw.orm.MaxValueFactory;
import scw.sql.Sql;
import scw.sql.SqlOperations;
import scw.sql.SqlStatementProcessor;
import scw.util.Pagination;
import scw.util.stream.Cursor;

public interface SqlTemplate extends EntityOperations, SqlOperations, MaxValueFactory, MapperProcessorFactory {
	SqlDialect getSqlDialect();

	default boolean createTable(Class<?> entityClass) {
		if (entityClass == null) {
			return false;
		}

		return createTable(null, entityClass);
	}

	boolean createTable(@Nullable String tableName, Class<?> entityClass);

	@Override
	default <T> boolean save(Class<? extends T> entityClass, T entity) {
		if (entityClass == null || entity == null) {
			return false;
		}

		return save(null, entityClass, entity);
	}

	<T> boolean save(@Nullable String tableName, Class<? extends T> entityClass, T entity);

	@Override
	default <T> boolean delete(Class<? extends T> entityClass, T entity) {
		if (entityClass == null || entity == null) {
			return false;
		}

		return delete(null, entityClass, entity);
	}

	<T> boolean delete(@Nullable String tableName, Class<? extends T> entityClass, T entity);

	@Override
	default boolean deleteById(Class<?> entityClass, Object... ids) {
		if (entityClass == null) {
			return false;
		}

		return deleteById(null, entityClass, ids);
	}

	boolean deleteById(@Nullable String tableName, Class<?> entityClass, Object... ids);

	@Override
	default <T> boolean update(Class<? extends T> entityClass, T entity) {
		if (entityClass == null || entity == null) {
			return false;
		}

		return update(null, entityClass, entity);
	}

	<T> boolean update(@Nullable String tableName, Class<? extends T> entityClass, T entity);

	@Override
	default <T> boolean saveOrUpdate(Class<? extends T> entityClass, T entity) {
		if (entityClass == null || entity == null) {
			return false;
		}

		return saveOrUpdate(null, entityClass, entity);
	}

	<T> boolean saveOrUpdate(@Nullable String tableName, Class<? extends T> entityClass, T entity);

	@Nullable
	@Override
	default <T> T getById(Class<? extends T> entityClass, Object... ids) {
		return getById(null, entityClass, ids);
	}

	@Nullable
	<T> T getById(@Nullable String tableName, Class<? extends T> entityClass, Object... ids);

	default <T> List<T> getByIdList(Class<? extends T> entityClass, Object... ids) {
		return getByIdList(null, entityClass, ids);
	}

	<T> List<T> getByIdList(@Nullable String tableName, Class<? extends T> entityClass, Object... ids);

	default <K, V> Map<K, V> getInIds(Class<? extends V> type, Collection<? extends K> inPrimaryKeys,
			Object... primaryKeys) {
		return getInIds(null, type, inPrimaryKeys, primaryKeys);
	}

	<K, V> Map<K, V> getInIds(String tableName, Class<? extends V> entityClass, Collection<? extends K> inPrimaryKeys,
			Object... primaryKeys);

	default <T> Cursor<T> query(Connection connection, TypeDescriptor resultType, Sql sql,
			SqlStatementProcessor statementProcessor) {
		return prepare(connection, sql, statementProcessor).query().stream(getMapperProcessor(resultType));
	}

	default <T> Cursor<T> query(Connection connection, TypeDescriptor resultType, Sql sql) {
		return prepare(connection, sql).query().stream(getMapperProcessor(resultType));
	}

	default <T> Cursor<T> query(TypeDescriptor resultType, Sql sql, SqlStatementProcessor statementProcessor) {
		return prepare(sql, statementProcessor).query().stream(getMapperProcessor(resultType));
	}

	default <T> Cursor<T> query(TypeDescriptor resultType, Sql sql) {
		return prepare(sql).query().stream(getMapperProcessor(resultType));
	}

	default <T> Cursor<T> query(Connection connection, Class<? extends T> resultType, Sql sql,
			SqlStatementProcessor statementProcessor) {
		return query(connection, TypeDescriptor.valueOf(resultType), sql, statementProcessor);
	}

	default <T> Cursor<T> query(Connection connection, Class<? extends T> resultType, Sql sql) {
		return query(connection, TypeDescriptor.valueOf(resultType), sql);
	}

	default <T> Cursor<T> query(Class<? extends T> resultType, Sql sql, SqlStatementProcessor statementProcessor) {
		return query(TypeDescriptor.valueOf(resultType), sql, statementProcessor);
	}

	default <T> Cursor<T> query(Class<? extends T> resultType, Sql sql) {
		return query(TypeDescriptor.valueOf(resultType), sql);
	}

	<T> Pagination<T> paginationQuery(TypeDescriptor resultType, Sql sql, long page, int limit);

	default <T> Pagination<T> paginationQuery(Class<? extends T> resultType, Sql sql, long page, int limit) {
		return paginationQuery(TypeDescriptor.valueOf(resultType), sql, page, limit);
	}

	/**
	 * 获取表的变更
	 * 
	 * @param tableClass
	 * @return
	 */
	default TableChanges getTableChanges(Class<?> tableClass) {
		return getTableChanges(tableClass, null);
	}

	/**
	 * 获取表的变更
	 * 
	 * @param tableClass
	 * @param tableName
	 * @return
	 */
	TableChanges getTableChanges(Class<?> tableClass, @Nullable String tableName);

	/**
	 * 获取对象指定字段的最大值
	 * 
	 * @param <T>
	 * @param type
	 * @param tableClass
	 * @param field
	 * @return
	 */
	@Nullable
	default <T> T getMaxValue(Class<? extends T> type, Class<?> tableClass, Field field) {
		return getMaxValue(type, tableClass, null, field);
	}

	/**
	 * 获取对象指定字段的最大值
	 * 
	 * @param type
	 * @param tableClass
	 * @param tableName
	 * @param field
	 * @return
	 */
	@Nullable
	<T> T getMaxValue(Class<? extends T> type, Class<?> tableClass, @Nullable String tableName, Field field);
}
