/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.List;

import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureService;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.AuthorizationFeatureRest;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;


/**
 * This is the repository responsible to manage AuthorizationFeature Rest object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component(AuthorizationFeatureRest.CATEGORY + "." + AuthorizationFeatureRest.PLURAL_NAME)
public class AuthorizationFeatureRestRepository extends DSpaceRestRepository<AuthorizationFeatureRest, String> {

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    @Autowired
    protected ConverterService converter;

    @Override
    public Class<AuthorizationFeatureRest> getDomainClass() {
        return AuthorizationFeatureRest.class;
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @Override
    public Page<AuthorizationFeatureRest> findAll(Context context, Pageable pageable) {
        return converter.toRestPage(authorizationFeatureService.findAll(), pageable, utils.obtainProjection());
    }

    @PreAuthorize("permitAll()")
    @Override
    public AuthorizationFeatureRest findOne(Context context, String id) {
        AuthorizationFeature authzFeature = authorizationFeatureService.find(id);
        if (authzFeature != null) {
            return converter.toRest(authzFeature, utils.obtainProjection());
        }
        return null;
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @SearchRestMethod(name = "resourcetype")
    public Page<AuthorizationFeatureRest> findByResourceType(@Parameter(value = "type", required = true) String type,
            Pageable pageable) {
        List<AuthorizationFeature> foundFeatures = authorizationFeatureService.findByResourceType(type);
        return converter.toRestPage(foundFeatures, pageable, utils.obtainProjection());
    }
}
