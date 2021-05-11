/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflow;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Linkage between a workflow step and some {@link org.dspace.curate.CurationTask}s.
 */
public class FlowStep {
    public final String step;
    public final String queue;
    public final List<Task> tasks = new ArrayList<>();

    /**
     * Create a set of curation tasks to be linked to a named workflow step.
     * If the name of a curation task queue is supplied, the tasks will be queued;
     * otherwise they will execute as the workflow item is passing through the
     * linked workflow step.
     *
     * @param name name of the workflow step.
     * @param queue name of the associated curation queue in which tasks will run,
     *              or {@code null} if these tasks should execute immediately.
     */
    public FlowStep(@NotNull String name, String queue) {
        this.step = name;
        this.queue = queue;
    }

    /**
     * Associate a curation task with the linked workflow step.
     * @param task a curation task configuration to be applied in this step.
     */
    public void addTask(@NotNull Task task) {
        tasks.add(task);
    }
}
