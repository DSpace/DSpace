/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.dspace.core.Context;
import org.dspace.statistics.LoginStatistics;

/**
 * Service that handle the LOGIN statistics.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface LoginStatisticsService {

    /**
     * Find a single login statistic entry by eperson's id.
     *
     * @param  context the DSpace Context
     * @param  id      the id of the eperson to search for
     * @return         the login statistics, if present
     */
    Optional<LoginStatistics> find(Context context, String id);

    /**
     * Find all the LOGIN statistics in the given range.
     *
     * @param  context   the DSpace Context
     * @param  startDate the start date, if null no lower bound is applied
     * @param  endDate   the end date, if null no upper bound is applied
     * @param  limit     the limit to apply to the returned login statistics. If the
     *                   provided limit is equals or minor than 0, no limit is
     *                   applied
     * @return           the found statistics
     */
    List<LoginStatistics> findByDateRange(Context context, Date startDate, Date endDate, int limit);
}
