/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Iterator;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.requestitem.RequestItem;
import org.dspace.app.requestitem.factory.RequestItemServiceFactory;
import org.dspace.app.requestitem.service.RequestItemService;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RequestItemBuilder;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link BitstreamServiceImpl#expunge}.
 *
 * Verifies that expunge() defensively cleans up bundle2bitstream, requestitem,
 * and resourcepolicy references before the hard-delete, so historical orphaned
 * rows do not cause FK constraint violations.
 *
 * @author Bram Luyten (bram at atmire.com)
 */
public class BitstreamExpungeIT extends AbstractIntegrationTestWithDatabase {

    private final BitstreamService bitstreamService =
        ContentServiceFactory.getInstance().getBitstreamService();
    private final BundleService bundleService =
        ContentServiceFactory.getInstance().getBundleService();
    private final RequestItemService requestItemService =
        RequestItemServiceFactory.getInstance().getRequestItemService();
    private final AuthorizeService authorizeService =
        AuthorizeServiceFactory.getInstance().getAuthorizeService();

    private Collection collection;

    @Before
    public void setup() {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).build();
        collection = CollectionBuilder.createCollection(context, parentCommunity).build();
        context.restoreAuthSystemState();
    }

    /**
     * Smoke test the normal path: a bitstream that has been soft-deleted via
     * delete() can be expunged. delete() already cleans up FK references, so
     * expunge() should succeed without any defensive work.
     */
    @Test
    public void testExpungeAfterNormalDelete() throws Exception {
        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection).withTitle("Test item").build();
        Bitstream bitstream = BitstreamBuilder
            .createBitstream(context, item, toInputStream("test content"))
            .build();
        UUID bitstreamId = bitstream.getID();

        bitstreamService.delete(context, bitstream);
        context.commit();

        bitstream = bitstreamService.find(context, bitstreamId);
        assertTrue("Bitstream should be marked as deleted", bitstream.isDeleted());

        bitstreamService.expunge(context, bitstream);
        context.commit();

        assertNull("Bitstream should not exist after expunge",
            bitstreamService.find(context, bitstreamId));

        context.restoreAuthSystemState();
    }

    /**
     * Simulate the historical bug scenario: a bitstream is marked deleted but
     * its bundle2bitstream row was never cleaned up. expunge() must remove the
     * leftover bundle association so the hard-delete does not hit a FK
     * constraint violation.
     */
    @Test
    public void testExpungeOrphanedBundleReference() throws Exception {
        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection).withTitle("Test item").build();
        Bitstream bitstream = BitstreamBuilder
            .createBitstream(context, item, toInputStream("test content"))
            .build();
        UUID bitstreamId = bitstream.getID();
        UUID bundleId = bitstream.getBundles().get(0).getID();

        // Mark the bitstream as deleted WITHOUT going through delete(), so the
        // bundle relationship stays in place. This is the orphan state the PR
        // is designed to recover from.
        bitstream.setDeleted(true);
        bitstreamService.update(context, bitstream);
        context.commit();

        bitstream = bitstreamService.find(context, bitstreamId);
        assertEquals("Bundle association should still exist before expunge",
            1, bitstream.getBundles().size());

        bitstreamService.expunge(context, bitstream);
        context.commit();

        assertNull("Bitstream should be removed after expunge",
            bitstreamService.find(context, bitstreamId));

        Bundle bundle = bundleService.find(context, bundleId);
        assertFalse("Bundle should no longer reference the bitstream",
            bundle.getBitstreams().stream().anyMatch(b -> b.getID().equals(bitstreamId)));

        context.restoreAuthSystemState();
    }

    /**
     * Variant where the orphaned bitstream is also the bundle's primary
     * bitstream. expunge() must unset the primary bitstream pointer before
     * hard-deleting, otherwise the bundle_primary_bitstream_id_fkey constraint
     * fires.
     */
    @Test
    public void testExpungeOrphanedPrimaryBitstream() throws Exception {
        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection).withTitle("Test item").build();
        Bitstream bitstream = BitstreamBuilder
            .createBitstream(context, item, toInputStream("test content"))
            .build();
        UUID bitstreamId = bitstream.getID();
        Bundle bundle = bitstream.getBundles().get(0);
        UUID bundleId = bundle.getID();

        bundle.setPrimaryBitstreamID(bitstream);
        bundleService.update(context, bundle);

        bitstream.setDeleted(true);
        bitstreamService.update(context, bitstream);
        context.commit();

        // Re-fetch in a fresh session state, simulating the cleanup script
        // path which iterates deleted bitstreams in a separate session.
        bitstream = bitstreamService.find(context, bitstreamId);
        bitstreamService.expunge(context, bitstream);
        context.commit();

        assertNull("Bitstream should be removed after expunge",
            bitstreamService.find(context, bitstreamId));
        assertNull("Bundle should no longer have a primary bitstream",
            bundleService.find(context, bundleId).getPrimaryBitstream());

        context.restoreAuthSystemState();
    }

    /**
     * Variant where a RequestItem references the orphaned bitstream. expunge()
     * must remove the request item so the requestitem_bitstream_id_fkey
     * constraint does not fire on the hard-delete.
     */
    @Test
    public void testExpungeOrphanedRequestItem() throws Exception {
        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection).withTitle("Test item").build();
        Bitstream bitstream = BitstreamBuilder
            .createBitstream(context, item, toInputStream("test content"))
            .build();
        UUID bitstreamId = bitstream.getID();

        RequestItemBuilder.createRequestItem(context, item, bitstream).build();

        bitstream.setDeleted(true);
        bitstreamService.update(context, bitstream);
        context.commit();

        Iterator<RequestItem> before = requestItemService.findByBitstreamId(context, bitstreamId);
        assertTrue("RequestItem should exist before expunge", before.hasNext());

        bitstream = bitstreamService.find(context, bitstreamId);
        bitstreamService.expunge(context, bitstream);
        context.commit();

        assertNull("Bitstream should be removed after expunge",
            bitstreamService.find(context, bitstreamId));
        assertFalse("RequestItem should be removed after expunge",
            requestItemService.findByBitstreamId(context, bitstreamId).hasNext());

        context.restoreAuthSystemState();
    }

    /**
     * Verify that policies attached to the bitstream are removed by expunge(),
     * so resource policy rows do not survive the hard-delete.
     */
    @Test
    public void testExpungeRemovesPolicies() throws Exception {
        context.turnOffAuthorisationSystem();

        Item item = ItemBuilder.createItem(context, collection).withTitle("Test item").build();
        Bitstream bitstream = BitstreamBuilder
            .createBitstream(context, item, toInputStream("test content"))
            .build();
        UUID bitstreamId = bitstream.getID();

        assertFalse("Bitstream should have inherited policies",
            authorizeService.getPolicies(context, bitstream).isEmpty());

        bitstream.setDeleted(true);
        bitstreamService.update(context, bitstream);
        context.commit();

        bitstream = bitstreamService.find(context, bitstreamId);
        bitstreamService.expunge(context, bitstream);
        context.commit();

        assertNull("Bitstream should be removed after expunge",
            bitstreamService.find(context, bitstreamId));

        context.restoreAuthSystemState();
    }

    private InputStream toInputStream(String content) {
        return IOUtils.toInputStream(content, "UTF-8");
    }
}
