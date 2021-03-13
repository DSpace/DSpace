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
import org.dspace.app.rest.model.SiteRest;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is a mock feature that always throw an exception during execution and support only SITE. It is possible to
 * disable the exception turning the feature in an "always false" using the configuration property
 * "org.dspace.app.rest.authorization.AlwaysThrowExceptionFeature.turnoff = true"
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
@AuthorizationFeatureDocumentation(name = AlwaysThrowExceptionFeature.NAME)
public class AlwaysThrowExceptionFeature implements AuthorizationFeature {

    public static final String NAME = "alwaysexception";

    @Autowired
    private ConfigurationService configurationService;

    @Override
    /**
     * This check will throw a runtime exception except if the
     * org.dspace.app.rest.authorization.AlwaysThrowExceptionFeature.turnoff property is set to true in the
     * configuration service. In this case it will return false
     */
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (!configurationService
                .getBooleanProperty("org.dspace.app.rest.authorization.AlwaysThrowExceptionFeature.turnoff", false)) {
            throw new  RuntimeException("Sometimes things go wrong and we should not hide it");
        }
        return false;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{SiteRest.CATEGORY + "." + SiteRest.NAME};
    }
}