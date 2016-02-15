/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.components;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.metrics.common.model.ConstantMetrics;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.utils.DSpace;

public class TopItemManager
{

    Logger log = Logger.getLogger(TopItemManager.class);

    private int maxResults = 10;

    private String queryDefault = "*:*";

    private String sortCriteria;

    private String sortOrder = "DESC";

    private List<String> fq;

    private SearchService searchService;
    
    public List<MostViewedItem> getMostViewed(Context context)
            throws SolrServerException, SQLException, SearchServiceException
    {

        SolrQuery query = new SolrQuery();
        query.setQuery(queryDefault);

        if (sortOrder == null || "DESC".equals(sortOrder))
        {
            query.setSort(ConstantMetrics.PREFIX_FIELD + sortCriteria,
                    ORDER.desc);
        }
        else
        {
            query.setSort(ConstantMetrics.PREFIX_FIELD + sortCriteria,
                    ORDER.asc);
        }

        if (fq != null)
        {
            for (String f : fq)
            {
                query.addFilterQuery(f);
            }
        }
        query.addFilterQuery("{!field f=search.resourcetype}" + Constants.ITEM);
        query.setFields("search.resourceid", "search.resourcetype", "handle", ConstantMetrics.PREFIX_FIELD + sortCriteria);
        query.setRows(maxResults);
        
        QueryResponse response = searchService.search(query);
        SolrDocumentList results = response.getResults();
        List<MostViewedItem> citedList = new ArrayList<MostViewedItem>();
        for (SolrDocument doc : results)
        {
            Integer resourceId = (Integer) doc
                    .getFirstValue("search.resourceid");
            Double count = (Double) doc
                    .getFieldValue(ConstantMetrics.PREFIX_FIELD + sortCriteria);

            Item item = (Item) Item.find(context, resourceId);
            if (item != null)
            {

                if (!item.isWithdrawn())
                {
                    MostViewedItem mvi = new MostViewedItem();
                    mvi.setItem(item);
                    if(count!=null) {
                        mvi.setVisits("" + count);
                    }
                    citedList.add(mvi);
                }
            }
            else
            {
                log.warn("A DELETED ITEM IS IN SOLR INDEX? itemId:"
                        + resourceId);
            }
        }
        return citedList;

    }

    public int getMaxResults()
    {
        return maxResults;
    }

    public void setMaxResults(int maxResults)
    {
        this.maxResults = maxResults;
    }

    public String getQueryDefault()
    {
        return queryDefault;
    }

    public void setQueryDefault(String queryDefault)
    {
        this.queryDefault = queryDefault;
    }

    public String getSortCriteria()
    {
        return sortCriteria;
    }

    public void setSortCriteria(String sortCriteria)
    {
        this.sortCriteria = sortCriteria;
    }

    public String getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder)
    {
        this.sortOrder = sortOrder;
    }

    public List<String> getFq()
    {
        return fq;
    }

    public void setFq(List<String> fq)
    {
        this.fq = fq;
    }

    public SearchService getSearchService()
    {
        return searchService;
    }

    public void setSearchService(SearchService searchService)
    {
        this.searchService = searchService;
    }

}
