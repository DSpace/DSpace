/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/**
 * An {@link Action} changes the state of an object in a workflow.
 * 
 * <p>A {@link org.dspace.xmlworkflow.state.Step Step} is associated with two
 * kinds of Action:  {@link org.dspace.xmlworkflow.state.actions.userassignment
 * user selection} (which users may participate in a step) and
 * {@link org.dspace.xmlworkflow.state.actions.processingaction processing} (how
 * those users participate).
 * Instances of these actions are configured using {@link UserSelectionActionConfig}
 * and {@link WorkflowActionConfig}.
 *
 * <p>An {@link Action} returns an {@link ActionResult} to indicate what
 * happened when the action was executed.  This may be used to determine how the
 * item should proceed through the workflow.
 */
package org.dspace.xmlworkflow.state.actions;
