package com.nuix.superutilities.misc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
import com.nuix.superutilities.SuperUtilities;
import com.nuix.superutilities.export.PdfWorkCache;

import nuix.Item;
import nuix.SingleItemImporter;

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
	
	/***
	 * Takes a given source PDF file, creates a copy output PDF file in which a watermark has been applied.
	 * @param inputFile The source PDF file
	 * @param outputFile The destination PDF file
	 * @param phrase The water mark's phrase
	 * @param fontSize The font size of the water mark
	 * @param opacity How transparent the water mark is
	 * @param rotation How rotated the water mark text should be
	 * @throws Exception Thrown if: Input file does not exist, error creating stream to output file, error creating PDFReader or PDFStamper.
	 */
	public static void createWaterMarkedPdf(String inputFile, String outputFile, String phrase, int fontSize, float opacity, float rotation) throws Exception {
		createWaterMarkedPdf(new File(inputFile), new File(outputFile), phrase, fontSize, opacity, rotation);
	}
	
	/***
	 * Takes a given source PDF file, creates a copy output PDF file in which a watermark has been applied.
	 * @param inputFile The source PDF file
	 * @param outputFile The destination PDF file
	 * @param phrase The water mark's phrase
	 * @param fontSize The font size of the water mark
	 * @param opacity How transparent the water mark is
	 * @param rotation How rotated the water mark text should be
	 * @throws Exception Thrown if: Input file does not exist, error creating stream to output file, error creating PDFReader or PDFStamper.
	 */
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
	
	/***
	 * This method applies water marks to printed images of items.  Each item has a PDF exported, from
	 * which a water marked copy is generated.  The water marked copy is then imported back in to
	 * Nuix as the new printed image of the given item.
	 * @param tempDirectory Temp directory PDFs are exported to, generated in and imported from.
	 * @param items The items which will be water marked.
	 * @param phrase The water mark's phrase
	 * @param fontSize The font size of the water mark
	 * @param opacity How transparent the water mark is
	 * @param rotation How rotated the water mark text should be
	 * @param progressCallback A BiConsumer function which will be invoked as progress is made.  Will be provided 2 integers,
	 * the first is the current progress value, the second the total progress.
	 * @throws Exception Thrown if something goes wrong.
	 */
	public static void waterMarkPrintedImages(String tempDirectory, Collection<Item> items, String phrase, int fontSize, float opacity, float rotation, BiConsumer<Integer,Integer> progressCallback) throws Exception {
		waterMarkPrintedImages(new File(tempDirectory),items,phrase,fontSize,opacity,rotation,progressCallback);
	}
	
	/***
	 * This method applies water marks to printed images of items.  Each item has a PDF exported, from
	 * which a water marked copy is generated.  The water marked copy is then imported back in to
	 * Nuix as the new printed image of the given item.
	 * @param tempDirectory Temp directory PDFs are exported to, generated in and imported from.
	 * @param items The items which will be water marked.
	 * @param phrase The water mark's phrase
	 * @param fontSize The font size of the water mark
	 * @param opacity How transparent the water mark is
	 * @param rotation How rotated the water mark text should be
	 * @param progressCallback A BiConsumer function which will be invoked as progress is made.  Will be provided 2 integers,
	 * the first is the current progress value, the second the total progress.
	 * @throws Exception Thrown if something goes wrong.
	 */
	public static void waterMarkPrintedImages(File tempDirectory, Collection<Item> items, String phrase, int fontSize, float opacity, float rotation, BiConsumer<Integer,Integer> progressCallback) throws Exception {
		PdfWorkCache pdfCache = new PdfWorkCache(tempDirectory);
		File resultDirectory = new File(tempDirectory,"WaterMarkedPDFs");
		SingleItemImporter importer = SuperUtilities.getInstance().getNuixUtilities().getPdfPrintImporter();
		
		int totalItems = items.size();
		AtomicInteger itemIndex = new AtomicInteger(0);
		AtomicLong lastProgress = new AtomicLong(System.currentTimeMillis());
		Consumer<Item> consumer = new Consumer<Item>() {
			@Override
			public void accept(Item item) {
				int index = itemIndex.addAndGet(1);
				
				File outputFile;
				try {
					File sourceFile = pdfCache.getPdfPath(item);
					outputFile = new File(resultDirectory,item.getGuid()+".pdf");
					createWaterMarkedPdf(sourceFile,outputFile,phrase,fontSize,opacity,rotation);
					importer.importItem(item, outputFile);
					outputFile.delete();
					pdfCache.forgetItem(item);
					long elapsedMillis = System.currentTimeMillis() - lastProgress.get();
					if(progressCallback != null && elapsedMillis >= 1000) {
						lastProgress.set(System.currentTimeMillis());
						progressCallback.accept(index, totalItems);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		
		ForkJoinPool pool = null;
		try {
			pool = new ForkJoinPool(4);
			pool.submit(()->{
				items.parallelStream().forEach(consumer);
			}).get();
		} catch (Exception e) {
			throw e;
		} finally {
			if(pool != null)
				pool.shutdown();
		}
		
		pdfCache.cleanupTemporaryPdfs();
	}
}
