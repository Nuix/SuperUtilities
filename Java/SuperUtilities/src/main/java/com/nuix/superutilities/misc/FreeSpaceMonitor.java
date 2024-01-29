package com.nuix.superutilities.misc;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;

/***
 * Provides a way to monitor file system locations and act when available disk space drops
 * below a specified threshold.
 * @author Jason Wells
 *
 */
public class FreeSpaceMonitor {
	
	private static Logger logger = Logger.getLogger(FreeSpaceMonitor.class);
	
	// Represents states a monitored location can be in
	enum MonitoredLocationState {
		NORMAL,
		LOW_SPACE,
	}
	
	// Internal class used to track information about a particular tracked location
	class MonitoredLocation {
		public DriveSpaceInfo driveInfo;
		public double gbThreshold;
		public MonitoredLocationState currentState = MonitoredLocationState.NORMAL;
	}
	
	private Thread monitorThread;
	private boolean continuePolling = true;
	private int pollingIntervalSeconds = 15;
	private long lastPollTime = 0;
	
	private FreeSpaceMonitorEventCallback lowSpaceCallback = null;
	private FreeSpaceMonitorEventCallback spaceIssueResolvedCallback = null;
	private FreeSpaceMonitorEventCallback errorCallback = null;
		
	private List<MonitoredLocation> monitoredLocations = new CopyOnWriteArrayList<MonitoredLocation>();
	
	/***
	 * Adds a location to be monitored
	 * @param pathString Path string to the location to be monitored
	 * @param freeSpaceThresholdGb Free space threshold to begin reacting to
	 */
	public void addMonitoredLocation(String pathString, double freeSpaceThresholdGb){
		MonitoredLocation location = new MonitoredLocation();
		location.driveInfo = new DriveSpaceInfo(pathString);
		location.gbThreshold = freeSpaceThresholdGb;
		monitoredLocations.add(location);
	}
	
	/***
	 * Allows you to register a callback which will be invoked when a monitored location
	 * is found to dip below the free space threshold.
	 * @param callback The callback to be invoked
	 */
	public void whenFreeSpaceBelowThreshold(FreeSpaceMonitorEventCallback callback){
		lowSpaceCallback = callback;
	}
	
	/***
	 * Informs callback of low space event
	 * @param location The monitored location where low space was detected
	 */
	private void fireLowSpaceEvent(MonitoredLocation location){
		if(lowSpaceCallback != null){
			lowSpaceCallback.monitorEventOccurred(this, location.driveInfo);
		}
	}
	
	/***
	 * Allows you to register a callback which will be invoked when a monitored location
	 * which previously was below a free space threshold is no longer below that threshold.
	 * @param callback The callback to be invoked
	 */
	public void whenFreeSpaceIssueResolved(FreeSpaceMonitorEventCallback callback){
		spaceIssueResolvedCallback = callback;
	}
	
	/***
	 * Informs callback of low space resolution event
	 * @param location The monitored location where low space was resolved
	 */
	private void fireFreeSpaceIssueResolved(MonitoredLocation location){
		if(spaceIssueResolvedCallback != null){
			spaceIssueResolvedCallback.monitorEventOccurred(this, location.driveInfo);
		}
	}
	
	/***
	 * Allows you to register a call which will be invoked when a monitoring error occurs.
	 * @param callback The callback to be invoked
	 */
	public void whenErrorOccurs(FreeSpaceMonitorEventCallback callback){
		errorCallback = callback;
	}
	
	/***
	 * Informs callback of monitoring error event.
	 * @param location The location being polled when the error occurred.
	 */
	private void fireErrorEvent(MonitoredLocation location){
		if(errorCallback != null){
			errorCallback.monitorEventOccurred(this, location.driveInfo);
		}
	}
	
	/***
	 * Begins monitoring locations by polling them at intervals.
	 * Should later be followed by a call to {@link #shutdownMonitoring()} once monitoring is
	 * no longer needed.
	 */
	public void beginMonitoring(){
		continuePolling = true;
		monitorThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while(continuePolling){
					if(System.currentTimeMillis() - lastPollTime > (pollingIntervalSeconds * 1000 )){
						for (MonitoredLocation monitoredLocation : monitoredLocations) {
							monitoredLocation.driveInfo.refresh();
							if(monitoredLocation.driveInfo.hadError()){
								fireErrorEvent(monitoredLocation);
							}
							else if(monitoredLocation.driveInfo.getFreeGigaBytes() <= monitoredLocation.gbThreshold &&
									monitoredLocation.currentState == MonitoredLocationState.NORMAL){
								monitoredLocation.currentState = MonitoredLocationState.LOW_SPACE;
								fireLowSpaceEvent(monitoredLocation);
							} else if (monitoredLocation.driveInfo.getFreeGigaBytes() > monitoredLocation.gbThreshold &&
									monitoredLocation.currentState == MonitoredLocationState.LOW_SPACE){
								monitoredLocation.currentState = MonitoredLocationState.NORMAL;
								fireFreeSpaceIssueResolved(monitoredLocation);
							}
						}
						lastPollTime = System.currentTimeMillis();
					}
				}
				logger.info("Shutting down drive space monitor thread...");
			}
		});
		logger.info("Starting drive space monitor thread...");
		monitorThread.start();
	}
	
	/***
	 * Signals to polling thread that it should wind down and stop polling the monitored locations.
	 * See method {@link #beginMonitoring()} for how to begin monitoring.
	 */
	public void shutdownMonitoring(){
		logger.info("Requestion drive space monitor thread shutdown...");
		continuePolling = false;
	}
}
