/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import java.sql.SQLException;

import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.core.Context;
import org.springframework.stereotype.Component;

/**
 * This is a mock feature that always return false and support SITE, COLLECTION, BITSTREAM
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
@AuthorizationFeatureDocumentation(name = AlwaysFalseFeature.NAME)
public class AlwaysFalseFeature implements AuthorizationFeature {

    public static final String NAME = "alwaysfalse";

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        return false;
    }

    @Override
    public String[] getSupportedTypes() {
       return new String[]{
               SiteRest.CATEGORY + "." + SiteRest.NAME,
               CollectionRest.CATEGORY + "." + CollectionRest.NAME,
               BitstreamRest.CATEGORY + "." + BitstreamRest.NAME
       };
    }
}