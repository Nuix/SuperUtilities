package com.nuix.superutilities.regex;

/***
 * Represents information about a regular expression match.
 * @author Jason Wells
 *
 */
public class RegexMatch {
	private PatternInfo patternInfo = null;
	private String location = null;
	private boolean isContentMatch = false;
	private String value = null;
	private String valueContext = null;
	private int matchStart = 0;
	private int matchEnd = 0;
	
	/***
	 * Creates a new instance
	 * @param patternInfo The pattern which made the match
	 * @param location Where the match was made (content or metadata property name)
	 * @param isContentMatch True if the match was made on item content text
	 * @param value The matched value
	 * @param valueContext Contextual text around match
	 * @param matchStart Offset in source text where match starts
	 * @param matchEnd Offset in source text where match ends
	 */
	public RegexMatch(PatternInfo patternInfo, String location, boolean isContentMatch, String value, String valueContext, int matchStart, int matchEnd){
		this.patternInfo = patternInfo;
		this.location = location;
		this.isContentMatch = isContentMatch;
		this.value = value;
		this.valueContext = valueContext;
		this.matchStart = matchStart;
		this.matchEnd = matchEnd;
	}
	
	/***
	 * Convenience method for obtaining the regular expression string from the associated {@link PatternInfo} object.
	 * @return The regular expression string from the associated {@link PatternInfo} object
	 */
	public String getExpression(){
		return patternInfo.getExpression();
	}

	/***
	 * Gets the associated {@link PatternInfo} object which made the match.
	 * @return The associated {@link PatternInfo} object which made the match
	 */
	public PatternInfo getPatternInfo() {
		return patternInfo;
	}

	/***
	 * Gets the location of match such as content or metdata property name.
	 * @return The location of match such as content or metdata property name
	 */
	public String getLocation() {
		return location;
	}
	
	/***
	 * Gets whether this match was made in the content text of an item.
	 * @return True if this match was made in the content text of an item.
	 */
	public boolean isContentMatch(){
		return isContentMatch;
	}

	/***
	 * Gets the matched value
	 * @return The matched value
	 */
	public String getValue() {
		return value;
	}

	/***
	 * Gets the offset in source text where match starts
	 * @return The offset in source text where match starts
	 */
	public long getMatchStart() {
		return matchStart;
	}

	/***
	 * Gets the offset in source text where match ends
	 * @return The offset in source text where match ends
	 */
	public long getMatchEnd() {
		return matchEnd;
	}

	/***
	 * Gets contextual string of match if one was provided
	 * @return The context string of match if one was provided
	 */
	public String getValueContext() {
		return valueContext;
	}
}
