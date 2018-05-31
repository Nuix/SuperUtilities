package com.nuix.superutilities.annotations;

import java.io.IOException;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.nuix.superutilities.SuperUtilities;
import com.nuix.superutilities.misc.FormatUtility;

import nuix.BulkAnnotater;
import nuix.Case;

public class CustomMetadataEvent extends AnnotationEvent {

	Boolean added = null;
	String fieldName = null;
	String valueType = null;
	String valueTimeZone = null;
	Long valueLong = null;
	String valueText = null;
	byte[] valueBinary = null;
	
	@Override
	public void replay(Case nuixCase) throws IOException {
		BulkAnnotater annotater = SuperUtilities.getBulkAnnotater();
		if(added){
			annotater.putCustomMetadata(fieldName, getValue(), getAssociatedItems(nuixCase), valueType, "user", null, null);
		} else {
			annotater.removeCustomMetadata(fieldName, getAssociatedItems(nuixCase), null);
		}
	}

	public Boolean getAdded() {
		return added;
	}

	public String getFieldName() {
		return fieldName;
	}

	public String getValueType() {
		return valueType;
	}

	public DateTime getValueAsDateTime(){
		if(valueType.contentEquals("date-time")){
			DateTime value = new DateTime(valueLong).withZone(DateTimeZone.forID(valueTimeZone));
			return value;
		} else {
			return null;
		}
	}
	
	public Long getValueAsLong(){
		if(valueType.contentEquals("long") || valueType.contentEquals("integer")){
			return valueLong;
		} else {
			return null;
		}
	}
	
	public Integer getValueAsInteger(){
		if(valueType.contentEquals("long") || valueType.contentEquals("integer")){
			return Math.toIntExact(valueLong);
		} else {
			return null;
		}
	}
	
	public Float getValueAsFloat(){
		if(valueType.contentEquals("float")){
			return Float.parseFloat(valueText);
		} else {
			return null;
		}
	}
	
	public byte[] getValueAsByteArray(){
		if(valueType.contentEquals("binary")){
			return valueBinary;
		} else {
			return null;
		}
	}
	
	public String getValueAsString(){
		if(valueType.contentEquals("text") || valueType.contentEquals("float")){
			return valueText;
		} else {
			return FormatUtility.getInstance().convertToString(getValue());
		}
	}
	
	public Object getValue(){
		if(valueType.contentEquals("date-time")){
			return getValueAsDateTime();
		} else if(valueType.contentEquals("long")){
			return getValueAsLong();
		} else if(valueType.contentEquals("integer")){
			return getValueAsInteger();
		} else if(valueType.contentEquals("float")){
			return getValueAsFloat();
		} else if(valueType.contentEquals("binary")){
			return getValueAsByteArray();
		} else if(valueType.contentEquals("text")){
			return getValueAsString();
		}
		else {
			return null;
		}
	}
	
	@Override
	public String toString() {
		if(added){
			return String.format("CustomMetadataEvent[%s]: Field '%s' added to %s items with value '%s' of type '%s'",
					timeStamp,fieldName,itemCount,getValueAsString(),valueType);
		} else {
			return String.format("CustomMetadataEvent[%s]: Field '%s' removed from %s items",
					timeStamp,fieldName,itemCount);
		}
	}
}
