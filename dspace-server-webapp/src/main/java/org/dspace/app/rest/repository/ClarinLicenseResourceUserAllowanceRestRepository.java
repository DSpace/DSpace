/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.ClarinLicenseResourceUserAllowanceRest;
import org.dspace.app.rest.model.ClarinLicenseRest;
import org.dspace.app.rest.model.MetadataValueWrapperRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.clarin.ClarinLicense;
import org.dspace.content.clarin.ClarinLicenseResourceUserAllowance;
import org.dspace.content.service.clarin.ClarinLicenseResourceUserAllowanceService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component(ClarinLicenseResourceUserAllowanceRest.CATEGORY + "." + ClarinLicenseResourceUserAllowanceRest.NAME)
public class ClarinLicenseResourceUserAllowanceRestRepository
        extends DSpaceRestRepository<ClarinLicenseResourceUserAllowanceRest, Integer> {

    @Autowired
    ClarinLicenseResourceUserAllowanceService clarinLicenseResourceUserAllowanceService;

    @Override
    @PreAuthorize("permitAll()")
    public ClarinLicenseResourceUserAllowanceRest findOne(Context context, Integer integer) {
        ClarinLicenseResourceUserAllowance clarinLicenseResourceUserAllowance;
        try {
            clarinLicenseResourceUserAllowance = clarinLicenseResourceUserAllowanceService.find(context, integer);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        if (Objects.isNull(clarinLicenseResourceUserAllowance)) {
            return null;
        }
        return converter.toRest(clarinLicenseResourceUserAllowance, utils.obtainProjection());
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<ClarinLicenseResourceUserAllowanceRest> findAll(Context context, Pageable pageable) {
        try {
            List<ClarinLicenseResourceUserAllowance> clarinLicenseResourceUserAllowanceList =
                    clarinLicenseResourceUserAllowanceService.findAll(context);
            return converter.toRestPage(clarinLicenseResourceUserAllowanceList, pageable, utils.obtainProjection());
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @SearchRestMethod(name = "byBitstreamAndUser")
    public Page<ClarinLicenseResourceUserAllowanceRest> findByValue(@Parameter(value = "bitstreamUUID", required = true) UUID
                                                            bitstreamUUID,
                                                      @Parameter(value = "userUUID", required = true) UUID userUUID,
                                                      Pageable pageable) throws SQLException {
        Context context = obtainContext();

        List<ClarinLicenseResourceUserAllowance> clarinLicenseResourceUserAllowance =
                clarinLicenseResourceUserAllowanceService.findByEPersonIdAndBitstreamId(context, userUUID,
                        bitstreamUUID);

        if (CollectionUtils.isEmpty(clarinLicenseResourceUserAllowance)) {
            return null;
        }
        return converter.toRestPage(clarinLicenseResourceUserAllowance, pageable, utils.obtainProjection());
    }

    @Override
    public Class<ClarinLicenseResourceUserAllowanceRest> getDomainClass() {
        return ClarinLicenseResourceUserAllowanceRest.class;
    }
}
