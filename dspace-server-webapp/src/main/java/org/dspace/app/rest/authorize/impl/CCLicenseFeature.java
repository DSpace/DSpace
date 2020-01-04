/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorize.impl;

import java.sql.SQLException;

import org.dspace.app.rest.authorize.AuthorizationFeature;
import org.dspace.app.rest.authorize.AuthorizationFeatureDocumentation;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.stereotype.Component;

/**
 * The cclicense feature
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
@AuthorizationFeatureDocumentation(name = "cclicense")
public class CCLicenseFeature implements AuthorizationFeature {

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