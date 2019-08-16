package com.nuix.superutilities.misc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.GrayColor;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfGState;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;

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
	
	public static void createWaterMarkedPdf(String inputFile, String outputFile, String phrase, int fontSize, float opacity, float rotation) throws Exception {
		createWaterMarkedPdf(new File(inputFile), new File(outputFile), phrase, fontSize, opacity, rotation);
	}
	
	public static void createWaterMarkedPdf(File inputFile, File outputFile, String phrase, int fontSize, float opacity, float rotation) throws Exception {
		if(!inputFile.exists()) {
			throw new IllegalArgumentException("inputFile does not exist");
		}
		
		outputFile.getParentFile().mkdirs();
		
		// Based on code found here: https://memorynotfound.com/add-watermark-to-pdf-document-using-itext-and-java/
		PdfReader reader = new PdfReader(inputFile.getAbsolutePath());
		OutputStream outputStream = new FileOutputStream(outputFile);
		PdfStamper stamper = new PdfStamper(reader,outputStream);
		
		Font font = new Font(Font.FontFamily.HELVETICA, fontSize, Font.BOLD, new GrayColor(0.5f));
		
		Phrase p = new Phrase(phrase, font);
		
		PdfContentByte over;
        Rectangle pagesize;
        float x, y;
        
        int n = reader.getNumberOfPages();
        for (int i = 1; i <= n; i++) {

            // get page size and position
            pagesize = reader.getPageSizeWithRotation(i);
            x = (pagesize.getLeft() + pagesize.getRight()) / 2;
            y = (pagesize.getTop() + pagesize.getBottom()) / 2;
            over = stamper.getOverContent(i);
            over.saveState();

            // set transparency
            PdfGState state = new PdfGState();
            state.setFillOpacity(opacity);
            over.setGState(state);

            ColumnText.showTextAligned(over, Element.ALIGN_CENTER, p, x, y, rotation);
            over.restoreState();
        }
        stamper.close();
        reader.close();
	}
}
