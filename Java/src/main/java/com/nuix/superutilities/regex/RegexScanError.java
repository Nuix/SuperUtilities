package com.nuix.superutilities.regex;

import nuix.Item;

/***
 * Represents information about an error which occurred in {@link RegexScanner} while scanning.
 * @author Jason Wells
 *
 */
public class RegexScanError {
	private Item item = null;
	private PatternInfo patternInfo = null;
	private String location = null;
	private Exception exception = null;
	
	/***
	 * Creates a new instance
	 * @param item The item being scanned during the error
	 * @param patternInfo The pattern in use when the error occurred
	 * @param location The location being scanned when the error occured (content or metadata property name)
	 * @param exception The exception which was thrown
	 */
	public RegexScanError(Item item, PatternInfo patternInfo, String location, Exception exception){
		this.item = item;
		this.patternInfo = patternInfo;
		this.location = location;
		this.exception = exception;
	}

	/***
	 * Gets the associated item
	 * @return The associated item
	 */
	public Item getItem() {
		return item;
	}

	/***
	 * Gets the associated pattern info
	 * @return The associated pattern info
	 */
	public PatternInfo getPatternInfo() {
		return patternInfo;
	}

	/***
	 * Gets the associated location
	 * @return The associated location
	 */
	public String getLocation() {
		return location;
	}

	/***
	 * Gets the associated exception
	 * @return The associated exception
	 */
	public Exception getException() {
		return exception;
	}
}
