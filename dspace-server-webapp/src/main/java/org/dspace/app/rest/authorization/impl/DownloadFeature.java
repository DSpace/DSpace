/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization.impl;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.authorization.AuthorizeServiceRestUtil;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.security.DSpaceRestPermission;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The create bitstream feature. It can be used to verify if bitstreams can be created in a specific bundle.
 *
 * Authorization is granted if the current user has ADD & WRITE permissions on the given bundle AND the item
 */
@Component
@AuthorizationFeatureDocumentation(name = DownloadFeature.NAME,
        description = "It can be used to verify if the user can download a bitstream")
public class DownloadFeature implements AuthorizationFeature {

    Logger log = Logger.getLogger(DownloadFeature.class);

    public final static String NAME = "canDownload";

    @Autowired
    private AuthorizeServiceRestUtil authorizeServiceRestUtil;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (object instanceof BitstreamRest) {
            return authorizeServiceRestUtil.authorizeActionBoolean(context, object, DSpaceRestPermission.READ);
        }
        return false;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{
                BitstreamRest.CATEGORY + "." + BitstreamRest.NAME,
        };
    }
}
