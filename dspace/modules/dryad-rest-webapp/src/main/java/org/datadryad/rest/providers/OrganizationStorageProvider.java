/*
 */
package org.datadryad.rest.providers;

import com.sun.jersey.spi.inject.SingletonTypeInjectableProvider;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import org.datadryad.rest.storage.AbstractJournalStorage;
import org.datadryad.rest.storage.rdbms.JournalDatabaseStorageImpl;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@Provider
public class OrganizationStorageProvider extends SingletonTypeInjectableProvider<Context, AbstractJournalStorage> {
    public OrganizationStorageProvider() {
        super(AbstractJournalStorage.class, new JournalDatabaseStorageImpl());
    }
}
