/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.content;


/**
 * Encapsulates all data to render the statistics as a table
 *
 * @author kevinvandevelde at atmire.com
 * Date: 23-dec-2008
 * Time: 9:27:52
 * 
 */
public class StatisticsTable extends StatisticsDisplay{

    public StatisticsTable(StatisticsData statisticsData){
        super(statisticsData);
    }
 
    @Override
	public String getType() {
		return "table";
	}
}
