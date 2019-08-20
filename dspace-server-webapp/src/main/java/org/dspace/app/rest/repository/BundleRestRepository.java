package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.app.rest.converter.BundleConverter;
import org.dspace.app.rest.model.BundleRest;
import org.dspace.app.rest.model.hateoas.BundleResource;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.repository.patch.DSpaceObjectPatch;
import org.dspace.content.Bundle;
import org.dspace.content.service.BundleService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible for managing the Bundle Rest object
 *
 * @author Jelle Pelgrims (jelle.pelgrims at atmire.com)
 */

@Component(BundleRest.CATEGORY + "." + BundleRest.NAME)
public class BundleRestRepository extends DSpaceObjectRestRepository<Bundle, BundleRest> {

    @Autowired
    private BundleService bundleService;

    public BundleRestRepository(BundleService dsoService,
                                BundleConverter dsoConverter) {
        super(dsoService, dsoConverter, new DSpaceObjectPatch<BundleRest>() {});
        this.bundleService = dsoService;
    }

    public BundleRest findOne(Context context, UUID uuid) {
        Bundle bundle = null;
        try {
            bundle = bundleService.find(context, uuid);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (bundle == null) {
            return null;
        }
        return dsoConverter.fromModel(bundle);
    }

    public Page<BundleRest> findAll(Context context, Pageable pageable) {
        return null;
    }

    public Class<BundleRest> getDomainClass() {
        return null;
    }

    public DSpaceResource<BundleRest> wrapResource(BundleRest model, String... rels) {
        return new BundleResource(model, utils, rels);
    }
}
