/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.SupervisionOrderRest;
import org.dspace.app.rest.model.WorkspaceItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.supervision.service.SupervisionOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for the supervision orders of an WorkspaceItem
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science dot it)
 */
@Component(WorkspaceItemRest.CATEGORY + "." + WorkspaceItemRest.NAME + "." + WorkspaceItemRest.SUPERVISION_ORDERS)
public class WorkspaceItemSupervisionOrdersLinkRepository
    extends AbstractDSpaceRestRepository implements LinkRestRepository {

    @Autowired
    WorkspaceItemService workspaceItemService;

    @Autowired
    SupervisionOrderService supervisionOrderService;

    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<SupervisionOrderRest> getSupervisionOrders(@Nullable HttpServletRequest request,
                                                           Integer id,
                                                           @Nullable Pageable optionalPageable,
                                                           Projection projection) {
        try {
            Context context = obtainContext();
            WorkspaceItem workspaceItem = workspaceItemService.find(context, id);
            if (workspaceItem == null) {
                throw new ResourceNotFoundException("No such workspace item: " + id);
            }
            return converter.toRestPage(
                supervisionOrderService.findByItem(context, workspaceItem.getItem()),
                optionalPageable, projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
