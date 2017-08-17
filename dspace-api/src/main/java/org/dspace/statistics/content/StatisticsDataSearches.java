/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.content;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.DSpaceObjectLegacySupport;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.statistics.Dataset;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.SolrLoggerServiceImpl;
import org.dspace.statistics.content.filter.StatisticsFilter;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * A statistics data implementation that will query the statistics backend for search information
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class StatisticsDataSearches extends StatisticsData {

    private static final DecimalFormat pageViewFormat = new DecimalFormat("0.00");
    private static final DecimalFormat percentageFormat = new DecimalFormat("0.00%");
    /** Current DSpaceObject for which to generate the statistics. */
    protected DSpaceObject currentDso;

    public StatisticsDataSearches(DSpaceObject dso) {
        super();
        this.currentDso = dso;
    }



    @Override
    public Dataset createDataset(Context context) throws SQLException, SolrServerException, IOException, ParseException {
        // Check if we already have one.
        // If we do then give it back.
        if(getDataset() != null)
        {
            return getDataset();
        }

        List<StatisticsFilter> filters = getFilters();
        List<String> defaultFilters = new ArrayList<String>();
        for (StatisticsFilter statisticsFilter : filters) {
            defaultFilters.add(statisticsFilter.toQuery());
        }

        String defaultFilterQuery = StringUtils.join(defaultFilters.iterator(), " AND ");

        String query = getQuery();

        Dataset dataset = new Dataset(0,0);
        List<DatasetGenerator> datasetGenerators = getDatasetGenerators();
        if(0 < datasetGenerators.size()){
            //At the moment we can only have one dataset generator
            DatasetGenerator datasetGenerator = datasetGenerators.get(0);
            if(datasetGenerator instanceof DatasetSearchGenerator){
                DatasetSearchGenerator typeGenerator = (DatasetSearchGenerator) datasetGenerator;

                if(typeGenerator.getMode() == DatasetSearchGenerator.Mode.SEARCH_OVERVIEW){
                    StringBuilder fqBuffer = new StringBuilder(defaultFilterQuery);
                    if(0 < fqBuffer.length())
                    {
                        fqBuffer.append(" AND ");
                    }
                    fqBuffer.append(getSearchFilterQuery());

                    ObjectCount[] topCounts = solrLoggerService.queryFacetField(query, fqBuffer.toString(), typeGenerator.getType(), typeGenerator.getMax(), (typeGenerator.isPercentage() || typeGenerator.isIncludeTotal()), null);
                    long totalCount = -1;
                    if(typeGenerator.isPercentage() && 0 < topCounts.length){
                        //Retrieve the total required to calculate the percentage
                        totalCount = topCounts[topCounts.length - 1].getCount();
                        //Remove the total count from view !
                        topCounts = (ObjectCount[]) ArrayUtils.subarray(topCounts, 0, topCounts.length - 1);
                    }

                    int nrColumns = 2;
                    if(typeGenerator.isPercentage()){
                        nrColumns++;
                    }
                    if(typeGenerator.isRetrievePageViews()){
                        nrColumns++;
                    }

                    dataset = new Dataset(topCounts.length, nrColumns);
                    dataset.setColLabel(0, "search-terms");
                    dataset.setColLabel(1, "searches");
                    if(typeGenerator.isPercentage()){
                        dataset.setColLabel(2, "percent-total");
                    }
                    if(typeGenerator.isRetrievePageViews()){
                        dataset.setColLabel(3, "views-search");
                    }
                    for (int i = 0; i < topCounts.length; i++) {
                        ObjectCount queryCount = topCounts[i];

                        dataset.setRowLabel(i, String.valueOf(i + 1));
                        String displayedValue = queryCount.getValue();
                        if(DSpaceServicesFactory.getInstance().getConfigurationService().getPropertyAsType("usage-statistics.search.statistics.unescape.queries", Boolean.TRUE)){
                            displayedValue = displayedValue.replace("\\", "");
                        }
                        dataset.addValueToMatrix(i, 0, displayedValue);
                        dataset.addValueToMatrix(i, 1, queryCount.getCount());
                        if(typeGenerator.isPercentage()){
                            //Calculate our percentage from the total !
                            dataset.addValueToMatrix(i, 2, percentageFormat.format(((float) queryCount.getCount() / totalCount)));
                        }
                        if(typeGenerator.isRetrievePageViews()){
                            String queryString = ClientUtils.escapeQueryChars(queryCount.getValue());
                            if(queryString.equals("")){
                                queryString = "\"\"";
                            }

                            ObjectCount totalPageViews = getTotalPageViews("query:" + queryString, defaultFilterQuery);
                            dataset.addValueToMatrix(i, 3, pageViewFormat.format((float) totalPageViews.getCount() / queryCount.getCount()));
                        }
                    }
                }else
                if(typeGenerator.getMode() == DatasetSearchGenerator.Mode.SEARCH_OVERVIEW_TOTAL){
                    //Retrieve the total counts !
                    ObjectCount totalCount = solrLoggerService.queryTotal(query, getSearchFilterQuery());

                    //Retrieve the filtered count by using the default filter query
                    StringBuilder fqBuffer = new StringBuilder(defaultFilterQuery);
                    if(0 < fqBuffer.length())
                    {
                        fqBuffer.append(" AND ");
                    }
                    fqBuffer.append(getSearchFilterQuery());

                    ObjectCount totalFiltered = solrLoggerService.queryTotal(query, fqBuffer.toString());


                    fqBuffer = new StringBuilder(defaultFilterQuery);
                    if(0 < fqBuffer.length())
                    {
                        fqBuffer.append(" AND ");
                    }
                    fqBuffer.append("statistics_type:").append(SolrLoggerServiceImpl.StatisticsType.SEARCH_RESULT.text());

                    ObjectCount totalPageViews = getTotalPageViews(query, defaultFilterQuery);

                    dataset = new Dataset(1, 3);
                    dataset.setRowLabel(0, "");


                    dataset.setColLabel(0, "searches");
                    dataset.addValueToMatrix(0, 0, totalFiltered.getCount());
                    dataset.setColLabel(1, "percent-total");
                    //Ensure that we do NOT divide by 0
                    float percentTotal;
                    if(totalCount.getCount() == 0){
                        percentTotal = 0;
                    }else{
                        percentTotal = (float) totalFiltered.getCount() / totalCount.getCount();
                    }


                    dataset.addValueToMatrix(0, 1, percentageFormat.format(percentTotal));
                    dataset.setColLabel(2, "views-search");
                    //Ensure that we do NOT divide by 0
                    float pageViews;
                    if(totalFiltered.getCount() == 0){
                        pageViews = 0;
                    }else{
                        pageViews = (float) totalPageViews.getCount() / totalFiltered.getCount();
                    }

                    dataset.addValueToMatrix(0, 2, pageViewFormat.format(pageViews));
                }
            }else{
                throw new IllegalArgumentException("Data generator with class" + datasetGenerator.getClass().getName() + " is not supported by the statistics search engine !");
            }
        }

        return dataset;
    }

    /**
     * Returns the query to be used in solr
     * in case of a dso a scopeDso query will be returned otherwise the default *:* query will be used
     * @return the query as a string
     */
    protected String getQuery() {
        String query;
        if(currentDso != null){
            query = "scopeType: " + currentDso.getType() + " AND ";
            if(currentDso instanceof DSpaceObjectLegacySupport){
                query += " (scopeId:" + currentDso.getID() + " OR scopeId:" + ((DSpaceObjectLegacySupport) currentDso).getLegacyId() + ")";
            }else{
                query += "scopeId:" + currentDso.getID();
            }
        }else{
            query = "*:*";
        }
        return query;
    }

    protected ObjectCount getTotalPageViews(String query, String defaultFilterQuery) throws SolrServerException {
        StringBuilder fqBuffer;
        fqBuffer = new StringBuilder(defaultFilterQuery);
        if(0 < fqBuffer.length())
        {
            fqBuffer.append(" AND ");
        }
        fqBuffer.append("statistics_type:").append(SolrLoggerServiceImpl.StatisticsType.SEARCH_RESULT.text());


        //Retrieve the number of page views by this query !
        return solrLoggerService.queryTotal(query, fqBuffer.toString());
    }

    /**
     * Returns a filter query that only allows new searches to pass
     * new searches are searches that haven't been paged through
     * @return a solr filterquery
     */
    protected String getSearchFilterQuery() {
        StringBuilder fqBuffer = new StringBuilder();
        fqBuffer.append("statistics_type:").append(SolrLoggerServiceImpl.StatisticsType.SEARCH.text());
        //Also append a filter query to ensure that paging is left out !
        fqBuffer.append(" AND -page:[* TO *]");
        return fqBuffer.toString();
    }
}
