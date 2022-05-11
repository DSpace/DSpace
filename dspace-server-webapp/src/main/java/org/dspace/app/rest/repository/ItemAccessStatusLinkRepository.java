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

import org.dspace.access.status.service.AccessStatusService;
import org.dspace.app.rest.model.AccessStatusRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for calculating the access status of an Item
 */
@Component(ItemRest.CATEGORY + "." + ItemRest.NAME + "." + ItemRest.ACCESS_STATUS)
public class ItemAccessStatusLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {

    @Autowired
    ItemService itemService;

    @Autowired
    AccessStatusService accessStatusService;

    @PreAuthorize("hasPermission(#itemId, 'ITEM', 'READ')")
    public AccessStatusRest getAccessStatus(@Nullable HttpServletRequest request,
                                            UUID itemId,
                                            @Nullable Pageable optionalPageable,
                                            Projection projection) {
        try {
            Context context = obtainContext();
            Item item = itemService.find(context, itemId);
            if (item == null) {
                throw new ResourceNotFoundException("No such item: " + itemId);
            }
            AccessStatusRest accessStatusRest = new AccessStatusRest();
            String accessStatus = accessStatusService.getAccessStatus(context, item);
            accessStatusRest.setStatus(accessStatus);
            return accessStatusRest;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
