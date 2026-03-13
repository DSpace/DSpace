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
 * Data Access Object interface for managing the metadata enhancement update queue.
 * 
 * <p><strong>Purpose:</strong></p>
 * <p>This DAO provides a contract for managing items that need their virtual metadata
 * refreshed when related entities change. It maintains a queue system to ensure
 * data consistency across entity relationships in a performant, asynchronous manner.</p>
 * 
 * <p><strong>How it is used:</strong></p>
 * <ul>
 *   <li><strong>Dependency Tracking:</strong> When an entity is modified, this DAO identifies
 *       and queues all dependent items that reference the entity through virtual metadata</li>
 *   <li><strong>Queue Management:</strong> Provides operations to add items to the update queue,
 *       retrieve the next item for processing, and remove completed items</li>
 *   <li><strong>Virtual Metadata Synchronization:</strong> Supports the virtual metadata system
 *       where derived metadata fields need to stay synchronized with their source entities</li>
 * </ul>
 * 
 * <p><strong>Workflow:</strong></p>
 * <ol>
 *   <li>When an entity is updated, queue all dependent items for enhancement</li>
 *   <li>Background processes poll for the next item to process</li>
 *   <li>Update the item's virtual metadata fields with current entity data</li>
 *   <li>Remove the processed item from the queue</li>
 * </ol>
 * 
 * @see org.dspace.content.enhancer.consumer.ItemEnhancerConsumer
 * @see org.dspace.content.enhancer.ItemEnhancer
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public interface ItemForMetadataEnhancementUpdateDAO {

    /**
     * Queues all items that are potentially affected by the update of an entity
     * with the specified UUID. Affected items are those that reference the entity
     * through virtual metadata source fields.
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
