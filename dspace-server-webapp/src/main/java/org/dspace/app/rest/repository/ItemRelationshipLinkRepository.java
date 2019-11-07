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

import org.dspace.app.rest.converter.ConverterService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    @Autowired
    ConverterService converter;

    //@PreAuthorize("hasPermission(#itemId, 'ITEM', 'READ')")
    public Page<RelationshipRest> getItemRelationships(@Nullable HttpServletRequest request,
                                                       UUID itemId,
                                                       @Nullable Pageable optionalPageable,
                                                       Projection projection) {
        try {
            Context context = obtainContext();
            Item item = itemService.find(context, itemId);
            if (item == null) {
                return null;
            }
            Pageable pageable = optionalPageable != null ? optionalPageable : new PageRequest(0, 20);
            Integer limit = pageable == null ? null : pageable.getPageSize();
            Integer offset = pageable == null ? null : pageable.getOffset();
            int total = relationshipService.countByItem(context, item);
            List<Relationship> relationships = relationshipService.findByItem(context, item, limit, offset);
            return new PageImpl<>(relationships, pageable, total)
                    .map((object) -> converter.toRest(object, projection));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
