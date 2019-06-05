package com.nuix.superutilities.loadfiles;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

public class DatLoadFileReader {
	public static void withEachRecord(File sourceFile, Consumer<LinkedHashMap<String,String>> recordConsumer) throws IOException {
		SimpleTextFileReader.withEachLine(sourceFile, new Consumer<String>() {
			String[] headers = null;
			@Override
			public void accept(String line) {
				if(headers == null) {
					headers = DatLoadFile.splitLine(line);
				} else {
					recordConsumer.accept(DatLoadFile.fromLine(headers, line));
				}
			}
		});
	}
}
