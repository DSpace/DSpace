/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;

import org.dspace.app.rest.authorize.Authorization;
import org.dspace.app.rest.authorize.AuthorizationFeature;
import org.dspace.app.rest.authorize.AuthorizationFeatureService;
import org.dspace.app.rest.authorize.AuthorizationRestUtil;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.AuthorizationRest;
import org.dspace.core.Context;
import org.dspace.discovery.FindableObject;
import org.dspace.eperson.EPerson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;


/**
 * This is the repository responsible to manage Authorization Rest object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */

@Component(AuthorizationRest.CATEGORY + "." + AuthorizationRest.NAME)
public class AuthorizationRestRepository extends DSpaceRestRepository<AuthorizationRest, String> {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationRestRepository.class);

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    @Autowired
    private AuthorizationRestUtil authorizationRestUtil;

    @Autowired
    protected ConverterService converter;

    @Override
    @PreAuthorize("hasPermission(#id, 'authorization', 'READ')")
    public AuthorizationRest findOne(Context context, String id) {

        AuthorizationRest authorizationRest = null;

        String featureName;
        try {
            featureName = authorizationRestUtil.getFeatureName(id);
        } catch (IllegalArgumentException e) {
            log.warn(e.getMessage(), e);
            return null;
        }
        try {
            FindableObject object = null;
            try {
                object = authorizationRestUtil.getObject(context, id);
            } catch (IllegalArgumentException e) {
                log.warn("Object informations not found in the specified id " + id, e);
                return null;
            }

            AuthorizationFeature authorizationFeature = null;
            if (featureName != null) {
                authorizationFeature = authorizationFeatureService.find(featureName);
            }

            if (authorizationFeature == null) {
                return null;
            }

            EPerson user;
            try {
                user = authorizationRestUtil.getEperson(context, id);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid eperson informations in the specified id " + id, e);
                return null;
            }
            EPerson currUser = context.getCurrentUser();
            context.setCurrentUser(user);

            if (authorizationFeatureService.isAuthorized(context, authorizationFeature, object)) {
                Authorization authz = new Authorization();
                authz.setEperson(user);
                authz.setFeature(authorizationFeature);
                authz.setObject(object);
                authorizationRest = converter.toRest(authz, utils.obtainProjection(true));
            }
            context.setCurrentUser(currUser);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        return authorizationRest;
    }

    @Override
    public Class<AuthorizationRest> getDomainClass() {
        return AuthorizationRest.class;
    }

    @Override
    public Page<AuthorizationRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException(AuthorizationRest.NAME, "findAll");
    }
}
