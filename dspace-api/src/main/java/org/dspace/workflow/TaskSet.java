/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflow;

import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * A collection of workflow tasks.
 */
public class TaskSet {
    public final String name;
    public final List<FlowStep> steps;

    /**
     * Create a TaskSet.
     *
     * @param name name of this task set.
     * @param steps workflow steps in this task set.
     */
    public TaskSet(@NotNull String name, @NotNull List<FlowStep> steps) {
        this.name = name;
        this.steps = steps;
    }
}
