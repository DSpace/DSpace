/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.VersionBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.kernel.ServiceManager;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class VersionedHandleIdentifierProviderIT extends AbstractIntegrationTestWithDatabase {

    private static final String REGISTERED_PROVIDERS_PREFIX = "VersionedHandleIdentifierProvider{0}";
    private List<IdentifierProvider> originalProviders;
    private List<String> registeredProviders = new ArrayList<>(1);

    private ServiceManager serviceManager;
    private IdentifierServiceImpl identifierService;

    private String firstHandle;

    private Collection collection;
    private Item itemV1;
    private Item itemV2;
    private Item itemV3;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        serviceManager = DSpaceServicesFactory.getInstance().getServiceManager();
        originalProviders = serviceManager.getServicesByType(IdentifierProvider.class);
        identifierService = serviceManager.getServicesByType(IdentifierServiceImpl.class).get(0);
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        collection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection")
                .build();

        context.restoreAuthSystemState();
    }

    @After
    public void destroy() throws Exception {
        // set all providers to cleanup the context.
        identifierService.setProviders(serviceManager.getServicesByType(IdentifierProvider.class));
        super.destroy();
        // unregister the created services
        unregisterServices();
    }

    private void unregisterServices() {
        // restore the default providers
        identifierService.setProviders(originalProviders);
        if (registeredProviders.isEmpty()) {
            return;
        }
        // clear the registered additional providers
        for (String serviceName : registeredProviders) {
            serviceManager.unregisterService(serviceName);
        }
        registeredProviders.clear();
        originalProviders.clear();
    }

    private void registerSingleProvider(IdentifierProvider provider) {
        identifierService.setProviders(List.of(provider));
    }

    private <T> T getOrProvide(Class<T> type) {
        List<T> servicesByType = serviceManager.getServicesByType(type);
        if (servicesByType == null || servicesByType.isEmpty()) {
            String serviceBeanName = MessageFormat.format(REGISTERED_PROVIDERS_PREFIX, type.getName());
            servicesByType = List.of(serviceManager.registerServiceClass(serviceBeanName, type));
            registeredProviders.add(serviceBeanName);
        }
        return servicesByType.get(0);
    }

    private void createVersions() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();

        itemV1 = ItemBuilder.createItem(context, collection)
                .withTitle("First version")
                .build();
        firstHandle = itemV1.getHandle();
        itemV2 = VersionBuilder.createVersion(context, itemV1, "Second version").build().getItem();
        itemV3 = VersionBuilder.createVersion(context, itemV1, "Third version").build().getItem();

        context.restoreAuthSystemState();
    }

    @Test
    public void testDefaultVersionedHandleProvider() throws Exception {
        registerSingleProvider(
            getOrProvide(VersionedHandleIdentifierProvider.class)
        );
        createVersions();

        // Confirm the original item only has its original handle
        assertEquals(firstHandle, itemV1.getHandle());
        assertEquals(1, itemV1.getHandles().size());
        // Confirm the second item has the correct version handle
        assertEquals(firstHandle + ".2", itemV2.getHandle());
        assertEquals(1, itemV2.getHandles().size());
        // Confirm the last item has the correct version handle
        assertEquals(firstHandle + ".3", itemV3.getHandle());
        assertEquals(1, itemV3.getHandles().size());
    }

    @Test
    public void testCanonicalVersionedHandleProvider() throws Exception {
        registerSingleProvider(
            getOrProvide(VersionedHandleIdentifierProviderWithCanonicalHandles.class)
        );
        createVersions();

        // Confirm the original item only has a version handle
        assertEquals(firstHandle + ".1", itemV1.getHandle());
        assertEquals(1, itemV1.getHandles().size());
        // Confirm the second item has the correct version handle
        assertEquals(firstHandle + ".2", itemV2.getHandle());
        assertEquals(1, itemV2.getHandles().size());
        // Confirm the last item has both the correct version handle and the original handle
        assertEquals(firstHandle, itemV3.getHandle());
        assertEquals(2, itemV3.getHandles().size());
        containsHandle(itemV3, firstHandle + ".3");
    }

    private void containsHandle(Item item, String handle) {
        assertTrue(item.getHandles().stream().anyMatch(h -> handle.equals(h.getHandle())));
    }
}
