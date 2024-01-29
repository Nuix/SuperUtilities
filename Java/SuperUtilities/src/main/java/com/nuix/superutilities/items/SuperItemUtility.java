package com.nuix.superutilities.items;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.nuix.superutilities.SuperUtilities;

import nuix.Case;
import nuix.Item;
import nuix.ItemUtility;
import nuix.TreePosition;

/***
 * Offers methods for working with items and item collections in the same spirit of the
 * ItemUtility offered in the Nuix API.
 * @author Jason Wells
 *
 */
public class SuperItemUtility {
	
	private static SuperItemUtility instance = null;
	
	protected SuperItemUtility(){}
	
	public static SuperItemUtility getInstance(){
		if(instance == null){
			instance = new SuperItemUtility();
		}
		return instance;
	}
	
	/***
	 * Unions multiple collections of items into a single Set of items.
	 * @param itemCollections A list of item collections to union into a single final Set
	 * @return A set at most 1 of each item in the provided input item collections
	 */
	public Set<Item> unionMany(List<Collection<Item>> itemCollections){
		ItemUtility iutil = SuperUtilities.getItemUtility();
		Set<Item> result = new HashSet<Item>();
		for (int i = 0; i < itemCollections.size(); i++) {
			result = iutil.union(result, itemCollections.get(i));
		}
		return result;
	}
	
	/***
	 * Traverses up an item's path to locate the ancestor physical file item (flag:physical_file) or
	 * null if there is no physical file ancestor item.
	 * @param item The item to find the physical file ancestor for.
	 * @return The physical file ancestor item (if there is on) otherwise null
	 */
	public Item findPhysicalFileAncestor(Item item){
		Item physicalFileAncestor = null;
		List<Item> pathItems = item.getPath();
		for (int i = pathItems.size()-1; i >= 0; i--) {
			Item currentItem = pathItems.get(i);
			if(currentItem.isPhysicalFile() && currentItem != item){
				physicalFileAncestor = currentItem;
				break;
			}
		}
		return physicalFileAncestor;
	}
	
	/***
	 * Similar to {@link #findPhysicalFileAncestor(Item)}, but resolves physical file ancestor items
	 * for multiple items at once.  Resulting set will not contain nulls.
	 * @param items The items to resolve the physical file ancestors of.
	 * @return The physical file ancestors of the input items (if there were any)
	 */
	public Set<Item> findPhysicalFileAncestors(Collection<Item> items){
		Set<Item> result = items.parallelStream()
				.map(i -> findPhysicalFileAncestor(i))
				.filter(i -> i != null)
				.collect(Collectors.toSet());
		return result;
	}
	
	/***
	 * Traverses up an item's path to locate the nearest ancestor container item (kind:container) or
	 * null if there is no container ancestor item.
	 * @param item The item to resolve the nearest container ancestor of.
	 * @return The nearest container ancestor of the input item or null if no container ancestor could be found
	 */
	public Item findContainerAncestor(Item item){
		Item containerAncestor = null;
		List<Item> pathItems = item.getPath();
		for (int i = pathItems.size()-1; i >= 0; i--) {
			Item currentItem = pathItems.get(i);
			if(currentItem.isKind("container") && currentItem != item){
				containerAncestor = currentItem;
				break;
			}
		}
		return containerAncestor;
	}
	
	/***
	 * Similar to {@link #findContainerAncestor(Item)}, but resolves nearest container ancestor items
	 * for multiple items at once.  Resulting set will not contain nulls.
	 * @param items The items to resolve the nearest container ancestors of.
	 * @return The nearest container ancestors of the input items.
	 */
	public Set<Item> findContainerAncestors(Collection<Item> items){
		Set<Item> result = items.parallelStream()
				.map(i -> findContainerAncestor(i))
				.filter(i -> i != null)
				.collect(Collectors.toSet());
		return result;
	}
	
	/***
	 * Splits a collection of items into a target chunk size, while maintaining families.  Each chunk is
	 * passed as an argument to the provided chunkConsumer.
	 * @param items The items to split
	 * @param targetChunkSize The target chunk size.  Actual chunks may differ from this size based on where
	 * family boundaries land.
	 * @param chunkConsumer A Consumer&lt;Collection&lt;Item&gt;&gt; which will receive each chunk as it is built.
	 */
	public void splitAndMaintainFamilies(Collection<Item> items, int targetChunkSize, Consumer<Collection<Item>> chunkConsumer){
		List<Item> currentSubSet = new ArrayList<Item>();
		List<Item> sortedItems = new ArrayList<Item>(items);
		sortedItems = SuperUtilities.getItemUtility().sortItemsByPosition(sortedItems);
		
		Item previousItem = null;
		for (int i = 0; i < sortedItems.size(); i++) {
			Item currentItem = sortedItems.get(i);
			boolean canCutHere = previousItem == null || currentItem.getTopLevelItem() == null || (currentItem.getTopLevelItem() != previousItem.getTopLevelItem());
			if(currentSubSet.size() >= targetChunkSize && canCutHere){
				chunkConsumer.accept(currentSubSet);
				currentSubSet = new ArrayList<Item>();
			}
			currentSubSet.add(currentItem);
		}
		
		if(currentSubSet.size() > 0){
			chunkConsumer.accept(currentSubSet);
		}
	}
	
	/***
	 * Convenience method for removing items responsive to a query from another collection of items.  Internally this
	 * method runs a search for the given query and then uses ItemUtility.difference to remove them from the provided
	 * input items, returning the differenced result.
	 * @param nuixCase The Nuix case (needed to run the search)
	 * @param inputItems The items from which you wish to remove items which are responsive to the given query
	 * @param itemsToRemoveQuery The query used to define items you wish to have removed form the input items
	 * @return The input items, with items responsive to the query removed
	 * @throws IOException Likely thrown if there is an issue with the provided query
	 */
	public Set<Item> removeItemsResponsiveToQuery(Case nuixCase, Collection<Item> inputItems, String itemsToRemoveQuery) throws IOException {
		Set<Item> toRemove = nuixCase.searchUnsorted(itemsToRemoveQuery);
		return SuperUtilities.getItemUtility().difference(inputItems, toRemove);
	}
	
	public Set<Item> findFamiliesWithoutItemsResponsiveToQuery(Case nuixCase, Collection<Item> inputItems, String itemsToRemoveQuery) throws IOException {
		Set<Item> topLevelItems = SuperUtilities.getItemUtility().findTopLevelItems(inputItems);
		Set<Item> families = SuperUtilities.getItemUtility().findItemsAndDescendants(topLevelItems);
		return removeItemsResponsiveToQuery(nuixCase,families,itemsToRemoveQuery);
	}
	
	/***
	 * Returns the file system path of an item's physical file ancestor.  Begins by calling {@link #findPhysicalFileAncestor(Item)}.
	 * If an physical file ancestor is located, then gets its URI and attempts to convert that to an actual file system path.
	 * @param item The item to resolve the physical file ancestor file system path of.
	 * @return The physical file ancestor file system path (if possible), otherwise null
	 */
	public String getPhysicalAncestorPath(Item item){
		Item physicalAncestor = findPhysicalFileAncestor(item);
		if(physicalAncestor == null){
			return "";
		}
		
		String uri = physicalAncestor.getUri();
		if(uri == null){
			return "";
		} else {
			uri = uri.replaceAll("\\\\+", "%2B");
			try {
				String path = URLDecoder.decode(uri,"UTF-8");
				path = path.replaceAll("^file:\\\\/\\\\/\\\\/", "");
				path = path.replaceAll("\\\\/", "\\\\");
				return path;
			} catch (UnsupportedEncodingException e) {
				return "Error decoding: "+uri+", "+e.getMessage();
			}
		}
	}
	
	/***
	 * Custom deduplication implementation allowing code to specify which original is kept.  The normal behavior
	 * of ItemUtility.deduplicate is that the original with the earliest position value, amongst the items with the same
	 * MD5, is considered the original.  This implementation allows code to provide a 2 argument function which will
	 * determine the winner.  It may be a good idea for the custom function provided to default to the default position
	 * behavior when all other comparisons it may perform are equal, to mimic the behavior of the API.  Like the API
	 * deduplicate method, items without an MD5 value are automatically included in the results and therefore never
	 * sent to the tie breaker function.
	 * @param items The items to deduplicate
	 * @param tieBreaker A function which is provided 2 items with the same MD5, the first argument is the current
	 * "champion" item (the item currently considered original) and the second argument is the "contender" item
	 * (the item which may become the new champion).  If function returns the "contender" item, then it becomes
	 * the new "champion", any other result (including null) leaves the current "champion" in place.
	 * @return A custom deduplicated set of items
	 */
	public Set<Item> deduplicateCustomTieBreaker(Collection<Item> items, BiFunction<Item,Item,Item> tieBreaker){
		Map<String,Item> working = new HashMap<String,Item>();
		List<Item> noMd5 = new ArrayList<Item>();
		for(Item item : items){
			String md5 = item.getDigests().getMd5();
			
			if(md5 == null || md5.trim().isEmpty()){
				noMd5.add(item);
				continue;
			}
			
			if(!working.containsKey(md5)){
				working.put(md5,item);
				continue;
			} else {
				Item currentChampion = working.get(md5);
				Item contender = item;
				Item winner = tieBreaker.apply(currentChampion, contender);
				if(winner == contender){
					working.put(md5,winner);
				}
			}
		}
		Set<Item> result = new HashSet<Item>(working.values());
		result.addAll(noMd5);
		return result;
	}
	
	/***
	 * Tests whether 2 items have the same parent by comparing their tree position values
	 * @param a The first item to compare
	 * @param b The second item to compare
	 * @return True if items appear to have the same parent item based on their tree position values
	 */
	public boolean itemsAreSiblings(Item a, Item b){
		TreePosition posA = a.getPosition();
		TreePosition posB = b.getPosition();
		int[] arrA = posA.toArray();
		int[] arrB = posB.toArray();
		
		// If position array length differs they are at
		// different depths and therefore cannot be siblings
		if(arrA.length != arrB.length){
			return false;
		}
		
		// Test to make sure position values match up to
		// but not including the last position int
		for (int i = 0; i < arrA.length-1; i++) {
			if(arrA[i] != arrB[i]) return false;
		}
		
		return true;
	}
	
	/***
	 * Gets items and sibling items within a certain ordinal distance.
	 * @param items The items to obtain the neighboring siblings of
	 * @param itemsBefore How many siblings before each item to include
	 * @param itemsAfter How many siblings after each item to include
	 * @return A new list of items which includes both input items and neighboring siblings, sorted by position
	 */
	public List<Item> getItemsAndNeighboringSiblings(List<Item> items, int itemsBefore, int itemsAfter){
		if(itemsBefore < 0) { itemsBefore = 0; }
		if(itemsAfter < 0) { itemsAfter = 0; }
		
		if(items.size() < 1) { return Collections.emptyList(); }
		else {
			Set<Item> tempSet = new HashSet<Item>();
			for(Item item : items) {
				Item parent = item.getParent();
				List<Item> siblings = parent.getChildren();
				int itemIndex = siblings.indexOf(item);
				int first = itemIndex - itemsBefore;
				int last = itemIndex + itemsAfter;
				if(first < 0) { first = 0; }
				if(last > items.size() - 1) { last = items.size() - 1; }
				for (int i = first; i <= last; i++) {
					tempSet.add(items.get(i));
				}
			}
			List<Item> result = new ArrayList<Item>();
			result.addAll(tempSet);
			return SuperUtilities.getItemUtility().sortItemsByPosition(result);
		}
	}
}
