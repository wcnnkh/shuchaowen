package scw.db.sql.mysql;

import java.util.HashMap;
import java.util.Map;

import scw.database.SQL;
import scw.database.TableInfo;

public class SelectByIdSQL implements SQL {
	private static final long serialVersionUID = 1L;
	private static Map<String, String> sqlCache = new HashMap<String, String>();
	private String sql;
	private Object[] params;

	public SelectByIdSQL(TableInfo info, String tableName, Object[] ids) {
		StringBuilder sb = new StringBuilder();
		sb.append(info.getClassInfo().getName());
		sb.append(tableName);
		sb.append("&");
		sb.append(ids == null ? 0 : ids.length);

		String id = sb.toString();
		this.sql = sqlCache.get(id);
		if (sql == null) {
			synchronized (sqlCache) {
				sql = sqlCache.get(id);
				if (sql == null) {
					sql = getSql(info, tableName, ids);
					sqlCache.put(id, sql);
				}
			}
		}
		this.params = ids;
	}

	public String getSql() {
		return sql;
	}

	public Object[] getParams() {
		return params;
	}

	private static String getSql(TableInfo info, String tableName, Object[] ids) {
		StringBuilder sb = new StringBuilder();
		sb.append("select * from `").append(tableName).append("`");
		if (ids != null && ids.length > 0) {
			for (int i = 0; i < ids.length; i++) {
				if (sb.length() != 0) {
					sb.append(" and ");
				}
				sb.append(info.getPrimaryKeyColumns()[i].getSQLName(tableName));
				sb.append("=?");
			}

			if (ids.length == info.getPrimaryKeyColumns().length) {
				sb.append(" limit 0,1");
			}
		}
		return sb.toString();
	}
}
