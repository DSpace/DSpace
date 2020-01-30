/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;

public class ItemUtils {
    private static Logger log = Logger.getLogger(ItemUtils.class);

    public final static int UNKNOWN = -1;
    public final static int WORKSPACE = 0;
    public final static int WORKFLOW = 1;
    public final static int ARCHIVE = 2;
    public final static int WITHDRAWN = 3;

    public static int getItemStatus(Context context, Item item) throws SQLException {
        if (item.isArchived()) {
            return ARCHIVE;
        }
        if (item.isWithdrawn()) {
            return WITHDRAWN;
        }

        WorkspaceItem row = ContentServiceFactory.getInstance().getWorkspaceItemService().findByItem(context, item);
        if (row != null) {
            return WORKSPACE;
        }

        return WORKFLOW;

    }

    private ItemUtils() {
    }
}
