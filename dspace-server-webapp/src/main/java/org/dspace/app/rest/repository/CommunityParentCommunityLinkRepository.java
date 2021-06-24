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
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Community;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * LinkRepository for the ParentCommunity object for a Community
 */
@Component(CommunityRest.CATEGORY + "." + CommunityRest.NAME + "." + CommunityRest.PARENT_COMMUNITY)
public class CommunityParentCommunityLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {

    @Autowired
    private CommunityService communityService;

    /**
     * This method retrieves the ParentCommunity object for the Community which is defined by the given communityId
     * It'll transform this Parent Community to a REST object and return this
     * @param httpServletRequest    The current request
     * @param communityId           The given Community UUID that will be used to find the communityId
     * @param optionalPageable      The pageable
     * @param projection            The current Projection
     * @return                      The Parent Community REST object
     */
    @PreAuthorize("hasPermission(#communityId, 'COMMUNITY', 'READ')")
    public CommunityRest getParentCommunity(@Nullable HttpServletRequest httpServletRequest,
                                            UUID communityId,
                                            @Nullable Pageable optionalPageable,
                                            Projection projection) {
        try {
            Context context = obtainContext();
            Community community = communityService.find(context, communityId);
            if (community == null) {
                throw new ResourceNotFoundException("No such community: " + community);
            }
            Community parentCommunity = (Community) communityService.getParentObject(context, community);
            if (parentCommunity == null) {
                return null;
            }
            return converter.toRest(parentCommunity, projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
