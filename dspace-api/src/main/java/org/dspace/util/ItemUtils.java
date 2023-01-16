/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
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
