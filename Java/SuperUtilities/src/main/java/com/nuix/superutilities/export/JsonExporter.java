package com.nuix.superutilities.export;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nuix.superutilities.SuperUtilities;
import com.nuix.superutilities.misc.FormatUtility;

import nuix.Address;
import nuix.Communication;
import nuix.Item;
import nuix.ItemType;
import nuix.SourceItem;
import nuix.WorkerItem;

public class JsonExporter {
	//Cache serialized types so we don't re map them over and over
	protected static Map<ItemType,Map<String,String>> itemTypeMapCache = new HashMap<ItemType,Map<String,String>>();
	//Store property names encountered so we can report this later
	protected static Map<String,Integer> encounteredPropertyNameCounts = new HashMap<String,Integer>();
	
	//Define property data type category names
	protected static String booleanCategory = "BooleanProperties";
	protected static String integerCategory = "IntegerProperties";
	protected static String longCategory = "LongProperties";
	protected static String floatCategory = "FloatProperties";
	protected static String stringCategory = "StringProperties";
	protected static String dateTimeCategory = "DateTimeProperties";
	protected static String durationCategory = "DurationProperties";
	protected static String binaryCategory = "BinaryProperties";
	protected static String otherCategory = "OtherProperties";
	
	private boolean includeText = true;
	private boolean includeProperties = true;
	private boolean includeCustomMetadata = true;
	private boolean includeTags = true;
	private boolean serializeNulls = true;
	private boolean prettyPrint = true;
	
	private boolean includeParentGuid = true;
	private boolean includeChildGuids = true;
	private boolean includePathGuids = true;
	
	private Consumer<Map<String,Object>> beforeSerializationCallback = null;
	
	/***
	 * Allows you to provide a callback which can inspect or modify the nested series of Maps before they are serialized by GSON to JSON
	 * by {@link #exportItemAsJson(Item, File)} or {@link #convertItemToJsonString(Item)}.
	 * @param callback A callback which may inspect or modify the nested Map form of an item before it is serialized to JSON
	 */
	public void beforeSerialization(Consumer<Map<String,Object>> callback) {
		beforeSerializationCallback = callback;
	}
	
	/***
	 * Gets whether item content text will be included in generated JSON
	 * @return True if content text will be included
	 */
	public boolean getIncludeText() {
		return includeText;
	}

	/***
	 * Sets whether item content text will be included in generated JSON
	 * @param includeText True if content text should be included
	 */
	public void setIncludeText(boolean includeText) {
		this.includeText = includeText;
	}

	/***
	 * Gets whether item metadata properties will be included in generated JSON
	 * @return True if item metadata properties will be included in generated JSON
	 */
	public boolean getIncludeProperties() {
		return includeProperties;
	}

	/***
	 * Sets whether item metadata properties will be included in generated JSON
	 * @param includeProperties True if item metadata properties should be included in generated JSON
	 */
	public void setIncludeProperties(boolean includeProperties) {
		this.includeProperties = includeProperties;
	}

	/***
	 * Gets whether item custom metadata will be included in generated JSON
	 * @return True if item custom metadata will be included in generated JSON
	 */
	public boolean getIncludeCustomMetadata() {
		return includeCustomMetadata;
	}

	/***
	 * Sets whether item custom metadata will be included in generated JSON
	 * @param includeCustomMetadata True if item custom metadata should be included in JSON
	 */
	public void setIncludeCustomMetadata(boolean includeCustomMetadata) {
		this.includeCustomMetadata = includeCustomMetadata;
	}

	/***
	 * Gets whether items' tags will be included in generated JSON
	 * @return True if items' tags will be included in generated JSON
	 */
	public boolean getIncludeTags() {
		return includeTags;
	}

	/***
	 * Sets whether items' tags will be included in generated JSON
	 * @param includeTags True if items' tags should be included in generated JSON
	 */
	public void setIncludeTags(boolean includeTags) {
		this.includeTags = includeTags;
	}
	
	/***
	 * Gets whether parent GUID should be serialized when calling {@link #mapItem(Item)}, {{@link #exportItemAsJson(Item, File)} or {{@link #convertItemToJsonString(Item)}.
	 * @return True if parent GUID will be serialized.
	 */
	public boolean getIncludeParentGuid() {
		return includeParentGuid;
	}

	/***
	 * Sets whether parent GUID should be serialized when calling {@link #mapItem(Item)}, {{@link #exportItemAsJson(Item, File)} or {{@link #convertItemToJsonString(Item)}.
	 * @param includeParentGuid True if parent GUID should be serialized.
	 */
	public void setIncludeParentGuid(boolean includeParentGuid) {
		this.includeParentGuid = includeParentGuid;
	}

	/***
	 * Gets whether child GUIDs should be serialized when calling {@link #mapItem(Item)}, {{@link #exportItemAsJson(Item, File)} or {{@link #convertItemToJsonString(Item)}.
	 * @return True if child GUIDs will be serialized
	 */
	public boolean getIncludeChildGuids() {
		return includeChildGuids;
	}

	/***
	 * Sets whether child GUIDs should be serialized when calling {@link #mapItem(Item)}, {{@link #exportItemAsJson(Item, File)} or {{@link #convertItemToJsonString(Item)}.
	 * @param includeChildGuids True if child GUIDs should be serialized
	 */
	public void setIncludeChildGuids(boolean includeChildGuids) {
		this.includeChildGuids = includeChildGuids;
	}

	/***
	 * Gets whether path GUIDs should be serialized when calling {@link #mapItem(Item)}, {{@link #exportItemAsJson(Item, File)} or {{@link #convertItemToJsonString(Item)}.
	 * @return True if path GUIDs will be serialized
	 */
	public boolean getIncludePathGuids() {
		return includePathGuids;
	}

	/***
	 * Sets whether path GUIDs should be serialized when calling {@link #mapItem(Item)}, {{@link #exportItemAsJson(Item, File)} or {{@link #convertItemToJsonString(Item)}.
	 * @param includePathGuids True if path GUIDs should be serialized
	 */
	public void setIncludePathGuids(boolean includePathGuids) {
		this.includePathGuids = includePathGuids;
	}

	/***
	 * Gets whether entries with a null value are included in the generated JSON
	 * @return True if entries with a null value will be included in generated JSON
	 */
	public boolean getSerializeNulls() {
		return serializeNulls;
	}

	/***
	 * Sets whether entries with null value are included in the generated JSON
	 * @param serializeNulls True if entries with null value should be included in generated JSON
	 */
	public void setSerializeNulls(boolean serializeNulls) {
		this.serializeNulls = serializeNulls;
	}

	/***
	 * Gets whether JSON will be "pretty printed".  Makes file more human readable, but can also make
	 * the file larger with all the extra white space characters for formatting.
	 * @return True if JSON will be "pretty printed"
	 */
	public boolean getPrettyPrint() {
		return prettyPrint;
	}

	/***
	 * Sets whether JSON will be "pretty printed".  Makes file more human readable, but can also make
	 * the file larger with all the extra white space characters for formatting.
	 * @param prettyPrint True if the output should be "pretty printed"
	 */
	public void setPrettyPrint(boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
	}
	
	/***
	 * This method take a provided WorkerItem and generates a series of nested {@link java.util.Map} instances
	 * representing the WorkerItem and its data.  The resulting {@link java.util.Map} is later used as input
	 * to GSON for serializing to JSON.
	 * 
	 * Internally this gets the SourceItem for the given WorkerItem and provides that to {@link #mapSourceItem(SourceItem)} to
	 * build most of the Map.  After that it adds a few additional entries based on what is available from the WorkerItem.
	 * @param workerItem WorkerItem to serialize into a map
	 * @return Map representing the WorkerItem and its SourceItem
	 */
	public Map<String,Object> mapWorkerItem(WorkerItem workerItem){
		Map<String,Object> itemMap = new LinkedHashMap<String,Object>();
		// We can use the source item to get at a majority of the data
		SourceItem sourceItem = workerItem.getSourceItem();
		itemMap.putAll(mapSourceItem(sourceItem));
		// Now we just add a few additional things we can since we have the worker item
		itemMap.put("GUID", workerItem.getItemGuid());
		if(includePathGuids) {
			itemMap.put("PathGUIDs", workerItem.getGuidPath());	
		}
		return itemMap;
	}

	/***
	 * This method takes a provided SourceItem and generates a series of nested {@link java.util.Map} instances
	 * representing the SourceItem and its data.  The resulting {@link java.util.Map} is later used as input
	 * to GSON for serializing to JSON.
	 * 
	 * In turn the following methods are called by this one: {@link #mapItemType(ItemType)}, {@link #mapProperties(Map)},
	 *  {@link #mapCommunication(Communication)}
	 * @param sourceItem Source item to serialize into a Map
	 * @return Map representation of the source item
	 */
	public Map<String,Object> mapSourceItem(SourceItem sourceItem) {
		Map<String,Object> itemMap = new LinkedHashMap<String,Object>();

		//Values from SourceItem
		itemMap.put("ItemType",mapItemType(sourceItem.getType()));
		itemMap.put("Name",sourceItem.getLocalisedName());
		itemMap.put("FileSize",sourceItem.getFileSize());
		itemMap.put("IsDeleted",sourceItem.isDeleted());
		itemMap.put("IsEncrypted",sourceItem.isEncrypted());
		itemMap.put("IsFileData",sourceItem.isFileData());
		itemMap.put("Communication",mapCommunication(sourceItem.getCommunication()));
		itemMap.put("PathNames", sourceItem.getPathNames());
		
		if(SuperUtilities.getCurrentVersion().isAtLeast("7.4")) {
			DateTime itemDate = sourceItem.getDate();
			if(itemDate != null){
				itemMap.put("ItemDate", sourceItem.getDate().toString());	
			} else {
				itemMap.put("ItemDate", null);
			}
		
			itemMap.put("IsTopLevel",sourceItem.isTopLevel());
		}
		
		if(SuperUtilities.getCurrentVersion().isAtLeast("7.6")) {
			itemMap.put("MD5", sourceItem.getDigests().getMd5());
			itemMap.put("SHA-1", sourceItem.getDigests().getSha1());
			itemMap.put("SHA-256", sourceItem.getDigests().getSha256());
			
			itemMap.put("ShannonEntropy", sourceItem.getShannonEntropy());
		}
		
		if(includeProperties){
			itemMap.put("Properties",mapProperties(sourceItem.getProperties()));
		}
		
		if(includeText){
			nuix.Text textObject = sourceItem.getText();
			if(textObject != null){
				try{
					itemMap.put("Text",textObject.toString());
				}
				catch(Exception exc){
					itemMap.put("Text",null);
					itemMap.put("TextError",exc.getMessage());
				}
			} else {
				itemMap.put("Text",null);
			}
		}
		
		return itemMap;
	}
	
	/***
	 * Converts a Nuix item into a series of nested Map objects, which later can be easily serialized
	 * into JSON.
	 * @param item The item to serialize into a Map
	 * @return Map representation of the item
	 */
	public Map<String,Object> mapItem(Item item){
		Map<String,Object> itemMap = new LinkedHashMap<String,Object>();

		itemMap.put("ItemType",mapItemType(item.getType()));
		itemMap.put("GUID", item.getGuid());
		itemMap.put("PathNames", item.getPathNames());
		
		if(includeParentGuid) {
			Item parent = item.getParent();
			if(parent != null){ itemMap.put("ParentGUID", parent.getGuid()); }
			else { itemMap.put("ParentGUID", null); }	
		}
		
		if(includePathGuids) {
			itemMap.put("PathGUIDs", item.getPath().stream().map(i -> i.getGuid()).collect(Collectors.toList()));	
		}
		
		if(includeChildGuids) {
			itemMap.put("ChildGUIDs", item.getChildren().stream().map(i -> i.getGuid()).collect(Collectors.toList()));	
		}
		
		itemMap.put("Name",item.getLocalisedName());
		
		itemMap.put("MD5", item.getDigests().getMd5());
		itemMap.put("SHA-1", item.getDigests().getSha1());
		itemMap.put("SHA-256", item.getDigests().getSha256());
		
		itemMap.put("AuditedSize",item.getAuditedSize());
		itemMap.put("FileSize",item.getFileSize());
		itemMap.put("Communication",mapCommunication(item.getCommunication()));
		itemMap.put("Comment", item.getComment());
		itemMap.put("Custodian", item.getCustodian());
		
		itemMap.put("CorrectedExtension", item.getCorrectedExtension());
		itemMap.put("OriginalExtension", item.getOriginalExtension());
		
		DateTime itemDate = item.getDate();
		if(itemDate != null){
			itemMap.put("ItemDate", item.getDate().toString());	
		} else {
			itemMap.put("ItemDate", null);
		}
		
		itemMap.put("Language", item.getLanguage());
		
		itemMap.put("IsAudited", item.isAudited());
		itemMap.put("IsExcluded", item.isExcluded());
		itemMap.put("IsLooseFile", item.isLooseFile());
		itemMap.put("IsPhysicalFile", item.isPhysicalFile());
		itemMap.put("IsTopLevel", item.isTopLevel());
		itemMap.put("IsDeleted",item.isDeleted());
		itemMap.put("IsEncrypted",item.isEncrypted());
		itemMap.put("IsFileData",item.isFileData());
		
		if(includeTags){
			itemMap.put("Tags", item.getTags());
		}
		
		if(includeProperties){
			itemMap.put("Properties",mapProperties(item.getProperties()));
		}
		
		if(includeCustomMetadata){
			itemMap.put("CustomMetadata",mapProperties(item.getCustomMetadata()));
		}
		
		if(includeText){
			nuix.Text textObject = item.getTextObject();
			if(textObject != null){
				try{
					itemMap.put("Text",textObject.toString());
				}
				catch(Exception exc){
					itemMap.put("Text",null);
					itemMap.put("TextError",exc.getMessage());
				}
			} else {
				itemMap.put("Text",null);
			}
		}
		
		return itemMap;
	}
	
	/***
	 * Exports an item as a JSON representation.  Items information is first converted into a series of Map objects
	 * by calling {@link JsonExporter#mapItem(Item)}, the result of which is then converted into JSON.
	 * @param item The item to serialize into JSON
	 * @param exportFilePath File path to write the JSON result to.
	 */
	public void exportItemAsJson(Item item, File exportFilePath){
		//Configure JSON instance based on settings
		Gson gson = null;
		GsonBuilder gsonBuilder = new GsonBuilder();
		if(serializeNulls) gsonBuilder.serializeNulls();
		if(prettyPrint) gsonBuilder.setPrettyPrinting();
		gson = gsonBuilder.create();
		
		FileOutputStream fos = null;
		BufferedWriter bw = null;
		try {
			fos = new FileOutputStream(exportFilePath);
			bw = new BufferedWriter(new OutputStreamWriter(fos));
			Map<String,Object> mappedData = mapItem(item);
			if(beforeSerializationCallback != null) { beforeSerializationCallback.accept(mappedData); }
			String asJson = gson.toJson(mappedData);
			bw.write(asJson);
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/***
	 * Exports an source item as a JSON representation.  The SourceItem's information is first converted into a series of Map objects
	 * by calling {@link JsonExporter#mapSourceItem(SourceItem)}, the result of which is then converted into JSON.
	 * @param sourceItem The SourceItem to serialize into JSON
	 * @param exportFilePath File path to write the JSON result to.
	 */
	public void exportSourceItemAsJson(SourceItem sourceItem, File exportFilePath){
		//Configure JSON instance based on settings
		Gson gson = null;
		GsonBuilder gsonBuilder = new GsonBuilder();
		if(serializeNulls) gsonBuilder.serializeNulls();
		if(prettyPrint) gsonBuilder.setPrettyPrinting();
		gson = gsonBuilder.create();
		
		FileOutputStream fos = null;
		BufferedWriter bw = null;
		try {
			fos = new FileOutputStream(exportFilePath);
			bw = new BufferedWriter(new OutputStreamWriter(fos));
			Map<String,Object> mappedData = mapSourceItem(sourceItem);
			if(beforeSerializationCallback != null) { beforeSerializationCallback.accept(mappedData); }
			String asJson = gson.toJson(mappedData);
			bw.write(asJson);
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/***
	 * Exports a worker item as a JSON representation.  The WorkerItem's information is first converted into a series of Map objects
	 * by calling {@link JsonExporter#mapWorkerItem(WorkerItem)}, the result of which is then converted into JSON.
	 * @param workerItem The WorkerItem to serialize into JSON
	 * @param exportFilePath File path to write the JSON result to.
	 */
	public void exportWorkerItemAsJson(WorkerItem workerItem, File exportFilePath){
		//Configure JSON instance based on settings
		Gson gson = null;
		GsonBuilder gsonBuilder = new GsonBuilder();
		if(serializeNulls) gsonBuilder.serializeNulls();
		if(prettyPrint) gsonBuilder.setPrettyPrinting();
		gson = gsonBuilder.create();
		
		FileOutputStream fos = null;
		BufferedWriter bw = null;
		try {
			fos = new FileOutputStream(exportFilePath);
			bw = new BufferedWriter(new OutputStreamWriter(fos));
			Map<String,Object> mappedData = mapWorkerItem(workerItem);
			if(beforeSerializationCallback != null) { beforeSerializationCallback.accept(mappedData); }
			String asJson = gson.toJson(mappedData);
			bw.write(asJson);
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/***
	 * Converts an item to a JSON String representation.  The Item's information is first converted into a series of Map objects
	 * by calling {@link JsonExporter#mapItem(Item)}, result of which is then serialized into a JSON String and returned.
	 * @param item The item to serialize into JSON
	 * @return A JSON String representation of the given item.
	 */
	public String convertItemToJsonString(Item item) {
		//Configure JSON instance based on settings
		Gson gson = null;
		GsonBuilder gsonBuilder = new GsonBuilder();
		if(serializeNulls) gsonBuilder.serializeNulls();
		if(prettyPrint) gsonBuilder.setPrettyPrinting();
		gson = gsonBuilder.create();
		Map<String,Object> mappedData = mapItem(item);
		if(beforeSerializationCallback != null) { beforeSerializationCallback.accept(mappedData); }
		String asJson = gson.toJson(mappedData);
		return asJson;
	}
	
	/***
	 * Converts a SourceItem to a JSON String representation.  The SourceItem's information is first converted into a series of Map objects
	 * by calling {@link #mapSourceItem(SourceItem)}, result of which is then serialized into a JSON String and returned.
	 * @param sourceItem The SourceItem to serialize into JSON
	 * @return A JSON String representation of the given SourceItem.
	 */
	public String convertSourceItemToJsonString(SourceItem sourceItem) {
		//Configure JSON instance based on settings
		Gson gson = null;
		GsonBuilder gsonBuilder = new GsonBuilder();
		if(serializeNulls) gsonBuilder.serializeNulls();
		if(prettyPrint) gsonBuilder.setPrettyPrinting();
		gson = gsonBuilder.create();
		Map<String,Object> mappedData = mapSourceItem(sourceItem);
		if(beforeSerializationCallback != null) { beforeSerializationCallback.accept(mappedData); }
		String asJson = gson.toJson(mappedData);
		return asJson;
	}
	
	/***
	 * Converts a WorkerItem to a JSON String representation.  The WorkerItem's information is first converted into a series of Map objects
	 * by calling {@link #mapWorkerItem(WorkerItem)}, result of which is then serialized into a JSON String and returned.
	 * @param workerItem The WorkerItem to serialize into JSON
	 * @return A JSON String representation of the given WorkerItem.
	 */
	public String convertWorkerItemToJsonString(WorkerItem workerItem) {
		//Configure JSON instance based on settings
		Gson gson = null;
		GsonBuilder gsonBuilder = new GsonBuilder();
		if(serializeNulls) gsonBuilder.serializeNulls();
		if(prettyPrint) gsonBuilder.setPrettyPrinting();
		gson = gsonBuilder.create();
		Map<String,Object> mappedData = mapWorkerItem(workerItem);
		if(beforeSerializationCallback != null) { beforeSerializationCallback.accept(mappedData); }
		String asJson = gson.toJson(mappedData);
		return asJson;
	}
	
	/***
	 * This method generates a {@link java.util.Map} of the properties for an item, organizing properties into
	 * sub-categories based on the data type of their values.
	 * @param properties A Nuix item properties map to convert
	 * @return A nested series of {@link java.util.Map} instances with properties categorized by value data type
	 */
	@SuppressWarnings("unchecked")
	public static Map<String,Object> mapProperties(Map<String,Object> properties) {
		//Sub categorize properties by data type
		Map<String,Object> propertiesMap = new LinkedHashMap<String,Object>();
		for(Map.Entry<String,Object> property : properties.entrySet()){
			String propertyName = property.getKey();
			Object propertyValue = property.getValue();
			
			//Boolean
			if(propertyValue instanceof Boolean){
				propertiesMap.putIfAbsent(booleanCategory, new HashMap<String,Boolean>());
				((Map<String, Boolean>) propertiesMap.get(booleanCategory)).put(propertyName,(Boolean) propertyValue);
			}
			//Integer
			else if(propertyValue instanceof Integer){
				propertiesMap.putIfAbsent(integerCategory, new HashMap<String,Integer>());
				((Map<String, Integer>) propertiesMap.get(integerCategory)).put(propertyName,(Integer) propertyValue);
			}
			//Long int
			else if(propertyValue instanceof Long){
				propertiesMap.putIfAbsent(longCategory, new HashMap<String,Long>());
				((Map<String, Long>) propertiesMap.get(longCategory)).put(propertyName,(Long) propertyValue);
			}
			//Float
			else if(propertyValue instanceof Float){
				propertiesMap.putIfAbsent(floatCategory, new HashMap<String,Float>());
				((Map<String, Float>) propertiesMap.get(floatCategory)).put(propertyName,(Float) propertyValue);
			}
			//String
			else if(propertyValue instanceof String){
				propertiesMap.putIfAbsent(stringCategory, new HashMap<String,String>());
				((Map<String, String>) propertiesMap.get(stringCategory)).put(propertyName,(String) propertyValue);
			}
			//DateTime
			else if(propertyValue instanceof DateTime){
				propertiesMap.putIfAbsent(dateTimeCategory, new HashMap<String,String>());
				((Map<String, String>) propertiesMap.get(dateTimeCategory)).put(propertyName,((DateTime)propertyValue).toString());
			}
			//Duration
			else if(propertyValue instanceof Duration){
				propertiesMap.putIfAbsent(durationCategory, new HashMap<String,Long>());
				((Map<String, Long>) propertiesMap.get(durationCategory)).put(propertyName,((Duration)propertyValue).getMillis());
			}
			//Byte array
			else if(propertyValue instanceof byte[]){
				propertiesMap.putIfAbsent(binaryCategory, new HashMap<String,String>());
				((Map<String, String>) propertiesMap.get(binaryCategory)).put(propertyName,FormatUtility.bytesToHex(((byte[])propertyValue)));
			}
			//Anything else
			else {
				propertiesMap.putIfAbsent(otherCategory, new HashMap<String,Object>());
				((Map<String, Object>) propertiesMap.get(otherCategory)).put(propertyName,propertyValue);
			}
		}
		return propertiesMap;
	}
	
	/***
	 * Converts a Nuix ItemType object to a HashMap&lt;String,String&gt; for JSON serialization.
	 * This method first checks a local cache to see if the value has already been converted and
	 * returns that if so.
	 * @param itemType The Nuix ItemType object to Map-ify
	 * @return The resulting Map
	 */
	public static Map<String,String> mapItemType(ItemType itemType) {
		Map<String,String> result = itemTypeMapCache.get(itemType);
		if(result == null) {
			//Cache didn't have this so we need to generate it
			result = new HashMap<String,String>();
			result.put("Kind",itemType.getKind().getName());
			result.put("Name",itemType.getLocalisedName());
			result.put("MimeType",itemType.getName());
		}
		return result;
	}
	
	/***
	 * Converts a Nuix Communication object to a HashMap&lt;String,Object&gt; for JSON serialization.  This method in turn
	 * calls {@link #mapAddresses(List)}.
	 * @param comm The Nuix Communication object to Map-ify
	 * @return The resulting Map
	 */
	public static Map<String,Object> mapCommunication(Communication comm){
		if(comm == null){
			return null;
		}else{
			Map<String,Object> result = new LinkedHashMap<String,Object>();
			result.put("DateTime",comm.getDateTime().toString());
			result.put("From",mapAddresses(comm.getFrom()));
			result.put("To",mapAddresses(comm.getTo()));
			result.put("CC",mapAddresses(comm.getCc()));
			result.put("BCC",mapAddresses(comm.getBcc()));
			return result;
		}
	}
	
	/***
	 * Converts a Nuix Address object to a HashMap&lt;String,Object&gt; for JSON serialization.
	 * @param address The Nuix Address object to Map-ify
	 * @return The resulting Map
	 */
	public static Map<String,Object> mapAddress(Address address){
		if(address == null){
			return null;
		}else{
			Map<String,Object> result = new LinkedHashMap<String,Object>();
			result.put("Type",address.getType());
			result.put("Address",address.getAddress());
			result.put("Personal",address.getPersonal());
			result.put("DisplayString",address.toDisplayString());
			result.put("RFC822String",address.toRfc822String());
			return result;
		}
	}
	
	/***
	 * Converts a List of Nuix Address objects to a list of HashMap&lt;String,Object&gt; for JSON serialization.  This method
	 * calls {@link #mapAddress(Address)} for each Address object provided.
	 * @param addresses The list of Nuix Address objects to Mapify
	 * @return The resulting Map
	 */
	public static List<Map<String,Object>> mapAddresses(List<Address> addresses){
		List<Map<String,Object>> result = new ArrayList<Map<String,Object>>();
		for (Address address : addresses) {
			result.add(mapAddress(address));
		}
		return result;
	}
}
