/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.content;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.statistics.content.filter.StatisticsFilter;
import org.dspace.statistics.factory.StatisticsServiceFactory;
import org.dspace.statistics.service.SolrLoggerService;

/**
 * Class that will hold the data needed to show
 * statistics in the browse and search pages.
 * 
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */

public class StatisticsBSAdapter {

    protected boolean displayItemViews;
    protected boolean displayBitstreamViews;
    protected boolean displayTotalViews;
    protected List<StatisticsFilter> filters;

    /** visitType is ITEM */
    public static final int ITEM_VISITS = 0;
    /** visitType is BITSTREAM */
    public static final int BITSTREAM_VISITS = 1;
    /** visitType is TOTAL */
    public static final int TOTAL_VISITS = 2;
    protected final SolrLoggerService solrLoggerService;

    public StatisticsBSAdapter() {
        displayItemViews = false;
        displayBitstreamViews = false;
        filters = new ArrayList<StatisticsFilter>();
        solrLoggerService = StatisticsServiceFactory.getInstance().getSolrLoggerService();
    }

    /**
     * Returns the number of visits for the item.
     * Depending on the visitType it can either be item, bitstream, total, ...
     * 
     * @param visitType the type of visits we want, from the item, bitstream, total
     * @param item the item from which we need our visits
     * @return the number of visits
     * @throws SolrServerException ....
     */
    public long getNumberOfVisits(int visitType, Item item) throws SolrServerException {
        switch (visitType){
            case ITEM_VISITS:
                return solrLoggerService.queryTotal("type: " + Constants.ITEM + " AND id: " + item.getID(), resolveFilterQueries()).getCount();
            case BITSTREAM_VISITS:
                return solrLoggerService.queryTotal("type: " + Constants.BITSTREAM + " AND owningItem: " + item.getID(), resolveFilterQueries()).getCount();
            case TOTAL_VISITS:
                return getNumberOfVisits(ITEM_VISITS, item) + getNumberOfVisits(BITSTREAM_VISITS, item);

        }
        return -1;
    }

    private String resolveFilterQueries(){
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < filters.size(); i++) {
            StatisticsFilter statisticsFilter = filters.get(i);
            out.append(statisticsFilter.toQuery());

            if(i != 0 && (i != filters.size() -1))
            {
                out.append(" AND ");
            }
        }
        return out.toString();
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
