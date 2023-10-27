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
}
