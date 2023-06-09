/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.net.URI;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.CollectionUtils;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.ResearcherProfileRest;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.repository.patch.ResourcePatch;
import org.dspace.app.rest.security.DSpacePermissionEvaluator;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.profile.ResearcherProfile;
import org.dspace.profile.service.ResearcherProfileService;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
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
@ConditionalOnProperty(value = "researcher-profile.entity-type")
public class ResearcherProfileRestRepository extends DSpaceRestRepository<ResearcherProfileRest, UUID> {

    public static final String NO_VISIBILITY_CHANGE_MSG = "Refused to perform the Researcher Profile patch based "
        + "on a token without changing the visibility";

    @Autowired
    private ResearcherProfileService researcherProfileService;

    @Autowired
    private DSpacePermissionEvaluator permissionEvaluator;

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private ResourcePatch<ResearcherProfile> resourcePatch;

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

    /**
     * Create a new researcher profile from scratch.
     */
    @Override
    @PreAuthorize("isAuthenticated()")
    protected ResearcherProfileRest createAndReturn(Context context) throws AuthorizeException, SQLException {

        UUID id = getEPersonIdFromRequest(context);
        if (isNotAuthorized(id, "WRITE")) {
            throw new AuthorizeException("User unauthorized to create a new profile for user " + id);
        }

        EPerson ePerson = ePersonService.find(context, id);
        if (ePerson == null) {
            throw new UnprocessableEntityException("No EPerson exists with id: " + id);
        }

        try {
            ResearcherProfile newProfile = researcherProfileService.createAndReturn(context, ePerson);
            return converter.toRest(newProfile, utils.obtainProjection());
        } catch (SearchServiceException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

    }

    /**
     * Create a new researcher profile claiming an already existing item.
     */
    @Override
    protected ResearcherProfileRest createAndReturn(final Context context, final List<String> list)
        throws AuthorizeException, SQLException, RepositoryMethodNotImplementedException {
        if (CollectionUtils.isEmpty(list) || list.size() > 1) {
            throw new IllegalArgumentException("Uri list must contain exactly one element");
        }


        UUID id = getEPersonIdFromRequest(context);
        if (isNotAuthorized(id, "WRITE")) {
            throw new AuthorizeException("User unauthorized to create a new profile for user " + id);
        }

        EPerson ePerson = ePersonService.find(context, id);
        if (ePerson == null) {
            throw new UnprocessableEntityException("No EPerson exists with id: " + id);
        }

        try {
            ResearcherProfile newProfile = researcherProfileService
                                               .claim(context, ePerson, URI.create(list.get(0)));
            return converter.toRest(newProfile, utils.obtainProjection());
        } catch (SearchServiceException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Page<ResearcherProfileRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'PROFILE', 'DELETE')")
    protected void delete(Context context, UUID id) {
        try {
            researcherProfileService.deleteById(context, id);
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'PROFILE', #patch)")
    protected void patch(Context context, HttpServletRequest request, String apiCategory, String model,
        UUID id, Patch patch) throws SQLException, AuthorizeException {

        ResearcherProfile profile = researcherProfileService.findById(context, id);
        if (profile == null) {
            throw new ResourceNotFoundException(apiCategory + "." + model + " with id: " + id + " not found");
        }

        resourcePatch.patch(context, profile, patch.getOperations());

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

    private boolean isNotAuthorized(UUID id, String permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return !permissionEvaluator.hasPermission(authentication, id, "PROFILE", permission);
    }

}
