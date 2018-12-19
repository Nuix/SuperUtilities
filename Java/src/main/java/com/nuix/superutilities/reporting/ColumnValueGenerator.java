package com.nuix.superutilities.reporting;

import nuix.Case;

public class ColumnValueGenerator {
	protected String label = "Value";
	public Object generateValue(Case nuixCase, String query) { return "Not Implemented"; }
	
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
}
