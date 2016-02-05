/*
 */
package org.datadryad.rest.providers;

import com.sun.jersey.spi.inject.SingletonTypeInjectableProvider;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import org.datadryad.rest.auth.AuthHelper;
import org.datadryad.rest.storage.rdbms.AuthorizationDatabaseStorageImpl;
import org.datadryad.rest.storage.rdbms.OAuthTokenDatabaseStorageImpl;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@Provider
public class AuthHelperProvider extends SingletonTypeInjectableProvider<Context, AuthHelper> {
    public AuthHelperProvider() {
        super(AuthHelper.class,new AuthHelper(new OAuthTokenDatabaseStorageImpl(), new AuthorizationDatabaseStorageImpl()));
    }
}
