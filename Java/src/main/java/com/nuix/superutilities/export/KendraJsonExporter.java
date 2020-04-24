package com.nuix.superutilities.export;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
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
import nuix.DigestCollection;
import nuix.Item;
import nuix.SourceItem;
import nuix.WorkerItem;

public class KendraJsonExporter {
	private boolean includeText = false;
	private boolean includeProperties = true;
	private boolean includeCommunication = true;
	private boolean serializeNulls = true;
	private boolean prettyPrint = true;
	
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
	
	public Map<String,Object> mapWorkerItem(WorkerItem workerItem){
		Map<String,Object> result = new LinkedHashMap<String,Object>();
		
		result.putAll(mapNuixProperties(workerItem));
		if(includeCommunication) {
			result.putAll(mapCommunication(workerItem));	
		}
		if(includeProperties) {
			result.putAll(mapProperties(workerItem,result.keySet()));
		}
		
		return result;
	}
	
	public Map<String,Object> mapNuixProperties(WorkerItem workerItem){
		SourceItem sourceItem = workerItem.getSourceItem();
		Map<String,Object> result = new LinkedHashMap<String,Object>();
		
		result.put("guid",workerItem.getItemGuid());
		
		if(includePathGuids) {
			result.put("path_guids", workerItem.getGuidPath());	
		}
		
		result.put("name",sourceItem.getLocalisedName());
		String path = String.join("\\",sourceItem.getLocalisedPathNames());
		result.put("path", path);
		
		// These weren't available until 7.4
		if(SuperUtilities.getCurrentVersion().isAtLeast("7.4")) {
			DateTime itemDate = sourceItem.getDate();
			if(itemDate != null){
				result.put("item_date", sourceItem.getDate().toString());	
			} else {
				result.put("item_date", null);
			}
		
			result.put("top_level",sourceItem.isTopLevel());
		}
		
		// These weren't available until 7.6
		if(SuperUtilities.getCurrentVersion().isAtLeast("7.6")) {
			DigestCollection digests = sourceItem.getDigests();
			result.put("md5",digests.getMd5());
			result.put("sha1",digests.getSha1());
			result.put("sha256",digests.getSha256());
		}
		
		return result;
	}
	
	public Map<String,Object> mapCommunication(WorkerItem workerItem){
		Map<String,Object> result = new LinkedHashMap<String,Object>();
		
		SourceItem sourceItem = workerItem.getSourceItem();
		
		Communication comm = sourceItem.getCommunication();
		
		if(comm == null) {
			result.put("comm_date",null);
			result.put("from",null);
			result.put("to",null);
			result.put("cc",null);
			result.put("bcc",null);
		} else {
			DateTime commDate = comm.getDateTime();
			if (commDate == null) {
				result.put("comm_date",null);	
			} else {
				result.put("comm_date",commDate.toString());
			}
			result.put("from",comm.getFrom().stream().map(a -> a.toRfc822String()).collect(Collectors.toList()));
			result.put("to",comm.getTo().stream().map(a -> a.toRfc822String()).collect(Collectors.toList()));
			result.put("cc",comm.getCc().stream().map(a -> a.toRfc822String()).collect(Collectors.toList()));
			result.put("bcc",comm.getBcc().stream().map(a -> a.toRfc822String()).collect(Collectors.toList()));
		}
		
		return result;
	}
	
	public Map<String,Object> mapProperties(WorkerItem workerItem, Set<String> alreadyPresentKeys){
		Map<String,Object> result = new LinkedHashMap<String,Object>();
		SourceItem sourceItem = workerItem.getSourceItem();
		Map<String,Object> properties = sourceItem.getProperties();
		for(Map.Entry<String,Object> property : properties.entrySet()) {
			String originalKey = property.getKey().toLowerCase().trim(); 
			String key = originalKey;
			Object value = property.getValue();
			
			//Check for existing key and suffix key as needed
			int suffix = 2;
			while(result.containsKey(key) || alreadyPresentKeys.contains(key)) {
				key = originalKey+suffix;
				suffix++;
			}
			
			// First we check if the value is of a value type that JSON natively supports
			if(value instanceof Boolean || value instanceof String || value instanceof Integer
					|| value instanceof Long || value instanceof Float) {
				result.put(key,value);
			} else if (value instanceof DateTime) {
				DateTime dateTimeValue = (DateTime)value;
				result.put(key,dateTimeValue.toString());
			} else if (value instanceof Duration) {
				Duration durationValue = (Duration)value;
				result.put(key,durationValue.getMillis());
			} else if (value instanceof byte[]) {
				byte[] byteArrayValue = (byte[])value;
				result.put(key,FormatUtility.bytesToHex(byteArrayValue));
			} else {
				// Anything else that comes through we will put in and hope for the best
				result.put(key,value);
			}
		}
		return result;
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
}
