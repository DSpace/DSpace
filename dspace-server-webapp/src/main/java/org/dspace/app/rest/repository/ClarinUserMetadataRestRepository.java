/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.ClarinLicenseResourceMappingRest;
import org.dspace.app.rest.model.ClarinLicenseResourceUserAllowanceRest;
import org.dspace.app.rest.model.ClarinLicenseRest;
import org.dspace.app.rest.model.ClarinUserMetadataRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.clarin.ClarinLicenseResourceMapping;
import org.dspace.content.clarin.ClarinUserMetadata;
import org.dspace.content.clarin.ClarinUserRegistration;
import org.dspace.content.service.clarin.ClarinUserMetadataService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Component(ClarinUserMetadataRest.CATEGORY + "." + ClarinUserMetadataRest.NAME)
public class ClarinUserMetadataRestRepository extends DSpaceRestRepository<ClarinUserMetadataRest, Integer> {
    @Autowired
    ClarinUserMetadataService clarinUserMetadataService;

    @Override
    @PreAuthorize("permitAll()")
    public ClarinUserMetadataRest findOne(Context context, Integer integer) {
        ClarinUserMetadata clarinUserMetadata;
        try {
            clarinUserMetadata = clarinUserMetadataService.find(context, integer);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        if (Objects.isNull(clarinUserMetadata)) {
            return null;
        }
        return converter.toRest(clarinUserMetadata, utils.obtainProjection());
    }

    @Override
    public Page<ClarinUserMetadataRest> findAll(Context context, Pageable pageable) {
        try {
            List<ClarinUserMetadata> clarinUserMetadataList =
                    clarinUserMetadataService.findAll(context);
            return converter.toRestPage(clarinUserMetadataList, pageable, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Class<ClarinUserMetadataRest> getDomainClass() {
        return ClarinUserMetadataRest.class;
    }
}
