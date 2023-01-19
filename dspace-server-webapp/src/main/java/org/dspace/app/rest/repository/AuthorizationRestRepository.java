/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.authorization.Authorization;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureService;
import org.dspace.app.rest.authorization.AuthorizationRestUtil;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.AuthorizationRest;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
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
            // get the user specified identified by the id, can be null for anonymous
            EPerson user;
            try {
                user = authorizationRestUtil.getEperson(context, id);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid eperson informations in the specified id " + id, e);
                return null;
            }
            EPerson currUser = context.getCurrentUser();
            if (currUser != user) {
                // Temporarily change the Context's current user in order to retrieve
                // authorizations based on that user
                context.switchContextUser(user);
            }

            if (authorizationFeatureService.isAuthorized(context, authorizationFeature, object)) {
                Authorization authz = new Authorization();
                authz.setEperson(user);
                authz.setFeature(authorizationFeature);
                authz.setObject(object);
                authorizationRest = converter.toRest(authz, utils.obtainProjection());
            }
            if (currUser != user) {
                // restore the real current user
                context.restoreContextUser();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        return authorizationRest;
    }


    /**
     * It returns the list of matching available authorizations granted to the specified eperson or to the anonymous
     * user. Only administrators and the user identified by the epersonUuid parameter can access this method
     * 
     * @param uri
     *            the uri of the object to check the authorization against
     * @param epersonUuid
     *            the eperson uuid to use in the authorization evaluation
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
            @Parameter(value = "eperson") UUID epersonUuid, @Parameter(value = "feature") String featureName,
            Pageable pageable) throws AuthorizeException, SQLException {

        Context context = obtainContext();

        EPerson currUser = context.getCurrentUser();

        // get the user specified in the requested parameters, can be null for anonymous
        EPerson user = getUserFromRequestParameter(context, epersonUuid);
        if (ObjectUtils.notEqual(currUser, user)) {
            // Temporarily change the Context's current user in order to retrieve
            // authorizations based on that user
            context.switchContextUser(user);
        }

        List<Authorization> authorizations = findAuthorizationsForUri(context, user, uri, featureName);

        if (ObjectUtils.notEqual(currUser, user)) {
            // restore the real current user
            context.restoreContextUser();
        }
        return converter.toRestPage(authorizations, pageable, utils.obtainProjection());
    }

    @PreAuthorize("#epersonUuid==null || hasPermission(#epersonUuid, 'EPERSON', 'READ')")
    @SearchRestMethod(name = "objects")
    public Page<AuthorizationRest> findByObjects(@Parameter(value = "uuid", required = true) List<String> uuidList,
            @Parameter(value = "type", required = true) String type,
            @Parameter(value = "eperson") UUID epersonUuid, @Parameter(value = "feature") List<String> featureNames,
            Pageable pageable) throws AuthorizeException, SQLException {

        Context context = obtainContext();

        EPerson currUser = context.getCurrentUser();

        // get the user specified in the requested parameters, can be null for anonymous
        EPerson user = getUserFromRequestParameter(context, epersonUuid);
        if (ObjectUtils.notEqual(currUser, user)) {
            // Temporarily change the Context's current user in order to retrieve
            // authorizations based on that user
            context.switchContextUser(user);
        }

        List<Authorization> authorizations =
                findAuthorizationsByUUIDList(context, type, uuidList, user, featureNames);

        if (ObjectUtils.notEqual(currUser, user)) {
            // restore the real current user
            context.restoreContextUser();
        }
        return converter.toRestPage(authorizations, pageable, utils.obtainProjection());
    }

    private List<Authorization> findAuthorizationsByUUIDList(
        Context context,
        String type, List<String> uuidList, EPerson user,
        List<String> featureNames) {

        if (featureNames.isEmpty()) {
            return new ArrayList<>();
        }

        List<Authorization> authorizations = new ArrayList<>();

        List<BaseObjectRest> objects = uuidList.stream()
            .map(uuid -> utils.getBaseObjectRestFromTypeAndUUID(context, type, uuid))
            .collect(Collectors.toList());

        objects.forEach(object ->
            featureNames.forEach(featureName -> {
                try {
                    authorizations.addAll(authorizationsForObject(context, user, featureName, object));
                } catch (Exception ex) {
                    log.error("An error occurred during authorizations check");
                    throw new RuntimeException(ex);
                }
            }));
        return authorizations;
    }

    private List<Authorization> findAuthorizationsForUri(
        Context context,
        EPerson user,
        String uri,
        String featureName) throws SQLException {

        BaseObjectRest restObject = utils.getBaseObjectRestFromUri(context, uri);
        return authorizationsForObject(context, user, featureName, restObject);
    }

    private List<Authorization> authorizationsForObject(
        Context context,
        EPerson user, String featureName,
        BaseObjectRest obj)
        throws SQLException {

        if (obj == null) {
            return new ArrayList<>();
        }

        List<Authorization> authorizations;
        if (isNotBlank(featureName)) {
            authorizations = findByObjectAndFeature(context, user, obj, featureName);
        } else {
            List<AuthorizationFeature> features =
                    authorizationFeatureService.findByResourceType(obj.getUniqueType());
            authorizations = new ArrayList<>();
            for (AuthorizationFeature f : features) {
                if (authorizationFeatureService.isAuthorized(context, f, obj)) {
                    authorizations.add(new Authorization(user, f, obj));
                }
            }
        }

        return authorizations;
    }

    private List<Authorization> findByObjectAndFeature(
            Context context, EPerson user, BaseObjectRest obj, String featureName
    ) throws SQLException {

        AuthorizationFeature feature = authorizationFeatureService.find(featureName);

        if (!authorizationFeatureService.isAuthorized(context, feature, obj)) {
            return emptyList();
        }

        return singletonList(new Authorization(user, feature, obj));
    }

    /**
     * Return the user specified in the request parameter if valid
     * 
     * @param context
     * @param epersonUuid
     * @return
     * @throws AuthorizeException if the user specified in the request parameter is
     *                            not valid
     * @throws SQLException       if a database error occurs
     */
    private EPerson getUserFromRequestParameter(Context context, UUID epersonUuid)
            throws AuthorizeException, SQLException {

        EPerson currUser = context.getCurrentUser();

        if (epersonUuid == null) {
            // no user is specified in the request parameters, check the permissions for the current user
            return currUser;

        } else {
            // a user is specified in the request parameters

            if (currUser == null) {
                throw new AuthorizeException("attempt to anonymously access the authorization of the eperson "
                        + epersonUuid);

            } else if (!authorizeService.isAdmin(context) && !epersonUuid.equals(currUser.getID())) {
                throw new AuthorizeException("attempt to access the authorization of the eperson " + epersonUuid
                        + " as a non-admin; only system administrators can see the authorization of other users");
            }

            return epersonService.find(context, epersonUuid);
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
