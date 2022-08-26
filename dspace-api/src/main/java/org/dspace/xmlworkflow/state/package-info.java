/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/**
 * A workflow consists of a sequence of steps.
 * A {@link org.dspace.workflow.WorkflowItem WorkflowItem} advances through the
 * sequence until it is either rejected or accepted.
 * 
 * <p>A {@link Workflow} holds the configuration of a workflow, and "knows"
 * which step comes next given the current step and its "outcome".
 * 
 * <p>A {@link Step} holds the configuration of a workflow step, including the
 * {@link org.dspace.xmlworkflow.state.actions.Action "actions"} to be performed,
 * the {@link org.dspace.xmlworkflow.Role "role"} to perform them,
 * and whether a user interface is required.
 */
package org.dspace.xmlworkflow.state;
