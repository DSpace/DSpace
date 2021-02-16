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
 * A workflow step.
 */
public class FlowStep {
    public final String step;
    public final String queue;
    public final List<Task> tasks = new ArrayList<>();

    /**
     * Create a workflow step.
     *
     * @param name name of the step.
     * @param queue name of the step's associated queue (if any).
     */
    public FlowStep(@NotNull String name, String queue) {
        this.step = name;
        this.queue = queue;
    }

    /**
     * Associate a task with this step.
     * @param task a task to be applied in this step.
     */
    public void addTask(@NotNull Task task) {
        tasks.add(task);
    }
}
