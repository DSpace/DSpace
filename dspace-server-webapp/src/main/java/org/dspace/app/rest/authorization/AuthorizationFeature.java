/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import java.sql.SQLException;

import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * A feature is the representation of a business goal used in the Authorization endpoint to declare what an user can do
 * on a specific object.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public interface AuthorizationFeature {
    /**
     * Check if the eperson in the provided context, or the anonymous user if not loggedin, has access to the feature
     * for the requested object
     * 
     * @param context
     *            the DSpace Context
     * @param object
     *            the object target by the feature (MUST be NOT null). Use the {@link SiteRest} object for repository
     *            wide feature
     * @return true if the user associated with the context has access to the feature for the specified object
     */
    boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException, SearchServiceException;

    /**
     * Return the name of the feature
     * 
     * @return the name of the feature
     */
    default String getName() {
        AuthorizationFeatureDocumentation anno =
                AnnotationUtils.findAnnotation(this.getClass(), AuthorizationFeatureDocumentation.class);
        if (anno != null) {
            return anno.name();
        }
        return this.getClass().getSimpleName();
    }

    /**
     * Return the description of the feature
     *
     * @return the description of the feature
     */
    default String getDescription() {
        AuthorizationFeatureDocumentation anno =
                AnnotationUtils.findAnnotation(this.getClass(), AuthorizationFeatureDocumentation.class);
        if (anno != null) {
            return anno.description();
        }
        return this.getClass().getSimpleName();
    }

    /**
     * Return the supported object type according to the {@link RestAddressableModel#getUniqueType()}
     * 
     * @return the supported object type, required to be not null
     */
    String[] getSupportedTypes();
}
