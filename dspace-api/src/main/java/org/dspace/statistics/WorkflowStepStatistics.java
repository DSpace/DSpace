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

    public WorkflowStepStatistics(String stepName, long count) {
        this.stepName = stepName;
        this.count = count;
        this.actionCounts = Map.of();
    }

    public WorkflowStepStatistics(String stepName, long count, Map<String, Long> actionCounts) {
        this.stepName = stepName;
        this.count = count;
        this.actionCounts = Collections.unmodifiableMap(actionCounts);
    }

    public String getStepName() {
        return stepName;
    }

    public long getCount() {
        return count;
    }

    public Map<String, Long> getActionCounts() {
        return actionCounts;
    }

}
