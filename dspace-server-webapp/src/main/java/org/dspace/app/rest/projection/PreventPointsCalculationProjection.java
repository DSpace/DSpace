/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.projection;

/**
 * A marker projection that, when specified by the client via {@code ?projection=usageReportPoints},
 * signals that the points calculation should be skipped when retrieving usage report statistics.
 * This can improve performance when only report headers are needed and the full data points
 * are not required.
 *
 * @see org.dspace.app.rest.repository.StatisticsRestRepository
 */
public class PreventPointsCalculationProjection extends AbstractProjection {

    public final static String NAME = "usageReportPoints";

    @Override
    public String getName() {
        return NAME;
    }

}
