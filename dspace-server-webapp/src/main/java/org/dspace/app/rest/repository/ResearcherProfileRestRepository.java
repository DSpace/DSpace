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
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.profile.ResearcherProfile;
import org.dspace.app.profile.service.ResearcherProfileService;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.ResearcherProfileRest;
import org.dspace.app.rest.security.DSpacePermissionEvaluator;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible of exposing researcher profiles.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Component(ResearcherProfileRest.CATEGORY + "." + ResearcherProfileRest.NAME)
public class ResearcherProfileRestRepository extends DSpaceRestRepository<ResearcherProfileRest, UUID> {

    @Autowired
    private ResearcherProfileService researcherProfileService;

    @Autowired
    private DSpacePermissionEvaluator permissionEvaluator;

    @Override
    @PreAuthorize("hasPermission(#id, 'PROFILE', 'READ')")
    public ResearcherProfileRest findOne(Context context, UUID id) {
        try {
            ResearcherProfile profile = researcherProfileService.findById(context, id);
            if (profile == null) {
                return null;
            }
            return converter.toRest(profile, utils.obtainProjection());
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    protected ResearcherProfileRest createAndReturn(Context context) throws AuthorizeException, SQLException {
        UUID id = getEPersonIdFromRequest(context);
        if (!isAuthorized(id, "WRITE")) {
            throw new AuthorizeException("User unauthorized to create a new profile for user " + id);
        }
        ResearcherProfile newProfile = researcherProfileService.createAndReturn(context, id);
        return converter.toRest(newProfile, utils.obtainProjection());
    }

    @Override
    public Page<ResearcherProfileRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'PROFILE', 'WRITE')")
    protected void delete(Context context, UUID id) {
        try {
            researcherProfileService.deleteById(context, id);
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Class<ResearcherProfileRest> getDomainClass() {
        return ResearcherProfileRest.class;
    }

    private UUID getEPersonIdFromRequest(Context context) {
        HttpServletRequest request = getRequestService().getCurrentRequest().getHttpServletRequest();

        String ePersonId = request.getParameter("eperson");
        if (ePersonId == null) {
            return context.getCurrentUser().getID();
        }

        UUID uuid = UUIDUtils.fromString(ePersonId);
        if (uuid == null) {
            throw new DSpaceBadRequestException("The provided eperson parameter is not a valid uuid");
        }
        return uuid;
    }

    private boolean isAuthorized(UUID id, String permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return permissionEvaluator.hasPermission(authentication, id, "PROFILE", permission);
    }

}
