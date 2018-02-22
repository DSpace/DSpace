/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest.service;

import java.sql.SQLException;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.harvest.HarvestedItem;

/**
 * Service interface class for the HarvestedItem object.
 * The implementation of this class is responsible for all business logic calls for the HarvestedItem object and is
 * autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface HarvestedItemService {

    /**
     * Find the harvest parameters corresponding to the specified DSpace item
     *
     * @param context The relevant DSpace Context.
     * @param item    target item
     * @return a HarvestedItem object corresponding to this item, null if not found.
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public HarvestedItem find(Context context, Item item) throws SQLException;

    /**
     * Retrieve a DSpace Item that corresponds to this particular combination of owning collection and OAI ID.
     *
     * @param context    The relevant DSpace Context.
     * @param itemOaiID  the string used by the OAI-PMH provider to identify the item
     * @param collection the local collection that the item should be found in
     * @return DSpace Item or null if no item was found
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public Item getItemByOAIId(Context context, String itemOaiID, Collection collection) throws SQLException;

    /**
     * Create a new harvested item row for a specified item id.
     *
     * @param context   The relevant DSpace Context.
     * @param item      target item
     * @param itemOAIid the string used by the OAI-PMH provider to identify the item
     * @return a new HarvestedItem object
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public HarvestedItem create(Context context, Item item, String itemOAIid) throws SQLException;

    public void update(Context context, HarvestedItem harvestedItem) throws SQLException;

    public void delete(Context context, HarvestedItem harvestedItem) throws SQLException;
}
