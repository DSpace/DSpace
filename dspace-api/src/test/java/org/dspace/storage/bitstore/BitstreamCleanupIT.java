/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.storage.bitstore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BitstreamCleanupIT extends AbstractIntegrationTestWithDatabase {
    Bitstream remaining;
    Bitstream deleted;

    boolean originalVersioning;
    long originalRecentHours;

    ConfigurationService configurationService;
    BitstreamService bitstreamService;

    @BeforeEach
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

    @AfterEach
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

        assertNotNull(remaining, "Remaining Bitstream should remain in the database");
        assertFalse(remaining.isDeleted(), "Remaining Bitstream should still be marked deleted=false");
        assertNotNull(deleted, "Deleted Bitstream should remain in the database");
        assertTrue(deleted.isDeleted(), "Deleted Bitstream should still be marked deleted=true");
        assertThrows(
            IOException.class,
            () -> ContentServiceFactory.getInstance().getBitstreamService().retrieve(context, deleted),
            "Deleted Bitstream content should be removed from the assetstore"
        );
    }

    @Test
    public void testCleanupVersioningEnabledExplicitDelete() throws Exception {
        new DSpace().getConfigurationService().setProperty("versioning.enabled", true);

        runCleanupScript("--delete");

        assertNotNull(remaining, "Remaining Bitstream should remain in the database");
        assertFalse(remaining.isDeleted(), "Remaining Bitstream should still be marked deleted=false");
        assertNull(deleted, "Deleted Bitstream should not remain in the database");
    }

    @Test
    public void testCleanupVersioningDisabledDefaultDelete() throws Exception {
        new DSpace().getConfigurationService().setProperty("versioning.enabled", false);

        runCleanupScript();

        assertNotNull(remaining, "Remaining Bitstream should remain in the database");
        assertFalse(remaining.isDeleted(), "Remaining Bitstream should still be marked deleted=false");
        assertNull(deleted, "Deleted Bitstream should not remain in the database");
    }

    @Test
    public void testCleanupVersioningDisabledExplicitLeave() throws Exception {
        new DSpace().getConfigurationService().setProperty("versioning.enabled", false);

        runCleanupScript("--leave");

        assertNotNull(remaining, "Remaining Bitstream should remain in the database");
        assertFalse(remaining.isDeleted(), "Remaining Bitstream should still be marked deleted=false");
        assertNotNull(deleted, "Deleted Bitstream should remain in the database");
        assertTrue(deleted.isDeleted(), "Deleted Bitstream should still be marked deleted=true");
        assertThrows(
            IOException.class,
            () -> ContentServiceFactory.getInstance().getBitstreamService().retrieve(context, deleted),
            "Deleted Bitstream content should be removed from the assetstore"
        );
    }

    @Test
    public void testCleanupComplainIfConfusing() throws Exception {
        assertThrows(
            IllegalArgumentException.class,
            () -> runCleanupScript("--leave", "--delete"),
            "Should throw IllegalArgumentException when both --leave and --delete are provided"
        );

        // Confirm that it did not affect the database or assetstore
        remaining = ContentServiceFactory.getInstance().getBitstreamService().find(context, remaining.getID());
        deleted = ContentServiceFactory.getInstance().getBitstreamService().find(context, deleted.getID());

        assertNotNull(remaining, "Remaining Bitstream should remain in the database");
        assertFalse(remaining.isDeleted(), "Remaining Bitstream should still be marked deleted=false");
        assertNotNull(deleted, "Deleted Bitstream should remain in the database");
        assertTrue(deleted.isDeleted(), "Deleted Bitstream should still be marked deleted=true");

        assertNotNull(
            ContentServiceFactory.getInstance().getBitstreamService().retrieve(context, deleted),
            "Deleted Bitstream content should not be removed from the assetstore"
        );
    }

    private void runCleanupScript(String... args) throws Exception {
        Cleanup.mainInternal(args);

        remaining = ContentServiceFactory.getInstance().getBitstreamService().find(context, remaining.getID());
        deleted = ContentServiceFactory.getInstance().getBitstreamService().find(context, deleted.getID());
    }
}
