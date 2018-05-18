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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.dspace.app.cris.integration.statistics.StatComponentsService;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.statistics.bean.TreeKeyMap;
import org.dspace.app.cris.statistics.bean.TwoKeyMap;
import org.dspace.app.webui.cris.components.BeanFacetComponent;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.SolrLogger;

public class StatSelectedObjectComponent<T extends DSpaceObject> extends
        StatsComponent<T>
{

    protected static String STATS_QUERY = "id:{0}";

    @Override
    public TreeKeyMap query(String id, HttpSolrServer solrServer, Date startDate, Date endDate)
            throws Exception
    {
        statisticDatasBeans = new TreeKeyMap();
        if (id != null && !id.equals("")
                && StatComponentsService.getYearsQuery() != null)
        {
            solrServer.setMaxRetries(0);
            SolrQuery solrQuery = new SolrQuery();

            _prepareBasicQuery(solrQuery, StatComponentsService.getYearsQuery(),startDate,endDate);

            if (StatComponentsService.isExcludeBot())
            {
                solrQuery.addFilterQuery("-isBot:true");
            }

            Integer relationType = getRelationObjectType();
            if (relationType == null)
            {
                relationType = CrisConstants
                        .getEntityType(getRelationObjectClass().newInstance());
            }
            solrQuery.addFilterQuery("type:" + relationType);

            String query = STATS_QUERY;
            if (getBean() != null)
            {
                String beanQuery = getBean().getQuery();
                query += (beanQuery != null && !beanQuery.isEmpty()) ? beanQuery
                        : "";
                for (String filter : getBean().getFilters())
                {
                    solrQuery.addFilterQuery(filter);
                }
            }

            solrQuery.setQuery(MessageFormat.format(query, id));
            if (getBean() instanceof BeanFacetComponent)
            {
                BeanFacetComponent beanFacet = (BeanFacetComponent) getBean();
                solrQuery.setFacet(true);
                solrQuery.addFacetQuery(beanFacet.getFacetQuery());
                solrQuery.addFacetField(beanFacet.getFacetField());
            }
            solrResponse = solrServer.query(solrQuery);

            buildPageResultModules(StatComponentsService._SELECTED_OBJECT);
        }
        else
        {
            throw new Exception("Item Id not valid");
        }

        return statisticDatasBeans;
    }

    @Override
    public TwoKeyMap getLabels(Context context, String type)
            throws SQLException
    {
        // none
        return new TwoKeyMap();
    }

    @Override
    public Map<String, ObjectCount[]> queryFacetDate(SolrLogger statsLogger,
            DSpaceObject object, String dateType, String dateStart,
            String dateEnd, int gap, Context context) throws SolrServerException
    {
        Map<String, ObjectCount[]> map = new HashMap<String, ObjectCount[]>();
        
        map.put(getMode(), statsLogger.queryFacetDate("search.uniqueid:" + object.getType()
                + "-" + object.getID() + " AND time:[* TO NOW/" + dateType
                + dateEnd + dateType + "]", null, 0, dateType, dateStart,
                dateEnd, gap, true, context));
        return map;
    }
    
    @Override
    public String getMode()
    {      
        return VIEW;
    }
}
