package com.nuix.superutilities.misc;

/***
 * Interface for callback for free space monitoring offered by {@link com.nuix.superutilities.misc.FreeSpaceMonitor}.
 * @author Jason Wells
 *
 */
public interface FreeSpaceMonitorEventCallback {
	public void monitorEventOccurred(FreeSpaceMonitor monitor, DriveSpaceInfo driveInfo);
}
