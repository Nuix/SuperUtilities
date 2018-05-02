package com.nuix.superutilities.misc;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.joda.time.DateTime;

/***
 * Encapsulates drive space information to callback responding to events fired
 * by {@link FreeSpaceMonitor}.
 * @author Jason Wells
 *
 */
public class DriveSpaceInfo {
	private String pathString;
	private long totalBytes = 0;
	private long freeBytes = 0;
	private boolean hadError = false;
	private Exception error;
	private DateTime lastUpdated = null;
	
	/***
	 * Creates a new instance for the specified file path.
	 * @param pathString The file path
	 */
	public DriveSpaceInfo(String pathString){
		this.pathString = pathString;
		refresh();
	}
	
	/***
	 * Causes this instance to refresh values for total bytes and free bytes returned by methods
	 * {@link #getTotalBytes()} and {@link #getFreeBytes()}.
	 */
	public void refresh(){
		try {
			Path path = Paths.get(pathString);
			FileStore store = Files.getFileStore(path);
			this.totalBytes = store.getTotalSpace();
			this.freeBytes = store.getUsableSpace();
		} catch (IOException e) {
			this.hadError = true;
			this.error = e;
		}
		lastUpdated = DateTime.now();
	}

	/***
	 * Gets the path this instance is associated with.
	 * @return The associate path string
	 */
	public String getPathString() {
		return pathString;
	}

	public long getTotalBytes() {
		return totalBytes;
	}

	public long getFreeBytes() {
		return freeBytes;
	}

	public boolean hadError() {
		return hadError;
	}

	public Exception getError() {
		return error;
	}
	
	public double getTotalGigaBytes(){
		return FormatUtility.bytesToGigaBytes(totalBytes, 3);
	}
	
	public double getFreeGigaBytes(){
		return FormatUtility.bytesToGigaBytes(freeBytes, 3);
	}

	public DateTime getLastUpdated() {
		return lastUpdated;
	}

	@Override
	public String toString() {
		return "DriveSpaceInfo [PathString=" + getPathString() + ", hadError=" + hadError()
				+ ", TotalGigaBytes=" + getTotalGigaBytes() + ", FreeGigaBytes=" + getFreeGigaBytes()
				+ ", LastUpdated=" + getLastUpdated() + "]";
	}
	
	
}
