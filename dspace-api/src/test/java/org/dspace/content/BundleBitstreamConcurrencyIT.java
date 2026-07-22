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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests that reproduce the race condition in the bundle2bitstream table.
 *
 * The bundle2bitstream table uses a bitstream_order column managed by JPA's
 * '@OrderColumn'. When two concurrent database transactions each modify the same bundle's
 * bitstream list, the bitstream_order values can become inconsistent.
 * Hibernate then returns a List containing null} elements for every
 * gap in the order sequence, which eventually causes a NullPointerException in callers.
 *
 * Each test reproduces the bug by simulating the classic stale-read race condition:
 *  - Thread A loads the bundle (and its bitstream collection) into its Hibernate cache.
 *  - Thread B modifies the bundle and commits, the DB now differs from Thread A's cache.
 *  - Thread A modifies the bundle using its stale in-memory state and commits.
 *
 * Without the fix (pessimistic locking + refresh), step 3 writes SQL based on the stale order,
 * leaving gaps. With the fix, Thread A acquires a SELECT FOR UPDATE lock on the bundle
 * row and refreshes the collection from DB before modifying, ensuring gap-free writes.
 */
public class BundleBitstreamConcurrencyIT extends AbstractIntegrationTestWithDatabase {

    private BundleService bundleService;
    private BitstreamService bitstreamService;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        bundleService = ContentServiceFactory.getInstance().getBundleService();
        bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    }

    // Test 1: Two concurrent removes of different bitstreams
    @Test
    public void testConcurrentRemoveTwoBitstreams() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Community").build();
        Collection col = CollectionBuilder.createCollection(context, parentCommunity).withName("Col").build();
        Item item = ItemBuilder.createItem(context, col).withTitle("Item").build();
        Bundle bundle = BundleBuilder.createBundle(context, item).withName("ORIGINAL").build();

        UUID bs0Id = addBitstreamToBundle(bundle, "bs0");
        UUID bs1Id = addBitstreamToBundle(bundle, "bs1");
        UUID bs2Id = addBitstreamToBundle(bundle, "bs2");
        UUID bs3Id = addBitstreamToBundle(bundle, "bs3");
        UUID bs4Id = addBitstreamToBundle(bundle, "bs4");
        UUID bundleId = bundle.getID();

        context.commit();

        CountDownLatch threadALoaded = new CountDownLatch(1);
        CountDownLatch threadBDone   = new CountDownLatch(1);

        AtomicReference<Throwable> threadAError = new AtomicReference<>();
        AtomicReference<Throwable> threadBError = new AtomicReference<>();

        // Thread A: load bundle into L1 cache, then wait, then remove bs3 with stale state
        Thread threadA = new Thread(() -> {
            try (Context ctx = new Context(Context.Mode.READ_WRITE)) {
                ctx.setCurrentUser(eperson);
                ctx.turnOffAuthorisationSystem();

                Bundle staleBundle = bundleService.find(ctx, bundleId);
                staleBundle.getBitstreams(); // force-initialise the lazy collection into L1 cache

                threadALoaded.countDown();                   // signal: collection loaded
                threadBDone.await(30, TimeUnit.SECONDS);    // wait for Thread B to commit

                Bitstream bs3 = bitstreamService.find(ctx, bs3Id);
                bundleService.removeBitstream(ctx, staleBundle, bs3);
                context.commit();

            } catch (Throwable e) {
                threadAError.set(e);
            }
        });

        // Thread B: wait for Thread A to have loaded the bundle, then remove bs1 and commit first
        Thread threadB = new Thread(() -> {
            try (Context ctx = new Context(Context.Mode.READ_WRITE)) {
                ctx.setCurrentUser(eperson);
                ctx.turnOffAuthorisationSystem();

                threadALoaded.await(30, TimeUnit.SECONDS);  // wait for Thread A to load

                Bundle freshBundle = bundleService.find(ctx, bundleId);
                Bitstream bs1 = bitstreamService.find(ctx, bs1Id);
                bundleService.removeBitstream(ctx, freshBundle, bs1);
                context.commit();

                threadBDone.countDown(); // signal: committed
            } catch (Throwable e) {
                threadBError.set(e);
            }
        });

        threadA.start();
        threadB.start();
        threadA.join(30_000);
        threadB.join(30_000);

        rethrowThreadErrors(threadAError, threadBError);

        // Three bitstreams should remain (bs0, bs2, bs4); the list must contain no nulls.
        try (Context ctx = new Context(Context.Mode.READ_ONLY)) {
            Bundle result = bundleService.find(ctx, bundleId);
            List<Bitstream> bitstreams = result.getBitstreams();

            assertFalse("Bundle bitstream list contains null, bitstream_order has gaps (concurrent remove bug)",
                        bitstreams.contains(null));
            assertEquals("Expected 3 bitstreams to remain after removing bs1 and bs3", 3, bitstreams.size());
            assertNoBitstreamOrderGaps(bundleId);
        }
    }

    // Test 2: Concurrent add + remove
    @Test
    public void testConcurrentAddAndRemove() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Community").build();
        Collection col = CollectionBuilder.createCollection(context, parentCommunity).withName("Col").build();
        Item item = ItemBuilder.createItem(context, col).withTitle("Item").build();
        Bundle bundle = BundleBuilder.createBundle(context, item).withName("ORIGINAL").build();

        UUID bs0Id = addBitstreamToBundle(bundle, "bs0");
        UUID bs1Id = addBitstreamToBundle(bundle, "bs1");
        UUID bs2Id = addBitstreamToBundle(bundle, "bs2");
        UUID bundleId = bundle.getID();

        context.commit();

        CountDownLatch threadALoaded = new CountDownLatch(1);
        CountDownLatch threadBDone   = new CountDownLatch(1);

        AtomicReference<Throwable> threadAError = new AtomicReference<>();
        AtomicReference<Throwable> threadBError = new AtomicReference<>();

        // Thread A: load bundle, wait, then remove bs1 with stale state
        Thread threadA = new Thread(() -> {
            try (Context ctx = new Context(Context.Mode.READ_WRITE)) {
                ctx.setCurrentUser(eperson);
                ctx.turnOffAuthorisationSystem();

                Bundle staleBundle = bundleService.find(ctx, bundleId);
                staleBundle.getBitstreams(); // force-init collection

                threadALoaded.countDown();
                threadBDone.await(30, TimeUnit.SECONDS);

                Bitstream bs1 = bitstreamService.find(ctx, bs1Id);
                bundleService.removeBitstream(ctx, staleBundle, bs1);
                ctx.commit();

            } catch (Throwable e) {
                threadAError.set(e);
            }
        });

        // Thread B: wait for Thread A to load, then add a new bitstream and commit first
        Thread threadB = new Thread(() -> {
            try (Context ctx = new Context(Context.Mode.READ_WRITE)) {
                ctx.setCurrentUser(eperson);
                ctx.turnOffAuthorisationSystem();

                threadALoaded.await(30, TimeUnit.SECONDS);

                Bundle freshBundle = bundleService.find(ctx, bundleId);
                BitstreamBuilder.createBitstream(ctx, freshBundle,
                    new ByteArrayInputStream("new file content".getBytes(StandardCharsets.UTF_8))).build();
                ctx.commit();


                threadBDone.countDown();
            } catch (Throwable e) {
                threadBError.set(e);
            }
        });

        threadA.start();
        threadB.start();
        threadA.join(30_000);
        threadB.join(30_000);

        rethrowThreadErrors(threadAError, threadBError);

        // Assertions
        // Net result: 3 bitstreams (bs0, bs2, bs_new). The list must have no null gaps.
        try (Context ctx = new Context(Context.Mode.READ_ONLY)) {
            Bundle result = bundleService.find(ctx, bundleId);
            List<Bitstream> bitstreams = result.getBitstreams();

            assertFalse("Bundle bitstream list contains null, bitstream_order has gaps (concurrent add+remove bug)",
                        bitstreams.contains(null));
            assertEquals("Expected 3 bitstreams to remain (bs0, bs2, bs_new)", 3, bitstreams.size());
            assertNoBitstreamOrderGaps(bundleId);
        }
    }

    private UUID addBitstreamToBundle(Bundle bundle, String name) throws Exception {
        Bitstream bs = BitstreamBuilder.createBitstream(context, bundle,
                new ByteArrayInputStream(name.getBytes(StandardCharsets.UTF_8))).build();
        return bs.getID();
    }

    /**
     * Verifies that the  bitstream_order} values in  bundle2bitstream} are exactly
     *  0, 1, 2, … with no gaps or duplicates.
     */
    private void assertNoBitstreamOrderGaps(UUID bundleId) throws Exception {
        DataSource dataSource = DSpaceServicesFactory.getInstance()
            .getServiceManager()
            .getServiceByName("dataSource", DataSource.class);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            String sql = "SELECT bitstream_order FROM bundle2bitstream "
                + "WHERE bundle_id = '" + bundleId + "' "
                + "ORDER BY bitstream_order";
            try (ResultSet rs = stmt.executeQuery(sql)) {
                List<Integer> orders = new ArrayList<>();
                while (rs.next()) {
                    orders.add(rs.getInt(1));
                }
                for (int i = 0; i < orders.size(); i++) {
                    assertEquals(
                        "bitstream_order is not contiguous at index " + i
                            + ". Actual orders: " + orders,
                        i, (int) orders.get(i));
                }
            }
        }
    }

    private void rethrowThreadErrors(AtomicReference<Throwable>... errors) throws Exception {
        for (AtomicReference<Throwable> error : errors) {
            if (error.get() != null) {
                Throwable t = error.get();
                if (t instanceof Exception) {
                    throw (Exception) t;
                }
                throw new RuntimeException(t);
            }
        }
    }
}
