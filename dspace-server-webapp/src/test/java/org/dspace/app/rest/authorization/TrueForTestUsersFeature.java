/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.core.Context;
import org.springframework.stereotype.Component;

/**
 * This is a mock feature that always return true if there is a logged-in user with an email containing the test word,
 * case insensitive. The feature support SITE, ITEM, COMMUNITY
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
@AuthorizationFeatureDocumentation(name = TrueForTestUsersFeature.NAME)
public class TrueForTestUsersFeature implements AuthorizationFeature {

    public static final String NAME = "alwaystruetest";

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (context.getCurrentUser() == null) {
            return false;
        }
        return StringUtils.containsIgnoreCase(context.getCurrentUser().getEmail(), "test");
    }

    @Override
    public String[] getSupportedTypes() {
       return new String[]{
               SiteRest.CATEGORY + "." + SiteRest.NAME,
               CommunityRest.CATEGORY + "." + CommunityRest.NAME,
               ItemRest.CATEGORY + "." + ItemRest.NAME
       };
    }
}