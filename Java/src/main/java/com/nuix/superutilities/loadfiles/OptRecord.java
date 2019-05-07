package com.nuix.superutilities.loadfiles;

public class OptRecord {
	// DOC-000000001_0001, ,IMAGE\000\000\001\DOC-000000001_0001.tif,Y,,,2
	// DOC-000000001_0002, ,IMAGE\000\000\001\DOC-000000001_0002.tif,,,,
	
	private String id = " ";
	private String volume = "";
	private String path = "";
	private String box = "";
	private String folder = "";
	private boolean firstPage = false;
	private int pages = 0;
	
	public static OptRecord fromLine(String line) {
		String[] pieces = line.trim().split(",",-1);
		OptRecord result = new OptRecord();
		result.id = pieces[0];
		result.volume = pieces[1];
		result.path = pieces[2];
		result.firstPage = pieces[3].toLowerCase().contentEquals("y");
		result.box = pieces[4];
		result.folder = pieces[5];
		if(pieces[6].trim().isEmpty()) {
			result.pages = 0;
		} else {
			result.pages = Integer.parseInt(pieces[6]);	
		}
		return result;
	}
	
	public String toLine() {
		String[] pieces = new String[7];
		pieces[0] = this.id;
		pieces[1] = this.volume;
		pieces[2] = this.path;
		pieces[3] = this.firstPage ? "Y" : "";
		pieces[4] = this.box;
		pieces[5] = this.folder;
		pieces[6] = Integer.toString(this.pages);
		return String.join(",", pieces);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getVolume() {
		return volume;
	}

	public void setVolume(String volume) {
		this.volume = volume;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public boolean isFirstPage() {
		return firstPage;
	}

	public void setFirstPage(boolean firstPage) {
		this.firstPage = firstPage;
	}

	public String getBox() {
		return box;
	}

	public void setBox(String box) {
		this.box = box;
	}

	public String getFolder() {
		return folder;
	}

	public void setFolder(String folder) {
		this.folder = folder;
	}

	public int getPages() {
		return pages;
	}

	public void setPages(int pages) {
		this.pages = pages;
	}
	
	
}
