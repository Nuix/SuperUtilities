package com.nuix.superutilities.annotations;

import com.nuix.superutilities.SuperUtilities;

import nuix.BulkAnnotater;
import nuix.Case;

public class TagEvent extends AnnotationEvent {

	String tag = null;
	Boolean added = null;
	
	@Override
	public void replay(Case nuixCase) throws Exception {
		BulkAnnotater annotater = SuperUtilities.getBulkAnnotater();
		if(added){
			annotater.addTag(tag,getAssociatedItems(nuixCase));
		} else {
			annotater.removeTag(tag,getAssociatedItems(nuixCase));
		}
	}

	public String getTag() {
		return tag;
	}

	public Boolean getAdded() {
		return added;
	}
	
	@Override
	public String toString() {
		if(added){
			return String.format("TagEvent[%s]: Tag '%s' added to %s items",
					timeStamp,tag,itemCount);
		} else {
			return String.format("TagEvent[%s]: Tag '%s' removed from %s items",
					timeStamp,tag,itemCount);
		}
	}
}
