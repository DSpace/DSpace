/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic.condition;

import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.core.Context;

/**
 * A condition that accepts a list of collection handles and returns true
 * if the item belongs to any of them.
 *
 * @author Kim Shepherd
 */
public class InCollectionCondition extends AbstractCondition {
    private static Logger log = LogManager.getLogger(InCollectionCondition.class);

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

        List<String> collectionHandles = (List<String>)getParameters().get("collections");

        // Look for the handle among an archived item's collections - this test will only work after submission
        // and archival is complete
        List<Collection> itemCollections = item.getCollections();
        for (Collection collection : itemCollections) {
            if (collectionHandles.contains(collection.getHandle())) {
                log.debug("item " + item.getHandle() + " is in collection "
                    + collection.getHandle() + ", returning true");
                return true;
            }
        }

        // Look for the parent object of the item. This is important as the item.getOwningCollection method
        // may return null, even though the item itself does have a parent object, at the point of archival
        try {
            DSpaceObject parent = itemService.getParentObject(context, item);
            if (parent != null) {
                log.debug("Got parent DSO for item: " + parent.getID().toString());
                log.debug("Parent DSO handle: " + parent.getHandle());
                if (collectionHandles.contains(parent.getHandle())) {
                    log.debug("item " + item.getHandle() + " is in collection "
                        + parent.getHandle() + ", returning true");
                    return true;
                }
            } else {
                log.debug("Parent DSO is null...");
            }
        } catch (SQLException e) {
            log.error("Error obtaining parent DSO", e);
            throw new LogicalStatementException(e);
        }

        // If we reach this statement, the item did not appear in any of the collections from the parameters
        log.debug("item " + item.getHandle() + " not found in the passed collection handle list");

        return false;
    }
}
