package com.nuix.superutilities.misc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZMethod;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.io.IOUtils;

public class ZipHelper {
	
	public static void compressDirectoryToSevenZipFile(String directory, String sevenZipFile) throws IOException {
		SevenZOutputFile sevenZipOutput = new SevenZOutputFile(new File(sevenZipFile));
		sevenZipOutput.setContentCompression(SevenZMethod.DEFLATE);
		compressDirectoryToSevenZipFile(directory, directory, sevenZipOutput);
		sevenZipOutput.finish();
		sevenZipOutput.close();
	}
	
	private static void compressDirectoryToSevenZipFile(String rootDir, String sourceDir, SevenZOutputFile sevenZipOutput) throws IOException {
		for (File file : new File(sourceDir).listFiles()) {
	        if (file.isDirectory()) {
	        	compressDirectoryToSevenZipFile(rootDir, sourceDir + File.separator + file.getName(), sevenZipOutput);
	        } else {
	        	String dir = sourceDir.replace(rootDir, "");
	        	SevenZArchiveEntry entry = null;
	        	if (!dir.trim().isEmpty()) {
	        		entry = sevenZipOutput.createArchiveEntry(file,dir + File.separator + file.getName());	
	        	} else {
	        		entry = sevenZipOutput.createArchiveEntry(file,file.getName());
	        	}
	        	sevenZipOutput.putArchiveEntry(entry);
	        	FileInputStream in = new FileInputStream(sourceDir + File.separator + file.getName());
	            BufferedInputStream bufferedIn = new BufferedInputStream(in);
	        	sevenZipOutput.write(bufferedIn);
	        	bufferedIn.close();
	        	in.close();
	        	sevenZipOutput.closeArchiveEntry();
	        }
		}
	}
	
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
		FileOutputStream fileOutStream = null;
		BufferedOutputStream bufferedOutStream = null;
		try{
			fileOutStream = new FileOutputStream(zipFile);
			bufferedOutStream = new BufferedOutputStream(fileOutStream);
			zipStream = new ZipOutputStream(bufferedOutStream);
			zipStream.setLevel(clamp(compressionLevel, 0, 9));

			compressDirectoryToZipfile(directory,directory,zipStream);
		} finally {
			IOUtils.closeQuietly(zipStream);
		}
	}

	private static int clamp(int value, int min, int max){
		return Math.max(min, Math.min(max, value));
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
	        	String dir = sourceDir.replace(rootDir, "");
	        	ZipEntry entry;
	        	if (!dir.trim().isEmpty()) {
	        		entry = new ZipEntry(dir + File.separator + file.getName());	
	        	} else {
	        		entry = new ZipEntry(file.getName());
	        	}
	            
	            out.putNextEntry(entry);
	            FileInputStream in = new FileInputStream(sourceDir + File.separator + file.getName());
	            BufferedInputStream bufferedIn = new BufferedInputStream(in);
	            IOUtils.copy(bufferedIn, out);
	            IOUtils.closeQuietly(in);
	        }
	    }
	}
}
