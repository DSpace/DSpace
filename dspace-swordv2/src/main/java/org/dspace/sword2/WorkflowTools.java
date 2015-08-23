/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * <p>
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.workflow.WorkflowException;
import org.dspace.workflowbasic.BasicWorkflowItem;
import org.dspace.workflowbasic.factory.BasicWorkflowServiceFactory;
import org.dspace.workflowbasic.service.BasicWorkflowItemService;
import org.dspace.workflowbasic.service.BasicWorkflowService;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.service.XmlWorkflowService;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;

import java.io.IOException;
import java.sql.SQLException;

public class WorkflowTools
{
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory
            .getInstance().getWorkspaceItemService();

    protected BasicWorkflowItemService basicWorkflowItemService = BasicWorkflowServiceFactory
            .getInstance().getBasicWorkflowItemService();

    protected BasicWorkflowService basicWorkflowService = BasicWorkflowServiceFactory
            .getInstance().getBasicWorkflowService();

    protected XmlWorkflowItemService xmlWorkflowItemService = XmlWorkflowServiceFactory
            .getInstance().getXmlWorkflowItemService();

    protected XmlWorkflowService xmlWorkflowService = XmlWorkflowServiceFactory
            .getInstance().getXmlWorkflowService();

    /**
     * Is the given item in the DSpace workflow?
     *
     * This method queries the database directly to determine if this is the
     * case rather than using the DSpace API (which is very slow).
     *
     * @param context
     * @param item
     * @throws DSpaceSwordException
     */
    public boolean isItemInWorkflow(Context context, Item item)
            throws DSpaceSwordException
    {
        try
        {
            if (ConfigurationManager
                    .getProperty("workflow", "workflow.framework")
                    .equals("xmlworkflow"))
            {
                return xmlWorkflowItemService.findByItem(context, item) != null;
            }
            else
            {
                return basicWorkflowItemService.findByItem(context, item) !=
                        null;
            }
        }
        catch (SQLException e)
        {
            throw new DSpaceSwordException(e);
        }
    }

    /**
     * Is the given item in a DSpace workspace?
     *
     * This method queries the database directly to determine if this is the
     * case rather than using the DSpace API (which is very slow).
     *
     * @param context
     * @param item
     * @throws DSpaceSwordException
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
     *
     * This method queries the database directly to determine if this is the
     * case rather than using the DSpace API (which is very slow).
     *
     * @param context
     * @param item
     * @throws DSpaceSwordException
     */
    public InProgressSubmission getWorkflowItem(Context context, Item item)
            throws DSpaceSwordException
    {
        try
        {
            if (ConfigurationManager
                    .getProperty("workflow", "workflow.framework")
                    .equals("xmlworkflow"))
            {
                return xmlWorkflowItemService.findByItem(context, item);
            }
            else
            {
                return basicWorkflowItemService.findByItem(context, item);
            }
        }
        catch (SQLException e)
        {
            throw new DSpaceSwordException(e);
        }
    }

    /**
     * Obtain the WorkspaceItem object which wraps the given Item.
     *
     * This method queries the database directly to determine if this is the
     * case rather than using the DSpace API (which is very slow).
     *
     * @param context
     * @param item
     * @throws DSpaceSwordException
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
     * @param item
     * @throws DSpaceSwordException
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
            if (ConfigurationManager
                    .getProperty("workflow", "workflow.framework")
                    .equals("xmlworkflow"))
            {
                if (notify)
                {
                    xmlWorkflowService.start(context, wsi);
                }
                else
                {
                    xmlWorkflowService.startWithoutNotify(context, wsi);
                }
            }
            else
            {
                if (notify)
                {
                    basicWorkflowService.start(context, wsi);
                }
                else
                {
                    basicWorkflowService.startWithoutNotify(context, wsi);
                }
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
     * @param item
     * @throws DSpaceSwordException
     */
    public void stopWorkflow(Context context, Item item)
            throws DSpaceSwordException
    {
        try
        {
            // find the item in the workflow if it exists
            InProgressSubmission wfi = this.getWorkflowItem(context, item);

            // abort the workflow
            if (wfi != null)
            {
                if (wfi instanceof BasicWorkflowItem)
                {
                    basicWorkflowService.abort(context, (BasicWorkflowItem) wfi,
                            context.getCurrentUser());
                }
                else
                {
                    xmlWorkflowService.abort(context, (XmlWorkflowItem) wfi,
                            context.getCurrentUser());
                }
            }
        }
        catch (SQLException | AuthorizeException | IOException e)
        {
            throw new DSpaceSwordException(e);
        }
    }

}
