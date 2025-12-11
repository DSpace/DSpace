/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.curate.Curator;
import org.dspace.identifier.AbstractIdentifierProviderIT;
import org.dspace.identifier.IdentifierProvider;
import org.dspace.identifier.VersionedHandleIdentifierProvider;
import org.dspace.identifier.VersionedHandleIdentifierProviderWithCanonicalHandles;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Test;

/**
 * Rudimentary test of the curation task.
 *
 * @author mwood
 */
public class CreateMissingIdentifiersIT
    extends AbstractIdentifierProviderIT {

    private static final String P_TASK_DEF
            = "plugin.named.org.dspace.curate.CurationTask";
    private static final String TASK_NAME = "test";

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    @Test
    public void testPerform()
            throws IOException {
        // Must remove any cached named plugins before creating a new one
        CoreServiceFactory.getInstance().getPluginService().clearNamedPluginClasses();
        // Define a new task dynamically
        String[] prevTaskDef = configurationService.getArrayProperty(P_TASK_DEF);
        configurationService.setProperty(P_TASK_DEF,
                CreateMissingIdentifiers.class.getCanonicalName() + " = " + TASK_NAME);

        Curator curator = new Curator();
        curator.addTask(TASK_NAME);

        context.setCurrentUser(admin);
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .build();
        Item item = ItemBuilder.createItem(context, collection)
                               .build();

        /*
         * First, install an incompatible provider to make the task fail.
         */
        registerProvider(VersionedHandleIdentifierProviderWithCanonicalHandles.class);

        curator.curate(context, item);
        System.out.format("With incompatible provider, result is '%s'.\n",
                curator.getResult(TASK_NAME));
        assertEquals("Curation should fail", Curator.CURATE_ERROR,
                curator.getStatus(TASK_NAME));

        // Unregister this non-default provider
        unregisterProvider(VersionedHandleIdentifierProviderWithCanonicalHandles.class);
        // Re-register the default provider (for later tests which may depend on it)
        registerProvider(VersionedHandleIdentifierProvider.class);

        /*
         * Now, verify curate with default Handle Provider works
         * (and that our re-registration of the default provider above was successful)
         */
        curator.curate(context, item);
        int status = curator.getStatus(TASK_NAME);
        assertEquals("Curation should succeed", Curator.CURATE_SUCCESS, status);
        configurationService.setProperty(P_TASK_DEF, prevTaskDef);
    }

    @Test
    public void testCreationOfMissingHandles() throws IOException {
        // Must remove any cached named plugins before creating a new one
        CoreServiceFactory.getInstance().getPluginService().clearNamedPluginClasses();
        // Define a new task dynamically
        configurationService.setProperty(P_TASK_DEF,
                CreateMissingIdentifiers.class.getCanonicalName() + " = " + TASK_NAME);

        // deactivate all identifier provider
        List<IdentifierProvider> identifierProviders = identifierService.getProviders();
        List<Class> identifierProviderClasses =
                identifierProviders.stream().map(Object::getClass).distinct().collect(Collectors.toList());
        for (Class identifierProviderClass : identifierProviderClasses) {
            unregisterProvider(identifierProviderClass);
        }

        try {
            context.setCurrentUser(admin);
            parentCommunity = CommunityBuilder.createCommunity(context)
                    .build();
            Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                    .build();
            // create item and assert it did not got any handle
            Item item = ItemBuilder.createItem(context, collection)
                    .build();
            assertNull("Internal error in createMissingIdentifiersIT: item should not have a handle",
                    item.getHandle());

            // setup the curator
            Curator curator = new Curator();
            curator.addTask(TASK_NAME);

            // register the default Handle Provider
            registerProvider(VersionedHandleIdentifierProvider.class);

            /*
             * Now, verify curate with default Handle Provider works
             * (and that our re-registration of the default provider above was successful)
             * Use the uuid as reference to the item as the curation system takes handles and uuid as cli arguments.
             * Do not use the item reference, to be sure the curator and the curation task are able to work without
             * handle.
             */
            curator.curate(context, item.getID().toString());
            int status = curator.getStatus(TASK_NAME);
            assertEquals("Curation should succeed", Curator.CURATE_SUCCESS, status);
            // assure we got a handle
            assertNotNull("Curation task CreateMissingIdentifiers, did not assign a handle.", item.getHandle());
        } finally {
            // restore the identifierProviders for following tests
            for (Class identifierProviderClass : identifierProviderClasses) {
                registerProvider(identifierProviderClass);
            }
        }
    }
}
