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

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.rest.model.UsageReportPointDsoTotalVisitsRest;
import org.dspace.app.rest.model.UsageReportRest;
import org.dspace.app.rest.utils.UsageReportUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.statistics.Dataset;
import org.dspace.statistics.content.DatasetDSpaceObjectGenerator;
import org.dspace.statistics.content.StatisticsDataVisits;
import org.dspace.statistics.content.StatisticsListing;

/**
 * This report generator provides TotalVisit on a DSO
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class TotalVisitGenerator implements UsageReportGenerator {

    /**
     * Create a stat usage report for the amount of TotalVisit on a DSO, containing one point with the amount of
     * views on the DSO in. If there are no views on the DSO this point contains views=0.
     *
     * @param context DSpace context
     * @param dso     DSO we want usage report with TotalVisits on the DSO
     * @return Rest object containing the TotalVisits usage report of the given DSO
     */
    public UsageReportRest createUsageReport(Context context, DSpaceObject dso) {
        Dataset dataset;
        try {
            dataset = this.getDSOStatsDataset(context, dso, 1, dso.getType());
        } catch (SQLException | IOException | ParseException | SolrServerException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        UsageReportRest usageReportRest = new UsageReportRest();
        UsageReportPointDsoTotalVisitsRest totalVisitPoint = new UsageReportPointDsoTotalVisitsRest();
        totalVisitPoint.setType(StringUtils.substringAfterLast(dso.getClass().getName().toLowerCase(), "."));
        totalVisitPoint.setId(dso.getID().toString());
        if (dataset.getColLabels().size() > 0) {
            totalVisitPoint.setLabel(dso.getName());
            totalVisitPoint.addValue("views", Integer.valueOf(dataset.getMatrix()[0][0]));
        } else {
            totalVisitPoint.setLabel(dso.getName());
            totalVisitPoint.addValue("views", 0);
        }

        usageReportRest.addPoint(totalVisitPoint);
        return usageReportRest;
    }

    @Override
    public String getReportType() {
        return UsageReportUtils.TOTAL_VISITS_REPORT_ID;
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
    Dataset getDSOStatsDataset(Context context, DSpaceObject dso, int facetMinCount, int dsoType)
        throws SQLException, IOException, ParseException, SolrServerException {
        StatisticsListing statsList = new StatisticsListing(new StatisticsDataVisits(dso));
        DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
        dsoAxis.addDsoChild(dsoType, 10, false, -1);
        statsList.addDatasetGenerator(dsoAxis);
        return statsList.getDataset(context, facetMinCount);
    }
}
