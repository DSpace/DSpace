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

import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.VersionBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.kernel.ServiceManager;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;

public class VersionedHandleIdentifierProviderWithCanonicalHandlesIT extends AbstractIdentifierProviderIT {
    private ServiceManager serviceManager;
    private IdentifierServiceImpl identifierService;

    private String firstHandle;
    private String dspaceUrl;


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
        dspaceUrl = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.ui.url");
        identifierService = serviceManager.getServicesByType(IdentifierServiceImpl.class).get(0);
        // Clean out providers to avoid any being used for creation of community and collection
        identifierService.setProviders(new ArrayList<>());

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        collection = CollectionBuilder.createCollection(context, parentCommunity)
                                      .withName("Collection")
                                      .build();

        registerProvider(VersionedHandleIdentifierProviderWithCanonicalHandles.class);

    }

    public void destroy() throws Exception {
        super.destroy();
        // Unregister this non-default provider
        unregisterProvider(VersionedHandleIdentifierProviderWithCanonicalHandles.class);
        // Re-register the default provider (for later tests)
        registerProvider(VersionedHandleIdentifierProvider.class);
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
    public void testCanonicalVersionedHandleProvider() throws Exception {
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

    @Test
    public void testCollectionHandleMetadata() {
        context.turnOffAuthorisationSystem();
        Community testCommunity = CommunityBuilder.createCommunity(context)
                                                  .withName("Test community")
                                                  .build();

        Collection testCollection = CollectionBuilder.createCollection(context, testCommunity)
                                                     .withName("Test Collection")
                                                     .build();
        context.restoreAuthSystemState();

        List<MetadataValue> metadata = ContentServiceFactory.getInstance().getDSpaceObjectService(testCollection)
                                                            .getMetadata(testCollection, "dc", "identifier", "uri",
                                                                         Item.ANY);

        assertEquals(1, metadata.size());
        assertEquals(dspaceUrl + "/handle/" + testCollection.getHandle(), metadata.get(0).getValue());
    }

    @Test
    public void testCommunityHandleMetadata() {
        context.turnOffAuthorisationSystem();
        Community testCommunity = CommunityBuilder.createCommunity(context)
                                                  .withName("Test community")
                                                  .build();
        context.restoreAuthSystemState();

        List<MetadataValue> metadata = ContentServiceFactory.getInstance().getDSpaceObjectService(testCommunity)
                                                            .getMetadata(testCommunity, "dc", "identifier", "uri",
                                                                         Item.ANY);

        assertEquals(1, metadata.size());
        assertEquals(dspaceUrl + "/handle/" + testCommunity.getHandle(), metadata.get(0).getValue());
    }

    private void containsHandle(Item item, String handle) {
        assertTrue(item.getHandles().stream().anyMatch(h -> handle.equals(h.getHandle())));
    }
}
