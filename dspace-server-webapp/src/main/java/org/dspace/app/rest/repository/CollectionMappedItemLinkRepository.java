/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "mappedItems" subresource of an individual collection.
 */
@Component(CollectionRest.CATEGORY + "." + CollectionRest.NAME + "." + CollectionRest.MAPPED_ITEMS)
public class CollectionMappedItemLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    @Autowired
    CollectionService collectionService;

    @Autowired
    ItemService itemService;

    @PreAuthorize("hasPermission(#collectionId, 'COLLECTION', 'READ')")
    public Page<ItemRest> getMappedItems(@Nullable HttpServletRequest request,
                                         UUID collectionId,
                                         @Nullable Pageable optionalPageable,
                                         Projection projection) {
        try {
            Context context = obtainContext();
            Collection collection = collectionService.find(context, collectionId);
            if (collection == null) {
                throw new ResourceNotFoundException("No such collection: " + collectionId);
            }
            int total = itemService.countByCollectionMapping(context, collection);
            Pageable pageable = utils.getPageable(optionalPageable);
            List<Item> items = new ArrayList<>();
            itemService.findByCollectionMapping(context, collection, pageable.getPageSize(),
                    Math.toIntExact(pageable.getOffset())).forEachRemaining(items::add);
            return converter.toRestPage(items, pageable, total, projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
