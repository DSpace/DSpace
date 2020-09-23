/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.AuthenticationTokenRest;
import org.dspace.app.rest.model.wrapper.AuthenticationToken;
import org.dspace.app.rest.projection.Projection;
import org.springframework.stereotype.Component;

/**
 * This is the converter from the AuthenticationToken to the REST data model
 */
@Component
public class AuthenticationTokenConverter implements DSpaceConverter<AuthenticationToken, AuthenticationTokenRest> {
    @Override
    public AuthenticationTokenRest convert(AuthenticationToken modelObject, Projection projection) {
        AuthenticationTokenRest token = new AuthenticationTokenRest();
        token.setToken(modelObject.getToken());
        return token;
    }

    @Override
    public Class<AuthenticationToken> getModelClass() {
        return AuthenticationToken.class;
    }
}
