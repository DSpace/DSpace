/*
 */
package org.datadryad.rest.providers;

import com.sun.jersey.spi.inject.SingletonTypeInjectableProvider;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import org.datadryad.rest.handler.ManuscriptHandlerGroup;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@Provider
public class ManuscriptHandlerGroupProvider  extends SingletonTypeInjectableProvider<Context, ManuscriptHandlerGroup> {
    // May not need a resolver subclass just to instantiate a new class
    public ManuscriptHandlerGroupProvider() {
        super(ManuscriptHandlerGroup.class, new ManuscriptHandlerGroup());
    }
}
