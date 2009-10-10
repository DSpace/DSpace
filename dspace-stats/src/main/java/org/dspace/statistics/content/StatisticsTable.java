/**
 * $Id$
 * $URL$
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace Foundation License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
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
