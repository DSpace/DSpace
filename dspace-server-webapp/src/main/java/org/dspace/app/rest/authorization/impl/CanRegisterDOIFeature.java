/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization.impl;
import java.sql.SQLException;

import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.authorization.AuthorizeServiceRestUtil;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.security.DSpaceRestPermission;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Can the current user register a DOI for this item?
 *
 * @author Kim Shepherd
 */
@Component
@AuthorizationFeatureDocumentation(name = CanRegisterDOIFeature.NAME,
    description = "It can be used to verify if the user can register a DOI for this item")
public class CanRegisterDOIFeature implements AuthorizationFeature {

    @Autowired
    private AuthorizeServiceRestUtil authorizeServiceRestUtil;
    @Autowired
    private ConfigurationService configurationService;

    public static final String NAME = "canRegisterDOI";

    @Override
    @SuppressWarnings("rawtypes")
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        // Check configuration to see if this REST operation is allowed
        if (!configurationService.getBooleanProperty("identifiers.item-status.register-doi", false)) {
            return false;
        }
        if (object instanceof ItemRest) {
            return authorizeServiceRestUtil.authorizeActionBoolean(context, object, DSpaceRestPermission.ADMIN);
        }
        return false;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{
            ItemRest.CATEGORY + "." + ItemRest.NAME
        };
    }

}
