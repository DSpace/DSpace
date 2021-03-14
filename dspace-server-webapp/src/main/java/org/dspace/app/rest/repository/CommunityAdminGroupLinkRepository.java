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

import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Community;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "admingroup" subresource of an individual community.
 *
 */
@Component(CommunityRest.CATEGORY + "." + CommunityRest.NAME + "." + CommunityRest.ADMIN_GROUP)
public class CommunityAdminGroupLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {

    @Autowired
    private CommunityService communityService;

    /**
     * This method is responsible for retrieving the AdminGroup of a Community
     * @param request           The current request
     * @param communityId       The id of the community that we'll retrieve the admingroup for
     * @param optionalPageable  The pageable if applicable
     * @param projection        The current Projection
     * @return                  The admingroup of the given community
     */
    @PreAuthorize("hasPermission(#communityId, 'COMMUNITY', 'READ')")
    public GroupRest getAdminGroup(@Nullable HttpServletRequest request,
                                   UUID communityId,
                                   @Nullable Pageable optionalPageable,
                                   Projection projection) {
        try {
            Context context = obtainContext();
            Community community = communityService.find(context, communityId);
            if (community == null) {
                throw new ResourceNotFoundException("No such community: " + communityId);
            }
            try {
                AuthorizeUtil.authorizeManageAdminGroup(context, community);
            } catch (AuthorizeException e) {
                throw new AccessDeniedException("The current user was not allowed to retrieve the AdminGroup for" +
                                                    " community: " + communityId);
            }
            Group administrators = community.getAdministrators();
            if (administrators == null) {
                return null;
            }
            return converter.toRest(administrators, projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
