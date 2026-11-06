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
import java.util.UUID;

import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.statistics.WorkflowOwnerStatistics;
import org.dspace.statistics.WorkflowStepStatistics;

/**
 * Service that handle the WORKFLOW statistics.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface WorkflowStatisticsService {

    /**
     * Find the workflow step statistics related to the step with the given name. If
     * no step exists with the provided name or does not have statistics, an empty
     * Optional is returned.
     * 
     * @param  context  the DSpace Context
     * @param  stepName the step name
     * @return          the found statistics
     */
    Optional<WorkflowStepStatistics> findStepStatistics(Context context, String stepName);

    /**
     * Find the workflow step statistics related to the owner with the given id. If
     * no owner (eperson or group) exists with the provided id or does not have
     * statistics, an empty Optional is returned.
     * 
     * @param  context  the DSpace Context
     * @return          the found statistics
     */
    Optional<WorkflowOwnerStatistics> findOwnerStatistics(Context context, UUID ownerId);

    /**
     * Returns the current count of all the workflow steps.
     *
     * @param  context the DSpace Context
     * @param  limit   the number of workflow step statistics to return
     * @return         the found statistics
     */
    List<WorkflowStepStatistics> findCurrentWorkflows(Context context, int limit);

    /**
     * Find all the WORKFLOW statistics in the given range.
     *
     * @param  context    the DSpace Context
     * @param  startDate  the start date, if null no lower bound is applied
     * @param  endDate    the end date, if null no upper bound is applied
     * @param  collection the scope of the workflow item to search for. If null, no
     *                    scope is applied
     * @param  limit      the limit to apply to the returned workflow statistics. If
     *                    the provided limit is equals or minor than 0, no limit is
     *                    applied
     * @return            the found statistics
     */
    List<WorkflowStepStatistics> findByDateRange(Context context, Date startDate,
        Date endDate, Collection collection, int limit);

    /**
     * Find all the WORKFLOW owner statistics in the given range.
     *
     * @param  context    the DSpace Context
     * @param  startDate  the start date, if null no lower bound is applied
     * @param  endDate    the end date, if null no upper bound is applied
     * @param  collection the scope of the workflow item to search for. If null, no
     *                    scope is applied
     * @param  limit      the limit to apply to the returned workflow statistics. If
     *                    the provided limit is equals or minor than 0, no limit is
     *                    applied
     * @return            the found statistics
     */
    List<WorkflowOwnerStatistics> findOwnersByDateRange(Context context, Date startDate,
        Date endDate, Collection collection, int limit);
}
