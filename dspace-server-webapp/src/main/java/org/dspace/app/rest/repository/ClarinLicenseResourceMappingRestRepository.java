/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.model.ClarinLicenseResourceMappingRest;
import org.dspace.app.rest.model.ClarinUserRegistrationRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.clarin.ClarinLicenseResourceMapping;
import org.dspace.content.clarin.ClarinUserRegistration;
import org.dspace.content.service.clarin.ClarinLicenseResourceMappingService;
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

@Component(ClarinLicenseResourceMappingRest.CATEGORY + "." + ClarinLicenseResourceMappingRest.NAME)
public class ClarinLicenseResourceMappingRestRepository
        extends DSpaceRestRepository<ClarinLicenseResourceMappingRest, Integer> {

    @Autowired
    ClarinLicenseResourceMappingService clarinLicenseResourceMappingService;

    @Override
    @PreAuthorize("permitAll()")
    public ClarinLicenseResourceMappingRest findOne(Context context, Integer integer) {
        ClarinLicenseResourceMapping clarinLicenseResourceMapping;
        try {
            clarinLicenseResourceMapping = clarinLicenseResourceMappingService.find(context, integer);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        if (Objects.isNull(clarinLicenseResourceMapping)) {
            return null;
        }
        return converter.toRest(clarinLicenseResourceMapping, utils.obtainProjection());
    }

    @Override
    public Page<ClarinLicenseResourceMappingRest> findAll(Context context, Pageable pageable) {
        try {
            List<ClarinLicenseResourceMapping> clarinLicenseResourceMappings =
                    clarinLicenseResourceMappingService.findAll(context);
            return converter.toRestPage(clarinLicenseResourceMappings, pageable, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }


    @SearchRestMethod(name = "byBitstream")
    public Page<ClarinLicenseResourceMappingRest> findByValue(@Parameter(value = "bitstreamUUID", required = true) UUID
                                              bitstreamUUID,
                                      Pageable pageable) throws SQLException {
        Context context = obtainContext();

        List<ClarinLicenseResourceMapping> clarinLicenseResourceMappingList =
                clarinLicenseResourceMappingService.findByBitstreamUUID(context, bitstreamUUID);
        if (CollectionUtils.isEmpty(clarinLicenseResourceMappingList)) {
            return null;
        }

        return converter.toRestPage(clarinLicenseResourceMappingList, pageable, utils.obtainProjection());
    }

    @Override
    public Class<ClarinLicenseResourceMappingRest> getDomainClass() {
        return ClarinLicenseResourceMappingRest.class;
    }
}
