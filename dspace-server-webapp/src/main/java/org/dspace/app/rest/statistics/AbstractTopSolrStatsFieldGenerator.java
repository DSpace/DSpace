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

import org.apache.solr.client.solrj.SolrServerException;
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
 * This report generator provides methods useful to build report around a solr stats field.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public abstract class AbstractTopSolrStatsFieldGenerator extends AbstractUsageReportGenerator {
    protected final SolrLoggerService solrLoggerService = StatisticsServiceFactory.getInstance().getSolrLoggerService();
    @Autowired
    private DiscoveryConfigurationService discoveryConfigurationService;

    private Integer dsoType;

    /**
     * Retrieves the stats dataset of a given dso, with a given axisType (example countryCode, city), which
     * corresponds to a solr field, and a given facetMinCount limit (usually either 0 or 1, 0 if we want a data point
     * even though the facet data point has 0 matching results).
     *
     * @param context        DSpace context
     * @param dso            DSO we want the stats dataset of
     * @param typeAxisString String of the type we want on the axis of the dataset (corresponds to solr field),
     *                       examples: countryCode, city
     * @param startDate      String to filter the start date of statistic
     * @param endDate        String to filter the end date of statistic
     * @return Stats dataset with the given type on the axis, of the given DSO and with given facetMinCount
     */

    Dataset getTypeStatsDataset(Context context, DSpaceObject dso, String typeAxisString, String startDate,
                                String endDate)
            throws SQLException, IOException, ParseException, SolrServerException {
        StatisticsDatasetDisplay statisticsDatasetDisplay = new StatisticsDatasetDisplay();
        Dataset dataset;
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
                    dso, discoveryConfiguration.getDefaultFilterQueries(), getDsoType(dso));
            }
        }

        if (!hasValidRelation) {
            if (getDsoType(dso) != -1) {
                query += "type: " + getDsoType(dso);
            }

            if (isNotSiteObject(dso)) {
                query += (query.equals("") ? "" : " AND ") + "id:" + dso.getID();
            }

        }

        String filter_query = statisticsDatasetDisplay
                                  .composeFilterQuery(startDate, endDate, hasValidRelation, getDsoType(dso));
        ObjectCount[] topCounts = solrLoggerService.queryFacetField(query, filter_query, typeAxisString,
                getMaxResults(), false, null, 1);
        dataset = new Dataset(1, topCounts.length);
        for (int i = 0; i < topCounts.length; i++) {
            ObjectCount count = topCounts[i];
            dataset.setColLabel(i, statisticsDatasetDisplay
                                       .getResultName(typeAxisString, count.getValue(),
                                                      dso, getDsoType(dso), -1, context));
            dataset.setColLabelAttr(i, statisticsDatasetDisplay
                                           .getAttributes(count.getValue(),
                                                          dso, getDsoType(dso), context));
            dataset.addValueToMatrix(0, i, count.getCount());
        }
        return dataset;
    }

    private boolean isNotSiteObject(DSpaceObject dso) {
        return dso.getType() != Constants.SITE;
    }

    private Integer getDsoType(DSpaceObject dso) {
        return dsoType != null ? dsoType : dso.getType();
    }

    public void setDsoType(Integer dsoType) {
        this.dsoType = dsoType;
    }

}
