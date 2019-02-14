package com.nuix.superutilities.namedentities;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import com.nuix.superutilities.misc.FormatUtility;
import com.nuix.superutilities.misc.PlaceholderResolver;
import com.nuix.superutilities.query.QueryHelper;

import nuix.Case;
import nuix.Item;
import nuix.ItemCustomMetadataMap;

public class NamedEntityUtility {
	private static Logger logger = Logger.getLogger(NamedEntityUtility.class);
	
	private NamedEntityRedactionProgressCallback progressCallback = null;
	
	private void fireProgress(int current, int total) {
		if(progressCallback != null) {
			progressCallback.progressUpdated(current, total);
		}
	}
	
	public void whenProgressUpdated(NamedEntityRedactionProgressCallback callback) {
		progressCallback = callback;
	}
	
	private Consumer<String> messageCallback = null;
	
	private void fireMessage(String message) {
		if(messageCallback != null) {
			messageCallback.accept(message);
		} else {
			logger.info(message);
		}
	}
	
	public void whenMessageGenerated(Consumer<String> callback) {
		messageCallback = callback;
	}
	
	/***
	 * Creates redacted copies of metadata properties and item content text by performing find and replace operations by finding
	 * named entity matches in the values and replacing them with redaction text.
	 * @param item The item to process
	 * @param settings The settings to use while processing the item.
	 * @return Results object which contains some information about how things went.
	 * @throws Exception Thrown is something unexpectedly goes wrong.
	 */
	public NamedEntityRedactionResults recordRedactedCopies(Item item, NamedEntityRedactionSettings settings) throws Exception {
		NamedEntityRedactionResults itemResult = new NamedEntityRedactionResults();
		
		ItemCustomMetadataMap customMetadata = item.getCustomMetadata();
		Map<String,Object> properties = item.getProperties();
		
		// We will start off by iterating each entity name.  Using that name, we get any
		// named entity matches on this item for that entity.  We then take the match value
		// and convert it to an escaped regex Pattern for quick find replace on value text.
		// We then store all the Patterns for a given entity name grouped by the entity name.
		Map<String,List<Pattern>> entityPatterns = new HashMap<String,List<Pattern>>(); 
		for(String entityName : settings.getEntityNames()) {
			List<Pattern> patterns = new ArrayList<Pattern>();
			Set<String> entityMatches = item.getEntities(entityName);
			for(String entityMatch : entityMatches) {
				String matchEscaped = Pattern.quote(entityMatch);
				patterns.add(Pattern.compile(matchEscaped));
			}
			entityPatterns.put(entityName, patterns);
		}
		
		PlaceholderResolver pr = new PlaceholderResolver();
		boolean itemWasUpdated = false;
		
		Set<String> specificProperties = settings.getSpecificProperties();
		boolean hasSpecificPropertiesList = specificProperties != null && specificProperties.size() > 0;
		
		// Process properties
		if(settings.getRedactProperties()) {
			// Iterate each property
			for(Map.Entry<String, Object> prop : properties.entrySet()) {
				
				// If settings provide a list of only specific properties to process, we check to
				// see if this property is on that list and skip it if it is not on that list
				if(hasSpecificPropertiesList) {
					if(!specificProperties.contains(prop.getKey())) {
						continue;
					}
				}
				
				// Get property value as a String
				String originalStringValue = FormatUtility.getInstance().convertToString(prop.getValue());
				String redactedStringValue = originalStringValue;
				// Calculate custom metadata field name for when we need it
				String customMetadataField = String.format("%s%s", settings.getCustomMetadataFieldPrefix(), prop.getKey());
				
				// Iterate each entity name grouped collection of patterns
				for(Map.Entry<String, List<Pattern>> patternGroup : entityPatterns.entrySet()) {
					// Resolve redaction template for the entity name we are working on
					pr.set("entity_name", patternGroup.getKey().toUpperCase());
					String redactionText = pr.resolveTemplate(settings.getRedactionReplacementTemplate());
					
					// Iterate each associated pattern, replacing any matches with our redaction text
					for(Pattern entityMatchPattern : patternGroup.getValue()) {
						redactedStringValue = entityMatchPattern.matcher(redactedStringValue).replaceAll(redactionText);
					}
				}
				
				// Each group of patterns has had its chance to perform replacements, now we record the modified value as
				// custom metadata depending on whether the text was actually modified and whether we want to only record
				// modified values.
				boolean propertyWasModified = originalStringValue.contentEquals(redactedStringValue) == false; 
				
				if(settings.getOnlyRecordChanges()) {
					if(propertyWasModified) {
						customMetadata.put(customMetadataField, redactedStringValue);
						itemWasUpdated = true;
					}
				} else {
					customMetadata.put(customMetadataField, redactedStringValue);
					itemWasUpdated = true;
				}
				
				if(propertyWasModified) {
					itemResult.tallyUpdatedProperty(prop.getKey());
				}
			}
		}
		
		// Process item's content text
		if(settings.getRedactContentText()) {
			String originalStringValue = item.getTextObject().toString();
			String redactedStringValue = originalStringValue;
			// Calculate custom metadata field name for when we need it
			String customMetadataField = String.format("%sContentText", settings.getCustomMetadataFieldPrefix());
			
			// Iterate each entity name grouped collection of patterns
			for(Map.Entry<String, List<Pattern>> patternGroup : entityPatterns.entrySet()) {
				// Resolve redaction template for the entity name we are working on
				pr.set("entity_name", patternGroup.getKey().toUpperCase());
				String redactionText = pr.resolveTemplate(settings.getRedactionReplacementTemplate());
				
				// Iterate each associated pattern, replacing any matches with our redaction text
				for(Pattern entityMatchPattern : patternGroup.getValue()) {
					redactedStringValue = entityMatchPattern.matcher(redactedStringValue).replaceAll(redactionText);
				}
			}
			
			// Each group of patterns has had its chance to perform replacements, now we record the modified value as
			// custom metadata depending on whether the text was actually modified and whether we want to only record
			// modified values.
			boolean contentTextWasModified = originalStringValue.contentEquals(redactedStringValue) == false;
			
			if(settings.getOnlyRecordChanges()) {
				if(contentTextWasModified) {
					customMetadata.put(customMetadataField, redactedStringValue);
					itemWasUpdated = true;
				}
			} else {
				customMetadata.put(customMetadataField, redactedStringValue);
				itemWasUpdated = true;
			}
			
			if(contentTextWasModified) { itemResult.tallyContentTextUdpated(); }
		}
		
		// Record time we performed our find and replace
		if(settings.getRecordTimeOfRedaction() && itemWasUpdated) {
			customMetadata.put(settings.getTimeOfRedactionFieldName(), DateTime.now());
		}
		
		if(itemWasUpdated) {
			itemResult.tallyUpdatedItem();
		}
		
		return itemResult;
	}
	
	/***
	 * Creates redacted copies of metadata properties and item content text by performing find and replace operations by finding
	 * named entity matches in the values and replacing them with redaction text.  This method processes multiple items by repeated calls
	 * to {@link #recordRedactedCopies(Item, NamedEntityRedactionSettings)}.
	 * @param items The items to process
	 * @param settings The settings to use when processing the items
	 * @return Results object which contains some information about how things went.  Is a combination of the results generated for each individual item.
	 */
	public NamedEntityRedactionResults recordRedactedCopies(Collection<Item> items, NamedEntityRedactionSettings settings) {
		NamedEntityRedactionResults overallResults = new NamedEntityRedactionResults();
		
		fireMessage(String.format("Items provided: %s",items.size()));
		fireMessage(String.format("Using named entities: %s", String.join("; ",settings.getEntityNames())));
		
		fireMessage("Beginning textual redaction...");
		int index = 0;
		for(Item item : items) {
			index++;
			fireProgress(index, items.size());
			try {
				NamedEntityRedactionResults itemResult = recordRedactedCopies(item,settings);
				overallResults.mergeOther(itemResult);
			} catch (Exception e) {
				String message = String.format("Error while generating redaction copies for item %s - '%s'", item.getGuid(), item.getLocalisedName());
				logger.error(message,e);
				fireMessage(message);
			}
		}
		fireMessage("Completed textual redaction");
		
		return overallResults;
	}
	
	/***
	 * Creates redacted copies of metadata properties and item content text by performing find and replace operations by finding
	 * named entity matches in the values and replacing them with redaction text.  This method locates items in the specified case
	 * based on the list of named entities specified in the {@link NamedEntityRedactionSettings} object provided.  Once those items
	 * are ontained, this method calls {@link #recordRedactedCopies(Collection, NamedEntityRedactionSettings)}.
	 * @param nuixCase The Nuix case from which items will be obtained.
	 * @param settings The settings used to process the obtained items.
	 * @return Results object which contains some information about how things went.  Is a combination of the results generated for each individual item.
	 * @throws Exception Thrown if something goes wrong running the search to obtain the items to process.
	 */
	public NamedEntityRedactionResults recordRedactedCopies(Case nuixCase, NamedEntityRedactionSettings settings) throws Exception {
		String query = QueryHelper.namedEntityQuery(settings.getEntityNames());
		fireMessage("Locating items with named entities using: "+query);
		Set<Item> items = nuixCase.searchUnsorted(query);
		return recordRedactedCopies(items,settings);
	}
	
	/***
	 * Builds and saves to a file a Nuix metadata profile containing fields 'Name' and 'Position' as well as a series of derived fields that use
	 * "first-non-blank" to coalesce a redacted field and the based original field.
	 * @param destination Where the profile should be saved to.
	 * @param redactionResults Results from a redaction run, used to determine which redacted fields need to be in the profile.
	 * @param settings Settings used in a redaction run, used mostly to determine prefix used for custom metadata fields.
	 * @throws Exception Thrown if something goes wrong unexpectedly.
	 */
	public static void saveRedactionProfile(File destination, NamedEntityRedactionResults redactionResults, NamedEntityRedactionSettings settings) throws Exception {
		logger.info("Saving redaction profile to: "+destination.getAbsolutePath());
		// Very crude code to generate profile for viewing redaction
		// Should probably make this proper XML generation at some point
		StringJoiner contents = new StringJoiner("\n");
		
		contents.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		contents.add("<metadata-profile xmlns=\"http://nuix.com/fbi/metadata-profile\">");
		contents.add("  <metadata-list>");
		contents.add("    <metadata type=\"SPECIAL\" name=\"Position\" default-column-width=\"101\" />");
		contents.add("    <metadata type=\"SPECIAL\" name=\"Name\" default-column-width=\"191\" />");

		for(String propertyName : redactionResults.getUpdatedPropertyNames()) {
			contents.add(String.format("    <metadata type=\"DERIVED\" name=\"%s\" default-column-width=\"198\">", propertyName));
			contents.add("      <first-non-blank>");
			String customFieldName = settings.getCustomMetadataFieldPrefix() + propertyName;
			contents.add(String.format("        <metadata type=\"CUSTOM\" name=\"%s\" />", customFieldName));
			contents.add(String.format("        <metadata type=\"PROPERTY\" name=\"%s\" />", propertyName));
			contents.add("      </first-non-blank>");
			contents.add("    </metadata>");
		}
		
		contents.add("  </metadata-list>");
		contents.add("</metadata-profile>");
		
		String profileContentsString = contents.toString();
		
		FileWriter fw = null;
		PrintWriter pw = null;
		try{
			fw = new FileWriter(destination);
			pw = new PrintWriter(fw);
			pw.print(profileContentsString);
		}catch(Exception exc){
			throw exc;
		}
		finally{
			try {
				fw.close();
			} catch (IOException e) {}
			pw.close();
		}
	}
	
	/***
	 * Builds and saves to a file a Nuix metadata profile containing fields 'Name' and 'Position' as well as a series of derived fields that use
	 * "first-non-blank" to coalesce a redacted field and the based original field.
	 * @param destination Where the profile should be saved to.
	 * @param redactionResults Results from a redaction run, used to determine which redacted fields need to be in the profile.
	 * @param settings Settings used in a redaction run, used mostly to determine prefix used for custom metadata fields.
	 * @throws Exception Thrown if something goes wrong unexpectedly.
	 */
	public static void saveRedactionProfile(String destination, NamedEntityRedactionResults redactionResults, NamedEntityRedactionSettings settings) throws Exception {
		saveRedactionProfile(new File(destination), redactionResults, settings);
	}
}
