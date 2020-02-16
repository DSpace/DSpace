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
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.stereotype.Component;

/**
 * The cclicense feature. It can be used by administrators (or community/collection delegate) to manage the Creative
 * Commons license for an item
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
@AuthorizationFeatureDocumentation(name = CCLicenseFeature.NAME)
public class CCLicenseFeature implements AuthorizationFeature {

    public static final String NAME = "cclicense";

    @Override
    public boolean isAuthorized(Context context, Object object) throws SQLException {
        if (!(object instanceof Item)) {
            return false;
        }
        try {
            AuthorizeUtil.authorizeManageCCLicense(context, (Item) object);
        } catch (AuthorizeException e) {
            return false;
        }
        return true;
    }

    @Override
    public int[] getSupportedTypes() {
        return new int[]{Constants.ITEM};
    }
}