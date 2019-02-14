package com.nuix.superutilities.namedentities;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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
	
	public Set<String> getEntityNames() {
		return entityNames;
	}

	public void setEntityNames(Collection<String> entityNames) {
		this.entityNames.addAll(entityNames);
	}
	
	public void clearEntityName() {
		this.entityNames.clear();
	}

	public boolean getRedactProperties() {
		return redactProperties;
	}

	public void setRedactProperties(boolean redactProperties) {
		this.redactProperties = redactProperties;
	}

	public boolean getRedactContentText() {
		return redactContentText;
	}

	public void setRedactContentText(boolean redactContentText) {
		this.redactContentText = redactContentText;
	}

	public boolean getOnlyRecordChanges() {
		return onlyRecordChanges;
	}

	public void setOnlyRecordChanges(boolean onlyRecordChanges) {
		this.onlyRecordChanges = onlyRecordChanges;
	}

	public String getCustomMetadataFieldPrefix() {
		return customMetadataFieldPrefix;
	}

	public void setCustomMetadataFieldPrefix(String customMetadataFieldPrefix) {
		this.customMetadataFieldPrefix = customMetadataFieldPrefix;
	}

	public String getRedactionReplacementTemplate() {
		return redactionReplacementTemplate;
	}

	public void setRedactionReplacementTemplate(String redactionReplacementTemplate) {
		this.redactionReplacementTemplate = redactionReplacementTemplate;
	}

	public boolean getRecordTimeOfRedaction() {
		return recordTimeOfRedaction;
	}

	public void setRecordTimeOfRedaction(boolean recordTimeOfRedaction) {
		this.recordTimeOfRedaction = recordTimeOfRedaction;
	}

	public String getTimeOfRedactionFieldName() {
		return timeOfRedactionFieldName;
	}

	public void setTimeOfRedactionFieldName(String timeOfRedactionFieldName) {
		this.timeOfRedactionFieldName = timeOfRedactionFieldName;
	}

	public Set<String> getSpecificProperties() {
		return specificProperties;
	}

	public void setSpecificProperties(Set<String> specificProperties) {
		this.specificProperties = specificProperties;
	}
	
}
