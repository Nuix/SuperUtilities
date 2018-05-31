package com.nuix.superutilities.annotations;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import nuix.Case;
import nuix.ItemSet;

public class ItemSetEvent extends AnnotationEvent {

	Boolean added = null;
	String settings = null;
	String itemSetName = null;
	String batchName = null;
	String description = null;
	
	@Override
	public void replay(Case nuixCase) throws IOException {
		ItemSet itemSet = getOrCreateRelatedItemSet(nuixCase);
		if(added){
			Map<String,Object> settings = new HashMap<String,Object>();
			settings.put("batch",batchName);
			itemSet.addItems(getAssociatedItems(nuixCase), settings);
		} else {
			itemSet.removeItems(getAssociatedItems(nuixCase));
		}
	}

	public Boolean getAdded() {
		return added;
	}

	@SuppressWarnings("unchecked")
	public Map<String,Object> getSettings() {
		Gson gson = null;
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.serializeNulls();
		gson = gsonBuilder.create();
		return gson.fromJson(settings,new HashMap<String,Object>().getClass());
	}

	public String getItemSetName() {
		return itemSetName;
	}

	public String getBatchName() {
		return batchName;
	}

	public ItemSet getRelatedItemSet(Case nuixCase){
		return nuixCase.findItemSetByName(itemSetName);
	}
	
	public ItemSet getOrCreateRelatedItemSet(Case nuixCase){
		ItemSet itemSet = getRelatedItemSet(nuixCase);
		if(itemSet == null){
			Map<String,Object> settings = getSettings();
			settings.put("description", description);
			itemSet = nuixCase.createItemSet(itemSetName, settings);
		}
		return itemSet;
	}
	
	@Override
	public String toString() {
		if(added){
			return String.format("ItemSetEvent[%s]: %s items added to item set '%s' as batch '%s'",
					timeStamp,itemCount,itemSetName,batchName);
		} else {
			return String.format("ItemSetEvent[%s]: %s items removed from item set '%s'",
					timeStamp,itemCount,itemSetName);
		}
	}
}
