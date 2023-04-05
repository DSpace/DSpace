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
import org.dspace.app.rest.model.BundleRest;
import org.dspace.app.rest.security.DSpaceRestPermission;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The reorder bitstream feature. It can be used to verify if bitstreams can be reordered in a specific bundle.
 *
 * Authorization is granted if the current user has WRITE permissions on the given bundle
 */
@Component
@AuthorizationFeatureDocumentation(name = ReorderBitstreamFeature.NAME,
    description = "It can be used to verify if bitstreams can be reordered in a specific bundle")
public class ReorderBitstreamFeature implements AuthorizationFeature {

    public final static String NAME = "canReorderBitstreams";

    @Autowired
    private AuthorizeServiceRestUtil authorizeServiceRestUtil;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (object instanceof BundleRest) {
            return authorizeServiceRestUtil.authorizeActionBoolean(context, object, DSpaceRestPermission.WRITE);
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
