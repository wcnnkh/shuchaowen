package scw.database;

import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import scw.common.exception.ShuChaoWenRuntimeException;

public abstract class AbstractResult implements Result {
	private static final long serialVersionUID = 1L;
	protected MetaData metaData;
	protected Object[] values;

	/**
	 * 序列化用
	 */
	protected AbstractResult() {
	}

	public AbstractResult(MetaData metaData, Object[] values) {
		this.metaData = metaData;
		this.values = values;
	}

	private Object wrapperTable(TableInfo tableInfo, String tableName)
			throws IllegalArgumentException, IllegalAccessException {
		Object o = tableInfo.newInstance();
		for (ColumnInfo column : tableInfo.getColumns()) {
			int index = metaData.getColumnIndex(column.getName(), tableName);
			if (index == -1 && !column.isNullAble()) {
				StringBuilder sb = new StringBuilder();
				sb.append(tableInfo.getClassInfo().getName());
				sb.append(" [");
				sb.append(column.getName());
				sb.append("] not found for DataSource");
				throw new ShuChaoWenRuntimeException(sb.toString());
			}

			Object v = values[index];
			if (v != null) {
				column.setValueToField(o, v);
			}
		}
		return o;
	}

	private Object wrapperSingle(TableInfo tableInfo) throws IllegalArgumentException, IllegalAccessException {
		Object o = tableInfo.newInstance();
		for (ColumnInfo column : tableInfo.getColumns()) {
			int index = metaData.getSingleIndex(column.getName());
			if (index == -1 && !column.isNullAble()) {
				StringBuilder sb = new StringBuilder();
				sb.append(tableInfo.getClassInfo().getName());
				sb.append(" [");
				sb.append(column.getName());
				sb.append("] not found for DataSource");
				throw new ShuChaoWenRuntimeException(sb.toString());
			}

			Object v = values[index];
			if (v != null) {
				column.setValueToField(o, v);
			}
		}
		return o;
	}

	public static String getTableName(TableInfo tableInfo, String tableName, Class<?> type,
			Map<Class<?>, String> tableMapping) {
		if (tableInfo.isTable()) {
			if (tableMapping == null) {
				return tableName;
			} else {
				String name = tableMapping.get(type);
				if (name == null) {
					return tableName;
				} else {
					return name;
				}
			}
		} else {
			if (tableMapping == null) {
				return "";
			} else {
				String name = tableMapping.get(type);
				if (name == null) {
					return tableName;
				} else {
					return "";
				}
			}
		}
	}

	private Object wrapper(TableInfo tableInfo, String tableName)
			throws IllegalArgumentException, IllegalAccessException {
		if (!metaData.isAsSingle()) {
			return wrapperSingle(tableInfo);
		} else {
			return wrapperTable(tableInfo, tableName);
		}
	}

	protected Object wrapper(TableInfo tableInfo, String tableName, Map<Class<?>, String> tableMapping)
			throws IllegalArgumentException, IllegalAccessException {
		Object o = wrapper(tableInfo, tableName);
		for (ColumnInfo column : tableInfo.getTableColumns()) {
			TableInfo tInfo = DataBaseUtils.getTableInfo(column.getType());
			String tName = getTableName(tInfo, tInfo.getName(), column.getType(), tableMapping);
			Object v = wrapper(tInfo, tName, tableMapping);
			if (v != null) {
				column.setValueToField(o, v);
			}
		}
		return o;
	}

	public AbstractResult(java.sql.ResultSet resultSet) throws SQLException {
		metaData = new MetaData(resultSet.getMetaData());
		values = new Object[metaData.getColumns().length];
		for (int i = 0; i < values.length; i++) {
			values[i] = resultSet.getObject(i + 1);
		}
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getValueMap(String tableName) {
		if (isEmpty()) {
			return Collections.EMPTY_MAP;
		}

		Map<String, Object> valueMap = new LinkedHashMap<String, Object>();
		MetaDataColumn[] columns = metaData.getColumns();
		for (int i = 0; i < columns.length; i++) {
			valueMap.put(columns[i].getName(), values[i]);
		}
		return valueMap;
	}

	public boolean isEmpty() {
		return values == null || values.length == 0 || metaData == null || metaData.isEmpty();
	}

	@SuppressWarnings("unchecked")
	public <T> T get(int index) {
		if (values == null) {
			return null;
		}

		return (T) values[index];
	}

	public int size() {
		return values == null ? 0 : values.length;
	}

	public Object[] getValues() {
		if (values == null) {
			return null;
		}

		Object[] dest = new Object[values.length];
		System.arraycopy(values, 0, dest, 0, dest.length);
		return dest;
	}
}