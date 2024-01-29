package com.nuix.superutilities.misc;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import com.nuix.script.impl.ExtendedItem;
import com.nuix.superutilities.query.QueryHelper;

import nuix.Case;
import nuix.Item;

public class TermExpander {
	private SimilarityCalculation fuzzyResolutionAlgorithm = SimilarityCalculation.Nuix;
	private BiConsumer<Integer,Integer> progressCallback;

	/***
	 * Gets which method this instance will use to resolve fuzzy terms.
	 * @return The fuzzy term resolution method
	 */
	public SimilarityCalculation getFuzzyResolutionAlgorithm() {
		return fuzzyResolutionAlgorithm;
	}

	/***
	 * Sets which method this instance will use to resolve fuzzy terms.
	 * @param resolutionAlgorithm The fuzzy term resolution method
	 */
	public void setFuzzyResolutionAlgorithm(SimilarityCalculation resolutionAlgorithm) {
		this.fuzzyResolutionAlgorithm = resolutionAlgorithm;
	}
	
	/***
	 * Allows you to provide a callback which will be invoked when {@link #expandTerm(Case, boolean, boolean, String, String)} (or similar overloads)
	 * make progress resolving a given term to matched terms.
	 * @param callback A BiConsumer that accepts 2 Integers, the first being current progress.  The second being total progress.
	 */
	public void whenProgressUpdated(BiConsumer<Integer,Integer> callback) {
		this.progressCallback = callback;
	}
	
	private void fireProgressUpdated(int current, int total) {
		if(progressCallback != null) {
			progressCallback.accept(current, total);
		}
	}

	/***
	 * This method "expands" a term to related terms in the specified case.  The intent being to get an idea of what a wild card or fuzzy term may be expanding into
	 * were it to be used in a query.  Its should only be provided single word terms, anything else will likely yield no or undefined results.
	 * Term provided may contain wild card matching characters or be a fuzzy term.<br><br>
	 * Wild card characters are <code>*</code> (match 0 or more characters) or <code>?</code> (match a single character).  Examples:<br>
	 * <code>ca*</code> - Example terms: cat, catch, cats, car, cars, caring<br>
	 * <code>*th</code> - Example terms: math, bath, fourth, fifth, wrath<br>
	 * <code>c?n</code> - Example terms: con, can, cnn<br><br>
	 * A fuzzy term may also be provided.  A fuzzy term is a term, followed by a tilde and optionally a similarity score.  For example:<br>
	 * <code>jason~0.5</code> - Example terms: json, jevon, jasen, mason, juston, jayson, etc<br>
	 * This version of the method internally calls {@link #expandTerm(Case, boolean, boolean, String, String)} providing true for arguments "content" and "properties"
	 * and a null scope query.
	 * @param nuixCase The Nuix case, needed to access case term statistics
	 * @param term The term to expand upon examples: ca*, *th, c?n, jason~0.5
	 * @return A list of {@link ExpandedTermInfo} objects, each containing information about a matched term.
	 * @throws Exception If something goes wrong
	 */
	public List<ExpandedTermInfo> expandTerm(Case nuixCase, String term) throws Exception {
		return expandTerm(nuixCase,true,true,term,null);
	}
	
	/***
	 * This method "expands" a term to related terms in the specified case.  The intent being to get an idea of what a wild card or fuzzy term may be expanding into
	 * were it to be used in a query.  Its should only be provided single word terms, anything else will likely yield no or undefined results.
	 * Term provided may contain wild card matching characters or be a fuzzy term.<br><br>
	 * Wild card characters are <code>*</code> (match 0 or more characters) or <code>?</code> (match a single character).  Examples:<br>
	 * <code>ca*</code> - Example terms: cat, catch, cats, car, cars, caring<br>
	 * <code>*th</code> - Example terms: math, bath, fourth, fifth, wrath<br>
	 * <code>c?n</code> - Example terms: con, can, cnn<br><br>
	 * A fuzzy term may also be provided.  A fuzzy term is a term, followed by a tilde and optionally a similarity score.  For example:<br>
	 * <code>jason~0.5</code> - Example terms: json, jevon, jasen, mason, juston, jayson, etc<br>
	 * This version of the method internally calls {@link #expandTerm(Case, boolean, boolean, String, String)} providing a null for scope query.
	 * @param nuixCase The Nuix case, needed to access case term statistics
	 * @param content Whether to include terms from item content text
	 * @param properties Whether to include terms from item metadata properties
	 * @param term The term to expand upon examples: ca*, *th, c?n, jason~0.5
	 * @return A list of {@link ExpandedTermInfo} objects, each containing information about a matched term.
	 * @throws Exception If something goes wrong
	 */
	public List<ExpandedTermInfo> expandTerm(Case nuixCase, boolean content, boolean properties, String term) throws Exception {
		return expandTerm(nuixCase,content,properties,term,null);
	}
	
	/***
	 * This method "expands" a term to related terms in the specified case.  The intent being to get an idea of what a wild card or fuzzy term may be expanding into
	 * were it to be used in a query.  Its should only be provided single word terms, anything else will likely yield no or undefined results.
	 * Term provided may contain wild card matching characters or be a fuzzy term.<br><br>
	 * Wild card characters are <code>*</code> (match 0 or more characters) or <code>?</code> (match a single character).  Examples:<br>
	 * <code>ca*</code> - Example terms: cat, catch, cats, car, cars, caring<br>
	 * <code>*th</code> - Example terms: math, bath, fourth, fifth, wrath<br>
	 * <code>c?n</code> - Example terms: con, can, cnn<br><br>
	 * A fuzzy term may also be provided.  A fuzzy term is a term, followed by a tilde and optionally a similarity score.  For example:<br>
	 * <code>jason~0.5</code> - Example terms: json, jevon, jasen, mason, juston, jayson, etc<br>
	 * @param nuixCase The Nuix case, needed to access case term statistics
	 * @param content Whether to include terms from item content text
	 * @param properties Whether to include terms from item metadata properties
	 * @param term The term to expand upon examples: ca*, *th, c?n, jason~0.5
	 * @param scopeQuery A Nuix query which can be used to limit which items' terms are considered.  An empty string or null will be all items in the case.
	 * @return A list of {@link ExpandedTermInfo} objects, each containing information about a matched term.
	 * @throws Exception If something goes wrong
	 */
	public List<ExpandedTermInfo> expandTerm(Case nuixCase, boolean content, boolean properties, String term, String scopeQuery) throws Exception{
		List<ExpandedTermInfo> result = new ArrayList<ExpandedTermInfo>();
		
		if(!content && !properties) {
			return result;
		}
		
		if(scopeQuery == null) {
			scopeQuery = "";
		}
		
		Map<String,Long> allTermStats = acquireAllRelevantTerms(nuixCase,content,properties,scopeQuery);

		if(term.trim().contentEquals("*") || term.trim().isEmpty()) {
			// Just a star or an empty string term returns all terms with a reported similarity of 0
			int current = 0;
			for(Map.Entry<String, Long> termStat : allTermStats.entrySet()) {
				current++;
				fireProgressUpdated(current, allTermStats.size());
				ExpandedTermInfo eti = new ExpandedTermInfo();
				eti.setOriginalTerm(term);
				eti.setMatchedTerm(termStat.getKey());
				eti.setOcurrences(termStat.getValue());
				eti.setSimilarity(0.0d);
				result.add(eti);
			}
		} else if(FuzzyTermInfo.isFuzzyTerm(term)) {
			if(fuzzyResolutionAlgorithm == SimilarityCalculation.Nuix) {
				expandFuzzyWithNuix(nuixCase, content, properties, term, scopeQuery, result, allTermStats);
			} else {
				expandFuzzyWithSimilarityFiltering(term, result, allTermStats);
			}
		} else {
			expandWildcardsWithRegex(term, result, allTermStats);
		}
		return result;
	}

	/***
	 * Resolves a non-fuzzy term potentially containing wild card characters '*' (0 or more characters) and/or '?' (1 character).
	 * Internally wild card characters are converted to equivalent regex expressions '.*' and '.'.
	 * @param term The term containing Lucene style wild card characters
	 * @param result The collection to add results to
	 * @param allTermStats The collection of term statistics to filter from
	 */
	private void expandWildcardsWithRegex(String term, List<ExpandedTermInfo> result, Map<String, Long> allTermStats) {
		// Else just assume we need to convert this to Pattern (regex) to filter words in case against
		Pattern filter = convertToPattern(term);
		int current = 0;
		for(Map.Entry<String, Long> termStat : allTermStats.entrySet()) {
			current++;
			fireProgressUpdated(current, allTermStats.size());
			if(filter.matcher(termStat.getKey()).matches()) {
				ExpandedTermInfo eti = new ExpandedTermInfo();
				eti.setOriginalTerm(term);
				eti.setMatchedTerm(termStat.getKey());
				eti.setOcurrences(termStat.getValue());
				eti.setSimilarity(0.0d);
				result.add(eti);
			}
		}
	}

	/***
	 * Resolves a fuzzy term against the case term statistics list using the similarity calculation
	 * specified by {@link #setFuzzyResolutionAlgorithm(SimilarityCalculation)}.
	 * @param term The fuzzy term
	 * @param result Collection to add results to
	 * @param allTermStats The collection of term statistics to filter from
	 */
	private void expandFuzzyWithSimilarityFiltering(String term, List<ExpandedTermInfo> result,
			Map<String, Long> allTermStats) {
		FuzzyTermInfo f = FuzzyTermInfo.parseFuzzyTerm(term);
		int current = 0;
		for(Map.Entry<String, Long> termStat : allTermStats.entrySet()) {
			current++;
			fireProgressUpdated(current, allTermStats.size());
			Double similarity = 0.0d;
			if(fuzzyResolutionAlgorithm == SimilarityCalculation.JaroWinkler) {
				similarity = f.calculateJaroWinklerSimilarityTo(termStat.getKey());
			}
			
			if(similarity >= f.getTargetSimilarity()) {
				ExpandedTermInfo eti = new ExpandedTermInfo();
				eti.setOriginalTerm(term);
				eti.setMatchedTerm(termStat.getKey());
				eti.setOcurrences(termStat.getValue());
				eti.setSimilarity(similarity);
				result.add(eti);
			}
		}
	}

	/***
	 * Resolves a fuzzy term to matched terms by inquiring with Nuix directly.
	 * @param nuixCase The Nuix case
	 * @param term The fuzzy term, i.e.: <code>jason~0.5</code>
	 * @param scopeQuery Optional scope query
	 * @param result Collection to add results to
	 * @param allTermStats 
	 * @throws IOException
	 */
	private void expandFuzzyWithNuix(Case nuixCase, boolean content, boolean properties, String term, String scopeQuery, List<ExpandedTermInfo> result, Map<String, Long> allTermStats)
			throws IOException {
		// Were going to ask Nuix what terms it gets for the fuzzy search
		int currentProgress = 0;
		
		// Build out the query we will be submitting to Nuix, including scope query only
		// if it is not an empty string.
		List<String> queryPieces = new ArrayList<String>();
		String termQueryPiece = "";
		if(content && properties) {
			termQueryPiece = String.format("content:%s OR properties:%s", term, term);
		} else if(content && !properties) {
			termQueryPiece = String.format("content:%s", term);
		} else if(properties && !content) {
			termQueryPiece = String.format("properties:%s", term);
		} else if(!content && !properties) {
			return;
		}
		queryPieces.add(termQueryPiece);
		if(!scopeQuery.isEmpty()) { queryPieces.add(scopeQuery); }
		String query = QueryHelper.parenThenJoinByAnd(queryPieces);
		
		FuzzyTermInfo f = FuzzyTermInfo.parseFuzzyTerm(term);
		
		// Inquire with Nuix what terms each item is responsive to for the given fuzzy query
		Set<Item> items = nuixCase.searchUnsorted(query);
		Set<String> distinctTerms = new HashSet<String>();
		for(Item item : items) {
			currentProgress++;
			fireProgressUpdated(currentProgress, items.size());
			ExtendedItem eItem = (ExtendedItem)item;
			for(String itemTerm : eItem.getTerms(termQueryPiece)) {
				distinctTerms.add(itemTerm);
			}
		}
		
		for(String distinctTerm : distinctTerms) {
			ExpandedTermInfo eti = new ExpandedTermInfo();
			eti.setOriginalTerm(term);
			eti.setMatchedTerm(distinctTerm);
			eti.setOcurrences(allTermStats.get(distinctTerm));
			eti.setSimilarity(f.calculateJaroWinklerSimilarityTo(distinctTerm));
			result.add(eti);
		}
	}

	private String stringToMd5(String input) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		StringBuffer hashBuffer = new StringBuffer();
		md.update(input.getBytes());
		byte[] digest = md.digest();
		for (byte b : digest) {
			hashBuffer.append(String.format("%02x", b & 0xff));
		}
		return hashBuffer.toString();
	}
	
	private Map<String,Long> cachedAllTerms = null;
	private String lastCacheId = null;
	
	/***
	 * Acquires term statistics either by freshly fetching them or by using a previously cached set of them if criteria has
	 * not changed since they were last obtained.  Arguments are converted into an ID string which is then used to determine
	 * whether cached term statistics have changed from call to call.
	 * @param nuixCase The Nuix case
	 * @param content Whether terms from item content text are included
	 * @param properties Whether terms from item properties are included
	 * @param scopeQuery A query to scope which items' terms are returned, blank is all items in the case
	 * @return A Map with term as key and occurrence count as value
	 * @throws Exception Likely caused by a bad scope query
	 */
	private Map<String,Long> acquireAllRelevantTerms(Case nuixCase, boolean content, boolean properties, String scopeQuery) throws Exception{
		// Attempt to return cached set of terms if all criteria used to obtain all term stats previously are the same
		// we do this by generating an identifier string based on the settings.  If the settings change, the identifier string
		// changes and we trigger fetching a new all term stats set.
		String scopeQueryMd5 = stringToMd5(scopeQuery);
		String cacheId = String.format("%s-%s-%s-%s", nuixCase.getGuid(),content,properties,scopeQueryMd5);
		if(cachedAllTerms == null || lastCacheId == null || !cacheId.contentEquals(lastCacheId)) {
			Map<String,Object> termSettings = new HashMap<String,Object>();
			termSettings.put("sort", "on");
			if(content && properties) {
				termSettings.put("field", "all");
			} else if(content && !properties) {
				termSettings.put("field","content");
			} else if(!content && properties) {
				termSettings.put("field","properties");
			}
			
			if(scopeQuery == null) {
				cachedAllTerms = nuixCase.getStatistics().getTermStatistics("",termSettings);	
			} else {
				cachedAllTerms = nuixCase.getStatistics().getTermStatistics(scopeQuery,termSettings);
			}
			lastCacheId = cacheId;
		}
		return cachedAllTerms;
	}
	
	/***
	 * Converts a term with '*' and '?' wild cards into a regular expression Pattern.
	 * @param term The term to convert
	 * @return A Pattern with similar matching characteristics as the equivalent Lucene wildcards
	 */
	private Pattern convertToPattern(String term) {
		String trimmedTerm = term.trim();
		if(trimmedTerm.contains("*") || trimmedTerm.contains("?")) {
			trimmedTerm = escapeUnsupportedRegexSpecialCharacters(trimmedTerm);
			trimmedTerm = trimmedTerm.replaceAll("\\*", "\\.\\*");
			trimmedTerm = trimmedTerm.replaceAll("\\?", "\\.");
			return Pattern.compile("^"+trimmedTerm+"$",Pattern.CASE_INSENSITIVE);
		} else {
			return Pattern.compile("^\\Q"+trimmedTerm+"\\E$",Pattern.CASE_INSENSITIVE);
		}
	}
	
	/***
	 * Escapes regular expression characters except '*' and '?'
	 * @param input The expression to escape
	 * @return The expression with regular expression characters escaped except for '*' and '?'
	 */
	private String escapeUnsupportedRegexSpecialCharacters(String input) {
		String result = input.trim();
		result = result.replace("\\", "\\\\");
		result = result.replace(".", "\\.");
		result = result.replace("{", "\\{");
		result = result.replace("}", "\\}");
		result = result.replace("^", "\\^");
		result = result.replace("$", "\\$");
		result = result.replace("(", "\\(");
		result = result.replace(")", "\\)");
		result = result.replace("-", "\\-");
		result = result.replace("|", "\\|");
		result = result.replace("<", "\\<");
		result = result.replace(">", "\\>");
		result = result.replace("[", "\\[");
		result = result.replace("]", "\\]");
		return result;
	}
}
