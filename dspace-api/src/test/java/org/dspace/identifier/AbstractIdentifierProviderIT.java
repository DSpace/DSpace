/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier;

import java.util.ArrayList;
import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.kernel.ServiceManager;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * AbstractIdentifierProviderIT which contains a few useful utility methods for IdentifierProvider Integration Tests
 */
public class AbstractIdentifierProviderIT extends AbstractIntegrationTestWithDatabase {

    protected final ServiceManager serviceManager = DSpaceServicesFactory.getInstance().getServiceManager();
    protected final IdentifierServiceImpl identifierService =
        serviceManager.getServicesByType(IdentifierServiceImpl.class).get(0);

    /**
     * Register a specific IdentifierProvider into the current IdentifierService (replacing any existing providers).
     * This method will also ensure the IdentifierProvider service is registered in the DSpace Service Manager.
     * @param type IdentifierProvider Class
     */
    protected void registerProvider(Class type) {
        // Register our new provider
        IdentifierProvider identifierProvider =
            (IdentifierProvider) DSpaceServicesFactory.getInstance().getServiceManager()
                                                      .getServiceByName(type.getName(), type);
        if (identifierProvider == null) {
            DSpaceServicesFactory.getInstance().getServiceManager().registerServiceClass(type.getName(), type);
            identifierProvider = (IdentifierProvider) DSpaceServicesFactory.getInstance().getServiceManager()
                                                                           .getServiceByName(type.getName(), type);
        }

        identifierService.setProviders(List.of(identifierProvider));
    }

    /**
     * Unregister a specific IdentifierProvider from the current IdentifierService (removing all existing providers).
     * This method will also ensure the IdentifierProvider service is unregistered in the DSpace Service Manager,
     * which ensures it does not conflict with other IdentifierProvider services.
     * @param type IdentifierProvider Class
     */
    protected void unregisterProvider(Class type) {
        // Find the provider service
        IdentifierProvider identifierProvider =
            (IdentifierProvider) DSpaceServicesFactory.getInstance().getServiceManager()
                                                      .getServiceByName(type.getName(), type);
        // If found, unregister it
        if (identifierProvider == null) {
            DSpaceServicesFactory.getInstance().getServiceManager().unregisterService(type.getName());
        }

        // Overwrite the identifier-service's providers with an empty list
        identifierService.setProviders(new ArrayList<>());
    }

}



