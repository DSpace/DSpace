/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.components;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.SolrLogger;
import org.dspace.utils.DSpace;

public class MostViewedItemManager
{

    Logger log = Logger.getLogger(MostViewedItemManager.class);

    private final String TYPE = "2";

    private final String STATISTICS_TYPE = SolrLogger.StatisticsType.VIEW
            .text();

    private DSpaceObject owningDso;

    private int maxResults;

    private String time_period;

    DSpace dspace = new DSpace();

    SolrLogger solrLogger = dspace.getServiceManager()
            .getServiceByName(SolrLogger.class.getName(), SolrLogger.class);

    public MostViewedItemManager(DSpaceObject dso, int max, String period)
    {
        owningDso = dso;
        maxResults = max;
        // NOW - 1MONTH
        time_period = period;
    }

    public MostViewedItemManager(DSpaceObject dso)
    {
        owningDso = dso;
        maxResults = 10;
        time_period = "*";

    }

    public List<MostViewedItem> getMostViewed(Context context)
            throws SolrServerException, SQLException
    {
        String query = "statistics_type:" + STATISTICS_TYPE;
        String filterQuery = "type:" + TYPE;

        if (owningDso != null)
        {
            filterQuery += " AND ";
            if (owningDso instanceof Collection)
            {
                filterQuery += "owningColl:";
            }
            else if (owningDso instanceof Community)
            {
                filterQuery += "owningComm:";
            }
            filterQuery += owningDso.getID();
        }

        if (StringUtils.isNotBlank(time_period))
        {
            filterQuery += " AND time:[" + time_period + " TO *]";
        }

        ObjectCount[] oc = solrLogger.queryFacetField(query, filterQuery, "id",
                maxResults, false, null);

        List<MostViewedItem> viewedList = new ArrayList<MostViewedItem>();
        for (int x = 0; x < oc.length; x++)
        {
            int id = Integer.parseInt(oc[x].getValue());
            Item item = Item.find(context, id);
            if (item != null)
            {
                if (!item.isWithdrawn())
                {
                    MostViewedItem mvi = new MostViewedItem();
                    mvi.setItem(item);
                    mvi.setVisits("" + oc[x].getCount());
                    viewedList.add(mvi);
                }

            }
            else
            {
                log.warn("A DELETED ITEM IS IN STATISTICS? itemId:" + id);
            }
        }
        return viewedList;

    }

    public DSpaceObject getOwningDso()
    {
        return owningDso;
    }

    public void setOwningDso(DSpaceObject owningDso)
    {
        this.owningDso = owningDso;
    }

    public int getMaxResults()
    {
        return maxResults;
    }

    public void setMaxResults(int maxResults)
    {
        this.maxResults = maxResults;
    }

    public String getTime_period()
    {
        return time_period;
    }

    public void setTime_period(String time_period)
    {
        this.time_period = time_period;
    }

}
