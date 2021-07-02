/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Item;
import org.dspace.content.Thumbnail;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for the thumbnail Bitstream of an Item
 */
@Component(ItemRest.CATEGORY + "." + ItemRest.NAME + "." + ItemRest.THUMBNAIL)
public class ItemThumbnailLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {
    @Autowired
    ItemService itemService;

    @PreAuthorize("hasPermission(#itemId, 'ITEM', 'READ')")
    public BitstreamRest getThumbnail(@Nullable HttpServletRequest request,
                                      UUID itemId,
                                      @Nullable Pageable optionalPageable,
                                      Projection projection) {
        try {
            Context context = obtainContext();
            Item item = itemService.find(context, itemId);
            if (item == null) {
                throw new ResourceNotFoundException("No such item: " + itemId);
            }
            Thumbnail thumbnail = itemService.getThumbnail(context, item, false);
            if (thumbnail == null) {
                return null;
            }
            return converter.toRest(thumbnail.getThumb(), projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
