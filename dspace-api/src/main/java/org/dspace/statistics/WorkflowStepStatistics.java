/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import java.util.Collections;
import java.util.Map;

/**
 * Model a single WORKFLOW statistic entry related to a specific step.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public final class WorkflowStepStatistics {

    private final String stepName;

    private final long count;

    private final Map<String, Long> actionCounts;

    /**
     * Creates a new workflow statistic entry related to a specific step with no action counts.
     *
     * @param stepName the name of the workflow step
     * @param count    the total number of actions performed on the step
     */
    public WorkflowStepStatistics(String stepName, long count) {
        this.stepName = stepName;
        this.count = count;
        this.actionCounts = Map.of();
    }

    /**
     * Creates a new workflow statistic entry related to a specific step.
     *
     * @param stepName     the name of the workflow step
     * @param count        the total number of actions performed on the step
     * @param actionCounts the count of performed actions grouped by action name
     */
    public WorkflowStepStatistics(String stepName, long count, Map<String, Long> actionCounts) {
        this.stepName = stepName;
        this.count = count;
        this.actionCounts = Collections.unmodifiableMap(actionCounts);
    }

    /**
     * Returns the name of the workflow step.
     *
     * @return the step name
     */
    public String getStepName() {
        return stepName;
    }

    /**
     * Returns the total number of actions performed on the step.
     *
     * @return the action count
     */
    public long getCount() {
        return count;
    }

    /**
     * Returns the count of performed actions grouped by action name.
     *
     * @return the action counts
     */
    public Map<String, Long> getActionCounts() {
        return actionCounts;
    }

}
