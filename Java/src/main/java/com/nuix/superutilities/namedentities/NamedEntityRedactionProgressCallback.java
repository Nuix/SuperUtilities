package com.nuix.superutilities.namedentities;

/***
 * Provides progress status callback functionality for {@link com.nuix.superutilities.namedentities.NamedEntityUtility}.
 * @author Jason Wells
 *
 */
public interface NamedEntityRedactionProgressCallback {
	public void progressUpdated(int current, int total);
}
