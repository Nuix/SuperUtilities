package com.nuix.superutilities.annotations;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.roaringbitmap.RoaringBitmap;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConfig.JournalMode;
import org.sqlite.SQLiteConfig.LockingMode;
import org.sqlite.SQLiteConfig.SynchronousMode;
import org.sqlite.SQLiteConfig.TransactionMode;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.nuix.superutilities.misc.FormatUtility;

import nuix.Case;
import nuix.HistoryEvent;
import nuix.Item;
import nuix.ItemSet;
import nuix.ProductionSet;

/***
 * Class for recording annotation in a source case and replaying on a destination case.  GUID is used to record
 * items in source case and match items in destination case.
 * @author Jason Wells
 *
 */
public class AnnotationHistoryRepository implements Closeable{
	private static Logger logger = Logger.getLogger(AnnotationHistoryRepository.class);
	
	private static final String sqlCreateGuidRefTable =
			"CREATE TABLE IF NOT EXISTS GUIDRef (BitmapIndex INTEGER, GUID BLOB)";
	
	private static final String sqlCreateGuidRefUniqueIndex =
			"CREATE UNIQUE INDEX IF NOT EXISTS IDX_GUID_Unique ON GUIDRef (GUID)";
	
	private static final String sqlCreateTagEventTable =
			"CREATE TABLE IF NOT EXISTS TagEvent "+
			"(TimeStamp INTEGER, Tag Text, Added INTEGER, SerializedBitmap BLOB, ItemCount INTEGER)";
	
	private static final String sqlCreateCmEventTable =
			"CREATE TABLE IF NOT EXISTS CustomMetadataEvent (TimeStamp INTEGER, Added INTEGER, FieldName TEXT, "+
			"ValueType TEXT, ValueTimeZone TEXT, ValueInteger INTEGER, ValueFloat REAL, ValueText TEXT, ValueBinary BLOB, "+
			"SerializedBitmap BLOB, ItemCount INTEGER)";
	
	private static final String sqlCreateItemSetEventTable =
			"CREATE TABLE IF NOT EXISTS ItemSetEvent "+
			"(TimeStamp INTEGER, Added INTEGER, Settings TEXT, ItemSetName Text, BatchName TEXT, Description TEXT, "+
			"SerializedBitmap BLOB, ItemCount INTEGER)";
	
	private static final String sqlCreateExclusionEventTable =
			"CREATE TABLE IF NOT EXISTS ExclusionEvent "+
			"(TimeStamp INTEGER, Excluded INTEGER, ExclusionName TEXT, SerializedBitmap BLOB, ItemCount INTEGER)";
	
	private static final String sqlCreateCustodianEventTable = 
			"CREATE TABLE IF NOT EXISTS CustodianEvent "+
			"(TimeStamp INTEGER, Assigned INTEGER, Custodian TEXT, SerializedBitmap BLOB, ItemCount INTEGER)";
	
	private static final String sqlCreateProductionSetEventTable =
			"CREATE TABLE IF NOT EXISTS ProductionSetEvent "+
			"(TimeStamp INTEGER, Added INTEGER, Created INTEGER, ProductionSetSettings TEXT, "+
			"ProductionSetName TEXT, SerializedBitmap BLOB, ItemCount INTEGER)";
	
	private static final String sqlCreateTextInfoTable =
			"CREATE TABLE IF NOT EXISTS TextInfo (Name TEXT, ValueText TEXT)";
	
	private static final String sqlCreateIntegerInfoTable =
			"CREATE TABLE IF NOT EXISTS IntegerInfo (Name TEXT, ValueInteger TEXT)";
	
	
	private static final String sqlInsertTagEvent =
			"INSERT INTO TagEvent "+
			"(TimeStamp,Tag,Added,SerializedBitmap,ItemCount) VALUES (?,?,?,?,?)";
	
	private static final String sqlInsertCustomMetadataEvent =
			"INSERT INTO CustomMetadataEvent "+
			"(TimeStamp,Added,FieldName,ValueType,ValueTimeZone,ValueInteger,ValueFloat,ValueText,"+
			"ValueBinary,SerializedBitmap,ItemCount) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
	
	private static final String sqlInsertItemSetEvent =
			"INSERT INTO ItemSetEvent "+
			"(TimeStamp,Added,Settings,ItemSetName,BatchName,Description,SerializedBitmap,ItemCount) "+
			"VALUES (?,?,?,?,?,?,?,?)";
	
	private static final String sqlInsertExclusionEvent =
			"INSERT INTO ExclusionEvent "+
			"(TimeStamp,Excluded,ExclusionName,SerializedBitmap,ItemCount) VALUES (?,?,?,?,?)";
	
	private static final String sqlInsertCustodianEvent = 
			"INSERT INTO CustodianEvent (TimeStamp,Assigned,Custodian,SerializedBitmap,ItemCount) VALUES (?,?,?,?,?)";
	
	private static final String sqlInsertProductionSetEvent =
			"INSERT INTO ProductionSetEvent (TimeStamp,Added,Created,ProductionSetSettings,ProductionSetName,"+
			"SerializedBitmap,ItemCount) VALUES(?,?,?,?,?,?,?)";
	
	
	private static final String sqlSelectFromTagEvent =
			"SELECT TimeStamp,Tag,Added,SerializedBitmap,ItemCount FROM TagEvent WHERE TimeStamp >= ? ORDER BY TimeStamp ASC";
	
	private static final String sqlSelectFromCustomMetadataEvent =
			"SELECT TimeStamp,Added,FieldName,ValueType,ValueTimeZone,ValueInteger,ValueFloat,ValueText,ValueBinary,"+
			"SerializedBitmap,ItemCount FROM CustomMetadataEvent WHERE TimeStamp >= ? ORDER BY TimeStamp ASC";
	
	private static final String sqlSelectFromItemSetEvent =
			"SELECT TimeStamp,Added,Settings,ItemSetName,BatchName,Description,SerializedBitmap,ItemCount " +
			"FROM ItemSetEvent WHERE TimeStamp > ? ORDER BY TimeStamp ASC";
	
	private static final String sqlSelectFromExclusionEvent =
			"SELECT TimeStamp,Excluded,ExclusionName,SerializedBitmap,ItemCount FROM ExclusionEvent " +
			"WHERE TimeStamp > ? ORDER BY TimeStamp ASC";
	
	private static final String sqlSelectFromCustodianEvent = 
			"SELECT TimeStamp,Assigned,Custodian,SerializedBitmap,ItemCount FROM CustodianEvent " + 
			"WHERE TimeStamp > ? ORDER BY TimeStamp ASC";
	
	private static final String sqlSelectFromProductionSetEvent = 
			"SELECT TimeStamp,Added,Created,ProductionSetSettings,ProductionSetName,SerializedBitmap,ItemCount " +
			"FROM ProductionSetEvent WHERE TimeStamp > ? ORDER BY TimeStamp ASC";
	
	private File databaseFile = null;
	private Properties connectionProperties = new Properties();
	private HashBiMap<String,Long> guidIndexLookup = HashBiMap.create();
	private int guidRefInsertBatchSize = 250000;
	private boolean snapshotFirstSync = true;
	private Connection persistentConnection = null;
	
	// Sync operation can pre index all guids => indices meaning you pay for the cost of the work only once
	// up front, this bool hints that this has been done to other methods so they may skip work
	private boolean allItemsPreIndexed = false;
	
	private String[] eventTableNames = new String[]{
		"TagEvent",
		"CustomMetadataEvent",
		"ItemSetEvent",
		"ExclusionEvent",
		"CustodianEvent",
		"ProductionSetEvent",
	};
	
	/***
	 * Creates a new instance against the given and database file.  If the database file does not
	 * already exist it will be created and initialized.
	 * @param databaseFile The database file to record annotations to or playback annotations from
	 * @throws SQLException If the SQL bits throw an error
	 */
	public AnnotationHistoryRepository(File databaseFile) throws SQLException{
		this.databaseFile = databaseFile;
		
		SQLiteConfig config = new SQLiteConfig();
		config.setCacheSize(2000);
		config.setPageSize(4096 * 10);
		config.setJournalMode(JournalMode.WAL);
		config.setLockingMode(LockingMode.EXCLUSIVE);
		config.setTransactionMode(TransactionMode.EXCLUSIVE);
		config.setSynchronous(SynchronousMode.OFF);
		connectionProperties = config.toProperties();
		
		if(databaseFile.exists() == false){
			buildTables();
		}
		loadGuidLookupFromDatabase();
	}
	
	/***
	 * Creates a new instance against the given and database file.  If the database file does not
	 * already exist it will be created and initialized.
	 * @param databaseFile The database file to record annotations to or playback annotations from
	 * @throws SQLException If the SQL bits throw an error
	 */
	public AnnotationHistoryRepository(String databaseFile) throws SQLException {
		this(new File(databaseFile));
	}
	
	/***
	 * Creates the tables in a fresh DB file
	 * @throws SQLException If the SQL bits throw an error
	 */
	private void buildTables() throws SQLException{
		logger.info("Building tables in new DB...");
		
		executeUpdate(sqlCreateGuidRefTable);
		executeUpdate(sqlCreateGuidRefUniqueIndex);
		executeUpdate(sqlCreateTagEventTable);
		executeUpdate(sqlCreateCmEventTable);
		executeUpdate(sqlCreateItemSetEventTable);
		executeUpdate(sqlCreateExclusionEventTable);
		executeUpdate(sqlCreateCustodianEventTable);
		executeUpdate(sqlCreateProductionSetEventTable);
		executeUpdate(sqlCreateTextInfoTable);
		executeUpdate(sqlCreateIntegerInfoTable);
		
		setIntegerInfo("SyncPointTimeStamp", 0L);
	}
	
	private boolean integerInfoExists(String name) throws SQLException{
		return executeLongScalar("SELECT COUNT(*) FROM IntegerInfo WHERE Name = ?",name) > 0;
	}
	
	private boolean textInfoExists(String name) throws SQLException{
		return executeLongScalar("SELECT COUNT(*) FROM TextInfo WHERE Name = ?",name) > 0;
	}
	
	public Long getIntegerInfo(String name) throws SQLException {
		return executeLongScalar("SELECT ValueInteger FROM IntegerInfo WHERE Name = ?", name);
	}
	
	public void setIntegerInfo(String name,Long value) throws SQLException {
		logger.info(String.format("Recording IntegerInfo: %s => %s",name,value));
		if(integerInfoExists(name)){
			executeUpdate("UPDATE OR IGNORE IntegerInfo SET ValueInteger = ? WHERE Name = ?",value,name);	
		} else {
			executeInsert("INSERT INTO IntegerInfo (Name,ValueInteger) VALUES (?,?)",name,value);
		}
	}
	
	public String getTextInfo(String name) throws SQLException {
		return executeStringScalar("SELECT ValueText FROM TextInfo WHERE Name = ?", name);
	}
	
	public void setTextInfo(String name,String value) throws SQLException {
		logger.info(String.format("Recording TextInfo: %s => %s",name,value));
		if(textInfoExists(name)){
			executeUpdate("UPDATE OR IGNORE TextInfo SET ValueText = ? WHERE Name = ?",value,name);	
		} else {
			executeInsert("INSERT INTO TextInfo (Name,ValueText) VALUES (?,?)",name,value);
		}
	}
	
	public void setSyncPointToNow() throws SQLException{
		setIntegerInfo("SyncPointTimeStamp", DateTime.now().getMillis());
	}
	
	public void setSyncPointFromDestinationCaseLastEvent(Case nuixCase) throws Exception{
		Map<String,Object> retrievalSettings = new HashMap<String,Object>();
		retrievalSettings.put("type", "annotation");
		retrievalSettings.put("order", "start_date_descending");
		long lastAnnotationEvent = 0;
		for(HistoryEvent event : nuixCase.getHistory(retrievalSettings)){
			lastAnnotationEvent = event.getStartDate().getMillis();
		}
		setIntegerInfo("SyncPointTimeStamp", lastAnnotationEvent);
	}
	
	public void setSyncPoint(DateTime dateTime) throws Exception{
		setIntegerInfo("SyncPointTimeStamp", dateTime.getMillis());
	}
	
	/***
	 * Items associated to a given event are stored in the database as a series of indices
	 * stored in a bitmap, serialized to a byte array.  We must maintain a xref between each
	 * GUID and the index it may hold in any given bitmap.  This is persisted to the database as
	 * constructed and kept in memory, but of course the in memory copy is transient, so on database
	 * open we need to slurp that back into memory from the database
	 * @throws SQLException If the SQL bits throw an error
	 */
	private void loadGuidLookupFromDatabase() throws SQLException{
		long startTime = System.currentTimeMillis();
		logger.info("Loading GUID Xref from database...");
		executeQuery("SELECT BitmapIndex,GUID FROM GUIDRef",null,rs -> {
			try {
				while(rs.next()){
					byte[] guid = rs.getBytes(2);
					long index = rs.getLong(1);
					guidIndexLookup.put(FormatUtility.bytesToHex(guid), index);
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		long finishTime = System.currentTimeMillis();
		logger.info(String.format("Loaded %s GUID Xref from database in %s ms",
				guidIndexLookup.size(), finishTime - startTime));
	}
	
	/***
	 * Given a list of GUID string, makes sure the in memory index and database has copy of the bitmap index
	 * to GUID string xref.
	 * @param guids GUIDs to make sure are present in the lookups
	 * @throws SQLException If the SQL bits throw an error
	 */
	private void indexGuids(Collection<String> guids) throws SQLException{
		class GuidIndexPair{
			long index = 0;
			byte[] guid = null; 
		}
		
		List<GuidIndexPair> toInsert = new ArrayList<GuidIndexPair>();
		List<Object> data = new ArrayList<Object>();
		
		for(String guid : guids){
			guid = guid.replace("-", "").toLowerCase();
			if(!guidIndexLookup.containsKey(guid)){
				long index = (long)guidIndexLookup.size()+1;
				guidIndexLookup.put(guid, index);
				data.clear();
				data.add(index);
				data.add(guid);
				GuidIndexPair gip = new GuidIndexPair();
				gip.index = index;
				gip.guid = FormatUtility.hexToBytes(guid);
				toInsert.add(gip);
				
			}
		}
		
		// Now we make sure DB is in sync
		int queued = 0;
		Connection conn = getConnection();
		conn.setAutoCommit(false);
		String sql = "INSERT OR IGNORE INTO GUIDRef (BitmapIndex,GUID) VALUES (?,?)";
		try(PreparedStatement statement = conn.prepareStatement(sql)){
			for(GuidIndexPair gip : toInsert){
				data.clear();
				data.add(gip.index);
				data.add(gip.guid);
				if(data != null){ bindData(statement,data); }
				statement.executeUpdate();
				queued++;
				
				if(queued >= guidRefInsertBatchSize){
					conn.commit();
					queued = 0;
				}
			}
		}
		
		if(queued > 0){
			conn.commit();	
		}
		
		conn.setAutoCommit(true);
	}
	
	/***
	 * Convenience method similar to {@link #indexGuids(Collection)} but accepts a collection of
	 * items and converts that to a list of GUIDs for you before making call to {@link #indexGuids(Collection)}.
	 * @param items Items for which to make sure have their GUID present in the lookups
	 * @throws SQLException If the SQL bits throw an error
	 */
	private void indexItemGuids(Collection<Item> items) throws SQLException{
		indexGuids(items.parallelStream().map(i -> i.getGuid()).collect(Collectors.toList()));
	}
	
	/***
	 * Given a collection of items, resolves those items to a collection of GUIDs and those GUIDs
	 * to a collection of bitmap indices
	 * @param items The items to obtain bitmap indices for
	 * @return Returns a list of bitmap indices for the provided items
	 * @throws SQLException If the SQL bits throw an error
	 */
	private List<Long> itemsToIndices(Collection<Item> items) throws SQLException{
		if(!allItemsPreIndexed){
			indexItemGuids(items);
		}
		
		List<Long> indices = new ArrayList<Long>();
		for(Item item : items){
			Long bitmapIndex = guidIndexLookup.get(item.getGuid().replace("-", "").toLowerCase());
			if(bitmapIndex == null){
				logger.error("No index for item with GUID "+item.getGuid());
			}
			indices.add(bitmapIndex);
		}
		return indices;
	}
	
	/***
	 * Does all the work of taking a given collection of items, resolving them to their GUIDs, resolving the
	 * GUIDs to bitmap indices, building a RoaringBitmap and converting that to a byte array suitable for
	 * storing in the SQLite database.
	 * @param items The items to serialize
	 * @return byte array serialization representing collection of items
	 * @throws IOException If there is an error while serializing bitamp to byte array
	 * @throws SQLException If the SQL bits throw an error
	 */
	byte[] dehydrateItemCollection(Collection<Item> items) throws IOException, SQLException{
		RoaringBitmap bitmap = new RoaringBitmap();
		List<Long> indices = itemsToIndices(items);
		for(Long index : indices){
			bitmap.add(index,index+1);
		}
		bitmap.runOptimize();
		byte[] result = new byte[bitmap.serializedSizeInBytes()];
		bitmap.serialize(new java.io.DataOutputStream(new java.io.OutputStream() { 
            int c = 0; 
            
            @Override 
            public void close() {} 

            @Override 
            public void flush() {} 

            @Override 
            public void write(int b) { result[c++] = (byte)b; } 

            @Override 
            public void write(byte[] b) { write(b,0,b.length); } 

            @Override 
            public void write(byte[] b, int off, int l) { 
                System.arraycopy(b, off, result, c, l); 
                c += l; 
            } 
        }));
		return result;
	}
	
	/***
	 * Does all the work of taking a byte array, deserializing it into a RoaringBitmap, extracting a list of bitmap indices
	 * from that bitmap, resolving the indices to GUIDs and running search batches in the Nuix case to obtain a collection
	 * of the items the bitmap represents.
	 * @param bitmapBytes byte array read from database representing bitmap
	 * @return Collection of items found in the currently associated Nuix case based on bitmap deserialized from provided byte array
	 * @throws IOException If the deserialization from byte array has an error
	 */
	Collection<Item> rehydrateItemCollection(Case nuixCase, byte[] bitmapBytes) throws IOException{
		RoaringBitmap bitmap = new RoaringBitmap();
		try(ByteArrayInputStream inputStream = new ByteArrayInputStream(bitmapBytes)){
			try(DataInputStream dataStream = new DataInputStream(inputStream)){
				bitmap.deserialize(dataStream);
			}
		}

		Set<String> guids = new HashSet<String>();
		BiMap<Long,String> indexLookup = guidIndexLookup.inverse();
		for(long index : bitmap){
			//logger.info("Index: "+index);
			guids.add(indexLookup.get(index));
		}
		
		Set<Item> items = new HashSet<Item>();
		List<String> chunk = new ArrayList<String>();
		for(String guid : guids){
			if(chunk.size() >= 1000){
				String guidCriteria = String.join(" OR ", chunk);
				String query = "guid:("+guidCriteria+")";
				Set<Item> chunkItems = nuixCase.searchUnsorted(query);
				items.addAll(chunkItems);
				chunk.clear();
			}
			
			chunk.add(guid);
		}
		
		if(chunk.size() > 0){
			String guidCriteria = String.join(" OR ", chunk);
			String query = "guid:("+guidCriteria+")";
			Set<Item> chunkItems = nuixCase.searchUnsorted(query);
			items.addAll(chunkItems);
			chunk.clear();
		}
		
		return items;
	}
	
	private Connection getConnection() throws SQLException {
		if(persistentConnection == null){
			logger.info("Building persistent connection...");
			String connectionString = String.format("jdbc:sqlite:%s", databaseFile);
			logger.info("└─ Connection String: "+connectionString);
			logger.info("└─ Properties:");
			for(Map.Entry<Object, Object> prop : connectionProperties.entrySet()){
				logger.info(String.format(" └─ %s: %s",prop.getKey(),prop.getValue()));
			}
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
	
	/***
	 * Returns the total number of event entries in the database file
	 * @return Total event row count
	 * @throws SQLException If the SQL bits throw an error
	 */
	public long getTotalEventCount() throws SQLException{
		long sum = 0;
		
		long tagEventCount = executeLongScalar("SELECT COUNT(*) FROM TagEvent");
		sum += tagEventCount;
		
		return sum;
	}
	
	public DateTime calculateLastDbEventStart() throws SQLException{
		long dbLastTimeStamp = 0;
		long lastDbTagEvent = executeLongScalar("SELECT MAX(TimeStamp) FROM TagEvent");
		long lastCustomMetadataEvent = executeLongScalar("SELECT MAX(TimeStamp) FROM CustomMetadataEvent");
		long lastItemSetEvent = executeLongScalar("SELECT MAX(TimeStamp) FROM ItemSetEvent");
		long lastExclusionEvent = executeLongScalar("SELECT MAX(TimeStamp) FROM ExclusionEvent");
		long lastCustodianEvent = executeLongScalar("SELECT MAX(TimeStamp) FROM CustodianEvent");
		long lastProductionSetEvent = executeLongScalar("SELECT MAX(TimeStamp) FROM ProductionSetEvent");
		long syncPoint = getIntegerInfo("SyncPointTimeStamp");
		
		if(lastDbTagEvent > dbLastTimeStamp){ dbLastTimeStamp = lastDbTagEvent; }
		if(lastCustomMetadataEvent > dbLastTimeStamp){ dbLastTimeStamp = lastCustomMetadataEvent; }
		if(lastItemSetEvent > dbLastTimeStamp){ dbLastTimeStamp = lastItemSetEvent; }
		if(lastExclusionEvent > dbLastTimeStamp){ dbLastTimeStamp = lastExclusionEvent; }
		if(lastCustodianEvent > dbLastTimeStamp){ dbLastTimeStamp = lastCustodianEvent; }
		if(lastProductionSetEvent > dbLastTimeStamp){ dbLastTimeStamp = lastProductionSetEvent; }
		if(syncPoint > dbLastTimeStamp){ dbLastTimeStamp = syncPoint; }
		
		DateTime lastDbEventStart = new DateTime(dbLastTimeStamp);
		return lastDbEventStart;
	}
	
	/***
	 * Records annotation event data to the database file.
	 * 
	 * If there are 0 events currently recorded and snap shot on first sync
	 * is enabled (see {@link #setSnapshotFirstSync(boolean)}, true is the default) then this will attempt to make a compacted
	 * representation of the state of the annotations in the case.  Otherwise this method will iterate every single history event
	 * in the case and record it in the database.
	 * 
	 * If the database already contains event entries, instead this method will determine the latest event started date and then
	 * query the Nuix case history for all further events started after that point, effectively obtaining and recording all new
	 * event history entries not yet recorded in this database.
	 * 
	 * @param nuixCase The case to record history events from
	 * @param settings The settings which determine how the sync is performed
	 * @throws IOException If there is an error: creating snapshot, getting case history or converting items into bitamp byte array for DB
	 * @throws SQLException If the SQL bits throw an error
	 */
	public void syncHistory(Case nuixCase, AnnotationSyncSettings settings) throws IOException, SQLException{
		if(settings == null){
			settings = new AnnotationSyncSettings();
		}
		
		logger.info(String.format("Begging sync of case: %s", nuixCase.getLocation().getPath()));
		logger.info("Settings: ");
		logger.info(settings.buildSettingsSummary());
		
		boolean indicesDropped = false;
		boolean snapshotTaken = false;
		
		allItemsPreIndexed = false;
		logger.info("Pre-indexing all item GUIDs...");
		indexItemGuids(nuixCase.search(""));
		allItemsPreIndexed = true;
		
		long totalHistoryRecordMillis = 0;
		long totalHistoryEventsRecorded = 0;
		
		DateTime lastDbEventStart = calculateLastDbEventStart();
		
		if(getTotalEventCount() < 1){
			// Record info about the case we are syncing from
			setTextInfo("SourceCaseName", nuixCase.getName());
			setTextInfo("SourceCaseLocation", nuixCase.getLocation().getAbsolutePath());
		}
		
		if(snapshotFirstSync && getTotalEventCount() < 1){
			createInitialStateSnapshot(nuixCase,settings);
			snapshotTaken = true;
			settings.setSyncTagEvents(false);
			settings.setSyncCustodianEvents(false);
		} 
		
		if(snapshotTaken){
			logger.info(String.format("Fetching remaining events after %s",lastDbEventStart));
		} else {
			logger.info(String.format("Fetching events after %s",lastDbEventStart));	
		}
		
		Map<String,Object> retrievalSettings = new HashMap<String,Object>();
		retrievalSettings.put("type", "annotation");
		retrievalSettings.put("startDateAfter", lastDbEventStart);
		
		long lastProgressUpdate = System.currentTimeMillis();
		long eventIndex = 0;
		
		for(HistoryEvent event : nuixCase.getHistory(retrievalSettings)){
			eventIndex++;
			
			if(System.currentTimeMillis() - lastProgressUpdate >= 10 * 1000){
				logger.info(String.format("Processing event %s", eventIndex));
				lastProgressUpdate = System.currentTimeMillis();
			}
			
			if(!indicesDropped){
				// More efficient to rebuild indices from scratch than it is
				// to update them as data is inserted
				for (int i = 0; i < eventTableNames.length; i++) {
					String tableName = eventTableNames[i];
					logger.info(String.format("Dropping %s TimeStamp index...",tableName));
					String indexSql = String.format("DROP INDEX IF EXISTS IDX_TimeStamp_%s", tableName);
					executeUpdate(indexSql);
				}
				
				indicesDropped = true;
			}

			Map<String,Object> details = event.getDetails();
			
			long historyRecordStart = System.currentTimeMillis();
			long historyRecordFinish = 0;
			
			// Tag Add/Remove events
			if(settings.getSyncTagEvents() && details.get("tag") != null){
				// Appears we have a tagging event
				recordTagEvent(event, details);
				historyRecordFinish = System.currentTimeMillis();
				totalHistoryEventsRecorded++;
				totalHistoryRecordMillis += historyRecordFinish - historyRecordStart;
			} else if(settings.getSyncCustomMetadataEvents() && details.get("fieldName") != null){
				//Appears we have add remove custom metadata events
				recordCustomMetadataEvent(nuixCase, event, details);
				historyRecordFinish = System.currentTimeMillis();
				totalHistoryEventsRecorded++;
				totalHistoryRecordMillis += historyRecordFinish - historyRecordStart;
			} else if(settings.getSyncItemSetEvents() && details.get("item-set") != null  && 
					(details.containsKey("items-assigned-count") || details.containsKey("items-unassigned-count"))){
				// Appears we have an item set event
				recordItemSetEvent(nuixCase, event, details);
				historyRecordFinish = System.currentTimeMillis();
				totalHistoryEventsRecorded++;
				totalHistoryRecordMillis += historyRecordFinish - historyRecordStart;
			} else if(settings.getSyncExclusionEvents() && details.get("excluded") != null){
				//Appears we have exclusion/inclusion event
				recordExclusionEvent(nuixCase, event, details);
				historyRecordFinish = System.currentTimeMillis();
				totalHistoryEventsRecorded++;
				totalHistoryRecordMillis += historyRecordFinish - historyRecordStart;
			} else if(settings.getSyncCustodianEvents() && details.get("assigned") != null){
				recordCustodianEvent(nuixCase, event, details);
				historyRecordFinish = System.currentTimeMillis();
				totalHistoryEventsRecorded++;
				totalHistoryRecordMillis += historyRecordFinish - historyRecordStart;
			} else if(settings.getSyncProductionSetEvents() && details.get("productionSet") != null){
				recordProductionSetEvent(nuixCase, event, details);
				historyRecordFinish = System.currentTimeMillis();
				totalHistoryEventsRecorded++;
				totalHistoryRecordMillis += historyRecordFinish - historyRecordStart;
			}
		}
		
		logger.info(String.format("History Events Recorded: %s", totalHistoryEventsRecorded));
		logger.info(String.format("Total History Event Record Time: %s ms", totalHistoryRecordMillis));
		if(totalHistoryEventsRecorded > 0){
			logger.info(String.format("Average Event Record Time: %s ms", totalHistoryRecordMillis / totalHistoryEventsRecorded));
		} else {
			logger.info(String.format("Average Event Record Time: %s ms", 0));	
		}
		
		if(indicesDropped){
			// Now that were done inserting, lets get those indexes back up
			for (int i = 0; i < eventTableNames.length; i++) {
				String tableName = eventTableNames[i];
				logger.info(String.format("Building %s TimeStamp index...",tableName));
				String indexSql = String.format("CREATE INDEX IF NOT EXISTS IDX_TimeStamp_%s ON %s (TimeStamp)", tableName, tableName);
				executeUpdate(indexSql);
			}
		}
	}
	
	private void recordProductionSetEvent(Case nuixCase, HistoryEvent event, Map<String, Object> details) throws IOException, SQLException {
		Set<Item> items = event.getAffectedItems();
		byte[] serializedItemBitmap = dehydrateItemCollection(items);
		
		boolean added = false;
		boolean created = false;
		
		if(details.containsKey("added")){
			added = (boolean) details.get("added");	
		} else {
			created = true;	
		}
		
		
		String productionSetSettings = null;
		String productionSetName = ((com.nuix.storage.stores.productionsets.ProductionSet) details.get("productionSet")).getName();
		
		// If this was a production set creation event, we need to record all the settings used to create the production
		// set so it can be recreated later on
		if(created){
			Map<String,Object> settings = new HashMap<String,Object>();
			ProductionSet prod = nuixCase.findProductionSetByName(productionSetName);
			if(prod == null){
				logger.error("Attempted to record ProductionSet creation event, but it appears a production set with that name no"+
						"longer exists, making it impossible to ascertain settings used to create the production set.");
				//TODO: Define rules regarding what should happen in this state
			} else {
				settings.put("numberingOptions", prod.getNumberingOptions());
				settings.put("stampingOptions", prod.getStampingOptions());
				settings.put("imagingOptions", prod.getImagingOptions());
				settings.put("markupSets", prod.getMarkupSets());
				settings.put("applyHighlights", prod.isApplyRedactions());
				settings.put("applyRedactions", prod.isApplyHighlights());
				
				GsonBuilder gsonBuilder = new GsonBuilder();
				gsonBuilder.serializeNulls();
				gsonBuilder.setPrettyPrinting();
				Gson gson = gsonBuilder.create();
				
				productionSetSettings = gson.toJson(settings);
			}
		}
		
		List<Object> data = new ArrayList<Object>();
		data.add(event.getStartDate().getMillis()); //TimeStamp
		data.add(added);
		data.add(created);
		data.add(productionSetSettings);
		data.add(productionSetName);
		data.add(serializedItemBitmap);
		data.add(items.size());
		
		if(created){
			logger.info(String.format("Recording creation of production set '%s' with settings:\n%s",
					productionSetName,productionSetSettings));
		} else {
			if(added){
				logger.info(String.format("Recording %s items being added to production set '%s'",
						items.size(),productionSetName));	
			} else {
				logger.info(String.format("Recording %s items being removed from production set '%s'",
						items.size(),productionSetName));
			}
		}
		
		executeInsert(sqlInsertProductionSetEvent,data);
	}

	private void recordCustodianEvent(Case nuixCase, HistoryEvent event, Map<String, Object> details) throws IOException, SQLException{
		Set<Item> items = event.getAffectedItems();
		byte[] serializedItemBitmap = dehydrateItemCollection(items);
		
		boolean assigned = (boolean) details.get("assigned");
		String custodian = (String) details.get("custodian");
		
		List<Object> data = new ArrayList<Object>();
		data.add(event.getStartDate().getMillis()); //TimeStamp
		data.add(assigned);
		data.add(custodian);
		data.add(serializedItemBitmap);
		data.add(items.size());
		
		if(assigned){
			logger.info(String.format("Recording assign custodian '%s' to %s items",
					custodian,items.size()));	
		} else {
			logger.info(String.format("Recording un-assign custodian from %s items",
					items.size()));
		}
		
		executeInsert(sqlInsertCustodianEvent,data);
	}
	
	private void recordExclusionEvent(Case nuixCase, HistoryEvent event, Map<String, Object> details) throws IOException, SQLException {
		Set<Item> items = event.getAffectedItems();
		byte[] serializedItemBitmap = dehydrateItemCollection(items);
		
		boolean excluded = (boolean) details.get("excluded");
		String exclusionName = (String) details.get("exclusion");
		
		List<Object> data = new ArrayList<Object>();
		data.add(event.getStartDate().getMillis()); //TimeStamp
		data.add(excluded);
		data.add(exclusionName);
		data.add(serializedItemBitmap);
		data.add(items.size());
		
		if(excluded){
			logger.info(String.format("Recording exclude items as '%s' event on %s items",
					exclusionName,items.size()));
		} else {
			logger.info(String.format("Recording include items event on %s items",
					items.size()));
		}
		
		executeInsert(sqlInsertExclusionEvent,data);
	}

	private void recordItemSetEvent(Case nuixCase, HistoryEvent event, Map<String, Object> details)
			throws IOException, SQLException {
		Set<Item> items = event.getAffectedItems();
		byte[] serializedItemBitmap = dehydrateItemCollection(items);
		
		String itemSetName = (String)details.get("item-set");
		String batchName = (String)details.get("batch");
		boolean added = details.containsKey("items-assigned-count");
		
		//Capture additional info from item set itself
		ItemSet itemSet = nuixCase.findItemSetByName(itemSetName);
		@SuppressWarnings("unchecked")
		Map<String,Object> itemSetSettings = (Map<String, Object>) itemSet.getSettings();
		Map<String,Object> settingsToSerialize = new HashMap<String,Object>();
		settingsToSerialize.put("custodianRanking", itemSetSettings.get("custodianRanking"));
		settingsToSerialize.put("deduplication", itemSetSettings.get("deduplication"));
		//settingsToSerialize.put("inclusionStrategy", itemSetSettings.get("inclusionStrategy").toString());
		settingsToSerialize.put("deduplicateBy", itemSetSettings.get("deduplicateBy"));
		String description = itemSet.getDescription();
		
		Gson gson = null;
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.serializeNulls();
		gson = gsonBuilder.create();
		String itemSetSettingsJson = gson.toJson(settingsToSerialize);
		
		List<Object> data = new ArrayList<Object>();
		data.add(event.getStartDate().getMillis()); //TimeStamp
		data.add(added);
		if(added){
			data.add(itemSetSettingsJson);
		} else {
			data.add("{}");
		}
		data.add(itemSetName);
		data.add(batchName);
		data.add(description);
		data.add(serializedItemBitmap);
		data.add(items.size());
		
		if(added){
			logger.info(String.format("Recording add %s items to item set '%s', batch '%s'",
					items.size(),itemSetName,batchName));
		} else {
			logger.info(String.format("Recording remove %s items from item set '%s'",
					items.size(),itemSetName));
		}
		
		executeInsert(sqlInsertItemSetEvent,data);
	}

	private void recordCustomMetadataEvent(Case nuixCase, HistoryEvent event, Map<String, Object> details)
			throws IOException, SQLException {
		Set<Item> items = event.getAffectedItems();
		byte[] serializedItemBitmap = dehydrateItemCollection(items);
		String fieldName = (String)details.get("fieldName");
		
		List<Object> data = new ArrayList<Object>();
		data.add(event.getStartDate().getMillis()); //TimeStamp
		
		if(details.get("type") != null){
			//Add event
			data.add(true); //Added
			data.add(fieldName); //FieldName
			String valueType = (String)details.get("type"); 
			data.add(valueType); //ValueType
			
			Object value = details.get("value");
			if(valueType.contentEquals("date-time")){
				DateTime valueDateTime = (DateTime)value;
				data.add(valueDateTime.getZone().getID()); //ValueTimeZone
				data.add(valueDateTime.getMillis()); //ValueInteger
				data.add(null); //ValueFloat
				data.add(null); //ValueText
				data.add(null); //ValueBinary
			} else if(valueType.contentEquals("integer") || valueType.contentEquals("long")){
				data.add(null); //ValueTimeZone
				data.add(value); //ValueInteger
				data.add(null); //ValueFloat
				data.add(null); //ValueText
				data.add(null); //ValueBinary
			} else if(valueType.contentEquals("float")){
				data.add(null); //ValueTimeZone
				data.add(null); //ValueInteger
				data.add((Double)value); //ValueFloat
				data.add(null); //ValueText
				data.add(null); //ValueBinary
			} else if(valueType.contentEquals("binary")){
				data.add(null); //ValueTimeZone
				data.add(null); //ValueInteger
				data.add(null); //ValueFloat
				data.add(null); //ValueText
				data.add((byte[])value); //ValueBinary
			} else {
				data.add(null); //ValueTimeZone
				data.add(null); //ValueInteger
				data.add(null); //ValueFloat
				data.add(FormatUtility.getInstance().convertToString(value)); //ValueText
				data.add(null); //ValueBinary
			}
			data.add(serializedItemBitmap);
			data.add(items.size());
			
			logger.info(String.format("Recording add custom metadata %s on %s items", fieldName, items.size()));
		} else {
			//Remove event
			data.add(false); //Added
			data.add((String)details.get("fieldName")); //FieldName
			data.add(null); //ValueType
			data.add(null); //ValueTimeZone
			data.add(null); //ValueInteger
			data.add(null); //ValueFloat
			data.add(null); //ValueText
			data.add(null); //ValueBinary
			data.add(serializedItemBitmap);
			data.add(items.size());
			
			logger.info(String.format("Recording remove custom metadata %s on %s items", fieldName, items.size()));
		}
		
		executeInsert(sqlInsertCustomMetadataEvent,data);
	}

	private void recordTagEvent(HistoryEvent event, Map<String, Object> details) throws IOException, SQLException {
		Boolean added = (Boolean)details.get("added");
		String tag = (String)details.get("tag");
		Set<Item> items = event.getAffectedItems();
		
		if(added){ logger.info(String.format("Recording add tag event: %s on %s items", tag, items.size())); }
		else{ logger.info(String.format("Recording remove tag event: %s on %s items", tag, items.size())); }
		
		byte[] serializedItemBitmap = dehydrateItemCollection(items);
		List<Object> data = new ArrayList<Object>();
		data.add(event.getStartDate().getMillis());
		data.add(tag);
		data.add(added);
		data.add(serializedItemBitmap);
		data.add(items.size());
		
		executeInsert(sqlInsertTagEvent,data);
	}
	
	private void createInitialStateSnapshot(Case nuixCase, AnnotationSyncSettings settings) throws IOException, SQLException{
		logger.info("Creating initial tag state snapshot...");
		long snapshotTimestamp = DateTime.now().getMillis();
		
		if(settings.getSyncTagEvents()){
			// Snapshot tag states
			Set<String> tags = nuixCase.getAllTags();
			int tagIndex = 0;
			for(String tag : tags){
				tagIndex++;
				String query = String.format("tag:\"%s\"", tag);
				Set<Item> items = nuixCase.searchUnsorted(query);
				logger.info(String.format("(%s/%s) Recording snapshot of tag '%s' for %s items",tagIndex,tags.size(),tag,items.size()));
				byte[] serializeItemBitmap = dehydrateItemCollection(items);
				List<Object> data = new ArrayList<Object>();
				data.add(snapshotTimestamp);
				data.add(tag);
				data.add(true);
				data.add(serializeItemBitmap);
				data.add(items.size());
				executeInsert(sqlInsertTagEvent,data);
			}
		}
		
		if(settings.getSyncCustodianEvents()){
			// Snapshot custodians
			Set<String> custodians = nuixCase.getAllCustodians();
			int custodianIndex = 0;
			for(String custodian : custodians){
				custodianIndex++;
				String query = String.format("custodian:\"%s\"", custodian);
				Set<Item> items = nuixCase.searchUnsorted(query);
				logger.info(String.format("(%s/%s) Recording snapshot of custodian '%s' for %s items",custodianIndex,
						custodians.size(),custodian,items.size()));
				
				byte[] serializedItemBitmap = dehydrateItemCollection(items);
				
				List<Object> data = new ArrayList<Object>();
				data.add(snapshotTimestamp); //TimeStamp
				data.add(true);
				data.add(custodian);
				data.add(serializedItemBitmap);
				data.add(items.size());
				executeInsert(sqlInsertCustodianEvent,data);
			}
		}
	}
	
	public void eachRecordedTagEvent(long startedAfter, Consumer<TagEvent> callback) throws SQLException{
		List<Object> data = new ArrayList<Object>();
		data.add(startedAfter);
		
		//Tag event retrieval
		executeQuery(sqlSelectFromTagEvent,data,rs ->{
			try {
				while(rs.next()){
					TagEvent event = new TagEvent();
					long timeStampMillis = rs.getLong(1);
					DateTime timeStamp = new DateTime(timeStampMillis);
					event.sourceRepo = this;
					event.timeStamp = timeStamp;
					event.tag = rs.getString(2);
					event.added = rs.getBoolean(3);
					event.bitmapBytes = rs.getBytes(4);
					event.itemCount = rs.getInt(5);
					
					callback.accept(event);
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}
	
	public void eachRecordedTagEvent(DateTime startedAfter, Consumer<TagEvent> callback) throws SQLException{
		eachRecordedTagEvent(startedAfter.getMillis(),callback);
	}
	
	public void eachRecordedExclusionEvent(long startedAfter, Consumer<ExclusionEvent> callback) throws SQLException{
		List<Object> data = new ArrayList<Object>();
		data.add(startedAfter);
		
		executeQuery(sqlSelectFromExclusionEvent,data,rs -> {
			try {
				while(rs.next()){
					long timeStampMillis = rs.getLong(1);
					DateTime timeStamp = new DateTime(timeStampMillis);
					ExclusionEvent event = new ExclusionEvent();
					event.sourceRepo = this;
					event.timeStamp = timeStamp;
					event.excluded = rs.getBoolean(2);
					event.exclusionName = rs.getString(3);
					event.bitmapBytes = rs.getBytes(4);
					event.itemCount = rs.getInt(5);
					
					callback.accept(event);
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}
	
	public void eachRecordedExclusionEvent(DateTime startedAfter, Consumer<ExclusionEvent> callback) throws SQLException{
		eachRecordedExclusionEvent(startedAfter.getMillis(),callback);
	}
	
	public void eachRecordedCustomMetadataEvent(long startedAfter, Consumer<CustomMetadataEvent> callback) throws SQLException{
		List<Object> data = new ArrayList<Object>();
		data.add(startedAfter);
		
		//Custom meta data event retrieval
		executeQuery(sqlSelectFromCustomMetadataEvent,data,rs ->{
			try {
				while(rs.next()){
					long timeStampMillis = rs.getLong(1);
					DateTime timeStamp = new DateTime(timeStampMillis);
					CustomMetadataEvent event = new CustomMetadataEvent();
					event.sourceRepo = this;
					event.timeStamp = timeStamp;
					event.added = rs.getBoolean(2);
					event.fieldName = rs.getString(3);
					event.valueType = rs.getString(4);
					event.valueTimeZone = rs.getString(5);
					event.valueLong = rs.getLong(6);
					event.valueFloat = rs.getDouble(7);
					event.valueText = rs.getString(8);
					event.valueBinary = rs.getBytes(9);
					event.bitmapBytes = rs.getBytes(10);
					event.itemCount = rs.getInt(11);
					callback.accept(event);
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}
	
	public void eachRecordedCustomMetadataEvent(DateTime startedAfter, Consumer<CustomMetadataEvent> callback) throws SQLException{
		eachRecordedCustomMetadataEvent(startedAfter.getMillis(),callback);
	}
	
	public void eachRecordedItemSetEvent(long startedAfter, Consumer<ItemSetEvent> callback) throws SQLException{
		List<Object> data = new ArrayList<Object>();
		data.add(startedAfter);
		
		// Item set event retrieval
		executeQuery(sqlSelectFromItemSetEvent,data,rs -> {
			try {
				while(rs.next()){
					long timeStampMillis = rs.getLong(1);
					DateTime timeStamp = new DateTime(timeStampMillis);
					ItemSetEvent event = new ItemSetEvent();
					event.sourceRepo = this;
					event.timeStamp = timeStamp;
					event.added = rs.getBoolean(2);
					event.settings = rs.getString(3);
					event.itemSetName = rs.getString(4);
					event.batchName = rs.getString(5);
					event.description = rs.getString(6);
					event.bitmapBytes = rs.getBytes(7);
					event.itemCount = rs.getInt(8);
					callback.accept(event);
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}
	
	public void eachRecordedItemSetEvent(DateTime startedAfter, Consumer<ItemSetEvent> callback) throws SQLException {
		eachRecordedItemSetEvent(startedAfter.getMillis(),callback);
	}
	
	public void eachRecordedCustodianEvent(long startedAfter, Consumer<CustodianEvent> callback) throws SQLException {
		List<Object> data = new ArrayList<Object>();
		data.add(startedAfter);
		
		// Item set event retrieval
		executeQuery(sqlSelectFromCustodianEvent,data,rs -> {
			try {
				while(rs.next()){
					long timeStampMillis = rs.getLong(1);
					DateTime timeStamp = new DateTime(timeStampMillis);
					CustodianEvent event = new CustodianEvent();
					event.sourceRepo = this;
					event.timeStamp = timeStamp;
					event.assigned = rs.getBoolean(2);
					event.custodian = rs.getString(3);
					event.bitmapBytes = rs.getBytes(4);
					event.itemCount = rs.getInt(5);
					callback.accept(event);
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}
	
	public void eachRecordedCustodianEvent(DateTime startedAfter, Consumer<CustodianEvent> callback) throws SQLException {
		eachRecordedCustodianEvent(startedAfter.getMillis(), callback);
	}
	
	public void eachRecordedProductionSetEvent(long startedAfter, Consumer<ProductionSetEvent> callback) throws SQLException {
		List<Object> data = new ArrayList<Object>();
		data.add(startedAfter);
		
		// Item set event retrieval
		executeQuery(sqlSelectFromProductionSetEvent,data,rs -> {
			try {
				GsonBuilder gsonBuilder = new GsonBuilder();
				gsonBuilder.serializeNulls();
				gsonBuilder.setPrettyPrinting();
				Gson gson = gsonBuilder.create();
				Type type = new TypeToken<Map<String, Object>>(){}.getType();
				
				while(rs.next()){
					long timeStampMillis = rs.getLong(1);
					DateTime timeStamp = new DateTime(timeStampMillis);
					ProductionSetEvent event = new ProductionSetEvent();
					event.sourceRepo = this;
					event.timeStamp = timeStamp;
					event.added = rs.getBoolean(2);
					event.created = rs.getBoolean(3);
					event.settingsJsonString = rs.getString(4);
					event.settings = gson.fromJson(event.settingsJsonString, type);
					event.productionSetName = rs.getString(5);
					event.bitmapBytes = rs.getBytes(6);
					event.itemCount = rs.getInt(7);
					callback.accept(event);
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}
	
	public void eachRecordedProductionSetEvent(DateTime startedAfter, Consumer<ProductionSetEvent> callback) throws SQLException {
		eachRecordedProductionSetEvent(startedAfter.getMillis(),callback);
	}
	
	/***
	 * Gets whether a more succinct snapshot should be created in a new database.
	 * @return True means the code will attempt to make a snapshot while false means the code will just record 
	 * all annotation history events.
	 */
	public boolean getSnapshotFirstSync() {
		return snapshotFirstSync;
	}

	/***
	 * Sets whether a more succinct snapshot should be created in a new database.
	 * @param snapshotFirstSync True means the code will attempt to make a snapshot while false means the code will just record 
	 * all annotation history events.
	 */
	public void setSnapshotFirstSync(boolean snapshotFirstSync) {
		this.snapshotFirstSync = snapshotFirstSync;
	}

	public int getGuidRefInsertBatchSize() {
		return guidRefInsertBatchSize;
	}

	public void setGuidRefInsertBatchSize(int guidRefInsertBatchSize) {
		this.guidRefInsertBatchSize = guidRefInsertBatchSize;
	}
	
	public AnnotationHistoryRepositorySummary buildSummary() throws SQLException{
		AnnotationHistoryRepositorySummary result = new AnnotationHistoryRepositorySummary();
		result.distinctItemsReferences = executeLongScalar("SELECT COUNT(*) FROM GUIDRef");
		
		result.totalCustomMetadataEvents = executeLongScalar("SELECT COUNT(*) FROM CustomMetadataEvent");
		result.totalExclusionEvents = executeLongScalar("SELECT COUNT(*) FROM ExclusionEvent");
		result.totalItemSetEvents = executeLongScalar("SELECT COUNT(*) FROM ItemSetEvent");
		result.totalTagEvents = executeLongScalar("SELECT COUNT(*) FROM TagEvent");
		
		return result;
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
