package com.nuix.superutilities.loadfiles;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class DatLoadFile {
	private static String delimiter = "\u0014";
	private static String quote = "\u00FE";
	private static String newlineEscape = "\u00AE";
	
	private static Pattern quoteTrimmer = null;
	private static Pattern newlineDecoder = null;
	
	static {
		quoteTrimmer = Pattern.compile("(^"+quote+")|("+quote+"$)");
		newlineDecoder = Pattern.compile(newlineEscape);
	}
	
	public static String[] splitLine(String line) {
		String[] columnValues = line.trim().split(delimiter, -1);
		for (int i = 0; i < columnValues.length; i++) {
			columnValues[i] = quoteTrimmer.matcher(columnValues[i]).replaceAll("");
			columnValues[i] = newlineDecoder.matcher(columnValues[i]).replaceAll("\n");
		}
		return columnValues;
	}
	
	public static LinkedHashMap<String,String> fromLine(String[] headers, String line){
		LinkedHashMap<String,String> result = new LinkedHashMap<String,String>();
		String[] values = splitLine(line);
		for (int i = 0; i < headers.length; i++) {
			result.put(headers[i], values[i]);
		}
		return result;
	}
	
	public static String toLine(LinkedHashMap<String,String> record) {
		StringJoiner result = new StringJoiner(delimiter);
		for(Map.Entry<String, String> col : record.entrySet()) {
			result.add(quote+col.getValue().replaceAll("\\r?\\n", newlineEscape)+quote);
		}
		return result.toString();
	}
	
	public static String toLine(String[] values) {
		StringJoiner result = new StringJoiner(delimiter);
		for(String value : values) {
			result.add(quote+value.replaceAll("\\r?\\n", newlineEscape)+quote);
		}
		return result.toString();
	}
	
	public static String toHeaderLine(LinkedHashMap<String,String> record) {
		StringJoiner result = new StringJoiner(delimiter);
		for(Map.Entry<String, String> col : record.entrySet()) {
			result.add(quote+col.getKey()+quote);
		}
		return result.toString();
	}
	
	public static String toHeaderLine(List<String> headers) {
		StringJoiner result = new StringJoiner(delimiter);
		for(String header : headers) {
			result.add(quote+header+quote);
		}
		return result.toString();
	}
	
	public static List<String> getHeadersFromRecord(LinkedHashMap<String,String> record){
		List<String> result = new ArrayList<String>();
		for(Map.Entry<String, String> col : record.entrySet()) {
			result.add(col.getKey());
		}
		return result;
	}
	
	public static void transpose(File sourceFile, File destinationFile, Consumer<LinkedHashMap<String,String>> recordModifier) throws IOException {
		try(DatLoadFileWriter datWriter = new DatLoadFileWriter(destinationFile)){
			DatLoadFileReader.withEachRecord(sourceFile, new Consumer<LinkedHashMap<String,String>>() {
				boolean headersWrittern = false;
				@Override
				public void accept(LinkedHashMap<String, String> record) {
					if(headersWrittern == false) {
						datWriter.writeRecordKeys(record);
						headersWrittern = true;
					} else {
						recordModifier.accept(record);
						datWriter.writeRecordValues(record);
					}
				}
			});
		}
	}
}
