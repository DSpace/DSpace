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

import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The replace bitstream feature. It can be used to verify if the user can replace a Bitstream.
 *
 * @author Jens Vannerum (jens dot vannerum at atmire dot com)
 */
@Component
@AuthorizationFeatureDocumentation(name = CanReplaceBitstreamFeature.NAME,
    description = "It can be used to verify if the user can replace a Bitstream")
public class CanReplaceBitstreamFeature implements AuthorizationFeature {

    public static final String NAME = "canReplaceBitstream";

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private BitstreamService bitstreamService;

    @Override
    @SuppressWarnings("rawtypes")
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (object instanceof BitstreamRest) {
            if (!configurationService.getBooleanProperty("replace-bitstream.enabled", false)) {
                return false;
            }
            EPerson currentUser = context.getCurrentUser();
            if (Objects.isNull(currentUser)) {
                return false;
            }
            Bitstream bitstream = bitstreamService.find(context, UUID.fromString(((BitstreamRest) object).getUuid()));
            if (Objects.nonNull(bitstream)) {
                return authorizeService.authorizeActionBoolean(context, bitstream, Constants.WRITE);
            }
        }
        return false;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{
            BitstreamRest.CATEGORY + "." + BitstreamRest.NAME
        };
    }

}
