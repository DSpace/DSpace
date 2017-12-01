/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.WorkflowException;
import org.dspace.xmlworkflow.XmlWorkflowManager;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

import javax.mail.MessagingException;
import java.io.IOException;
import java.sql.SQLException;

public class WorkflowTools
{
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
            if(ConfigurationManager.getProperty("workflow","workflow.framework").equals("xmlworkflow")){
                return XmlWorkflowItem.findByItem(context, item) != null;
            }else{
                return WorkflowItem.findByItem(context, item) != null;
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
            return WorkspaceItem.findByItem(context, item) != null;
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
            if(ConfigurationManager.getProperty("workflow","workflow.framework").equals("xmlworkflow")){
                return XmlWorkflowItem.findByItem(context, item);
            }else{
                return WorkflowItem.findByItem(context, item);
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
            return WorkspaceItem.findByItem(context, item);
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
            if (ConfigurationManager.getProperty("workflow", "workflow.framework").equals("xmlworkflow")) {
                if (notify) {
                    XmlWorkflowManager.start(context, wsi);
                } else {
                    XmlWorkflowManager.startWithoutNotify(context, wsi);
                }
            } else {
                if (notify) {
                    WorkflowManager.start(context, wsi);
                } else {
                    WorkflowManager.startWithoutNotify(context, wsi);
                }
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
        } catch (WorkflowException e) {
            throw new DSpaceSwordException(e);
        } catch (WorkflowConfigurationException e) {
            throw new DSpaceSwordException(e);
        } catch (MessagingException e) {
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
                if(wfi instanceof WorkflowItem)
                {
                    WorkflowManager.abort(context, (WorkflowItem) wfi, context.getCurrentUser());
                }else{
                    XmlWorkflowManager.abort(context, (XmlWorkflowItem) wfi, context.getCurrentUser());
                }
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
