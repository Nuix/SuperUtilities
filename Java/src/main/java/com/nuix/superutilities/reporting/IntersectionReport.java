package com.nuix.superutilities.reporting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import nuix.Case;

public class IntersectionReport {
	
	private List<NamedQuery> rowCriteria = new ArrayList<NamedQuery>();
	private List<NamedQuery> colCriteria = new ArrayList<NamedQuery>();
	private List<ColumnValueGenerator> valueGenerators = new ArrayList<ColumnValueGenerator>();

	private String rowCategoryLabel = "Term";
	private String colPrimaryCategoryLabel = "Column Category";
	
	private SimpleXlsx xlsx = null;
	
	private String parenExpression(String expression) {
		return "("+expression+")";
	}
	
	private String andExpressions(String... expressions) {
		return String.join(" AND ", expressions);
	}
	
	public IntersectionReport(String file) throws Exception {
		xlsx = new SimpleXlsx(file);
	}
	
	public void generate(Case nuixCase, String sheetName) throws Exception {
		SimpleWorksheet sheet = xlsx.getSheet(sheetName);
		
		List<String> parenRowCriteria = rowCriteria.stream().map(c -> parenExpression(c.getQuery())).collect(Collectors.toList());
		List<String> parenColCriteria = colCriteria.stream().map(c -> parenExpression(c.getQuery())).collect(Collectors.toList());
		
		List<Object> rowValues = new ArrayList<Object>();
		
		// Start out by building out headers
		sheet.setValue(0, 0, colPrimaryCategoryLabel);
		sheet.setValue(1, 0, rowCategoryLabel);
		for (int c = 0; c < colCriteria.size(); c++) {
			// First Row
			String colCriterion = colCriteria.get(c).getName();
			int col = 1 + (c * valueGenerators.size());
			sheet.setValue(0, col, colCriterion);
			sheet.mergeCols(0, col, valueGenerators.size());
			
			// Second Row
			for (int sc = 0; sc < valueGenerators.size(); sc++) {
				int subCol = col + sc;
				sheet.setValue(1, subCol, valueGenerators.get(sc).getLabel());
			}
		}
		sheet.setCurrentRow(sheet.getCurrentRow()+2);
		
		for (int r = 0; r < rowCriteria.size(); r++) {
			String parenRowCriterion = parenRowCriteria.get(r);
			
			rowValues.clear();
			rowValues.add(rowCriteria.get(r).getName());
			for(String parenColCriterion : parenColCriteria) {
				String query = andExpressions(parenRowCriterion, parenColCriterion);
				for(ColumnValueGenerator generator : valueGenerators) {
					rowValues.add(generator.generateValue(nuixCase, query));
				}
			}
			
			//TESTING
			List<String> stringRowValues = rowValues.stream().map(v -> v.toString()).collect(Collectors.toList());
			System.out.println(String.join("\t", stringRowValues));
			
			sheet.appendRow(rowValues);
		}
		sheet.autoFitColumns();
		xlsx.save();
	}

	public List<NamedQuery> getRowCriteria() {
		return rowCriteria;
	}

	public void setRowCriteria(List<NamedQuery> rowCriteria) {
		this.rowCriteria = rowCriteria;
	}
	
	public void addRowCriterion(NamedQuery criterion) {
		this.rowCriteria.add(criterion);
	}
	
	public void addRowCriterion(String name, String query) {
		NamedQuery nq = new NamedQuery(name,query);
		addRowCriterion(nq);
	}
	
	public void clearRowCriteria() {
		this.rowCriteria.clear();
	}

	public List<NamedQuery> getColCriteria() {
		return colCriteria;
	}

	public void setColCriteria(List<NamedQuery> colCriteria) {
		this.colCriteria = colCriteria;
	}
	
	public void addColCriterion(NamedQuery criterion) {
		this.colCriteria.add(criterion);
	}
	
	public void addColCriterion(String name, String query) {
		NamedQuery nq = new NamedQuery(name,query);
		addColCriterion(nq);
	}

	public List<ColumnValueGenerator> getValueGenerators() {
		return valueGenerators;
	}

	public void setValueGenerators(List<ColumnValueGenerator> valueGenerators) {
		this.valueGenerators = valueGenerators;
	}
	
	public void addScriptedValueGenerator(String label, BiFunction<Case,String,Object> expression) {
		ScriptedColumnValueGenerator scriptedGenerator = new ScriptedColumnValueGenerator(label,expression);
		this.valueGenerators.add(scriptedGenerator);
	}

	public String getRowCategoryLabel() {
		return rowCategoryLabel;
	}

	public void setRowCategoryLabel(String rowCategoryLabel) {
		this.rowCategoryLabel = rowCategoryLabel;
	}

	public String getColPrimaryCategoryLabel() {
		return colPrimaryCategoryLabel;
	}

	public void setColPrimaryCategoryLabel(String colPrimaryCategoryLabel) {
		this.colPrimaryCategoryLabel = colPrimaryCategoryLabel;
	}
	
}
