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
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The reinstate feature. It can be used by administrators (or community/collection delegate) to reinstate an item that
 * was previously withdrawn
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
@AuthorizationFeatureDocumentation(name = ReinstateFeature.NAME,
        description = "It can be used by administrators (or community/collection delegate) to reinstate an item that "
                + "was previously withdrawn")
public class ReinstateFeature implements AuthorizationFeature {

    public static final String NAME = "reinstateItem";

    @Autowired
    private Utils utils;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (!(object instanceof ItemRest)) {
            return false;
        }
        Item item = (Item) utils.getDSpaceAPIObjectFromRest(context, object);
        if (!item.isWithdrawn()) {
            return false;
        }
        try {
            AuthorizeUtil.authorizeReinstateItem(context, item);
        } catch (AuthorizeException e) {
            return false;
        }
        return true;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] { ItemRest.CATEGORY + "." + ItemRest.NAME };
    }
}