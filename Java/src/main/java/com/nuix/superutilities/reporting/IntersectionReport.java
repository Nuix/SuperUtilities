package com.nuix.superutilities.reporting;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.aspose.cells.BackgroundType;
import com.aspose.cells.Color;
import com.aspose.cells.Style;

import nuix.Case;

/***
 * This class generates an "intersection report".  The report is generated using 3 things: row criteria, column criteria and column value generators.
 * Row and column criteria are collections of {@link NamedQuery} objects.  The value reported in a given cell is calculated by first AND'ing together
 * the relevant row and column criterion.  In turn the AND'ed expression is provided to a {@link ColumnValueGenerator} which can make use of the query
 * provided to calculate its particular value.  Examples might be running Case.count(query) to get a responsive item count or running Case.search(query)
 * and tweaking the results to include families or calling methods on CaseStatistics such as getAuditSize(query) to report responsive items total audited size.
 * This class takes care of iteratively running these things and providing formatting as they are written into an XLSX Excel spreadsheet.  If a scope query
 * is provided via the method {@link #setScopeQuery(String)}, the provided query will be AND'ed together as well with each row/col criteria combination, allowing
 * you to further scope the overall report to things like particular evidence, item types, tags, etc.
 * @author Jason Wells
 *
 */
public class IntersectionReport {
	
	private static Logger logger = Logger.getLogger(IntersectionReport.class);
	
	
	
	private SimpleXlsx xlsx = null;
	
	private Style rowCategoryLabelStyle = null;
	private Style colPrimaryCategoryLabelStyle = null;
	private Style rowCategoryStyle = null;
	private Style colCategoryStyle = null;
	private Style rowGeneralStyle = null;
	
	private ColorRing colCategoryColorRing = new ColorRing();
	
	/***
	 * Takes provided expression string and returns that expression wrapped in parens
	 * @param expression The expression to wrap in parens
	 * @return The expression wrapped in parens
	 */
	private String parenExpression(String expression) {
		return "("+expression+")";
	}
	
	/***
	 * Joins a list of expressions into a single AND'ed expression
	 * @param expressions The expressions to AND together
	 * @return A single query expression of the provided expressions AND'ed together
	 */
	private String andExpressions(String... expressions) {
		StringJoiner result = new StringJoiner(" AND ");
		for (int i = 0; i < expressions.length; i++) {
			String expression = expressions[i];
			if(expression == null || expression.isEmpty()) {
				// Don't want to include empty or null expressions
				continue;
			}
			else if(expression.contentEquals("()")) {
				// Strip out empty expressions that may have passed throuh parenExpression method
				continue;
			}
			else {
				result.add(expression);
			}
		}
		return result.toString();
	}
	
	/***
	 * Creates a new instance
	 * @param file File path of existing XLSX if you wish to add to an existing workbook, can be null
	 * @throws Exception Thrown if there are errors
	 */
	public IntersectionReport(String file) throws Exception {
		xlsx = new SimpleXlsx(file);
		
		rowCategoryLabelStyle = xlsx.createStyle();
		rowCategoryLabelStyle.getFont().setBold(true);
		rowCategoryLabelStyle.getFont().setSize(14);
		AsposeCellsStyleHelper.enableAllBorders(rowCategoryLabelStyle);
		
		colPrimaryCategoryLabelStyle = xlsx.createStyle();
		colPrimaryCategoryLabelStyle.getFont().setBold(true);
		colPrimaryCategoryLabelStyle.getFont().setSize(14);
		AsposeCellsStyleHelper.enableAllBorders(colPrimaryCategoryLabelStyle);
		
		rowCategoryStyle = xlsx.createStyle();
		rowCategoryStyle.getFont().setBold(true);
		rowCategoryStyle.setPattern(BackgroundType.SOLID);
		AsposeCellsStyleHelper.enableAllBorders(rowCategoryStyle);
		
		colCategoryStyle = xlsx.createStyle();
		colCategoryStyle.getFont().setBold(true);
		colCategoryStyle.setPattern(BackgroundType.SOLID);
		AsposeCellsStyleHelper.enableAllBorders(colCategoryStyle);
		
		colCategoryColorRing.addTintSeries(Color.fromArgb(255, 51, 51), 4); // Red
		colCategoryColorRing.addTintSeries(Color.fromArgb(51, 204, 51), 4); // Green
		colCategoryColorRing.addTintSeries(Color.fromArgb(0, 153, 204), 4); // Blue
		
		rowGeneralStyle = xlsx.createStyle();
		AsposeCellsStyleHelper.enableAllBorders(rowGeneralStyle);
	}
	
	/***
	 * Generates a new report sheet using the row criteria, column criteria and value generators currently assigned to this instance
	 * @param nuixCase The Nuix case report data is collected against
	 * @param sheetName The name of the excel worksheet to create
	 * @throws Exception Thrown if there are errors
	 */
	public void generate(Case nuixCase, String sheetName, IntersectionReportSheetConfiguration sheetConfig) throws Exception {
		SimpleWorksheet sheet = xlsx.getSheet(sheetName);
		
		List<String> parenRowCriteria = sheetConfig.getRowCriteria().stream().map(c -> parenExpression(c.getQuery())).collect(Collectors.toList());
		List<String> parenColCriteria = sheetConfig.getColCriteria().stream().map(c -> parenExpression(c.getQuery())).collect(Collectors.toList());
		
		List<Object> rowValues = new ArrayList<Object>();
		String parenScopeQuery = parenExpression(sheetConfig.getScopeQuery());
		
		// Start out by building out headers
		sheet.setValue(0, 0, sheetConfig.getColPrimaryCategoryLabel());
		sheet.setStyle(0, 0, colPrimaryCategoryLabelStyle);
		
		sheet.setValue(1, 0, sheetConfig.getRowCategoryLabel());
		sheet.setStyle(1, 0, rowCategoryLabelStyle);
		
		for (int c = 0; c < sheetConfig.getColCriteria().size(); c++) {
			// First Row
			Style colCategoryStyleCopy = xlsx.createStyle();
			colCategoryStyleCopy.copy(colCategoryStyle);
			colCategoryStyleCopy.setForegroundColor(colCategoryColorRing.next());
			
			String colCriterion = sheetConfig.getColCriteria().get(c).getName();
			int col = 1 + (c * sheetConfig.getValueGenerators().size());
			sheet.setValue(0, col, colCriterion);
			
			sheet.setStyle(0, col, colCategoryStyleCopy);
			sheet.mergeCols(0, col, sheetConfig.getValueGenerators().size());
			
			// Second Row
			for (int sc = 0; sc < sheetConfig.getValueGenerators().size(); sc++) {
				int subCol = col + sc;
				sheet.setValue(1, subCol, sheetConfig.getValueGenerators().get(sc).getLabel());
				sheet.setStyle(1, subCol, colCategoryStyle);
			}
		}
		sheet.setCurrentRow(sheet.getCurrentRow()+2);
		
		for (int r = 0; r < sheetConfig.getRowCriteria().size(); r++) {
			String parenRowCriterion = parenRowCriteria.get(r);
			
			rowValues.clear();
			rowValues.add(sheetConfig.getRowCriteria().get(r).getName());
			
			for(String parenColCriterion : parenColCriteria) {
				String query = andExpressions(parenScopeQuery, parenRowCriterion, parenColCriterion);
				logger.info(String.format("Query: %s", query));
				for(ColumnValueGenerator generator : sheetConfig.getValueGenerators()) {
					rowValues.add(generator.generateValue(nuixCase, query));
				}
			}
			
			//TESTING
//			List<String> stringRowValues = rowValues.stream().map(v -> v.toString()).collect(Collectors.toList());
//			logger.info(String.join("\t", stringRowValues));
			
			sheet.appendRow(rowValues, rowGeneralStyle);
			
			// Apply styles
			sheet.setStyle(sheet.getCurrentRow()-1, 0, rowCategoryStyle);
		}
		sheet.autoFitColumns();
		xlsx.save();
	}

	
	
}
