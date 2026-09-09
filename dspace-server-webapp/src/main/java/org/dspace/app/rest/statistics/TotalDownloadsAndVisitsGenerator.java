/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.statistics;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.rest.model.UsageReportPointDsoTotalVisitsRest;
import org.dspace.app.rest.model.UsageReportRest;
import org.dspace.app.rest.utils.UsageReportUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.content.StatisticsDatasetDisplay;
import org.dspace.statistics.factory.StatisticsServiceFactory;
import org.dspace.statistics.service.SolrLoggerService;
import org.springframework.beans.factory.annotation.Autowired;

public class TotalDownloadsAndVisitsGenerator extends AbstractUsageReportGenerator {
    @Autowired
    private DiscoveryConfigurationService discoveryConfigurationService;
    protected final SolrLoggerService solrLoggerService = StatisticsServiceFactory.getInstance().getSolrLoggerService();

    /**
     * Create stat usage report of the items most popular over the entire site or a
     * specific community, collection
     *
     * @param context   DSpace context
     * @param dso       DSO we want the stats dataset of
     * @param startDate String to filter the start date of statistic
     * @param endDate   String to filter the end date of statistic
     * @return Usage report with top most popular items
     */

    @Override
    public UsageReportRest createUsageReport(Context context, DSpaceObject dso, String startDate, String endDate)
        throws SolrServerException, IOException, SQLException {
        StatisticsDatasetDisplay statisticsDatasetDisplay = new StatisticsDatasetDisplay();
        boolean hasValidRelation = false;
        // query for item
        String query = "";
        if (getRelation() != null) {
            DiscoveryConfiguration discoveryConfiguration =
                discoveryConfigurationService.getDiscoveryConfigurationByName(getRelation());
            if (discoveryConfiguration == null) {
                // not valid because not found bean with this relation configuration name
                hasValidRelation = false;

            } else {
                hasValidRelation = true;
                query = statisticsDatasetDisplay.composeQueryWithInverseRelation(dso,
                    discoveryConfiguration.getDefaultFilterQueries(), dso.getType());
            }
        }
        if (!hasValidRelation) {
            query += "type: " + dso.getType();
            query += " AND ";
            query += "id:" + dso.getID();
        }
        String filter_query = statisticsDatasetDisplay.
                                                          composeFilterQuery(
                                                              startDate, endDate,
                                                              hasValidRelation, dso.getType());
        String filter_query_bitstream = statisticsDatasetDisplay
                                            .composeFilterQuery(startDate, endDate,
                                                                hasValidRelation, Constants.BITSTREAM);
        //generate usageraport
        UsageReportRest usageReportRest = new UsageReportRest();
        // View point
        UsageReportPointDsoTotalVisitsRest totalVisitPointItem = new UsageReportPointDsoTotalVisitsRest();
        totalVisitPointItem.setType(StringUtils.substringAfterLast(dso.getClass().getName().toLowerCase(), "."));
        totalVisitPointItem.setLabel("Item visits");
        //Downloads point
        UsageReportPointDsoTotalVisitsRest totalVisitPointBitstream = new UsageReportPointDsoTotalVisitsRest();
        totalVisitPointBitstream.setType("bitstream");
        totalVisitPointBitstream.setLabel("File visits");
        // first check item visits
        ObjectCount[] topCounts = solrLoggerService.queryFacetField(query, filter_query, "id",
                getMaxResults(), false, null, 1);
        //use accumulator in case the query returns more than one data
        int downloads = 0;
        int views = 0;
        for (ObjectCount topCount : topCounts) {
            //add visits for item
            views += (int) topCount.getCount();
            // check bitstreams  statistics related with this item
            String bitStreamQuery = createQueryKeyword(dso.getType()) + ":" + topCount.getValue();
            ObjectCount[] topCounts1 = solrLoggerService.queryFacetField(bitStreamQuery,
                    filter_query_bitstream, "id",
                    getMaxResults(), false, null, 1);
            //it can have more than one
            for (ObjectCount objectCountBitStream : topCounts1) {
                //calculate downloads for item count bitstream values
                downloads += objectCountBitStream.getCount();
            }
        }
        // add views to report
        totalVisitPointItem.addValue("views", views);
        usageReportRest.addPoint(totalVisitPointItem);
        // add downloads to report
        totalVisitPointBitstream.addValue("views", downloads);
        usageReportRest.addPoint(totalVisitPointBitstream);
        return usageReportRest;
    }


    @Override
    public String getReportType() {
        return UsageReportUtils.TOTAL_VISITS_TOTAL_DOWNLOADS;
    }

    // to support different types of dso
    public String createQueryKeyword(int dsoType) {
        String owningStr = "";
        switch (dsoType) {
            case Constants.ITEM:
                owningStr = "owningItem";
                break;
            case Constants.COLLECTION:
                owningStr = "owningColl";
                break;
            case Constants.COMMUNITY:
                owningStr = "owningComm";
                break;
            default:
                owningStr = "owningItem";
                break;
        }
        return owningStr;
    }

}
