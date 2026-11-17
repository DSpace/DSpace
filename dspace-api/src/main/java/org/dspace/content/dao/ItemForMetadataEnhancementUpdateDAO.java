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

/**
 * Data Access Object for the queue of items whose relationship-derived metadata must be
 * refreshed when a related entity changes.
 *
 * <p>When an entity is updated, the dependent items are queued; a background process then polls
 * the queue and enhances them one at a time.</p>
 *
 * @see org.dspace.content.enhancer.consumer.ItemEnhancerConsumer
 * @see org.dspace.content.enhancer.ItemEnhancer
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public interface ItemForMetadataEnhancementUpdateDAO {

    /**
     * Queues all items that are potentially affected by the update of an entity
     * with the specified UUID. Affected items are those that own relationship-backed
     * metadata whose relationship has the entity as one of its endpoints.
     * 
     * @param context the DSpace Context object
     * @param uuid    the UUID of the updated entity
     * @return the number of affected items scheduled for update
     * @throws SQLException if a database error occurs
     */
    int saveAffectedItemsForUpdate(Context context, UUID uuid);

    /**
     * Removes a processed item from the metadata enhancement queue.
     * 
     * @param context      the DSpace Context object
     * @param itemToRemove the UUID of the processed item to remove from the queue
     * @throws SQLException if a database error occurs
     */
    void removeItemForUpdate(Context context, UUID itemToRemove);

    /**
     * Retrieves and removes the next item from the metadata enhancement queue
     * for processing. Items are processed in the order they were queued to ensure
     * fair processing of enhancement requests.
     * 
     * @param context the DSpace Context object
     * @return the UUID of the next item to process, or null if the queue is empty
     * @throws SQLException if a database error occurs
     */
    UUID pollItemToUpdate(Context context);
}
