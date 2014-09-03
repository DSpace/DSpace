/*
 */
package org.datadryad.rest.providers;

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
public class ManuscriptStorageProvider extends SingletonTypeInjectableProvider<Context, AbstractManuscriptStorage> {
    public ManuscriptStorageProvider() {
        super(AbstractManuscriptStorage.class, new ManuscriptDatabaseStorageImpl());
    }
}
