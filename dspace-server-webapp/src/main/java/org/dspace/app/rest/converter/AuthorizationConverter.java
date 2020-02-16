/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.authorization.Authorization;
import org.dspace.app.rest.model.AuthorizationRest;
import org.dspace.app.rest.projection.Projection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class provides the method to convert an Authorization to its REST representation, the
 * AuthorizationRest
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class AuthorizationConverter
        implements DSpaceConverter<Authorization, AuthorizationRest> {

    @Autowired
    private ConverterService converter;

    @Override
    public AuthorizationRest convert(Authorization authz, Projection projection) {
        AuthorizationRest featureRest = new AuthorizationRest();
        featureRest.setProjection(projection);
        if (authz != null) {
            featureRest.setId(authz.getID());
            if (authz.getEperson() != null) {
                featureRest.setEperson(converter.toRest(authz.getEperson(), projection));
            }
            featureRest.setFeature(converter.toRest(authz.getFeature(), projection));
            featureRest.setObject(converter.toRest(authz.getObject(), projection));
        }
        return featureRest;
    }

    @Override
    public Class<Authorization> getModelClass() {
        return Authorization.class;
    }

}
