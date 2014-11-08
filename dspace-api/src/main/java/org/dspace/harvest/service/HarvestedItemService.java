/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest.service;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.harvest.HarvestedItem;

import java.sql.SQLException;

/**
 * Service interface class for the HarvestedItem object.
 * The implementation of this class is responsible for all business logic calls for the HarvestedItem object and is autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface HarvestedItemService {

    /**
     * Find the harvest parameters corresponding to the specified DSpace item
     *
     * @return a HarvestedItem object corresponding to this item, null if not found.
     */
    public HarvestedItem find(Context context, Item item) throws SQLException;

    /**
     * Retrieve a DSpace Item that corresponds to this particular combination of owning collection and OAI ID.
     *
     * @param context
     * @param itemOaiID  the string used by the OAI-PMH provider to identify the item
     * @param collection the local collection that the item should be found in
     * @return DSpace Item or null if no item was found
     */
    public Item getItemByOAIId(Context context, String itemOaiID, Collection collection) throws SQLException;

    /**
     * Create a new harvested item row for a specified item id.
     *
     * @return a new HarvestedItem object
     */
    public HarvestedItem create(Context context, Item item, String itemOAIid) throws SQLException;

    public void update(Context context, HarvestedItem harvestedItem) throws SQLException;

    public void delete(Context context, HarvestedItem harvestedItem) throws SQLException;
}
