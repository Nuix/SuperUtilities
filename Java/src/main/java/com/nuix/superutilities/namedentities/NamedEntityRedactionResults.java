package com.nuix.superutilities.namedentities;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public class NamedEntityRedactionResults {
	private int updatedItemCount = 0;
	private int contentTextUpdatedCount = 0;
	private Map<String,Integer> updatedProperties = new HashMap<String,Integer>();
	
	public void tallyUpdatedItem() {
		updatedItemCount++;
	}
	
	public void tallyContentTextUdpated() {
		contentTextUpdatedCount++;
	}

	public void tallyUpdatedProperty(String propertyName) {
		if(!updatedProperties.containsKey(propertyName)) {
			updatedProperties.put(propertyName, 1);
		} else {
			updatedProperties.put(propertyName, updatedProperties.get(propertyName) + 1);
		}
	}

	public int getUpdatedItemCount() {
		return updatedItemCount;
	}
	
	public Map<String, Integer> getUpdatedProperties() {
		return updatedProperties;
	}
	
	public Set<String> getUpdatedPropertyNames(){
		return updatedProperties.keySet();
	}
	
	public void mergeOther(NamedEntityRedactionResults other) {
		updatedItemCount += other.updatedItemCount;
		contentTextUpdatedCount += other.contentTextUpdatedCount;
		for(Map.Entry<String,Integer> otherEntry : other.getUpdatedProperties().entrySet()) {
			String otherPropertyName = otherEntry.getKey();
			int otherCount = otherEntry.getValue();
			if(!updatedProperties.containsKey(otherPropertyName)) {
				updatedProperties.put(otherPropertyName, otherCount);
			} else {
				updatedProperties.put(otherPropertyName, updatedProperties.get(otherPropertyName) + otherCount);
			}
		}
	}

	@Override
	public String toString() {
		StringJoiner result = new StringJoiner("\n");
		result.add(String.format("Updated Item Count: %s", updatedItemCount));
		result.add(String.format("Content Text Updated Count: %s", contentTextUpdatedCount));
		result.add("Updated Properties:");
		for(Map.Entry<String, Integer> entry : updatedProperties.entrySet()) {
			result.add(String.format("  %s => %s", entry.getKey(), entry.getValue()));
		}
		return result.toString();
	}
}
