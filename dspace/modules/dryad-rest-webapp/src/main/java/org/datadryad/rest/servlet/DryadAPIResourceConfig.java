package org.datadryad.rest.servlet;

import org.datadryad.rest.auth.AuthHelper;
import org.datadryad.rest.binders.DryadAPIBinder;
import org.datadryad.rest.filters.AuthenticationRequestFilter;
import org.datadryad.rest.handler.ManuscriptHandlerGroup;
import org.datadryad.rest.storage.rdbms.AuthorizationDatabaseStorageImpl;
import org.datadryad.rest.storage.rdbms.OAuthTokenDatabaseStorageImpl;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Created by daisie on 4/16/17.
 */
public class DryadAPIResourceConfig extends ResourceConfig {
    public DryadAPIResourceConfig() {
        register(new DryadAPIBinder());
        packages(true, "org.datadryad.rest.resources.v1");
        packages(true, "org.datadryad.rest.auth");
        packages(true, "org.datadryad.rest.storage");
        register(AuthenticationRequestFilter.class);
        register(ManuscriptHandlerGroup.class);
        register(AuthHelper.class);
        register(OAuthTokenDatabaseStorageImpl.class);
        register(AuthorizationDatabaseStorageImpl.class);
    }
}