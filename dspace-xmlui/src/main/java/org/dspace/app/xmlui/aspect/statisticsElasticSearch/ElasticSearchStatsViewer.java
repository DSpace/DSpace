/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.statisticsElasticSearch;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.content.*;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.statistics.DataTermsFacet;
import org.dspace.statistics.factory.StatisticsServiceFactory;
import org.dspace.statistics.service.ElasticSearchLoggerService;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;

import org.elasticsearch.index.query.*;

import org.elasticsearch.search.facet.FacetBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet;
import org.elasticsearch.search.facet.terms.TermsFacet;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * Usage Statistics viewer, powered by Elastic Search.
 * Allows for the user to dig deeper into the statistics for topDownloads, topCountries, etc.
 *
 * @deprecated  As of DSpace 6.0, ElasticSearch statistics are replaced by Solr statistics
 * @see org.dspace.app.xmlui.aspect.statistics.StatisticsTransformer#StatisticsTransformer
 *
 * @author Peter Dietz (pdietz84@gmail.com)
 */
public class ElasticSearchStatsViewer extends AbstractDSpaceTransformer {
    private static Logger log = Logger.getLogger(ElasticSearchStatsViewer.class);
    
    public static final String elasticStatisticsPath = "stats";

    private static ThreadLocal<DateFormat> monthAndYearFormat = new ThreadLocal<DateFormat>(){
                        @Override
                        protected DateFormat initialValue() {
                            return new SimpleDateFormat("MMMMM yyyy");
                        }
                      };
    private static ThreadLocal<DateFormat> dateFormat = new ThreadLocal<DateFormat>(){
                        @Override
                        protected DateFormat initialValue() {
                            return new SimpleDateFormat("yyyy-MM-dd");
                        }
                      };

    private static Client client;
    private static Division division;
    private static DSpaceObject dso;
    private static Date dateStart;
    private static Date dateEnd;

    protected ElasticSearchLoggerService elasticSearchLoggerService = StatisticsServiceFactory.getInstance().getElasticSearchLoggerService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
	protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();

    protected static TermFilterBuilder justOriginals = FilterBuilders.termFilter("bundleName", "ORIGINAL");

    protected static FacetBuilder facetTopCountries = FacetBuilders.termsFacet("top_countries").field("country.untouched").size(150)
            .facetFilter(FilterBuilders.andFilter(
                justOriginals,
                FilterBuilders.notFilter(FilterBuilders.termFilter("country.untouched", "")))
            );

    protected static FacetBuilder facetMonthlyDownloads = FacetBuilders.dateHistogramFacet("monthly_downloads").field("time").interval("month")
            .facetFilter(FilterBuilders.andFilter(
                FilterBuilders.termFilter("type", "BITSTREAM"),
                justOriginals
            ));
    
    protected static FacetBuilder facetTopBitstreamsAllTime = FacetBuilders.termsFacet("top_bitstreams_alltime").field("id")
            .facetFilter(FilterBuilders.andFilter(
                    FilterBuilders.termFilter("type", "BITSTREAM"),
                    justOriginals
            ));
    
    protected static FacetBuilder facetTopUSCities = FacetBuilders.termsFacet("top_US_cities").field("city.untouched").size(50)
            .facetFilter(FilterBuilders.andFilter(
                FilterBuilders.termFilter("countryCode", "US"),
                justOriginals,
                FilterBuilders.notFilter(FilterBuilders.termFilter("city.untouched", ""))
            ));
    
    protected static FacetBuilder facetTopUniqueIP = FacetBuilders.termsFacet("top_unique_ips").field("ip");
    
    protected static FacetBuilder facetTopTypes = FacetBuilders.termsFacet("top_types").field("type");

    /** Language strings */
    private static final Message T_dspace_home = message("xmlui.general.dspace_home");

    private static final Message T_trail = message("xmlui.ArtifactBrowser.ItemViewer.trail");

    public void addPageMeta(PageMeta pageMeta) throws WingException, SQLException {
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        pageMeta.addMetadata("title").addContent("Statistics Report for : " + dso.getName());

        pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
        HandleUtil.buildHandleTrail(context, dso,pageMeta,contextPath, true);
        pageMeta.addTrail().addContent("View Statistics");
    }

    public ElasticSearchStatsViewer() {

    }

    public ElasticSearchStatsViewer(DSpaceObject dso, Date dateStart, Date dateEnd) {
        this.dso = dso;
        this.dateStart = dateStart;
        this.dateEnd = dateEnd;
        client = elasticSearchLoggerService.getClient();
    }
    
    public void addBody(Body body) throws WingException, SQLException {
        try {
            //Try to find our dspace object
            dso = HandleUtil.obtainHandle(objectModel);
            client = elasticSearchLoggerService.getClient();

            division = body.addDivision("elastic-stats");
            division.setHead("Statistical Report for " + dso.getName());
            division.addHidden("containerName").setValue(dso.getName());

            division.addHidden("baseURLStats").setValue(contextPath + "/handle/" + dso.getHandle() + "/" + elasticStatisticsPath);
            Request request = ObjectModelHelper.getRequest(objectModel);
            String[] requestURIElements = request.getRequestURI().split("/");

            // If we are on the homepage of the statistics portal, then we just show the summary report
            // Otherwise we will show a form to let user enter more information for deeper detail.
            if(requestURIElements[requestURIElements.length-1].trim().equalsIgnoreCase(elasticStatisticsPath)) {
                //Homepage will show the last 5 years worth of Data, and no form generator.
                Calendar cal = Calendar.getInstance();
                dateEnd = cal.getTime();

                //Roll back to Jan 1 0:00.000 five years ago.
                cal.roll(Calendar.YEAR, -5);
                cal.set(Calendar.MONTH, 0);
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY,0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                dateStart = cal.getTime();

                division.addHidden("reportDepth").setValue("summary");
                String dateRange = "Last Five Years";
                division.addPara("Showing Data ( " + dateRange + " )");
                division.addHidden("timeRangeString").setValue("Data Range: " + dateRange);
                if(dateStart != null) {
                    division.addHidden("dateStart").setValue(dateFormat.get().format(dateStart));
                }
                if(dateEnd != null) {
                    division.addHidden("dateEnd").setValue(dateFormat.get().format(dateEnd));
                }

                showAllReports();
                
            } else {
                //Other pages will show a form to choose which date range.
                ReportGenerator reportGenerator = new ReportGenerator();
                reportGenerator.addReportGeneratorForm(division, request);
                
                dateStart = reportGenerator.getDateStart();
                dateEnd = reportGenerator.getDateEnd();

                String requestedReport = requestURIElements[requestURIElements.length-1];
                log.info("Requested report is: "+ requestedReport);
                division.addHidden("reportDepth").setValue("detail");
                
                String dateRange = "";
                if(dateStart != null && dateEnd != null) {
                    dateRange = "from: "+dateFormat.get().format(dateStart) + " to: "+dateFormat.get().format(dateEnd);
                } else if (dateStart != null && dateEnd == null) {
                    dateRange = "starting from: "+dateFormat.get().format(dateStart);
                } else if(dateStart == null && dateEnd != null) {
                    dateRange = "ending with: "+dateFormat.get().format(dateEnd);
                } else if(dateStart == null && dateEnd == null) {
                    dateRange = "All Data Available";
                }
                division.addPara("Showing Data ( " + dateRange + " )");
                division.addHidden("timeRangeString").setValue(dateRange);
                if(dateStart != null) {
                    division.addHidden("dateStart").setValue(dateFormat.get().format(dateStart));
                }
                if(dateEnd != null) {
                    division.addHidden("dateEnd").setValue(dateFormat.get().format(dateEnd));
                }


                division.addHidden("reportName").setValue(requestedReport);

                if(requestedReport.equalsIgnoreCase("topCountries"))
                {
                    SearchRequestBuilder requestBuilder = facetedQueryBuilder(facetTopCountries, facetTopUSCities);
                    searchResponseToDRI(requestBuilder);
                }
                else if(requestedReport.equalsIgnoreCase("fileDownloads"))
                {
                    SearchRequestBuilder requestBuilder = facetedQueryBuilder(facetMonthlyDownloads);
                    searchResponseToDRI(requestBuilder);
                }
                else if(requestedReport.equalsIgnoreCase("topDownloads"))
                {
                    SearchRequestBuilder requestBuilder = facetedQueryBuilder(facetTopBitstreamsAllTime, facetTopBitstreamsLastMonth());
                    SearchResponse resp = searchResponseToDRI(requestBuilder);

                    TermsFacet bitstreamsAllTimeFacet = resp.getFacets().facet(TermsFacet.class, "top_bitstreams_alltime");
                    addTermFacetToTable(bitstreamsAllTimeFacet, division, "Bitstream", "Top Downloads (all time)");

                    TermsFacet bitstreamsFacet = resp.getFacets().facet(TermsFacet.class, "top_bitstreams_lastmonth");
                    addTermFacetToTable(bitstreamsFacet, division, "Bitstream", "Top Downloads for " + getLastMonthString());
                }
            }

        } finally {
            //client.close();
        }
    }
    
    public void showAllReports() throws WingException, SQLException{
        List<FacetBuilder> summaryFacets = new ArrayList<FacetBuilder>();
        summaryFacets.add(facetTopTypes);
        summaryFacets.add(facetTopUniqueIP);
        summaryFacets.add(facetTopCountries);
        summaryFacets.add(facetTopUSCities);
        summaryFacets.add(facetTopBitstreamsLastMonth());
        summaryFacets.add(facetTopBitstreamsAllTime);
        summaryFacets.add(facetMonthlyDownloads);

        SearchRequestBuilder requestBuilder = facetedQueryBuilder(summaryFacets);
        SearchResponse resp = searchResponseToDRI(requestBuilder);

                // Top Downloads to Owning Object
        TermsFacet bitstreamsFacet = resp.getFacets().facet(TermsFacet.class, "top_bitstreams_lastmonth");
        addTermFacetToTable(bitstreamsFacet, division, "Bitstream", "Top Downloads for " + getLastMonthString());

        // Convert Elastic Search data to a common DataTermsFacet object, and stuff in DRI/HTML of page.
        TermsFacet topBitstreamsFacet = resp.getFacets().facet(TermsFacet.class, "top_bitstreams_lastmonth");
        List<? extends TermsFacet.Entry> termsFacetEntries = topBitstreamsFacet.getEntries();
        DataTermsFacet termsFacet = new DataTermsFacet();
        for(TermsFacet.Entry entry : termsFacetEntries) {
            termsFacet.addTermFacet(new DataTermsFacet.TermsFacet(entry.getTerm().string(), entry.getCount()));
        }
        division.addHidden("jsonTopDownloads").setValue(termsFacet.toJson());
    }
    
    public FacetBuilder facetTopBitstreamsLastMonth() {
        Calendar calendar = Calendar.getInstance();

        // Show Previous Whole Month
        calendar.add(Calendar.MONTH, -1);

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
        String lowerBound = dateFormat.get().format(calendar.getTime());

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        String upperBound = dateFormat.get().format(calendar.getTime());

        log.info("Lower:"+lowerBound+" -- Upper:"+upperBound);
        
        return FacetBuilders.termsFacet("top_bitstreams_lastmonth").field("id")
                .facetFilter(FilterBuilders.andFilter(
                        FilterBuilders.termFilter("type", "BITSTREAM"),
                        justOriginals,
                        FilterBuilders.rangeFilter("time").from(lowerBound).to(upperBound)
                ));
    }
    
    public String getLastMonthString() {
        Calendar calendar = Calendar.getInstance();
        // Show Previous Whole Month
        calendar.add(Calendar.MONTH, -1);

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
        return monthAndYearFormat.get().format(calendar.getTime());
    }
    
    public SearchRequestBuilder facetedQueryBuilder(FacetBuilder facet) throws WingException{
        List<FacetBuilder> facetList = new ArrayList<FacetBuilder>();
        facetList.add(facet);
        return facetedQueryBuilder(facetList);
    }

    public SearchRequestBuilder facetedQueryBuilder(FacetBuilder... facets) throws WingException {
        List<FacetBuilder> facetList = new ArrayList<FacetBuilder>();

        for(FacetBuilder facet : facets) {
            facetList.add(facet);
        }

        return facetedQueryBuilder(facetList);
    }
    
    public SearchRequestBuilder facetedQueryBuilder(List<FacetBuilder> facetList) {
        TermQueryBuilder termQuery = QueryBuilders.termQuery(getOwningText(dso), dso.getID());
        FilterBuilder rangeFilter = FilterBuilders.rangeFilter("time").from(dateStart).to(dateEnd);
        FilteredQueryBuilder filteredQueryBuilder = QueryBuilders.filteredQuery(termQuery, rangeFilter);

        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(elasticSearchLoggerService.getIndexName())
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(filteredQueryBuilder)
                .setSize(0);

        for(FacetBuilder facet : facetList) {
            searchRequestBuilder.addFacet(facet);
        }

        return searchRequestBuilder;
    }

    public SearchResponse searchResponseToDRI(SearchRequestBuilder searchRequestBuilder) throws WingException{
        division.addHidden("request").setValue(searchRequestBuilder.toString());

        SearchResponse resp = searchRequestBuilder.execute().actionGet();

        if(resp == null) {
            log.info("Elastic Search is down for searching.");
            division.addPara("Elastic Search seems to be down :(");
            return null;
        }

        division.addHidden("response").setValue(resp.toString());
        division.addDivision("chart_div");

        return resp;
    }

    private void addTermFacetToTable(TermsFacet termsFacet, Division division, String termName, String tableHeader) throws WingException, SQLException {
        List<? extends TermsFacet.Entry> termsFacetEntries = termsFacet.getEntries();

        if(termName.equalsIgnoreCase("country")) {
            division.addDivision("chart_div_map");
        }

        Table facetTable = division.addTable("facet-"+termName, termsFacetEntries.size()+1, 10);
        facetTable.setHead(tableHeader);

        Row facetTableHeaderRow = facetTable.addRow(Row.ROLE_HEADER);
        if(termName.equalsIgnoreCase("bitstream")) {
            facetTableHeaderRow.addCellContent("Title");
            facetTableHeaderRow.addCellContent("Creator");
            facetTableHeaderRow.addCellContent("Publisher");
            facetTableHeaderRow.addCellContent("Date");
        } else {
            facetTableHeaderRow.addCell().addContent(termName);
        }

        facetTableHeaderRow.addCell().addContent("Count");

        if(termsFacetEntries.size() == 0) {
            facetTable.addRow().addCell().addContent("No Data Available");
            return;
        }

        for(TermsFacet.Entry facetEntry : termsFacetEntries) {
            Row row = facetTable.addRow();

            if(termName.equalsIgnoreCase("bitstream")) {
                Bitstream bitstream = bitstreamService.findByIdOrLegacyId(context, facetEntry.getTerm().string());
                Item item = (Item) bitstreamService.getParentObject(context, bitstream);
                row.addCell().addXref(contextPath + "/handle/" + item.getHandle(), item.getName());
                row.addCellContent(getFirstMetadataValue(item, "dc.creator"));
                row.addCellContent(getFirstMetadataValue(item, "dc.publisher"));
                row.addCellContent(getFirstMetadataValue(item, "dc.date.issued"));
            } else if(termName.equalsIgnoreCase("country")) {
                row.addCell("country", Cell.ROLE_DATA,"country").addContent(new Locale("en", facetEntry.getTerm().string()).getDisplayCountry());
            } else {
                row.addCell().addContent(facetEntry.getTerm().string());
            }
            row.addCell("count", Cell.ROLE_DATA, "count").addContent(facetEntry.getCount());
        }
    }

    private void addDateHistogramToTable(DateHistogramFacet monthlyDownloadsFacet, Division division, String termName, String termDescription) throws WingException {
        List<? extends DateHistogramFacet.Entry> monthlyFacetEntries = monthlyDownloadsFacet.getEntries();

        if(monthlyFacetEntries.size() == 0) {
            division.addPara("Empty result set for: "+termName);
            return;
        }

        Table monthlyTable = division.addTable(termName, monthlyFacetEntries.size(), 10);
        monthlyTable.setHead(termDescription);
        Row tableHeaderRow = monthlyTable.addRow(Row.ROLE_HEADER);
        tableHeaderRow.addCell("date", Cell.ROLE_HEADER,null).addContent("Month/Date");
        tableHeaderRow.addCell("count", Cell.ROLE_HEADER,null).addContent("Count");

        for(DateHistogramFacet.Entry histogramEntry : monthlyFacetEntries) {
            Row dataRow = monthlyTable.addRow();
            Date facetDate = new Date(histogramEntry.getTime());
            dataRow.addCell("date", Cell.ROLE_DATA,"date").addContent(dateFormat.get().format(facetDate));
            dataRow.addCell("count", Cell.ROLE_DATA,"count").addContent("" + histogramEntry.getCount());
        }
    }
    
    private String getOwningText(DSpaceObject dso) {
        switch (dso.getType()) {
            case Constants.ITEM:
                return "owningItem";
            case Constants.COLLECTION:
                return "owningColl";
            case Constants.COMMUNITY:
                return "owningComm";
            default:
                return "";
        }
    }
    
    private String getFirstMetadataValue(Item item, String metadataKey) {
        List<MetadataValue> dcValue = itemService.getMetadataByMetadataString(item, metadataKey);
        if(dcValue.size() > 0) {
            return dcValue.get(0).getValue();
        } else {
            return "";
        }
    }
}
