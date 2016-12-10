/*
 */
package org.datadryad.rest.providers;

import com.sun.jersey.spi.inject.SingletonTypeInjectableProvider;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import org.datadryad.rest.storage.AbstractPackageStorage;
import org.datadryad.rest.storage.rdbms.PackageDatabaseStorageImpl;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@Provider
public class PackageStorageProvider extends SingletonTypeInjectableProvider<Context, AbstractPackageStorage> {
    public PackageStorageProvider() {
        super(AbstractPackageStorage.class, new PackageDatabaseStorageImpl());
    }
}
