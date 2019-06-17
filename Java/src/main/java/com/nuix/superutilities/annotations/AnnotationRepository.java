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
import com.nuix.superutilities.misc.FormatUtility;
import com.nuix.superutilities.misc.SQLiteBacked;
import com.nuix.superutilities.query.QueryHelper;

import jxl.common.Logger;
import nuix.Case;
import nuix.Item;
import nuix.Markup;
import nuix.MarkupSet;
import nuix.MutablePrintedImage;
import nuix.MutablePrintedPage;
import nuix.PrintedPage;

public class AnnotationRepository extends SQLiteBacked {
	private static Logger logger = Logger.getLogger(AnnotationRepository.class);
	
	private boolean abortWasRequested = false;
	private Consumer<String> messageLoggedCallback = null;
	
	/***
	 * Allows you to provide a callback which will be invoked when this instance emits a log message.
	 * @param callback The callback to receive logged messages
	 */
	public void whenMessageLogged(Consumer<String> callback) {
		messageLoggedCallback = callback;
	}
	
	private void logMessage(String message) {
		if(messageLoggedCallback != null) {
			messageLoggedCallback.accept(message);
		} else {
			logger.info(message);
		}
	}
	
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
	
	private void fireProgressUpdated(int current, int total) {
		if(progressUpdatedCallback != null) {
			progressUpdatedCallback.accept(current,total);
		}
	}
	
	private HashBiMap<String,Long> itemGuidIdLookup = HashBiMap.create();
	private HashBiMap<String,Long> markupSetIdLookup = HashBiMap.create();
	
	public AnnotationRepository(String databaseFile) throws SQLException {
		this(new File(databaseFile));
	}
	
	public AnnotationRepository(File databaseFile) throws SQLException {
		super(databaseFile);
		createTables();
	}
	
	private void createTables() throws SQLException {
		// Create item table
		String createTableItem = "CREATE TABLE IF NOT EXISTS Item ("+
				"ID INTEGER PRIMARY KEY AUTOINCREMENT, Name TEXT, GUID TEXT, MD5 TEXT)";
		executeUpdate(createTableItem);
		
		String createTableMarkupSet = "CREATE TABLE IF NOT EXISTS MarkupSet ("+
				"ID INTEGER PRIMARY KEY AUTOINCREMENT, Name TEXT, Description TEXT, RedactionReason TEXT)";
		executeUpdate(createTableMarkupSet);
		
		String createTableItemMarkup = "CREATE TABLE IF NOT EXISTS ItemMarkup ("+
				"ID INTEGER PRIMARY KEY AUTOINCREMENT, Item_ID INTEGER, MarkupSet_ID INTEGER, PageNumber INTEGER,"+
				"IsRedaction INTEGER, X REAL, Y REAL, Width REAL, Height REAL)";
		executeUpdate(createTableItemMarkup);
		
		rebuildXrefs();
	}
	
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
	}
	
	public void storeAllMarkupSets(Case nuixCase) throws IOException, SQLException {
		abortWasRequested = false;
		List<MarkupSet> markupSets = nuixCase.getMarkupSets();
		for(MarkupSet markupSet : markupSets) {
			if(abortWasRequested) { break; }
			storeMarkupSet(nuixCase, markupSet);
		}
	}
	
	public void storeMarkupSet(Case nuixCase, MarkupSet markupSet) throws IOException, SQLException {
		abortWasRequested = false;
		logMessage("Storing markups from MarkupSet: "+markupSet.getName());
		long itemMarkupCountBefore = getItemMarkupCount();
		logMessage("Item Markup Count Before: %s", itemMarkupCountBefore);
		String insertItemMarkup = "INSERT INTO ItemMarkup (Item_ID,MarkupSet_ID,PageNumber,IsRedaction,X,Y,Width,Height) VALUES (?,?,?,?,?,?,?,?)";
		String itemQuery = QueryHelper.markupSetQuery(markupSet);

		long markupSetId = getMarkupSetId(markupSet);
		Set<Item> markupSetItems = nuixCase.searchUnsorted(itemQuery);
		int currentItemIndex = 1;
		for(Item item : markupSetItems) {
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
	
	public long getItemId(Item item) throws SQLException {
		String guid = item.getGuid().replaceAll("\\-", "");
		
		if(itemGuidIdLookup.containsKey(guid)) {
			return itemGuidIdLookup.get(guid);
		} else {
			String md5 = item.getDigests().getMd5();
			String name = item.getLocalisedName();
			
			byte[] guidBytes = FormatUtility.hexToBytes(guid);
			byte[] md5Bytes = FormatUtility.hexToBytes(md5);
			
			String sql = "INSERT INTO Item (GUID,MD5,Name) VALUES (?,?,?)";
			executeInsert(sql, guidBytes, md5Bytes, name);
			long id = executeLongScalar("SELECT ID FROM Item WHERE GUID = ?", guidBytes);
			itemGuidIdLookup.put(guid, id);
			return id;
		}
	}
	
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
	
	public void applyMarkupsFromDatabaseToCase(Case nuixCase, boolean addToExistingMarkupSet, AnnotationMatchingMethod matchingMethod) throws SQLException {
		abortWasRequested = false;
		Map<String,MarkupSet> markupSetLookup = new HashMap<String,MarkupSet>();
		for(MarkupSet existingMarkupSet : nuixCase.getMarkupSets()) {
			markupSetLookup.put(existingMarkupSet.getName(), existingMarkupSet);
		}
		
		List<Object> bindData = new ArrayList<Object>();
		
		for(Map.Entry<String, Long> markupEntry : markupSetIdLookup.entrySet()) {
			if(abortWasRequested) { break; }
			String markupSetName = markupEntry.getKey();
			long markupSetId = markupEntry.getValue();
			String markupSetDescription = executeStringScalar("SELECT Description FROM MarkupSet WHERE ID = ?",markupSetId);
			String markupSetRedactionReason = executeStringScalar("SELECT RedactionReason FROM MarkupSet WHERE ID = ?",markupSetId);
			
			logMessage("Applying markups to case from MarkupSet: %s",markupSetName);
			if(matchingMethod == AnnotationMatchingMethod.GUID) {
				logMessage("Matching DB entries to case items using: GUID");
			} else if(matchingMethod == AnnotationMatchingMethod.MD5) {
				logMessage("Matching DB entries to case items using: MD5");
			}
			
			// We need to resolve the MarkupSet object, either by obtaining existing one in case or creating new one
			MarkupSet markupSet = null;
			if(markupSetLookup.containsKey(markupSetName)) {
				if(addToExistingMarkupSet) {
					logMessage("Applying markups in destination case to existing markup set: %s",markupSetName);
					markupSet = markupSetLookup.get(markupSetName);
				} else {
					// When addToExisting is false and we have a name collision, we will attempt to find a usable name
					int nameSequence = 2;
					String targetName = markupSetName+"_"+nameSequence;
					while(markupSetLookup.containsKey(targetName)) {
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
			
			// Now that we have a MarkupSet, we need to get ItemMarkup records from DB
			String itemMarkupSql = "SELECT i.GUID,i.MD5,im.PageNumber,im.IsRedaction,im.X,im.Y,im.Width,im.Height FROM ItemMarkup AS im " + 
					"INNER JOIN Item AS i ON im.Item_ID = i.ID " + 
					"WHERE im.MarkupSet_ID = ? " + 
					"ORDER BY MD5,GUID,PageNumber";
			String itemMarkupTotalCountSql = "SELECT COUNT(*) FROM ItemMarkup AS im " + 
					"INNER JOIN Item AS i ON im.Item_ID = i.ID " + 
					"WHERE im.MarkupSet_ID = ? " + 
					"ORDER BY MD5,GUID,PageNumber";
			
			bindData.clear();
			bindData.add(markupSetId);
			
			int totalItemMarkups = executeLongScalar(itemMarkupTotalCountSql,bindData).intValue();
			LoadingCache<String,Set<Item>> itemCache = CacheBuilder.newBuilder()
					.maximumSize(1000)
					.build(new CacheLoader<String,Set<Item>>(){

						@Override
						public Set<Item> load(String guidOrMd5) throws Exception {
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
			
			executeQuery(itemMarkupSql,bindData,new Consumer<ResultSet>() {
				int currentIndex = 1;
				@Override
				public void accept(ResultSet rs) {
					try {
						while(rs.next()) {
							if(abortWasRequested) { break; }
							fireProgressUpdated(currentIndex,totalItemMarkups);
							String guid = FormatUtility.bytesToHex(rs.getBytes(1));
							String md5 = FormatUtility.bytesToHex(rs.getBytes(2));
							long pageNumber = rs.getLong(3);
							boolean isRedaction = rs.getBoolean(4);
							double x = rs.getDouble(5);
							double y = rs.getDouble(6);
							double width = rs.getDouble(7);
							double height = rs.getDouble(8);
							
							Set<Item> items = null;
							
							if(matchingMethod == AnnotationMatchingMethod.GUID) {
								items = itemCache.get(guid);
							} else if(matchingMethod == AnnotationMatchingMethod.MD5) {
								items = itemCache.get(md5);
							}
														
							for(Item item : items) {
								MutablePrintedImage itemImage = item.getPrintedImage();
								List<? extends PrintedPage> pages = itemImage.getPages();
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
	
	public long getItemMarkupCount() throws SQLException {
		return executeLongScalar("SELECT COUNT(*) FROM ItemMarkup");
	}
	
	public void abort() {
		logMessage("Signalling abort...");
		abortWasRequested = true;
	}
}
