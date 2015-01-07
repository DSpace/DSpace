/*
 */
package org.datadryad.rest.providers;

import com.sun.jersey.spi.inject.SingletonTypeInjectableProvider;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import org.datadryad.rest.storage.AbstractOrganizationStorage;
import org.datadryad.rest.storage.rdbms.OrganizationDatabaseStorageImpl;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@Provider
public class OrganizationStorageProvider extends SingletonTypeInjectableProvider<Context, AbstractOrganizationStorage> {
    public OrganizationStorageProvider() {
        super(AbstractOrganizationStorage.class, new OrganizationDatabaseStorageImpl());
    }
}
