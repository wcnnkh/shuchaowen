package scw.db.database;

import scw.common.exception.NotFoundException;
import scw.common.utils.StringUtils;

public class MysqlDataBase extends AbstractDataBase {
	private String database;
	private String connectionUrl;
	private String driverClass;
	private String charsetName = "utf8";
	private String collate = "utf8_general_ci";

	public MysqlDataBase(String driverClass, String url, String username, String password) {
		super(username, password, DataBaseType.MySQL);
		this.driverClass = driverClass;

		int databaseBeginIndex = url.indexOf("//");
		if (databaseBeginIndex == -1) {
			throw new NotFoundException("无法解析数据库名称：" + url);
		}

		databaseBeginIndex = url.indexOf("/", databaseBeginIndex + 2);
		if (databaseBeginIndex == -1) {
			throw new NotFoundException("无法解析数据库名称：" + url);
		}

		databaseBeginIndex++;

		int databaseEndIndex = url.indexOf("?", databaseBeginIndex);
		if (databaseEndIndex == -1) {
			databaseEndIndex = url.indexOf("#", databaseBeginIndex);
		}

		if (databaseEndIndex == -1) {
			this.database = url.substring(databaseBeginIndex);
			this.connectionUrl = url.substring(0, databaseBeginIndex - 1);
		} else {
			this.database = url.substring(databaseBeginIndex, databaseEndIndex);
			this.connectionUrl = url.substring(0, databaseBeginIndex - 1) + url.substring(databaseEndIndex);
		}
	}

	public String getDataBase() {
		return database;
	}

	public String getConnectionURL() {
		return connectionUrl;
	}
	
	@Override
	public String getCreateSql(String database) {
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE DATABASE IF NOT EXISTS `").append(database).append("`");
		if (!StringUtils.isEmpty(charsetName)) {
			sb.append(" CHARACTER SET ").append(charsetName);
		}

		if (!StringUtils.isEmpty(collate)) {
			sb.append(" COLLATE ").append(collate);
		}

		return sb.toString();
	}

	public String getDriverClassName() {
		return StringUtils.isEmpty(driverClass) ? getDataBaseType().getDriverClassName() : driverClass;
	}
}