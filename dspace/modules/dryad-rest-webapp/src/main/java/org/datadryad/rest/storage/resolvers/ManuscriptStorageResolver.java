/*
 */
package org.datadryad.rest.storage.resolvers;

import com.sun.jersey.spi.inject.SingletonTypeInjectableProvider;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import org.datadryad.rest.storage.AbstractManuscriptStorage;
import org.datadryad.rest.storage.rdbms.ManuscriptDatabaseStorageImpl;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@Provider
public class ManuscriptStorageResolver extends SingletonTypeInjectableProvider<Context, AbstractManuscriptStorage> {
    public ManuscriptStorageResolver() {
        super(AbstractManuscriptStorage.class, new ManuscriptDatabaseStorageImpl("/opt/dryad/config/dspace.cfg"));
    }
}
