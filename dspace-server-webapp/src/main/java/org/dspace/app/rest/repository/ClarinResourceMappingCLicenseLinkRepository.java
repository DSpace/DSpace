/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import org.dspace.app.rest.model.ClarinLicenseResourceMappingRest;
import org.dspace.app.rest.model.ClarinLicenseResourceUserAllowanceRest;
import org.dspace.app.rest.model.ClarinLicenseRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.clarin.ClarinLicense;
import org.dspace.content.clarin.ClarinLicenseResourceMapping;
import org.dspace.content.clarin.ClarinLicenseResourceUserAllowance;
import org.dspace.content.service.clarin.ClarinLicenseResourceMappingService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.Objects;

@Component(ClarinLicenseResourceMappingRest.CATEGORY + "." + ClarinLicenseResourceMappingRest.NAME +
        "." + ClarinLicenseResourceMappingRest.CLARIN_LICENSE)
public class ClarinResourceMappingCLicenseLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    @Autowired
    ClarinLicenseResourceMappingService clarinLicenseResourceMappingService;

    public ClarinLicenseRest getClarinLicense(@Nullable HttpServletRequest request,
                                              Integer mappingID,
                                              @Nullable Pageable optionalPageable,
                                              Projection projection) throws SQLException {
        Context context = obtainContext();

        ClarinLicenseResourceMapping clarinLicenseResourceMapping =
                clarinLicenseResourceMappingService.find(context, mappingID);

        if (Objects.isNull(clarinLicenseResourceMapping)) {
            throw new ResourceNotFoundException("The ClarinLicenseResourceMapping for id: " + mappingID +
                    " couldn't be found");
        }
        ClarinLicense clarinLicense = clarinLicenseResourceMapping.getLicense();
        if (Objects.isNull(clarinLicense)) {
            throw new ResourceNotFoundException("The clarinLicense for " +
                    "ClarinLicenseResourceMapping with id: " + mappingID + " doesn't exists.");
        }
        return converter.toRest(clarinLicense, projection);
    }

}
