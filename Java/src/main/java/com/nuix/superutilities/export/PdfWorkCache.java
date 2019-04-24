package com.nuix.superutilities.export;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.nuix.superutilities.SuperUtilities;

import nuix.Item;
import nuix.SingleItemExporter;

/***
 * This class provides access to PDFs as files when you need them on the file system to perform some operation.  Calls to {@link #getPdfPath(Item)} will
 * either provide a path to an already exported PDF for the given item or generate the PDF as needed.
 * @author Jason Wells
 *
 */
public class PdfWorkCache {
	private Map<String,File> pdfCache = new HashMap<String,File>();
	private File tempDirectory = null;
	private SingleItemExporter pdfExporter = null;
	
	/***
	 * Creates a new instance
	 * @param tempDirectory Temporary directory where PDF files will be saved to.
	 */
	public PdfWorkCache(File tempDirectory) {
		this.tempDirectory = tempDirectory;
		this.tempDirectory.mkdirs();
		pdfExporter =  SuperUtilities.getInstance().getNuixUtilities().getPdfPrintExporter();
	}
	
	/***
	 * Gets the path to a PDF exported for an item if it is already exported.  If the given item's PDF has not yet been exported, this will
	 * export a PDF for that item and then return the newly generated PDFs path.
	 * @param item The item you wish to obtain the PDF file path of
	 * @return Path to the PDF file for the given item
	 * @throws Exception If something goes wrong
	 */
	public synchronized File getPdfPath(Item item) throws Exception {
		File tempPdf = null;
		if(pdfCache.containsKey(item.getGuid())) {
			tempPdf = pdfCache.get(item.getGuid());
		} else {
			String guid = item.getGuid();
			tempPdf = new File(tempDirectory,guid.substring(0, 3));
			tempPdf = new File(tempPdf,guid.substring(3, 6));
			tempPdf.mkdirs();
			tempPdf = new File(tempPdf,item.getGuid()+".pdf");
			item.getPrintedImage().generate(); // Make sure PDF is generated or exported can have issues
			pdfExporter.exportItem(item, tempPdf);
			pdfCache.put(item.getGuid(), tempPdf);
		}
		return tempPdf;
	}
	
	/***
	 * Calling this will delete all temporary PDFs created by this instance, as well as clear internal listing of PDFs and their associated
	 * file paths.
	 * @throws IOException If something goes wrong
	 */
	public synchronized void cleanupTemporaryPdfs() throws IOException {
//		for(Map.Entry<String, File> pdfCacheEntry : pdfCache.entrySet()) {
//			if(pdfCacheEntry.getValue().exists()) {
//				pdfCacheEntry.getValue().delete();
//			}
//		}
		FileUtils.deleteDirectory(tempDirectory);
		pdfCache.clear();
	}
}
