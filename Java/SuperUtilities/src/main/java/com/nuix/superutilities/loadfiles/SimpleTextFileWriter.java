package com.nuix.superutilities.loadfiles;

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SimpleTextFileWriter implements Closeable {
    protected File destinationFile;
    protected FileWriter fw;

    public SimpleTextFileWriter(File destinationFile) throws IOException {
        this.destinationFile = destinationFile;
        fw = new FileWriter(destinationFile, StandardCharsets.UTF_8);
    }

    public void writeLine(String line) throws IOException {
        fw.write(line + "\n");
    }

    public File getDestinationFile() {
        return destinationFile;
    }

    @Override
    public void close() throws IOException {
        if (fw != null) {
            fw.close();
        }
    }
}
