/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import java.sql.SQLException;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.discovery.indexobject.IndexableCommunity;
import org.dspace.discovery.indexobject.IndexableItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DSpaceObjectResolver {
    /* Log4j logger */
    private static final Logger log = Logger.getLogger(DSpaceObjectResolver.class);

    @Autowired
    ItemService itemService;

    @Autowired
    CollectionService collectionService;

    @Autowired
    CommunityService communityService;

    public IndexableObject resolveObject(Context context, UUID uuid) {
        IndexableObject object = null;
        if (uuid != null) {
            try {
                object = new IndexableCommunity(communityService.find(context, uuid));
                if (object.getIndexedObject() == null) {
                    object = new IndexableCollection(collectionService.find(context, uuid));
                }
                if (object.getIndexedObject() == null) {
                    object = new IndexableItem(itemService.find(context, uuid));
                }
                if (object.getIndexedObject() == null) {
                    throw new IllegalArgumentException("UUID " + uuid + " is expected to resolve to a Community, " +
                            "Collection or Item, but didn't resolve to any");
                }
            } catch (SQLException e) {
                log.warn("Unable to retrieve DSpace Object with ID " + uuid + " from the database", e);
            }
        }
        return object;
    }

}
