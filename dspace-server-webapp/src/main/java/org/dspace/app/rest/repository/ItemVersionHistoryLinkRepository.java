/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.VersionHistoryRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.service.VersionHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "versionhistory" subresource of an individual item.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component(ItemRest.CATEGORY + "." + ItemRest.NAME + "." + ItemRest.VERSION_HISTORY)
public class ItemVersionHistoryLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {

    @Autowired
    private ItemService itemService;

    @Autowired
    private VersionHistoryService versionHistoryService;

    @PreAuthorize("hasAuthority('ADMIN')")
    public VersionHistoryRest getVersionHistory(@Nullable HttpServletRequest request,
                                                          UUID itemId,
                                                          @Nullable Pageable optionalPageable,
                                                          Projection projection) {
        try {
            Context context = obtainContext();
            Item item = itemService.find(context, itemId);
            if (item == null) {
                throw new ResourceNotFoundException("No such item: " + itemId);
            }
            VersionHistory vh = versionHistoryService.findByItem(context, item);
            return Objects.nonNull(vh) ? converter.toRest(vh, projection) : null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}