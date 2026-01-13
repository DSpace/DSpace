/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.storage.bitstore;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharEncoding;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.junit.Test;

public class BitstreamCleanupIT extends AbstractIntegrationTestWithDatabase {
    Bitstream remaining;
    Bitstream deleted;

    boolean originalVersioning;
    long originalRecentHours;

    ConfigurationService configurationService;
    BitstreamService bitstreamService;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        configurationService = new DSpace().getConfigurationService();
        originalVersioning = configurationService.getBooleanProperty("versioning.enabled", true);
        originalRecentHours = configurationService.getLongProperty("bitstream.cleanup.isRecent.hours");
        configurationService.setProperty("bitstream.cleanup.isRecent.hours", 0L);

        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        Item item = ItemBuilder.createItem(context, collection).build();

        remaining
            = BitstreamBuilder.createBitstream(context, item, IOUtils.toInputStream("keep", CharEncoding.UTF_8))
                                              .build();
        deleted
            = BitstreamBuilder.createBitstream(context, item, IOUtils.toInputStream("delete", CharEncoding.UTF_8))
                              .build();

        bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
        bitstreamService.delete(context, deleted);
        context.commit();
    }

    @Override
    public void destroy() throws Exception {
        super.destroy();

        configurationService.setProperty("versioning.enabled", originalVersioning);
        configurationService.setProperty("bitstream.cleanup.isRecent.hours", originalRecentHours);
    }

    @Test
    public void testCleanupVersioningEnabledDefaultLeave() throws Exception {
        new DSpace().getConfigurationService().setProperty("versioning.enabled", true);

        runCleanupScript();

        assertNotNull("Remaining Bitstream should remain in the database", remaining);
        assertFalse("Remaining Bitstream should still be marked deleted=false", remaining.isDeleted());
        assertNotNull("Deleted Bitstream should remain in the database", deleted);
        assertTrue("Deleted Bitstream should still be marked deleted=true", deleted.isDeleted());
        assertThrows(
            "Deleted Bitstream content should be removed from the assetstore",
            IOException.class,
            () -> ContentServiceFactory.getInstance().getBitstreamService().retrieve(context, deleted)
        );
    }

    @Test
    public void testCleanupVersioningEnabledExplicitDelete() throws Exception {
        new DSpace().getConfigurationService().setProperty("versioning.enabled", true);

        runCleanupScript("--delete");

        assertNotNull("Remaining Bitstream should remain in the database", remaining);
        assertFalse("Remaining Bitstream should still be marked deleted=false", remaining.isDeleted());
        assertNull("Deleted Bitstream should not remain in the database", deleted);
    }

    @Test
    public void testCleanupVersioningDisabledDefaultDelete() throws Exception {
        new DSpace().getConfigurationService().setProperty("versioning.enabled", false);

        runCleanupScript();

        assertNotNull("Remaining Bitstream should remain in the database", remaining);
        assertFalse("Remaining Bitstream should still be marked deleted=false", remaining.isDeleted());
        assertNull("Deleted Bitstream should not remain in the database", deleted);
    }

    @Test
    public void testCleanupVersioningDisabledExplicitLeave() throws Exception {
        new DSpace().getConfigurationService().setProperty("versioning.enabled", false);

        runCleanupScript("--leave");

        assertNotNull("Remaining Bitstream should remain in the database", remaining);
        assertFalse("Remaining Bitstream should still be marked deleted=false", remaining.isDeleted());
        assertNotNull("Deleted Bitstream should remain in the database", deleted);
        assertTrue("Deleted Bitstream should still be marked deleted=true", deleted.isDeleted());
        assertThrows(
            "Deleted Bitstream content should be removed from the assetstore",
            IOException.class,
            () -> ContentServiceFactory.getInstance().getBitstreamService().retrieve(context, deleted)
        );
    }

    @Test
    public void testCleanupComplainIfConfusing() throws Exception {
        runCleanupScript("--leave", "--delete");
        // Note: the script will fail, but does not surface the exact Exception it fails with

        assertNotNull("Remaining Bitstream should remain in the database", remaining);
        assertFalse("Remaining Bitstream should still be marked deleted=false", remaining.isDeleted());
        assertNotNull("Deleted Bitstream should remain in the database", deleted);
        assertTrue("Deleted Bitstream should still be marked deleted=true", deleted.isDeleted());

        // Instead, we can confirm that it did not affect the assetstore
        assertNotNull(
            "Deleted Bitstream content should not be removed from the assetstore",
            ContentServiceFactory.getInstance().getBitstreamService().retrieve(context, deleted)
        );
    }

    private void runCleanupScript(String... args) throws Exception {
        runDSpaceScript(
            Stream.concat(Stream.of("cleanup"), Stream.of(args))
                  .toArray(String[]::new)
        );

        remaining = ContentServiceFactory.getInstance().getBitstreamService().find(context, remaining.getID());
        deleted = ContentServiceFactory.getInstance().getBitstreamService().find(context, deleted.getID());
    }
}
