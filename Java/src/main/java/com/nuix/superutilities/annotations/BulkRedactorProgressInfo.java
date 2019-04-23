package com.nuix.superutilities.annotations;

public class BulkRedactorProgressInfo {
	private int current = 0;
	private int total = 0;
	private int matches = 0;
	
	public int getCurrent() {
		return current;
	}
	public void setCurrent(int current) {
		this.current = current;
	}
	public int getTotal() {
		return total;
	}
	public void setTotal(int total) {
		this.total = total;
	}
	public int getMatches() {
		return matches;
	}
	public void setMatches(int matches) {
		this.matches = matches;
	}
}
