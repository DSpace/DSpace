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
import org.dspace.content.Site;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.dspace.statistics.Dataset;
import org.dspace.statistics.content.DatasetDSpaceObjectGenerator;
import org.dspace.statistics.content.StatisticsDataVisits;
import org.dspace.statistics.content.StatisticsListing;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This report generator provides usage data by top children
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class TopItemsGenerator implements UsageReportGenerator {

    @Autowired
    private HandleService handleService;

    /**
     * Create stat usage report of the items most popular over the entire site or a
     * specific community, collection
     *
     * @param context DSpace context
     * @param dso     DSO we want the stats dataset of
     * @return Usage report with top most popular items
     */
    public UsageReportRest createUsageReport(Context context, DSpaceObject root) {
        StatisticsListing statListing = new StatisticsListing(
                new StatisticsDataVisits(root instanceof Site ? null : root));
        // Adding a new generator for our top 10 items without a name length delimiter
        DatasetDSpaceObjectGenerator dsoAxis = new DatasetDSpaceObjectGenerator();
        // TODO make max nr of top items (views wise)? Must be set
        dsoAxis.addDsoChild(Constants.ITEM, 10, false, -1);
        statListing.addDatasetGenerator(dsoAxis);
        Dataset dataset;
        try {
            dataset = statListing.getDataset(context, 1);
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
            return usageReportRest;
        } catch (SQLException | SolrServerException | IOException | ParseException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public String getReportType() {
        return UsageReportUtils.TOTAL_VISITS_REPORT_ID;
    }
}
