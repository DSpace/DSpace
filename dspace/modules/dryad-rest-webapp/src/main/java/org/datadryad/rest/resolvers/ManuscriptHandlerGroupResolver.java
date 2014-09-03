/*
 */
package org.datadryad.rest.resolvers;

import com.sun.jersey.spi.inject.SingletonTypeInjectableProvider;
import javax.ws.rs.core.Context;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class ManuscriptHandlerGroupResolver  extends SingletonTypeInjectableProvider<Context, ManuscriptHandlerGroupResolver> {
    // May not need a resolver subclass just to instantiate a new class
    public ManuscriptHandlerGroupResolver() {
        super(ManuscriptHandlerGroupResolver.class, new ManuscriptHandlerGroupResolver());
    }
}
