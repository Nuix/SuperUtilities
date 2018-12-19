package com.nuix.superutilities.reporting;

import java.util.List;

import com.aspose.cells.Cell;
import com.aspose.cells.Worksheet;

public class SimpleWorksheet {
	@SuppressWarnings("unused")
	private SimpleXlsx owningXlsx = null;
	private Worksheet worksheet = null;
	
	private int currentRow = 0;
	
	SimpleWorksheet(SimpleXlsx owningXlsx, Worksheet worksheet){
		this.owningXlsx = owningXlsx;
		this.worksheet = worksheet;
	}
	
	public Worksheet getAsposeWorksheet() {
		return worksheet;
	}
	
	public Cell getCell(int row, int col) {
		return worksheet.getCells().get(row,col);
	}
	
	public Object getValue(int row, int col) {
		return getCell(row,col).getValue();
	}
	
	public void setValue(int row, int col, Object value) {
		getCell(row,col).setValue(value);
	}
	
	public void mergeCols(int row, int col, int colCount) {
		worksheet.getCells().merge(row, col, 1, colCount);
	}
	
	public void autoFitColumns() throws Exception {
		worksheet.autoFitColumns();
	}
	
	public void appendRow(List<Object> rowValues) {
		for (int c = 0; c < rowValues.size(); c++) {
			Object rowValue = rowValues.get(c);
			setValue(currentRow,c,rowValue);
		}
		currentRow++;
	}

	public int getCurrentRow() {
		return currentRow;
	}

	public void setCurrentRow(int currentRow) {
		this.currentRow = currentRow;
	}
}
