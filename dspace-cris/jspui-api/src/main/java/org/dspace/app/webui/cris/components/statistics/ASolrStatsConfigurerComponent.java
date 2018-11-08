/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.components.statistics;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.dspace.app.cris.integration.statistics.StatComponentsService;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.statistics.bean.BarrChartStatisticDatasBean;
import org.dspace.app.cris.statistics.bean.MapDataBean;
import org.dspace.app.cris.statistics.bean.MapPointBean;
import org.dspace.app.cris.statistics.bean.PieStatisticBean;
import org.dspace.app.cris.statistics.bean.StatisticDatasBeanRow;
import org.dspace.app.cris.statistics.bean.TreeKeyMap;
import org.dspace.content.DSpaceObject;

public abstract class ASolrStatsConfigurerComponent<T extends DSpaceObject>
{

    /** log4j logger */
    private static Logger log = Logger
            .getLogger(ASolrStatsConfigurerComponent.class);

    public static final String _OTHER = "other";

    public static final String FILE = "sectionid";

    protected static final String ID = "id";

    protected static final String TIME_VIEW = "time";

    protected static final String GEO_VIEW = "geo";

    public static final String _TOTAL = "total";

    public static final String _LAST_MONTH = "lastMonth";

    public static final String _LAST_YEAR = "lastYear";

    public static final String _FISCAL_YEAR_MONTHS = "fiscalYearMonths";

    public static final String _ALL_MONTHS = "allMonths";

    public static final String _CONTINENT = "continent";

    public static final String _COUNTRY_CODE = "countryCode";

    public static final String _CITY = "city";

    public static final String _LENGTH = "length";

    public static final String _NotAvailable = "NA";

    protected static final String _LOCATION = "location";

    protected static final String _FISCALYEAR = "fiscalYear";

    protected static final String _SOLARYEAR = "solarYear";

    private static final String DELIM_LATITUDE_LONGITUDE = ",";

    protected static final String TOP = "top";

    protected static final String CATEGORY = "category";

    public static final String _FACET_DATE_FIELD_SUFFIX = "T00:00:00.001Z";

    protected TreeKeyMap statisticDatasBeans = new TreeKeyMap();

    public QueryResponse solrResponse;
    
    private Integer relationObjectType;

    protected BarrChartStatisticDatasBean generateTotalView(String key1,
            String key2, String key3)
    {
        BarrChartStatisticDatasBean totalResolutBean = new BarrChartStatisticDatasBean(
                key1, key2, key3);
        totalResolutBean.setName(key2);
        totalResolutBean.setHits(1);
        SolrDocumentList sdl = (SolrDocumentList) solrResponse.getResponse()
                .get("response");
        totalResolutBean.setDataTable(new String[][] { new String[] { Long
                .toString(sdl.getNumFound()) } });
        return totalResolutBean;
    }

    public MapDataBean generateMapView(String key1, String key2, String key3,
            Integer hitsNumber)
    {
        MapDataBean mapDataBean = new MapDataBean(key1, key2, key3);
        mapDataBean.setName(key2);
        Collection<MapPointBean> fullData = new ArrayList<MapPointBean>();

        try
        {
            NamedList result = (NamedList) ((SimpleOrderedMap) ((SimpleOrderedMap) solrResponse
                    .getResponse().get("facet_counts")).get("facet_fields"))
                    .get(key3);
            int limit = result.size();
            if (key3.equals(_CONTINENT) && hitsNumber != null && hitsNumber > 0)
            {
                limit = hitsNumber;
            }
            else if (key3.equals(_COUNTRY_CODE) && hitsNumber != null
                    && hitsNumber > 0)
            {
                limit = hitsNumber;
            }
            if (key3.equals(_CITY) && hitsNumber != null && hitsNumber > 0)
            {
                limit = hitsNumber;
            }

            Integer other = 0;

            for (int i = 0; i < result.size(); i++)
            {
                try
                {
                    if (result.getVal(i).getClass().equals(Integer.class))
                    {
                        StringTokenizer st = new StringTokenizer(
                                (result.getName(i)).toString(),
                                DELIM_LATITUDE_LONGITUDE);
                        MapPointBean mapPointBean = new MapPointBean(
                                st.nextToken(), st.nextToken(),
                                (Integer) result.getVal(i));
                        fullData.add(mapPointBean);
                    }

                }
                catch (Exception e)
                {
                    fullData.add(new MapPointBean(_NotAvailable, _NotAvailable,
                            null));
                    e.printStackTrace();
                }
            }
            mapDataBean.setHits(result.size());
            mapDataBean.setDataTable(fullData);
            try
            {
                mapDataBean.setPercentages();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        catch (Exception e)
        {
            log.warn(e.getMessage());
            fullData.add(new MapPointBean(_NotAvailable, _NotAvailable, null));
        }
        return mapDataBean;
    }

    public PieStatisticBean generateFacetFieldView(String key1, String key2,
            String key3, Integer hitsNumber)
    {
        PieStatisticBean pieStatisticBean = new PieStatisticBean(key1, key2,
                key3);
        pieStatisticBean.setName(key2);
        Collection<StatisticDatasBeanRow> fullData = new ArrayList<StatisticDatasBeanRow>();
        Collection<StatisticDatasBeanRow> limitedData = new ArrayList<StatisticDatasBeanRow>();
        try
        {
            NamedList result = (NamedList) ((SimpleOrderedMap) ((SimpleOrderedMap) solrResponse
                    .getResponse().get("facet_counts")).get("facet_fields"))
                    .get(key3);
            int limit = result.size();
            if (key3.equals(_CONTINENT) && hitsNumber != null && hitsNumber > 0)
            {
                limit = hitsNumber;
            }
            else if (key3.equals(_COUNTRY_CODE) && hitsNumber != null
                    && hitsNumber > 0)
            {
                limit = hitsNumber;
            }
            if (key3.equals(_CITY) && hitsNumber != null && hitsNumber > 0)
            {
                limit = hitsNumber;
            }

            Integer other = 0;

            for (int i = 0; i < result.size(); i++)
            {
                try
                {
                    if (result.getVal(i).getClass().equals(Integer.class))
                    {
                        String name = (String) result.getName(i);
                        if (StringUtils.isBlank(name))
                            name = "Unknown";
                        // if (i<limit && (Integer)result.getVal(i)>0){
                        if (i < limit && (Integer)result.getVal(i)>0)
                        {
                            limitedData.add(new StatisticDatasBeanRow(name,
                                    result.getVal(i)));
                        }
                        else
                        {
                            other += (Integer) result.getVal(i);
                        }
                        fullData.add(new StatisticDatasBeanRow(name, result
                                .getVal(i)));
                    }

                }
                catch (Exception e)
                {
                    fullData.add(new StatisticDatasBeanRow(_NotAvailable, null));
                    limitedData.add(new StatisticDatasBeanRow(_NotAvailable,
                            null));
                    e.printStackTrace();
                }
            }
            if (result.size() > limit && other > 0)
            {
                limitedData.add(new StatisticDatasBeanRow(_OTHER, other));
            }
            pieStatisticBean.setHits(result.size());
            pieStatisticBean.setDataTable(fullData);
            pieStatisticBean.setLimitedDataTable(limitedData);
            try
            {
                pieStatisticBean.setPercentages();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        catch (Exception e)
        {
            log.warn(e.getMessage());
            fullData.add(new StatisticDatasBeanRow(_NotAvailable, null));
            limitedData.add(new StatisticDatasBeanRow(_NotAvailable, null));
        }
        return pieStatisticBean;
    }

    protected BarrChartStatisticDatasBean generateAllMonthsVeiw(String key1,
            String key2, String key3)
    {
        BarrChartStatisticDatasBean totalResolutBean = new BarrChartStatisticDatasBean(
                key1, key2, key3);
        totalResolutBean.setName(key2);
        totalResolutBean.setHits(1);
        try
        {
            FacetField times = solrResponse.getFacetDate("time");
            String[][] data = new String[times.getValues().size()][2];
            for (int i = 0; i < times.getValues().size(); i++)
            {
                data[i][0] = times.getValues().get(i).getName();
                data[i][1] = String
                        .valueOf(times.getValues().get(i).getCount());
            }
            totalResolutBean.setDataTable(data);
        }
        catch (Exception e)
        {
            log.warn(e.getMessage());
            totalResolutBean
                    .setDataTable(new String[][] { new String[] { _NotAvailable } });
        }
        return totalResolutBean;
    }
    
    protected BarrChartStatisticDatasBean generateAllMonthsUpload(String key1,
            String key2, String key3)
    {
        BarrChartStatisticDatasBean totalResolutBean = new BarrChartStatisticDatasBean(
                key1, key2, key3);
        totalResolutBean.setName(key2);
        totalResolutBean.setHits(1);
        try
        {
            FacetField times = solrResponse.getFacetDate("dc.date.accessioned_dt");
            String[][] data = new String[times.getValues().size()][2];
            for (int i = 0; i < times.getValues().size(); i++)
            {
                data[i][0] = times.getValues().get(i).getName();
                data[i][1] = String
                        .valueOf(times.getValues().get(i).getCount());
            }
            totalResolutBean.setDataTable(data);
        }
        catch (Exception e)
        {
            log.warn(e.getMessage());
            totalResolutBean
                    .setDataTable(new String[][] { new String[] { _NotAvailable } });
        }
        return totalResolutBean;
    }    

    protected void buildTopResultModules(Integer type)
    {
        //String key2 =  Constants.typeText[type].toLowerCase();
        String key2 = CrisConstants.getEntityTypeText(type);
        buildTopTimeBasedResult(key2);
        buildTopGeoBasedResult(key2);
        buildTopMapBasedResult(key2);
    }

    protected void buildTopMapBasedResult(String key2)
    {
        statisticDatasBeans.addValue(TOP, key2, _LOCATION,
                generateMapView(TOP, key2, _LOCATION, null));
    }

    protected void buildTopGeoBasedResult(String key2)
    {
        statisticDatasBeans.addValue(
                TOP,
                key2,
                _CONTINENT,
                generateFacetFieldView(TOP, key2, _CONTINENT,
                        StatComponentsService.getTopContinentLength()));
        statisticDatasBeans.addValue(
                TOP,
                key2,
                _COUNTRY_CODE,
                generateFacetFieldView(TOP, key2, _COUNTRY_CODE,
                        StatComponentsService.getTopCountryLength()));
        statisticDatasBeans.addValue(
                TOP,
                key2,
                _CITY,
                generateFacetFieldView(TOP, key2, _CITY,
                        StatComponentsService.getTopCityLength()));
        statisticDatasBeans.addValue(
                TOP,
                key2,
                ID,
                generateFacetFieldView(TOP, key2, ID,
                        StatComponentsService.getTopCityLength()));
        statisticDatasBeans.addValue(
                TOP,
                key2,
                FILE,
                generateFacetFieldView(TOP, key2, FILE,
                        StatComponentsService.getTopCityLength()));
    }

    protected void buildTopTimeBasedResult(String key2)
    {
        statisticDatasBeans.addValue(TOP, key2, _TOTAL,
                generateTotalView(TOP, key2, _TOTAL));
        statisticDatasBeans.addValue(TOP, key2, _LAST_MONTH,
                generateLastMonthView(TOP, key2));
        statisticDatasBeans.addValue(TOP, key2, _LAST_YEAR,
                generateLastFiscalYearView(TOP, key2));
        // statisticDatasBeans.addValue(TIME_VIEW, _FISCAL_YEAR_MONTHS,);
        statisticDatasBeans.addValue(TOP, key2, _ALL_MONTHS,
                generateAllMonthsVeiw(TOP, key2, _ALL_MONTHS));
    }
    
    protected void buildTopTimeUploadBasedResult(String key2)
    {
        statisticDatasBeans.addValue(TOP, key2, _TOTAL,
                generateTotalView(TOP, key2, _TOTAL));
        statisticDatasBeans.addValue(TOP, key2, _LAST_MONTH,
                generateLastMonthUpload(TOP, key2));
        //statisticDatasBeans.addValue(TOP, key2, _LAST_YEAR,
          //      generateLastFiscalYearView(TOP, key2));
        // statisticDatasBeans.addValue(TIME_VIEW, _FISCAL_YEAR_MONTHS,);
        statisticDatasBeans.addValue(TOP, key2, _ALL_MONTHS,
                generateAllMonthsUpload(TOP, key2, _ALL_MONTHS));
    }    

    private Object generateLastFiscalYearView(String key1, String key2)
    {
        BarrChartStatisticDatasBean totalResolutBean = new BarrChartStatisticDatasBean(
                key1, key2, _FISCALYEAR);
        totalResolutBean.setName(key2);
        totalResolutBean.setHits(1);
        List<Count> counts = solrResponse.getFacetField(_FISCALYEAR)
                .getValues();
        if (counts != null && counts.size() > 0)
            totalResolutBean
                    .setDataTable(new String[][] { new String[] { String
                            .valueOf(counts.get(counts.size() - 1).getCount()) } });
        else
            totalResolutBean
                    .setDataTable(new String[][] { new String[] { _NotAvailable } });
        return totalResolutBean;
    }

    private Object generateLastMonthView(String key1, String key2)
    {
        BarrChartStatisticDatasBean totalResolutBean = new BarrChartStatisticDatasBean(
                key1, key2, _LAST_MONTH);
        totalResolutBean.setName(key2);
        totalResolutBean.setHits(1);
        List<Count> counts = solrResponse.getFacetDate("time").getValues();
        if (counts != null && counts.size() > 0)
            totalResolutBean
                    .setDataTable(new String[][] { new String[] { String
                            .valueOf(counts.get(counts.size() - 1).getCount()) } });
        else
            totalResolutBean
                    .setDataTable(new String[][] { new String[] { _NotAvailable } });

        return totalResolutBean;
    }
    
    private Object generateLastMonthUpload(String key1, String key2)
    {
        BarrChartStatisticDatasBean totalResolutBean = new BarrChartStatisticDatasBean(
                key1, key2, _LAST_MONTH);
        totalResolutBean.setName(key2);
        totalResolutBean.setHits(1);
        List<Count> counts = solrResponse.getFacetDate("dc.date.accessioned_dt").getValues();
        if (counts != null && counts.size() > 0)
            totalResolutBean
                    .setDataTable(new String[][] { new String[] { String
                            .valueOf(counts.get(counts.size() - 1).getCount()) } });
        else
            totalResolutBean
                    .setDataTable(new String[][] { new String[] { _NotAvailable } });

        return totalResolutBean;
    }    

    protected void buildPageResultModules(String key1)
    {
        buildSelectedObjectTimeBasedResults(key1);
        buildSelectedObjectGeoBasedResults(key1);
        buildSelectedObjectMapBasedResults(key1);
    }

    protected void buildSelectedObjectMapBasedResults(String key1)
    {
        statisticDatasBeans.addValue(key1, GEO_VIEW, _LOCATION,
                generateMapView(key1, GEO_VIEW, _LOCATION, null));
    }

    protected void buildSelectedObjectGeoBasedResults(String key1)
    {
        statisticDatasBeans.addValue(
                key1,
                GEO_VIEW,
                _CONTINENT,
                generateFacetFieldView(key1, GEO_VIEW, _CONTINENT,
                        StatComponentsService.getTopContinentLength()));
        statisticDatasBeans.addValue(
                key1,
                GEO_VIEW,
                _COUNTRY_CODE,
                generateFacetFieldView(key1, GEO_VIEW, _COUNTRY_CODE,
                        StatComponentsService.getTopCountryLength()));
        statisticDatasBeans.addValue(
                key1,
                GEO_VIEW,
                _CITY,
                generateFacetFieldView(key1, GEO_VIEW, _CITY,
                        StatComponentsService.getTopCityLength()));
        statisticDatasBeans.addValue(
                key1,
                GEO_VIEW,
                ID,
                generateFacetFieldView(key1, GEO_VIEW, ID,
                        StatComponentsService.getTopCityLength()));
        statisticDatasBeans.addValue(
                key1,
                GEO_VIEW,
                FILE,
                generateFacetFieldView(key1, GEO_VIEW, FILE,
                        StatComponentsService.getTopCityLength()));
    }

    protected void buildSelectedObjectTimeBasedResults(String key1)
    {
        statisticDatasBeans.addValue(key1, TIME_VIEW, _TOTAL,
                generateTotalView(key1, TIME_VIEW, _TOTAL));
        statisticDatasBeans.addValue(key1, TIME_VIEW, _LAST_MONTH,
                generateLastMonthView(key1, TIME_VIEW));
        statisticDatasBeans.addValue(key1, TIME_VIEW, _LAST_YEAR,
                generateLastFiscalYearView(key1, TIME_VIEW));
        statisticDatasBeans.addValue(key1, TIME_VIEW, _ALL_MONTHS,
                generateAllMonthsVeiw(key1, TIME_VIEW, _ALL_MONTHS));
    }

    protected void _prepareBasicQuery(SolrQuery solrQuery, Integer yearsQuery,Date startDate, Date endDate)
    {
        _addBasicConfiguration(solrQuery, yearsQuery, startDate, endDate);
        solrQuery.addFacetField(_CONTINENT, _COUNTRY_CODE, _CITY, ID,
                _LOCATION, _FISCALYEAR, _SOLARYEAR);
        solrQuery.setFacetMissing(true);
        solrQuery.set("f." + _LOCATION + ".facet.missing", false);
        solrQuery.set("f." + ID + ".facet.missing", false);
        solrQuery.set("f." + _FISCALYEAR + ".facet.missing", false);
        solrQuery.set("f." + _SOLARYEAR + ".facet.missing", false);
        solrQuery.set("f." + _FISCALYEAR + ".facet.sort", false);
        solrQuery.set("f." + _SOLARYEAR + ".facet.sort", false);

        solrQuery.set("f." + _CONTINENT + ".facet.mincount", 1);
        solrQuery.set("f." + _COUNTRY_CODE + ".facet.mincount", 1);
        solrQuery.set("f." + _CITY + ".facet.mincount", 1);
        solrQuery.set("f." + ID + ".facet.mincount", 1);
        solrQuery.set("f." + _LOCATION + ".facet.mincount", 1);
        solrQuery.set("f." + _FISCALYEAR + ".facet.mincount", 1);
        solrQuery.set("f." + _SOLARYEAR + ".facet.mincount", 1);
    }

    protected void _addBasicConfiguration(SolrQuery solrQuery,
            Integer yearsQuery, Date startDate, Date endDate)
    {
    	if (startDate != null || endDate != null) {
    		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    		String fq = "time:[";
    		if (startDate != null) {
    			fq += sdf.format(startDate);
    		}
    		else {
    			fq += "*";
    		}
    		fq += " TO ";
    		if (endDate != null) {
    			fq += sdf.format(endDate);
    		}
    		else {
    			fq += "*";
    		}
    		fq += "]";
    		solrQuery.addFilterQuery(fq);
    	}    	
        solrQuery.setRows(0);
        solrQuery.setFacet(true);
        solrQuery.set("facet.date", "time");
        solrQuery.set("facet.date.end", "NOW/MONTH+1MONTH");

        solrQuery.set("facet.date.start", "NOW/MONTH-" + yearsQuery + "YEARS");
        solrQuery.set("facet.date.include", "upper");
        solrQuery.set("facet.date.gap", "+1MONTHS");
        // solrQuery.set("facet.mincount", "1");
    }

    protected String addLocationCondition(String continet, String country,
            String city, String query)
    {
        if (continet != null)
        {
            query += " AND " + _CONTINENT + ":" + continet + " ";
        }
        if (country != null)
        {
            query += " AND " + _COUNTRY_CODE + ":" + country + " ";
        }
        if (city != null)
        {
            query += " AND " + _CITY + ":" + city + " ";
        }
        return query;
    }

    public PieStatisticBean generateCategoryView(SolrServer server,
            String key1, String key2, String key3, Integer hitsNumber,
            String query, Map<String, String> subQueries, String id, String... filters)
    {
        PieStatisticBean pieStatisticBean = new PieStatisticBean(key1, key2,
                key3);
        pieStatisticBean.setName(key2);
        Collection<StatisticDatasBeanRow> fullData = new ArrayList<StatisticDatasBeanRow>();
        Collection<StatisticDatasBeanRow> limitedData = new ArrayList<StatisticDatasBeanRow>();
        try
        {
            int totalHit = 0;
            for (String key : subQueries.keySet())
            {
                String q = "";
                try
                {
                    String name = key;
                    SolrQuery solrQuery = new SolrQuery();
                    q = query + " AND ("
                            + MessageFormat.format(subQueries.get(key), null,
                                    id) + ")";
                    solrQuery.setQuery(q);
                    if (StringUtils.isEmpty(name))
                        name = "Unknown";

                    if(filters != null) {
                        solrQuery.addFilterQuery(filters);
                    }

                    int count = new Long(server.query(solrQuery).getResults().getNumFound()).intValue();
                    limitedData.add(new StatisticDatasBeanRow(name, count));
                    totalHit += count;

                }
                catch (Exception e)
                {
                    limitedData.add(new StatisticDatasBeanRow(_NotAvailable,
                            null));
                    e.printStackTrace();
                }
            }

            pieStatisticBean.setHits(totalHit);
            pieStatisticBean.setDataTable(fullData);
            pieStatisticBean.setLimitedDataTable(limitedData);
            try
            {
                pieStatisticBean.setPercentages();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        catch (Exception e)
        {
            log.warn(e.getMessage());
            fullData.add(new StatisticDatasBeanRow(_NotAvailable, null));
            limitedData.add(new StatisticDatasBeanRow(_NotAvailable, null));
        }
        return pieStatisticBean;
    }
    
   
    public Integer getRelationObjectType()
    {
        return this.relationObjectType;
    }

    public void setRelationObjectType(Integer relationObjectType)
    {
        this.relationObjectType = relationObjectType;
    }
}
