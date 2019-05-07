package com.nuix.superutilities.loadfiles;

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;

public class DatLoadFileWriter implements Closeable {
	private File destinationFile;
	private PrintWriter pw;
	private FileWriter fw;
	
	public DatLoadFileWriter(File destinationFile) throws IOException {
		this.destinationFile = destinationFile;
		fw = new FileWriter(destinationFile);
		pw = new PrintWriter(fw);
	}
	
	public void writeLine(String[] values) {
		pw.println(DatLoadFile.toLine(values));
	}
	
	public void writeRecord(LinkedHashMap<String,String> record) {
		pw.println(DatLoadFile.toLine(record));
	}
	
	public void writeHeaders(LinkedHashMap<String,String> record) {
		pw.println(DatLoadFile.toHeaderLine(record));
	}

	@Override
	public void close() throws IOException {
		if(pw != null) { pw.close(); }
		if(fw != null) { fw.close(); }
	}

	public File getDestinationFile() {
		return destinationFile;
	}
}
