package scw.db;

import java.sql.SQLException;

import scw.beans.annotation.AutoImpl;
import scw.db.cache.CacheManager;
import scw.orm.sql.ORMOperations;
import scw.sql.Sql;
import scw.sql.SqlOperations;

@AutoImpl(className = {"scw.db.DefaultDB"})
public interface DB extends ORMOperations, SqlOperations {
	CacheManager getCacheManager();
	
	void createTable(Class<?> tableClass, boolean registerManager);

	void createTable(Class<?> tableClass, String tableName,
			boolean registerManager);

	void createTable(String packageName, boolean registerManager);

	void executeSqlByFile(String filePath, boolean lines) throws SQLException;

	void asyncExecute(AsyncExecute asyncExecute);

	void asyncSave(Object... objs);

	void asyncUpdate(Object... objs);

	void asyncDelete(Object... objs);

	void asyncExecute(Sql... sqls);
}