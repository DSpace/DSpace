/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.model.ClarinUserMetadataRest;
import org.dspace.content.clarin.ClarinUserMetadata;
import org.dspace.content.service.clarin.ClarinUserMetadataService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

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

    @SearchRestMethod(name = "byUserRegistrationAndBitstream")
    public Page<ClarinUserMetadataRest> findByUserRegistrationAndBitstream(
            @Parameter(value = "userRegUUID", required = true) Integer userRegId,
            @Parameter(value = "bitstreamUUID", required = true) UUID bitstreamUUID,
            Pageable pageable) throws SQLException {
        Context context = obtainContext();

        List<ClarinUserMetadata> clarinUserMetadataList =
                clarinUserMetadataService.findByUserRegistrationAndBitstream(context, userRegId, bitstreamUUID, true);

        return converter.toRestPage(clarinUserMetadataList, pageable, utils.obtainProjection());
    }

    @Override
    public Class<ClarinUserMetadataRest> getDomainClass() {
        return ClarinUserMetadataRest.class;
    }
}
