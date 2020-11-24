package com.nuix.superutilities.misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.jena.ext.com.google.common.primitives.Ints;

public class ZipHelper {
	public static void compressDirectoryToZipFile(String directory, String zipFile, int compressionLevel) throws IOException, FileNotFoundException {
		ZipOutputStream zipStream = null;
		try{
			zipStream = new ZipOutputStream(new FileOutputStream(zipFile));
			zipStream.setLevel(Ints.constrainToRange(compressionLevel, 0, 9));
			compressDirectoryToZipfile(directory,directory,zipStream);	
		} finally {
			IOUtils.closeQuietly(zipStream);	
		}
	}
	
	/***
	 * Recursively adds contents of a directory to a zip file.  Based on:
	 * https://stackoverflow.com/questions/23318383/compress-directory-into-a-zipfile-with-commons-io
	 * @param rootDir The root directory, removed to make relative paths in zip
	 * @param sourceDir The directory to archive
	 * @param out The zip output stream
	 * @throws IOException Thrown if there are issues with IO
	 * @throws FileNotFoundException Thrown is file is not found
	 */
	private static void compressDirectoryToZipfile(String rootDir, String sourceDir, ZipOutputStream out) throws IOException, FileNotFoundException {
	    for (File file : new File(sourceDir).listFiles()) {
	        if (file.isDirectory()) {
	            compressDirectoryToZipfile(rootDir, sourceDir + File.separator + file.getName(), out);
	        } else {
	            ZipEntry entry = new ZipEntry(sourceDir.replace(rootDir, "") + file.getName());
	            out.putNextEntry(entry);
	            FileInputStream in = new FileInputStream(sourceDir + "/" + file.getName());
	            IOUtils.copy(in, out);
	            IOUtils.closeQuietly(in);
	        }
	    }
	}
}
