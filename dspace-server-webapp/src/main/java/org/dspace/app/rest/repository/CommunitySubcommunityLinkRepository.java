/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Community;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "subcommunities" subresource of an individual community.
 */
@Component(CommunityRest.CATEGORY + "." + CommunityRest.NAME + "." + CommunityRest.SUBCOMMUNITIES)
public class CommunitySubcommunityLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    @Autowired
    CommunityService communityService;

    @Autowired
    AuthorizeService authorizeService;

    @PreAuthorize("hasPermission(#communityId, 'COMMUNITY', 'READ')")
    public Page<CommunityRest> getSubcommunities(@Nullable HttpServletRequest request,
                                                 UUID communityId,
                                                 @Nullable Pageable optionalPageable,
                                                 Projection projection) {
        try {
            Context context = obtainContext();
            Community community = communityService.find(context, communityId);
            if (community == null) {
                throw new ResourceNotFoundException("No such community: " + communityId);
            }
            List<Community> allSubcommunities = community.getSubcommunities();
            List<Community> publicSubcommunities = new LinkedList<Community>();
            for (Community subCommunity : allSubcommunities ) {
                if (authorizeService.authorizeActionBoolean(context, subCommunity, Constants.READ)) {
                    publicSubcommunities.add(subCommunity);
                }
            }
            return converter.toRestPage(utils.getPage(publicSubcommunities, optionalPageable), projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
