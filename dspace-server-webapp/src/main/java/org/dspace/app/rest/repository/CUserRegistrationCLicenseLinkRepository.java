/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import org.dspace.app.rest.model.ClarinLicenseRest;
import org.dspace.app.rest.model.ClarinUserRegistrationRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.VersionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.clarin.ClarinLicense;
import org.dspace.content.clarin.ClarinUserMetadata;
import org.dspace.content.clarin.ClarinUserRegistration;
import org.dspace.content.service.clarin.ClarinUserRegistrationService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component(ClarinUserRegistrationRest.CATEGORY + "." + ClarinUserRegistrationRest.NAME + "." +
        ClarinUserRegistrationRest.CLARIN_LICENSES)
public class CUserRegistrationCLicenseLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    @Autowired
    ClarinUserRegistrationService clarinUserRegistrationService;

    public Page<ClarinLicenseRest> getClarinLicenses(@Nullable HttpServletRequest request,
                                                     Integer userRegistrationID,
                                                     @Nullable Pageable optionalPageable,
                                                     Projection projection) throws SQLException {
        Context context = obtainContext();
        ClarinUserRegistration clarinUserRegistration = clarinUserRegistrationService.find(context, userRegistrationID);
        if (Objects.isNull(clarinUserRegistration)) {
            throw new ResourceNotFoundException("The CLARIN User Registration for id: " + userRegistrationID +
                    " couldn't be found");
        }
        Pageable pageable = utils.getPageable(optionalPageable);

        List<ClarinLicense> clarinLicenseList = clarinUserRegistration.getClarinLicenses();
        return converter.toRestPage(clarinLicenseList, pageable, utils.obtainProjection());
    }
}
