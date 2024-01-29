package com.nuix.superutilities.annotations;

import java.util.HashMap;
import java.util.Map;

import nuix.Case;
import nuix.ProductionSet;

public class ProductionSetEvent extends AnnotationEvent {
	boolean added = false;
	boolean created = false;
	String settingsJsonString = "{}";
	Map<String,Object> settings = new HashMap<String,Object>();
	String productionSetName = "";
	
	@Override
	public void replay(Case nuixCase) throws Exception {
		ProductionSet prodSet = nuixCase.findProductionSetByName(productionSetName);
		if(prodSet == null){
			prodSet = nuixCase.newProductionSet(productionSetName, settings);
			prodSet.setNumberingOptions((Map<?, ?>) settings.get("numberingOptions"));
			prodSet.setImagingOptions((Map<?, ?>) settings.get("imagingOptions"));
			prodSet.setStampingOptions((Map<?, ?>) settings.get("stampingOptions"));
			prodSet.setApplyRedactions((boolean) settings.get("applyRedactions"));
			prodSet.setApplyHighlights((boolean) settings.get("applyHighlights"));
		}
		if(added){
			prodSet.addItems(this.getAssociatedItems(nuixCase));
		} else {
			prodSet.removeItems(this.getAssociatedItems(nuixCase));
		}
	}

	@Override
	public String toString() {
		if(created){
			return String.format("ProductionSetEvent[%s]: Production '%s' set created with settings:\n%s",
					timeStamp,productionSetName,settingsJsonString);
		} else {
			if(added){
				return String.format("ProductionSetEvent[%s]: %s items added to production set '%s'",
						timeStamp,itemCount,productionSetName);
			} else {
				//Removed
				return String.format("ProductionSetEvent[%s]: %s items removed from production set '%s'",
						timeStamp,itemCount,productionSetName);
			}
		}
	}
}
