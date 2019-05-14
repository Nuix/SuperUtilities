package com.nuix.superutilities.loadfiles;

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class SimpleTextFileWriter implements Closeable {
	protected File destinationFile;
	protected PrintWriter pw;
	protected FileWriter fw;
	
	public SimpleTextFileWriter(File destinationFile) throws IOException {
		this.destinationFile = destinationFile;
		fw = new FileWriter(destinationFile);
		pw = new PrintWriter(fw);
	}
	
	public void writeLine(String line) {
		pw.println(line);
	}

	public File getDestinationFile() {
		return destinationFile;
	}
	
	@Override
	public void close() throws IOException {
		if(pw != null) { pw.close(); }
		if(fw != null) { fw.close(); }
	}
}
