/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public interface ItemVersionProvider {
    public Item createNewItemAndAddItInWorkspace(Context c, Item item);

    public void deleteVersionedItem(Context c, Version versionToDelete, VersionHistory history) throws SQLException;

    /**
     * Copy all data (minus a few exceptions) from the old item to the new item.
     * @param c the DSpace context.
     * @param itemNew the new version of the item.
     * @param previousItem the old version of the item.
     * @return the new version of the item, with data from the old item.
     */
    public Item updateItemState(Context c, Item itemNew, Item previousItem);
}
