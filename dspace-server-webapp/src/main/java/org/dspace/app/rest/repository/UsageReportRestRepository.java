/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.UsageReportPointCityRest;
import org.dspace.app.rest.model.UsageReportPointCountryRest;
import org.dspace.app.rest.model.UsageReportPointDateRest;
import org.dspace.app.rest.model.UsageReportPointDsoTotalVisitsRest;
import org.dspace.app.rest.model.UsageReportRest;
import org.dspace.app.rest.utils.DSpaceObjectUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.statistics.Dataset;
import org.dspace.statistics.content.DatasetDSpaceObjectGenerator;
import org.dspace.statistics.content.DatasetTimeGenerator;
import org.dspace.statistics.content.DatasetTypeGenerator;
import org.dspace.statistics.content.StatisticsDataVisits;
import org.dspace.statistics.content.StatisticsListing;
import org.dspace.statistics.content.StatisticsTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the REST repository dealing with the {@link UsageReportRest} logic
 *
 * @author Maria Verdonck (Atmire) on 08/06/2020
 */
@Component(UsageReportRest.CATEGORY + "." + UsageReportRest.NAME)
public class UsageReportRestRepository extends AbstractDSpaceRestRepository {

    @Autowired
    private DSpaceObjectUtils dspaceObjectUtil;

    /**
     * Creates the stat different stat usage report based on the report id.
     * If the report id or the object uuid is invalid, an exception is thrown.
     *
     * @param context  DSpace context
     * @param uuid     DSpace object UUID we want a stat usage report on
     * @param reportId Type of usage report requested
     * @return Rest object containing the stat usage report, see {@link UsageReportRest}
     */
    public UsageReportRest createUsageReport(Context context, UUID uuid, String reportId)
        throws ParseException, SolrServerException, IOException {
        try {
            DSpaceObject dso = dspaceObjectUtil.findDSpaceObject(context, uuid);
            UsageReportRest usageReportRest;
            switch (reportId) {
                case "TotalVisits":
                    usageReportRest = resolveTotalVisits(context, dso);
                    usageReportRest.setReportType("TotalVisits");
                    break;
                case "TotalVisitsPerMonth":
                    usageReportRest = resolveTotalVisitsPerMonth(context, dso);
                    usageReportRest.setReportType("TotalVisitsPerMonth");
                    break;
                case "TotalDownloads":
                    usageReportRest = resolveTotalDownloads(context, dso);
                    usageReportRest.setReportType("TotalDownloads");
                    break;
                case "TopCountries":
                    usageReportRest = resolveTopCountries(context, dso);
                    usageReportRest.setReportType("TopCountries");
                    break;
                case "TopCities":
                    usageReportRest = resolveTopCities(context, dso);
                    usageReportRest.setReportType("TopCities");
                    break;
                default:
                    throw new DSpaceBadRequestException("The given report id can't be resolved: " + reportId + "; " +
                                                        "available reports: TotalVisits, TotalVisitsPerMonth, " +
                                                        "TotalDownloads, TopCountries, TopCities");
            }
            return usageReportRest;
        } catch (SQLException e) {
            throw new DSpaceBadRequestException("The given object uuid can't be resolved to an object: " + uuid);
        }
    }

    /**
     * Create a stat usage report for the amount of TotalVisit on a DSO, containing one point with the amount of
     * views on the DSO in. If there are no views on the DSO this point contains views=0.
     *
     * @param context DSpace context
     * @param dso     DSO we want usage report with TotalVisits on the DSO
     * @return Rest object containing the TotalVisits usage report of the given DSO
     */
    private UsageReportRest resolveTotalVisits(Context context, DSpaceObject dso)
        throws SQLException, IOException, ParseException, SolrServerException {
        Dataset dataset = this.getDSOStatsDataset(context, dso, 0, dso.getType());

        UsageReportRest usageReportRest = new UsageReportRest();
        UsageReportPointDsoTotalVisitsRest totalVisitPoint = new UsageReportPointDsoTotalVisitsRest();
        totalVisitPoint.setType(StringUtils.substringAfterLast(dso.getClass().getName().toLowerCase(), "."));
        totalVisitPoint.setId(dso.getID().toString());
        if (dataset.getColLabels().size() > 0) {
            totalVisitPoint.addValue("views", Integer.valueOf(dataset.getMatrix()[0][0]));
        } else {
            totalVisitPoint.addValue("views", 0);
        }

        usageReportRest.addPoint(totalVisitPoint);
        return usageReportRest;
    }

    /**
     * Create a stat usage report for the amount of TotalVisitPerMonth on a DSO, containing one point for each month
     * with the views on that DSO in that month with the range -6 months to now. If there are no views on the DSO
     * in a month, the point on that month contains views=0.
     *
     * @param context DSpace context
     * @param dso     DSO we want usage report with TotalVisitsPerMonth to the DSO
     * @return Rest object containing the TotalVisits usage report on the given DSO
     */
    private UsageReportRest resolveTotalVisitsPerMonth(Context context, DSpaceObject dso)
        throws SQLException, IOException, ParseException, SolrServerException {
        StatisticsTable statisticsTable = new StatisticsTable(new StatisticsDataVisits(dso));
        DatasetTimeGenerator timeAxis = new DatasetTimeGenerator();
        // TODO month start and end as request para?
        timeAxis.setDateInterval("month", "-6", "+1");
        statisticsTable.addDatasetGenerator(timeAxis);
        DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
        dsoAxis.addDsoChild(dso.getType(), 10, false, -1);
        statisticsTable.addDatasetGenerator(dsoAxis);
        Dataset dataset = statisticsTable.getDataset(context, 0);

        UsageReportRest usageReportRest = new UsageReportRest();
        for (int i = 0; i < dataset.getColLabels().size(); i++) {
            UsageReportPointDateRest monthPoint = new UsageReportPointDateRest();
            monthPoint.setId(dataset.getColLabels().get(i));
            monthPoint.addValue("views", Integer.valueOf(dataset.getMatrix()[0][i]));
            usageReportRest.addPoint(monthPoint);
        }
        return usageReportRest;
    }

    /**
     * Create a stat usage report for the amount of TotalDownloads on the files of an Item or of a Bitstream,
     * containing a point for each bitstream of the item that has been visited at least once or one point for the
     * bitstream containing the amount of times that bitstream has been visited (even if 0)
     * If the item has no bitstreams, or no bitstreams that have ever been downloaded/visited, then it contains an
     * empty list of points=[]
     * If the given UUID is for DSO that is neither a Bitstream nor an Item, an exception is thrown.
     *
     * @param context DSpace context
     * @param dso     Item/Bitstream we want usage report on with TotalDownloads of the Item's bitstreams or of the
     *                bitstream itself
     * @return Rest object containing the TotalDownloads usage report on the given Item/Bitstream
     */
    private UsageReportRest resolveTotalDownloads(Context context, DSpaceObject dso)
        throws SQLException, SolrServerException, ParseException, IOException {
        if (dso instanceof org.dspace.content.Bitstream) {
            return this.resolveTotalVisits(context, dso);
        }

        if (dso instanceof org.dspace.content.Item) {
            Dataset dataset = this.getDSOStatsDataset(context, dso, 1, Constants.BITSTREAM);

            UsageReportRest usageReportRest = new UsageReportRest();
            for (int i = 0; i < dataset.getColLabels().size(); i++) {
                UsageReportPointDsoTotalVisitsRest totalDownloadsPoint = new UsageReportPointDsoTotalVisitsRest();
                totalDownloadsPoint.setType("bitstream");
                totalDownloadsPoint.setId(dataset.getColLabels().get(i));
                totalDownloadsPoint.addValue("views", Integer.valueOf(dataset.getMatrix()[0][i]));
                usageReportRest.addPoint(totalDownloadsPoint);
            }
            return usageReportRest;
        }
        throw new IllegalArgumentException("TotalDownloads report only available for items and bitstreams");
    }

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
    private UsageReportRest resolveTopCountries(Context context, DSpaceObject dso)
        throws SQLException, IOException, ParseException, SolrServerException {
        Dataset dataset = this.getTypeStatsDataset(context, dso, "countryCode", 1);

        UsageReportRest usageReportRest = new UsageReportRest();
        for (int i = 0; i < dataset.getColLabels().size(); i++) {
            UsageReportPointCountryRest countryPoint = new UsageReportPointCountryRest();
            countryPoint.setLabel(dataset.getColLabels().get(i));
            countryPoint.addValue("views", Integer.valueOf(dataset.getMatrix()[0][i]));
            usageReportRest.addPoint(countryPoint);
        }
        return usageReportRest;
    }

    /**
     * Create a stat usage report for the TopCities that have visited the given DSO. If there have been no visits, or
     * no visits with a valid Geolite determined city (based on IP), this report contains an empty list of points=[].
     * The list of points is limited to the top 100 cities, and each point contains the city name and the amount of
     * views on the given DSO from that city.
     *
     * @param context DSpace context
     * @param dso     DSO we want usage report of the TopCities on the given DSO
     * @return Rest object containing the TopCities usage report on the given DSO
     */
    private UsageReportRest resolveTopCities(Context context, DSpaceObject dso)
        throws SQLException, IOException, ParseException, SolrServerException {
        Dataset dataset = this.getTypeStatsDataset(context, dso, "city", 1);

        UsageReportRest usageReportRest = new UsageReportRest();
        for (int i = 0; i < dataset.getColLabels().size(); i++) {
            UsageReportPointCityRest cityPoint = new UsageReportPointCityRest();
            cityPoint.setId(dataset.getColLabels().get(i));
            cityPoint.addValue("views", Integer.valueOf(dataset.getMatrix()[0][i]));
            usageReportRest.addPoint(cityPoint);
        }
        return usageReportRest;
    }

    /**
     * Retrieves the stats dataset of a given DSO, of given type, with a given facetMinCount limit (usually either 0
     * or 1, 0 if we want a data point even though the facet data point has 0 matching results).
     *
     * @param context       DSpace context
     * @param dso           DSO we want the stats dataset of
     * @param facetMinCount Minimum amount of results on a facet data point for it to be added to dataset
     * @param dsoType       Type of DSO we want the stats dataset of
     * @return Stats dataset with the given filters.
     */
    private Dataset getDSOStatsDataset(Context context, DSpaceObject dso, int facetMinCount, int dsoType)
        throws SQLException, IOException, ParseException, SolrServerException {
        StatisticsListing statsList = new StatisticsListing(new StatisticsDataVisits(dso));
        DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
        dsoAxis.addDsoChild(dsoType, 10, false, -1);
        statsList.addDatasetGenerator(dsoAxis);
        return statsList.getDataset(context, facetMinCount);
    }

    /**
     * Retrieves the stats dataset of a given dso, with a given axisType (example countryCode, city), which
     * corresponds to a solr field, and a given facetMinCount limit (usually either 0 or 1, 0 if we want a data point
     * even though the facet data point has 0 matching results).
     *
     * @param context        DSpace context
     * @param dso            DSO we want the stats dataset of
     * @param typeAxisString String of the type we want on the axis of the dataset (corresponds to solr field),
     *                       examples: countryCode, city
     * @param facetMinCount  Minimum amount of results on a facet data point for it to be added to dataset
     * @return Stats dataset with the given type on the axis, of the given DSO and with given facetMinCount
     */
    private Dataset getTypeStatsDataset(Context context, DSpaceObject dso, String typeAxisString, int facetMinCount)
        throws SQLException, IOException, ParseException, SolrServerException {
        StatisticsListing statListing = new StatisticsListing(new StatisticsDataVisits(dso));
        DatasetTypeGenerator typeAxis = new DatasetTypeGenerator();
        typeAxis.setType(typeAxisString);
        // TODO make max nr of top countries/cities a request para? Must be set
        typeAxis.setMax(100);
        statListing.addDatasetGenerator(typeAxis);
        return statListing.getDataset(context, facetMinCount);
    }
}
