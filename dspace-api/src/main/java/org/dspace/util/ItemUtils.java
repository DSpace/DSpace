/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.InProgressSubmissionService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.workflow.WorkflowItemService;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

public class ItemUtils {
    public final static int UNKNOWN = -1;
    public final static int WORKSPACE = 0;
    public final static int WORKFLOW = 1;
    public final static int ARCHIVE = 2;
    public final static int WITHDRAWN = 3;

    private ItemUtils() {
    }

    public static void removeOrWithdrawn(Context context, Item item)
            throws SQLException, AuthorizeException, IOException {
        // Find item in workspace or workflow...
        InProgressSubmissionService inprogressService = getWorkspaceItemService();
        InProgressSubmission inprogress = /* WorkspaceItem */getWorkspaceItemService().findByItem(context, item);
        if (inprogress == null) {
            inprogressService = getWorkflowService();
            inprogress = /* WorkflowItem */getWorkflowService().findByItem(context, item);
        }
        // if we have an item that has been public at some time, better to keep
        // it for history
        if (item.getHandle() != null) {

            // Reopened
            if (inprogress != null) {
                item.setOwningCollection(inprogress.getCollection());
            }
//            item.withdraw();
            getItemService().withdraw(context, item);
//            item.update();
            getItemService().update(context, item);

            // Delete wrapper
            if (inprogress != null) {
                inprogressService.deleteWrapper(context, inprogress);
//                inprogress.deleteWrapper();
            }

        } else {
            inprogressService.deleteWrapper(context, inprogress);
//            inprogress.deleteWrapper();
            getItemService().delete(context, item);
//            item.delete();

        }
    }

    public static int getItemStatus(Context context, Item item) throws SQLException {
        if (item.isArchived()) {
            return ARCHIVE;
        }
        if (item.isWithdrawn()) {
            return WITHDRAWN;
        }

        WorkspaceItem row = getWorkspaceItemService().findByItem(context, item);
        if (row != null) {
            return WORKSPACE;
        }

        return WORKFLOW;

    }

    private static WorkspaceItemService getWorkspaceItemService() {
        return ContentServiceFactory.getInstance().getWorkspaceItemService();
    }

    private static WorkflowItemService<XmlWorkflowItem> getWorkflowService() {
        return WorkflowServiceFactory.getInstance().getWorkflowItemService();
    }

    private static ItemService getItemService() {
        return ContentServiceFactory.getInstance().getItemService();
    }
}
