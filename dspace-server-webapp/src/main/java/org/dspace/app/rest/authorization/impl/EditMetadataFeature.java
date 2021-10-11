/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization.impl;

import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.authorization.AuthorizeServiceRestUtil;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.BundleRest;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.app.rest.security.DSpaceRestPermission;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The edit metadata feature. It can be used to verify if the metadata of the specified objects can be edited.
 *
 * Authorization is granted if the current user has WRITE permissions on the given DSO
 */
@Component
@AuthorizationFeatureDocumentation(name = EditMetadataFeature.NAME,
    description = "It can be used to verify if the metadata of the specified objects can be edited")
public class EditMetadataFeature implements AuthorizationFeature {

    public final static String NAME = "canEditMetadata";

    @Autowired
    private GroupService groupService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private AuthorizeServiceRestUtil authorizeServiceRestUtil;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (object instanceof CommunityRest
                || object instanceof CollectionRest
                || object instanceof ItemRest
                || object instanceof BundleRest
                || object instanceof BitstreamRest
                || object instanceof SiteRest
        ) {
            String defaultGroupUUID = configurationService.getProperty("edit.metadata.allowed-group");
            if (StringUtils.isBlank(defaultGroupUUID)) {
                return authorizeServiceRestUtil.authorizeActionBoolean(context, object,DSpaceRestPermission.WRITE);
            }
            Group defaultGroup = StringUtils.isNotBlank(defaultGroupUUID) ?
                                 groupService.find(context, UUID.fromString(defaultGroupUUID)) : null;
            if (Objects.nonNull(defaultGroup) && groupService.isMember(context, defaultGroup)) {
                return authorizeServiceRestUtil.authorizeActionBoolean(context, object,DSpaceRestPermission.WRITE);
            }
        }
        return false;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{
            CommunityRest.CATEGORY + "." + CommunityRest.NAME,
            CollectionRest.CATEGORY + "." + CollectionRest.NAME,
            ItemRest.CATEGORY + "." + ItemRest.NAME,
            BundleRest.CATEGORY + "." + BundleRest.NAME,
            BitstreamRest.CATEGORY + "." + BitstreamRest.NAME,
            SiteRest.CATEGORY + "." + SiteRest.NAME
        };
    }
}
