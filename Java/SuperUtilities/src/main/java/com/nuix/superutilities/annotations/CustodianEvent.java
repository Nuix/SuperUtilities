package com.nuix.superutilities.annotations;

import com.nuix.superutilities.SuperUtilities;

import nuix.BulkAnnotater;
import nuix.Case;

public class CustodianEvent extends AnnotationEvent {
	boolean assigned = false;
	String custodian = "";
	
	@Override
	public void replay(Case nuixCase) throws Exception {
		BulkAnnotater annotater = SuperUtilities.getBulkAnnotater();
		if(assigned){
			annotater.assignCustodian(custodian, getAssociatedItems(nuixCase));
		} else {
			annotater.unassignCustodian(getAssociatedItems(nuixCase));
		}
	}
	
	@Override
	public String toString() {
		if(assigned){
			return String.format("CustodianEvent[%s]: Custodian '%s' assigned to %s items",
					timeStamp,custodian,itemCount);
		} else {
			return String.format("CustodianEvent[%s]: Custodian un-assigned from %s items",
					timeStamp,itemCount);
		}
	}

	public boolean getAssigned() {
		return assigned;
	}

	public String getCustodian() {
		return custodian;
	}
}
