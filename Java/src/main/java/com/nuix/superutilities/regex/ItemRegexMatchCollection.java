package com.nuix.superutilities.regex;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import nuix.Item;

/***
 * Represents an item and the associated regular expression matches made against it.
 * @author Jason Wells
 *
 */
public class ItemRegexMatchCollection {
	private Item item = null;
	private List<RegexMatch> matchData = new ArrayList<RegexMatch>();
	
	/***
	 * Creates a new empty instance against the specified item.
	 * @param item The item to associated.
	 */
	public ItemRegexMatchCollection(Item item){
		this.item = item;
	}
	
	/***
	 * Gets the associated item.
	 * @return The associated item.
	 */
	public Item getItem(){
		return item;
	}
	
	/***
	 * Gets the matches associated.
	 * @return The matches associated.
	 */
	public List<RegexMatch> getMatches(){
		return matchData;
	}
	
	/***
	 * Get the matches associated specifically to the item's content text.
	 * @return The matches associated specifically to the item's content text.
	 */
	public List<RegexMatch> getContentMatches(){
		return matchData.stream().filter(m -> m.isContentMatch()).collect(Collectors.toList());
	}
	
	/***
	 * Gets the matches associated specifically to the item's property text.
	 * @return The matches associated specifically to the item's property text.
	 */
	public List<RegexMatch> getPropertyMatches(){
		return matchData.stream().filter(m -> !m.isContentMatch()).collect(Collectors.toList());
	}
	
	/***
	 * Adds a match to this instance.
	 * @param patternInfo The pattern info which made this match
	 * @param location Location match was made (ex: Content, Property Name)
	 * @param isContentMatch True is this match was made against the item's content text
	 * @param value Value text of the match made.
	 * @param valueContext Contextual text associated with this match (can be empty)
	 * @param matchStart Offset in source text where this match begins
	 * @param matchEnd Offset in source text where this match ends
	 */
	public void addMatch(PatternInfo patternInfo, String location, boolean isContentMatch, String value, String valueContext, int matchStart, int matchEnd){
		matchData.add(new RegexMatch(patternInfo,location,isContentMatch,value,valueContext,matchStart,matchEnd));
	}
	
	/***
	 * Gets the number of matches currently associated with this instance.
	 * @return The number of matches associated with this instance.
	 */
	public int getMatchCount(){
		return matchData.size();
	}
}
