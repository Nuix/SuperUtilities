package com.nuix.superutilities.misc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfReader;

/***
 * A class containing some helper methods for PDFs.
 * @author Jason Wells
 *
 */
public class PdfUtility {
	/***
	 * Merges multiple PDF files into a single PDF file.  Optionally can create book marks corresponding to the beginning page of
	 * each constituent PDFs. 
	 * @param outputFile File path for merged PDF which will be generated
	 * @param inputFiles The input files to be merged
	 * @param createBookmarks Whether to create book marks on each page which represent the first page of each input PDF 
	 * @param bookmarkTitles Optional list of book mark titles, if not provided sequential numbers will be used.
	 * @throws Exception Thrown if an error occurs
	 */
	public static void mergePdfFiles(File outputFile, List<File> inputFiles, boolean createBookmarks, List<String> bookmarkTitles) throws Exception {
		try {
			outputFile.getParentFile().mkdirs();
		} catch (Exception e) {
			throw new Exception("Error while creating directory for output file",e);
		}
		
		PdfReader reader = null;
		int pageOffset = 0;
		List<HashMap<String,Object>> bookmarkData = new ArrayList<HashMap<String,Object>>();
		
		Document document = new Document();
		OutputStream outputStream = new FileOutputStream(outputFile);
		PdfCopy copy = new PdfCopy(document, outputStream);
		document.open();
		
		int inputFileIndex = 0;
		for(File inputFile : inputFiles){
			reader = new PdfReader(inputFile.getPath());
			int pages = reader.getNumberOfPages();
			for (int i = 0; i < pages; i++) {
				copy.addPage(copy.getImportedPage(reader, i+1));
			}
			copy.freeReader(reader);
			reader.close();
			
			if (createBookmarks) {
				String title = FormatUtility.getInstance().formatNumber(inputFileIndex+1);
				if(bookmarkTitles != null && inputFileIndex < bookmarkTitles.size()){
					title = bookmarkTitles.get(inputFileIndex);
				}
				HashMap<String,Object> bookmark = new HashMap<String,Object>();
				bookmark.put("Title", title);
				bookmark.put("Action", "GoTo");
				bookmark.put("Page", (pageOffset+1)+" Fit");
				bookmarkData.add(bookmark);
			}
			pageOffset += pages;
			
			inputFileIndex++;
		}
		
		if(createBookmarks){
			copy.setOutlines(bookmarkData);
		}
		
		document.close();
		copy.close();
		outputStream.close();
	}
	
	/***
	 * Merges multiple PDF files into a single PDF file.  Optionally can create book marks corresponding to the beginning page of
	 * each constituent PDFs. 
	 * @param outputFile File path for merged PDF which will be generated
	 * @param inputFiles The input files to be merged
	 * @param createBookmarks Whether to create book marks on each page which represent the first page of each input PDF 
	 * @param bookmarkTitles Optional list of book mark titles, if not provided sequential numbers will be used.
	 * @throws Exception Thrown if an error occurs
	 */
	public static void mergePdfFiles(String outputFile, List<String> inputFiles, boolean createBookmarks, List<String> bookmarkTitles) throws Exception {
		File jOutputFile = new File(outputFile);
		List<File> jInputFiles = new ArrayList<File>();
		for(String inputFile : inputFiles){
			jInputFiles.add(new File(inputFile));
		}
		mergePdfFiles(jOutputFile,jInputFiles,createBookmarks,bookmarkTitles);
	}
}
