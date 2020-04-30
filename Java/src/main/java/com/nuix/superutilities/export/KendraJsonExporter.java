package com.nuix.superutilities.export;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.google.code.regexp.Pattern;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nuix.superutilities.SuperUtilities;
import com.nuix.superutilities.misc.FormatUtility;

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
	
	private boolean reportIsDeleted = false;
	private boolean reportIsEncrypted = false;
	private boolean reportIsFileData = false;
	
	private Consumer<Map<String,Object>> beforeSerializationCallback = null;
	
	private static DateTime fallbackDate = null;
	
	static {
		fallbackDate = new DateTime(1900,1,1,0,0,DateTimeZone.UTC);	
	}
	
	/***
	 * Allows you to provide a callback which can inspect or modify the nested series of Maps before they are serialized by GSON to JSON
	 * by {@link #exportItemAsJson(Item, File)} or {@link #convertItemToJsonString(Item)}.
	 * @param callback A callback which may inspect or modify the nested Map form of an item before it is serialized to JSON
	 */
	public void beforeSerialization(Consumer<Map<String,Object>> callback) {
		beforeSerializationCallback = callback;
	}
	
	public Map<String,Object> mapWorkerItem(WorkerItem workerItem){
		SourceItem sourceItem = workerItem.getSourceItem();
		Map<String,Object> result = new LinkedHashMap<String,Object>();
			
		result.putAll(mapNuixProperties(workerItem));
		
		if(includeCommunication) {
			result.putAll(mapCommunication(workerItem));	
		}
		
		if(includeProperties) {
			result.put("properties",mapProperties(workerItem));
		}
		
		if(includeText) {
			String itemContentText = sourceItem.getText().toString();
			result.put("content_text",itemContentText);
		}
		
		Map<String,Object> outerObject = new LinkedHashMap<String,Object>();
		outerObject.put("DocumentId",workerItem.getItemGuid().replace("-", ""));
		outerObject.put("Attributes",result);
		outerObject.put("AccessControlList",new String[] {}); //TODO Collect this from user?
		outerObject.put("Title",sourceItem.getLocalisedName());
		outerObject.put("ContentType","PLAIN_TEXT");
		return outerObject;
	}
	
	public Map<String,Object> mapNuixProperties(WorkerItem workerItem){
		SourceItem sourceItem = workerItem.getSourceItem();
		Map<String,Object> result = new LinkedHashMap<String,Object>();
		
		result.put("guid",workerItem.getItemGuid().replace("-", ""));
		
		if(includePathGuids) {
			result.put("path_guids", workerItem.getGuidPath());	
		}
		
		result.put("name",sourceItem.getLocalisedName());
		String path = String.join("/",sourceItem.getLocalisedPathNames());
		result.put("path", path);
		
		String pathKinds = String.join("/",sourceItem.getPath().stream().map(si -> si.getKind().getName()).collect(Collectors.toList()));
		result.put("path_kinds", pathKinds);
		
		result.put("item_date",sourceItem.getDate().toString());
		
//		Amazon Kendra has six reserved attributes that you can use. The attributes are:
//			_category (String)
//			_created_at (ISO 8601 encoded string)
//			_last_updated_at (ISO 8601 encoded string)
//			_source_uri (String)
//			_version (String)
//			_view_count (Long)
		
		result.put("_category",sourceItem.getKind().getName());
		
		Map<String,Object> properties = sourceItem.getProperties();
		
		if (properties.containsKey("File Modified")) {
			DateTime fileModified = (DateTime)properties.get("File Modified");
			fileModified = makeUTC(fileModified);
			if(fileModified != null) {
				result.put("_last_updated_at",fileModified.toString());		
			}
		}
		
		if (properties.containsKey("File Created")) {
			DateTime fileCreated = (DateTime)properties.get("File Created");
			fileCreated = makeUTC(fileCreated);
			if(fileCreated != null) {
				result.put("_created_at",fileCreated.toString());		
			}
		}
		
		// Currently not sure what would make sense to map these to so will leave them
		// here commented out for easy addition later if a mapping is decided upon.
//		result.put("_source_uri","NO_MAPPING_YET");
//		result.put("_version","NO_MAPPING_YET");
//		result.put("_view_count","NO_MAPPING_YET");
		
		result.put("mime_type", sourceItem.getType().getName());
		result.put("file_size",sourceItem.getFileSize());
		
		if (reportIsDeleted) { result.put("is_deleted",sourceItem.isDeleted()); }
		if (reportIsEncrypted) { result.put("is_encrypted",sourceItem.isEncrypted()); }
		if (reportIsFileData) { result.put("is_file_data",sourceItem.isFileData()); }
		
		// These weren't available until 7.4
		if(SuperUtilities.getCurrentVersion().isAtLeast("7.4")) {
			DateTime itemDate = sourceItem.getDate();
			if(itemDate != null){
				result.put("item_date",  makeUTC(itemDate).toString());	
			} else {
				result.put("item_date", fallbackDate.toString());
			}
		
			result.put("top_level",Boolean.toString(sourceItem.isTopLevel()));
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
			result.put("comm_date", fallbackDate.toString());
			result.put("from",new String[] {});
			result.put("recipients",new String[] {});
		} else {
			DateTime commDate = comm.getDateTime();
			if (commDate == null) {
				result.put("comm_date",fallbackDate.toString());	
			} else {
				result.put("comm_date",makeUTC(commDate).toString());
			}
			result.put("from",comm.getFrom().stream().map(a -> a.toRfc822String()).collect(Collectors.toList()));
			
			List<String> recipients = new ArrayList<String>();
			recipients.addAll(comm.getTo().stream().map(a -> a.toRfc822String()).collect(Collectors.toList()));
			recipients.addAll(comm.getCc().stream().map(a -> a.toRfc822String()).collect(Collectors.toList()));
			recipients.addAll(comm.getBcc().stream().map(a -> a.toRfc822String()).collect(Collectors.toList()));
			
			result.put("recipients",recipients);
		}
		
		return result;
	}
	
	protected static Pattern colonReplacement = Pattern.compile(":\\s+");
	protected static Pattern whitepsaceReplacement = Pattern.compile("\\s+");
	
	public List<String> mapProperties(WorkerItem workerItem){
		List<String> result = new ArrayList<String>();
		SourceItem sourceItem = workerItem.getSourceItem();
		Map<String,Object> properties = sourceItem.getProperties();
		
		for(Map.Entry<String,Object> property : properties.entrySet()) {
			String originalKey = property.getKey().toLowerCase().trim();
			originalKey = colonReplacement.matcher(originalKey).replaceAll("-");
			originalKey = whitepsaceReplacement.matcher(originalKey).replaceAll("_");
			String key = originalKey;
			Object value = property.getValue();
			
			if(value instanceof DateTime) {
				value = makeUTC((DateTime)value);
			}
			
			String objectStringValue = FormatUtility.getInstance().convertToString(value);
			
			result.add(String.format("%s: %s", key, objectStringValue));
		}
		
		Collections.sort(result);
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
	
	public static DateTime makeUTC(DateTime value) {
		return value.withZone(DateTimeZone.UTC);
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
