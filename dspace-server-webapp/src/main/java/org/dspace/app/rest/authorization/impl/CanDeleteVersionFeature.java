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

import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.converter.ItemConverter;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.VersionRest;
import org.dspace.app.rest.projection.DefaultProjection;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.versioning.Version;
import org.dspace.versioning.service.VersioningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The delete version feature. It can be used to verify
 * if the user can delete the version of an Item.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component
@AuthorizationFeatureDocumentation(name = CanDeleteVersionFeature.NAME,
    description = "It can be used to verify if the user can delete a version of an Item")
public class CanDeleteVersionFeature extends DeleteFeature {

    @Autowired
    private ItemService itemService;
    @Autowired
    private ItemConverter itemConverter;
    @Autowired
    private VersioningService versioningService;
    @Autowired
    private ConfigurationService configurationService;

    public static final String NAME = "canDeleteVersion";

    @Override
    @SuppressWarnings("rawtypes")
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (object instanceof VersionRest) {
            if (!configurationService.getBooleanProperty("versioning.enabled", true)) {
                return false;
            }
            Version version = versioningService.getVersion(context, ((VersionRest)object).getId());
            if (Objects.nonNull(version) && Objects.nonNull(version.getItem())) {
                ItemRest itemRest = itemConverter.convert(version.getItem(), DefaultProjection.DEFAULT);
                return super.isAuthorized(context, itemRest);
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
