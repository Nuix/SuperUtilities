package com.nuix.superutilities.export;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import nuix.Item;
import nuix.MetadataProfile;

/***
 * Generates a series of DAT loadfiles, based on a metadata profile.
 * @author Jason Wells
 *
 */
public class ChunkedDATExporter {
	// Used to pass chunk information to work threads
	public class ChunkInfo {
		public int chunkIndex = 1;
		public List<Item> items = new ArrayList<Item>();
		public ChunkInfo(int index){
			chunkIndex = index;
		}
	}
	
	// Determines how many threads will be used
	private int concurrency = 4;
	
	// Determines how many items will be put in each chunk
	private int chunkSize = 10000;
	
	// Determines the directory load file chunks will be written to
	private File exportDirectory;
	
	// Determines the profile used to generate each load file
	private MetadataProfile profile;
	
	// Determines the prefix added to each load file's name
	private String filenamePrefix = "Chunk_";
	
	// When true only the first load file will have headers
	// When false all load files will have headers
	private boolean headersOnFirstChunkOnly = false;
	
	public void export(List<Item> items){
		// ** Lets make sure general state is good **
		
		// Make sure concurrency value is reasonable
		if(concurrency < 1){ throw new IllegalStateException("concurrency must be greater than 0, value provided was "+concurrency); }
		
		// Make sure chunk size is reasonable
		if(chunkSize < 1){ throw new IllegalStateException("chunkSize must be greater than 0, value provided was "+chunkSize); }
		
		// Make sure we have an export directory set
		if(exportDirectory == null){ throw new IllegalStateException("An export directory has not been provided"); }
		
		// Make sure we have a metadata profile set
		if(profile == null){ throw new IllegalStateException("A metadata profile has not been provided"); }
		
		// If file name perfix is null, lets make it an empty string to prevent errors later
		if(filenamePrefix == null){ filenamePrefix = ""; }
		
		// ** Lets make sure arguments are good **
		
		// Make sure items is not null
		if(items == null){ throw new IllegalArgumentException("Argument 'items' cannot be null"); }
		
		// Make sure we have at least one item
		if(items.size() < 1){ throw new IllegalArgumentException("Argument 'items' must contain 1 or more items"); }

		// Create export directory as needed
		if(!exportDirectory.exists()){ exportDirectory.mkdirs(); }
		
		// Setup our queue
		BlockingQueue<ChunkInfo> workQueue = new LinkedBlockingQueue<ChunkInfo>();
		
		// Setup our threads
		List<Thread> workThreads = new ArrayList<Thread>();
		for (int i = 0; i < concurrency; i++) {
			workThreads.add(new Thread(() -> {
				// Loop until logic below tells us we are done
				while(true){
					ChunkInfo chunk = null;
					try {
						chunk = workQueue.take();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					// Use negative chunk index to indicate we are done
					if(chunk.chunkIndex < 0){
						break;
					} else {
						exportToFile(chunk);
					}
				}
			}));
			// We need to make sure we start up each thread
			workThreads.get(i).start();
		}

		// Batch items into chunks and dump them into the queue
		ChunkInfo currentChunk = new ChunkInfo(1);
		for (int i = 0; i < items.size(); i++) {
			if(currentChunk.items.size() == chunkSize){
				try {
					workQueue.put(currentChunk);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				currentChunk = new ChunkInfo(currentChunk.chunkIndex+1);
			}
			currentChunk.items.add(items.get(i));
		}
		
		// Make sure we enqueue last chunk which is likely less
		// than chunk size items
		try {
			workQueue.put(currentChunk);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// Enqueue 1 chunk info for each thread with index of -1
		// these chunks signal each thread that it is done
		try {
			for (int i = 0; i < concurrency; i++) {
				workQueue.put(new ChunkInfo(-1));
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// Join all the threads to make sure we wait for them
		// all to have completed
		for (int i = 0; i < concurrency; i++) {
			try {
				workThreads.get(i).join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	// Exports a provided chunk to a load file
	protected void exportToFile(ChunkInfo chunk){
		// Calculate this chunk load file's name
		File exportFile = new File(exportDirectory,filenamePrefix+String.format("%08d",chunk.chunkIndex)+".DAT");
		// Write out the load file
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(exportFile))){
			if(headersOnFirstChunkOnly == false || chunk.chunkIndex == 1){
				List<String> headers = profile.getMetadata().stream().map(f -> f.getName()).collect(Collectors.toList()); 
				writer.write(generateRecord(headers)+"\n");
			}
			for (int i = 0; i < chunk.items.size(); i++) {
				Item currentItem = chunk.items.get(i);
				writer.write(generateRecord(currentItem)+"\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// Helper method for converting an item into the related profile fields' values
	// then generating a Concordance DAT record line from those values
	protected String generateRecord(Item item){
		List<String> values = profile.getMetadata().stream().map(field -> {
			try {
				// Get the given profile fields value for this item
				return field.evaluate(item);
			} catch (Exception e) {
				// Yield error message for field value if
				// we encountered an error
				return "error: "+e.getMessage();
			}
		}).collect(Collectors.toList());
		// Use other generateRecord method to build DAT record line
		return generateRecord(values);
	}
	
	// Helper method to generate a Concordance DAT record from a list of values 
	protected String generateRecord(List<String> values){
		List<String> formattedValues = values.stream().map(v -> "þ"+v+"þ").collect(Collectors.toList());
		String result = String.join("\u0014", formattedValues);
		return result;
	}

	// Getters and Setters
	public int getConcurrency() { return concurrency; }
	public void setConcurrency(int concurrency) { this.concurrency = concurrency; }
	public int getChunkSize() { return chunkSize; }
	public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }
	public File getExportDirectory() { return exportDirectory; }
	public void setExportDirectory(File exportDirectory) { this.exportDirectory = exportDirectory; }
	public void setExportDirectory(String exportDirectory) { setExportDirectory(new File(exportDirectory)); }
	public MetadataProfile getProfile() { return profile; }
	public void setProfile(MetadataProfile profile) { this.profile = profile; }
	public String getFilenamePrefix() { return filenamePrefix; }
	public void setFilenamePrefix(String filenamePrefix) { this.filenamePrefix = filenamePrefix; }
	public boolean getHeadersOnFirstChunkOnly() { return headersOnFirstChunkOnly; }
	public void setHeadersOnFirstChunkOnly(boolean headersOnFirstChunkOnly) { this.headersOnFirstChunkOnly = headersOnFirstChunkOnly; }

}
