/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.access.status;

import java.sql.SQLException;
import java.util.Date;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Plugin interface for the access status calculation.
 */
public interface AccessStatusHelper {
    /**
     * Calculate the access status for the item.
     *
     * @param context the DSpace context
     * @param item    the item
     * @return an access status value
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public String getAccessStatusFromItem(Context context, Item item, Date threshold)
        throws SQLException;
}
