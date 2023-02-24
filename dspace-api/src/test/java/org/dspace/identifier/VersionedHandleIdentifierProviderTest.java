package org.dspace.identifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.SQLException;
import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.VersionBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.identifier.service.IdentifierService;
import org.dspace.kernel.ServiceManager;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class VersionedHandleIdentifierProviderTest extends AbstractIntegrationTestWithDatabase {
    private ServiceManager serviceManager;

    private String handlePrefix;

    private Collection collection;
    private Item itemV1;
    private Item itemV2;
    private Item itemV3;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();

        ConfigurationService configurationService = new DSpace().getConfigurationService();
        handlePrefix = configurationService.getProperty("handle.prefix");

        serviceManager = DSpaceServicesFactory.getInstance().getServiceManager();

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
        IdentifierServiceImpl identifierService = serviceManager.getServicesByType(IdentifierServiceImpl.class).get(0);
        identifierService.setProviders(List.of(identifierProvider));
    }

    private void createVersions() throws SQLException, AuthorizeException {
        itemV1 = ItemBuilder.createItem(context, collection)
                .withTitle("First version")
                .build();
        itemV2 = VersionBuilder.createVersion(context, itemV1, "Second version").build().getItem();
        itemV3 = VersionBuilder.createVersion(context, itemV1, "Third version").build().getItem();
    }

    @Test
    public void testDefaultVersionedHandleProvider() throws Exception {
        registerProvider(VersionedHandleIdentifierProvider.class);
        createVersions();

        assertEquals(handlePrefix + "/1", itemV1.getHandle());
        assertEquals(handlePrefix + "/1.2", itemV2.getHandle());
        assertEquals(handlePrefix + "/1.3", itemV3.getHandle());
    }

    @Test
    public void testCanonicalVersionedHandleProvider() throws Exception {
        registerProvider(VersionedHandleIdentifierProviderWithCanonicalHandles.class);
        createVersions();

        assertEquals(handlePrefix + "/1.3", itemV1.getHandle());
        assertEquals(handlePrefix + "/1.2", itemV2.getHandle());
        assertEquals(handlePrefix + "/1", itemV3.getHandle());
    }

    @After
    @Override
    public void destroy() throws Exception {
        super.destroy();
        // serviceManager.getApplicationContext().refresh();
    }
}
