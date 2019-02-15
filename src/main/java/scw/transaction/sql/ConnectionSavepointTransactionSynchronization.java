package scw.transaction.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import scw.common.utils.CollectionUtils;
import scw.sql.ConnectionFactory;
import scw.sql.Sql;
import scw.sql.SqlUtils;
import scw.transaction.TransactionDefinition;
import scw.transaction.TransactionException;
import scw.transaction.synchronization.AbstractTransaction;
import scw.transaction.synchronization.TransactionSynchronization;

public class ConnectionSavepointTransactionSynchronization extends AbstractTransaction
		implements TransactionSynchronization {
	private ConnectionTransaction connectionTransaction;
	private LinkedHashMap<String, Sql> sqlMap;
	private Object savepoint;

	public ConnectionSavepointTransactionSynchronization(ConnectionFactory connectionFactory,
			TransactionDefinition transactionDefinition, boolean active) {
		super(active);
		setNewTransaction(true);
		this.connectionTransaction = new ConnectionTransaction(connectionFactory, transactionDefinition, active);
	}

	public void createTempSavepoint() {
		if (savepoint == null) {
			throw new TransactionException("不能重复创建savepoint");
		}
		this.savepoint = createSavepoint();
	}

	public void addSql(Sql... sqls) {
		if (sqlMap == null) {
			sqlMap = new LinkedHashMap<String, Sql>(4, 1);
		}

		for (Sql sql : sqls) {
			if (sql == null) {
				continue;
			}

			sqlMap.put(SqlUtils.getSqlId(sql), sql);
		}
	}

	public Connection getConnection() throws SQLException {
		return connectionTransaction.getConnection();
	}

	public void addSql(Collection<Sql> sqls) {
		if (CollectionUtils.isEmpty(sqls)) {
			return;
		}

		if (sqlMap == null) {
			sqlMap = new LinkedHashMap<String, Sql>(4, 1);
		}

		Iterator<Sql> iterator = sqls.iterator();
		while (iterator.hasNext()) {
			Sql sql = iterator.next();
			if (sql == null) {
				continue;
			}

			sqlMap.put(SqlUtils.getSqlId(sql), sql);
		}
	}

	public void begin() throws TransactionException {
		if (sqlMap != null) {
			Connection connection;
			try {
				connection = connectionTransaction.getConnection();
			} catch (SQLException e) {
				throw new TransactionException(e);
			}

			for (Entry<String, Sql> entry : sqlMap.entrySet()) {
				PreparedStatement preparedStatement = null;
				try {
					preparedStatement = SqlUtils.createPreparedStatement(connection, entry.getValue());
				} catch (SQLException e) {
					throw new TransactionException(e);
				} finally {
					if (preparedStatement != null) {
						try {
							preparedStatement.close();
						} catch (SQLException e) {
							throw new TransactionException(e);
						}
					}
				}
			}
		}
	}

	public void commit() throws TransactionException {
		if (isNewTransaction()) {
			connectionTransaction.commit();
		}
	}

	public void rollback() throws TransactionException {
		if (isNewTransaction()) {
			connectionTransaction.rollback();
		} else if (savepoint != null) {
			rollbackToSavepoint(savepoint);
		}
	}

	public void end() {
		if (isNewTransaction()) {
			connectionTransaction.end();
		} else if (savepoint != null) {
			releaseSavepoint(savepoint);
		}
	}

	public Object createSavepoint() throws TransactionException {
		return connectionTransaction.createSavepoint();
	}

	public void rollbackToSavepoint(Object savepoint) throws TransactionException {
		connectionTransaction.rollbackToSavepoint(savepoint);
	}

	public void releaseSavepoint(Object savepoint) throws TransactionException {
		releaseSavepoint(savepoint);
	}

}
