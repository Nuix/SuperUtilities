/******************************************
Copyright 2018 Nuix
http://www.apache.org/licenses/LICENSE-2.0
*******************************************/

package com.nuix.superutilities.misc;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;

import nuix.Item;

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
	
	private void recordPlaceholderPattern(String key) {
		if(!placeholderPatterns.containsKey(key)) {
			placeholderPatterns.put(key,Pattern.compile(Pattern.quote("{"+key+"}"),Pattern.CASE_INSENSITIVE));
		}
	}
	
	/***
	 * Automatically sets various place holders based on the provided item.  Place holders set:<br/><br/>
	 * <code>{guid}</code> - The item's GUID.<br/>
	 * <code>{guid_prefix}</code> Characters 0-2 of the item's GUID.  Useful for creating sub-directories based on GUID.<br/>
	 * <code>{guid_infix}</code> Characters 3-5 of the item's GUID.  Useful for creating sub-directories based on GUID.<br/>
	 * <code>{name}</code> - The item's name as obtained by <code>Item.getLocalisedName</code><br/>
	 * <code>{md5}</code> - The item's MD5 or <code style="font-weight:bold">NO_MD5</code> for items without an MD5 value<br/>
	 * <code>{type}</code> - The item's type name as obtained by <code>ItemType.getLocalisedName</code><br/>
	 * <code>{mime_type}</code> - The item's mime type as obtained by <code>ItemType.getName</code><br/>
	 * <code>{kind}</code> - The item's kind name as obtained by <code>ItemType.getKind.getName</code><br/>
	 * <code>{custodian}</code> - The item's assigned custodian or <code style="font-weight:bold">NO_CUSTODIAN</code> for items without a custodian assigned<br/>
	 * <code>{evidence_name}</code> - The name of the evidence the item belongs to.<br/>
	 * <code>{item_date_short}</code> - The item's item date formatted <code>YYYYMMDD</code> or <code style="font-weight:bold">NO_DATE</code> for items without an item date.<br/>
	 * <code>{item_date_long}</code> - The item's item date formatted <code>YYYYMMdd-HHmmss</code> or <code style="font-weight:bold">NO_DATE</code> for items without an item date.<br/>
	 * <code>{item_date_year}</code> - The item's item date 4 digit year or <code style="font-weight:bold">NO_DATE</code> for items without an item date.<br/>
	 * <code>{top_level_guid}</code> - The GUID of the provided item's top level item or <code style="font-weight:bold">ABOVE_TOP_LEVEL</code> for items which are above top level.<br/>
	 * <code>{top_level_name}</code> - The name (via <code>Item.getLocalisedName</code>) of the provided item's top level item or <code style="font-weight:bold">ABOVE_TOP_LEVEL</code> for items which are above top level.</br>
	 * <code>{top_level_kind}</code> - The kind (via <code>ItemType.getKind.getName</code>) of the provided item's top level item or <code style="font-weight:bold">ABOVE_TOP_LEVEL</code> for items which are above top level.</br>
	 * <code>{original_extension}</code> - The original extension as obtained from Nuix via <code>Item.getOriginalExtension</code> or <code style="font-weight:bold">NO_ORIGINAL_EXTENSION</code> for items where Nuix does not have an original extension value.<br/>
	 * <code>{corrected_extension}</code> - The corrected extension as obtained from Nuix via <code>Item.getCorrectedExtension</code> or <code style="font-weight:bold">NO_CORRECTED_EXTENSION</code> for items where Nuix does not have a corrected extension value.<br/> 
	 * @param item The item used to set all of the item based placeholder values.
	 */
	public void setFromItem(Item item) {
		String guid = item.getGuid();
		set("guid",guid);
		set("guid_prefix",guid.substring(0, 3));
		set("guid_infix",guid.substring(3, 6));
		
		set("name",item.getLocalisedName());
		set("type",item.getType().getLocalisedName());
		set("mime_type",item.getType().getName());
		set("kind",item.getType().getKind().getName());
		set("evidence_name",item.getRoot().getLocalisedName());
		
		String md5 = item.getDigests().getMd5();
		if(md5 == null || md5.trim().isEmpty()) {
			md5 = "NO_MD5";
		}
		set("md5",md5);
		
		String custodian = item.getCustodian();
		if(custodian == null || custodian.trim().isEmpty()) { custodian = "NO_CUSTODIAN"; }
		set("custodian",custodian);
		
		DateTime itemDate = item.getDate();
		if(itemDate == null) {
			set("item_date_short","NO_DATE");
			set("item_date_long","NO_DATE");
			set("item_date_year","NO_DATE");
		} else {
			set("item_date_short",itemDate.toString("YYYYMMdd"));
			set("item_date_long",itemDate.toString("YYYYMMdd-HHmmss"));
			set("item_date_year",itemDate.toString("YYYY"));
		}
		
		Item topLevelItem = item.getTopLevelItem();
		if(topLevelItem == null) {
			set("top_level_guid","ABOVE_TOP_LEVEL");
			set("top_level_name","ABOVE_TOP_LEVEL");
			set("top_level_kind","ABOVE_TOP_LEVEL");
		} else {
			set("top_level_guid",topLevelItem.getGuid());
			set("top_level_name",topLevelItem.getLocalisedName());
			set("top_level_kind",topLevelItem.getType().getKind().getName());
		}
		
		String originalExtension = item.getOriginalExtension();
		if(originalExtension == null || originalExtension.trim().isEmpty()) { originalExtension = "NO_ORIGINAL_EXTENSION"; }
		set("original_extension",originalExtension);
		
		String correctedExtension = item.getCorrectedExtension();
		if(correctedExtension == null || correctedExtension.trim().isEmpty()) { correctedExtension = "NO_CORRECTED_EXTENSION"; }
		set("corrected_extension",correctedExtension);
	}
	
	/***
	 * Set they value for a given placeholder
	 * @param key The placeholder name without '{' or '}'
	 * @param value The value to associate
	 */
	public void set(String key, String value) {
		key = key.toLowerCase();
		placeholderData.put(key,value);
		recordPlaceholderPattern(key);
	}
	
	/***
	 * Similar to the {@link #set} method except this has logic to appropriately handle file paths.
	 * @param key The placeholder name without '{' or '}' 
	 * @param value The file/directory path value to associate
	 */
	public void setPath(String key, String value) {
		key = key.toLowerCase();
		placeholderData.put(key,value);
		recordPlaceholderPattern(key);
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
		placeholderPaths.clear();
		placeholderPatterns.clear();
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
	 * Resolves place holders into a string based on the currently associated values and the multi-value place holders provided.  For example
	 * imagine you wish to render the template 1 time for each tag associated to an item that has 3 tags assigned.  You can call this method,
	 * providing a {@link NamedStringList} with a name of <code>tags</code> and those 3 tags as values.  This method will then return 3 resolutions
	 * of a template containing the placeholder <code>{tags}</code>.  Each returned result containing 1 of the 3 tags substituted.  This method allows
	 * you to provide multiple multi-value place holders like this, but it is important to note the number of resulting values is multiplied.  So if I provide
	 * a placeholder <code>animals</code> with 3 values, a placeholder <code>colors</code> with 5 values and a placeholder <code>names</code> with 4 values, the number
	 * of possible resulting values is <code>3*5*4=60</code>.
	 * @param template The template to resolve
	 * @param multiValuePlaceholders Place holders to resolve multiple times with multiple values.
	 * @return The various resulting values generated from all the combinations.
	 */
	public Set<String> resolveTemplateMultiValues(String template, List<NamedStringList> multiValuePlaceholders){
		Set<String> resolvedValues = new HashSet<String>();
		resolveTemplateMultiValuesRecursive(template,multiValuePlaceholders,0,resolvedValues);
		return resolvedValues;
	}
	
	private void resolveTemplateMultiValuesRecursive(String template, List<NamedStringList> multiValuePlaceholders, int namedListIndex, Set<String> results) {
		if(namedListIndex == multiValuePlaceholders.size() - 1) {
			NamedStringList nsl = multiValuePlaceholders.get(namedListIndex);
			nsl.forEach(v -> {
				set(nsl.getName(),v);
				results.add(resolveTemplate(template));
			});
		} else {
			NamedStringList nsl = multiValuePlaceholders.get(namedListIndex);
			nsl.forEach(v -> {
				set(nsl.getName(),v);
				resolveTemplateMultiValuesRecursive(template,multiValuePlaceholders,namedListIndex+1,results);
			});
		}
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
	 * Resolves place holders into a string based on the currently associated values and the multi-value place holders provided.  For example
	 * imagine you wish to render the template 1 time for each tag associated to an item that has 3 tags assigned.  You can call this method,
	 * providing a {@link NamedStringList} with a name of <code>tags</code> and those 3 tags as values.  This method will then return 3 resolutions
	 * of a template containing the placeholder <code>{tags}</code>.  Each returned result containing 1 of the 3 tags substituted.  This method allows
	 * you to provide multiple multi-value place holders like this, but it is important to note the number of resulting values is multiplied.  So if I provide
	 * a placeholder <code>animals</code> with 3 values, a placeholder <code>colors</code> with 5 values and a placeholder <code>names</code> with 4 values, the number
	 * of possible resulting values is <code>3*5*4=60</code>.
	 * This method is similar to {{@link #resolveTemplateMultiValues(String, List)} except the template is resolved using {{@link #resolveTemplatePath(String)} instead
	 * of the method {{@link #resolveTemplate(String)} which is used by {{@link #resolveTemplateMultiValues(String, List)}. 
	 * @param template The template to resolve
	 * @param multiValuePlaceholders Place holders to resolve multiple times with multiple values.
	 * @return The various resulting values generated from all the combinations.
	 */
	public Set<String> resolveTemplatePathMultiValues(String template, List<NamedStringList> multiValuePlaceholders){
		Set<String> resolvedValues = new HashSet<String>();
		resolveTemplatePathMultiValuesRecursive(template,multiValuePlaceholders,0,resolvedValues);
		return resolvedValues;
	}
	
	private void resolveTemplatePathMultiValuesRecursive(String template, List<NamedStringList> multiValuePlaceholders, int namedListIndex, Set<String> results) {
		if(namedListIndex == multiValuePlaceholders.size() - 1) {
			NamedStringList nsl = multiValuePlaceholders.get(namedListIndex);
			nsl.forEach(v -> {
				set(nsl.getName(),v);
				results.add(resolveTemplatePath(template));
			});
		} else {
			NamedStringList nsl = multiValuePlaceholders.get(namedListIndex);
			nsl.forEach(v -> {
				set(nsl.getName(),v);
				resolveTemplatePathMultiValuesRecursive(template,multiValuePlaceholders,namedListIndex+1,results);
			});
		}
	}
	
	/***
	 * Helper method to strip common illegal path characters from a string 
	 * @param input The string to clean up
	 * @return The string with illegal path characters replaced with '_'
	 */
	public static String cleanPathString(String input){
		return input.replaceAll("[\\Q<>:\\/\"|?*[]\\E]","_");
	}
}