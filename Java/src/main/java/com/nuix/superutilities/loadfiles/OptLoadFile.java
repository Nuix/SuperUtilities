package com.nuix.superutilities.loadfiles;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class OptLoadFile {
	List<OptRecord> records = new ArrayList<OptRecord>();
	
	public static OptLoadFile fromFile(String sourceFile) throws IOException {
		return fromFile(new File(sourceFile));
	}
	
	public static OptLoadFile fromFile(File sourceFile) throws IOException {
		OptLoadFile result = new OptLoadFile();
		SimpleTextFileReader.withEachLine(sourceFile, new Consumer<String>() {
			@Override
			public void accept(String line) { result.addRecord(OptRecord.fromLine(line)); }
		});
		return result;
	}
	
	public void toFile(File destinationFile) throws IOException {
		try(PrintWriter pw = new PrintWriter(new FileWriter(destinationFile))){
			for(OptRecord record : records) {
				pw.println(record.toLine());
			}
		}
	}
	
	public static void transpose(File sourceFile, File destinationFile, Consumer<OptRecord> recordModifier) throws IOException {
		try(PrintWriter pw = new PrintWriter(new FileWriter(destinationFile))){
			SimpleTextFileReader.withEachLine(sourceFile, new Consumer<String>() {
				@Override
				public void accept(String line) {
					OptRecord currentRecord = OptRecord.fromLine(line);
					recordModifier.accept(currentRecord);
					pw.println(currentRecord.toLine());
				}
			});
		}
	}
	
	public Set<String> getVolumeNames(){
		Set<String> result = new HashSet<String>();
		for(OptRecord record : records) {
			result.add(record.getVolume());
		}
		return result;
	}
	
	public List<OptRecord> getFirstPageRecords(){
		return records.stream().filter(r -> r.isFirstPage()).collect(Collectors.toList());
	}

	public List<OptRecord> getRecords() {
		return records;
	}

	public void setRecords(List<OptRecord> records) {
		this.records = records;
	}
	
	public void addRecord(OptRecord record) {
		this.records.add(record);
	}
}
