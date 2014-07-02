/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.common;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

//import org.apache.log4j.Logger;
import org.dspace.statistics.ObjectCount;

@XmlRootElement(name = "stats_data")
public class StatsData {
	private List<StatsEntry> downloads;
	private long total_downloads=0;
	private List<StatsEntry> views;
	private long total_views=0;
	
	/** log4j category */
//    private static final Logger log = Logger.getLogger(StatsData.class);
    
    public StatsData(){
    	views= new ArrayList<StatsEntry>();
    	downloads = new ArrayList<StatsEntry>();
    }
	
	public void setViews(ObjectCount[]data){
		for(ObjectCount count : data){
			if(count.getValue().compareTo("total")==0){
//				total_views+=count.getCount();
			} else {
				total_views+=count.getCount();
				views.add(new StatsEntry(count.getValue(), count.getCount()));
			}
		}
	}
	
	public void setDownloads(ObjectCount[]data){
		for(ObjectCount count : data){
			if(count.getValue().compareTo("total")==0){
//				total_downloads+= count.getCount();
			} else {
				total_downloads+=count.getCount();
				downloads.add(new StatsEntry(count.getValue(), count.getCount()));
			}
		}
	}

	public List<StatsEntry> getDownloads() {
		return downloads;
	}

	public void setDownloads(List<StatsEntry> downloads) {
		this.downloads.addAll(downloads);
		for(StatsEntry entry : downloads){
			this.total_downloads+=entry.getCount();
		}
	}

	public List<StatsEntry> getViews() {
		return views;
	}

	public void setViews(List<StatsEntry> views) {
		this.views.addAll(views);
		for(StatsEntry entry : views){
			this.total_views+=entry.getCount();
		}
	}
	
	public void addViews(ObjectCount[]data) {
		for(int i=0; i<data.length; i++){
			boolean found = false;
			ObjectCount entry = data[i];
			if(entry.getValue().compareTo("total")==0){
				continue;
			}
			this.total_views+=entry.getCount();
			for(StatsEntry stats : this.views){
				if(stats.getLabel().compareTo(entry.getValue())==0){
					stats.setCount(stats.getCount()+entry.getCount());
					found =true;
					break;
				}
			}
			if(found==false){
				this.views.add(new StatsEntry(entry.getValue(), entry.getCount()));
			}
		}
		
	}
	
	public void addDownloads(ObjectCount[]data) {
		for(int i=0; i<data.length; i++){
			boolean found = false;
			ObjectCount entry = data[i];
			if(entry.getValue().compareTo("total")==0){
				continue;
			}
			this.total_downloads+=entry.getCount();
			for(StatsEntry stats : this.downloads){
				if(stats.getLabel().compareTo(entry.getValue())==0){
					stats.setCount(stats.getCount()+entry.getCount());
					found =true;
					break;
				}
			}
			if(found==false){
				this.downloads.add(new StatsEntry(entry.getValue(), entry.getCount()));
			}
		}
		
	}
	
	protected long getTotalViews(){
		return total_views;
	}
	
	protected long getTotalDownloads(){
		return total_downloads;
	}

}
