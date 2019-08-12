package com.nuix.superutilities.misc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.search.spell.LuceneLevenshteinDistance;
import org.apache.lucene.search.spell.JaroWinklerDistance;
import org.apache.lucene.search.spell.NGramDistance;

/***
 * Encapsulates information about a fuzzy term expression.  Used by {@link TermExpander}.
 * @author Jason Wells
 *
 */
public class FuzzyTermInfo {
	private static Pattern fuzzyPattern = Pattern.compile("(?<term>([a-z0-9]+))~(?<similarity>([0-1]\\.?[0-9]*)?)",Pattern.CASE_INSENSITIVE);
	
	private static LuceneLevenshteinDistance luceneLevDist = new LuceneLevenshteinDistance();
	private static JaroWinklerDistance jaroDist = new JaroWinklerDistance();
	private static NGramDistance ngramDist = new NGramDistance();
	
	public static boolean isFuzzyTerm(String term) {
		return fuzzyPattern.matcher(term.trim()).find();
	}
	
	/***
	 * Parses a fuzzy term string into component term and similarity score.  When a similarity score is
	 * not present defaults to 0.5 (like <a href="https://lucene.apache.org/core/2_9_4/queryparsersyntax.html#Fuzzy%20Searches">Lucene</a>).
	 * @param term Fuzzy term expression to parse in form: <code>term~0.5</code> or <code>term~</code>.
	 * @return A Fuzzy object containing term and similarity score.
	 */
	public static FuzzyTermInfo parseFuzzyTerm(String term) {
		Matcher m = fuzzyPattern.matcher(term);
		FuzzyTermInfo f = new FuzzyTermInfo();
		if(m.find()) {
			f.term = m.group("term");
			String similarity = m.group("similarity");
			if(similarity.trim().isEmpty()) {
				f.similarity = 0.5f;
			} else {
				f.setTargetSimilarity(Float.parseFloat(similarity));
			}
		}
		return f;
	}
	
	public float calculateLuceneLevenshteinSimilarityTo(String otherTerm) {
		return luceneLevDist.getDistance(this.term, otherTerm);
	}
	
	public float calculateJaroWinklerSimilarityTo(String otherTerm) {
		return jaroDist.getDistance(this.term, otherTerm);
	}
	
	public float calculateNGramSimilarityTo(String otherTerm) {
		return ngramDist.getDistance(this.term, otherTerm);
	}
	
	private String term = "";
	private float similarity = 0.5f;
	public FuzzyTermInfo() {}

	public String getTerm() {
		return term;
	}

	public void setTerm(String term) {
		this.term = term;
	}

	public float getTargetSimilarity() {
		return similarity;
	}

	public void setTargetSimilarity(float similarity) {
		if(similarity < 0.0f) { this.similarity = 0.0f; }
		else if(similarity > 1.0f) { this.similarity = 1.0f; }
		else { this.similarity = similarity; }
	}
	
	
}
