package org.dspace.app.rest.authorization.impl;

import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

@Component
@AuthorizationFeatureDocumentation(name = CoarNotifyLdnEnabled.NAME,
        description = "It can be used to verify if the user can see the coar notify protocol is enabled")
public class CoarNotifyLdnEnabled implements AuthorizationFeature {

    public final static String NAME = "coarLdnEnabled";

    @Autowired
    private ConfigurationService configurationService;
    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException, SearchServiceException {
        return configurationService.getBooleanProperty("coar-notify.enabled", true);
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{SiteRest.CATEGORY+"."+SiteRest.NAME};
    }
}