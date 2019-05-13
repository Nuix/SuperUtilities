package com.nuix.superutilities.loadfiles;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

public class DatLoadFileWriter extends SimpleTextFileWriter {
	public DatLoadFileWriter(File destinationFile) throws IOException {
		super(destinationFile);
	}
	
	public void writeDatLine(String[] values) {
		writeLine(DatLoadFile.toLine(values));
	}
	
	public void writeRecordValues(LinkedHashMap<String,String> record) {
		writeLine(DatLoadFile.toLine(record));
	}
	
	public void writeRecordKeys(LinkedHashMap<String,String> record) {
		writeLine(DatLoadFile.toHeaderLine(record));
	}
	
	public void writeValues(List<String> headers) {
		writeLine(DatLoadFile.toHeaderLine(headers));
	}
}
