package com.nuix.superutilities.loadfiles;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class SimpleTextFileReader {
    public static void withEachLine(File sourceFile, Consumer<String> consumer) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(sourceFile, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                consumer.accept(line);
            }
        }
    }
}
