/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.AuthenticationTokenRest;
import org.dspace.app.rest.projection.Projection;
import org.springframework.stereotype.Component;

/**
 * This is the converter from the AuthenticationToken string to tge REST data model
 */
@Component
public class AuthenticationTokenConverter implements DSpaceConverter<String, AuthenticationTokenRest> {
    @Override
    public AuthenticationTokenRest convert(String modelObject, Projection projection) {
        AuthenticationTokenRest token = new AuthenticationTokenRest();
        token.setToken(modelObject);
        return token;
    }

    @Override
    public Class<String> getModelClass() {
        return String.class;
    }
}
