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
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;

import java.io.IOException;
import java.sql.SQLException;

public class WorkflowTools
{
    /**
     * Is the given item in the DSpace workflow
     *
     * This method queries the database directly to determine if this is the
     * case rather than using the DSpace API (which is very slow)
     *
     * @param context
     * @param item
     * @return
     * @throws DSpaceSwordException
     */
    public boolean isItemInWorkflow(Context context, Item item)
            throws DSpaceSwordException
    {
        try
        {
            String query = "SELECT workflow_id FROM workflowitem WHERE item_id = ?";
            Object[] params = { item.getID() };
            TableRowIterator tri = DatabaseManager.query(context, query, params);
            if (tri.hasNext())
            {
                tri.close();
                return true;
            }
            return false;
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
     * case rather than using the DSpace API (which is very slow)
     *
     * @param context
     * @param item
     * @return
     * @throws DSpaceSwordException
     */
    public boolean isItemInWorkspace(Context context, Item item)
            throws DSpaceSwordException
    {
        try
        {
            String query = "SELECT workspace_item_id FROM workspaceitem WHERE item_id = ?";
            Object[] params = { item.getID() };
            TableRowIterator tri = DatabaseManager.query(context, query, params);
            if (tri.hasNext())
            {
                tri.close();
                return true;
            }
            return false;
        }
        catch (SQLException e)
        {
            throw new DSpaceSwordException(e);
        }
    }

    /**
     * Obtain the WorkflowItem object which wraps the given Item
     *
     * This method queries the database directly to determine if this is the
     * case rather than using the DSpace API (which is very slow)
     *
     * @param context
     * @param item
     * @return
     * @throws DSpaceSwordException
     */
    public WorkflowItem getWorkflowItem(Context context, Item item)
            throws DSpaceSwordException
    {
        try
        {
            String query = "SELECT workflow_id FROM workflowitem WHERE item_id = ?";
            Object[] params = { item.getID() };
            TableRowIterator tri = DatabaseManager.query(context, query, params);
            if (tri.hasNext())
            {
                TableRow row = tri.next();
                int wfid = row.getIntColumn("workflow_id");
                WorkflowItem wfi = WorkflowItem.find(context, wfid);
                tri.close();
                return wfi;
            }
            return null;
        }
        catch (SQLException e)
        {
            throw new DSpaceSwordException(e);
        }
    }

    /**
     * Obtain the WorkspaceItem object which wraps the given Item
     *
     * This method queries the database directly to determine if this is the
     * case rather than using the DSpace API (which is very slow)
     *
     * @param context
     * @param item
     * @return
     * @throws DSpaceSwordException
     */
    public WorkspaceItem getWorkspaceItem(Context context, Item item)
            throws DSpaceSwordException
    {
        try
        {
            String query = "SELECT workspace_item_id FROM workspaceitem WHERE item_id = ?";
            Object[] params = { item.getID() };
            TableRowIterator tri = DatabaseManager.query(context, query, params);
            if (tri.hasNext())
            {
                TableRow row = tri.next();
                int wsid = row.getIntColumn("workspace_item_id");
                WorkspaceItem wsi = WorkspaceItem.find(context, wsid);
                tri.close();
                return wsi;
            }
            return null;
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
            boolean notify = ConfigurationManager.getBooleanProperty("swordv2-server", "workflow.notify");
            if (notify)
            {
                WorkflowManager.start(context, wsi);
            }
            else
            {
                WorkflowManager.startWithoutNotify(context, wsi);
            }
        }
        catch (SQLException e)
        {
            throw new DSpaceSwordException(e);
        }
        catch (AuthorizeException e)
        {
            throw new DSpaceSwordException(e);
        }
        catch (IOException e)
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
            WorkflowItem wfi = this.getWorkflowItem(context, item);

            // abort the workflow
            if (wfi != null)
            {
                WorkflowManager.abort(context, wfi, context.getCurrentUser());
            }
        }
        catch (SQLException e)
        {
            throw new DSpaceSwordException(e);
        }
        catch (AuthorizeException e)
        {
            throw new DSpaceSwordException(e);
        }
        catch (IOException e)
        {
            throw new DSpaceSwordException(e);
        }
    }

}
