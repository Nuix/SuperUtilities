package com.nuix.superutilities.misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.nuix.superutilities.SuperUtilities;

import nuix.Case;
import nuix.Item;

public class DigestList implements Iterable<String> {
	
	private static Logger logger = Logger.getLogger(DigestList.class);
	
	class DigestIterator implements Iterator<String> {
		private Iterator<ByteBuffer> iter = null;
		
		@Override
		public boolean hasNext() {
			return iter.hasNext();
		}

		@Override
		public String next() {
			return FormatUtility.bytesToHex(iter.next().array());
		}
	}
	
	private TreeSet<ByteBuffer> digests = new TreeSet<ByteBuffer>();
	
	public static void combineDigestFiles(File outputFile, Collection<File> inputFiles) throws Exception {
		DigestList temp = new DigestList();
		for(File inputFile : inputFiles) {
			int importedCount = temp.importFile(inputFile);
			logger.info(String.format("Imported %s digest from %s", importedCount, inputFile));
		}
		logger.info(String.format("Temp digest now contains %s", temp.size()));
		temp.saveFile(outputFile);
	}
	
	public void saveFile(File digestListFile) throws Exception {
		try(FileOutputStream outputStream = new FileOutputStream(digestListFile)){
			outputStream.write("F2DL".getBytes());
			outputStream.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(1).array());
			outputStream.write(ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putShort((short) 3).array());
			outputStream.write("MD5".getBytes());
			int digestsWritten = 0;
			for(ByteBuffer digestBytes : digests) {
				outputStream.write(digestBytes.array());
				digestsWritten++;
			}
			logger.info(String.format("Digests written: %s", digestsWritten));
		}
	}
	
	public void saveCaseLevelDigestList(Case nuixCase, String name) throws Exception {
		File digestFile = new File(nuixCase.getLocation(),"Stores");
		digestFile = new File(digestFile,"UserData");
		digestFile = new File(digestFile,name+".hash");
		saveFile(digestFile);
	}
	
	public void saveUserLevelDigestList(String name) throws Exception {
		File digestFile = new File(System.getenv("APPDATA"),"Nuix");
		digestFile = new File(digestFile,"Digest Lists");
		digestFile = new File(digestFile,name+".hash");
		saveFile(digestFile);
	}
	
	public void saveSystemLevelDigestList(String name) throws Exception {
		File digestFile = new File(System.getenv("PROGRAMDATA"),"Nuix");
		digestFile = new File(digestFile,"Digest Lists");
		digestFile = new File(digestFile,name+".hash");
		saveFile(digestFile);
	}
	
	public int importFile(String digestListFile) throws Exception {
		return importFile(new File(digestListFile));
	}
	
	public int importFile(File digestListFile) throws Exception {
		int importedCount = 0;
		try(FileInputStream inputStream = new FileInputStream(digestListFile)){
			// Skip past the header
			inputStream.skip(13);
			// Read rest in 16 byte chunks (each is an MD5)
			byte[] buffer = new byte[16];
			while(inputStream.available() != 0) {
				inputStream.read(buffer);
				if(!containsMd5(buffer)) {
					addMd5(buffer);
					importedCount++;
				}
			}
		}
		return importedCount;
	}
	
	public int importCaseLevelDigestList(Case nuixCase, String name) throws Exception {
		File digestFile = new File(nuixCase.getLocation(),"Stores");
		digestFile = new File(digestFile,"UserData");
		digestFile = new File(digestFile,name+".hash");
		return importFile(digestFile);
	}
	
	public int importUserLevelDigestList(String name) throws Exception {
		File digestFile = new File(System.getenv("APPDATA"),"Nuix");
		digestFile = new File(digestFile,"Digest Lists");
		digestFile = new File(digestFile,name+".hash");
		return importFile(digestFile);
	}
	
	public int importSystemLevelDigestList(String name) throws Exception {
		File digestFile = new File(System.getenv("PROGRAMDATA"),"Nuix");
		digestFile = new File(digestFile,"Digest Lists");
		digestFile = new File(digestFile,name+".hash");
		return importFile(digestFile);
	}
	
	public DigestList() {}
	
	public void addItem(Item item) {
		String md5 = item.getDigests().getMd5();
		if(md5 != null && !md5.trim().isEmpty()) {
			addMd5(md5);	
		}
	}
	
	public void addItems(Collection<Item> items) {
		for(Item item : items) {
			addItem(item);
		}
	}
	
	public void removeItem(Item item) {
		String md5 = item.getDigests().getMd5();
		if(md5 != null && !md5.trim().isEmpty()) {
			removeMd5(md5);
		}
	}
	
	public void removeItems(Collection<Item> items) {
		for(Item item : items) {
			removeItem(item);
		}
	}
	
	public void addMd5(String md5) {
		byte[] md5Bytes = FormatUtility.hexToBytes(md5);
		addMd5(md5Bytes);
	}
	
	public void addMd5(byte[] md5Bytes) {
		digests.add(ByteBuffer.wrap(md5Bytes));
	}
	
	public void removeMd5(String md5) {
		byte[] md5Bytes = FormatUtility.hexToBytes(md5);
		removeMd5(md5Bytes);
	}
	
	public void removeMd5(byte[] md5Bytes) {
		digests.remove(ByteBuffer.wrap(md5Bytes));
	}
	
	public boolean containsMd5(byte[] md5Bytes) {
		return digests.contains(ByteBuffer.wrap(md5Bytes));
	}
	
	public boolean containsMd5(String md5) {
		return containsMd5(FormatUtility.hexToBytes(md5));
	}
	
	public int size() {
		return digests.size();
	}
	
	public Set<Item> findMatchingItems(Case nuixCase, int chunkSize) throws Exception {
		Set<Item> result = new HashSet<Item>();
		List<String> digestBuffer = new ArrayList<String>();
		
		// Iterate MD5s, when we have collected up chunkSize MD5s, we run a search and then union (merge) those items
		// into our result, clear our digest buffer and continue
		for(String md5 : this) {
			digestBuffer.add(md5);
			if(digestBuffer.size() >= chunkSize) {
				String guidCriteria = String.join(" OR ", digestBuffer);
				String query = String.format("md5:(%s)",guidCriteria);
				Set<Item> itemChunk = nuixCase.searchUnsorted(query); 
				result = SuperUtilities.getInstance().getNuixUtilities().getItemUtility().union(result, itemChunk);
				digestBuffer.clear();
			}
		}
		
		// Make sure we fetch any remaining
		if(digestBuffer.size() > 0) {
			String guidCriteria = String.join(" OR ", digestBuffer);
			String query = String.format("md5:(%s)",guidCriteria);
			Set<Item> itemChunk = nuixCase.searchUnsorted(query); 
			result = SuperUtilities.getInstance().getNuixUtilities().getItemUtility().union(result, itemChunk);
			digestBuffer.clear();
		}
		
		return result;
	}
	
	public Set<Item> findMatchingItems(Case nuixCase) throws Exception {
		return findMatchingItems(nuixCase,10000);
	}

	@Override
	public Iterator<String> iterator() {
		DigestIterator iterator = new DigestIterator();
		iterator.iter = digests.iterator();
		return iterator;
	}
}
