/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.model.AuthorizationFeatureRest;
import org.dspace.app.rest.projection.Projection;
import org.springframework.stereotype.Component;

/**
 * This class provides the method to convert an AuthorizationFeature to its REST representation, the
 * AuthorizationFeatureRest
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class AuthorizationFeatureConverter
        implements DSpaceConverter<AuthorizationFeature, AuthorizationFeatureRest> {

    @Override
    public AuthorizationFeatureRest convert(AuthorizationFeature feature, Projection projection) {
        AuthorizationFeatureRest featureRest = new AuthorizationFeatureRest();
        featureRest.setProjection(projection);
        if (feature != null) {
            featureRest.setId(feature.getName());
            featureRest.setDescription(feature.getDescription());
            List<String> types = new ArrayList<String>();
            for (String t : feature.getSupportedTypes()) {
                types.add(t);
            }
            featureRest.setResourceTypes(types);
        }
        return featureRest;
    }

    @Override
    public Class<AuthorizationFeature> getModelClass() {
        return AuthorizationFeature.class;
    }

}
