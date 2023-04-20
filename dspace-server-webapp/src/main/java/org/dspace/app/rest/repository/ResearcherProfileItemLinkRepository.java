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

import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.ResearcherProfileRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.profile.ResearcherProfile;
import org.dspace.profile.service.ResearcherProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "item" subresource of an individual researcher profile.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Component(ResearcherProfileRest.CATEGORY + "." + ResearcherProfileRest.NAME + "." + ResearcherProfileRest.ITEM)
public class ResearcherProfileItemLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {

    @Autowired
    private ResearcherProfileService researcherProfileService;

    /**
     * Returns the item related to the Research profile with the given UUID.
     *
     * @param request    the http servlet request
     * @param id         the profile UUID
     * @param pageable   the optional pageable
     * @param projection the projection object
     * @return the item rest representation
     */
    @PreAuthorize("hasPermission(#id, 'PROFILE', 'READ')")
    public ItemRest getItem(@Nullable HttpServletRequest request, UUID id,
        @Nullable Pageable pageable, Projection projection) {

        try {
            Context context = obtainContext();

            ResearcherProfile profile = researcherProfileService.findById(context, id);
            if (profile == null) {
                throw new ResourceNotFoundException("No such item related to a profile with EPerson UUID: " + id);
            }

            return converter.toRest(profile.getItem(), projection);
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        }

    }
}
