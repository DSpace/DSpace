/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization.impl;

import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The Manage Group Feature. It can be used to verify if the current user can manage a specific group.
 *
 * Authorization is granted if the current user has site-wide ADMIN permissions or if the current user
 * is a member of this group in case the group is linked to a community or collection.
 */
@Component
@AuthorizationFeatureDocumentation(name = ManageGroupFeature.NAME, description =
    "It can be used to verify if the current user can manage a specific group")
public class ManageGroupFeature implements AuthorizationFeature {

    public final static String NAME = "canManageGroup";

    @Autowired
    GroupService groupService;

    @Autowired
    AuthorizeService authorizeService;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        // this authorization only applies to GroupRest
        if (!(object instanceof GroupRest)) {
            return false;
        }
        GroupRest groupRest = (GroupRest) object;

        // get the group
        Group group = groupService.findByName(context, groupRest.getName());
        if (group == null) {
            return false;
        }

        // admins can manage any group
        if (authorizeService.isAdmin(context)) {
            return true;
        }

        // Community/Collection admins cannot manage groups that are not COMMUNITY/COLLECTION bound
        // This check is required until https://github.com/DSpace/DSpace/issues/3323 is fixed.
        if (!(StringUtils.startsWith(group.getName(), "COLLECTION_")
              || StringUtils.startsWith(group.getName(), "COMMUNITY_"))) {
            return false;
        }

        return authorizeService.isAdmin(context, group);
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{
            GroupRest.CATEGORY + "." + GroupRest.NAME
        };
    }

}
