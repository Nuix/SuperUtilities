package com.nuix.superutilities.annotations;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

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
		List<MarkupSet> markupSets = nuixCase.getMarkupSets();
		for(MarkupSet markupSet : markupSets) {
			storeMarkupSet(nuixCase, markupSet);
		}
	}
	
	public void storeMarkupSet(Case nuixCase, MarkupSet markupSet) throws IOException, SQLException {
		String insertItemMarkup = "INSERT INTO ItemMarkup (Item_ID,MarkupSet_ID,PageNumber,IsRedaction,X,Y,Width,Height) VALUES (?,?,?,?,?,?,?,?)";
		String itemQuery = QueryHelper.markupSetQuery(markupSet);

		long markupSetId = getMarkupSetId(markupSet);
		Set<Item> markupSetItems = nuixCase.searchUnsorted(itemQuery);
		for(Item item : markupSetItems) {
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
		}
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
			long id = executeScalar("SELECT ID FROM Item WHERE GUID = ?", guidBytes);
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
			long id = executeScalar("SELECT ID FROM MarkupSet WHERE Name = ?", name);
			markupSetIdLookup.put(name, id);
			return id;
		}
	}
}
