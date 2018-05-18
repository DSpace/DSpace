/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.components.statistics;

import it.cilea.osd.jdyna.model.PropertiesDefinition;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.statistics.bean.PieStatisticBean;
import org.dspace.app.cris.statistics.bean.StatisticDatasBeanRow;
import org.dspace.app.cris.statistics.bean.TwoKeyMap;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.SolrLogger;

public abstract class StatCrisDownloadSelectedObjectComponent extends
        StatSelectedObjectComponent
{
    
    private ApplicationService applicationService;
    
    @Override
    public TwoKeyMap getLabels(Context context, String type)
            throws SQLException
    {

        TwoKeyMap labels = new TwoKeyMap();

        PieStatisticBean myvalue = (PieStatisticBean) statisticDatasBeans
                .get("selectedObject").get("geo").get("sectionid");
        if (myvalue != null)
        {
            if (myvalue.getLimitedDataTable() != null)
            {
                for (StatisticDatasBeanRow row : myvalue.getLimitedDataTable())
                {
                    String pkey = (String)row.getLabel();
                    
                    PropertiesDefinition def = innerCall(Integer.parseInt(pkey));
                           
                    labels.addValue(row.getLabel(), "label", def.getLabel()!=null?def.getLabel():def.getShortName());
                    
                    if (def != null)
                    {
                        labels.addValue("geo", row.getLabel(), def);
                    }
                }
            }
        }
        return labels;
    }

    protected abstract PropertiesDefinition innerCall(Integer pkey);
    
    
    @Override
    protected void _prepareBasicQuery(SolrQuery solrQuery, Integer yearsQuery,Date startDate, Date endDate)
    {
        _addBasicConfiguration(solrQuery, yearsQuery, startDate, endDate);
        solrQuery.addFacetField(_CONTINENT, _COUNTRY_CODE, _CITY, ID,
                _LOCATION, _FISCALYEAR, _SOLARYEAR);
        solrQuery.set("facet.missing", true);
        solrQuery.set("f." + _LOCATION + ".facet.missing", false);
        solrQuery.set("f." + ID + ".facet.missing", false);
        solrQuery.set("f." + _FISCALYEAR + ".facet.missing", false);
        solrQuery.set("f." + _SOLARYEAR + ".facet.missing", false);
        solrQuery.set("f." + _FISCALYEAR + ".facet.sort", false);
        solrQuery.set("f." + _SOLARYEAR + ".facet.sort", false);

        solrQuery.set("f." + FILE + ".facet.missing", false);
        solrQuery.set("f." + FILE + ".facet.mincount", 1);
        solrQuery.set("f." + ID + ".facet.mincount", 1);
        solrQuery.set("f." + _CONTINENT + ".facet.mincount", 1);
        solrQuery.set("f." + _COUNTRY_CODE + ".facet.mincount", 1);
        solrQuery.set("f." + _CITY + ".facet.mincount", 1);
        solrQuery.set("f." + _LOCATION + ".facet.mincount", 1);
        solrQuery.set("f." + _FISCALYEAR + ".facet.mincount", 1);
        solrQuery.set("f." + _SOLARYEAR + ".facet.mincount", 1);
        solrQuery.set("f." + _CONTINENT + ".facet.mincount", 1);
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

    public ApplicationService getApplicationService()
    {
        return applicationService;
    }
    
    @Override
    public Map<String, ObjectCount[]> queryFacetDate(SolrLogger statsLogger,
            DSpaceObject object, String dateType, String dateStart,
            String dateEnd, int gap, Context context) throws SolrServerException
    {
        Map<String, ObjectCount[]> map = new HashMap<String, ObjectCount[]>();
        
        map.put(getMode(), statsLogger.queryFacetDate("search.uniqueid:" + object.getType()
                + "-" + object.getID() + " AND sectionid:[* TO *] AND time:[* TO NOW/" + dateType
                + dateEnd + dateType + "]", null, 0, dateType, dateStart,
                dateEnd, gap, true, context));
        return map;
    }
    
    @Override
    public String getMode()
    {     
        return DOWNLOAD;
    }
}
