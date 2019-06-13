package com.nuix.superutilities.misc;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConfig.JournalMode;
import org.sqlite.SQLiteConfig.LockingMode;
import org.sqlite.SQLiteConfig.SynchronousMode;
import org.sqlite.SQLiteConfig.TransactionMode;

/***
 * Provides a wrapper around a SQLite database.  Intended to be extended by other classes which intended to leverage a
 * SQLite database is some way.
 * @author Jason Wells
 *
 */
public class SQLiteBacked implements Closeable {
	private File databaseFile = null;
	private Properties connectionProperties = new Properties();
	private Connection persistentConnection = null;
	
	public SQLiteBacked(File databaseFile) {
		this.databaseFile = databaseFile;
		SQLiteConfig config = new SQLiteConfig();
		config.setCacheSize(2000);
		config.setPageSize(4096 * 10);
		config.setJournalMode(JournalMode.WAL);
		config.setLockingMode(LockingMode.EXCLUSIVE);
		config.setTransactionMode(TransactionMode.EXCLUSIVE);
		config.setSynchronous(SynchronousMode.OFF);
		connectionProperties = config.toProperties();
	}
	
	private Connection getConnection() throws SQLException {
		if(persistentConnection == null){
			String connectionString = String.format("jdbc:sqlite:%s", databaseFile);
			persistentConnection = DriverManager.getConnection(connectionString, connectionProperties);
		}
		return persistentConnection;
	}
	
	/***
	 * Binds a list of objects to a prepared statement
	 * @param statement The prepared statement to bind data to
	 * @param data The data to bind
	 * @throws SQLException If the SQL bits throw an error
	 */
	private void bindData(PreparedStatement statement, List<Object> data) throws SQLException{
		if(data != null){
			for (int i = 0; i < data.size(); i++) {
				Object value = data.get(i);
				statement.setObject(i+1, value);
			}
		}
	}
	
	private void bindData(PreparedStatement statement, Object[] data) throws SQLException{
		if(data != null){
			for (int i = 0; i < data.length; i++) {
				Object value = data[i];
				statement.setObject(i+1, value);
			}
		}
	}
	
	/***
	 * Executes an update query against the SQLite database file
	 * @param sql The SQL to execute
	 * @param data Optional list of associated data, can be null
	 * @return Count of affected records
	 * @throws SQLException If the SQL bits throw an error
	 */
	public int executeUpdate(String sql, List<Object> data) throws SQLException{
		Connection conn = getConnection();
		try(PreparedStatement statement = conn.prepareStatement(sql)){
			if(data != null){ bindData(statement,data); }
			return statement.executeUpdate();
		}
	}
	
	public int executeUpdate(String sql, Object ...data) throws SQLException{
		Connection conn = getConnection();
		try(PreparedStatement statement = conn.prepareStatement(sql)){
			if(data != null){ bindData(statement,data); }
			return statement.executeUpdate();
		}
	}
	
	public int executeUpdate(String sql) throws SQLException{
		Connection conn = getConnection();
		try(PreparedStatement statement = conn.prepareStatement(sql)){
			return statement.executeUpdate();
		}
	}
	
	/***
	 * Executes an insert query against the SQLite database file
	 * @param sql The SQL to execute
	 * @param data Optional list of associated data, can be null
	 * @throws SQLException If the SQL bits throw an error
	 */
	public void executeInsert(String sql, List<Object> data) throws SQLException{
		Connection conn = getConnection();
		try(PreparedStatement statement = conn.prepareStatement(sql)){
			if(data != null){ bindData(statement,data); }
			statement.executeUpdate();
		}
	}
	
	public void executeInsert(String sql, Object ...data) throws SQLException{
		Connection conn = getConnection();
		try(PreparedStatement statement = conn.prepareStatement(sql)){
			if(data != null){ bindData(statement,data); }
			statement.executeUpdate();
		}
	}
	
	public Long executeLongScalar(String sql, Object ...data) throws SQLException{
		Connection conn = getConnection();
		try(PreparedStatement statement = conn.prepareStatement(sql)){
			if(data != null){ bindData(statement,data); }
			try(ResultSet resultSet = statement.executeQuery()){
				return resultSet.getLong(1);	
			}
		}
	}
	
	public Long executeLongScalar(String sql, List<Object> data) throws SQLException{
		Connection conn = getConnection();
		try(PreparedStatement statement = conn.prepareStatement(sql)){
			if(data != null){ bindData(statement,data); }
			try(ResultSet resultSet = statement.executeQuery()){
				return resultSet.getLong(1);	
			}
		}
	}
	
	public Long executeLongScalar(String sql) throws SQLException{
		Connection conn = getConnection();
		try(PreparedStatement statement = conn.prepareStatement(sql)){
			try(ResultSet resultSet = statement.executeQuery()){
				return resultSet.getLong(1);	
			}
		}
	}
	
	public String executeStringScalar(String sql, Object ...data) throws SQLException{
		Connection conn = getConnection();
		try(PreparedStatement statement = conn.prepareStatement(sql)){
			if(data != null){ bindData(statement,data); }
			try(ResultSet resultSet = statement.executeQuery()){
				return resultSet.getString(1);	
			}
		}
	}
	
	public String executeStringScalar(String sql, List<Object> data) throws SQLException{
		Connection conn = getConnection();
		try(PreparedStatement statement = conn.prepareStatement(sql)){
			if(data != null){ bindData(statement,data); }
			try(ResultSet resultSet = statement.executeQuery()){
				return resultSet.getString(1);	
			}
		}
	}
	
	public String executeStringScalar(String sql) throws SQLException{
		Connection conn = getConnection();
		try(PreparedStatement statement = conn.prepareStatement(sql)){
			try(ResultSet resultSet = statement.executeQuery()){
				return resultSet.getString(1);	
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T> T executeScalar(String sql, Object ...data) throws SQLException{
		Connection conn = getConnection();
		try(PreparedStatement statement = conn.prepareStatement(sql)){
			if(data != null){ bindData(statement,data); }
			try(ResultSet resultSet = statement.executeQuery()){
				return (T)resultSet.getObject(1);	
			}
		}
	}
	
	/***
	 * Executes a query which is expected to return row data, providing the result set to the provided callback.
	 * @param sql The SQL query to execute
	 * @param data Optional list of associated data, can be null
	 * @param resultConsumer Callback which will be provided the result set.  This is where you provide code to make use of the results.
	 * @throws SQLException If the SQL bits throw an error
	 */
	public void executeQuery(String sql, List<Object> data, Consumer<ResultSet> resultConsumer) throws SQLException{
		Connection conn = getConnection();
		try(PreparedStatement statement = conn.prepareStatement(sql)){
			if(data != null){ bindData(statement,data); }
			try(ResultSet resultSet = statement.executeQuery()){
				resultConsumer.accept(resultSet);	
			}
		}
	}
	
	@Override
	public void close() throws IOException {
		if(persistentConnection != null){
			try {
				persistentConnection.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
