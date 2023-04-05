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
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is a mock feature that always return true if there is a logged-in user
 * members of the "Test Feature Group" group. The feature support SITE, ITEM,
 * COMMUNITY
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
@AuthorizationFeatureDocumentation(name = TrueForUsersInGroupTestFeature.NAME)
public class TrueForUsersInGroupTestFeature implements AuthorizationFeature {

    public static final String NAME = "alwaystruetestgroup";

    public static final String GROUP_NAME = "Test Feature Group";

    @Autowired
    private GroupService groupService;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (context.getCurrentUser() == null) {
            return false;
        }
        Group testGroup = groupService.findByName(context, GROUP_NAME);
        if (testGroup != null) {
            return groupService.isMember(context, testGroup);
        } else {
            return false;
        }
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