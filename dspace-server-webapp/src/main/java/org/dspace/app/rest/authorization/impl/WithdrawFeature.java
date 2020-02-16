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
 * The withdrawn feature. It can be used by administrators (or community/collection delegate) to logically delete an
 * item retiring it from the archive
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
@AuthorizationFeatureDocumentation(name = WithdrawFeature.NAME)
public class WithdrawFeature implements AuthorizationFeature {
    public final static String NAME = "withdrawItem";

    @Override
    public boolean isAuthorized(Context context, Object object) throws SQLException {
        if (!(object instanceof Item)) {
            return false;
        }
        Item item = (Item) object;
        if (!item.isArchived()) {
            return false;
        }
        try {
            AuthorizeUtil.authorizeWithdrawItem(context, item);
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