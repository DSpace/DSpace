/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.workflow.WorkflowException;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowItemService;
import org.dspace.workflow.WorkflowService;
import org.dspace.workflow.factory.WorkflowServiceFactory;

import java.io.IOException;
import java.sql.SQLException;

public class WorkflowTools
{
    protected WorkspaceItemService workspaceItemService =
        ContentServiceFactory.getInstance().getWorkspaceItemService();

    protected WorkflowItemService workflowItemService =
        WorkflowServiceFactory.getInstance().getWorkflowItemService();

    protected WorkflowService workflowService =
        WorkflowServiceFactory.getInstance().getWorkflowService();

    /**
     * Is the given item in the DSpace workflow?
     * <p>
     * This method queries the database directly to determine if this is the
     * case rather than using the DSpace API (which is very slow).
     *
     * @param context
     *     The relevant DSpace Context.
     * @param item
     *     item to check
     * @return true if item is in workflow
     * @throws DSpaceSwordException
     *     can be thrown by the internals of the DSpace SWORD implementation
     */
    public boolean isItemInWorkflow(Context context, Item item)
            throws DSpaceSwordException
    {
        try
        {
            return workflowItemService.findByItem(context, item) != null;
        }
        catch (SQLException e)
        {
            throw new DSpaceSwordException(e);
        }
    }

    /**
     * Is the given item in a DSpace workspace?
     * <p>
     * This method queries the database directly to determine if this is the
     * case rather than using the DSpace API (which is very slow).
     *
     * @param context
     *     The relevant DSpace Context.
     * @param item
     *     item to check
     * @return true if item is in workspace
     * @throws DSpaceSwordException
     *     can be thrown by the internals of the DSpace SWORD implementation
     */
    public boolean isItemInWorkspace(Context context, Item item)
            throws DSpaceSwordException
    {
        try
        {
            return workspaceItemService.findByItem(context, item) != null;
        }
        catch (SQLException e)
        {
            throw new DSpaceSwordException(e);
        }
    }

    /**
     * Obtain the WorkflowItem object which wraps the given Item.
     * <p>
     * This method queries the database directly to determine if this is the
     * case rather than using the DSpace API (which is very slow).
     *
     * @param context
     *     The relevant DSpace Context.
     * @param item
     *     item to check
     * @return workflow item
     * @throws DSpaceSwordException
     *     can be thrown by the internals of the DSpace SWORD implementation
     */
    public WorkflowItem getWorkflowItem(Context context, Item item)
            throws DSpaceSwordException
    {
        try
        {
            return workflowItemService.findByItem(context, item);
        }
        catch (SQLException e)
        {
            throw new DSpaceSwordException(e);
        }
    }

    /**
     * Obtain the WorkspaceItem object which wraps the given Item.
     * <p>
     * This method queries the database directly to determine if this is the
     * case rather than using the DSpace API (which is very slow).
     *
     * @param context
     *     The relevant DSpace Context.
     * @param item
     *     item to check
     * @return workspace item
     * @throws DSpaceSwordException
     *     can be thrown by the internals of the DSpace SWORD implementation
     */
    public WorkspaceItem getWorkspaceItem(Context context, Item item)
            throws DSpaceSwordException
    {
        try
        {
            return workspaceItemService.findByItem(context, item);
        }
        catch (SQLException e)
        {
            throw new DSpaceSwordException(e);
        }
    }

    /**
     * Start the DSpace workflow on the given item
     *
     * @param context
     *     The relevant DSpace Context.
     * @param item
     *     item to check
     * @throws DSpaceSwordException
     *     can be thrown by the internals of the DSpace SWORD implementation
     */
    public void startWorkflow(Context context, Item item)
            throws DSpaceSwordException
    {
        try
        {
            // obtain the workspace item which should therefore exist
            WorkspaceItem wsi = this.getWorkspaceItem(context, item);

            // kick off the workflow
            boolean notify = ConfigurationManager
                    .getBooleanProperty("swordv2-server", "workflow.notify");
            if (notify)
            {
                workflowService.start(context, wsi);
            }
            else
            {
                workflowService.startWithoutNotify(context, wsi);
            }
        }
        catch (SQLException | WorkflowException | IOException | AuthorizeException e)
        {
            throw new DSpaceSwordException(e);
        }
    }

    /**
     * Stop the DSpace workflow, and return the item to the user workspace
     *
     * @param context
     *     The relevant DSpace Context.
     * @param item
     *     item to check
     * @throws DSpaceSwordException
     *     can be thrown by the internals of the DSpace SWORD implementation
     */
    public void stopWorkflow(Context context, Item item)
            throws DSpaceSwordException
    {
        try
        {
            // find the item in the workflow if it exists
            WorkflowItem wfi = this.getWorkflowItem(context, item);

            // abort the workflow
            if (wfi != null)
            {
                workflowService.abort(context, wfi, context.getCurrentUser());
            }
        }
        catch (SQLException | AuthorizeException | IOException e)
        {
            throw new DSpaceSwordException(e);
        }
    }

}
