/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflow;

import java.util.List;

import jakarta.validation.constraints.NotNull;

/**
 * A collection of {@link org.dspace.curate.CurationTask curation tasks} to be
 * attached to a workflow.
 */
public class TaskSet {
    /** Name of this TaskSet. */
    public final String name;

    /** The {@link FlowStep}s assigned to this TaskSet. */
    public final List<FlowStep> steps;

    /**
     * Create a name for a collection of {@link FlowStep}s.
     *
     * @param name name of this task set.
     * @param steps workflow steps in this task set.
     */
    public TaskSet(@NotNull String name, @NotNull List<FlowStep> steps) {
        this.name = name;
        this.steps = steps;
    }
}
