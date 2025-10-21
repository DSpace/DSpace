/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic.condition;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.core.Context;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.factory.WorkflowServiceFactory;

/**
 * A condition that accepts a list of community handles and returns true
 * if the item belongs to any of them.
 *
 * @author Kim Shepherd
 */
public class InCommunityCondition extends AbstractCondition {
    private final static Logger log = LogManager.getLogger();

    /**
     * Return true if item is in one of the specified collections
     * Return false if not
     * @param context   DSpace context
     * @param item      Item to evaluate
     * @return boolean result of evaluation
     * @throws LogicalStatementException
     */
    @Override
    public boolean getResult(Context context, Item item) throws LogicalStatementException {

        List<String> communityHandles = (List<String>)getParameters().get("communities");
        // item.getCollections returns an array as immutable list. We need to copy it to a list we can extend.
        List<Collection> itemCollections = new ArrayList<>();
        itemCollections.addAll(item.getCollections());

        if (!item.isArchived()) {
            // do we have a worskpace or workflowitem?
            WorkspaceItem wsi = null;
            try {
                wsi = ContentServiceFactory.getInstance().getWorkspaceItemService().findByItem(context, item);
            } catch (SQLException ex) {
                log.warn("Caught and SQLException", ex);
            }
            if (wsi != null && wsi.getCollection() != null) {
                itemCollections.add(wsi.getCollection());
            }
            // do we have a workflowItem?
            WorkflowItem wfi = null;
            try {
                wfi = WorkflowServiceFactory.getInstance().getWorkflowItemService().findByItem(context, item);
            } catch (SQLException ex) {
                log.warn("Caught and SQLException", ex);
            }
            if (wfi != null && wfi.getCollection() != null) {
                itemCollections.add(wfi.getCollection());
            }
        }
        // Check communities of item.getCollections() - this will only see collections if the item is archived
        for (Collection collection : itemCollections) {
            try {
                List<Community> communities = collection.getCommunities();
                for (Community community : communities) {
                    if (communityHandles.contains(community.getHandle())) {
                        return true;
                    }
                }
            } catch (SQLException e) {
                log.error(e.getMessage());
                throw new LogicalStatementException(e);
            }
        }

        // Look for the parent object of the item. This is important as the item.getOwningCollection method
        // may return null, even though the item itself does have a parent object, at the point of archival
        try {
            DSpaceObject parent = itemService.getParentObject(context, item);
            if (parent instanceof Collection) {
                log.debug("Got parent DSO for item: " + parent.getID().toString());
                log.debug("Parent DSO handle: " + parent.getHandle());
                try {
                    // Now iterate communities of this parent collection
                    Collection collection = (Collection)parent;
                    List<Community> communities = collection.getCommunities();
                    for (Community community : communities) {
                        if (communityHandles.contains(community.getHandle())) {
                            return true;
                        }
                    }
                } catch (SQLException e) {
                    log.error(e.getMessage());
                    throw new LogicalStatementException(e);
                }
            } else {
                log.debug("Parent DSO is null or is not a Collection...");
            }
        } catch (SQLException e) {
            log.error("Error obtaining parent DSO", e);
            throw new LogicalStatementException(e);
        }

        return false;
    }
}
