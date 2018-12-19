package com.nuix.superutilities.reporting;

public class NamedQuery {
	private String name = "Name";
	private String query = "Query";
	
	public NamedQuery() {}
	
	public NamedQuery(String name, String query) {
		this.name = name;
		this.query = query;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}
}
