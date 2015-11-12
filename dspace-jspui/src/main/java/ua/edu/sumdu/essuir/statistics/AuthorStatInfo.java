package ua.edu.sumdu.essuir.statistics;


public class AuthorStatInfo {

	public AuthorStatInfo(String name, long downloads) {
		this.name = name;
		this.downloads = downloads;
	}
	
	
	public String getName() {
		return name;
	}
	
	
	public long getDownloads() {
		return downloads;
	}
	
	
	private String name = null;
	private long downloads = -1;
}
