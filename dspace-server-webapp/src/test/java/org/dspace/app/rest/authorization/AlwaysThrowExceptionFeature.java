/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import java.sql.SQLException;

import org.dspace.app.rest.authorize.AuthorizationFeature;
import org.dspace.app.rest.authorize.AuthorizationFeatureDocumentation;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.stereotype.Component;

/**
 * This is a mock feature that always throw an exception during execution and support only SITE
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
@AuthorizationFeatureDocumentation(name = AlwaysThrowExceptionFeature.NAME)
public class AlwaysThrowExceptionFeature implements AuthorizationFeature {

    public static final String NAME = "alwaysexception";

    @Override
    public boolean isAuthorized(Context context, Object object) throws SQLException {
        throw new  RuntimeException("Sometimes things go wrong and we should not hide it");
    }

    @Override
    public int[] getSupportedTypes() {
        return new int[]{Constants.SITE};
    }
}