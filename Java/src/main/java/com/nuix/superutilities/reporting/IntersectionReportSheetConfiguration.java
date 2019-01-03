package com.nuix.superutilities.reporting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import nuix.Case;

public class IntersectionReportSheetConfiguration {
	private List<NamedQuery> rowCriteria = new ArrayList<NamedQuery>();
	private List<NamedQuery> colCriteria = new ArrayList<NamedQuery>();
	private List<ColumnValueGenerator> valueGenerators = new ArrayList<ColumnValueGenerator>();

	private String rowCategoryLabel = "Term";
	private String colPrimaryCategoryLabel = "Column Category";
	private String scopeQuery = "";
	
	/***
	 * Gets the list of criteria used for each row.
	 * @return List of row criteria.
	 */
	public List<NamedQuery> getRowCriteria() {
		return rowCriteria;
	}

	/***
	 * Sets the list of criteria used for each row.
	 * @param rowCriteria The list of criteria which defines each row.
	 */
	public void setRowCriteria(List<NamedQuery> rowCriteria) {
		this.rowCriteria = rowCriteria;
	}
	
	/***
	 * Adds a single {@link NamedQuery} to the list of row criteria.
	 * @param criterion The named query to add to the list of row criteria.
	 */
	public void addRowCriterion(NamedQuery criterion) {
		this.rowCriteria.add(criterion);
	}
	
	/***
	 * Adds a single {@link NamedQuery}, constructed from the provided arguments, to the list of row criteria.
	 * @param name Name value used when constructing the {@link NamedQuery} object.
	 * @param query Query value used when constructing the {@link NamedQuery} object.
	 */
	public void addRowCriterion(String name, String query) {
		NamedQuery nq = new NamedQuery(name,query);
		addRowCriterion(nq);
	}
	
	/***
	 * Removes all currently assigned row criteria.
	 */
	public void clearRowCriteria() {
		this.rowCriteria.clear();
	}

	/***
	 * Gets the list of criteria used for each primary column category.
	 * @return The list of criteria used for each primary column category.
	 */
	public List<NamedQuery> getColCriteria() {
		return colCriteria;
	}

	/***
	 * Sets the list of criteria used for each primary column category.
	 * @param colCriteria The list of criteria to use for each primary column category.
	 */
	public void setColCriteria(List<NamedQuery> colCriteria) {
		this.colCriteria = colCriteria;
	}
	
	/***
	 * Adds a single {@link NamedQuery} to the list of primary column criteria.
	 * @param criterion The named query to add to the list of primary column criteria.
	 */
	public void addColCriterion(NamedQuery criterion) {
		this.colCriteria.add(criterion);
	}
	
	/***
	 * Adds a single {@link NamedQuery}, constructed from the provided arguments, to the list of primary column criteria.
	 * @param name Name value used when constructing the {@link NamedQuery} object.
	 * @param query Query value used when constructing the {@link NamedQuery} object.
	 */
	public void addColCriterion(String name, String query) {
		NamedQuery nq = new NamedQuery(name,query);
		addColCriterion(nq);
	}

	/***
	 * Gets the list of {@link ColumnValueGenerator} objects used to calculate the value of each secondary column nested beneath any given primary column.
	 * @return The list of {@link ColumnValueGenerator} objects used to calculate the value of each secondary column nested beneath any given primary column.
	 */
	public List<ColumnValueGenerator> getValueGenerators() {
		return valueGenerators;
	}

	/***
	 * Sets the list of {@link ColumnValueGenerator} objects used to calculate the value of each secondary column nested beneath any given primary column.
	 * @param valueGenerators The list of {@link ColumnValueGenerator} objects used to calculate the value of each secondary column nested beneath any given primary column.
	 */
	public void setValueGenerators(List<ColumnValueGenerator> valueGenerators) {
		this.valueGenerators = valueGenerators;
	}
	
	/***
	 * Adds a {@link ColumnValueGenerator} which calculates its value using the expression provided.
	 * @param label The label used for this secondary column
	 * @param expression The expression used to calculate this secondary column's value.  Expression is provided a Nuix Case object and query and should return an object such as a String or integer value.
	 */
	public void addScriptedValueGenerator(String label, BiFunction<Case,String,Object> expression) {
		ScriptedColumnValueGenerator scriptedGenerator = new ScriptedColumnValueGenerator(label,expression);
		this.valueGenerators.add(scriptedGenerator);
	}

	/***
	 * Gets the overall row category label.
	 * @return The overall row category label.
	 */
	public String getRowCategoryLabel() {
		return rowCategoryLabel;
	}

	/***
	 * Sets the overall row category label.  For example if each row is a search term, then you might set the label to "Terms".
	 * @param rowCategoryLabel The overall row category label.
	 */
	public void setRowCategoryLabel(String rowCategoryLabel) {
		this.rowCategoryLabel = rowCategoryLabel;
	}

	/***
	 * Gets the overall primary column category label.
	 * @return The overall primary column category label.
	 */
	public String getColPrimaryCategoryLabel() {
		return colPrimaryCategoryLabel;
	}

	/***
	 * Sets the overall primary column category label.  For example, if each primary column is a custodian name, then you might set the label to "Custodians".
	 * @param colPrimaryCategoryLabel The overall primary column label.
	 */
	public void setColPrimaryCategoryLabel(String colPrimaryCategoryLabel) {
		this.colPrimaryCategoryLabel = colPrimaryCategoryLabel;
	}

	/***
	 * Gets the current scope query which scopes the overall item set reported on.  A blank value is equivalent to no overall scope.
	 * @return The current overall scope query.
	 */
	public String getScopeQuery() {
		return scopeQuery;
	}

	/***
	 * Sets the current scope query which scopes the overall item set reported on.  A blank value is equivalent to no overall scope.
	 * @param scopeQuery The overall scope query to use.
	 */
	public void setScopeQuery(String scopeQuery) {
		this.scopeQuery = scopeQuery;
	}
}
