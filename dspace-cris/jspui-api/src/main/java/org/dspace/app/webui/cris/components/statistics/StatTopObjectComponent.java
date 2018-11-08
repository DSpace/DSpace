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

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.dspace.app.cris.integration.statistics.StatComponentsService;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.statistics.bean.PieStatisticBean;
import org.dspace.app.cris.statistics.bean.StatisticDatasBeanRow;
import org.dspace.app.cris.statistics.bean.TreeKeyMap;
import org.dspace.app.cris.statistics.bean.TwoKeyMap;
import org.dspace.app.webui.cris.components.BeanFacetComponent;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.SolrLogger;

public class StatTopObjectComponent<T extends DSpaceObject> extends
        StatsComponent<T>
{

	private static final String QUERY_COMMON = "'''{'''!join from={0} to=search.uniqueid fromIndex={2}'''}'''{1} AND -withdrawn:true";
	private static final String QUERY_GLOBAL = "'''{'''!join from={0} to=search.uniqueid fromIndex={1}'''}'''-withdrawn:true";

    private String fromField;

    @Override
    public TreeKeyMap query(String id, HttpSolrServer solrServer,Date startDate, Date endDate) throws Exception
    {
        statisticDatasBeans = new TreeKeyMap();
        if (id != null && !id.equals("") && StatComponentsService.getYearsQuery() != null)
        {

            // HttpSolrServer solrServer = new HttpSolrServer(
            // solrConfig.getUrl()+solrCore);
            solrServer.setMaxRetries(0);
            SolrQuery solrQuery = new SolrQuery();
            // http://localhost:8983/solr/statistics/select/?q=type%3A2&rows=20&facet=true&facet.date=time&facet.date.start=2008-07-00T00:00:00.000Z&facet.date.end=2009-06-31T00:00:00.000Z&facet.date.gap=%2B1MONTHS&facet.field=id
            _prepareBasicQuery(solrQuery, StatComponentsService.getYearsQuery(),startDate,endDate);
            // _prepareTopQuery(type, id, fieldName, solrQuery);

            if(StatComponentsService.isExcludeBot()) {
                solrQuery.addFilterQuery("-isBot:true");    
            }

            Integer relationType = getRelationObjectType();
            if(relationType==null) {
                relationType = CrisConstants.getEntityType(getRelationObjectClass().newInstance());
            }
            solrQuery.addFilterQuery("type:"+ relationType);      
            for(String filter : getBean().getFilters()) {
                solrQuery.addFilterQuery(filter);
            }
            String query="";
            if(!StringUtils.equals(id,"0") ){
            	query = MessageFormat.format(QUERY_COMMON, getFromField(), getBean().getQuery(), getSearchCore());
            }else{
            	query = MessageFormat.format(QUERY_GLOBAL, getFromField(), getSearchCore());
            }
            String sID = getObjectId(id);
            query = MessageFormat.format(query, sID);
            solrQuery.setQuery(query);
            if (getBean() instanceof BeanFacetComponent)
            {
                BeanFacetComponent beanFacet = (BeanFacetComponent) getBean();
                solrQuery.setFacet(true);
                solrQuery.addFacetQuery(beanFacet.getFacetQuery());
                solrQuery.addFacetField(beanFacet.getFacetField());
            }
            solrResponse = solrServer.query(solrQuery);            
            if(!getBean().getSubQueries().isEmpty()) {                
                statisticDatasBeans.addValue(TOP, CrisConstants.getEntityTypeText(relationType), CATEGORY,
                    generateCategoryView(solrServer, TOP, relationType.toString(), CATEGORY, StatComponentsService.getTopCityLength(), query, getBean().getSubQueries(), sID, solrQuery.getFilterQueries()));
            }
            buildTopResultModules(relationType);

        }
        else
        {
            throw new Exception("Object Id not valid");
        }
        return statisticDatasBeans;
    }

    protected String getObjectId(String id)
    {
        return id;
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
                    DSpaceObject item = DSpaceObject.find(context, getRelationObjectType(), Integer.parseInt(row.getLabel()));
//                    if (item != null)
                    {
                        labels.addValue(type, row.getLabel(), item);
                    }
                }
            }
        }

     
        return labels;
    }

    @Override
    protected void _prepareBasicQuery(SolrQuery solrQuery, Integer yearsQuery,Date startDate, Date endDate)
    {
        _addBasicConfiguration(solrQuery, yearsQuery, startDate, endDate);
        solrQuery.addFacetField(_CONTINENT, _COUNTRY_CODE, _CITY, ID, _LOCATION,
                _FISCALYEAR, _SOLARYEAR);
        solrQuery.set("facet.missing", true);
        solrQuery.set("f." + _LOCATION + ".facet.missing", true);
        solrQuery.set("f." + _CONTINENT + ".facet.missing", true);
        solrQuery.set("f." + _COUNTRY_CODE + ".facet.missing", true);
        solrQuery.set("f." + _CITY + ".facet.missing", true);
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
    
    public void setFromField(String fromField)
    {
        this.fromField = fromField;
    }

    public String getFromField()
    {
        return fromField;
    }

    @Override
    public Map<String, ObjectCount[]> queryFacetDate(SolrLogger statsLogger, DSpaceObject object,
            String dateType, String dateStart, String dateEnd, int gap, Context context) throws SolrServerException
    {
        String query = MessageFormat.format(QUERY_COMMON, getFromField(), getBean().getQuery(), getSearchCore());
        String sID = getObjectId(""+object.getID());
        query = MessageFormat.format(query, sID);
        Map<String, ObjectCount[]> map = new HashMap<String, ObjectCount[]>();
        
        map.put(getMode(), statsLogger.queryFacetDate(query, "time:[* TO NOW/" + dateType + dateEnd + dateType + "]", 0, dateType, dateStart,
                dateEnd, gap, true, context));
        return map;


    }
    
    private String getSearchCore() {
		return ConfigurationManager.getProperty("solr-statistics",
				"solr.join.core");
	}

	@Override
    public String getMode()
    {      
        return VIEW;
    }

}
