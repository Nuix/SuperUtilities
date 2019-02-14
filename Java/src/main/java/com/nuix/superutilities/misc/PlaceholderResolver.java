/******************************************
Copyright 2018 Nuix
http://www.apache.org/licenses/LICENSE-2.0
*******************************************/

package com.nuix.superutilities.misc;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/***
 * This class provides a way to allow user input to make use of place holder values which will be
 * substituted at run time with appropriate values.
 * @author Jason Wells
 *
 */
public class PlaceholderResolver {
	private Map<String,String> placeholderData = new LinkedHashMap<String,String>();
	private Map<String,Pattern> placeholderPatterns = new LinkedHashMap<String,Pattern>();
	private Set<String> placeholderPaths = new HashSet<String>();
	
	/***
	 * Set they value for a given placeholder
	 * @param key The placeholder name without '{' or '}'
	 * @param value The value to associate
	 */
	public void set(String key, String value) {
		key = key.toLowerCase();
		placeholderData.put(key,value);
		placeholderPatterns.put(key,Pattern.compile(Pattern.quote("{"+key+"}"),Pattern.CASE_INSENSITIVE));
	}
	
	/***
	 * Similar to the {@link #set} method except this has logic to appropriately handle file paths.
	 * @param key The placeholder name without '{' or '}' 
	 * @param value The file/directory path value to associate
	 */
	public void setPath(String key, String value) {
		key = key.toLowerCase();
		placeholderData.put(key,value);
		placeholderPatterns.put(key,Pattern.compile(Pattern.quote("{"+key+"}"),Pattern.CASE_INSENSITIVE));
		placeholderPaths.add(key);
	}
	
	/***
	 * Get the value currently associated for a given placeholder
	 * @param key The placeholder name without '{' or '}'
	 * @return The currently associated placeholder value
	 */
	public String get(String key) {
		return placeholderData.get(key.toLowerCase());
	}
	
	/***
	 * Clears all currently associated place holders (keys and values)
	 */
	public void clear() {
		placeholderData.clear();
	}
	
	/***
	 * Gets the Map containing all the current place holder data
	 * @return A Map containing all the current place holder data
	 */
	public Map<String,String> getPlaceholderData(){
		return placeholderData;
	}
	
	/***
	 * Resolves place holders into a string based on the currently associated values
	 * @param template The input string containing place holders
	 * @return The input string in which place holders have been replaced with associated values
	 */
	public String resolveTemplate(String template) {
		String result = template;
		for(Map.Entry<String,Pattern> entry : placeholderPatterns.entrySet()){
			Pattern p = entry.getValue();
			String value = Matcher.quoteReplacement(get(entry.getKey()));
			result = p.matcher(result).replaceAll(value);
		}
		return result;
	}
	
	/***
	 * Resolves place holders into a path string based on the currently associated values.  Contains logic
	 * to sterilize the resulting path string so that it does not contain common illegal path characters.
	 * @param template A file/directory path string containing place holders
	 * @return The input string in which place holders have been replaced with associated values
	 */
	public String resolveTemplatePath(String template) {
		String result = template;
		for(Map.Entry<String,Pattern> entry : placeholderPatterns.entrySet()){
			Pattern p = entry.getValue();
			String value = Matcher.quoteReplacement(get(entry.getKey()));
			if(!placeholderPaths.contains(entry.getKey())){
				value = cleanPathString(value);
			}
			result = p.matcher(result).replaceAll(value);
		}
		return result;
	}
	
	/***
	 * Helper method to strip common illegal path characters from a string 
	 * @param input The string to clean up
	 * @return The string with illegal path characters replaced with '_'
	 */
	public static String cleanPathString(String input){
		return input.replaceAll("[\\Q<>:\"|?*[]\\E]","_");
	}
}