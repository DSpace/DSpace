/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The can request a copy feature. It can be used to verify if a copy can be requested of a bitstream or of a bitstream
 * in an item.
 *
 * Authorization is granted for a bitstream if the user has no access to the bitstream
 * and the bitstream is part of an archived item.
 * Authorization is granted for an item if the user has no access to a bitstream in the item, and the item is archived.
 */
@Component
@AuthorizationFeatureDocumentation(name = RequestCopyFeature.NAME,
        description = "It can be used to verify if the user can request a copy of a bitstream")
public class RequestCopyFeature implements AuthorizationFeature {

    Logger log = LogManager.getLogger();

    public final static String NAME = "canRequestACopy";

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        String requestType = configurationService.getProperty("request.item.type");
        if (StringUtils.isBlank(requestType)) {
            return false;
        } else if (StringUtils.equalsIgnoreCase(requestType, "logged")) {
            EPerson currentUser = context.getCurrentUser();
            if (currentUser == null) {
                return false;
            }
        } else if (!StringUtils.equalsIgnoreCase(requestType, "all")) {
            log.warn("The configuration parameter \"request.item.type\" contains an invalid value.");
            return false;
        }
        if (object instanceof ItemRest) {
            ItemRest itemRest = (ItemRest) object;
            String id = itemRest.getId();
            Item item = itemService.find(context, UUID.fromString(id));
            if (!item.isArchived()) {
                return false;
            }
            List<Bundle> bunds = itemService.getBundles(item, Constants.DEFAULT_BUNDLE_NAME);

            for (Bundle bund : bunds) {
                List<Bitstream> bitstreams = bund.getBitstreams();
                for (Bitstream bitstream : bitstreams) {
                    boolean authorized = authorizeService.authorizeActionBoolean(context, bitstream, Constants.READ);
                    if (!authorized) {
                        return true;
                    }
                }
            }
        } else if (object instanceof BitstreamRest) {
            BitstreamRest bitstreamRest = (BitstreamRest) object;
            Bitstream bitstream = bitstreamService.find(context, UUID.fromString(bitstreamRest.getId()));

            DSpaceObject parentObject = bitstreamService.getParentObject(context, bitstream);
            if (parentObject instanceof Item) {
                if (((Item) parentObject).isArchived()) {
                    return !authorizeService.authorizeActionBoolean(context, bitstream, Constants.READ);
                }
            }
        }
        return false;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{
            ItemRest.CATEGORY + "." + ItemRest.NAME,
            BitstreamRest.CATEGORY + "." + BitstreamRest.NAME,
        };
    }
}
