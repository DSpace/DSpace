/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.statistics;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.app.rest.model.UsageReportPointDateRest;
import org.dspace.app.rest.model.UsageReportRest;
import org.dspace.app.rest.utils.UsageReportUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.content.DatasetTimeGenerator;
import org.dspace.statistics.content.StatisticsDatasetDisplay;
import org.dspace.statistics.content.filter.StatisticsFilter;
import org.dspace.statistics.content.filter.StatisticsSolrDateFilter;
import org.dspace.statistics.factory.StatisticsServiceFactory;
import org.dspace.statistics.service.SolrLoggerService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This report generator provides usage data aggregated over a specific period
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class TotalVisitPerPeriodGenerator extends AbstractUsageReportGenerator {
    private String periodType = "month";
    private int increment = 1;
    protected final SolrLoggerService solrLoggerService = StatisticsServiceFactory.getInstance().getSolrLoggerService();

    public void setPeriodType(String periodType) {
        this.periodType = periodType;
    }

    public void setIncrement(int increment) {
        this.increment = increment;
    }

    @Autowired
    private DiscoveryConfigurationService discoveryConfigurationService;

    /**
     * Create a stat usage report for the amount of TotalVisitPerMonth on a DSO, containing one point for each month
     * with the views on that DSO in that month with the range -6 months to now. If there are no views on the DSO
     * in a month, the point on that month contains views=0.
     *
     * @param context   DSpace context
     * @param dso       DSO we want usage report with TotalVisitsPerMonth to the DSO
     * @param startDate String to filter the start date of statistic
     * @param endDate   String to filter the end date of statistic
     * @return Rest object containing the TotalVisits usage report on the given DSO
     */
    public UsageReportRest createUsageReport(Context context, DSpaceObject dso, String startDate, String endDate)
        throws IOException, SolrServerException {
        DatasetTimeGenerator timeAxis = new DatasetTimeGenerator();
        boolean showTotal = timeAxis.isIncludeTotal();
        StatisticsDatasetDisplay statisticsDatasetDisplay = new StatisticsDatasetDisplay();
        String query = "";
        boolean hasValidRelation = false;
        if (getRelation() != null) {
            DiscoveryConfiguration discoveryConfiguration = discoveryConfigurationService
                                                                .getDiscoveryConfigurationByName(getRelation());
            if (discoveryConfiguration == null) {
                // not valid because not found bean with this relation configuration name
                hasValidRelation = false;
            } else {
                hasValidRelation = true;
                query = statisticsDatasetDisplay.composeQueryWithInverseRelation(
                    dso, discoveryConfiguration.getDefaultFilterQueries());
            }
        }
        if (!hasValidRelation) {
            if (dso.getType() != -1) {
                query += "type: " + dso.getType();
            }
            query += (query.equals("") ? "" : " AND ");
            query += "id:" + dso.getID();
        }
        //add date facets to filter query
        String filter_query = "";
        StatisticsSolrDateFilter dateFilter = new StatisticsSolrDateFilter();
        dateFilter.setStartStr("-" + (increment * getMaxResults()));
        dateFilter.setEndStr("+" + increment);
        dateFilter.setTypeStr(periodType);
        StatisticsFilter filter = dateFilter;
        filter_query += "(" + filter.toQuery() + ")";
        if (StringUtils.isNotBlank(filter_query)) {
            filter_query += " AND ";
        }
        filter_query += statisticsDatasetDisplay
                            .composeFilterQuery(startDate, endDate, hasValidRelation, dso.getType());
        // execute query
        ObjectCount[] topCounts1 = solrLoggerService.queryFacetField(query, filter_query, "id",
                getMaxResults(), false, null, 1);
        //if no data
        if (topCounts1.length == 0) {
            return returnEmptyDataReport();
        }
        UsageReportRest usageReportRest = new UsageReportRest();
        //in case of inverse relation hold total of views based on each month
        // foreach object that has relationship with dso
        int[] total_views = new int[increment + getMaxResults()];
        for (int j = 0; j < topCounts1.length; j++) {
            String newQuery = "";
            if (!hasValidRelation) {
                newQuery = "id" + ": " + ClientUtils
                        .escapeQueryChars(topCounts1[j].getValue()) + " AND " + query;
            } else {
                newQuery = "id" + ": " + ClientUtils
                        .escapeQueryChars(topCounts1[j].getValue());
            }
            // execute second query foreach result of first query
            ObjectCount[] maxDateFacetCounts = solrLoggerService
                                                   .queryFacetDate(newQuery, filter_query,
                                                                   getMaxResults(), periodType.toUpperCase(),
                    "-" + (increment * getMaxResults()), "+" + increment, showTotal, context,
                    0);
            if (topCounts1.length == 1) {
                for (ObjectCount maxDateFacetCount : maxDateFacetCounts) {
                    UsageReportPointDateRest monthPoint = new UsageReportPointDateRest();
                    monthPoint.setId(maxDateFacetCount.getValue());
                    monthPoint.addValue("views", (int) maxDateFacetCount.getCount());
                    usageReportRest.addPoint(monthPoint);
                }
            } else {
                //it means that have more than one data returned from first query
                //here is managed the case when dso has inverse relationship
                //for each value returned add views foreach object thas has relation with dso
                for (int i = 0; i < maxDateFacetCounts.length; i++) {
                    UsageReportPointDateRest monthPoint = new UsageReportPointDateRest();
                    //to not repeat month setting foreach object
                    if (j == 0) {
                        monthPoint.setId(maxDateFacetCounts[i].getValue());
                        usageReportRest.addPoint(monthPoint);
                    }
                    //add views of item related in month ->  i
                    total_views[i] += maxDateFacetCounts[i].getCount();
                }
            }
        }
        if (hasValidRelation && topCounts1.length > 1) {
            for (int pos = 0; pos < usageReportRest.getPoints().size(); pos++) {
                Map<String, Integer> values = new HashMap<>();
                values.put("views", total_views[pos]);
                usageReportRest.getPoints().get(pos).setValues(values);
            }
        }
        return usageReportRest;
    }

    @Override
    public String getReportType() {
        return UsageReportUtils.TOTAL_VISITS_PER_MONTH_REPORT_ID;
    }

    public UsageReportRest returnEmptyDataReport() {
        UsageReportRest usageReportRest = new UsageReportRest();
        Calendar cal = Calendar.getInstance();
        for (int k = 0; k <= increment * getMaxResults(); k++) {
            UsageReportPointDateRest monthPoint = new UsageReportPointDateRest();
            monthPoint.addValue("views", 0);
            String month = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
            monthPoint.setId(month + " " + cal.get(Calendar.YEAR));
            usageReportRest.addPoint(monthPoint);
            cal.add(Calendar.MONTH, -1);
        }
        return usageReportRest;
    }
}
