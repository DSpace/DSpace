/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link org.dspace.content.BitstreamServiceImpl#expunge}.
 * Verifies that expunge() defensively cleans up FK references before
 * hard-deleting, preventing constraint violations from orphaned rows.
 *
 * @author Bram Luyten (bram at atmire.com)
 */
public class BitstreamExpungeIT extends AbstractIntegrationTestWithDatabase {

    private BitstreamService bitstreamService =
        ContentServiceFactory.getInstance().getBitstreamService();
    private Collection collection;

    @Before
    public void setup() {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).build();
        collection = CollectionBuilder.createCollection(context, parentCommunity).build();
        context.restoreAuthSystemState();
    }

    /**
     * Test that expunge() succeeds on a soft-deleted bitstream.
     * The defensive cleanup in expunge() should handle any remaining
     * bundle2bitstream, requestitem, and policy references.
     */
    @Test
    public void testExpungeDeletedBitstream() throws Exception {
        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .build();

        InputStream is = IOUtils.toInputStream("test content", "UTF-8");
        Bitstream bitstream = BitstreamBuilder
            .createBitstream(context, item, is)
            .build();
        UUID bitstreamId = bitstream.getID();

        // Soft-delete the bitstream
        bitstreamService.delete(context, bitstream);
        context.commit();

        // Re-fetch in clean session state
        bitstream = bitstreamService.find(context, bitstreamId);
        assertNotNull("Bitstream should still exist after soft-delete", bitstream);
        assertTrue("Bitstream should be marked as deleted", bitstream.isDeleted());

        // expunge() should succeed without FK constraint violations
        bitstreamService.expunge(context, bitstream);
        context.commit();

        // Verify the bitstream is fully removed
        bitstream = bitstreamService.find(context, bitstreamId);
        assertNull("Bitstream should not exist after expunge", bitstream);

        context.restoreAuthSystemState();
    }
}
