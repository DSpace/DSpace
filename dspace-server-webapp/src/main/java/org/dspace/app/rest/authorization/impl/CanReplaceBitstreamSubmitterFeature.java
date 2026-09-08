/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization.impl;

import java.sql.SQLException;

import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.core.Context;
import org.springframework.stereotype.Component;

/**
 * The replace bitstream feature. It can be used to verify if the user can replace a Bitstream.
 *
 * @author Jens Vannerum (jens dot vannerum at atmire dot com)
 */
@Component
@AuthorizationFeatureDocumentation(name = CanReplaceBitstreamSubmitterFeature.NAME,
    description = "It can be used to verify if the user can replace a Bitstream from the submitter UI")
public class CanReplaceBitstreamSubmitterFeature extends CanReplaceBitstreamFeature {

    public static final String NAME = "canReplaceBitstreamSubmitter";

    @Override
    @SuppressWarnings("rawtypes")
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        return object instanceof BitstreamRest
            && configurationService.getBooleanProperty("replace-bitstream.ui.submitter", true)
            && super.isAuthorized(context, object);
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{
            BitstreamRest.CATEGORY + "." + BitstreamRest.NAME
        };
    }

}
