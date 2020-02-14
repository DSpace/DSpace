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
import org.springframework.stereotype.Component;

@Component(CommunityRest.CATEGORY + "." + CommunityRest.NAME + "." + CommunityRest.PARENT_COMMUNITY)
public class CommunityParentCommunityLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {

    @Autowired
    private CommunityService communityService;

    public CommunityRest getParentCommunity(@Nullable HttpServletRequest httpServletRequest,
                                            UUID communityId,
                                            @Nullable Pageable optionalPageable,
                                            Projection projection) {
        try {
            Context context = obtainContext();
            Community community = communityService.find(context, communityId);
            Community parentCommunity = (Community) communityService.getParentObject(context, community);
            if (community == null) {
                throw new ResourceNotFoundException("No such community: " + community);
            }
            if (parentCommunity == null) {
                return null;
            }
            return converter.toRest(parentCommunity, projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
