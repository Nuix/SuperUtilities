package com.nuix.superutilities.misc;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import nuix.Case;
import nuix.Item;
import nuix.ItemExpression;
import nuix.ItemSet;
import nuix.MetadataItem;
import nuix.MetadataProfile;
import nuix.ItemEventCallback;
import nuix.ItemEventInfo;

public class ProfileDigester {
	private static Logger logger = Logger.getLogger(ProfileDigester.class);
	
	private boolean includeItemText = false;
	private MetadataProfile profile = null;
	
	private BiConsumer<Integer,Integer> progressCallback = null;
	private Consumer<String> infoMessageCallback = null;
	private BiConsumer<String,Item> errorCallback = null;
	
	private MessageDigest md5Digester = null;
	
	private void fireProgressUpdate(int current, int total) {
		if(progressCallback != null) {
			progressCallback.accept(current, total);
		}
	}
	
	private void logInfo(String message) {
		if(infoMessageCallback != null) {
			infoMessageCallback.accept(message);
		} else {
			logger.info(message);
		}
	}
	
	private void logError(String message, Item item) {
		if(errorCallback != null) {
			errorCallback.accept(message, item);
		} else {
			logger.error(message);
		}
	}
	
	/***
	 * Invoked when progress is updated in {@link #addItemsToItemSet(Case, String, String, Collection)}.  Provides 2 integers, the first is current progress and the second is total progress.
	 * @param callback Callback to be invoked when progress is updated.
	 */
	public void whenProgressUpdated(BiConsumer<Integer,Integer> callback) {
		progressCallback = callback;
	}
	
	/***
	 * Invoked when a message is logged by {@link #addItemsToItemSet(Case, String, String, Collection)}.
	 * @param callback Callback invoked when a message is logged.  If callback is not provided, message is instead sent to Nuix log.
	 */
	public void whenMessageLogged(Consumer<String> callback) {
		infoMessageCallback = callback;
	}
	
	/***
	 * Invoked when an error occurs in {@link #addItemsToItemSet(Case, String, String, Collection)}.  Provides a message String and the item being processed when
	 * the error occurred.  If callback is not provided, message will instead be sent to Nuix log.
	 * @param callback
	 */
	public void whenErrorLogged(BiConsumer<String,Item> callback) {
		errorCallback = callback;
	}
	
	public ProfileDigester() {
		try {
			md5Digester = MessageDigest.getInstance("md5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
	public ProfileDigester(MetadataProfile metadataProfile) {
		this();
		profile = metadataProfile;
	}
	
	/***
	 * Adds items to an item set using "Scripted" deduplication, providing a MD5 hash generated from the concatenation of the values yielded
	 * by the MetadataProfile associated with this instance.  Additionally can include an items content text in the concatenated values.
	 * Effectively this allows you to create an item set where rather than deduplicating by the MD5 Nuix calculated for a given item during
	 * ingestion, the values of the given metadata profile are used to determine original/duplicate status of the provided items. 
	 * @param nuixCase The Nuix Case containing the items to be deduplicated and where the ItemSet will be created.
	 * @param itemSetName Name of item set.  If item set already exists, existing item set will be used, if not one will be created.  <b>Important:</b> when
	 * adding items to an existing item set, it is important that items previously added to that item set were added using this method, the same metadata profile
	 * and same setting for includeItemText, otherwise deduplication results are undefined.
	 * @param deduplicateBy Valid options are "INDIVIDUAL" and "FAMILY", these settings correspond to the behaviors noted in <a href="https://download.nuix.com/releases/desktop/stable/docs/en/scripting/api/nuix/ItemSet.html#addItems-java.util.Collection-java.util.Map-">addItems(Collection<Item> items, Map<?,?> options)</a>.
	 * @param items The items to add to the item set.
	 * @return The item set the items were added to.
	 */
	public ItemSet addItemsToItemSet(Case nuixCase, String itemSetName, String deduplicateBy, Collection<Item> items) {
		// Require we have a profile to work with
		if(profile == null) {
			throw new IllegalArgumentException("profile cannot be null, please provide a profile by calling setProfile(MetadataProfile profile) before calling this method");
		}
		
		// Require that an item set name was provided
		if(itemSetName == null || itemSetName.trim().isEmpty()) {
			throw new IllegalArgumentException("itemSetName cannot be null or empty");
		}
		
		// Require that a "deduplicateBy" value is provided which will be accepted by the API
		deduplicateBy = deduplicateBy.toUpperCase().trim();
		if(deduplicateBy.equalsIgnoreCase("FAMILY") == false && deduplicateBy.equalsIgnoreCase("INDIVIDUAL") == false) {
			throw new IllegalArgumentException("deduplicateBy can only be 'FAMILY' or 'INDIVIDUAL', was provided: "+deduplicateBy);
		}
		
		logInfo("Deduplicate By: "+deduplicateBy);
		String profileName = profile.getName();
		if(profileName == null || profileName.trim().isEmpty()) {
			profileName = "<NO NAME>";
		}
		logInfo("Using metadata profile "+profileName);
		
		// Is there an existing item set with this name?
		ItemSet targetItemSet = nuixCase.findItemSetByName(itemSetName);
		
		// If not, we shall create an item set with this name
		if(targetItemSet == null) {
			logInfo("Creating ItemSet with name "+itemSetName);
			String description = null;
			if(includeItemText) {
				description = String.format("Generated using MD5 of profile '%s' field values concatenation and Item Text", profile.getName());
			} else {
				description = String.format("Generated using MD5 of profile '%s' field values concatenation", profile.getName());
			}
			
			Map<String,Object> itemSetSettings = new HashMap<String,Object>();
			itemSetSettings.put("deduplication", "Scripted");
			itemSetSettings.put("description", description);
			itemSetSettings.put("deduplicateBy", deduplicateBy);
			targetItemSet = nuixCase.createItemSet(itemSetName, itemSetSettings);
		} else {
			logInfo("Using existing ItemSet with name "+itemSetName);
		}
		
		// Build settings Map for call to addItems which includes:
		// - Our custom expression which internally generates the custom MD5 for each item using provided metadata profile
		// - Progress callback which will in turn call fireProgressUpdate
		Map<String,Object> settings = new HashMap<String,Object>();
		
		// Define custom expression
		settings.put("expression", new ItemExpression<String>() {
			@Override
			public String evaluate(Item item) {
				try {
					return generateMd5String(item);
				} catch (Exception e) {
					String message = String.format("Error while generating custom MD5 for item with GUID %s and name %s", item.getGuid(), item.getLocalisedName());
					logError(message, item);
					return "ERROR";
				}
			}
		});
		
		// Define progress callback which will in turn push out progress updates to callback on this instance
		settings.put("progress", new ItemEventCallback() {
			@Override
			public void itemProcessed(ItemEventInfo info) {
				fireProgressUpdate((int)info.getStageCount(),items.size());
			}
		});
		
		// Add the items to the item set
		targetItemSet.addItems(items, settings);
		
		// Provide back item set we used/created
		return targetItemSet;
	}
	
	/***
	 * Generates MD5 digest byte array for a given item.  Digest is generated by digesting concatenation of values yielded by the
	 * metadata profile associated with this instance for the given item and optionally including the item's content text.
	 * Note that method is synchronized due to:
	 * - Reuse of MD5 digester
	 * - Some metadata profile fields don't seem to play nice when called concurrently
	 * @param item The item to generate a custom MD5 digest for.
	 * @return Byte array representation of the MD5 digest
	 * @throws Exception Most likely if metadata profile has not yet been set for this instance.
	 */
	public synchronized byte[] generateMd5Bytes(Item item) throws Exception {
		if(profile == null) {
			throw new IllegalArgumentException("profile cannot be null, please provide a profile by calling setProfile(MetadataProfile profile) before calling this method");
		}
		
		for(MetadataItem field : profile.getMetadata()) {
			String fieldValue = field.evaluate(item);
			if(fieldValue != null) {
				md5Digester.update(fieldValue.getBytes());
			}
		}
		
		if(includeItemText) {
			md5Digester.update(item.getTextObject().toString().getBytes(Charset.forName("utf8")));
		}
		
		// Capture our result and then cleanup for the next call
		byte[] result = md5Digester.digest();
		md5Digester.reset();
		
		return result;
	}
	
	/***
	 * Generates MD5 digest hex string for a given item.  Digest is generated by digesting values concatenation of values yielded by the
	 * metadata profile associated with this instance for the given item and optionally including the item's content text.
	 * Internally this method first calls {@link #generateMd5Bytes(Item)} then converts the result of that method
	 * into a hexadecimal string.
	 * @param item The item to generate a custom MD5 digest for.
	 * @return Hexadecimal string representation of the MD5 digest
	 * @throws Exception Most likely if metadata profile has not yet been set for this instance.
	 */
	public String generateMd5String(Item item) throws Exception {
		if(profile == null) {
			throw new IllegalArgumentException("profile cannot be null, please provide a profile by calling setProfile(MetadataProfile profile) before calling this method");
		}
		return FormatUtility.bytesToHex(generateMd5Bytes(item));
	}

	/***
	 * Gets whether this instance should include the item's content text when calculating a digest.
	 * @return Whether this instance should include the item's content text when calculating a digest.
	 */
	public boolean getIncludeItemText() {
		return includeItemText;
	}

	/***
	 * Sets whether this instance should include the item's content text when calculating a digest.
	 * @param includeItemText Whether this instance should include the item's content text when calculating a digest.
	 */
	public void setIncludeItemText(boolean includeItemText) {
		this.includeItemText = includeItemText;
	}

	/***
	 * Gets the metadata profile used to obtain the values provided to the hash computation.
	 * @return The metadata profile used to obtain the values provided to the hash computation.
	 */
	public MetadataProfile getProfile() {
		return profile;
	}

	/***
	 * Sets the metadata profile used to obtain the values provided to the hash computation.
	 * @param profile The metadata profile used to obtain the values provided to the hash computation.
	 */
	public void setProfile(MetadataProfile profile) {
		this.profile = profile;
	}	
}
