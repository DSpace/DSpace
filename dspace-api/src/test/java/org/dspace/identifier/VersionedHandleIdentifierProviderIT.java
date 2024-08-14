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
import org.junit.Before;
import org.junit.Test;

public class VersionedHandleIdentifierProviderIT extends AbstractIntegrationTestWithDatabase {
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
        context.turnOffAuthorisationSystem();

        serviceManager = DSpaceServicesFactory.getInstance().getServiceManager();
        identifierService = serviceManager.getServicesByType(IdentifierServiceImpl.class).get(0);
        // Clean out providers to avoid any being used for creation of community and collection
        identifierService.setProviders(new ArrayList<>());

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        collection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection")
                .build();
    }

    private void registerProvider(Class type) {
        // Register our new provider
        serviceManager.registerServiceClass(type.getName(), type);
        IdentifierProvider identifierProvider =
                (IdentifierProvider) serviceManager.getServiceByName(type.getName(), type);

        // Overwrite the identifier-service's providers with the new one to ensure only this provider is used
        identifierService.setProviders(List.of(identifierProvider));
    }

    private void createVersions() throws SQLException, AuthorizeException {
        itemV1 = ItemBuilder.createItem(context, collection)
                .withTitle("First version")
                .build();
        firstHandle = itemV1.getHandle();
        itemV2 = VersionBuilder.createVersion(context, itemV1, "Second version").build().getItem();
        itemV3 = VersionBuilder.createVersion(context, itemV1, "Third version").build().getItem();
    }

    @Test
    public void testDefaultVersionedHandleProvider() throws Exception {
        registerProvider(VersionedHandleIdentifierProvider.class);
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
        registerProvider(VersionedHandleIdentifierProviderWithCanonicalHandles.class);
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
