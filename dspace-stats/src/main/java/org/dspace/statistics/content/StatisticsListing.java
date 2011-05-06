/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.content;


/**
 * Encapsulates all data to render the statistics as a list
 * 
 * @author kevinvandevelde at atmire.com
 * Date: 23-dec-2008
 * Time: 12:38:58
 * 
 */
public class StatisticsListing extends StatisticsDisplay {

    public StatisticsListing(StatisticsData statisticsData){
        super(statisticsData);
    }

    @Override
	public String getType() {
		return "listing";
	}
}
