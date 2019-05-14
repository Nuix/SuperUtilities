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
	
	private BiConsumer<Integer,Integer> progressCallback;
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
	 * Term provided may contain wild card matching characters or be a fuzzy term.<br/><br/>
	 * Wild card characters are <code>*</code> (match 0 or more characters) or <code>?</code> (match a single character).  Examples:<br/>
	 * <code>ca*</code> - Example terms: cat, catch, cats, car, cars, caring<br/>
	 * <code>*th</code> - Example terms: math, bath, fourth, fifth, wrath<br/>
	 * <code>c?n</code> - Example terms: con, can, cnn<br/><br/>
	 * A fuzzy term may also be provided.  A fuzzy term is a term, followed by a tilde and optionally a similarity score.  For example:<br/>
	 * <code>jason~0.5</code> - Example terms: json, jevon, jasen, mason, juston, jayson, etc<br/>
	 * This version of the method internally calls {@link #expandTerm(Case, boolean, boolean, String, String)} providing true for arguments "content" and "properties"
	 * and a null scope query.
	 * @param nuixCase The Nuix case, needed to access case term statistics
	 * @param term The term to expand upon examples: ca*, *th, c?n, jason~0.5
	 * @return A list of {@link ExpandedTermInfo} objects, each containing information about a matched term.
	 * @throws Exception
	 */
	public List<ExpandedTermInfo> expandTerm(Case nuixCase, String term) throws Exception {
		return expandTerm(nuixCase,true,true,term,null);
	}
	
	/***
	 * This method "expands" a term to related terms in the specified case.  The intent being to get an idea of what a wild card or fuzzy term may be expanding into
	 * were it to be used in a query.  Its should only be provided single word terms, anything else will likely yield no or undefined results.
	 * Term provided may contain wild card matching characters or be a fuzzy term.<br/><br/>
	 * Wild card characters are <code>*</code> (match 0 or more characters) or <code>?</code> (match a single character).  Examples:<br/>
	 * <code>ca*</code> - Example terms: cat, catch, cats, car, cars, caring<br/>
	 * <code>*th</code> - Example terms: math, bath, fourth, fifth, wrath<br/>
	 * <code>c?n</code> - Example terms: con, can, cnn<br/><br/>
	 * A fuzzy term may also be provided.  A fuzzy term is a term, followed by a tilde and optionally a similarity score.  For example:<br/>
	 * <code>jason~0.5</code> - Example terms: json, jevon, jasen, mason, juston, jayson, etc<br/>
	 * This version of the method internally calls {@link #expandTerm(Case, boolean, boolean, String, String)} providing a null for scope query.
	 * @param nuixCase The Nuix case, needed to access case term statistics
	 * @param content Whether to include terms from item content text
	 * @param properties Whether to include terms from item metadata properties
	 * @param term The term to expand upon examples: ca*, *th, c?n, jason~0.5
	 * @return A list of {@link ExpandedTermInfo} objects, each containing information about a matched term.
	 * @throws Exception
	 */
	public List<ExpandedTermInfo> expandTerm(Case nuixCase, boolean content, boolean properties, String term) throws Exception {
		return expandTerm(nuixCase,content,properties,term,null);
	}
	
	/***
	 * This method "expands" a term to related terms in the specified case.  The intent being to get an idea of what a wild card or fuzzy term may be expanding into
	 * were it to be used in a query.  Its should only be provided single word terms, anything else will likely yield no or undefined results.
	 * Term provided may contain wild card matching characters or be a fuzzy term.<br/><br/>
	 * Wild card characters are <code>*</code> (match 0 or more characters) or <code>?</code> (match a single character).  Examples:<br/>
	 * <code>ca*</code> - Example terms: cat, catch, cats, car, cars, caring<br/>
	 * <code>*th</code> - Example terms: math, bath, fourth, fifth, wrath<br/>
	 * <code>c?n</code> - Example terms: con, can, cnn<br/><br/>
	 * A fuzzy term may also be provided.  A fuzzy term is a term, followed by a tilde and optionally a similarity score.  For example:<br/>
	 * <code>jason~0.5</code> - Example terms: json, jevon, jasen, mason, juston, jayson, etc<br/>
	 * @param nuixCase The Nuix case, needed to access case term statistics
	 * @param content Whether to include terms from item content text
	 * @param properties Whether to include terms from item metadata properties
	 * @param term The term to expand upon examples: ca*, *th, c?n, jason~0.5
	 * @param scopeQuery A Nuix query which can be used to limit which items' terms are considered.  An empty string or null will be all items in the case.
	 * @return A list of {@link ExpandedTermInfo} objects, each containing information about a matched term.
	 * @throws Exception
	 * @throws  
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
				eti.setSimilarity(0.0f);
				result.add(eti);
			}
		} else if(FuzzyTermInfo.isFuzzyTerm(term)) {
			if(fuzzyResolutionAlgorithm == SimilarityCalculation.Nuix) {
				expandFuzzyWithNuix(nuixCase, term, scopeQuery, result);
			} else {
				expandFuzzyWithSimilarityFiltering(term, result, allTermStats);
			}
		} else {
			expandWildcardsWithRegex(term, result, allTermStats);
		}
		return result;
	}

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
				eti.setSimilarity(0.0f);
				result.add(eti);
			}
		}
	}

	private void expandFuzzyWithSimilarityFiltering(String term, List<ExpandedTermInfo> result,
			Map<String, Long> allTermStats) {
		FuzzyTermInfo f = FuzzyTermInfo.parseFuzzyTerm(term);
		int current = 0;
		for(Map.Entry<String, Long> termStat : allTermStats.entrySet()) {
			current++;
			fireProgressUpdated(current, allTermStats.size());
			float similarity = 0.0f;
			if(fuzzyResolutionAlgorithm == SimilarityCalculation.JaroWinkler) {
				similarity = f.calculateJaroWinklerSimilarityTo(termStat.getKey());
			} else if(fuzzyResolutionAlgorithm == SimilarityCalculation.Levenstein) {
				similarity = f.calculateLevensteinSimilarityTo(termStat.getKey());
			} else if(fuzzyResolutionAlgorithm == SimilarityCalculation.LuceneLevenshstein) {
				similarity = f.calculateLuceneLevenshteinSimilarityTo(termStat.getKey());
			} else if(fuzzyResolutionAlgorithm == SimilarityCalculation.NGram) {
				similarity = f.calculateNGramSimilarityTo(termStat.getKey());
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

	private void expandFuzzyWithNuix(Case nuixCase, String term, String scopeQuery, List<ExpandedTermInfo> result)
			throws IOException {
		// Were going to ask Nuix what terms it gets for the fuzzy search
		int current = 0;
		List<String> queryPieces = new ArrayList<String>();
		queryPieces.add(term);
		if(!scopeQuery.isEmpty()) { queryPieces.add(scopeQuery); }
		String query = QueryHelper.parenThenJoinByAnd(queryPieces);
		Set<Item> items = nuixCase.searchUnsorted(query);
		Set<String> distinctTerms = new HashSet<String>();
		for(Item item : items) {
			current++;
			fireProgressUpdated(current, items.size());
			ExtendedItem eItem = (ExtendedItem)item;
			for(String itemTerm : eItem.getTerms(term)) {
				distinctTerms.add(itemTerm);
			}
		}
		
		for(String distinctTerm : distinctTerms) {
			ExpandedTermInfo eti = new ExpandedTermInfo();
			eti.setOriginalTerm(term);
			eti.setMatchedTerm(distinctTerm);
			eti.setOcurrences(-1);
			eti.setSimilarity(-1);
			result.add(eti);
		}
	}

	private String queryToMd5(String query) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		StringBuffer hashBuffer = new StringBuffer();
		md.update(query.getBytes());
		byte[] digest = md.digest();
		for (byte b : digest) {
			hashBuffer.append(String.format("%02x", b & 0xff));
		}
		return hashBuffer.toString();
	}
	
	private Map<String,Long> cachedAllTerms = null;
	private String lastCacheId = null;
	private Map<String,Long> acquireAllRelevantTerms(Case nuixCase, boolean content, boolean properties, String scopeQuery) throws Exception{
		// Attempt to return cached set of terms if all criteria used to obtain all term stats previously are the same
		// we do this by generating an identifier string based on the settings.  If the settings change, the identifier string
		// changes and we trigger fetching a new all term stats set.
		String scopeQueryMd5 = queryToMd5(scopeQuery);
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
	
	private Pattern convertToPattern(String term) {
		String trimmedTerm = term.trim();
		if(trimmedTerm.contains("*") || trimmedTerm.contains("?")) {
			trimmedTerm = trimmedTerm.replaceAll("\\*", "\\.\\*");
			trimmedTerm = trimmedTerm.replaceAll("\\?", "\\.");
			trimmedTerm = escapeUnsupportedRegexSpecialCharacters(trimmedTerm);
			return Pattern.compile("^"+trimmedTerm+"$",Pattern.CASE_INSENSITIVE);
		} else {
			return Pattern.compile("^\\Q"+trimmedTerm+"\\E$",Pattern.CASE_INSENSITIVE);
		}
	}
	
	private String escapeUnsupportedRegexSpecialCharacters(String input) {
		String result = input.trim();
		result = result.replace("\\", "\\\\");
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
