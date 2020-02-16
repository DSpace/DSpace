/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import java.sql.SQLException;

import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.stereotype.Component;

/**
 * This is a mock feature that always return true and support all the resource types
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
@AuthorizationFeatureDocumentation(name = AlwaysTrueFeature.NAME)
public class AlwaysTrueFeature implements AuthorizationFeature {
    public final static String NAME = "alwaystrue";

    @Override
    public boolean isAuthorized(Context context, Object object) throws SQLException {
        return true;
    }

    @Override
    public int[] getSupportedTypes() {
        int[] supportedTypes = new int[Constants.typeText.length];
        for (int i = 0; i < Constants.typeText.length; i++) {
            supportedTypes[i] = Constants.getTypeID(Constants.typeText[i]);
        }
        return supportedTypes;
    }
}