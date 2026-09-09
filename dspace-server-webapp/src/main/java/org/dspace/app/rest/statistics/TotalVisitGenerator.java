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
import java.text.ParseException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

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
import org.dspace.statistics.Dataset;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.content.StatisticsDatasetDisplay;
import org.dspace.statistics.factory.StatisticsServiceFactory;
import org.dspace.statistics.service.SolrLoggerService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This report generator provides TotalVisit on a DSO
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class TotalVisitGenerator extends AbstractUsageReportGenerator {
    protected final SolrLoggerService solrLoggerService = StatisticsServiceFactory.getInstance().getSolrLoggerService();
    @Autowired
    private DiscoveryConfigurationService discoveryConfigurationService;

    /**
     * Create a stat usage report for the amount of TotalVisit on a DSO, containing one point with the amount of
     * views on the DSO in. If there are no views on the DSO this point contains views=0.
     *
     * @param context DSpace context
     * @param dso     DSO we want usage report with TotalVisits on the DSO
     * @return Rest object containing the TotalVisits usage report of the given DSO
     */

    public UsageReportRest createUsageReport(Context context, DSpaceObject dso, String startDate, String endDate) {
        Dataset dataset;
        try {
            dataset = this.getDSOStatsDataset(context, dso, dso.getType(), startDate, endDate);
        } catch (SQLException | IOException | ParseException | SolrServerException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        UsageReportRest usageReportRest = new UsageReportRest();
        UsageReportPointDsoTotalVisitsRest totalVisitPoint = new UsageReportPointDsoTotalVisitsRest();
        totalVisitPoint.setType(StringUtils.substringAfterLast(dso.getClass().getName().toLowerCase(), "."));
        totalVisitPoint.setId(dso.getID().toString());
        if (dataset.getColLabels().size() > 0) {
            totalVisitPoint.setLabel(getLabel(dso));
            //to support the case when returns more thane one data -> cases with inverse relation
            String[] values = dataset.getMatrix()[0];
            int total_views = 0;
            if (values != null) {
                for (String value : values) {
                    total_views += Integer.parseInt(value);
                }
            }
            totalVisitPoint.addValue("views", total_views);
        } else {
            totalVisitPoint.setLabel(getLabel(dso));
            totalVisitPoint.addValue("views", 0);
        }
        usageReportRest.addPoint(totalVisitPoint);
        return usageReportRest;
    }

    private String getLabel(final DSpaceObject dso) {
        return Optional.ofNullable(this.getRelation()).map(ignored -> "Views").orElseGet(dso::getName);
    }


    @Override
    public String getReportType() {
        return UsageReportUtils.TOTAL_VISITS_REPORT_ID;
    }

    /**
     * Retrieves the stats dataset of a given DSO, of given type, with a given facetMinCount limit (usually either 0
     * or 1, 0 if we want a data point even though the facet data point has 0 matching results).
     *
     * @param context   DSpace context
     * @param dso       DSO we want the stats dataset of
     * @param dsoType   Type of DSO we want the stats dataset of
     * @param startDate String to filter the start date of statistic
     * @param endDate   String to filter the end date of statistic
     * @return Stats dataset with the given filters.
     */
    Dataset getDSOStatsDataset(Context context, DSpaceObject dso, int dsoType, String startDate, String endDate)
            throws SQLException, IOException, ParseException, SolrServerException {
        StatisticsDatasetDisplay statisticsDatasetDisplay = new StatisticsDatasetDisplay();
        Dataset dataset;
        int type_of_dso = dsoType;
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
                    dso, discoveryConfiguration.getDefaultFilterQueries(), dso.getType());
                type_of_dso = dso.getType();
            }
        }
        if (!hasValidRelation) {
            if (dso != null) {
                query += createQuery(dso.getID(), dso.getType(), dsoType);
            }
        }
        String filter_query = statisticsDatasetDisplay
                                  .composeFilterQuery(startDate, endDate, hasValidRelation, type_of_dso);
        //is used when generator has inverse relation property
        ObjectCount[] result = new ObjectCount[0];
        ObjectCount[] topCounts = solrLoggerService.queryFacetField(query, filter_query, "id",
                getMaxResults(), false, null, 1);
        // if dso.getType() != dsoType means that are are searching for downloads
        //this condition supports TotalDownloadsGenerator
        if (hasValidRelation && dso.getType() != dsoType) {
            //in case of inverse relation first search for objects related with dso
            // then find bitstream views for those objects
            for (ObjectCount topCount : topCounts) {
                String fq = "(statistics_type:view)";
                String newQuery = createQueryForInverseRelation(topCount.getValue());
                ObjectCount[] topCountsBitstream = solrLoggerService.queryFacetField(newQuery, fq, "id",
                        getMaxResults(), false, null, 1);
                // add each query result to result variable
                result = Stream.of(result, topCountsBitstream).flatMap(Stream::of)
                        .toArray(ObjectCount[]::new);
            }
            dataset = new Dataset(1, result.length);
            topCounts = result;
        } else {
            dataset = new Dataset(1, topCounts.length);
        }
        for (int i = 0; i < topCounts.length; i++) {
            ObjectCount count = topCounts[i];
            dataset.setColLabel(i, statisticsDatasetDisplay
                                       .getResultName("", count.getValue(),
                                                      dso, dsoType, -1, context));
            dataset.setColLabelAttr(i, statisticsDatasetDisplay.getAttributes(count.getValue(), dso, dsoType, context));
            dataset.addValueToMatrix(0, i, count.getCount());
        }
        return dataset;
    }

    public String createQuery(UUID dso_id, int type, int dsoType) {
        String query = "type: " + dsoType;
        if (dsoType == type) {
            query += " AND ";
            query += "id:" + dso_id;
        } else {
            query += " AND ";
            String owningStr = "";
            switch (type) {
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
                    break;
            }
            owningStr += ":" + dso_id;
            query += owningStr;
        }
        return query;
    }

    public String createQueryForInverseRelation(String uuid) {
        //as type of the objects in inverse relation is unknown, query all types with OR
        String query = "type: " + Constants.BITSTREAM;
        query += " AND ";
        query += "(owningItem:" + uuid + " OR " + "owningColl:" + uuid + " OR " + "owningComm:" + uuid + ")";
        query += " AND -bundleName:LICENSE AND -bundleName:THUMBNAIL AND -bundleName:SWORD AND -bundleName:TEXT";
        return query;
    }

}
