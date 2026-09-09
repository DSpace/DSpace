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

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.rest.model.UsageReportRest;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

/**
 * This is the interface that each report generator must implements
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public interface UsageReportGenerator {
    /**
     * Creates the requested report for the specified dspace object.
     * If the report id or the object uuid is invalid, an exception is thrown.
     *
     * @param context  DSpace context
     * @param dso     DSpace object we want a stat usage report on
     * @return Rest object containing the stat usage report, see {@link UsageReportRest}
     */
    public UsageReportRest createUsageReport(Context context,
                                             DSpaceObject dso, String startDate, String endDate)
        throws SolrServerException, IOException, SQLException;

    /**
     *
     * @return the report type generated
     */
    public String getReportType();

    /**
     *
     * @return the suggested view mode to apply to the report
     */
    public String getViewMode();

}
