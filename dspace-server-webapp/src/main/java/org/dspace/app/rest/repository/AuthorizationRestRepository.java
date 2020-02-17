/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.authorization.Authorization;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureService;
import org.dspace.app.rest.authorization.AuthorizationRestUtil;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.RepositoryNotFoundException;
import org.dspace.app.rest.model.AuthorizationRest;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.ConfigurationService;
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
    private AuthorizeService authorizeService;

    @Autowired
    private EPersonService epersonService;

    @Autowired
    protected ConverterService converter;

    @Autowired
    ConfigurationService configurationService;

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
            BaseObjectRest object = null;
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
                authorizationRest = converter.toRest(authz, utils.obtainProjection());
            }
            context.setCurrentUser(currUser);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        return authorizationRest;
    }


    /**
     * It returns the list of matching available authorizations granted to the specified eperson or to the anonymous
     * user. Only administrators and the user identified by the epersonUuid parameter can access this method
     *
     * 
     * @param context
     *            the DSpace Context
     * @param uri
     *            the uri of the object to check the authorization against
     * @param epersonUuid
     *            the eperson uuid to use in the authorization evaluation
     * @param featureName
     *            limit the authorization check to only the feature identified via its name
     * @param pageable
     *            the pagination options
     * @return the list of matching authorization available for the requested user and object, filtered by feature if
     *         provided
     * @throws AuthorizeException
     * @throws SQLException
     */
    @PreAuthorize("#epersonUuid==null || hasPermission(#epersonUuid, 'EPERSON', 'READ')")
    @SearchRestMethod(name = "object")
    public Page<AuthorizationRest> findByObject(@Parameter(value = "uri", required = true) String uri,
            @Parameter(value = "eperson") UUID epersonUuid,
            Pageable pageable) throws AuthorizeException, SQLException {
        Context context = obtainContext();
        BaseObjectRest obj = getObject(context, uri);
        if (obj == null) {
            return null;
        }

        EPerson currUser = context.getCurrentUser();
        EPerson user = currUser;

        if (epersonUuid != null) {
            if (context.getCurrentUser() == null) {
                throw new AuthorizeException("attempt to anonymously access the authorization of the eperson "
                        + epersonUuid);
            } else {
                if (!authorizeService.isAdmin(context) && !epersonUuid.equals(currUser.getID())) {
                    throw new AuthorizeException("attempt to access the authorization of the eperson " + epersonUuid
                            + " only system administrators can see the authorization of other users");
                }
                user = epersonService.find(context, epersonUuid);
            }
        } else {
            user = null;
        }
        context.setCurrentUser(user);
        List<AuthorizationFeature> features = authorizationFeatureService.findByResourceType(obj.getUniqueType());
        List<Authorization> authorizations = new ArrayList<Authorization>();
        for (AuthorizationFeature f : features) {
            if (authorizationFeatureService.isAuthorized(context, f, obj)) {
                authorizations.add(new Authorization(user, f, obj));
            }
        }
        context.setCurrentUser(currUser);
        return converter.toRestPage(utils.getPage(authorizations, pageable), utils.obtainProjection());
    }

    /**
     * It returns the authorization related to the requested feature if granted to the specified eperson or to the
     * anonymous user. Only administrators and the user identified by the epersonUuid parameter can access this method
     *
     * 
     * @param context
     *            the DSpace Context
     * @param uri
     *            the uri of the object to check the authorization against
     * @param epersonUuid
     *            the eperson uuid to use in the authorization evaluation
     * @param featureName
     *            limit the authorization check to only the feature identified via its name
     * @param pageable
     *            the pagination options
     * @return the list of matching authorization available for the requested user and object, filtered by feature if
     *         provided
     * @throws AuthorizeException
     * @throws SQLException
     */
    @PreAuthorize("#epersonUuid==null || hasPermission(#epersonUuid, 'EPERSON', 'READ')")
    @SearchRestMethod(name = "objectAndFeature")
    public AuthorizationRest findByObjectAndFeature(@Parameter(value = "uri", required = true) String uri,
            @Parameter(value = "eperson") UUID epersonUuid,
            @Parameter(value = "feature", required = true) String featureName,
            Pageable pageable) throws AuthorizeException, SQLException {
        Context context = obtainContext();
        BaseObjectRest obj = getObject(context, uri);
        if (obj == null) {
            return null;
        }

        EPerson currUser = context.getCurrentUser();
        EPerson user = currUser;
        if (epersonUuid != null) {
            if (context.getCurrentUser() == null) {
                throw new AuthorizeException("attempt to anonymously access the authorization of the eperson "
                        + epersonUuid);
            } else {
                if (!authorizeService.isAdmin(context) && !epersonUuid.equals(currUser.getID())) {
                    throw new AuthorizeException("attempt to access the authorization of the eperson " + epersonUuid
                            + " only system administrators can see the authorization of other users");
                }
                user = epersonService.find(context, epersonUuid);
            }
        } else {
            user = null;
        }
        context.setCurrentUser(user);
        AuthorizationFeature feature = authorizationFeatureService.find(featureName);
        AuthorizationRest authorizationRest = null;
        if (authorizationFeatureService.isAuthorized(context, feature, obj)) {
            Authorization authz = new Authorization();
            authz.setEperson(user);
            authz.setFeature(feature);
            authz.setObject(obj);
            authorizationRest = converter.toRest(authz, utils.obtainProjection());
        }
        context.setCurrentUser(currUser);
        return authorizationRest;
    }

    private BaseObjectRest getObject(Context context, String uri) throws SQLException {
        String dspaceUrl = configurationService.getProperty("dspace.server.url");
        if (!StringUtils.startsWith(uri, dspaceUrl)) {
            throw new IllegalArgumentException("the supplied uri is not valid: " + uri);
        }
        String[] uriParts = uri.substring(dspaceUrl.length() + (dspaceUrl.endsWith("/") ? 0 : 1) + "api/".length())
                .split("/", 3);
        if (uriParts.length != 3) {
            throw new IllegalArgumentException("the supplied uri is not valid: " + uri);
        }

        DSpaceRestRepository repository;
        try {
            repository = utils.getResourceRepository(uriParts[0], uriParts[1]);
            if (!(repository instanceof FindableObjectRepository)) {
                throw new IllegalArgumentException("the supplied uri is not valid: " + uri);
            }
        } catch (RepositoryNotFoundException e) {
            throw new IllegalArgumentException("the supplied uri is not valid: " + uri, e);
        }

        Serializable pk;
        try {
            pk = utils.castToPKClass((FindableObjectRepository) repository, uriParts[2]);
        } catch (Exception e) {
            throw new IllegalArgumentException("the supplied uri is not valid: " + uri, e);
        }
        try {
            // disable the security as we only need to retrieve the object to further process the authorization
            context.turnOffAuthorisationSystem();
            return (BaseObjectRest) repository.findOne(context, pk);
        } finally {
            context.restoreAuthSystemState();
        }
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
