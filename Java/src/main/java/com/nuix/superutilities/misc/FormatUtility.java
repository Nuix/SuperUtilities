package com.nuix.superutilities.misc;

import java.io.File;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.joda.time.DateTime;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import nuix.Item;

/***
 * Class which offers methods for formatting values.
 * @author Jason Wells
 *
 */
public class FormatUtility {
	private static final int SECOND = 1;
	private static final int MINUTE = SECOND * 60;
	private static final int HOUR = MINUTE * 60;
	private static final int DAY = 24 * HOUR;
	private static boolean useSIUnits = true;
	
	private static FormatUtility instance = null;
	protected FormatUtility(){}
	public static FormatUtility getInstance(){
		if (instance == null){
			instance = new FormatUtility();
		}
		return instance;
	}
	
	/***
	 * Convenience method for converting hex string to byte array.
	 * @param hex String of hexadecimal.
	 * @return Byte array equivalent.
	 */
	public static byte[] hexToBytes(String hex){
		if(hex == null) { return null; }
		String normalizedHex = hex.replace("-", "");
		return DatatypeConverter.parseHexBinary(normalizedHex);
	}
	
	/***
	 * Convenience method for converting a byte array to a hex string.
	 * @param bytes The bytes to convert.
	 * @return A string representation of the byte array as hexadecimal.
	 */
	public static String bytesToHex(byte[] bytes){
		if(bytes == null) { return null; }
		return DatatypeConverter.printHexBinary(bytes);
	}
	
	/***
	 * Attempts to convert data types, which may be values of metadata properties
	 * or custom metadata, to a String.  Data types supported:
	 * - String
	 * - Integer
	 * - Long Integer
	 * - Boolean
	 * - Float
	 * - Double
	 * - Byte Array
	 * Data of other types will just have their toString method called.
	 * @param value Value to attempt to convert to a String
	 * @return A String value of the provided data.
	 */
	public String convertToString(Object value){
		if(value == null){
			return "";
		} else if(value instanceof String){
			return (String)value;
		} else if(value instanceof Integer){
			return Integer.toString((Integer)value);
		} else if(value instanceof Long){
			return Long.toString((Long)value);
		} else if(value instanceof Boolean){
			return Boolean.toString((Boolean)value);
		} else if(value instanceof Float){
			return Float.toString((Float)value);
		} else if(value instanceof Double){
			return Double.toString((Double)value);
		} else if(value instanceof byte[]){
			return FormatUtility.bytesToHex((byte[])value);
		} else if (value instanceof String[]) {
			// Flatten String array property value to single delimited string
			String[] stringArray = (String[])value;
			StringJoiner result = new StringJoiner("; ");
			for (int i = 0; i < stringArray.length; i++) {
				result.add(this.convertToString(stringArray[i]));
			}
			return result.toString();
		} else if (value instanceof Object[]) {
			// Flatten String array property value to single delimited string
			Object[] objectArray = (Object[])value;
			StringJoiner result = new StringJoiner("; ");
			for (int i = 0; i < objectArray.length; i++) {
				result.add(this.convertToString(objectArray[i]));
			}
			return result.toString();
		}
		else {
			return value.toString();
		}
	}
	
	/***
	 * Converts a value representing some number of seconds to an "elapsed" string.
	 * For example a value of 75 seconds would be converted to the string "00:01:15".
	 * If the time span represents more than 24 hours you may get a value such as
	 * "3 Days 11:01:37".
	 * @param offsetSeconds The number of seconds elapsed
	 * @return A String representing the elapsed time.
	 */
	public String secondsToElapsedString(Double offsetSeconds){
		return secondsToElapsedString(offsetSeconds.longValue());
	}
	
	/***
	 * Converts a value representing some number of seconds to an "elapsed" string.
	 * For example a value of 75 seconds would be converted to the string "00:01:15".
	 * If the time span represents more than 24 hours you may get a value such as
	 * "3 Days 11:01:37".
	 * @param offsetSeconds The number of seconds elapsed
	 * @return A String representing the elapsed time.
	 */
	public String secondsToElapsedString(long offsetSeconds){
		long seconds = offsetSeconds;
		
		long days = seconds / DAY;
		seconds -= days * DAY;
		
		long hours = seconds / HOUR;
		seconds -= hours * HOUR;
		
		long minutes = seconds / MINUTE;
		seconds -= minutes * MINUTE;
		
		if(days > 0){
			if(days > 1){
				return String.format("%d Days %02d:%02d:%02d",days,hours,minutes,seconds);	
			} else {
				return String.format("%d Day %02d:%02d:%02d",days,hours,minutes,seconds);
			}
		} else {
			return String.format("%02d:%02d:%02d",hours,minutes,seconds);	
		}
	}
	
	/***
	 * Convenience method for formatting numeric value using US locale.
	 * @param number The number to format
	 * @return Formatting String version of the number
	 */
	public String formatNumber(int number){
		return NumberFormat.getNumberInstance(Locale.US).format(number);
	}
	
	/***
	 * Convenience method for formatting numeric value using US locale.
	 * @param number The number to format
	 * @return Formatting String version of the number
	 */
	public String formatNumber(long number){
		return NumberFormat.getNumberInstance(Locale.US).format(number);
	}
	
	/***
	 * Convenience method for formatting numeric value using US locale.
	 * @param number The number to format
	 * @return Formatting String version of the number
	 */
	public String formatNumber(double number){
		return NumberFormat.getNumberInstance(Locale.US).format(number);
	}
	
	/***
	 * Simple method to replace place holders with values in a template string.  Placeholders are values
	 * starting with a '{', followed by a name, followed by '}'.  For example "{name}".
	 * @param template The template text to resolve place holders in 
	 * @param placeholderValues Map of place holder names and associated values to replace them with.  Name should not include '{' or '}'
	 * @return Template string with place holders replaced
	 */
	public String resolvePlaceholders(String template, Map<String,Object> placeholderValues){
		String result = template;
		for (Map.Entry<String,Object> entry : placeholderValues.entrySet()) {
			String key = entry.getKey();
			String value = convertToString(entry.getValue());
			result = result.replaceAll(Pattern.quote("{"+key+"}"), Matcher.quoteReplacement(value));
		}
		return result;
	}
	
	/***
	 * Generates a time stamp string which is safe to use in a file or directory name.
	 * @return File/directory name safe time stamp string.
	 */
	public String getFilenameTimestamp(){
		return new DateTime().toString("YYYY-MM-dd_HH-mm-ss");
	}
	
	/***
	 * Gets value indicating whether SI Unit (1000 Bytes = 1 Kilo Byte) will be used.
	 * @return True if size methods use 1000 bytes, false if they will use 1024 bytes
	 */
	public static boolean getUseSIUnits() {
		return useSIUnits;
	}
	
	/***
	 * Gets value indicating whether SI Unit (1000 Bytes = 1 Kilo Byte) will be used.
	 * @param useSIUnits True if size methods use 1000 bytes, false if they will use 1024 bytes
	 */
	public static void setUseSIUnits(boolean useSIUnits) {
		FormatUtility.useSIUnits = useSIUnits;
	}
	
	/***
	 * Gets the unit base which will be used in size calculations.  Will return 1000 if
	 * {@link #getUseSIUnits()} returns true and 1024 if {@link #getUseSIUnits()}
	 * returns false.
	 * @return Size base unit used in size calculations.
	 */
	public static double getUnitBase(){
		double unitBase = 0.0d;
		
		if(useSIUnits)
			unitBase = 1000d;
		else
			unitBase = 1024d;
		
		return unitBase;
	}
	
	/***
	 * Converts size specified in bytes to size specified in kilo bytes, rounding to specified
	 * number of decimal places.
	 * @param bytes Size in bytes
	 * @param decimalPlaces Number of decimal places to round to
	 * @return Size in bytes converted to size in kilo bytes
	 */
	public static double bytesToKiloBytes(long bytes, int decimalPlaces){
		double unitBase = getUnitBase();
		double kb_in_bytes = unitBase;
		double kb = ((double)bytes) / kb_in_bytes;
		kb = round(kb,decimalPlaces);
		return kb;
	}
	
	/***
	 * Converts size specified in bytes to size specified in mega bytes, rounding to specified
	 * number of decimal places.
	 * @param bytes Size in bytes
	 * @param decimalPlaces Number of decimal places to round to
	 * @return Size in bytes converted to size in mega bytes
	 */
	public static double bytesToMegaBytes(long bytes, int decimalPlaces){
		double unitBase = getUnitBase();
		double mb_in_bytes = Math.pow(unitBase,2d);
		double mb = ((double)bytes) / mb_in_bytes;
		mb = round(mb,decimalPlaces);
		return mb;
	}
	
	/***
	 * Converts size specified in bytes to size specified in giga bytes, rounding to specified
	 * number of decimal places.
	 * @param bytes Size in bytes
	 * @param decimalPlaces Number of decimal places to round to
	 * @return Size in bytes converted to size in giga bytes
	 */
	public static double bytesToGigaBytes(long bytes, int decimalPlaces){
		double unitBase = getUnitBase();
		double gb_in_bytes = Math.pow(unitBase,3d);
		double gb = ((double)bytes) / gb_in_bytes;
		gb = round(gb,decimalPlaces);
		return gb;
	}
	
	/***
	 * Converts size in bytes to a String representing the file size, as you might see it represented
	 * by the OS, such as "1.24 KB" or "3.42 GB"
	 * @param bytes The size in bytes
	 * @param decimalPlaces The number of decimal places to include in the result
	 * @return A String representing the size in appropriate format
	 */
	public static String bytesToDynamicSize(long bytes, int decimalPlaces){
		double unitBase = getUnitBase();
		double gb_in_bytes = Math.pow(unitBase,3d);
		double mb_in_bytes = Math.pow(unitBase,2d);
		double kb_in_bytes = unitBase;
		
		if (bytes >= gb_in_bytes){
			double gb = bytesToGigaBytes(bytes, decimalPlaces);
			return Double.toString(gb)+" GB";
		} else if (bytes >= mb_in_bytes){
			double mb = bytesToMegaBytes(bytes, decimalPlaces);
			return Double.toString(mb)+" MB";
		} else if (bytes >= kb_in_bytes){
			double kb = bytesToKiloBytes(bytes, decimalPlaces);
			return Double.toString(kb)+" KB";
		} else{
			return Long.toString(bytes)+" Bytes";
		}
			 
	}
	
	/***
	 * Helper method for rounding a decimal value to a particular number of decimal places.
	 * @param value The value to round
	 * @param numberOfDigitsAfterDecimalPoint Number of digits to round to
	 * @return The given value rounded off to specified decimal places
	 */
	public static double round(double value, int numberOfDigitsAfterDecimalPoint) {
        BigDecimal bigDecimal = new BigDecimal(value);
        bigDecimal = bigDecimal.setScale(numberOfDigitsAfterDecimalPoint,
                BigDecimal.ROUND_HALF_UP);
        return bigDecimal.doubleValue();
    }
	
	/***
	 * Convenience method for using Lucene's StandardTokenizer to tokenize arbitrary text.
	 * @param inputText The text to tokenize
	 * @return A list of tokens parsed from inputText by Lucene.
	 * @throws Exception Most likely thrown if Lucene tokenizer encounters a problem.
	 */
	public static List<String> tokenizeText(String inputText) throws Exception{
		List<String> tokens = new ArrayList<String>();
		
		if(inputText != null) {
			try(StringReader stringReader = new StringReader(inputText)){
				try(StandardTokenizer tokenizer = new StandardTokenizer()){
					tokenizer.setReader(stringReader);
					CharTermAttribute attribute = tokenizer.getAttribute(CharTermAttribute.class);
					tokenizer.reset();
					while(tokenizer.incrementToken()) {
						tokens.add(attribute.toString());
					}
					tokenizer.end();
				}
			}
		}
		return tokens;
	}
	
	/***
	 * Convenience method for getting the "rendered text" (text as seen in browser) of HTML source code using Jsoup.
	 * @param htmlSource The HTML source code to parse the text from.
	 * @return The "rendered text" of the provided HTML source code.
	 */
	public static String getHtmlText(String htmlSource) {
		Document doc = Jsoup.parse(htmlSource);
		String result = doc.body().text();
		return result;
	}
	
	public static String formatAsTextualTable(List<List<String>> rows) {
		List<Integer> columnWidths = new ArrayList<Integer>();
		for(List<String> row : rows) {
			for (int col = 0; col < row.size(); col++) {
				if(col+1 > columnWidths.size()) {
					columnWidths.add(0);
				}
				
				int width = row.get(col).length(); 
				if(width > columnWidths.get(col)) {
					columnWidths.set(col, width);
				}
			}
		}
		
		StringJoiner result = new StringJoiner("\n");
		for(List<String> row : rows) {
			List<String> justifiedCols = new ArrayList<String>();
			for (int col = 0; col < row.size(); col++) {
				int paddedWidth = columnWidths.get(col) + 2;
				justifiedCols.add(StringUtils.center(row.get(col), paddedWidth));
			}
			result.add(String.join(" | ", justifiedCols));
		}
		
		return result.toString();
	}
	
	public static String debugString(Map<String,?> map) {
		StringJoiner result = new StringJoiner("\n");
		for(Map.Entry<String,?> entry : map.entrySet()){
			result.add(String.format("'%s' => '%s'",entry.getKey(), getInstance().convertToString(entry.getValue())));
		}
		return result.toString();
	}
	
	public static String debugString(Exception exc) {
		return ExceptionUtils.getStackTrace(exc);
	}
	
	public static File makeGuidPath(Item item, File rootDirectory, String extension) {
		File tempFile = null;
		String guid = item.getGuid();
		tempFile = new File(rootDirectory,guid.substring(0, 3));
		tempFile = new File(tempFile,guid.substring(3, 6));
		tempFile.mkdirs();
		tempFile = new File(tempFile,item.getGuid()+"."+extension);
		return tempFile;
	}
}
