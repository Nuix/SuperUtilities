package com.nuix.superutilities.reporting;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.aspose.cells.BackgroundType;
import com.aspose.cells.Color;
import com.aspose.cells.Style;
import com.nuix.superutilities.misc.FormatUtility;

import nuix.BatchLoadDetails;
import nuix.Case;

/***
 * This class generates an "intersection report".  The report is generated using 3 things: row criteria, column criteria and column value generators.
 * Row and column criteria are collections of {@link NamedQuery} objects.  The value reported in a given cell is calculated by first AND'ing together
 * the relevant row and column criterion.  In turn the AND'ed expression is provided to a {@link ColumnValueGenerator} which can make use of the query
 * provided to calculate its particular value.  Examples might be running Case.count(query) to get a responsive item count or running Case.search(query)
 * and tweaking the results to include families or calling methods on CaseStatistics such as getAuditSize(query) to report responsive items total audited size.
 * This class takes care of iteratively running these things and providing formatting as they are written into an XLSX Excel spreadsheet.  If a scope query
 * is provided via the method {@link IntersectionReportSheetConfiguration#setScopeQuery(String)}, the provided query will be AND'ed together as well with each row/col criteria combination, allowing
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
	
	private IntersectionReportProgressCallback progressCallback = null;
	
	private void fireProgress(int current, int total) {
		if(progressCallback != null) {
			progressCallback.progressUpdated(current, total);
		}
	}
	
	public void whenProgressUpdated(IntersectionReportProgressCallback callback) {
		progressCallback = callback;
	}
	
	private Consumer<String> messageCallback = null;
	
	private void fireMessage(String message) {
		if(messageCallback != null) {
			messageCallback.accept(message);
		}
	}
	
	public void whenMessageGenerated(Consumer<String> callback) {
		messageCallback = callback;
	}
	
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
		
		// These are intended as generic defaults and you should really call getColCategoryColorRing
		// and configure the colors if they want them to look at all nice
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
	 * @param sheetConfig Configuration details regarding how this sheet should be generated
	 * @throws Exception Thrown if there are errors
	 */
	public void generate(Case nuixCase, String sheetName, IntersectionReportSheetConfiguration sheetConfig) throws Exception {
		long generationStarted = System.currentTimeMillis();
		colCategoryColorRing.restart();
		SimpleWorksheet sheet = xlsx.getSheet(sheetName);
		
		List<String> parenRowCriteria = sheetConfig.getRowCriteria().stream().map(c -> parenExpression(c.getQuery())).collect(Collectors.toList());
		List<String> parenColCriteria = sheetConfig.getColCriteria().stream().map(c -> parenExpression(c.getQuery())).collect(Collectors.toList());
		
		int currentCellIndex = 0;
		int totalCells = parenRowCriteria.size() * parenColCriteria.size() * sheetConfig.getValueGenerators().size();
		long lastProgress = System.currentTimeMillis();
		
		List<Object> rowValues = new ArrayList<Object>();
		String parenScopeQuery = parenExpression(sheetConfig.getScopeQuery());
		
		// If we have batch load dates we wish to constrain the results to, we must go look at all the batch loads
		// in the case and filter to those within our date range.  Using that filtered set we then get their batch
		// load GUIDs and use that to attach additional scope query for those batch loads.
		if(sheetConfig.hasBatchLoadDateCriteria()) {
			List<String> inRangeBatchLoadGuids = new ArrayList<String>();
			List<BatchLoadDetails> allBatchLoads = nuixCase.getBatchLoads();
			
			// Filter all batch loads by whether their loaded date falls within
			// the range specifies by the min/max values
			for(BatchLoadDetails bld : allBatchLoads) {
				boolean satisfiesMin = true;
				boolean satisfiesMax = true;
				
				if(sheetConfig.getBatchLoadMinDate() != null) {
					satisfiesMin = bld.getLoaded().isAfter(sheetConfig.getBatchLoadMinDate());
				}
				
				if(sheetConfig.getBatchLoadMaxDate() != null) {
					satisfiesMax = bld.getLoaded().isBefore(sheetConfig.getBatchLoadMaxDate());
				}
				
				if(satisfiesMin && satisfiesMax) {
					inRangeBatchLoadGuids.add(bld.getBatchId());
				}
			}
			
			String batchLoadGuidQuery = "";
			if(inRangeBatchLoadGuids.size() > 0) {
				// We have batch loads that met our criteria so we update the scope
				// query to restrict to those batch loads
				batchLoadGuidQuery = String.format("(batch-load-guid:%s)",String.join(" OR ", inRangeBatchLoadGuids));
				
			} else {
				// We have a funny situation now, user specified to filter to particular batch loads based on their
				// loaded date and no batch loads met the required criteria.  If we do nothing now, effectively no
				// batch loads matching is the same as all batch loads matching.  We are going to better represent no
				// batch loads matching by modifying scope criteria to accept no batch loads.  This will yield 0 results in the
				// report, which technically is an accurate representation of our results.  Should probably have logic long
				// before we reach this point that would catch batch load date criteria matching nothing and warning that user
				// then, before we have reached this point.
				batchLoadGuidQuery = "(NOT batch-load-guid:*)";
			}
			
			// Add batch load criteria we calculated above to the scope query
			parenScopeQuery = andExpressions(parenScopeQuery,batchLoadGuidQuery);
		}
		
		// Start out by building out headers
		fireMessage("Building headers...");
		sheet.setValue(0, 0, sheetConfig.getColPrimaryCategoryLabel());
		sheet.setStyle(0, 0, colPrimaryCategoryLabelStyle);
		
		sheet.setValue(1, 0, sheetConfig.getRowCategoryLabel());
		sheet.setStyle(1, 0, rowCategoryLabelStyle);
		
		for (int c = 0; c < sheetConfig.getColCriteria().size(); c++) {
			// First Row
			Style colCategoryStyleCopy = xlsx.createStyle();
			colCategoryStyleCopy.copy(colCategoryStyle);
			Color primaryCategoryColor = colCategoryColorRing.next();
			colCategoryStyleCopy.setForegroundColor(primaryCategoryColor);
			
			String colCriterion = sheetConfig.getColCriteria().get(c).getName();
			int col = 1 + (c * sheetConfig.getValueGenerators().size());
			sheet.setValue(0, col, colCriterion);
			
			sheet.setStyle(0, col, colCategoryStyleCopy);
			sheet.mergeCols(0, col, sheetConfig.getValueGenerators().size());
			
			// Second Row
			for (int sc = 0; sc < sheetConfig.getValueGenerators().size(); sc++) {
				int subCol = col + sc;
				
				// Secondary column headers are tinted colors of primary column header color
				Style colSecondaryCategoryStyleCopy = xlsx.createStyle();
				colSecondaryCategoryStyleCopy.copy(colCategoryStyle);
				colSecondaryCategoryStyleCopy.setTextWrapped(true);
				
				// We want to tint the primary color to some degree, 1.0 tends to blow out the color to white
				// so we will tint it relative.  For example if we have 4 columns we want roughly the first tinted 25%
				// then the next 50%, then 75% and finally 100%.  We subtract a little so that we never hit
				// 100% tint.
				float tintDegree = ((float)sc+1) / ((float)sheetConfig.getValueGenerators().size()) - 0.15f;
				
				Color primaryCategoryColorTint = AsposeCellsColorHelper.getTint(primaryCategoryColor, tintDegree);
				colSecondaryCategoryStyleCopy.setForegroundColor(primaryCategoryColorTint);
				
				String secondaryColumnLabel = sheetConfig.getValueGenerators().get(sc).getColumnLabel();
				sheet.setValue(1, subCol, secondaryColumnLabel);
				sheet.setStyle(1, subCol, colSecondaryCategoryStyleCopy);
			}
		}
		
		sheet.autoFitRow(2);
		sheet.setCurrentRow(sheet.getCurrentRow()+2);
		
		fireMessage("Building rows...");
		for (int r = 0; r < sheetConfig.getRowCriteria().size(); r++) {
			String parenRowCriterion = parenRowCriteria.get(r);
			
			rowValues.clear();
			rowValues.add(sheetConfig.getRowCriteria().get(r).getName());
			
			for(String parenColCriterion : parenColCriteria) {
				String query = andExpressions(parenScopeQuery, parenRowCriterion, parenColCriterion);
				logger.info(String.format("Query: %s", query));
				for(ColumnValueGenerator generator : sheetConfig.getValueGenerators()) {
					rowValues.add(generator.generateValue(nuixCase, query));
					currentCellIndex++;
					
					// Periodic progress
					if(System.currentTimeMillis() - lastProgress >= 1000 ){
						fireProgress(currentCellIndex, totalCells);
						lastProgress = System.currentTimeMillis();
					}
				}
			}
			
			sheet.appendRow(rowValues, rowGeneralStyle);
			
			// Apply styles
			sheet.setStyle(sheet.getCurrentRow()-1, 0, rowCategoryStyle);
		}
		
		fireMessage("Autofitting columns...");
		sheet.autoFitColumns();
		
		if(sheetConfig.getFreezePanes() == true) {
			// Freeze first column and top 2 rows
			sheet.getAsposeWorksheet().freezePanes(2, 1, 2, 1);	
		}
		
		fireMessage("Saving...");
		xlsx.save();
		fireMessage("Saved");
		
		long generationFinished = System.currentTimeMillis();
		long elapsedMillis = generationFinished - generationStarted;
		long elapsedSeconds = elapsedMillis / 1000;
		fireMessage("Sheet generated in "+FormatUtility.getInstance().secondsToElapsedString(elapsedSeconds));
	}

	public ColorRing getColCategoryColorRing() {
		return colCategoryColorRing;
	}

	public void setColCategoryColorRing(ColorRing colCategoryColorRing) {
		this.colCategoryColorRing = colCategoryColorRing;
	}
}
