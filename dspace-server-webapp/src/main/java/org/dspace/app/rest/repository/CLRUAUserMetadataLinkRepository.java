/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.rest.model.ClarinLicenseResourceUserAllowanceRest;
import org.dspace.app.rest.model.ClarinUserMetadataRest;
import org.dspace.app.rest.model.ClarinUserRegistrationRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.clarin.ClarinLicenseResourceUserAllowance;
import org.dspace.content.clarin.ClarinUserMetadata;
import org.dspace.content.clarin.ClarinUserRegistration;
import org.dspace.content.service.clarin.ClarinLicenseResourceUserAllowanceService;
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

/**
 * CLRUA = ClarinLicenseResourceUserAllowance
 */
@Component(ClarinLicenseResourceUserAllowanceRest.CATEGORY + "." + ClarinLicenseResourceUserAllowanceRest.NAME +
        "." + ClarinLicenseResourceUserAllowanceRest.USER_METADATA)
public class CLRUAUserMetadataLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    @Autowired
    ClarinLicenseResourceUserAllowanceService clarinLicenseResourceUserAllowanceService;

    public Page<ClarinUserMetadataRest> getUserMetadata(@Nullable HttpServletRequest request,
                                                        Integer clruaID,
                                                        @Nullable Pageable optionalPageable,
                                                        Projection projection) throws SQLException {
        Context context = obtainContext();

        ClarinLicenseResourceUserAllowance clarinLicenseResourceUserAllowance =
                clarinLicenseResourceUserAllowanceService.find(context, clruaID);
        if (Objects.isNull(clarinLicenseResourceUserAllowance)) {
            throw new ResourceNotFoundException("The ClarinLicenseResourceUserAllowance for if: " + clruaID +
                    " couldn't be found");
        }

        List<ClarinUserMetadata> clarinUserMetadata = clarinLicenseResourceUserAllowance.getUserMetadata();
        if (CollectionUtils.isEmpty(clarinUserMetadata)) {
            throw new ResourceNotFoundException("The ClarinUserMetadata for ClarinLicenseResourceUserAllowance " +
                    "with id: " + clruaID + "doesn't exists.");
        }

        return converter.toRestPage(clarinUserMetadata, optionalPageable, projection);
    }
}
