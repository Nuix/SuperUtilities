package com.nuix.superutilities.regex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;

import com.nuix.superutilities.misc.FormatUtility;

import nuix.Item;

/***
 * Class for scanning a series of items with a series of regular expressions.
 * @author Jason Wells
 *
 */
public class RegexScanner {
	private static Logger logger = Logger.getLogger(RegexScanner.class);
	private static int maxToStringLength = 1024 * 1024 * 5;
	
	/***
	 * Configures the character count threshold in which the CharSequence TextObject of an item, obtained from the
	 * API, is first converted to a String object before being scanned for regular expression matches.  CharSequence may
	 * make use of less memory and perform slower but scanning value as a String may perform faster and user more memory.
	 * @param maxLength Maximum text length that should be converted to a String before scanning
	 */
	public static void setMaxToStringLength(int maxLength){
		maxToStringLength = maxLength;
	}
	
	private boolean scanProperties = true;
	private Set<String> propertiesToScan = new HashSet<String>();
	private boolean scanContent = true;
	private boolean caseSensitive = false;
	private boolean captureContextualText = true;
	private int contextSize = 100;
	
	private boolean abortWasRequested = false;
	private Object scanErrorLock = new Object();
	
	private List<PatternInfo> patterns = new ArrayList<PatternInfo>();
	
	private Consumer<Integer> progressUpdatedCallback = null;
	private Consumer<RegexScanError> errorCallback;
	
	/***
	 * Allows you to provide a callback which will be invoked when progress updates occur.
	 * @param callback Callback to receive progress updates
	 */
	public void whenProgressUpdated(Consumer<Integer> callback){
		progressUpdatedCallback = callback;
	}
	
	/***
	 * Fires progress update if there is a callback listening
	 * @param value The progress value
	 */
	protected void fireProgressUpdated(int value){
		if(progressUpdatedCallback != null){
			progressUpdatedCallback.accept(value);
		}
	}
	
	/***
	 * Allows you to provide a callback which will be invoked when an error occurs during scanning.
	 * @param errorCallback The callback to be invoked when errors occur
	 */
	public void whenErrorOccurs(Consumer<RegexScanError> errorCallback){
		this.errorCallback = errorCallback;
	}
	
	/***
	 * Fires error event if there is a callback listening.
	 * @param error The error which occurred
	 */
	protected void fireScanError(RegexScanError error){
		synchronized(scanErrorLock){
			if(errorCallback != null){
				errorCallback.accept(error);
			}
			
			StringJoiner errorMessage = new StringJoiner("\n");
			errorMessage.add("Error while scanning:");
			if(error.getPatternInfo() != null){
				errorMessage.add("\tExpression: "+error.getPatternInfo().getTitle()+" => "+error.getPatternInfo().getExpression());	
			}
			if(error.getLocation() != null){
				errorMessage.add("\tLocation: "+error.getLocation());	
			}
			errorMessage.add("\tItem GUID: "+error.getItem().getGuid());
			logger.error(errorMessage.toString());
			logger.error(error.getException());
		}
	}
	
	/***
	 * Adds a regular expression to be part of the scan with a given title.  Creates a new instance of
	 * {@link PatternInfo} using the values provided.
	 * @param title The associated title
	 * @param expression The Java regular expression string to add
	 */
	public void addPattern(String title, String expression){
		PatternInfo info = new PatternInfo(title,expression);
		patterns.add(info);
	}
	
	/***
	 * Scans a series of items serially (no concurrency)
	 * @param items The items to scan
	 * @return List of matches
	 */
	public List<ItemRegexMatchCollection> scanItems(Collection<Item> items){
		List<ItemRegexMatchCollection> result = new ArrayList<ItemRegexMatchCollection>();
		
		for (PatternInfo p : patterns) {
			p.compile(caseSensitive);
		}
		
		for (Item item : items) {
			try {
				ItemRegexMatchCollection itemMatches = scanItem(item);
				
				if(itemMatches.getMatchCount() > 0){
					result.add(itemMatches);
				}
			} catch (Exception e) {
				RegexScanError error = new RegexScanError(item, null, null, e);
				fireScanError(error);
			}
		}
		
		return result;
	}
	
	/***
	 * Scans a series of items, providing each item's matches to callback as they are obtained.
	 * Items are scanned in serial (no concurrency).
	 * @param items The items to scan
	 * @param callback Callback which will received each item's matches as they are obtained.
	 */
	public void scanItems(Collection<Item> items, Consumer<ItemRegexMatchCollection> callback){
		abortWasRequested = false;
		
		for (PatternInfo p : patterns) {
			p.compile(caseSensitive);
		}
		
		int itemIndex = 0;
		for (Item item : items) {
			try {
				ItemRegexMatchCollection itemMatches = scanItem(item);
				
				if(itemMatches.getMatchCount() > 0){
					callback.accept(itemMatches);
				}
			} catch (Exception e) {
				RegexScanError error = new RegexScanError(item, null, null, e);
				fireScanError(error);
			}
			
			itemIndex++;
			fireProgressUpdated(itemIndex);
			
			if(abortWasRequested){ break; }
		}
	}
	
	/***
	 * Scans a series of items, providing each item's matches to callback as they are obtained.
	 * Items are scanned in parallel using a Java parallel stream.
	 * @param items The items to scan
	 * @param callback Callback which will received each item's matches as they are obtained.
	 */
	public void scanItemsParallel(Collection<Item> items, Consumer<ItemRegexMatchCollection> callback){
		abortWasRequested = false;
		
		for (PatternInfo p : patterns) {
			p.compile(caseSensitive);
		}
		
		AtomicInteger itemIndex = new AtomicInteger(0);
		
		items.parallelStream().forEach(new Consumer<Item>(){
			@Override
			public void accept(Item item) {
				if(!abortWasRequested){
					try {
						ItemRegexMatchCollection itemMatches = scanItem(item);
						
						if(itemMatches.getMatchCount() > 0){
							callback.accept(itemMatches);	
						}
					} catch (Exception e) {
						RegexScanError error = new RegexScanError(item, null, null, e);
						fireScanError(error);
					}
					
					synchronized(itemIndex){
						fireProgressUpdated(itemIndex.addAndGet(1));
					}
				}
			}
		});
	}
	
	/***
	 * Scans a series of items, providing each item's matches to callback as they are obtained.
	 * Items are scanned in parallel using a Java parallel stream.  This differs from the method {@link #scanItemsParallel(Collection, Consumer)}
	 * in that this method invokes the parallel stream within a thread pool to allow for controlling how many threads are used.
	 * @param items The items to scan
	 * @param callback Callback which will received each item's matches as they are obtained.
	 * @param concurrency Number of threads to create in worker pool that parallel stream is invoked in
	 * @throws Exception if there is an error
	 */
	public void scanItemsParallel(Collection<Item> items, Consumer<ItemRegexMatchCollection> callback, final int concurrency) throws Exception{
		if(concurrency < 1){
			throw new IllegalArgumentException("concurrency cannot be less than 1");
		}
		
		ForkJoinPool pool = null;
		abortWasRequested = false;
		
		for (PatternInfo p : patterns) {
			p.compile(caseSensitive);
		}
		
		AtomicInteger itemIndex = new AtomicInteger(0);
		AtomicLong lastProgress = new AtomicLong(System.currentTimeMillis());
		
		Consumer<Item> consumer = new Consumer<Item>(){
			@Override
			public void accept(Item item) {
				if(!abortWasRequested){
					try {
						ItemRegexMatchCollection itemMatches = scanItem(item);
						
						if(itemMatches.getMatchCount() > 0){
							callback.accept(itemMatches);	
						}
						
						itemMatches = null;
					} catch (Exception e) {
						RegexScanError error = new RegexScanError(item, null, null, e);
						fireScanError(error);
					}
					
					int index = itemIndex.addAndGet(1);
					long elapsedMillis = System.currentTimeMillis() - lastProgress.get();
					if(elapsedMillis >= 1000){
						fireProgressUpdated(index);
						lastProgress.set(System.currentTimeMillis());
					}
				}
			}
		};
		
		try {
			pool = new ForkJoinPool(concurrency);
			pool.submit(()->{
				items.parallelStream().forEach(consumer);
			}).get();
		} catch (Exception e) {
			logger.error("Error while scanning",e);
			throw e;
		} finally {
			if(pool != null)
				pool.shutdown();
		}
	}

	/***
	 * Scans a single item
	 * @param item The item to be scanned
	 * @return The matches for that item
	 */
	protected ItemRegexMatchCollection scanItem(Item item) {
		ItemRegexMatchCollection itemMatches = new ItemRegexMatchCollection(item);
		
		if(scanProperties){
			try {
				for (PatternInfo p : patterns) {
					Matcher m = null;
					
					for (Entry<String,String> propertyEntry : getStringProperties(item,propertiesToScan).entrySet()) {
						String propertyName = propertyEntry.getKey();
						try {
							String propertyValue = propertyEntry.getValue();
							if (m == null){
								m = p.getPattern().matcher(propertyValue);
							} else {
								m.reset(propertyValue);
							}
							
							while(m.find()){
								int matchStart = m.start();
								int matchEnd = m.end();
								String value = m.group();
								if(captureContextualText && contextSize > 0){
									String context = getContextualSubString(propertyValue,matchStart,matchEnd,contextSize);
									itemMatches.addMatch(p,propertyName,false,value,context,matchStart,matchEnd);	
								} else {
									itemMatches.addMatch(p,propertyName,false,value,"",matchStart,matchEnd);	
								}
							}
						} catch (Exception e) {
							RegexScanError error = new RegexScanError(item, p, propertyName, e);
							fireScanError(error);
						}
					}
				}
			} catch (Exception e) {
				RegexScanError error = new RegexScanError(item, null, null, e);
				fireScanError(error);
			}
		}
		
		if(scanContent){
			try {
				for (PatternInfo p : patterns) {
					try {
						CharSequence contentTextCharSequence = item.getTextObject();
						if(contentTextCharSequence != null){
							Matcher m = null;
							if(contentTextCharSequence.length() < maxToStringLength){
								m = p.getPattern().matcher(contentTextCharSequence.toString());
								if(!captureContextualText || contextSize < 1){
									// We have no more need for char sequence
									contentTextCharSequence = null;
								}
							} else {
								m = p.getPattern().matcher(contentTextCharSequence);
							}
							
							while(m.find()){
								int matchStart = m.start();
								int matchEnd = m.end();
								String value = m.group();
								if(captureContextualText && contextSize > 0){
									String context = getContextualSubString(contentTextCharSequence,matchStart,matchEnd,contextSize);
									itemMatches.addMatch(p,"Content",false,value,context,matchStart,matchEnd);
								} else {
									itemMatches.addMatch(p,"Content",false,value,"",matchStart,matchEnd);	
								}
							}
							m = null;
							contentTextCharSequence = null;
						}
					} catch (Exception e) {
						RegexScanError error = new RegexScanError(item, p, "Content", e);
						fireScanError(error);
					}
				}
			} catch (Exception e) {
				RegexScanError error = new RegexScanError(item, null, null, e);
				fireScanError(error);
			}
		}
		return itemMatches;
	}
	
	/***
	 * Convenience method for converting the metadata properties of an item into a Map&lt;String,String&gt; so that
	 * regular expressions may be ran against them.
	 * @param item The item from which metadata properties will be pulled
	 * @param specificProperties List of specific properties to be pulled.  If null is provided, all properties will be pulled.
	 * @return Map of "stringified" metadata properties for the specified item
	 */
	public static Map<String,String> getStringProperties(Item item, Set<String> specificProperties){
		Map<String,String> result = new HashMap<String,String>();
		for (Entry<String, Object> entry : item.getProperties().entrySet()) {
			if(specificProperties == null || specificProperties.contains(entry.getKey())){
				result.put(entry.getKey(), FormatUtility.getInstance().convertToString(entry.getValue()));
			}
		}
		return result;
	}
	
	public static String getContextualSubString(CharSequence textSequence, int matchStart, int matchEnd, int contextSize){
		int rangeStart = matchStart - contextSize;
		int rangeEnd = matchEnd + contextSize + 1;
		
		if(rangeStart < 0) rangeStart = 0;
		if(rangeEnd > textSequence.length()) rangeEnd = textSequence.length();
		
		return textSequence.subSequence(rangeStart,rangeEnd).toString().replaceAll("\r?\n", " ");
	}

	public boolean getScanProperties() {
		return scanProperties;
	}

	public void setScanProperties(boolean scanProperties) {
		this.scanProperties = scanProperties;
	}

	public boolean getScanContent() {
		return scanContent;
	}

	public void setScanContent(boolean scanContent) {
		this.scanContent = scanContent;
	}

	public boolean getCaseSensitive() {
		return caseSensitive;
	}

	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	public boolean getCaptureContextualText() {
		return captureContextualText;
	}

	public void setCaptureContextualText(boolean captureContextualText) {
		this.captureContextualText = captureContextualText;
	}

	public int getContextSize() {
		return contextSize;
	}

	public void setContextSize(int contextSize) {
		this.contextSize = contextSize;
	}

	public List<PatternInfo> getPatterns() {
		return patterns;
	}

	public void setPatterns(List<PatternInfo> patterns) {
		this.patterns = patterns;
	}
	
	public List<String> getPropertiesToScan() {
		return new ArrayList<String>(propertiesToScan);
	}
	
	public void setPropertiesToScan(List<String> propertiesToScan) {
		this.propertiesToScan = new HashSet<String>();
		for(String propertyName : propertiesToScan){
			this.propertiesToScan.add(propertyName);
		}
	}
	
	/***
	 * When running a scan by providing a Consumer callback, this will signal
	 * that further scanning should be aborted.
	 */
	public void abortScan(){
		abortWasRequested = true;
	}
}
