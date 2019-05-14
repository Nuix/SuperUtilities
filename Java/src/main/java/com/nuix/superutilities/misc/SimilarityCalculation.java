package com.nuix.superutilities.misc;

/***
 * Used by {@link com.nuix.superutilities.misc.TermExpander} to determine how fuzzy similarity should be
 * calculated to expand a given fuzzy term into matched terms.
 * @author Jason Wells
 *
 */
public enum SimilarityCalculation {
	Nuix,
	Levenstein,
	LuceneLevenshstein,
	JaroWinkler,
	NGram,
}
