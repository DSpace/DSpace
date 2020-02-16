/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import java.sql.SQLException;

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is a mock feature that always return true and support SITE, ITEM, COMMUNITY
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
@AuthorizationFeatureDocumentation(name = TrueForAdminsFeature.NAME)
public class TrueForAdminsFeature implements AuthorizationFeature {

    public static final String NAME = "alwaystrueadmins";

    @Autowired
    AuthorizeService authService;

    @Override
    public boolean isAuthorized(Context context, Object object) throws SQLException {
        return authService.isAdmin(context);
    }

    @Override
    public int[] getSupportedTypes() {
        return new int[]{Constants.SITE, Constants.ITEM, Constants.COMMUNITY};
    }
}