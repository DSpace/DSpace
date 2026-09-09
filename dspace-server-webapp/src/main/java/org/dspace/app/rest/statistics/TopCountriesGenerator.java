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
import org.dspace.app.rest.model.UsageReportPointCountryRest;
import org.dspace.app.rest.model.UsageReportRest;
import org.dspace.app.rest.utils.UsageReportUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.statistics.Dataset;
import org.dspace.statistics.util.LocationUtils;

/**
 * This report generator provides the TopCountries that have visited the given DSO.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class TopCountriesGenerator extends AbstractTopSolrStatsFieldGenerator {

    /**
     * Create a stat usage report for the TopCountries that have visited the given DSO. If there have been no visits, or
     * no visits with a valid Geolite determined country (based on IP), this report contains an empty list of points=[].
     * The list of points is limited to the top 100 countries, and each point contains the country name, its iso code
     * and the amount of views on the given DSO from that country.
     *
     * @param context DSpace context
     * @param dso     DSO we want usage report of the TopCountries on the given DSO
     * @return Rest object containing the TopCountries usage report on the given DSO
     */
    public UsageReportRest createUsageReport(Context context, DSpaceObject dso, String startDate, String endDate) {
        Dataset dataset;
        try {
            dataset = this.getTypeStatsDataset(context, dso, "countryCode", startDate, endDate);
        } catch (SQLException | IOException | ParseException | SolrServerException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        UsageReportRest usageReportRest = new UsageReportRest();
        for (int i = 0; i < dataset.getColLabels().size(); i++) {
            UsageReportPointCountryRest countryPoint = new UsageReportPointCountryRest();
            final String countryCode = dataset.getColLabels().get(i);
            countryPoint.setIdAndLabel(countryCode,
                                       LocationUtils.getCountryName(countryCode,
                                                                    context.getCurrentLocale()));
            countryPoint.addValue("views", Integer.valueOf(dataset.getMatrix()[0][i]));
            usageReportRest.addPoint(countryPoint);
        }
        return usageReportRest;
    }


    @Override
    public String getReportType() {
        return UsageReportUtils.TOP_COUNTRIES_REPORT_ID;
    }
}
