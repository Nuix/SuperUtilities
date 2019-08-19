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
	
	private Map<String,Object> printSettings = new HashMap<String,Object>();
	
	/***
	 * Creates a new instance
	 * @param tempDirectory Temporary directory where PDF files will be saved to.
	 */
	public PdfWorkCache(File tempDirectory) {
		this.tempDirectory = tempDirectory;
		this.tempDirectory.mkdirs();
		pdfExporter =  SuperUtilities.getInstance().getNuixUtilities().getPdfPrintExporter();
		printSettings.put("regenerateStored", false);
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
			item.getPrintedImage().generate(printSettings); // Make sure PDF is generated or export can have issues
			pdfExporter.exportItem(item, tempPdf);
			pdfCache.put(item.getGuid(), tempPdf);
		}
		return tempPdf;
	}
	
	/***
	 * Removes PDF from cache and deletes it from the file system.  Should not error if:<br>
	 * - PDF file no longer exists<br>
	 * - There is no entry in cache for given item<br>
	 * @param item The item to "forget" (remove from cache and delete PDF file)
	 */
	public synchronized void forgetItem(Item item) {
		String guid = item.getGuid();
		if(pdfCache.containsKey(guid)) {
			File pdfFile = pdfCache.get(guid);
			if(pdfFile.exists()) {
				pdfFile.delete();
			}
			pdfCache.remove(guid);
		}
	}
	
	/***
	 * Calling this will delete all temporary PDFs created by this instance, as well as clear internal listing of PDFs and their associated
	 * file paths.
	 * @throws IOException If something goes wrong
	 */
	public synchronized void cleanupTemporaryPdfs() throws IOException {
		FileUtils.deleteDirectory(tempDirectory);
		pdfCache.clear();
	}
	
	public boolean getRegenerateStored() {
		return (Boolean)printSettings.get("regenerateStored");
	}
	
	public void setRegenerateStored(boolean value) {
		printSettings.put("regenerateStored",value);
	}
}
