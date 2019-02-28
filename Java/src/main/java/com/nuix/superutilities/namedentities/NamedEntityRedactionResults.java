package com.nuix.superutilities.namedentities;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public class NamedEntityRedactionResults {
	private int updatedItemCount = 0;
	private int contentTextUpdatedCount = 0;
	private Map<String,Integer> updatedProperties = new HashMap<String,Integer>();
	
	/***
	 * Adds 1 to updated item count, as obtained by calling {@link #getUpdatedItemCount()}.
	 */
	public void tallyUpdatedItem() {
		updatedItemCount++;
	}
	
	/***
	 * Adds 1 to count of items which had their content text redacted, as obtained by calling {@link #getContentTextUpdatedCount()}.
	 */
	public void tallyContentTextUdpated() {
		contentTextUpdatedCount++;
	}

	/***
	 * Records that a particular property was recorded and adds 1 to count of times that property was updated.  Count of updated properties
	 * can later be obtained by calling {@link #getUpdatedProperties()}.  If you are just looking for list of poperty names which were updated
	 * you can call {@link #getUpdatedPropertyNames()}.
	 * @param propertyName The property name to increment the tally for
	 */
	public void tallyUpdatedProperty(String propertyName) {
		if(!updatedProperties.containsKey(propertyName)) {
			updatedProperties.put(propertyName, 1);
		} else {
			updatedProperties.put(propertyName, updatedProperties.get(propertyName) + 1);
		}
	}

	/***
	 * Gets the number of items which had at least one redaction recorded to a custom metadata field.
	 * @return The number of items which had at least one redaction recorded to a custom metadata field.
	 */
	public int getUpdatedItemCount() {
		return updatedItemCount;
	}
	
	/***
	 * Gets a map of properties by name and how many times each property had a redacted custom metadata field recorded for it. 
	 * @return A map of properties by name and how many times each property had a redacted custom metadata field recorded for it.
	 */
	public Map<String, Integer> getUpdatedProperties() {
		return updatedProperties;
	}
	
	/***
	 * Gets a listing of metadata properties that had a redacted custom metadat field recorded for them at least once.
	 * @return A listing of metadata properties that had a redacted custom metadat field recorded for them at least once.
	 */
	public Set<String> getUpdatedPropertyNames(){
		return updatedProperties.keySet();
	}
	
	/***
	 * Gets a count of items which had a redacted custom metadata field recorded for the item's content text.
	 * @return A count of items which had a redacted custom metadata field recorded for the item's content text.
	 */
	public int getContentTextUpdatedCount() {
		return contentTextUpdatedCount;
	}

	/***
	 * Combines counts from another instance into this instance.  Used by {@link com.nuix.superutilities.namedentities.NamedEntityUtility#recordRedactedCopies(java.util.Collection, NamedEntityRedactionSettings)}
	 * to coalesce results generated from processing individual items into a final overall result.
	 * @param other The other NamedEntityRedactionResults instance to merge into this instance
	 */
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

	/***
	 * Generates a string with a handy summary of the results represented by this instance.
	 */
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
