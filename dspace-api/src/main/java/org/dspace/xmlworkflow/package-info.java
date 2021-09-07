/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

/**
 * An Item submission begins as a {@link WorkspaceItem} in some user's workspace.
 * The WorkspaceItem wraps an uninstalled {@link Item} while it is in the user's
 * workspace.
 *
 * <p>A {@link WorkspaceItem} enters a workflow by being presented to
 * {@link XmlWorkflowService#start}.  This causes the wrapped Item to be
 * unwrapped and re-wrapped into a new {@link WorkflowItem}, and links the
 * WorkflowItem to objects which represent its state of progress through the
 * workflow of its owning Collection.  This unwrapping/re-wrapping removes the
 * Item from the user's workspace and places it into the target Collection's
 * workflow.
 * 
 * <p>The WorkflowItem proceeds through a sequence of
 * {@link org.dspace.xmlworkflow.state.Step "steps"}.  Each step is configured
 * with "actions" and a {@link Role} which is to carry out the actions.  A Role
 * is configured with lists of {@code Group}s and {@code EPerson}s who will be
 * notified, when the item has entered the step, that there is a review task
 * awaiting them.
 *
 * <p>When a WorkflowItem enters a Step, it can be acted on first by one or more
 * configured {@link org.dspace.curate.CurationTask Curation Task}s.  See
 * {@link org.dspace.curate.service.XmlWorkflowCuratorService the workflow
 * curation service}.
 * 
 * <p>An Item which has completed all "steps" in the target Collection's
 * workflow is removed from the workflow and installed in the archive.
 */

package org.dspace.xmlworkflow;

import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.workflow.WorkflowItem;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
