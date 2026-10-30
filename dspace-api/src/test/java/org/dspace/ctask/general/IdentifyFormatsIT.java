/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.curate.Curator;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Test;

/**
 * Integration test for the {@link IdentifyFormats} curation task, which re-identifies
 * bitstream formats and corrects bitstreams that were stored as "Unknown".
 */
public class IdentifyFormatsIT extends AbstractIntegrationTestWithDatabase {

    private static final String P_TASK_DEF = "plugin.named.org.dspace.curate.CurationTask";
    private static final String TASK_NAME = "identifyformats";

    private final ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();
    private final BitstreamFormatService bitstreamFormatService =
        ContentServiceFactory.getInstance().getBitstreamFormatService();

    /**
     * A bitstream whose PDF content was stored as "Unknown" (because its name had no usable
     * extension) is corrected to Adobe PDF by re-identifying it from its content.
     */
    @Test
    public void testCorrectsUnknownFormat() throws Exception {
        // Register the task dynamically so the test does not depend on curate.cfg
        CoreServiceFactory.getInstance().getPluginService().clearNamedPluginClasses();
        String[] previousTaskDef = configurationService.getArrayProperty(P_TASK_DEF);
        configurationService.setProperty(P_TASK_DEF,
            IdentifyFormats.class.getCanonicalName() + " = " + TASK_NAME);

        try {
            context.turnOffAuthorisationSystem();
            parentCommunity = CommunityBuilder.createCommunity(context).build();
            Collection collection = CollectionBuilder.createCollection(context, parentCommunity).build();
            Item item = ItemBuilder.createItem(context, collection).build();

            // Add a PDF but give it a name with no usable extension, so it is stored as "Unknown".
            Bitstream bitstream;
            File pdf = new File(testProps.get("test.bitstream").toString());
            try (InputStream is = new FileInputStream(pdf)) {
                bitstream = BitstreamBuilder.createBitstream(context, item, is)
                                            .withName("mysteryfile")
                                            .build();
            }
            context.restoreAuthSystemState();

            // Precondition: the bitstream is currently unidentified.
            assertEquals("Unknown", bitstream.getFormat(context).getShortDescription());

            // Run the curation task as an administrator.
            context.setCurrentUser(admin);
            Curator curator = new Curator();
            curator.addTask(context, TASK_NAME);
            curator.curate(context, item);
            assertEquals("Curation should succeed", Curator.CURATE_SUCCESS, curator.getStatus(TASK_NAME));

            // The bitstream is now correctly identified as PDF from its content.
            Bitstream reloaded = context.reloadEntity(bitstream);
            assertEquals("Adobe PDF", reloaded.getFormat(context).getShortDescription());
        } finally {
            configurationService.setProperty(P_TASK_DEF, previousTaskDef);
        }
    }

    /**
     * A bitstream that is already correctly identified is left untouched (and by default a
     * non-Unknown bitstream is not even re-examined).
     */
    @Test
    public void testLeavesKnownFormatUntouched() throws Exception {
        CoreServiceFactory.getInstance().getPluginService().clearNamedPluginClasses();
        String[] previousTaskDef = configurationService.getArrayProperty(P_TASK_DEF);
        configurationService.setProperty(P_TASK_DEF,
            IdentifyFormats.class.getCanonicalName() + " = " + TASK_NAME);

        try {
            context.turnOffAuthorisationSystem();
            parentCommunity = CommunityBuilder.createCommunity(context).build();
            Collection collection = CollectionBuilder.createCollection(context, parentCommunity).build();
            Item item = ItemBuilder.createItem(context, collection).build();

            Bitstream bitstream;
            File pdf = new File(testProps.get("test.bitstream").toString());
            try (InputStream is = new FileInputStream(pdf)) {
                bitstream = BitstreamBuilder.createBitstream(context, item, is)
                                            .withName("document.pdf")
                                            .withMimeType("application/pdf")
                                            .build();
            }
            context.restoreAuthSystemState();

            assertEquals("Adobe PDF", bitstream.getFormat(context).getShortDescription());

            context.setCurrentUser(admin);
            Curator curator = new Curator();
            curator.addTask(context, TASK_NAME);
            curator.curate(context, item);
            assertEquals("Curation should succeed", Curator.CURATE_SUCCESS, curator.getStatus(TASK_NAME));

            Bitstream reloaded = context.reloadEntity(bitstream);
            assertEquals("Adobe PDF", reloaded.getFormat(context).getShortDescription());
        } finally {
            configurationService.setProperty(P_TASK_DEF, previousTaskDef);
        }
    }
}
