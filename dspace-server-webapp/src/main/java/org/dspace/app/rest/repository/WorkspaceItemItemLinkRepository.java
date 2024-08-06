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

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.WorkspaceItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "item" subresource of a workspace item.
 */
@Component(WorkspaceItemRest.CATEGORY + "." + WorkspaceItemRest.PLURAL_NAME + "." + WorkspaceItemRest.ITEM)
public class WorkspaceItemItemLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    @Autowired
    WorkspaceItemService wis;

    /**
     * Retrieve the item for a workspace item.
     *
     * @param request          - The current request
     * @param id               - The workspace item ID for which to retrieve the item
     * @param optionalPageable - optional pageable object
     * @param projection       - the current projection
     * @return the item for the workspace item
     */
    @PreAuthorize("hasPermission(#id, 'WORKSPACEITEM', 'READ')")
    public ItemRest getWorkspaceItemItem(@Nullable HttpServletRequest request, Integer id,
                                         @Nullable Pageable optionalPageable, Projection projection) {
        try {
            Context context = obtainContext();
            WorkspaceItem witem = wis.find(context, id);
            if (witem == null) {
                throw new ResourceNotFoundException("No such workspace item: " + id);
            }

            return converter.toRest(witem.getItem(), projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
