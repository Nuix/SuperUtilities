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
	/***
	 * Compresses the contents of the given directory (files and sub-directories) in to a Zip file.
	 * @param directory The directory to archive into the Zip file.
	 * @param zipFile Where to write the Zip file.
	 * @param compressionLevel What level of compression to use between 0 and 9, with 0 being no compression and 9 being the most compression.
	 * @throws IOException Thrown if there are issues with IO
	 * @throws FileNotFoundException Thrown if file is not found
	 */
	@SuppressWarnings("deprecation")
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
	 * @throws FileNotFoundException Thrown if file is not found
	 */
	@SuppressWarnings("deprecation")
	private static void compressDirectoryToZipfile(String rootDir, String sourceDir, ZipOutputStream out) throws IOException, FileNotFoundException {
	    for (File file : new File(sourceDir).listFiles()) {
	        if (file.isDirectory()) {
	            compressDirectoryToZipfile(rootDir, sourceDir + File.separator + file.getName(), out);
	        } else {
	            ZipEntry entry = new ZipEntry(sourceDir.replace(rootDir, "") + File.separator + file.getName());
	            out.putNextEntry(entry);
	            FileInputStream in = new FileInputStream(sourceDir + File.separator + file.getName());
	            IOUtils.copy(in, out);
	            IOUtils.closeQuietly(in);
	        }
	    }
	}
}
