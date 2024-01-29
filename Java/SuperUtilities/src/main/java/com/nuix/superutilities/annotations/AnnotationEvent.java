package com.nuix.superutilities.annotations;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import nuix.Case;
import nuix.Item;

public abstract class AnnotationEvent {
	private static Logger logger = Logger.getLogger(AnnotationEvent.class);
	
	// Package accessible members
	DateTime timeStamp = null;
	AnnotationHistoryRepository sourceRepo = null;
	byte[] bitmapBytes = null;
	Integer itemCount = -1;
	
	public abstract void replay(Case nuixCase) throws Exception;
	
	public Collection<Item> getAssociatedItems(Case nuixCase) throws Exception{
		if(nuixCase == null){ throw new Exception("Nuix case cannot be null"); }
		if(nuixCase.isClosed()){ throw new Exception("Nuix case cannot be closed"); }
		if(bitmapBytes == null){ throw new Exception("bitmapBytes cannot be null"); }
		
		Collection<Item> result = sourceRepo.rehydrateItemCollection(nuixCase, bitmapBytes);
		if(result.size() != getExpectedItemCount()){
			logger.warn(String.format("%s.getAssociatedItems returned %s items while DB states expected count to be %s",
					this.getClass().getName(),result.size(),getExpectedItemCount()));
		}
		return result;
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
