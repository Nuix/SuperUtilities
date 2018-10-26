package com.nuix.superutilities.annotations;

import java.util.StringJoiner;

public class AnnotationSyncSettings {
	private boolean syncCustomMetadataEvents = true;
	private boolean syncTagEvents = true;
	private boolean syncItemSetEvents = true;
	private boolean syncExclusionEvents = true;
	private boolean syncCustodianEvents = true;
	private boolean syncProductionSetEvents = true;
	
	public boolean getSyncCustomMetadataEvents() {
		return syncCustomMetadataEvents;
	}
	
	public void setSyncCustomMetadataEvents(boolean syncCustomMetadataEvents) {
		this.syncCustomMetadataEvents = syncCustomMetadataEvents;
	}
	
	public boolean getSyncTagEvents() {
		return syncTagEvents;
	}
	
	public void setSyncTagEvents(boolean syncTagEvents) {
		this.syncTagEvents = syncTagEvents;
	}
	
	public boolean getSyncItemSetEvents() {
		return syncItemSetEvents;
	}
	
	public void setSyncItemSetEvents(boolean syncItemSetEvents) {
		this.syncItemSetEvents = syncItemSetEvents;
	}
	
	public boolean getSyncExclusionEvents() {
		return syncExclusionEvents;
	}
	
	public void setSyncExclusionEvents(boolean syncExclusionEvents) {
		this.syncExclusionEvents = syncExclusionEvents;
	}
	
	public boolean getSyncCustodianEvents() {
		return syncCustodianEvents;
	}
	
	public void setSyncCustodianEvents(boolean syncCustodianEvents) {
		this.syncCustodianEvents = syncCustodianEvents;
	}
	
	public boolean getSyncProductionSetEvents() {
		return syncProductionSetEvents;
	}
	
	public void setSyncProductionSetEvents(boolean syncProductionSetEvents) {
		this.syncProductionSetEvents = syncProductionSetEvents;
	}
	
	public String buildSettingsSummary(){
		StringJoiner result = new StringJoiner("\n");
		result.add(String.format("Sync Custom Metadata: %s", syncCustomMetadataEvents));
		result.add(String.format("Sync Custodians: %s", syncCustodianEvents));
		result.add(String.format("Sync Exclusions: %s", syncExclusionEvents));
		result.add(String.format("Sync Item Sets: %s", syncItemSetEvents));
		result.add(String.format("Sync Production Sets: %s", syncProductionSetEvents));
		result.add(String.format("Sync Tags: %s", syncTagEvents));
		return result.toString();
	}
}
