/**
 * $Id: StatisticsBSAdapter.java 4405 2009-10-07 08:35:32Z mdiggory $
 * $URL: http://scm.dspace.org/svn/repo/dspace/tags/dspace-1.6.2/dspace-stats/src/main/java/org/dspace/statistics/content/StatisticsBSAdapter.java $
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace Foundation License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.statistics.content;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.statistics.SolrLogger;
import org.dspace.statistics.content.filter.StatisticsFilter;

/**
 * User: @author kevinvandevelde at atmire.com (kevin at atmire.com)
 * Date: 20-mei-2009
 * Time: 16:44:29
 * Class that will hold the data needed to show
 * statistical data in the browse, search pages
 */

public class StatisticsBSAdapter {

    private boolean displayItemViews;
    private boolean displayBitstreamViews;
    private boolean displayTotalViews;
    private List<StatisticsFilter> filters;

    public static final int ITEM_VISITS = 0;
    public static final int BITSTREAM_VISITS = 1;
    public static final int TOTAL_VISITS = 2;

    public StatisticsBSAdapter() {
        displayItemViews = false;
        displayBitstreamViews = false;
        filters = new ArrayList<StatisticsFilter>();
    }

    /**
     * Returns the number of visits for the item,
     * depending on the visitype it can either be item, bitstream, total, ...
     * @param visitType the type of visits we want, from the item, bitstream, total
     * @param item the item from which we need our visits
     * @return the number of visits
     * @throws SolrServerException ....
     */
    public long getNumberOfVisits(int visitType, Item item) throws SolrServerException {
        switch (visitType){
            case ITEM_VISITS:
                return SolrLogger.queryTotal("type: " + Constants.ITEM + " AND id: " + item.getID(), resolveFilterQueries()).getCount();
            case BITSTREAM_VISITS:
                return SolrLogger.queryTotal("type: " + Constants.BITSTREAM + " AND owningItem: " + item.getID(), resolveFilterQueries()).getCount();
            case TOTAL_VISITS:
                return getNumberOfVisits(ITEM_VISITS, item) + getNumberOfVisits(BITSTREAM_VISITS, item);

        }
        return -1;
    }

    private String resolveFilterQueries(){
        String out = "";
        for (int i = 0; i < filters.size(); i++) {
            StatisticsFilter statisticsFilter = filters.get(i);
            out += statisticsFilter.toQuery();

            if(i != 0 && (i != filters.size() -1))
                out += " AND ";
        }
        return out;
    }

    ///////////////////////
    // GETTERS & SETTERS //
    ///////////////////////
    public boolean isDisplayTotalViews() {
        return displayTotalViews;
    }

    public void setDisplayTotalViews(boolean displayTotalViews) {
        this.displayTotalViews = displayTotalViews;
    }

    public boolean isDisplayItemViews() {
        return displayItemViews;
    }

    public void setDisplayItemViews(boolean displayItemViews) {
        this.displayItemViews = displayItemViews;
    }

    public boolean isDisplayBitstreamViews() {
        return displayBitstreamViews;
    }

    public void setDisplayBitstreamViews(boolean displayBitstreamViews) {
        this.displayBitstreamViews = displayBitstreamViews;
    }



    public List<StatisticsFilter> getFilters() {
        return filters;
    }

    public void addFilter(StatisticsFilter filter){
        this.filters.add(filter);
    }

    public void setFilters(List<StatisticsFilter> filters) {
        this.filters = filters;
    }

}
