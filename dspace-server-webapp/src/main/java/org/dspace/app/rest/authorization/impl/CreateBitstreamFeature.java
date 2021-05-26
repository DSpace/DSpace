/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization.impl;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.authorization.AuthorizeServiceRestUtil;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.BundleRest;
import org.dspace.app.rest.security.DSpaceRestPermission;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.BundleService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The create bitstream feature. It can be used to verify if bitstreams can be created in a specific bundle.
 *
 * Authorization is granted if the current user has ADD & WRITE permissions on the given bundle AND the item
 */
@Component
@AuthorizationFeatureDocumentation(name = CreateBitstreamFeature.NAME,
    description = "It can be used to verify if bitstreams can be created in a specific bundle")
public class CreateBitstreamFeature implements AuthorizationFeature {

    Logger log = LogManager.getLogger();

    public final static String NAME = "canCreateBitstream";

    @Autowired
    private AuthorizeServiceRestUtil authorizeServiceRestUtil;
    @Autowired
    private BundleService bundleService;
    @Autowired
    private Utils utils;
    @Autowired
    private AuthorizeService authorizeService;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (object instanceof BundleRest) {
            if (!authorizeServiceRestUtil.authorizeActionBoolean(context, object, DSpaceRestPermission.WRITE)) {
                return false;
            }
            if (!authorizeServiceRestUtil.authorizeActionBoolean(context, object, DSpaceRestPermission.ADD)) {
                return false;
            }

            DSpaceObject owningObject = bundleService.getParentObject(context,
                (Bundle)utils.getDSpaceAPIObjectFromRest(context, object));

            // Safety check. In case this is ever not true, this method should be revised.
            if (!(owningObject instanceof Item)) {
                log.error("The parent object of bundle " + object.getType() + " is not an item");
                return false;
            }

            if (!authorizeService.authorizeActionBoolean(context, context.getCurrentUser(), owningObject,
                Constants.WRITE, true)) {
                return false;
            }

            return authorizeService.authorizeActionBoolean(context, context.getCurrentUser(), owningObject,
                Constants.ADD, true);
        }
        return false;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{
            BundleRest.CATEGORY + "." + BundleRest.NAME
        };
    }
}
