/*
 */
package org.datadryad.rest.providers;

import com.sun.jersey.spi.inject.SingletonTypeInjectableProvider;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import org.datadryad.rest.storage.AbstractOrganizationConceptStorage;
import org.datadryad.rest.storage.rdbms.JournalConceptDatabaseStorageImpl;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@Provider
public class JournalConceptStorageProvider extends SingletonTypeInjectableProvider<Context, AbstractOrganizationConceptStorage> {
    public JournalConceptStorageProvider() {
        super(AbstractOrganizationConceptStorage.class, new JournalConceptDatabaseStorageImpl());
    }
}
