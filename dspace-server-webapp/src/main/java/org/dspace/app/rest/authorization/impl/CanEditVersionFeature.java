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

import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.VersionRest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.versioning.Version;
import org.dspace.versioning.service.VersioningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The create version feature. It can be used to verify
 * if the user can create the version of an Item.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component
@AuthorizationFeatureDocumentation(name = CanEditVersionFeature.NAME,
    description = "It can be used to verify if the user can edit the summary of a version of an Item")
public class CanEditVersionFeature implements AuthorizationFeature {

    public static final String NAME = "canEditVersion";

    @Autowired
    private AuthorizeService authorizeService;
    @Autowired
    private VersioningService versioningService;
    @Autowired
    private ConfigurationService configurationService;

    @Override
    @SuppressWarnings("rawtypes")
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (object instanceof VersionRest) {
            boolean isEnabled = configurationService.getBooleanProperty("versioning.enabled", true);
            if (Objects.isNull(context.getCurrentUser()) || !isEnabled) {
                return false;
            }
            Version version = versioningService.getVersion(context, (((VersionRest) object).getId()));
            if (Objects.nonNull(version) && Objects.nonNull(version.getItem())) {
                return authorizeService.isAdmin(context, version.getItem());
            }
        }
        return false;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{
            VersionRest.CATEGORY + "." + VersionRest.NAME
        };
    }

}
