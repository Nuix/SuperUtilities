package com.nuix.superutilities.reporting;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.aspose.cells.Style;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;

public class SimpleXlsx {
	public static void initializeAspose() throws Exception {
		String[] potentialAsposeLocations = new String[]{
				"com.nuix.data.util.aspose.AsposeCells",
				"com.nuix.util.aspose.AsposeCells",
				"com.nuix.util.AsposeCells",
		};
		boolean foundAspose = false;
		
		for(String packageToTry : potentialAsposeLocations){
			if(foundAspose){ break; }
			try {
				Class<?> clazz = Class.forName(packageToTry);
				Method method = clazz.getMethod("ensureInitialised");
				method.invoke(null);
				foundAspose = true;
			} catch (ClassNotFoundException e) {}
		}
		
		if(!foundAspose){
			throw new Exception("Couldn't initialize Aspose, this version of the script may not be compatible with current version of Nuix");
		}
	}
	
	private File file = null;
	private Workbook workbook = null;
	@SuppressWarnings("unused")
	private Map<String,Style> createdStyles = new HashMap<String,Style>();
	
	public SimpleXlsx(String file) throws Exception {
		initializeAspose();
		this.file = new File(file);
		if(this.file.exists()) {
			workbook = new Workbook(file);
		} else {
			workbook = new Workbook();
			workbook.getWorksheets().removeAt("Sheet1");
		}
	}
	
	public void saveAs(String file) throws Exception {
		workbook.save(file);
	}
	
	public void save() throws Exception {
		workbook.save(file.getAbsolutePath());
	}
	
	public SimpleWorksheet getSheet(String name) {
		Worksheet worksheet = workbook.getWorksheets().get(name);
		if(worksheet == null) {
			worksheet = workbook.getWorksheets().add(name);
		}
		return new SimpleWorksheet(this,worksheet);
	}
	
	public Workbook getAsposeWorkbook() {
		return workbook;
	}
}
