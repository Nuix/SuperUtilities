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
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.nuix.superutilities.SuperUtilities;

import nuix.Case;
import nuix.Item;

/***
 * This class provides an in-memory representation of a Nuix binary digest list.  This class is capable of loading Nuix binary digest lists into memory,
 * modify digests present (add, remove, import) and saving out a new Nuix binary digest list.<br><br>
 * <b>Note:</b> When saving a digest list to one of the directories that Nuix looks for digest lists, Nuix may not immediately recognize the presence
 * of that new digest list until all workbench tabs are closed and re-opened, case is closed and reopened, etc.
 * @author Jason Wells
 *
 */
public class DigestList implements Iterable<String> {
	
	private static Logger logger = Logger.getLogger(DigestList.class);
	
	// Wrapper around TreeSet<ByteBuffer> iterator that yields hexadecimal string representation
	// of digest values based on byte[] backing each given ByteBuffer.
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
	
	// Digests are stored in this TreeSet.  A TreeSet is used so that iteration will yield back MD5s in order.
	// ByteBuffers wrap each MD5 byte[] because ByteBuffer provides usable equals/hashCode methods for the underlying
	// byte[] they wrap around.
	private TreeSet<ByteBuffer> digests = new TreeSet<ByteBuffer>();
	
	/***
	 * Saves a new Nuix binary digest list based on the digests stored in memory of this instance.  Will overwrite existing files.
	 * @param digestListFile The location to which to save the new digest list.
	 * @throws Exception Thrown most likely if there are IO errors.
	 */
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
	
	/***
	 * Imports digests found in given input file to this instance (if not already present).
	 * @param digestListFile The location of the input Nuix binary digest list file.
	 * @return How many digests were imported from the given input Nuix binary digest list file
	 * that were not already present in this instance.
	 * @throws Exception Thrown most likely due to IO errors.
	 */
	public int importFile(String digestListFile) throws Exception {
		return importFile(new File(digestListFile));
	}
	
	/***
	 * Create a new digest list file from multiple source digest lists.  A convenience method for the process of
	 * creating a new DigestList instance, importing each source digest list and then saving the combined imported
	 * results to a new output digest list.
	 * @param outputFile Location to save output Nuix binary digest list file.
	 * @param inputFiles Locations of source Nuix binary digest list files.
	 * @throws Exception Thrown if an exception occurs while importing or saving combined result.
	 */
	public static void combineDigestFiles(File outputFile, Collection<File> inputFiles) throws Exception {
		DigestList temp = new DigestList();
		for(File inputFile : inputFiles) {
			int importedCount = temp.importFile(inputFile);
			logger.info(String.format("Imported %s digest from %s", importedCount, inputFile));
		}
		logger.info(String.format("Temp digest now contains %s", temp.size()));
		temp.saveFile(outputFile);
	}
	
	/***
	 * Create a new digest list file from multiple source digest lists.  A convenience method for the process of
	 * creating a new DigestList instance, importing each source digest list and then saving the combined imported
	 * results to a new output digest list.
	 * @param outputFile Location to save output Nuix binary digest list file.
	 * @param inputFiles Locations of source Nuix binary digest list files.
	 * @throws Exception Thrown if an exception occurs while importing or saving combined result.
	 */
	public static void combineDigestFiles(String outputFile, Collection<String> inputFiles) throws Exception {
		combineDigestFiles(new File(outputFile),inputFiles.stream().map(p -> new File(p)).collect(Collectors.toList()));
	}
	
	/***
	 * Provides a way to iterate each MD5 string present in a given digest list file without first reading
	 * the entirety of that digest list file into memory.
	 * @param sourceDigestList The Nuix binary digest list file to read MD5 digests from.
	 * @param md5StringConsumer Callback which will receive each MD5 as a string as it is read from the source file.
	 * @throws Exception Most likely thrown if there are IO errors while reading from the source file.
	 */
	public static void eachDigest(File sourceDigestList, Consumer<String> md5StringConsumer) throws Exception {
		try(FileInputStream inputStream = new FileInputStream(sourceDigestList)){
			// Skip past the header
			inputStream.skip(13);
			while(inputStream.available() != 0) {
				// Read rest in 16 byte chunks (each is an MD5)
				byte[] buffer = new byte[16];
				inputStream.read(buffer);
				String md5 = FormatUtility.bytesToHex(buffer);
				md5StringConsumer.accept(md5);
			}
		}
	}
	
	/***
	 * Returns a count of digests present in the given Nuix binary digest list file.  This is really just a convenience
	 * for the calculation:<br><br>
	 * DIGEST_COUNT = (FILE_SIZE_BYTES - 13) / 16
	 * @param sourceDigestList The digest list file to calculate the digest count of.
	 * @return The number of digest lists present in the given file based on file size in bytes.
	 */
	public static int getDigestCount(File sourceDigestList) {
		long sourceSizeBytes = sourceDigestList.length();
		if(sourceSizeBytes < 13) { return 0; }
		else {
			// Header is 13 bytes long
			return ((int)(sourceSizeBytes - 13) / 16);
		}
	}
	
	/***
	 * Imports digests found in given input file to this instance (if not already present).
	 * @param digestListFile The location of the input Nuix binary digest list file.
	 * @return How many digests were imported from the given input Nuix binary digest list file
	 * that were not already present in this instance.
	 * @throws Exception Thrown most likely due to IO errors.
	 */
	public int importFile(File digestListFile) throws Exception {
		int importedCount = 0;
		try(FileInputStream inputStream = new FileInputStream(digestListFile)){
			// Skip past the header
			inputStream.skip(13);
			while(inputStream.available() != 0) {
				// Read rest in 16 byte chunks (each is an MD5)
				byte[] buffer = new byte[16];
				inputStream.read(buffer);
				if(!containsMd5(buffer)) {
					addMd5(buffer);
					importedCount++;
				}
			}
		}
		return importedCount;
	}
	
	/***
	 * Saves a new digest list to a file located at "[CASE_DIRECTORY]\Stores\User Data\Digest Lists\[NAME].hash" (case level digest list location).
	 * If digest list file already exists, it will be overwritten.
	 * @param nuixCase The case to save the digest relative to.
	 * @param name The name of the digest list to save.
	 * @throws Exception Thrown if there is an error while saving.
	 */
	public void saveCaseLevelDigestList(Case nuixCase, String name) throws Exception {
		File digestFile = new File(nuixCase.getLocation(),"Stores");
		digestFile = new File(digestFile,"User Data");
		digestFile = new File(digestFile,"Digest Lists");
		digestFile = new File(digestFile,name+".hash");
		saveFile(digestFile);
	}
	
	/***
	 * Saves a new digest list to a file located at "%appdata%\Nuix\Digest Lists\[NAME].hash" (user level digest list location).
	 * If digest list file already exists, it will be overwritten.
	 * @param name The name of the digest list to save.
	 * @throws Exception Thrown if there is an error while saving.
	 */
	public void saveUserLevelDigestList(String name) throws Exception {
		File digestFile = new File(System.getenv("APPDATA"),"Nuix");
		digestFile = new File(digestFile,"Digest Lists");
		digestFile = new File(digestFile,name+".hash");
		saveFile(digestFile);
	}
	
	/***
	 * Saves a new digest list to a file located at "%programdata%\Nuix\Digest Lists\[NAME].hash" (system level digest list location).
	 * If digest list file already exists, it will be overwritten.
	 * @param name The name of the digest list to save.
	 * @throws Exception Thrown if there is an error while saving.
	 */
	public void saveSystemLevelDigestList(String name) throws Exception {
		File digestFile = new File(System.getenv("PROGRAMDATA"),"Nuix");
		digestFile = new File(digestFile,"Digest Lists");
		digestFile = new File(digestFile,name+".hash");
		saveFile(digestFile);
	}
	
	/***
	 * Imports digests from a file located at "[CASE_DIRECTORY]\Stores\User Data\Digest Lists\[NAME].hash" (case level digest list location).
	 * @param nuixCase The Nuix case the digest list is relative to.
	 * @param name The name of the digest list to import.
	 * @return How many digests were imported from the given input Nuix binary digest list file
	 * that were not already present in this instance.
	 * @throws Exception Thrown if there was an error while importing.
	 */
	public int importCaseLevelDigestList(Case nuixCase, String name) throws Exception {
		File digestFile = new File(nuixCase.getLocation(),"Stores");
		digestFile = new File(digestFile,"User Data");
		digestFile = new File(digestFile,"Digest Lists");
		digestFile = new File(digestFile,name+".hash");
		return importFile(digestFile);
	}
	
	/***
	 * Imports digests from a file located at "%appdata%\Nuix\Digest Lists\[NAME].hash" (user level digest list location).
	 * @param name The name of the digest list to import.
	 * @return How many digests were imported from the given input Nuix binary digest list file
	 * that were not already present in this instance.
	 * @throws Exception Thrown if there was an error while importing.
	 */
	public int importUserLevelDigestList(String name) throws Exception {
		File digestFile = new File(System.getenv("APPDATA"),"Nuix");
		digestFile = new File(digestFile,"Digest Lists");
		digestFile = new File(digestFile,name+".hash");
		return importFile(digestFile);
	}
	
	/***
	 * Imports digests from a file located at "%programdata%\Nuix\Digest Lists\[NAME].hash" (system level digest list location).
	 * @param name The name of the digest list to import.
	 * @return How many digests were imported from the given input Nuix binary digest list file
	 * that were not already present in this instance.
	 * @throws Exception Thrown if there was an error while importing.
	 */
	public int importSystemLevelDigestList(String name) throws Exception {
		File digestFile = new File(System.getenv("PROGRAMDATA"),"Nuix");
		digestFile = new File(digestFile,"Digest Lists");
		digestFile = new File(digestFile,name+".hash");
		return importFile(digestFile);
	}
	
	/***
	 * Creates a new empty instance.
	 */
	public DigestList() {}
	
	/***
	 * Creates a new instance and then imports digests from each provided source Nuix binary digest list file by iteratively
	 * calling {@link #importFile(File)} for each.
	 * @param sourceFiles One or more Nuix binary digest list files to import into this instance.
	 * @throws Exception Thrown if there is an error while importing any digest list file.
	 */
	public DigestList(File... sourceFiles) throws Exception {
		for(File sourceFile : sourceFiles) {
			importFile(sourceFile);
		}
	}
	
	/***
	 * Adds the MD5 of the provided item to this instance (if not already present).  If the item has no MD5 it is ignored.
	 * @param item The item for which the MD5 of will be added to this instance.
	 */
	public void addItem(Item item) {
		String md5 = item.getDigests().getMd5();
		if(md5 != null && !md5.trim().isEmpty()) {
			addMd5(md5);	
		}
	}
	
	/***
	 * Adds the MD5s of the provided items to this instance (if not already present).  Items without
	 * and MD5 value are ignored.
	 * @param items The items for which the MD5s will be added to this instance.
	 */
	public void addItems(Collection<Item> items) {
		for(Item item : items) {
			addItem(item);
		}
	}
	
	/***
	 * Removes MD5 of provided item from this instance.  If the item has no MD5, it is ignored.
	 * @param item The item for which the MD5 will be removed from this instance.
	 */
	public void removeItem(Item item) {
		String md5 = item.getDigests().getMd5();
		if(md5 != null && !md5.trim().isEmpty()) {
			removeMd5(md5);
		}
	}
	
	/***
	 * Removes MD5s from this instance based on the MD5s of the given items.  Items without an MD5
	 * value are ignored.
	 * @param items The items for which MD5s will be removed from this instance.
	 */
	public void removeItems(Collection<Item> items) {
		for(Item item : items) {
			removeItem(item);
		}
	}
	
	/***
	 * Adds the given MD5 to this instance (if not already present).
	 * @param md5 Hexadecimal string of MD5 to add to this instance.
	 */
	public void addMd5(String md5) {
		byte[] md5Bytes = FormatUtility.hexToBytes(md5);
		addMd5(md5Bytes);
	}
	
	/***
	 * Adds the given MD5 to this instance (if not already present).
	 * @param md5Bytes Byte array of MD5 to add to this instance.
	 */
	public void addMd5(byte[] md5Bytes) {
		digests.add(ByteBuffer.wrap(md5Bytes));
	}
	
	/***
	 * Removes the given MD5 from this instance.
	 * @param md5 MD5 string to remove.
	 */
	public void removeMd5(String md5) {
		byte[] md5Bytes = FormatUtility.hexToBytes(md5);
		removeMd5(md5Bytes);
	}
	
	/***
	 * Removes the given MD5 from this instance.
	 * @param md5Bytes MD5 byte array to remove.
	 */
	public void removeMd5(byte[] md5Bytes) {
		digests.remove(ByteBuffer.wrap(md5Bytes));
	}
	
	/***
	 * Gets whether the given MD5 is present in this instance.
	 * @param md5Bytes MD5 byte array to check for the presence of.
	 * @return True if the given MD5 is present in this instance.
	 */
	public boolean containsMd5(byte[] md5Bytes) {
		ByteBuffer bb = ByteBuffer.wrap(md5Bytes);
		return digests.contains(bb);
	}
	
	/***
	 * Gets whether the given MD5 is present in this instance.
	 * @param md5 MD5 string to check for the presence of.
	 * @return True if the given MD5 is present in this instance.
	 */
	public boolean containsMd5(String md5) {
		return containsMd5(FormatUtility.hexToBytes(md5));
	}
	
	/***
	 * Gets the count of digests present in this instance.
	 * @return The number of digests present.
	 */
	public int size() {
		return digests.size();
	}
	
	/***
	 * Finds items in given case which have MD5s matching those found in this instance.  Searches for MD5 values rather than using "digest-list"
	 * search field, meaning this digest list does not need to exist as a file in a location Nuix can find it, it can exist purely in memory.
	 * Searches are ran using a query like "md5:(md5A OR md5B OR ...)" with <b>chunkSize</b> determining how many MD5s max are submit in any
	 * given single query.  Items obtained through each round of searching are then added to result using ItemUtility.union until all MD5s in
	 * this instance have been searched for.  Resulting set should only ever have 1 instance of any given item, but also can have multiple different
	 * items with any given MD5.
	 * @param nuixCase The case to find matching items in.
	 * @param chunkSize How many MD5s 
	 * @return Set of items which have MD5s matching those present in this instance.
	 * @throws Exception If there is an error while searching for a chunk of items.
	 */
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
	
	/***
	 * Finds items in given case which have MD5s matching those found in this instance.  Searches for MD5 values rather than using "digest-list"
	 * search field, meaning this digest list does not need to exist as a file in a location Nuix can find it, it can exist purely in memory.
	 * Searches are ran using a query like "md5:(md5A OR md5B OR ...)" with <b>chunkSize</b> determining how many MD5s max are submit in any
	 * given single query.  Items obtained through each round of searching are then added to result using ItemUtility.union until all MD5s in
	 * this instance have been searched for.  Resulting set should only ever have 1 instance of any given item, but also can have multiple different
	 * items with any given MD5.
	 * @param nuixCase The case to find matching items in.
	 * @return Set of items which have MD5s matching those present in this instance.
	 * @throws Exception If there is an error while searching for a chunk of items.
	 */
	public Set<Item> findMatchingItems(Case nuixCase) throws Exception {
		return findMatchingItems(nuixCase,10000);
	}
	
	/***
	 * Provides an iterator over the hexadecimal string versions of the MD5s present in this instance.  MD5 values iterate in
	 * order of MD5 values, based on comparison logic of ByteBuffer.
	 */
	@Override
	public Iterator<String> iterator() {
		DigestIterator iterator = new DigestIterator();
		iterator.iter = digests.iterator();
		return iterator;
	}
}
