

package org.dspace.rest.common.search;

import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.rest.common.Bitstream;
import org.dspace.rest.common.Item;
import org.dspace.rest.common.Community;
import org.dspace.rest.common.Collection;

import javax.ws.rs.WebApplicationException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

 /** 
 * @author Hamed Yousefi Nasab
 */

@XmlRootElement(name = "result")
public class ResultSet {

	Logger log = Logger.getLogger(ResultSet.class);
	
	private long totalResults;
	private int searchTime;
	private Community[] communities;
	private Collection[] collections;
	private Item[] items;
	private Bitstream[] bitstreams;
	private FacetResult[] facet;
	
	public void setTotalResults(long totalResults){
		this.totalResults = totalResults;
	}
	
	public void setSearchTime(int searchTime){
		this.searchTime = searchTime;
	}
	
	public void setCommunities(Community[] communities){
		this.communities = communities;
	}
	
	public void setCollections(Collection[] collections){
		this.collections = collections;
	}
	
	public void setItems(Item[] items){
		this.items = items;
	}

	public void setBitstreams(Bitstream[] bitstreams){
		this.bitstreams = bitstreams;
	}
	
	public void setFacet(FacetResult[] facet){
		this.facet = facet;
	}
	
	public long getTotalResults(){
		return totalResults;
	}
	
	public int getSearchTime(){
		return searchTime;
	}
	
	public Community[] getCommunities(){
		return communities;
	}
	
	public Collection[] getCollections(){
		return collections;
	}
	
	public Item[] getItems(){
		return items;
	}

	public Bitstream[] getBitstreams(){
		return bitstreams;
	}

	public FacetResult[] getFacet(){
		return facet;
	}
	
}