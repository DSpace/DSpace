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
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.UsageReportPointCityRest;
import org.dspace.app.rest.model.UsageReportPointCountryRest;
import org.dspace.app.rest.model.UsageReportPointDateRest;
import org.dspace.app.rest.model.UsageReportPointDsoTotalVisitsRest;
import org.dspace.app.rest.model.UsageReportRest;
import org.dspace.app.rest.utils.DSpaceObjectUtils;
import org.dspace.content.DSpaceObject;
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
     * TODO
     *
     * @param context
     * @param uuid
     * @param reportId
     * @return
     * @throws ParseException
     * @throws SolrServerException
     * @throws IOException
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
                    throw new RepositoryMethodNotImplementedException("TODO", "TotalDownloads");
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

    private UsageReportRest resolveTotalVisits(Context context, DSpaceObject dso)
        throws SQLException, IOException, ParseException, SolrServerException {
        StatisticsListing statListing = new StatisticsListing(new StatisticsDataVisits(dso));
        DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
        dsoAxis.addDsoChild(dso.getType(), 10, false, -1);
        statListing.addDatasetGenerator(dsoAxis);
        Dataset dataset = statListing.getDataset(context);

        UsageReportRest usageReportRest = new UsageReportRest();
        for (int i = 0; i < dataset.getColLabels().size(); i++) {
            UsageReportPointDsoTotalVisitsRest totalVisitPoint = new UsageReportPointDsoTotalVisitsRest();
            totalVisitPoint.setType(StringUtils.substringAfterLast(dso.getClass().getName().toLowerCase(), "."));
            totalVisitPoint.setId(dso.getID().toString());
            totalVisitPoint.addValue("views", Integer.valueOf(dataset.getMatrix()[0][i]));
            usageReportRest.addPoint(totalVisitPoint);
        }
        return usageReportRest;
    }

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
        Dataset dataset = statisticsTable.getDataset(context);

        UsageReportRest usageReportRest = new UsageReportRest();
        for (int i = 0; i < dataset.getColLabels().size(); i++) {
            UsageReportPointDateRest monthPoint = new UsageReportPointDateRest();
            monthPoint.setId(dataset.getColLabels().get(i));
            monthPoint.addValue("views", Integer.valueOf(dataset.getMatrix()[0][i]));
            usageReportRest.addPoint(monthPoint);
        }
        return usageReportRest;
    }

    private UsageReportRest resolveTopCountries(Context context, DSpaceObject dso)
        throws SQLException, IOException, ParseException, SolrServerException {
        Dataset dataset = this.getTypeStatsDataset(context, dso, "countryCode");

        UsageReportRest usageReportRest = new UsageReportRest();
        for (int i = 0; i < dataset.getColLabels().size(); i++) {
            UsageReportPointCountryRest countryPoint = new UsageReportPointCountryRest();
            countryPoint.setLabel(dataset.getColLabels().get(i));
            countryPoint.addValue("views", Integer.valueOf(dataset.getMatrix()[0][i]));
            usageReportRest.addPoint(countryPoint);
        }
        return usageReportRest;
    }

    private UsageReportRest resolveTopCities(Context context, DSpaceObject dso)
        throws SQLException, IOException, ParseException, SolrServerException {
        Dataset dataset = this.getTypeStatsDataset(context, dso, "city");

        UsageReportRest usageReportRest = new UsageReportRest();
        for (int i = 0; i < dataset.getColLabels().size(); i++) {
            UsageReportPointCityRest cityPoint = new UsageReportPointCityRest();
            cityPoint.setId(dataset.getColLabels().get(i));
            cityPoint.addValue("views", Integer.valueOf(dataset.getMatrix()[0][i]));
            usageReportRest.addPoint(cityPoint);
        }
        return usageReportRest;
    }

    private Dataset getTypeStatsDataset(Context context, DSpaceObject dso, String typeAxisString)
        throws SQLException, IOException, ParseException, SolrServerException {
        StatisticsListing statListing = new StatisticsListing(new StatisticsDataVisits(dso));
        DatasetTypeGenerator typeAxis = new DatasetTypeGenerator();
        typeAxis.setType(typeAxisString);
        // TODO make max nr of top countries/cities a request para? Must be set
        typeAxis.setMax(100);
        statListing.addDatasetGenerator(typeAxis);
        return statListing.getDataset(context);
    }
}
