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

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.model.DSpaceObjectRest;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for the parent object of a group.
 */
@Component(GroupRest.CATEGORY + "." + GroupRest.PLURAL_NAME + "." + GroupRest.OBJECT)
public class GroupParentObjectLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    @Autowired
    GroupService groupService;

    @Autowired
    private AuthorizeService authorizeService;

    /**
     * This returns the DSpace Object (Community, Collection) belonging to this Group.
     * This is only applicable for roles in that DSpace Object
     * e.g. the Community Administrator or Collection Submitter Group
     */
    @PreAuthorize("hasPermission(#groupId, 'GROUP', 'READ') or hasAuthority('ADMIN')")
    public DSpaceObjectRest getParentObject(
            @Nullable HttpServletRequest request,
            UUID groupId,
            @Nullable Pageable optionalPageable,
            Projection projection
    ) {
        Context context = obtainContext();
        try {
            Group group = groupService.find(context, groupId);
            if (group == null) {
                throw new ResourceNotFoundException(
                        GroupRest.CATEGORY + "." + GroupRest.NAME
                                + " with id: " + groupId + " not found"
                );
            } else {
                DSpaceObject parent = groupService.getParentObject(context, group);
                if (parent != null) {
                    return converter.toRest(parent, utils.obtainProjection());
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}
