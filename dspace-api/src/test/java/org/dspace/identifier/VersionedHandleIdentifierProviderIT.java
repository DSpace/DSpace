/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;
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
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;

public class VersionedHandleIdentifierProviderIT extends AbstractIdentifierProviderIT  {

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

        dspaceUrl = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.ui.url");
        // Clean out providers to avoid any being used for creation of community and collection

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        collection = CollectionBuilder.createCollection(context, parentCommunity)
                                      .withName("Collection")
                                      .build();

        context.restoreAuthSystemState();
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
}
