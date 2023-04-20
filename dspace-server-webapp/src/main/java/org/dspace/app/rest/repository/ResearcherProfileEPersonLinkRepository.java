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

import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.ResearcherProfileRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.profile.ResearcherProfile;
import org.dspace.profile.service.ResearcherProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "ePerson" subresource of an individual researcher
 * profile.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Component(ResearcherProfileRest.CATEGORY + "." + ResearcherProfileRest.NAME + "." + ResearcherProfileRest.EPERSON)
public class ResearcherProfileEPersonLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private ResearcherProfileService researcherProfileService;

    /**
     * Returns the ePerson related to the Research profile with the given UUID.
     *
     * @param request    the http servlet request
     * @param id         the profile UUID
     * @param pageable   the optional pageable
     * @param projection the projection object
     * @return the ePerson rest representation
     */
    @PreAuthorize("hasPermission(#id, 'EPERSON', 'READ')")
    public EPersonRest getEPerson(@Nullable HttpServletRequest request, UUID id,
        @Nullable Pageable pageable, Projection projection) {

        try {
            Context context = obtainContext();

            ResearcherProfile profile = researcherProfileService.findById(context, id);
            if (profile == null) {
                throw new ResourceNotFoundException("No such profile with UUID: " + id);
            }

            EPerson ePerson = ePersonService.find(context, id);
            if (ePerson == null) {
                throw new ResourceNotFoundException("No such eperson related to a profile with EPerson UUID: " + id);
            }

            return converter.toRest(ePerson, projection);
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        }

    }
}
