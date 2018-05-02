package com.nuix.superutilities.cases;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

import nuix.Case;
import nuix.HistoryEvent;

/***
 * Helper class for iterating a case's history.  The reason for this class is mainly for the functionality
 * to iterate multiple event types at once without having to iterate them all.  Same functionality in API
 * requires you either return all history event types or just a single type.
 * @author Jason Wells
 *
 */
public class CaseHistoryHelper implements Iterator<HistoryEvent> {
	private Map<String,Iterator<HistoryEvent>> eventIterators;
	private Map<String,HistoryEvent> currentEvents;
	
	public CaseHistoryHelper(Case nuixCase,List<String> eventTypes, DateTime minStart, DateTime maxStart) throws Exception{
		eventIterators = new HashMap<String,Iterator<HistoryEvent>>();
		
		Map<String,Object> settings = new HashMap<String,Object>();
		settings.put("order", "start_date_ascending");
		settings.put("startDateAfter", minStart);
		settings.put("startDateBefore", maxStart);
		for(String eventType : eventTypes){
			settings.put("type", eventType);
			eventIterators.put(eventType, nuixCase.getHistory(settings).iterator());
		}
		
		// Seed initial history events
		currentEvents = new HashMap<String,HistoryEvent>();
		for(String eventType : eventTypes){
			if(eventIterators.get(eventType).hasNext()){
				currentEvents.put(eventType, eventIterators.get(eventType).next());	
			}
		}
	}

	@Override
	public boolean hasNext() {
		return currentEvents.size() > 0;
	}

	@Override
	public HistoryEvent next() {
		String earliestEventTypeName = null;
		Long earliestEventMillis = null;
		HistoryEvent nextEvent = null;
		
		for(Map.Entry<String, HistoryEvent> entry : currentEvents.entrySet()){
			long millis = entry.getValue().getStartDate().getMillis();
			if(earliestEventMillis == null || millis < earliestEventMillis){
				earliestEventTypeName = entry.getValue().getTypeString();
				earliestEventMillis = millis;
				nextEvent = entry.getValue();
			}
		}
		
		if (eventIterators.get(earliestEventTypeName).hasNext()){
			currentEvents.put(earliestEventTypeName, eventIterators.get(earliestEventTypeName).next());
		} else {
			eventIterators.remove(earliestEventTypeName);
			currentEvents.remove(earliestEventTypeName);
		}
		
		return nextEvent;
	}
	
	/***
	 * Convenience method for obtaining earliest case history event.
	 * @param nuixCase Case to obtain history event from
	 * @return Earliest case history event
	 * @throws Exception Thrown if an error occurs
	 */
	public static DateTime getEarliestEventStart(Case nuixCase) throws Exception{
		Map<String,Object> settings = new HashMap<String,Object>();
		settings.put("order", "start_date_ascending");
		Iterator<HistoryEvent> iter = nuixCase.getHistory(settings).iterator();
		if(iter.hasNext()){
			return iter.next().getStartDate();
		} else {
			return null;
		}
	}
	
	/***
	 * Convenience method for obtaining most recent case history event.
	 * @param nuixCase Case to obtain history event from
	 * @return Earliest case history event
	 * @throws Exception Thrown if an error occurs
	 */
	public static DateTime getLatestEventStart(Case nuixCase) throws Exception{
		Map<String,Object> settings = new HashMap<String,Object>();
		settings.put("order", "start_date_descending");
		Iterator<HistoryEvent> iter = nuixCase.getHistory(settings).iterator();
		if(iter.hasNext()){
			return iter.next().getStartDate();
		} else {
			return null;
		}
	}
}
