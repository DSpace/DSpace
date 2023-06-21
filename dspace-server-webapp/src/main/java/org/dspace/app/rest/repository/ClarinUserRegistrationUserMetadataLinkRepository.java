/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.ClarinUserMetadataRest;
import org.dspace.app.rest.model.ClarinUserRegistrationRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.clarin.ClarinUserRegistration;
import org.dspace.content.service.clarin.ClarinUserRegistrationService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

@Component(ClarinUserRegistrationRest.CATEGORY + "." + ClarinUserRegistrationRest.NAME + "." +
        ClarinUserRegistrationRest.USER_METADATA)
public class ClarinUserRegistrationUserMetadataLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    @Autowired
    ClarinUserRegistrationService clarinUserRegistrationService;

    public Page<ClarinUserMetadataRest> getUserMetadata(@Nullable HttpServletRequest request,
                                                        Integer userRegistrationID,
                                                        @Nullable Pageable optionalPageable,
                                                        Projection projection) throws SQLException {
        Context context = obtainContext();

        ClarinUserRegistration clarinUserRegistration = clarinUserRegistrationService.find(context, userRegistrationID);
        if (Objects.isNull(clarinUserRegistration)) {
            throw new ResourceNotFoundException("The ClarinLicenseResourceUserAllowance for if: " + userRegistrationID +
                    " couldn't be found");
        }

        return converter.toRestPage(clarinUserRegistration.getUserMetadata(), optionalPageable, projection);
    }
}
