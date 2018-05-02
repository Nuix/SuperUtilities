package com.nuix.superutilities.regex;

import java.util.regex.Pattern;

/***
 * Represents a regular expression including associated title.
 * @author Jason Wells
 *
 */
public class PatternInfo {
	private String title = null;
	private String expression = null;
	private Pattern pattern = null;
	
	/***
	 * Create a new instance
	 * @param title The associated title of this pattern
	 * @param expression The Java regular expression
	 */
	public PatternInfo(String title, String expression){
		this.title = title;
		this.expression = expression;
	}
	
	/***
	 * Compiles the regular expression String provided into a Java Pattern object
	 * @param caseSensitive Whether it should be case sensitive
	 */
	public void compile(boolean caseSensitive){
		if(pattern == null){
			if(caseSensitive)
				pattern = Pattern.compile(expression);
			else
				pattern = Pattern.compile(expression,Pattern.CASE_INSENSITIVE);
		}
	}

	/***
	 * Gets the title associated with this instance
	 * @return The associated title
	 */
	public String getTitle() {
		return title;
	}

	/***
	 * Gets the Java regular expression string associated with this instance
	 * @return The Java regular expression string
	 */
	public String getExpression() {
		return expression;
	}

	/***
	 * Gets the compiled Pattern object.  Note this will return null until {@link #compile(boolean)} is called.
	 * @return The compiled Pattern object or null if {@link #compile(boolean)} has not yet been called.
	 */
	public Pattern getPattern() {
		return pattern;
	}
}
