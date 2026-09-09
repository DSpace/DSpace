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
import org.dspace.app.rest.model.UsageReportPointDsoTotalVisitsRest;
import org.dspace.app.rest.model.UsageReportRest;
import org.dspace.app.rest.utils.UsageReportUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.statistics.Dataset;

/**
 * This report generator provides download data
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class TotalDownloadsGenerator extends TotalVisitGenerator {
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
    public UsageReportRest createUsageReport(Context context, DSpaceObject dso, String startDate, String endDate) {
        if (dso instanceof org.dspace.content.Bitstream) {
            return super.createUsageReport(context, dso, startDate, endDate);
        }

        if (dso instanceof org.dspace.content.Item) {
            Dataset dataset;
            try {
                dataset = this.getDSOStatsDataset(context, dso, Constants.BITSTREAM, startDate, endDate);
            } catch (SQLException | IOException | ParseException | SolrServerException e) {
                throw new RuntimeException(e.getMessage(), e);
            }

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

    @Override
    public String getReportType() {
        return UsageReportUtils.TOTAL_DOWNLOADS_REPORT_ID;
    }
}
