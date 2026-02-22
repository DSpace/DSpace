/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.core.Context;

public interface ItemForMetadataEnhancementUpdateDAO {

    /**
     * Add to the metadata_enhancement_update table queue an entry of each items
     * that is potentially affected by the update of the Item with the specified
     * uuid. The items potentially affected are the one that have the provided uuid
     * as value of a cris.virtualsource.* metadata
     * 
     * @param context the DSpace Context object
     * @param uuid    the uuid of the updated item
     * @return the number of affected items scheduled for update
     * @throws SQLException if a problem with the database occurs
     */
    int saveAffectedItemsForUpdate(Context context, UUID uuid);

    /**
     * Remove from the metadata_enhancement_update table queue the entry if any
     * related to the specified id in older than the current date
     * 
     * @param context      the DSpace Context object
     * @param itemToRemove the uuid of the processed item
     * @throws SQLException if a problem with the database occurs
     */
    void removeItemForUpdate(Context context, UUID itemToRemove);

    /**
     * Extract and remove from the table the first uuid to process from the
     * itemupdate_metadata_enhancement table ordered by date queued asc (older
     * first)
     * 
     * @param context
     * @return
     */
    UUID pollItemToUpdate(Context context);
}
