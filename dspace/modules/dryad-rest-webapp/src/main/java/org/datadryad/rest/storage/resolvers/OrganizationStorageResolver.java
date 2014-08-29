/*
 */
package org.datadryad.rest.storage.resolvers;

import org.datadryad.rest.storage.rdbms.OrganizationDatabaseStorageImpl;
import com.sun.jersey.spi.inject.SingletonTypeInjectableProvider;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import org.datadryad.rest.storage.AbstractOrganizationStorage;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@Provider
public class OrganizationStorageResolver extends SingletonTypeInjectableProvider<Context, AbstractOrganizationStorage> {
    public OrganizationStorageResolver() {
        super(AbstractOrganizationStorage.class, new OrganizationDatabaseStorageImpl("/opt/dryad/config/dspace.cfg"));
    }
}
