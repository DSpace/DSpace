/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service.clarin;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.core.Context;

/**
 * Service interface class for the Item object.
 * This service is enhancement of the ItemService service for Clarin project purposes.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public interface ClarinItemService {

    /**
     * Find Item by the BitstreamUUID
     * @param context DSpace context object
     * @param bitstreamUUID UUID of the finding bitstream
     * @return found bitstream or null
     * @throws SQLException database error
     */
    List<Item> findByBitstreamUUID(Context context, UUID bitstreamUUID) throws SQLException;

    /**
     * Find Item by the Handle
     * @param context DSpace context object
     * @param handle String of the finding item
     * @return found Item or null
     * @throws SQLException database error
     */
    List<Item> findByHandle(Context context, MetadataField metadataField, String handle) throws SQLException;

    /**
     * Get item/collection/community's owning community
     * @param context DSpace context object
     * @param dso item/collection/community
     * @return owning community or null
     */
    Community getOwningCommunity(Context context, DSpaceObject dso);

    /**
     * Get owning community from the collection with UUID which is passed to the method.
     * @param context DSpace context object
     * @param owningCollectionId UUID of the collection to get the owning community
     * @return owning community or null
     * @throws SQLException
     */
    Community getOwningCommunity(Context context, UUID owningCollectionId) throws SQLException;

    /**
     * Update item's metadata about its files (local.has.files, local.files.size, local.files.count).
     * This method doesn't require Item's Bundle to be passed as a parameter. The ORIGINAL bundle is used by default.
     * @param context DSpace context object
     * @param item Update metadata for this Item
     * @throws SQLException
     */
    void updateItemFilesMetadata(Context context, Item item) throws SQLException;

    /**
     * Update item's metadata about its files (local.has.files, local.files.size, local.files.count).
     * @param context DSpace context object
     * @param item Update metadata for this Item
     * @param bundle Bundle to be used for the metadata update - it if is not the ORIGINAL bundle
     *               the method will be skipped.
     * @throws SQLException
     */
    void updateItemFilesMetadata(Context context, Item item, Bundle bundle) throws SQLException;

    /**
     * Update item's metadata about its files (local.has.files, local.files.size, local.files.count).
     * The Item and Bundle information is taken from the Bitstream object.
     * @param context
     * @param bit
     * @throws SQLException
     */
    void updateItemFilesMetadata(Context context, Bitstream bit) throws SQLException;

    /**
     * Update item's metadata about its dates (dc.date.issued, local.approximateDate.issued).
     * If the local.approximateDate.issued has any approximate value, e.g. 'cca 1938 - 1945' or 'approx. 1995'
     * or similar, use 0000
     * If the local.approximateDate.issued has several values, e.g. 1993, 1918, 2021 use the last one:
     * `dc.date.issued` = 2021
     *
     * @param context DSpace context object
     * @param item Update metadata for this Item
     */
    void updateItemDatesMetadata(Context context, Item item) throws SQLException;

}
