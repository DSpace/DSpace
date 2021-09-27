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
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.security.DSpaceRestPermission;
import org.dspace.app.rest.utils.Utils;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The download bitstream feature. It can be used to verify if a bitstream can be downloaded.
 *
 * Authorization is granted if the current user has READ permissions on the given bitstream.
 */
@Component
@AuthorizationFeatureDocumentation(name = DownloadFeature.NAME,
        description = "It can be used to verify if the user can download a bitstream")
public class DownloadFeature implements AuthorizationFeature {

    public final static String NAME = "canDownload";

    @Autowired
    private AuthorizeServiceRestUtil authorizeServiceRestUtil;

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    private Utils utils;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {

        if (!(object instanceof BitstreamRest)) {
            return false;
        }

        if (authorizeServiceRestUtil.authorizeActionBoolean(context, object, DSpaceRestPermission.READ)) {
            return true;
        }

        if (context.getCurrentUser() != null) {
            return false;
        }

        DSpaceObject dSpaceObject = (DSpaceObject) utils.getDSpaceAPIObjectFromRest(context, object);
        if (dSpaceObject == null) {
            return false;
        }

        return bitstreamService.isRelatedToAProcessStartedByDefaultUser(context, (Bitstream) dSpaceObject);
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{
            BitstreamRest.CATEGORY + "." + BitstreamRest.NAME,
        };
    }
}
