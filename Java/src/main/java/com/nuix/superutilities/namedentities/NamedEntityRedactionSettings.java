package com.nuix.superutilities.namedentities;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/***
 * Provides settings for performing named entity text redaction performed by {@link com.nuix.superutilities.namedentities.NamedEntityUtility}, specifically in the methods
 * {@link com.nuix.superutilities.namedentities.NamedEntityUtility#recordRedactedCopies(nuix.Item, NamedEntityRedactionSettings)},
 * {@link com.nuix.superutilities.namedentities.NamedEntityUtility#recordRedactedCopies(Collection, NamedEntityRedactionSettings)} and 
 * {@link com.nuix.superutilities.namedentities.NamedEntityUtility#recordRedactedCopies(nuix.Case, NamedEntityRedactionSettings)}.
 * @author Jason Wells
 *
 */
public class NamedEntityRedactionSettings {
	private Set<String> entityNames = new HashSet<String>();
	private boolean redactProperties = true;
	private boolean redactContentText = true;
	private boolean onlyRecordChanges = true;
	private String customMetadataFieldPrefix = "R_";
	private String redactionReplacementTemplate = "[REDACTED {entity_name}]";
	private boolean recordTimeOfRedaction = true;
	private String timeOfRedactionFieldName = "TextualRedactionUpdated";
	private Set<String> specificProperties = new HashSet<String>();
	
	public NamedEntityRedactionSettings() {
		
	}
	
	/***
	 * Adds the names of all the built-in entities as of Nuix 7.8:
	 * company, country, credit-card-num, email, ip-address, money, person, personal-id-num, url and phone-number 
	 */
	public void addAllBuiltInEntities() {
		// Based on 7.8
		entityNames.add("company");
		entityNames.add("country");
		entityNames.add("credit-card-num");
		entityNames.add("email");
		entityNames.add("ip-address");
		entityNames.add("money");
		entityNames.add("person");
		entityNames.add("personal-id-num");
		entityNames.add("url");
		entityNames.add("phone-number");
	}
	
	/***
	 * Gets the entity names currently associated with this instance
	 * @return The entity names currently associated with this instance
	 */
	public Set<String> getEntityNames() {
		return entityNames;
	}

	/***
	 * Adds entity names to this instance.
	 * @param entityNames The entity names to add
	 */
	public void addEntityNames(Collection<String> entityNames) {
		this.entityNames.addAll(entityNames);
	}
	
	/***
	 * Clears all entity names associated with this instance
	 */
	public void clearEntityNames() {
		this.entityNames.clear();
	}

	/***
	 * Gets value determining whether metadata properties should be processed.
	 * @return True if metadata properties should be processed
	 */
	public boolean getRedactProperties() {
		return redactProperties;
	}

	/***
	 * Sets value determining whether metadata properties should be processed
	 * @param redactProperties Whether metadata properties should be processed
	 */
	public void setRedactProperties(boolean redactProperties) {
		this.redactProperties = redactProperties;
	}

	/***
	 * Gets value determining whether item's content text should be processed
	 * @return True if item's content text should be processed
	 */
	public boolean getRedactContentText() {
		return redactContentText;
	}

	/***
	 * Sets value determining whether items' content text should be processed
	 * @param redactContentText Whether items' content text should be processed
	 */
	public void setRedactContentText(boolean redactContentText) {
		this.redactContentText = redactContentText;
	}

	/***
	 * Gets value determining whether only values that have a redaction performed should be recorded.
	 * @return True when only updated fields are recorded, false when all fields are recorded
	 */
	public boolean getOnlyRecordChanges() {
		return onlyRecordChanges;
	}

	/***
	 * Sets value determining whether only values that have a redaction performed should be recorded
	 * @param onlyRecordChanges True when only updated fields are recorded, false when all fields are recorded
	 */
	public void setOnlyRecordChanges(boolean onlyRecordChanges) {
		this.onlyRecordChanges = onlyRecordChanges;
	}

	/***
	 * Gets the prefix applied to custom metadata fields names.  Custom metadata field names are source property field name prepended with
	 * this prefix or "ContentText" (for item content text) prepended with this prefix.
	 * @return The prefix which will be appended to custom metadata field names.  Can be an empty string.
	 */
	public String getCustomMetadataFieldPrefix() {
		return customMetadataFieldPrefix;
	}

	/***
	 * Sets the prefix applied to custom metadata fields names.  Custom metadata field names are source property field name prepended with
	 * this prefix or "ContentText" (for item content text) prepended with this prefix.
	 * @param customMetadataFieldPrefix
	 */
	public void setCustomMetadataFieldPrefix(String customMetadataFieldPrefix) {
		this.customMetadataFieldPrefix = customMetadataFieldPrefix;
	}

	/***
	 * Gets the template string used to generate the replacement text put in place of a redaction in the text.
	 * @return The template string used to generate replacement text put in place of a redaction in the text
	 */
	public String getRedactionReplacementTemplate() {
		return redactionReplacementTemplate;
	}

	/***
	 * Sets the template string used to generate replacement text put in place of a redaction in the text.  The placeholder
	 * {entity_name} will be replaced at run-time with the name of the entity if present in the template.
	 * @param redactionReplacementTemplate The template string used to generate replacement text put in place of a redaction in the text
	 */
	public void setRedactionReplacementTemplate(String redactionReplacementTemplate) {
		this.redactionReplacementTemplate = redactionReplacementTemplate;
	}

	/***
	 * Gets whether a custom metadata field will be written to updated items with a time stamp of when the redaction was performed.
	 * The name of the custom metadata field used can be set using {@link #setTimeOfRedactionFieldName(String)}.
	 * @return True if time stamp will be written to updated items
	 */
	public boolean getRecordTimeOfRedaction() {
		return recordTimeOfRedaction;
	}

	/***
	 * Sets whether a custom metadata field will be written to updated items with a time stamp of when the redaction was performed.
	 * The name of the custom metadata field used can be set using {@link #setTimeOfRedactionFieldName(String)}.
	 * @param recordTimeOfRedaction
	 */
	public void setRecordTimeOfRedaction(boolean recordTimeOfRedaction) {
		this.recordTimeOfRedaction = recordTimeOfRedaction;
	}

	/***
	 * Gets the name of the custom metadata field used to record redaction time stamp when {@link #getRecordTimeOfRedaction()} returns true.
	 * @return The name of the custom metadata field used to record redaction time stamp
	 */
	public String getTimeOfRedactionFieldName() {
		return timeOfRedactionFieldName;
	}

	/***
	 * Sets the name of the custom metadata field used to record redaction time stamp when {@link #getRecordTimeOfRedaction()} returns true.
	 * @param timeOfRedactionFieldName The name of the custom metadata field used to record redaction time stamp
	 */
	public void setTimeOfRedactionFieldName(String timeOfRedactionFieldName) {
		this.timeOfRedactionFieldName = timeOfRedactionFieldName;
	}

	/***
	 * Gets list of specific metadata property names to be scanned.  If null or has no entries, all metadata properties are scanned.  If this contains
	 * a list of metadata property names, only those metadata properties will be scanned.
	 * @return Set of specific metadata properties to be scanned or a null or empty collection if all properties are to be scanned.
	 */
	public Set<String> getSpecificProperties() {
		return specificProperties;
	}

	/***
	 * Sets list of specific metadata property names to be scanned.  If null or has no entries, all metadata properties are scanned.  If this contains
	 * a list of metadata property names, only those metadata properties will be scanned.
	 * @param specificProperties Set of specific metadata properties to be scanned or a null or empty collection means all properties are to be scanned.
	 */
	public void setSpecificProperties(Collection<String> specificProperties) {
		this.specificProperties.clear();
		this.specificProperties.addAll(specificProperties);
	}
	
}
