package org.dspace.statistics.content.filter;

import org.apache.commons.lang3.StringUtils;

public class StatisticsSolrLocationFilter implements StatisticsFilter {
	
	private final String COMMUNITY_FILTER  ="owningComm";
	private final String COLLECTION_FILTER  ="owningColl";

	private String locType;
	private String locID;
	
	public String getLocType() {
		return locType;
	}
	public void setLocType(String locType) {
		this.locType = locType;
	}
	public String getLocID() {
		return locID;
	}
	public void setLocID(String locID) {
		this.locID = locID;
	}
	@Override
	public String toQuery() {
		String query="*:*";
		if(StringUtils.isNotBlank(locType)  && StringUtils.isNotBlank(locID)
				&& (StringUtils.equalsIgnoreCase(locType, COMMUNITY_FILTER) ||   StringUtils.equalsIgnoreCase(locType, COLLECTION_FILTER)) ){
					query=locType+":"+locID;
		}
		return query;
	}

}

