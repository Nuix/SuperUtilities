package com.nuix.superutilities.annotations;

import java.util.StringJoiner;

public class AnnotationHistoryRepositorySummary {
	long distinctItemsReferences = 0;
	
	long totalTagEvents = 0;
	long totalCustomMetadataEvents = 0;
	long totalItemSetEvents = 0;
	long totalExclusionEvents = 0;
	
	@Override
	public String toString() {
		
		StringJoiner sj = new StringJoiner("\n");
		sj.add(String.format("Distinct Items Referenced: %s", distinctItemsReferences));
		sj.add(String.format("Total Tag Events: %s", totalTagEvents));
		sj.add(String.format("Total Custom Metadata Events: %s", totalCustomMetadataEvents));
		sj.add(String.format("Total Item Set Events: %s", totalItemSetEvents));
		sj.add(String.format("Total Exclusion Events: %s", totalExclusionEvents));
		return sj.toString();
	}
}
