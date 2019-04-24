package com.nuix.superutilities.annotations;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import nuix.Case;
import nuix.MarkupSet;

/***
 * Provides settings regarding bulk redaction performed by {@link BulkRedactor#findAndRedact(Case, BulkRedactorSettings, Collection)}.
 * @author Jason Wells
 *
 */
public class BulkRedactorSettings {
	private static Pattern whitespaceSplitter = Pattern.compile("\\s+");
	
	private String markupSetName = "BulkRedactor";
	private File tempDirectory = null;
	private Set<String> expressions = new HashSet<String>();
	private Set<String> namedEntityTypes = new HashSet<String>();
	
	/***
	 * Gets the name of the markup set to which markups will be recorded against.
	 * @return The name of the markup set to use.
	 */
	public String getMarkupSetName() {
		return markupSetName;
	}
	
	/***
	 * Sets the name of the markup set to which markup will be recorded against.
	 * @param markupSetName The name of the markup set to use.  If the name provided does not exist, a markup set will be created.
	 */
	public void setMarkupSetName(String markupSetName) {
		this.markupSetName = markupSetName;
	}
	
	/***
	 * Gets the temp directory to which {@link BulkRedactor} will export PDF files which will be provided to Aspose.
	 * @return The PDF export temp directory.
	 */
	public File getTempDirectory() {
		return tempDirectory;
	}
	
	/***
	 * Sets the temp directory to which {@link BulkRedactor} will export PDF files which will be provided to Aspose.
	 * @param tempDirectory The PDF export temp directory.
	 */
	public void setTempDirectory(File tempDirectory) {
		this.tempDirectory = tempDirectory;
	}
	
	/***
	 * Sets the temp directory to which {@link BulkRedactor} will export PDF files which will be provided to Aspose.
	 * @param tempDirectory The PDF export temp directory.
	 */
	public void setTempDirectory(String tempDirectory) {
		this.tempDirectory = new File(tempDirectory);
	}
	
	/***
	 * Gets the regular expressions which will be used to locate text to markup.
	 * @return The regular expressions which will be used to locate text to markup.
	 */
	public Set<String> getExpressions() {
		return expressions;
	}
	
	/***
	 * Sets the regular expressions which will be used to locate text to markup.
	 * @param expressions The regular expressions which will be used to locate text to markup.
	 */
	public void setExpressions(Collection<String> expressions) {
		this.expressions.clear();
		this.expressions.addAll(expressions);
	}
	
	/***
	 * Adds a regular expression to the list of expressions used to locate text to markup.
	 * @param regularExpression A regular expression to add
	 */
	public void addExpression(String regularExpression) {
		expressions.add(regularExpression.trim());
	}
	
	/***
	 * Gets the list of named entity types to be redacted.
	 * @return The list of named entity types to be redacted.
	 */
	public Set<String> getNamedEntityTypes() {
		return namedEntityTypes;
	}
	
	/***
	 * Sets the list of named entity type to be redacted.
	 * @param namedEntityTypes The named entity types to be redacted.
	 */
	public void setNamedEntityTypes(Collection<String> namedEntityTypes) {
		this.namedEntityTypes.clear();
		this.namedEntityTypes.addAll(namedEntityTypes);
	}
	
	/***
	 * Adds a named entity type to the list of types to be redacted.
	 * @param namedEntityType The named entity type to be added to the list.
	 */
	public void addNamedEntity(String namedEntityType) {
		namedEntityTypes.add(namedEntityType);
	}
	
	/***
	 * Gets the Nuix MarkupSet object based on the markup set name as set by {@link #setMarkupSetName(String)}.  If the markup set by the given name
	 * already exists, then this will yield that existing markup set.  If it does not, a new markup set with the given name will be created.
	 * @param nuixCase The relevant Nuix Case
	 * @return Either the existing markup set or a newly created markup set, based on the markup set name as set by {@link #setMarkupSetName(String)}.
	 */
	public MarkupSet getMarkupSet(Case nuixCase) {
		MarkupSet result = null;
		for(MarkupSet ms : nuixCase.getMarkupSets()) {
			if(ms.getName().contentEquals(markupSetName)) {
				result = ms;
				break;
			}
		}
		
		if(result == null) {
			result = nuixCase.createMarkupSet(markupSetName);
		}
		
		return result;
	}
	
	/***
	 * Escapes regular expression characters in the input string.
	 * @param input String with regular expression characters to escape.
	 * @return String with the regular expression characters escaped.
	 */
	public static String escapeRegexSpecialCharacters(String input) {
		String result = input.trim();
		result = result.replace("\\", "\\\\");
		result = result.replace("{", "\\{");
		result = result.replace("}", "\\}");
		result = result.replace(".", "\\.");
		result = result.replace("^", "\\^");
		result = result.replace("$", "\\$");
		result = result.replace("(", "\\(");
		result = result.replace(")", "\\)");
		result = result.replace("-", "\\-");
		result = result.replace("*", "\\*");
		result = result.replace("?", "\\?");
		result = result.replace("|", "\\|");
		result = result.replace("<", "\\<");
		result = result.replace(">", "\\>");
		result = result.replace("[", "\\[");
		result = result.replace("]", "\\]");
		return result;
	}
	
	/***
	 * Converts a plain text term to a regular expression which is case insensitive.
	 * This is done by generating a regular expression with a character class matching each letter in both lower
	 * case and upper case.  For example if the input term is "cat", this returns
	 * a regular expression "[Cc][Aa][Tt]".
	 * @param inputTerm Term to convert to a case insensitive regular expression
	 * @return String Regular expression that is case insensitive regular expression for input term
	 */
	public static String termToRegex(String inputTerm) {
		StringJoiner result = new StringJoiner("");
		String escapedInput = escapeRegexSpecialCharacters(inputTerm);
		for(int i = 0; i < escapedInput.length(); i++) {
			char c = escapedInput.charAt(i);
			if(Character.isLetter(c)) {
				result.add(String.format("[%s%s]", Character.toUpperCase(c), Character.toLowerCase(c)));	
			} else {
				result.add(Character.toString(c));
			}
		}
		return result.toString();
	}
	
	/***
	 * Splits a string into tokens on whitespace using the regular expression: \s+
	 * @param phrase The phrase to tokenize.
	 * @return List of token strings.
	 */
	public static List<String> splitPhrase(String phrase){
		String[] terms = whitespaceSplitter.split(phrase);
		List<String> result = new ArrayList<String>();
		for (int i = 0; i < terms.length; i++) {
			result.add(terms[i].trim());
		}
		return result;
	}
	
	/***
	 * Converts a phrase (string with potentially multiple terms) into a regular expression for used searching for text to redact in a PDF.
	 * @param phrase The phrase to convert into a regular expression.  May be single or multiple terms.
	 * @return A regular expression to match that phrase in a case insensitive manner.  Internally calls {@link #splitPhrase(String)} and {@link #termToRegex(String)}.
	 */
	public static String phraseToExpression(String phrase) {
		List<String> tokenized = splitPhrase(phrase);
		String expression = String.join("\\s+",tokenized.stream().map(t -> termToRegex(t)).collect(Collectors.toList()));
		return expression;
	}
	
	/***
	 * Adds a phrase to the list of expressions used to find and markup text.  Phrase is converted to an expression using {@link #phraseToExpression(String)}.
	 * Resulting expression is then surrounded '\b' to anchor it to word boundaries.
	 * @param phrase The phrase to add to the list of expressions used to find and markup text.
	 */
	public void addPhrase(String phrase) {
		expressions.add("\\b"+phraseToExpression(phrase)+"\\b");
	}
	
	/**
	 * Adds multiple phrases to the list of expressions used to find and markup text.  Internally calls {@link #addPhrase(String)}
	 * for each string in the provide value.
	 * @param phrases Multiple phrases to the list of expressions used to find and markup text.
	 */
	public void addPhrases(Collection<String> phrases) {
		for(String phrase : phrases) {
			addPhrase(phrase);
		}
	}
}
