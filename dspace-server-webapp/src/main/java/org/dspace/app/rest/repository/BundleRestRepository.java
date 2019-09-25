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

import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.converter.BundleConverter;
import org.dspace.app.rest.model.BundleRest;
import org.dspace.app.rest.model.hateoas.BundleResource;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.model.patch.Patch;
import org.dspace.app.rest.repository.patch.BundlePatch;
import org.dspace.app.rest.repository.patch.DSpaceObjectPatch;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bundle;
import org.dspace.content.service.BundleService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @Autowired
    private BundlePatch bundlePatch;

    public BundleRestRepository(BundleService dsoService,
                                BundleConverter dsoConverter,
                                BundlePatch dsoPatch) {
        super(dsoService, dsoConverter, dsoPatch);
        this.bundleService = dsoService;
    }

    @PreAuthorize("hasPermission(#uuid, 'BUNDLE', 'READ')")
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
        throw new RuntimeException("Method not allowed!");
    }

    @Override
    @PreAuthorize("hasPermission(#uuid, 'BUNDLE', 'WRITE')")
    protected void patch(Context context, HttpServletRequest request, String apiCategory, String model, UUID uuid,
                         Patch patch) throws AuthorizeException, SQLException {
        patchDSpaceObject(apiCategory, model, uuid, patch);
    }

    public Class<BundleRest> getDomainClass() {
        return BundleRest.class;
    }

    public DSpaceResource<BundleRest> wrapResource(BundleRest model, String... rels) {
        return new BundleResource(model, utils, rels);
    }
}
