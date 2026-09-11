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

    public WorkflowOwnerStatistics(EPerson owner, long count, Map<String, Long> actionCounts) {
        this.owner = owner;
        this.count = count;
        this.actionCounts = Collections.unmodifiableMap(actionCounts);
    }

    public EPerson getOwner() {
        return owner;
    }

    public long getCount() {
        return count;
    }

    public String getOwnerName() {
        return owner.getFullName();
    }

    public Map<String, Long> getActionCounts() {
        return actionCounts;
    }

}
