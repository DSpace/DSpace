/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import org.dspace.app.util.service.DSpaceObjectUtils;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.springframework.beans.factory.annotation.Autowired;


public class DSpaceObjectUtilsImpl implements DSpaceObjectUtils {

    @Autowired
    private ContentServiceFactory contentServiceFactory;
    @Autowired
    private HandleService handleService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private CollectionService collectionService;
    @Autowired
    private CommunityService communityService;

    /**
     * Retrieve a DSpaceObject from its uuid. As this method need to iterate over all the different services that
     * support concrete class of DSpaceObject it has poor performance. Please consider the use of the direct service
     * (ItemService, CommunityService, etc.) if you know in advance the type of DSpaceObject that you are looking for
     *
     * @param context
     *            DSpace context
     * @param uuid
     *            the uuid to lookup
     * @return the DSpaceObject if any with the supplied uuid
     * @throws SQLException
     */
    public DSpaceObject findDSpaceObject(Context context, UUID uuid) throws SQLException {
        for (DSpaceObjectService<? extends DSpaceObject> dSpaceObjectService :
            contentServiceFactory.getDSpaceObjectServices()) {
            DSpaceObject dso = dSpaceObjectService.find(context, uuid);
            if (dso != null) {
                return dso;
            }
        }
        return null;
    }

    /**
     * Retrieve a DSpaceObject from its uuid or handle. As this method need to iterate over all the different services
     * that support concrete class of DSpaceObject it has poor performance. Please consider the use of the direct
     * service (ItemService, CommunityService, etc.) if you know in advance the type of DSpaceObject that you are
     * looking for
     *
     * @param context DSpace context
     * @param id the uuid or handle to lookup
     * @return the DSpaceObject if any with the supplied uuid or handle
     * @throws SQLException
     */
    public DSpaceObject findDSpaceObject(Context context, String id) throws SQLException {
        DSpaceObject dso = handleService.resolveToObject(context, id);
        // if the id did not resolve to a handle, check if it is a uuid
        if (dso == null) {
            UUID uuid = null;
            try {
                uuid = UUID.fromString(id);
            } catch (IllegalArgumentException iae) {
                // nothing to do here. We check later fo empty uuids anyway
            }
            if (uuid != null) {
                dso = findDSpaceObject(context, uuid);
            }
        }
        return dso;
    }

    /**
     * Resolves a handle or UUID identifier to a basic handle-addressable DSpace Object
     * (Item, Collection, or Community) and returns it as Optional/empty.
     *
     * @param identifier   The UUID or handle of the DSpace object.
     * @return An Optional containing the DSpaceObject if found.
     * @throws SQLException If database error occurs.
     */
    @Override
    public Optional<DSpaceObject> resolveBasicDSpaceObject(Context context, String identifier)
            throws SQLException {
        UUID uuid = null;
        try {
            uuid = UUID.fromString(identifier);
        } catch (Exception e) {
            // It's not a UUID, proceed to treat it as a handle.
        }

        if (uuid != null) {
            Item item = itemService.find(context, uuid);
            if (item != null) {
                return Optional.of(item);
            }
            Community community = communityService.find(context, uuid);
            if (community != null) {
                return Optional.of(community);
            }
            Collection collection = collectionService.find(context, uuid);
            if (collection != null) {
                return Optional.of(collection);
            }
        }
        DSpaceObject dso = handleService.resolveToObject(context, identifier);
        return dso != null ? Optional.of(dso) : Optional.empty();
    }
}
