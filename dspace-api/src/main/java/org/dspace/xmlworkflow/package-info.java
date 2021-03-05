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
 * <p>A {@link WorkspaceItem} enters a workflow by presenting
 * it to {@link XmlWorkflowService#start}.  This places the wrapped Item
 * into a new {@link WorkflowItem} and links the WorkflowItem to objects which
 * represent its state of progress through the workflow of its owning Collection.
 *
 * <p>TODO More to follow....
 */

package org.dspace.xmlworkflow;

import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.workflow.WorkflowItem;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
