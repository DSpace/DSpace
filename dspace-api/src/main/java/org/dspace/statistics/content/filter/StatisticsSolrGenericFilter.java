/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.content.filter;

import org.apache.commons.lang3.StringUtils;

public class StatisticsSolrGenericFilter implements StatisticsFilter {

	private String query;
	
	public String getQuery() {
		return query;
	}
	public void setQuery(String query) {
		this.query = query;
	}
	@Override
	public String toQuery() {
		if(StringUtils.isNotBlank(query)){
			return query;
		}
		return "*:*";
		
	}

}
