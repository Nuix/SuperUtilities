package com.nuix.superutilities.reporting;

/***
 * Encapsulates a Nuix query string and an associated name.
 * @author Jason Wells
 *
 */
public class NamedQuery {
	private String name = "Name";
	private String query = "Query";
	
	public NamedQuery() {}
	
	public NamedQuery(String name, String query) {
		this.name = name;
		this.query = query;
	}

	/***
	 * Gets the associated name
	 * @return The associated name
	 */
	public String getName() {
		return name;
	}

	/***
	 * Sets the associated name
	 * @param name The name to associate
	 */
	public void setName(String name) {
		this.name = name;
	}

	/***
	 * Gets the associated query
	 * @return The associated query
	 */
	public String getQuery() {
		return query;
	}

	/***
	 * Sets the associated query
	 * @param query The query to associate
	 */
	public void setQuery(String query) {
		this.query = query;
	}
}
