/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.discovery.indexobject.IndexableCommunity;
import org.dspace.discovery.indexobject.IndexableItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Utility bean that can resolve a scope in the REST API to a DSpace Object
 */
@Component
public class ScopeResolver {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ScopeResolver.class);

    @Autowired
    CollectionService collectionService;

    @Autowired
    CommunityService communityService;

    @Autowired
    ItemService itemService;

    /**
     * Returns an IndexableObject corresponding to the community, collection, or item
     * of the given scope, or null if the scope is not a valid UUID, or is a
     * valid UUID that does not correspond to a community, collection, or item.
     *
     * @param context the DSpace context
     * @param scope a String containing the UUID of the DSpace object to return
     * @return an IndexableObject corresponding to the community, collection, or item
     * of the given scope, or null if the scope is not a valid UUID, or is a
     * valid UUID that does not correspond to a community, collection, or item.
     */
    public IndexableObject resolveScope(Context context, String scope) {
        Optional<UUID> uuidOptional = Optional.ofNullable(scope)
                                              .filter(StringUtils::isNotBlank)
                                              .map(this::asUUID);

        // First try to resolve as a Community
        return uuidOptional
            .flatMap(uuid -> resolveWithIndexedObject(context, uuid, communityService))
            // If not a Community, try as a Collection
            .or(() -> uuidOptional.flatMap(uuid -> resolveWithIndexedObject(context, uuid, collectionService)))
            // If not a Community or Collection, try as an Item
            .or(() -> uuidOptional.flatMap(uuid -> resolveWithIndexedObject(context, uuid, itemService)))
            .orElseGet(() -> {
                log.warn("Cannot find any valid scope object related to this scope {}", scope);
                return null;
            });
    }

    /**
     * Attempts to convert a string to a UUID
     *
     * @param scope the string to convert
     * @return the UUID or null if conversion fails
     */
    private UUID asUUID(String scope) {
        try {
            return UUID.fromString(scope);
        } catch (IllegalArgumentException ex) {
            log.warn("The given scope string {} is not a valid UUID", StringUtils.trimToEmpty(scope));
            return null;
        }
    }

    /**
     * Resolves a UUID to an IndexableObject using the provided service
     *
     * @param context the DSpace context
     * @param uuid    the UUID to resolve
     * @param service the service to use for resolution
     * @param <T>     the type of DSpaceObject
     * @return an Optional containing the IndexableObject if found, empty otherwise
     */
    private <T extends DSpaceObject> Optional<IndexableObject> resolveWithIndexedObject(
        Context context, UUID uuid, DSpaceObjectService<T> service
    ) {
        return Optional.ofNullable(resolve(context, uuid, service));
    }

    /**
     * Resolves a UUID to a DSpaceObject and wraps it in the appropriate IndexableObject
     *
     * @param context the DSpace context
     * @param uuid the UUID to resolve
     * @param service the service to use for resolution
     * @param <T> the type of DSpaceObject
     * @return the IndexableObject or null if not found
     */
    public <T extends DSpaceObject> IndexableObject resolve(
        Context context, UUID uuid, DSpaceObjectService<T> service
    ) {
        if (uuid == null) {
            return null;
        }

        T dspaceObject = null;
        try {
            dspaceObject = service.find(context, uuid);
        } catch (IllegalArgumentException ex) {
            if (log.isDebugEnabled()) {
                log.debug("Invalid UUID format: {}", StringUtils.trimToEmpty(uuid.toString()), ex);
            }
        } catch (SQLException ex) {
            if (log.isDebugEnabled()) {
                log.debug("Error retrieving object with ID {}", StringUtils.trimToEmpty(uuid.toString()), ex);
            }
        }

        if (dspaceObject == null) {
            return null;
        }

        // Create the appropriate IndexableObject based on the type of DSpaceObject
        if (dspaceObject instanceof Community) {
            return new IndexableCommunity((Community) dspaceObject);
        } else if (dspaceObject instanceof Collection) {
            return new IndexableCollection((Collection) dspaceObject);
        } else if (dspaceObject instanceof Item) {
            return new IndexableItem((Item) dspaceObject);
        }

        // This should not happen if all DSpaceObject types are handled above.
        log.warn("Unhandled DSpaceObject type: {}", dspaceObject.getClass().getName());
        return null;
    }
}
