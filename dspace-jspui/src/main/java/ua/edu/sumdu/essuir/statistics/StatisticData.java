package ua.edu.sumdu.essuir.statistics;


public class StatisticData {

	public StatisticData() {
	}
	
	
	public long getTotalCount() {
		return totalCount;
	}
	
	
	public String getLastUpdate() {
		return lastUpdate;
	}
	
	
	public long getTotalViews() {
		return totalViews;
	}
	
	
	public long getTotalDownloads() {
		return totalDownloads;
	}
	
	
	public void setTotalCount(long totalCount) {
		this.totalCount = totalCount;
	}
	
	
	public void setLastUpdate(String lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
	
	
	public void setTotalViews(long totalViews)  {
		this.totalViews = totalViews;
	}
	
	
	public void setTotalDownloads(long totalDownloads) {
		this.totalDownloads = totalDownloads;
	}
	
	
	private long totalCount = 0;
	private String lastUpdate = "";
	private long totalViews = 0;
	private long totalDownloads = 0;
}
