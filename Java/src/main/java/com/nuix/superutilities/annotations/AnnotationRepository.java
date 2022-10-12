package com.nuix.superutilities.annotations;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashBiMap;
import com.nuix.superutilities.SuperUtilities;
import com.nuix.superutilities.misc.FormatUtility;
import com.nuix.superutilities.misc.SQLiteBacked;
import com.nuix.superutilities.query.QueryHelper;

import org.apache.log4j.Logger;
import nuix.BulkAnnotater;
import nuix.Case;
import nuix.Item;
import nuix.Markup;
import nuix.MarkupSet;
import nuix.MutablePrintedImage;
import nuix.MutablePrintedPage;
import nuix.PrintedPage;

/***
 * This class leverages a SQLite database file to export and import annotations between Nuix cases.
 * @author Jason Wells
 *
 */
public class AnnotationRepository extends SQLiteBacked {
	private static Logger logger = Logger.getLogger(AnnotationRepository.class);
	
	private boolean abortWasRequested = false;
	private Consumer<String> messageLoggedCallback = null;
	private boolean alwaysCreateTagOnImport = false;
	
	/***
	 * Allows you to provide a callback which will be invoked when this instance emits a log message.
	 * @param callback The callback to receive logged messages
	 */
	public void whenMessageLogged(Consumer<String> callback) {
		messageLoggedCallback = callback;
	}
	
	/***
	 * Logs a message, either providing it to the callback supplied in a call to {@link #whenMessageLogged(Consumer)} or in
	 * absence of that callback, to log4j.
	 * @param message
	 */
	private void logMessage(String message) {
		if(messageLoggedCallback != null) {
			messageLoggedCallback.accept(message);
		} else {
			logger.info(message);
		}
	}
	
	/***
	 * Logs a message, either providing it to the callback supplied in a call to {@link #whenMessageLogged(Consumer)} or in
	 * absence of that callback, to log4j.  This method passes the message through a call to String.format with the message
	 * being provided as the format and the params provided as the params to String.format.
	 * @param format The message format string, formatted as accepted by String.format.
	 * @param params Parameters to be inserted into the formatted string, as accepted by String.format.
	 */
	private void logMessage(String format, Object... params) {
		logMessage(String.format(format, params));
	}
	
	private BiConsumer<Integer,Integer> progressUpdatedCallback = null;
	
	/***
	 * Allows you to provide a callback which will be invoked when this instance emits a progress update.
	 * @param callback The callback to invoke when progress is updated
	 */
	public void whenProgressUpdated(BiConsumer<Integer,Integer> callback) {
		progressUpdatedCallback = callback;
	}
	
	/***
	 * Invokes callback previously provided in a call to {@link #whenProgressUpdated(BiConsumer)}, if one has been provided.
	 * @param current The current progress amount.
	 * @param total The total amount of work.
	 */
	private void fireProgressUpdated(int current, int total) {
		if(progressUpdatedCallback != null) {
			progressUpdatedCallback.accept(current,total);
		}
	}
	
	private HashBiMap<String,Long> itemGuidIdLookup = HashBiMap.create();
	private HashBiMap<String,Long> markupSetIdLookup = HashBiMap.create();
	private HashBiMap<String,Long> tagIdLookup = HashBiMap.create();
	
	/***
	 * Creates a new instance associated to the specified SQLite DB file.  File will be created if it does not already exist.
	 * @param databaseFile The SQLite database file to use.
	 * @throws SQLException Thrown if there are errors while interacting with the SQLite DB file.
	 */
	public AnnotationRepository(String databaseFile) throws SQLException {
		this(new File(databaseFile));
	}
	
	/***
	 * Creates a new instance associated to the specified SQLite DB file.  File will be created if it does not already exist.
	 * @param databaseFile The SQLite database file to use.
	 * @throws SQLException Thrown if there are errors while interacting with the SQLite DB file.
	 */
	public AnnotationRepository(File databaseFile) throws SQLException {
		super(databaseFile);
		createTables();
	}
	
	/***
	 * Ensures the expected tables exist in the associated SQLite DB file.
	 * @throws SQLException Thrown if there are errors while interacting with the SQLite DB file.
	 */
	private void createTables() throws SQLException {
		// Create table with item info
		String createTableItem = "CREATE TABLE IF NOT EXISTS Item ("+
				"ID INTEGER PRIMARY KEY AUTOINCREMENT, Name TEXT, GUID TEXT, MD5 TEXT)";
		executeUpdate(createTableItem);
		
		// Create table with markup set info
		String createTableMarkupSet = "CREATE TABLE IF NOT EXISTS MarkupSet ("+
				"ID INTEGER PRIMARY KEY AUTOINCREMENT, Name TEXT, Description TEXT, RedactionReason TEXT)";
		executeUpdate(createTableMarkupSet);
		
		// Create table with markup information
		String createTableItemMarkup = "CREATE TABLE IF NOT EXISTS ItemMarkup ("+
				"ID INTEGER PRIMARY KEY AUTOINCREMENT, Item_ID INTEGER, MarkupSet_ID INTEGER, PageNumber INTEGER,"+
				"IsRedaction INTEGER, X REAL, Y REAL, Width REAL, Height REAL)";
		executeUpdate(createTableItemMarkup);
		
		// Create table with tag info
		String createTableTag = "CREATE TABLE IF NOT EXISTS Tag ("+
				"ID INTEGER PRIMARY KEY AUTOINCREMENT, Name TEXT)";
		executeUpdate(createTableTag);
		
		// Create table with tag to item associations
		String createTableItemTag = "CREATE TABLE IF NOT EXISTS ItemTag ("+
				"ID INTEGER PRIMARY KEY AUTOINCREMENT, Item_ID INTEGER, Tag_ID INTEGER)";
		executeUpdate(createTableItemTag);
		
		rebuildXrefs();
	}
	
	/***
	 * Rebuilds in memory look ups for GUID/MD5 => database record IDs.  Later as new records are added
	 * to the database file, these lookup will be updated in tandem as in memory caches.
	 * @throws SQLException Thrown if there are errors while interacting with the SQLite DB file.
	 */
	private void rebuildXrefs() throws SQLException {
		logMessage("Building GUID lookup from any existing entries in DB...");
		itemGuidIdLookup.clear();
		String sql = "SELECT GUID,ID FROM Item";
		executeQuery(sql, null, new Consumer<ResultSet>() {
			@Override
			public void accept(ResultSet rs) {
				try {
					while(rs.next()) {
						String guid = FormatUtility.bytesToHex(rs.getBytes(1));
						long id = rs.getLong(2);
						itemGuidIdLookup.put(guid, id);
					}
				} catch (SQLException exc) {
					logger.error("Error building GUID to ID XREF",exc);
				}
			}
		});
		
		logMessage("Building MarkupSet name lookup from any existing entries in DB...");
		markupSetIdLookup.clear();
		sql = "SELECT Name,ID FROM MarkupSet";
		executeQuery(sql,null, new Consumer<ResultSet>() {
			@Override
			public void accept(ResultSet rs) {
				try {
					while(rs.next()) {
						String name = rs.getString(1);
						long id = rs.getLong(2);
						markupSetIdLookup.put(name, id);
					}
				} catch (SQLException exc) {
					logger.error("Error building Markup Set Name to ID XREF",exc);
				}
			}
		});
		
		logMessage("Building Tag name lookup from any existing entries in DB...");
		tagIdLookup.clear();
		sql = "SELECT Name,ID FROM Tag";
		executeQuery(sql,null, new Consumer<ResultSet>() {
			@Override
			public void accept(ResultSet rs) {
				try {
					while(rs.next()) {
						String name = rs.getString(1);
						long id = rs.getLong(2);
						tagIdLookup.put(name, id);
					}
				} catch (SQLException exc) {
					logger.error("Error building Tag Name to ID XREF",exc);
				}
			}
		});
	}
	
	/***
	 * Stores all markup sets found in the provided case to the SQLite DB file.
	 * @param nuixCase The Nuix case to record markup sets from.
	 * @throws IOException Thrown most likely if there was an issue searching or retrieving printed pages of and item.
	 * @throws SQLException Thrown if anything goes wrong interacting with the SQLite database file.
	 */
	public void storeAllMarkupSets(Case nuixCase) throws IOException, SQLException {
		List<MarkupSet> markupSets = nuixCase.getMarkupSets();
		for(MarkupSet markupSet : markupSets) {
			// Support aborting
			if(abortWasRequested) { break; }
			
			storeMarkupSet(nuixCase, markupSet);
		}
	}
	
	/***
	 * Gets the sequentially assigned ID value from the Item table for a given item based on its GUID.  Will attempt to get this from a cache first.
	 * @param item The item to retrieve the DB ID number for.
	 * @return The DB ID number for the given item, based on finding a record in the Item table with a matching GUID.
	 * @throws SQLException Thrown if there are errors while interacting with the SQLite DB file.
	 */
	public long getItemId(Item item) throws SQLException {
		String guid = item.getGuid().replaceAll("\\-", "");
		
		if(itemGuidIdLookup.containsKey(guid)) {
			return itemGuidIdLookup.get(guid);
		} else {
			String md5 = item.getDigests().getMd5();
			String name = item.getLocalisedName();
			
			byte[] guidBytes = FormatUtility.hexToBytes(guid);
			byte[] md5Bytes = null;
			if(md5 == null) {
				logMessage("Item with GUID %s has no MD5",guid);
			} else {
				md5Bytes = FormatUtility.hexToBytes(md5);
			}
			
			String sql = "INSERT INTO Item (GUID,MD5,Name) VALUES (?,?,?)";
			executeInsert(sql, guidBytes, md5Bytes, name);
			long id = executeLongScalar("SELECT ID FROM Item WHERE GUID = ?", guidBytes);
			itemGuidIdLookup.put(guid, id);
			return id;
		}
	}
	
	/***
	 * Gets the sequentially assigned ID value from the MarkupSet table for a given markup.  Will attempt to get this from a cache first.
	 * @param markupSet The markup set to find the DB ID for.
	 * @return The DB ID number for the given markup set, based on finding a record in the MarkupSet table with a matching name.
	 * @throws SQLException Thrown if there are errors while interacting with the SQLite DB file.
	 */
	public long getMarkupSetId(MarkupSet markupSet) throws SQLException {
		String name = markupSet.getName();
		if(markupSetIdLookup.containsKey(name)) {
			return markupSetIdLookup.get(name);
		} else {
			String description = markupSet.getDescription();
			String redactionReason = markupSet.getRedactionReason();
			
			String sql = "INSERT INTO MarkupSet (Name,Description,RedactionReason) VALUES (?,?,?)";
			executeInsert(sql,name,description,redactionReason);
			long id = executeLongScalar("SELECT ID FROM MarkupSet WHERE Name = ?", name);
			markupSetIdLookup.put(name, id);
			return id;
		}
	}
	
	/***
	 * Gets the sequentially assigned ID value from the Tag table for a given tag name.  Will attempt to get this from a cache first.
	 * @param tagName Name of the tag you wish to get the ID of.
	 * @return The DB ID number for the given Tag, based on finding a record in the Tag table with a matching name.
	 * @throws SQLException Thrown if there are errors while interacting with the SQLite DB file.
	 */
	public long getTagId(String tagName) throws SQLException {
		if(tagIdLookup.containsKey(tagName)) {
			return tagIdLookup.get(tagName);
		} else {
			String sql = "INSERT INTO Tag (Name) VALUES (?)";
			executeInsert(sql,tagName);
			long id = executeLongScalar("SELECT ID FROM Tag WHERE Name = ?", tagName);
			tagIdLookup.put(tagName, id);
			return id;
		}
	}
	
	/***
	 * Stores a specific tag present in the provided case as records in the DB file. 
	 * @param nuixCase The Nuix case that the specified tag is present in.
	 * @param tagName The name of the tag in the specified case to store in the DB file.
	 * @throws IOException Thrown if a search error occurs.
	 * @throws SQLException Thrown if there are errors while interacting with the SQLite DB file.
	 */
	public void storeTag(Case nuixCase, String tagName) throws IOException, SQLException {
		logMessage("Storing tag: %s",tagName);
		String insertItemTag = "INSERT INTO ItemTag (Item_ID,Tag_ID) VALUES (?,?)";
		String itemQuery = QueryHelper.orTagQuery(tagName);
		Set<Item> tagItems = nuixCase.searchUnsorted(itemQuery);
		long tagId = getTagId(tagName);
		int currentItemIndex = 1;
		for(Item item : tagItems) {
			// Support aborting
			if(abortWasRequested) { break; }
			fireProgressUpdated(currentItemIndex, tagItems.size());
			long itemId = getItemId(item);
			executeInsert(insertItemTag,itemId,tagId);
		}
	}
	
	/***
	 * Stores all tags present in the provided case as records in the DB file. 
	 * @param nuixCase The Nuix case to record tags from.
	 * @throws IOException Thrown if a search error occurs.
	 * @throws SQLException Thrown if there are errors while interacting with the SQLite DB file.
	 */
	public void storeAllTags(Case nuixCase) throws IOException, SQLException {
		Set<String> tags = nuixCase.getAllTags();
		for(String tag : tags) {
			// Support aborting
			if(abortWasRequested) { break; }
			storeTag(nuixCase,tag);
		}
	}
	
	/***
	 * Applies tags to items in the provided case based on tag records in the DB file associated to this instance.
	 * @param nuixCase The case in which items will be tagged.
	 * @param matchingMethod Determines how a record in the DB file is associated to an item in the case to apply tags to it.
	 * @throws SQLException Thrown if there are errors while interacting with the SQLite DB file.
	 */
	public void applyTagsFromDatabaseToCase(Case nuixCase, AnnotationMatchingMethod matchingMethod) throws SQLException {
		// Will reuse this multiple times to provide values to be bound to prepared SQL statements later
		List<Object> bindData = new ArrayList<Object>();
		// Will use this to apply tags later
		BulkAnnotater annotater = SuperUtilities.getBulkAnnotater();
		
		// SQL query to get information about each item a given tag is to be applied to
		String itemTagSql = "SELECT i.GUID,i.MD5,i.Name FROM ItemTag AS t " + 
				"INNER JOIN Item AS i ON t.Item_ID = i.ID " + 
				"WHERE t.Tag_ID = ? " + 
				"ORDER BY MD5,GUID";
		
		// SQL query to get count of items a given tag should be applied to
		String itemTagCountSql = "SELECT COUNT(*) FROM ItemTag AS t " + 
				"INNER JOIN Item AS i ON t.Item_ID = i.ID " + 
				"WHERE t.Tag_ID = ? " + 
				"ORDER BY MD5,GUID";
		
		// Always good to tell the user what you're doing and create a record of the settings in use
		logMessage("Applying tags to case...");
		if(matchingMethod == AnnotationMatchingMethod.GUID) {
			logMessage("Matching DB entries to case items using: GUID");
		} else if(matchingMethod == AnnotationMatchingMethod.MD5) {
			logMessage("Matching DB entries to case items using: MD5");
		}
		
		// Since we can apply any given tag to multiple items at once and this is a more efficient approach,
		// we gather up all the items a given tag will be applied to and then apply that tag in batches.  Periodically
		// we will apply a batch of tags and clear this collection so we don't need to hold on to all the items receiving
		// a given tag at once.
		Set<Item> tagGroupedItems = new HashSet<Item>();
		
		// Use our in memory cache of Name->ID to drive application of each tag since it should
		// already be in memory and synced to the state of the database.
		for(Map.Entry<String, Long> tagEntry : tagIdLookup.entrySet()) {
			// Support aborting
			if(abortWasRequested) { break; }
			
			String tagName = tagEntry.getKey();
			long tagId = tagEntry.getValue();
			
			
			
			bindData.clear();
			bindData.add(tagId);
			
			// Determine how many items this tag should be applied to based on the number
			// of ItemTag records associated to this tag.
			int totalItemTags = executeLongScalar(itemTagCountSql,bindData).intValue();
			
			// Here we run the query for ItemTag records associated with the tag we are currently
			// processing.  We will then collect up the relevant items.  When we get a good batch of
			// items, we tag them, clear the collection and continue on.
			logMessage("Processing tag '%s' and %s items",tagName,totalItemTags);
			
			// If we intend to create the tag regardless of whether there are items that
			// it will be applied to, we can just create it directly in the case.
			if(alwaysCreateTagOnImport) {
				try {
					nuixCase.createTag(tagName);
				} catch (IOException e) {
					logger.error("Error creating tag '"+tagName+"' in case: ", e);
					logMessage("Error creating tag '"+tagName+"' in case: ",e.getMessage());
				}
			}
			
			executeQuery(itemTagSql,bindData, rs ->{
				int currentIndex = 1;
				try {
					while(rs.next()) {
						fireProgressUpdated(currentIndex,totalItemTags);
						
						// GUID and MD5 are hex strings.  We store them in the database as the byte arrays those hex
						// strings represent which reduces the storage footprint for these values.  Nuix needs them as
						// hex strings for use in queries, so we need to convert them back to hex strings here.
						String guid = FormatUtility.bytesToHex(rs.getBytes(1));
						String md5 = FormatUtility.bytesToHex(rs.getBytes(2));
						String itemName = rs.getString(3);
						
						// If a given record does not have an MD5 (likely because the source item had no Md5) we can't really
						// use MD5 matching from database record to destination case item, so we report the issue to the user
						// and skip this record.
						if(md5 == null && matchingMethod == AnnotationMatchingMethod.MD5) {
							logMessage("Record for item named '%s' with GUID %s does not have an MD5 value",itemName,guid);
							continue;
						}
						
						Set<Item> items = null;
						
						// Obtain the relevant item or items depending on the matching method specified
						if(matchingMethod == AnnotationMatchingMethod.GUID) {
							items = nuixCase.searchUnsorted("guid:"+guid);
						} else if(matchingMethod == AnnotationMatchingMethod.MD5) {
							items = nuixCase.searchUnsorted("md5:"+md5);
						}
						
						// Add all the items we found to our collection
						tagGroupedItems.addAll(items);
						
						// If our collection has 5000 items or more in it now, lets tag those items and then
						// clear the collection so we aren't holding on to all of the items at once.
						if(tagGroupedItems.size() > 5000) {
							logMessage("    Tagging batch of %s items",tagGroupedItems.size());
							annotater.addTag(tagName, tagGroupedItems);
							tagGroupedItems.clear();
						}
						
						currentIndex++;
					}
					
					// If there are any items left in our collection that still need a tag applied, we check and
					// tag them here.
					if(tagGroupedItems.size() > 0) {
						logMessage("    Tagging final batch of %s items",tagGroupedItems.size());
						annotater.addTag(tagName, tagGroupedItems);
						tagGroupedItems.clear();
					}
					
				} catch (SQLException e) {
					logger.error("Error retrieving ItemTag data from database", e);
					logMessage("Error retrieving ItemTag data from database: %s",e.getMessage());
				} catch (IOException e) {
					logger.error("Error retrieving item from case", e);
					logMessage("Error retrieving item from case: ",e.getMessage());
				}
			});
		}
	}
	
	/***
	 * Stores a particular markup set present in the provided Nuix case.
	 * @param nuixCase The Nuix case containing the provided markup set.
	 * @param markupSet The specific markup set to store.
	 * @throws IOException Thrown most likely if there was an issue searching or retrieving printed pages of and item.
	 * @throws SQLException Thrown if anything goes wrong interacting with the SQLite database file.
	 */
	public void storeMarkupSet(Case nuixCase, MarkupSet markupSet) throws IOException, SQLException {
		logMessage("Storing markups from MarkupSet: "+markupSet.getName());
		long itemMarkupCountBefore = getItemMarkupCount();
		logMessage("Item Markup Count Before: %s", itemMarkupCountBefore);
		String insertItemMarkup = "INSERT INTO ItemMarkup (Item_ID,MarkupSet_ID,PageNumber,IsRedaction,X,Y,Width,Height) VALUES (?,?,?,?,?,?,?,?)";
		String itemQuery = QueryHelper.markupSetQuery(markupSet);

		long markupSetId = getMarkupSetId(markupSet);
		Set<Item> markupSetItems = nuixCase.searchUnsorted(itemQuery);
		int currentItemIndex = 1;
		for(Item item : markupSetItems) {
			// Support aborting
			if(abortWasRequested) { break; }
			
			fireProgressUpdated(currentItemIndex, markupSetItems.size());
			long itemId = getItemId(item);
			MutablePrintedImage itemImage = item.getPrintedImage();
			List<? extends PrintedPage> pages = itemImage.getPages();
			for (int i = 0; i < pages.size(); i++) {
				MutablePrintedPage page = (MutablePrintedPage) pages.get(i);
				Set<Markup> pageMarkups = page.getMarkups(markupSet);
				for(Markup pageMarkup : pageMarkups) {
					executeInsert(insertItemMarkup,
							itemId,
							markupSetId,
							i+1,
							pageMarkup.isRedaction(),
							pageMarkup.getX(),
							pageMarkup.getY(),
							pageMarkup.getWidth(),
							pageMarkup.getHeight());
				}
			}
			currentItemIndex++;
		}
		
		long itemMarkupCountAfter = getItemMarkupCount();
		logMessage("Item Markup Count After: %s",itemMarkupCountAfter);
		logMessage("Difference: +%s",(itemMarkupCountAfter - itemMarkupCountBefore));
	}
	
	/***
	 * Applies markups present in the SQLite DB file associated to this instance to the provided Nuix case.
	 * @param nuixCase The Nuix case to apply the DB file markups to.
	 * @param addToExistingMarkupSet Whether to append marukps to existing markup sets of the the same name or create a new markup set with a suffixed name.
	 * @param matchingMethod Specifies whether to match records in the DB file to item in the case by using GUID or MD5.
	 * @throws SQLException Thrown if there are errors while interacting with the SQLite DB file.
	 */
	public void applyMarkupsFromDatabaseToCase(Case nuixCase, boolean addToExistingMarkupSet, AnnotationMatchingMethod matchingMethod) throws SQLException {
		Map<String,MarkupSet> existingMarkupSetLookup = new HashMap<String,MarkupSet>();
		for(MarkupSet existingMarkupSet : nuixCase.getMarkupSets()) {
			existingMarkupSetLookup.put(existingMarkupSet.getName(), existingMarkupSet);
		}
		
		List<Object> bindData = new ArrayList<Object>();
		
		// Use our in memory cache of Name->ID to drive application of each markup set since it should
		// already be in memory and synced to the state of the database.
		for(Map.Entry<String, Long> markupEntry : markupSetIdLookup.entrySet()) {
			// Support aborting
			if(abortWasRequested) { break; }
			
			String markupSetName = markupEntry.getKey();
			long markupSetId = markupEntry.getValue();
			
			// SQL to get description for this markup set
			String markupSetDescription = executeStringScalar("SELECT Description FROM MarkupSet WHERE ID = ?",markupSetId);
			// SQL to get reason for this markup set
			String markupSetRedactionReason = executeStringScalar("SELECT RedactionReason FROM MarkupSet WHERE ID = ?",markupSetId);
			
			// Always good to echo back to user the settings they are using
			logMessage("Applying markups to case from MarkupSet: %s",markupSetName);
			if(matchingMethod == AnnotationMatchingMethod.GUID) {
				logMessage("Matching DB entries to case items using: GUID");
			} else if(matchingMethod == AnnotationMatchingMethod.MD5) {
				logMessage("Matching DB entries to case items using: MD5");
			}
			
			// We need to resolve the MarkupSet object, either by obtaining an existing one in destination case or creating a new one.
			MarkupSet markupSet = null;
			if(existingMarkupSetLookup.containsKey(markupSetName)) {
				if(addToExistingMarkupSet) {
					// We can just add more annotations the the existing markup set with the same name
					logMessage("Applying markups in destination case to existing markup set: %s",markupSetName);
					markupSet = existingMarkupSetLookup.get(markupSetName);
				} else {
					// When addToExisting is false and we have a name collision, we will attempt to find a usable name
					int nameSequence = 2;
					String targetName = markupSetName+"_"+nameSequence;
					while(existingMarkupSetLookup.containsKey(targetName)) {
						nameSequence++;
						targetName = markupSetName+"_"+nameSequence;
					}
					
					logMessage("Applying markups in DB to new markup set: %s",targetName);
					
					Map<String,Object> markupSetSettings = new HashMap<String,Object>();
					markupSetSettings.put("description", markupSetDescription);
					markupSetSettings.put("redactionReason", markupSetRedactionReason);
					markupSet = nuixCase.createMarkupSet(targetName, markupSetSettings);
				}
			} else {
				logMessage("Applying markups in DB to new markup set: %s",markupSetName);
				// Markup set does not appear to already exist, so lets create it
				Map<String,Object> markupSetSettings = new HashMap<String,Object>();
				markupSetSettings.put("description", markupSetDescription);
				markupSetSettings.put("redactionReason", markupSetRedactionReason);
				markupSet = nuixCase.createMarkupSet(markupSetName, markupSetSettings);
			}
			
			final MarkupSet targetMarkupSet = markupSet;
			
			
			// SQL for info needed to apply markups.  Sorted by MD5/GUID/PageNumber so that we should get markups for the same item
			// one after another, making our cache defined below more efficiently leveraged.
			String itemMarkupSql = "SELECT i.GUID,i.MD5,i.Name,im.PageNumber,im.IsRedaction,im.X,im.Y,im.Width,im.Height FROM ItemMarkup AS im " + 
					"INNER JOIN Item AS i ON im.Item_ID = i.ID " + 
					"WHERE im.MarkupSet_ID = ? " + 
					"ORDER BY MD5,GUID,PageNumber";
			
			// SQL to determine total item markup records for the current markup set
			String itemMarkupTotalCountSql = "SELECT COUNT(*) FROM ItemMarkup AS im " + 
					"INNER JOIN Item AS i ON im.Item_ID = i.ID " + 
					"WHERE im.MarkupSet_ID = ? ";
			
			bindData.clear();
			bindData.add(markupSetId);
			
			int totalItemMarkups = executeLongScalar(itemMarkupTotalCountSql,bindData).intValue();
			
			// We use a cache for item retrieval, running a serach for the item by GUID or MD5 if requested but
			// not currently present in the cache.
			LoadingCache<String,Set<Item>> itemCache = CacheBuilder.newBuilder()
					.maximumSize(1000)
					.build(new CacheLoader<String,Set<Item>>(){

						@Override
						public Set<Item> load(String guidOrMd5) throws Exception {
							// When a given GUID or MD5 is found to note already be present in our cache
							// we will need to go find it in our case, cache it and return it.
							Set<Item> items = new HashSet<Item>();
							if(matchingMethod == AnnotationMatchingMethod.GUID) {
								items = nuixCase.searchUnsorted("guid:"+guidOrMd5);
								if(items.size() < 1) {
									logMessage("No items in case found to match GUID: %s",guidOrMd5);
								}
							} else if(matchingMethod == AnnotationMatchingMethod.MD5) {
								items = nuixCase.searchUnsorted("md5:"+guidOrMd5);
								if(items.size() < 1) {
									logMessage("No items in case found to match MD5: %s",guidOrMd5);
								}
							}
							return items;
						}
					});
			
			// We now run our SQL to get item markup information, iterating each result and applying it
			// to matching items in our case.
			executeQuery(itemMarkupSql,bindData,new Consumer<ResultSet>() {
				int currentIndex = 1;
				@Override
				public void accept(ResultSet rs) {
					try {
						while(rs.next()) {
							// Support aborting
							if(abortWasRequested) { break; }
							
							fireProgressUpdated(currentIndex,totalItemMarkups);
							
							// GUID and MD5 are stored in database as byte arrays to save space, but we need them as hex strings
							// for Nuix searching, so we need to convert them back to strings here.
							String guid = FormatUtility.bytesToHex(rs.getBytes(1));
							String md5 = FormatUtility.bytesToHex(rs.getBytes(2));
							String itemName = rs.getString(3);
							
							// Get details needed to apply a markup to the relevant item
							long pageNumber = rs.getLong(4);
							boolean isRedaction = rs.getBoolean(5);
							double x = rs.getDouble(6);
							double y = rs.getDouble(7);
							double width = rs.getDouble(8);
							double height = rs.getDouble(9);
							
							// If our matching method is MD5, but the current record does not have an MD5 (likely because the originating item
							// did not have an MD5, we let the user know and then skip this record.
							if(md5 == null && matchingMethod == AnnotationMatchingMethod.MD5) {
								logMessage("Record for item named '%s' with GUID %s does not have an MD5 value",itemName,guid);
								continue;
							}
							
							Set<Item> items = null;
							
							// Leverage our cache to minimize unnecessary searching for the same item or items repeatedly
							if(matchingMethod == AnnotationMatchingMethod.GUID) {
								items = itemCache.get(guid);
							} else if(matchingMethod == AnnotationMatchingMethod.MD5) {
								items = itemCache.get(md5);
							}
							
							// Apply markup to relevant items in the destination case
							for(Item item : items) {
								MutablePrintedImage itemImage = item.getPrintedImage();
								List<? extends PrintedPage> pages = itemImage.getPages();
								
								if(pages == null || pages.size() < 1) {
									logMessage("Item named '%s' and GUID %s has no printed pages, generating now...",itemName,guid);
									itemImage.generate();
									pages = itemImage.getPages();
								}
								
								if(pages.size() < pageNumber-1) {
									logMessage("Item named '%s' and GUID %s does not have a page %s",itemName,guid,pageNumber);
									continue;
								}
								
								MutablePrintedPage page = (MutablePrintedPage)pages.get((int) (pageNumber-1));
								if(isRedaction) {
									page.createRedaction(targetMarkupSet, x, y, width, height);
								} else {
									page.createHighlight(targetMarkupSet, x, y, width, height);
								}
							}
							currentIndex++;
						}
					} catch (SQLException exc) {
						logger.error("Error retrieving ItemMarkup data from database", exc);
						logMessage("Error retrieving ItemMarkup data from database: %s",exc.getMessage());
					} catch (IOException exc2) {
						logger.error("Error retrieving item from case", exc2);
						logMessage("Error retrieving item from case: ",exc2.getMessage());
					} catch (ExecutionException e) {
						logger.error(e);
					}
				}
			});
		}
	}
	
	/***
	 * Queries the DB for the names of tags associated with the provided MD5.
	 * @param md5 The MD5 to find the associated tags of.
	 * @return A list of 0 or more Tag names associated with the specified MD5.
	 * @throws SQLException Thrown if there are errors while interacting with the SQLite DB file.
	 */
	public List<String> getTagsForMd5(String md5) throws SQLException{
		String sql = "SELECT DISTINCT t.Name AS TagName FROM Tag AS t "+
				"INNER JOIN ItemTag AS it ON it.Tag_ID = t.ID "+
				"INNER JOIN Item AS i ON it.Item_ID = i.ID "+
				"WHERE i.MD5 = ? ";
		List<String> result = new ArrayList<String>();
		List<Object> bindData = new ArrayList<Object>();
		bindData.add(FormatUtility.hexToBytes(md5));
		executeQuery(sql,bindData,rs ->{
			try {
				while(rs.next()) {
					String tag = rs.getString(1);
					result.add(tag);
				}
			} catch (SQLException e) {
				logger.error("Error retrieving tags associated to MD5 "+md5,e);
			}
		});
		return result;
	}
	
	/***
	 * Queries the DB for the names of tags associated with the provided GUID.
	 * @param guid The GUID to find the associated tags of.
	 * @return A list of 0 or more Tag names associated with the specified GUID.
	 * @throws SQLException Thrown if there are errors while interacting with the SQLite DB file.
	 */
	public List<String> getTagsForGuid(String guid) throws SQLException{
		String sql = "SELECT DISTINCT t.Name AS TagName FROM Tag AS t "+
				"INNER JOIN ItemTag AS it ON it.Tag_ID = t.ID "+
				"INNER JOIN Item AS i ON it.Item_ID = i.ID "+
				"WHERE i.GUID = ? ";
		List<String> result = new ArrayList<String>();
		List<Object> bindData = new ArrayList<Object>();
		bindData.add(FormatUtility.hexToBytes(guid));
		executeQuery(sql,bindData,rs ->{
			try {
				while(rs.next()) {
					String tag = rs.getString(1);
					result.add(tag);
				}
			} catch (SQLException e) {
				logger.error("Error retrieving tags associated to GUID "+guid,e);
			}
		});
		return result;
	}
	
	/***
	 * Gets the number of markups present in the ItemMarkup table.
	 * @return The number of markup records present in the ItemMarkup table of the DB file.
	 * @throws SQLException Thrown if there are errors while interacting with the SQLite DB file.
	 */
	public long getItemMarkupCount() throws SQLException {
		return executeLongScalar("SELECT COUNT(*) FROM ItemMarkup");
	}
	
	/***
	 * Signals that you wish to abort.  Export and import logic test for the abort signal in various loops and will cut short their iteration when
	 * the abort signal has been set.
	 */
	public void abort() {
		logMessage("Signalling abort...");
		abortWasRequested = true;
	}

	/***
	 * Gets whether a tag should always be created even when the source case had no items associated to that tag.
	 * @return True if tag should be created during import, even if no items are associated.
	 */
	public boolean getAlwaysCreateTagOnImport() {
		return alwaysCreateTagOnImport;
	}

	/***
	 * Sets whether a tag should always be created even when the source case had no items associated to that tag.
	 * @param alwaysCreateTagOnImport True if tag should be created during import, even if no items are associated.
	 */
	public void setAlwaysCreateTagOnImport(boolean alwaysCreateTagOnImport) {
		this.alwaysCreateTagOnImport = alwaysCreateTagOnImport;
	}
}
