/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import java.sql.SQLException;
import java.util.List;

import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;

/**
 * This service provides access to the Authorization Features and check if the feature is allowed or not in a specific
 * context and object.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public interface AuthorizationFeatureService {
    /**
     * Check if the eperson in the provided context, or the anonymous user if not loggedin, has access to the requested
     * feature for the requested object
     * 
     * @param context
     *            the DSpace Context
     * @param feature
     *            the Authorization Feature to check
     * @param object
     *            the object target by the feature. Passing a null object always return false. To check repository wide
     *            feature pass the {@link SiteRest} object
     * @return true if the user associated with the context has access to the feature
     */
    boolean isAuthorized(Context context, AuthorizationFeature feature, BaseObjectRest object)
        throws SQLException, SearchServiceException;

    /**
     * Get all the authorization features defined in the system
     *
     * @return a list of all the authorization features
     */
    public List<AuthorizationFeature> findAll();

    /**
     * Get the authorization feature by its unique name
     * 
     * @param name
     *            the authorization feature unique name
     * @return the authorization feature if any
     */
    public AuthorizationFeature find(String name);

    /**
     * Return all the feature that apply to the rest resources identified by the
     * uniqueType string category.model
     * 
     * @param categoryDotModel
     * @return
     */
    List<AuthorizationFeature> findByResourceType(String categoryDotModel);
}
