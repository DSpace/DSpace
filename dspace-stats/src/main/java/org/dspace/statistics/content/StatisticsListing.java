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
