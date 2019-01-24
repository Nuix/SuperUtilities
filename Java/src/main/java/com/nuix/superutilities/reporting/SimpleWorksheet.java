package com.nuix.superutilities.reporting;

import java.util.List;
import java.util.function.Consumer;

import com.aspose.cells.Cell;
import com.aspose.cells.Style;
import com.aspose.cells.Worksheet;

/***
 * Wrapper class around Aspose Cells Worksheet object which simplifies some of the
 * more common operations.
 * @author Jason Wells
 *
 */
public class SimpleWorksheet {
	@SuppressWarnings("unused")
	private SimpleXlsx owningXlsx = null;
	private Worksheet worksheet = null;
	
	private int currentRow = 0;
	
	/***
	 * Creates a new instance.
	 * @param owningXlsx The {@link SimpleXlsx} this sheet is associated with.
	 * @param worksheet The Aspose Cells Worksheet object this is wrapping around.
	 */
	SimpleWorksheet(SimpleXlsx owningXlsx, Worksheet worksheet){
		this.owningXlsx = owningXlsx;
		this.worksheet = worksheet;
	}
	
	/***
	 * Gets the underlying Aspose Cells Worksheet object.
	 * @return The underlying Aspose Cells Worksheet object.
	 */
	public Worksheet getAsposeWorksheet() {
		return worksheet;
	}
	
	/***
	 * Gets a particular Cell object from this work sheet.
	 * @param row The 0 based row index
	 * @param col The 0 based column index
	 * @return The underlying Aspose Cells Cell object at the given indices
	 */
	public Cell getCell(int row, int col) {
		return worksheet.getCells().get(row,col);
	}
	
	/***
	 * Gets the value of a particular cell in this work sheet
	 * @param row The 0 based row index
	 * @param col The 0 based column index
	 * @return The value of the cell at the given indices
	 */
	public Object getValue(int row, int col) {
		return getCell(row,col).getValue();
	}
	
	/***
	 * Sets the value of a particular cell in this work sheet
	 * @param row The 0 based row index
	 * @param col The 0 based column index
	 * @param value The value to assign to the cell at the given indices
	 */
	public void setValue(int row, int col, Object value) {
		getCell(row,col).setValue(value);
	}
	
	/***
	 * Sets the style of of a particular cell in this work sheet
	 * @param row The 0 based row index
	 * @param col The 0 based column index
	 * @param style The Style to apply to the cell at the given indices
	 */
	public void setStyle(int row, int col, Style style) {
		getCell(row, col).setStyle(style);
	}
	
	/***
	 * Gets the style of a particular cell in this work sheet
	 * @param row The 0 based row index
	 * @param col The 0 based column index
	 * @return The Style of the cell at the given indices
	 */
	public Style getStyle(int row, int col) {
		return getCell(row,col).getStyle();
	}
	
	/***
	 * Merges a series of cells horizontally
	 * @param row The 0 based row index
	 * @param col The 0 based column index of the first column in the merged group of cells.
	 * @param colCount How many cells total to merge, so if you want the 2 cells following the first cell specified by the indices, provide a value of 3.
	 */
	public void mergeCols(int row, int col, int colCount) {
		worksheet.getCells().merge(row, col, 1, colCount);
	}
	
	public void autoFitColumns() throws Exception {
		worksheet.autoFitColumns();
	}
	
	public void autoFitRow(int row) throws Exception {
		worksheet.autoFitRow(row);
	}
	
	public void appendRow(List<Object> rowValues) {
		for (int c = 0; c < rowValues.size(); c++) {
			Object rowValue = rowValues.get(c);
			setValue(currentRow,c,rowValue);
		}
		currentRow++;
	}
	
	public void appendRow(List<Object> rowValues, Style defaultStyle) {
		for (int c = 0; c < rowValues.size(); c++) {
			Object rowValue = rowValues.get(c);
			setValue(currentRow,c,rowValue);
			setStyle(currentRow,c,defaultStyle);
		}
		currentRow++;
	}
	
	public void eachCell(int startRow, int endRow, int startCol, int endCol, Consumer<Cell> activity) {
		for (int r = startRow; r <= endRow; r++) {
			for (int c = startCol; c <= endCol; c++) {
				activity.accept(getCell(r,c));
			}
		}
	}

	public int getCurrentRow() {
		return currentRow;
	}

	public void setCurrentRow(int currentRow) {
		this.currentRow = currentRow;
	}
}
