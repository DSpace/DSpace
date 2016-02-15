/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
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
