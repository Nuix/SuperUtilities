package com.nuix.superutilities.annotations;

import com.nuix.superutilities.SuperUtilities;

import nuix.BulkAnnotater;
import nuix.Case;

public class ExclusionEvent extends AnnotationEvent {

	Boolean excluded = null;
	String exclusionName = null;
	
	@Override
	public void replay(Case nuixCase) throws Exception {
		BulkAnnotater annotater = SuperUtilities.getBulkAnnotater();
		if(excluded){
			annotater.exclude(exclusionName, getAssociatedItems(nuixCase));
		} else {
			annotater.include(getAssociatedItems(nuixCase));
		}
	}

	public Boolean getExcluded() {
		return excluded;
	}

	public String getExclusionName() {
		return exclusionName;
	}

	@Override
	public String toString() {
		if(excluded){
			return String.format("ExclusionEvent[%s]: %s items excluded as '%s'",
					timeStamp,itemCount,exclusionName);
		} else {
			return String.format("ExclusionEvent[%s]: %s items included",
					timeStamp,itemCount);
		}
	}
}
