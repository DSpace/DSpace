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
 * This is a mock feature that always return false and support SITE, COLLECTION, BITSTREAM
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
@AuthorizationFeatureDocumentation(name = AlwaysFalseFeature.NAME)
public class AlwaysFalseFeature implements AuthorizationFeature {

    public static final String NAME = "alwaysfalse";

    @Override
    public boolean isAuthorized(Context context, Object object) throws SQLException {
        return false;
    }

    @Override
    public int[] getSupportedTypes() {
        return new int[]{Constants.SITE, Constants.COLLECTION, Constants.BITSTREAM};
    }
}