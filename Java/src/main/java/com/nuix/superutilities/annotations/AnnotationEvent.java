package com.nuix.superutilities.annotations;

import java.io.IOException;
import java.util.Collection;

import org.joda.time.DateTime;

import nuix.Case;
import nuix.Item;

public abstract class AnnotationEvent {
	// Package accessible members
	DateTime timeStamp = null;
	AnnotationHistoryRepository sourceRepo = null;
	byte[] bitmapBytes = null;
	Integer itemCount = -1;
	
	public abstract void replay(Case nuixCase) throws Exception;
	
	public Collection<Item> getAssociatedItems(Case nuixCase) throws IOException{
		return sourceRepo.rehydrateItemCollection(nuixCase, bitmapBytes);
	}

	public DateTime getTimeStamp() {
		return timeStamp;
	}

	public AnnotationHistoryRepository getSourceRepo() {
		return sourceRepo;
	}
	
	public Integer getExpectedItemCount(){
		return itemCount;
	}
}
