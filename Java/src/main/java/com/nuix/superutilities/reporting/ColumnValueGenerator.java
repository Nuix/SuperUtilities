package com.nuix.superutilities.reporting;

import nuix.Case;

/***
 * Base class for reporting.  You should not use this class directly, but instead use classes
 * derived from it.
 * @author Jason Wells
 *
 */
public class ColumnValueGenerator {
	protected String label = "Value";
	protected String columnLabel = null;
	public Object generateValue(Case nuixCase, String query) { return "Not Implemented"; }
	
	/***
	 * Gets the label associated to this instance.
	 * @return The associated label
	 */
	public String getLabel() {
		return label;
	}
	
	public String getColumnLabel() {
		if(columnLabel == null) { return label; }
		else { return columnLabel; }
	}
	
	/***
	 * Sets the label associated to this instance
	 * @param label The label to associate
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	public void setColumnLabel(String categoryLabel) {
		this.columnLabel = categoryLabel;
	}
}
