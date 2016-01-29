package org.dspace.app.webui.components;

import org.dspace.discovery.IGlobalSearchResult;

public class MostViewedItem {
	
	private IGlobalSearchResult item;
	private String bitstreamName;
	
	private String visits;
	
	
	public String getVisits() {
		return visits;
	}
	public void setVisits(String visits) {
		this.visits = visits;
	}
    public IGlobalSearchResult getItem()
    {
        return item;
    }
    public void setItem(IGlobalSearchResult item)
    {
        this.item = item;
    }
    public String getBitstreamName()
    {
        return bitstreamName;
    }
    public void setBitstreamName(String bitstreamName)
    {
        this.bitstreamName = bitstreamName;
    }

}
