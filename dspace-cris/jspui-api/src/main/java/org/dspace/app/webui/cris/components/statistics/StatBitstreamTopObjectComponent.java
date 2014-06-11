/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.components.statistics;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.cris.discovery.CrisSearchService;
import org.dspace.app.cris.statistics.CrisSolrLogger;
import org.dspace.app.cris.statistics.bean.PieStatisticBean;
import org.dspace.app.cris.statistics.bean.StatisticDatasBeanRow;
import org.dspace.app.cris.statistics.bean.TwoKeyMap;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SolrServiceImpl;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.SolrLogger;

public class StatBitstreamTopObjectComponent extends StatTopObjectComponent
{
    private static final String CUSTOM_FROMFIELD = "ORIGINAL_mvuntokenized";

    private static final Logger log = Logger
            .getLogger(StatBitstreamTopObjectComponent.class);

    private CrisSearchService crisSearchService;
  
    public StatBitstreamTopObjectComponent()
    {
        setRelationObjectType(Constants.BITSTREAM);     
    }

    @Override
    public TwoKeyMap getLabels(Context context, String type) throws SQLException
    {

        TwoKeyMap labels = new TwoKeyMap();

        PieStatisticBean myvalue = (PieStatisticBean) statisticDatasBeans
                .get("top").get(type).get("id");
        if (myvalue != null)
        {
            if (myvalue.getLimitedDataTable() != null)
            {
                for (StatisticDatasBeanRow row : myvalue.getLimitedDataTable())
                {
                    Bitstream bitstream = Bitstream.find(context,
                            Integer.parseInt(row.getLabel()));

                    SolrQuery solrQuery = new SolrQuery();
                    solrQuery.setQuery(getFromField() +":"+ Constants.BITSTREAM + "-"+ bitstream.getID());
                    try
                    {
                        QueryResponse solrQueryResponse = crisSearchService.search(solrQuery);
                        for (SolrDocument doc : solrQueryResponse.getResults())
                        {
                            labels.addValue(row.getLabel(), "handle", doc.getFieldValue("handle"));
                        }
                    }
                    catch (SearchServiceException e)
                    {
                        log.error(e.getMessage(), e);
                    }

                    if (bitstream != null)
                    {
                        labels.addValue(type, row.getLabel(), bitstream);
                    }
                }
            }
        }
        return labels;

    }

    public void setCrisSearchService(CrisSearchService crisDiscoveryService)
    {
        this.crisSearchService = crisDiscoveryService;
    }

    @Override
    public String getFromField()
    {
       if(super.getFromField()==null) {
           return CUSTOM_FROMFIELD;
       }
       return super.getFromField();
    }
 
    @Override
    public String getMode()
    {      
        return DOWNLOAD;
    }
}
