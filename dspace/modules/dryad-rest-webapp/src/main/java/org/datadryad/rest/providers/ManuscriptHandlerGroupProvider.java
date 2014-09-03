/*
 */
package org.datadryad.rest.providers;

import com.sun.jersey.spi.inject.SingletonTypeInjectableProvider;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@Provider
public class ManuscriptHandlerGroupProvider  extends SingletonTypeInjectableProvider<Context, ManuscriptHandlerGroupProvider> {
    // May not need a resolver subclass just to instantiate a new class
    public ManuscriptHandlerGroupProvider() {
        super(ManuscriptHandlerGroupProvider.class, new ManuscriptHandlerGroupProvider());
    }
}
