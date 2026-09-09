/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.rest.model.UsageReportCategoryRest;
import org.dspace.app.rest.model.UsageReportPointCityRest;
import org.dspace.app.rest.model.UsageReportPointCountryRest;
import org.dspace.app.rest.model.UsageReportPointDateRest;
import org.dspace.app.rest.model.UsageReportPointDsoTotalVisitsRest;
import org.dspace.app.rest.model.UsageReportRest;
import org.dspace.app.rest.statistics.StatisticsReportsConfiguration;
import org.dspace.app.rest.statistics.UsageReportGenerator;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Site;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.statistics.Dataset;
import org.dspace.statistics.content.DatasetDSpaceObjectGenerator;
import org.dspace.statistics.content.DatasetTimeGenerator;
import org.dspace.statistics.content.DatasetTypeGenerator;
import org.dspace.statistics.content.StatisticsDataVisits;
import org.dspace.statistics.content.StatisticsListing;
import org.dspace.statistics.content.StatisticsTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

/**
 * This is the Service dealing with the {@link UsageReportRest} logic
 *
 * @author Maria Verdonck (Atmire) on 08/06/2020
 */
@Component
public class UsageReportUtils {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private StatisticsReportsConfiguration configuration;

    @Autowired
    private HandleService handleService;

    public static final String TOTAL_VISITS_REPORT_ID = "TotalVisits";
    public static final String TOP_ITEMS_REPORT_ID = "TopItems";
    public static final String TOTAL_VISITS_REPORT_ID_RELATION_PERSON_RESEARCHOUTPUTS
        = "TotalVisitsPersonResearchoutputs";
    public static final String TOTAL_VISITS_REPORT_ID_RELATION_PERSON_PROJECTS
        = "TotalVisitsPersonProjects";
    public static final String TOTAL_VISITS_REPORT_ID_RELATION_ORGUNIT_RP_RESEARCHOUTPUTS
        = "TotalVisitsOrgUnitRppublications";
    public static final String TOTAL_VISITS_REPORT_ID_RELATION_ORGUNIT_PROJECTS
        = "TotalVisitsOrgUnitRpprojects";
    public static final String TOP_ITEMS_REPORT_RELATION_ORGUNIT_RP_RESEARCHOUTPUTS = "TopItemsOrgUnitRppublications";
    public static final String TOTAL_DOWNLOADS_REPORT_ID_RELATION_PERSON_RESEARCHOUTPUTS
        = "TotalDownloadsPersonResearchoutputs";
    public static final String TOTAL_DOWNLOADS_REPORT_ID_RELATION_ORGUNIT_RP_RESEARCHOUTPUTS
        = "TotalDownloadsOrgUnitRppublications";
    public static final String TOTAL_VISITS_PER_MONTH_REPORT_ID_RELATION_PERSON_RESEARCHOUTPUTS
        = "TotalVisitPerPeriodPersonResearchoutputs";
    public static final String TOTAL_VISITS_PER_MONTH_REPORT_ID_RELATION_PERSON_PROJECTS
        = "TotalVisitPerPeriodPersonProjects";
    public static final String TOTAL_VISITS_PER_MONTH_REPORT_ID_RELATION_ORGUNIT_RP_RESEARCHOUTPUTS
        = "TotalVisitPerPeriodOrgUnitRppublications";
    public static final String TOTAL_VISITS_PER_MONTH_REPORT_ID = "TotalVisitsPerMonth";
    public static final String TOTAL_DOWNLOADS_REPORT_ID = "TotalDownloads";
    public static final String TOP_COUNTRIES_REPORT_ID = "TopCountries";
    public static final String TOP_COUNTRIES_REPORT_ID_RELATION_PERSON_RESEARCHOUTPUTS
        = "TopCountriesPersonResearchoutputs";
    public static final String TOP_COUNTRIES_REPORT_ID_RELATION_PERSON_PROJECTS
        = "TopCountriesPersonProjects";
    public static final String TOP_COUNTRIES_REPORT_ID_RELATION_ORGUNIT_RP_RESEARCHOUTPUTS
        = "TopCountriesOrgUnitRppublications";
    public static final String TOP_CITIES_REPORT_ID = "TopCities";
    public static final String TOP_CONTINENTS_REPORT_ID = "TopContinents";
    public static final String TOP_CATEGORIES_REPORT_ID = "TopCategories";
    public static final String TOP_CITIES_REPORT_ID_RELATION_PERSON_PROJECTS =
        "TopCitiesPersonProjects";
    public static final String TOP_CITIES_REPORT_ID_RELATION_PERSON_RESEARCHOUTPUTS
        = "TopCitiesPersonResearchoutputs";
    public static final String TOP_CITIES_REPORT_ID_RELATION_ORGUNIT_RP_RESEARCHOUTPUTS
        = "TopCitiesOrgUnitRppublications";
    public static final String TOTAL_VISITS_TOTAL_DOWNLOADS = "TotalVisitsAndDownloads";
    public static final String TOTAL_VISITS_TOTAL_DOWNLOADS_RELATION_PERSON_RESEARCHOUTPUTS
        = "TotalViewsDownloadsPersonResearchoutputs";
    public static final String TOTAL_VISITS_TOTAL_DOWNLOADS_RELATION_ORGUNIT_RP_RESEARCHOUTPUTS
        = "TotalViewsDownloadsOrgUnitRppublications";

    public static final String TOP_DOWNLOAD_CONTINENTS_REPORT_ID = "TopDownloadsContinents";
    public static final String TOP_DOWNLOAD_COUNTRIES_REPORT_ID = "TopDownloadsCountries";
    public static final String TOP_DOWNLOAD_CITIES_REPORT_ID = "TopDownloadsCities";
    public static final String TOTAL_DOWNLOAD_PER_MONTH_REPORT_ID = "TotalDownloadsPerMonth";
    public static final String TOP_ITEMS_CITIES_REPORT_ID = "TopItemsCities";
    public static final String TOP_ITEMS_CONTINENTS_REPORT_ID = "TopItemsContinents";
    public static final String TOP_ITEMS_COUNTRIES_REPORT_ID = "TopItemsCountries";
    public static final String TOP_ITEMS_CATEGORIES_REPORT_ID = "TopItemsCategories";
    public static final String TOTAL_ITEMS_VISITS_REPORT_ID = "TotalItemsVisits";
    public static final String TOTAL_ITEMS_VISITS_PER_MONTH_REPORT_ID = "TotalItemsVisitsPerMonth";

    /**
     * Get list of usage reports that are applicable to the DSO (of given UUID)
     *
     * @param context   DSpace context
     * @param dso       DSpaceObject we want all available usage reports of
     * @param category  if not null, limit the reports to the ones included in the specified category
     * @return List of usage reports, applicable to the given DSO
     */
    public List<UsageReportRest> getUsageReportsOfDSO(Context context,
                                                      DSpaceObject dso, String category,
                                                      String startDate, String endDate)
        throws SQLException, ParseException, SolrServerException, IOException {
        List<UsageReportCategoryRest> categories = configuration.getCategories(dso);
        List<String> reportIds = new ArrayList();
        List<UsageReportRest> reports = new ArrayList();
        for (UsageReportCategoryRest cat : categories) {
            if (category == null || StringUtils.equals(cat.getId(), category)) {
                for (Entry<String, UsageReportGenerator> entry : cat.getReports().entrySet()) {
                    if (!reportIds.contains(entry.getKey())) {
                        reportIds.add(entry.getKey());
                        reports.add(createUsageReport(context, dso, entry.getKey(), startDate, endDate));
                    }
                }
            }
        }
        return reports;
    }

    private List<String> getReports(Context context, DSpaceObject dso, String category) {
        List<String> reports = new ArrayList();
        if (dso instanceof Site) {
            reports.add(TOTAL_VISITS_REPORT_ID);
        } else {
            reports.add(TOTAL_VISITS_REPORT_ID);
            reports.add(TOTAL_VISITS_PER_MONTH_REPORT_ID);
            reports.add(TOP_COUNTRIES_REPORT_ID);
            reports.add(TOP_CITIES_REPORT_ID);
        }
        if (dso instanceof Item || dso instanceof Bitstream) {
            reports.add(TOTAL_DOWNLOADS_REPORT_ID);
            reports.add(TOTAL_VISITS_TOTAL_DOWNLOADS);
        }
        return reports;
    }

    /**
     * Get list of usage reports categories that are applicable to the DSO (of given UUID)
     *
     * @param context the DSpace Context
     * @param dso     DSpaceObject we want all available usage reports categories of
     *
     * @return List of usage reports categories, applicable to the given DSO
     */
    public List<UsageReportCategoryRest> getUsageReportsCategoriesOfDSO(Context context, DSpaceObject dso)
            throws SQLException, ParseException, SolrServerException, IOException {
        return configuration.getCategories(dso);
    }

    /**
     * Creates the stat different stat usage report based on the report id.
     * If the report id or the object uuid is invalid, an exception is thrown.
     *
     * @param context  DSpace context
     * @param dso     DSpace object we want a stat usage report on
     * @param reportId Type of usage report requested
     * @return Rest object containing the stat usage report, see {@link UsageReportRest}
     */
    public UsageReportRest createUsageReport(Context context, DSpaceObject dso, String reportId)
            throws ParseException, SolrServerException, IOException, SQLException {
        UsageReportGenerator generator = configuration.getReportGenerator(dso, reportId);
        if (generator != null) {
            UsageReportRest usageReportRest = generator.createUsageReport(context, dso, null, null);
            usageReportRest.setId(dso.getID() + "_" + reportId);
            usageReportRest.setReportType(generator.getReportType());
            usageReportRest.setViewMode(generator.getViewMode());
            return usageReportRest;
        } else {
            throw new ResourceNotFoundException("The given report id can't be resolved: " + reportId + "; "
                    + "available reports: TotalVisits, TotalVisitsPerMonth, "
                    + "TotalDownloads, TopCountries, TopCities");
        }
    }

    /**
     * Create stat usage report of the items most popular over entire site
     *
     * @param context DSpace context
     * @return Usage report with top most popular items
     */
    private UsageReportRest resolveGlobalUsageReport(Context context)
        throws SQLException, IOException, ParseException, SolrServerException {
        int topItemsLimit = configurationService.getIntProperty("usage-statistics.topItemsLimit", 10);

        StatisticsListing statListing = new StatisticsListing(
            new StatisticsDataVisits());

        // Adding a new generator for our top n items without a name length delimiter
        DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
        dsoAxis.addDsoChild(Constants.ITEM, topItemsLimit, false, -1);
        statListing.addDatasetGenerator(dsoAxis);

        Dataset dataset = statListing.getDataset(context, 1);

        UsageReportRest usageReportRest = new UsageReportRest();
        for (int i = 0; i < dataset.getColLabels().size(); i++) {
            UsageReportPointDsoTotalVisitsRest totalVisitPoint = new UsageReportPointDsoTotalVisitsRest();
            totalVisitPoint.setType("item");
            String urlOfItem = dataset.getColLabelsAttrs().get(i).get("url");
            if (urlOfItem != null) {
                String handle = StringUtils.substringAfterLast(urlOfItem, "handle/");
                if (handle != null) {
                    DSpaceObject dso = handleService.resolveToObject(context, handle);
                    totalVisitPoint.setId(dso != null ? dso.getID().toString() : urlOfItem);
                    totalVisitPoint.setLabel(dso != null ? dso.getName() : urlOfItem);
                    totalVisitPoint.addValue("views", Integer.valueOf(dataset.getMatrix()[0][i]));
                    usageReportRest.addPoint(totalVisitPoint);
                }
            }
        }
        usageReportRest.setReportType(TOTAL_VISITS_REPORT_ID);
        return usageReportRest;
    }

    public UsageReportRest createUsageReport(Context context, DSpaceObject dso, String reportId,
                                             String startDate, String endDate)
            throws ParseException, SolrServerException, IOException, SQLException {
        UsageReportGenerator generator = configuration.getReportGenerator(dso, reportId);
        if (generator != null) {
            UsageReportRest usageReportRest = generator.createUsageReport(context, dso, startDate, endDate);
            usageReportRest.setId(dso.getID() + "_" + reportId);
            usageReportRest.setReportType(generator.getReportType());
            usageReportRest.setViewMode(generator.getViewMode());
            return usageReportRest;
        } else {
            throw new ResourceNotFoundException("The given report id can't be resolved: " + reportId + "; "
                    + "available reports: TotalVisits, TotalVisitsPerMonth, "
                    + "TotalDownloads, TopCountries, TopCities");
        }
    }
    public boolean categoryExists(DSpaceObject dso, String category) {
        List<UsageReportCategoryRest> categories = configuration.getCategories(dso);
        if (categories != null) {
            return categories.stream().anyMatch(x -> StringUtils.equals(category, x.getId()));
        }
        return false;
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
        Dataset dataset = this.getDSOStatsDataset(context, dso, 1, dso.getType());

        UsageReportRest usageReportRest = new UsageReportRest();
        UsageReportPointDsoTotalVisitsRest totalVisitPoint = new UsageReportPointDsoTotalVisitsRest();
        totalVisitPoint.setType(StringUtils.substringAfterLast(dso.getClass().getName().toLowerCase(), "."));
        totalVisitPoint.setId(dso.getID().toString());
        if (!dataset.getColLabels().isEmpty()) {
            totalVisitPoint.setLabel(dso.getName());
            totalVisitPoint.addValue("views", Integer.valueOf(dataset.getMatrix()[0][0]));
        } else {
            totalVisitPoint.setLabel(dso.getName());
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
        String startDateInterval =
            configurationService.getProperty("usage-statistics.startDateInterval", "-6");
        String endDateInterval =
            configurationService.getProperty("usage-statistics.endDateInterval", "+1");

        StatisticsTable statisticsTable = new StatisticsTable(new StatisticsDataVisits(dso));
        DatasetTimeGenerator timeAxis = new DatasetTimeGenerator();
        timeAxis.setDateInterval("month", startDateInterval, endDateInterval);
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

                totalDownloadsPoint.setId(dataset.getColLabelsAttrs().get(i).get("id"));
                totalDownloadsPoint.setLabel(dataset.getColLabels().get(i));

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
        int topCountriesLimit =
            configurationService.getIntProperty("usage-statistics.topCountriesLimit", 100);

        Dataset dataset = this.getTypeStatsDataset(context, dso, "countryCode", topCountriesLimit, 1);

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
        int topCitiesLimit =
            configurationService.getIntProperty("usage-statistics.topCitiesLimit", 100);

        Dataset dataset = this.getTypeStatsDataset(context, dso, "city", topCitiesLimit, 1);

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
     * @param typeAxisMax    Maximum amount of results to return in the dataset
     * @param facetMinCount  Minimum amount of results on a facet data point for it to be added to dataset
     * @return Stats dataset with the given type on the axis, of the given DSO and with given facetMinCount
     */
    private Dataset getTypeStatsDataset(Context context, DSpaceObject dso, String typeAxisString, int typeAxisMax,
                                        int facetMinCount)
        throws SQLException, IOException, ParseException, SolrServerException {
        StatisticsListing statListing = new StatisticsListing(new StatisticsDataVisits(dso));
        DatasetTypeGenerator typeAxis = new DatasetTypeGenerator();
        typeAxis.setType(typeAxisString);
        typeAxis.setMax(typeAxisMax);
        statListing.addDatasetGenerator(typeAxis);
        return statListing.getDataset(context, facetMinCount);
    }
}
