/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.RelationshipRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "relationships" subresource of an individual item.
 */
@Component(ItemRest.CATEGORY + "." + ItemRest.NAME + "." + ItemRest.RELATIONSHIPS)
public class ItemRelationshipLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    @Autowired
    RelationshipService relationshipService;

    @Autowired
    ItemService itemService;

    @PreAuthorize("hasPermission(#itemId, 'ITEM', 'READ')")
    public Page<RelationshipRest> getRelationships(@Nullable HttpServletRequest request,
                                                   UUID itemId,
                                                   @Nullable Pageable optionalPageable,
                                                   Projection projection) {
        try {
            Context context = obtainContext();
            Item item = itemService.find(context, itemId);
            if (item == null) {
                throw new ResourceNotFoundException("No such item: " + itemId);
            }
            int total = relationshipService.countByItem(context, item, true, true);
            Pageable pageable = utils.getPageable(optionalPageable);
            List<Relationship> relationships = relationshipService.findByItem(context, item,
                    pageable.getPageSize(), Math.toIntExact(pageable.getOffset()), true, true);
            return converter.toRestPage(relationships, pageable, total, projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
