/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.storage.bitstore.factory.StorageServiceFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class BitstreamStorageServiceImplIT extends AbstractIntegrationTestWithDatabase {
    private BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    private BitstreamStorageServiceImpl bitstreamStorageService =
        (BitstreamStorageServiceImpl) StorageServiceFactory.getInstance().getBitstreamStorageService();
    private Collection collection;

    private Map<Integer, BitStoreService> originalBitstores;

    private static final Integer SOURCE_STORE = 0;
    private static final Integer DEST_STORE = 1;

    @Rule
    public final TemporaryFolder tempStoreDir = new TemporaryFolder();

    @Before
    public void setup() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .build();

        originalBitstores = bitstreamStorageService.getStores();
        Map<Integer, BitStoreService> stores = new HashMap<>();
        DSBitStoreService sourceStore = new DSBitStoreService();
        sourceStore.setBaseDir(tempStoreDir.newFolder("src"));

        stores.put(SOURCE_STORE, sourceStore);
        bitstreamStorageService.setStores(stores);

        context.restoreAuthSystemState();
    }

    @After
    public void cleanUp() throws IOException {
        // Restore the bitstore storage stores
        bitstreamStorageService.setStores(originalBitstores);
    }

    /**
     * Test batch commit checkpointing, using the default batch commit size of 1
     *
     * @throws Exception if an exception occurs.
     */
    @Test
    public void testDefaultBatchCommitSize() throws Exception {
        Context context = this.context;

        // Destination assetstore fails after two bitstreams have been migrated
        DSBitStoreService destinationStore = new LimitedTempDSBitStoreService(tempStoreDir, 2);
        Map<Integer, BitStoreService> stores = bitstreamStorageService.getStores();
        stores.put(DEST_STORE, destinationStore);

        // Create three bitstreams in the source assetstore
        createBitstreams(context, 3);

        // Three bitstreams in source assetstore at the start
        assertThat(bitstreamService.countByStoreNumber(context, SOURCE_STORE).intValue(), equalTo(3));

        // No bitstreams in destination assetstore at the start
        assertThat(bitstreamService.countByStoreNumber(context, DEST_STORE).intValue(), equalTo(0));

        /// Commit any pending transaction to database
        context.commit();

        // Migrate bitstreams
        context.turnOffAuthorisationSystem();

        boolean deleteOld = false;
        Integer batchCommitSize = 1;
        try {
            bitstreamStorageService.migrate(
                context, SOURCE_STORE, DEST_STORE, deleteOld,
                batchCommitSize
            );
            fail("IOException should have been thrown");
        } catch (IOException ioe) {
            // Rollback any pending transaction
            context.rollback();
        }

        context.restoreAuthSystemState();

        // One bitstream should still be in the source assetstore, due to the
        // interrupted migration
        assertThat(bitstreamService.countByStoreNumber(context, SOURCE_STORE).intValue(), equalTo(1));

        // Two bitstreams should have migrated to the destination assetstore
        assertThat(bitstreamService.countByStoreNumber(context, DEST_STORE).intValue(), equalTo(2));
    }

    /**
     * Test batch commit checkpointing, using the default batch commit size of 3
     *
     * @throws Exception if an exception occurs.
     */
    @Test
    public void testBatchCommitSizeThree() throws Exception {
        Context context = this.context;

        // Destination assetstore fails after four bitstreams have been migrated
        DSBitStoreService destinationStore = new LimitedTempDSBitStoreService(tempStoreDir, 4);
        Map<Integer, BitStoreService> stores = bitstreamStorageService.getStores();
        stores.put(DEST_STORE, destinationStore);

        // Create five bitstreams in the source assetstore
        createBitstreams(context, 5);

        // Five bitstreams in source assetstore at the start
        assertThat(bitstreamService.countByStoreNumber(context, SOURCE_STORE).intValue(), equalTo(5));

        // No bitstreams in destination assetstore at the start
        assertThat(bitstreamService.countByStoreNumber(context, DEST_STORE).intValue(), equalTo(0));

        // Commit any pending transaction to database
        context.commit();

        // Migrate bitstreams
        context.turnOffAuthorisationSystem();

        boolean deleteOld = false;
        Integer batchCommitSize = 3;
        try {
            bitstreamStorageService.migrate(
                context, SOURCE_STORE, DEST_STORE, deleteOld,
                batchCommitSize
            );
            fail("IOException should have been thrown");
        } catch (IOException ioe) {
            // Rollback any pending transaction
            context.rollback();
        }

        context.restoreAuthSystemState();

        // Since the batch commit size is 3, only three bitstreams should be
        // marked as migrated, so there should still be two bitstreams
        // in the source assetstore, due to the interrupted migration
        assertThat(bitstreamService.countByStoreNumber(context, SOURCE_STORE).intValue(), equalTo(2));

        // Three bitstreams should have migrated to the destination assetstore
        assertThat(bitstreamService.countByStoreNumber(context, DEST_STORE).intValue(), equalTo(3));
    }

    private void createBitstreams(Context context, int numBitstreams)
        throws SQLException {
        context.turnOffAuthorisationSystem();
        for (int i = 0; i < numBitstreams; i++) {
            String content = "Test bitstream " + i;
            createBitstream(content);
        }
        context.restoreAuthSystemState();
        context.commit();
    }

    private Bitstream createBitstream(String content) {
        try {
            return BitstreamBuilder
                .createBitstream(context, createItem(), toInputStream(content))
                .build();
        } catch (SQLException | AuthorizeException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Item createItem() {
        return ItemBuilder.createItem(context, collection)
            .withTitle("Test item")
            .build();
    }


    private InputStream toInputStream(String content) {
        return IOUtils.toInputStream(content, UTF_8);
    }


    /**
     * DSBitStoreService variation that only allows a limited number of puts
     * to the bit store before throwing an IOException, to test the
     * error handling of the BitstreamStorageService.migrate() method.
     */
    class LimitedTempDSBitStoreService extends DSBitStoreService {
        // The number of put calls allowed before throwing an IOException
        protected int maxPuts = Integer.MAX_VALUE;

        // The number of "put" method class seen so far.
        protected int putCallCount = 0;

        /**
         * Constructor.
         *
         * @param maxPuts the number of put calls to allow before throwing an
         * IOException
         */
        public LimitedTempDSBitStoreService(TemporaryFolder tempStoreDir, int maxPuts) throws IOException {
            super();
            setBaseDir(tempStoreDir.newFolder());
            this.maxPuts = maxPuts;
        }

        /**
         * Store a stream of bits.
         *
         * After "maxPut" number of calls, this method throws an IOException.
         * @param in The stream of bits to store
         * @throws java.io.IOException If a problem occurs while storing the bits
         */
        @Override
        public void put(Bitstream bitstream, InputStream in) throws IOException {
            putCallCount = putCallCount + 1;
            if (putCallCount > maxPuts) {
                throw new IOException("Max 'put' method calls exceeded");
            } else {
                super.put(bitstream, in);
            }
        }
    }
}
