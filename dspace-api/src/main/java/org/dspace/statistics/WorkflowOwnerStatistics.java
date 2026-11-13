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

import org.dspace.eperson.EPerson;

/**
 * Model a single WORKFLOW statistic entry related to a specific owner.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public final class WorkflowOwnerStatistics {

    private final EPerson owner;

    private final long count;

    private final Map<String, Long> actionCounts;

    /**
     * Creates a new workflow statistic entry related to a specific owner.
     *
     * @param owner        the owner the statistic refers to
     * @param count        the total number of workflow actions performed by the owner
     * @param actionCounts the count of performed actions grouped by action name
     */
    public WorkflowOwnerStatistics(EPerson owner, long count, Map<String, Long> actionCounts) {
        this.owner = owner;
        this.count = count;
        this.actionCounts = Collections.unmodifiableMap(actionCounts);
    }

    /**
     * Returns the owner related to this statistic entry.
     *
     * @return the owner
     */
    public EPerson getOwner() {
        return owner;
    }

    /**
     * Returns the total number of workflow actions performed by the owner.
     *
     * @return the action count
     */
    public long getCount() {
        return count;
    }

    /**
     * Returns the full name of the owner.
     *
     * @return the owner full name
     */
    public String getOwnerName() {
        return owner.getFullName();
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
