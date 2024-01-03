/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class is customization of the BitstreamStorageServiceImpl class.
 * The bitstream is synchronized if it is stored in both S3 and local assetstore.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class SyncBitstreamStorageServiceImpl extends BitstreamStorageServiceImpl {

    /**
     * log4j log
     */
    private static final Logger log = LogManager.getLogger();
    private boolean syncEnabled = false;

    public static final int SYNCHRONIZED_STORES_NUMBER = 77;

    @Autowired
    ConfigurationService configurationService;

    public SyncBitstreamStorageServiceImpl() {
        super();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for (Map.Entry<Integer, BitStoreService> storeEntry : getStores().entrySet()) {
            if (storeEntry.getValue().isEnabled() && !storeEntry.getValue().isInitialized()) {
                storeEntry.getValue().init();
            }
        }
        this.syncEnabled = configurationService.getBooleanProperty("sync.storage.service.enabled", false);
    }

    @Override
    public UUID store(Context context, Bitstream bitstream, InputStream is) throws SQLException, IOException {
        // Create internal ID
        String id = Utils.generateKey();
        /*
         * Set the store number of the new bitstream If you want to use some
         * other method of working out where to put a new bitstream, here's
         * where it should go
         */
        if (syncEnabled) {
            bitstream.setStoreNumber(SYNCHRONIZED_STORES_NUMBER);
        } else {
            bitstream.setStoreNumber(getIncoming());
        }
        bitstream.setDeleted(true);
        bitstream.setInternalId(id);


        BitStoreService store = this.getStore(getIncoming());
        //For efficiencies sake, PUT is responsible for setting bitstream size_bytes, checksum, and checksum_algorithm
        store.put(bitstream, is);
        //bitstream.setSizeBytes(file.length());
        //bitstream.setChecksum(Utils.toHex(dis.getMessageDigest().digest()));
        //bitstream.setChecksumAlgorithm("MD5");

        bitstream.setDeleted(false);
        try {
            //Update our bitstream but turn off the authorization system since permissions haven't been set at this
            // point in time.
            context.turnOffAuthorisationSystem();
            bitstreamService.update(context, bitstream);
        } catch (AuthorizeException e) {
            log.error(e);
            //Can never happen since we turn off authorization before we update
        } finally {
            context.restoreAuthSystemState();
        }

        UUID bitstreamId = bitstream.getID();

        if (log.isDebugEnabled()) {
            log.debug("Stored bitstreamID " + bitstreamId);
        }

        return bitstreamId;
    }

    /**
     * Register a bitstream already in storage.
     *
     * @param context       The current context
     * @param assetstore    The assetstore number for the bitstream to be
     *                      registered
     * @param bitstreamPath The relative path of the bitstream to be registered.
     *                      The path is relative to the path of ths assetstore.
     * @return The ID of the registered bitstream
     * @throws SQLException If a problem occurs accessing the RDBMS
     * @throws IOException  if IO error
     */
    @Override
    public UUID register(Context context, Bitstream bitstream, int assetstore,
                         String bitstreamPath) throws SQLException, IOException, AuthorizeException {

        // mark this bitstream as a registered bitstream
        String sInternalId = REGISTERED_FLAG + bitstreamPath;

        // Create a deleted bitstream row, using a separate DB connection
        bitstream.setDeleted(true);
        bitstream.setInternalId(sInternalId);
        if (syncEnabled) {
            bitstream.setStoreNumber(SYNCHRONIZED_STORES_NUMBER);
        } else {
            bitstream.setStoreNumber(assetstore);
        }
        bitstreamService.update(context, bitstream);

        List<String> wantedMetadata = List.of("size_bytes", "checksum", "checksum_algorithm");
        Map<String, Object> receivedMetadata = this.getStore(assetstore).about(bitstream, wantedMetadata);

        if (MapUtils.isEmpty(receivedMetadata)) {
            String message = "Not able to register bitstream:" + bitstream.getID() + " at path: " + bitstreamPath;
            log.error(message);
            throw new IOException(message);
        } else {
            if (receivedMetadata.containsKey("checksum_algorithm")) {
                bitstream.setChecksumAlgorithm(receivedMetadata.get("checksum_algorithm").toString());
            }

            if (receivedMetadata.containsKey("checksum")) {
                bitstream.setChecksum(receivedMetadata.get("checksum").toString());
            }

            if (receivedMetadata.containsKey("size_bytes")) {
                bitstream.setSizeBytes(Long.valueOf(receivedMetadata.get("size_bytes").toString()));
            }
        }

        bitstream.setDeleted(false);
        bitstreamService.update(context, bitstream);

        UUID bitstreamId = bitstream.getID();
        if (log.isDebugEnabled()) {
            log.debug("Registered bitstream " + bitstreamId + " at location " + bitstreamPath);
        }
        return bitstreamId;
    }

    @Override
    public Map computeChecksum(Context context, Bitstream bitstream) throws IOException {
        int storeNumber = this.whichStoreNumber(bitstream);
        return this.getStore(storeNumber).about(bitstream, List.of("checksum", "checksum_algorithm"));
    }

    /**
     * Compute the checksum of a bitstream in a specific store.
     * @param context DSpace Context object
     * @param bitstream Bitstream to compute checksum for
     * @param storeNumber Store number to compute checksum for
     * @return Map with checksum and checksum algorithm
     * @throws IOException if IO error
     */
    public Map computeChecksumSpecStore(Context context, Bitstream bitstream, int storeNumber) throws IOException {
        return this.getStore(storeNumber).about(bitstream, List.of("checksum", "checksum_algorithm"));
    }

    @Override
    public InputStream retrieve(Context context, Bitstream bitstream)
            throws SQLException, IOException {
        int storeNumber = this.whichStoreNumber(bitstream);
        return this.getStore(storeNumber).get(bitstream);
    }

    @Override
    public void cleanup(boolean deleteDbRecords, boolean verbose) throws SQLException, IOException, AuthorizeException {
        Context context = new Context(Context.Mode.BATCH_EDIT);

        int offset = 0;
        int limit = 100;

        int cleanedBitstreamCount = 0;

        int deletedBitstreamCount = bitstreamService.countDeletedBitstreams(context);
        System.out.println("Found " + deletedBitstreamCount + " deleted bistream to cleanup");

        try {
            context.turnOffAuthorisationSystem();

            while (cleanedBitstreamCount < deletedBitstreamCount) {

                List<Bitstream> storage = bitstreamService.findDeletedBitstreams(context, limit, offset);

                if (CollectionUtils.isEmpty(storage)) {
                    break;
                }

                for (Bitstream bitstream : storage) {
                    UUID bid = bitstream.getID();
                    List<String> wantedMetadata = List.of("size_bytes", "modified");
                    int storeNumber = this.whichStoreNumber(bitstream);
                    Map<String, Object> receivedMetadata = this.getStore(storeNumber)
                            .about(bitstream, wantedMetadata);


                    // Make sure entries which do not exist are removed
                    if (MapUtils.isEmpty(receivedMetadata)) {
                        log.debug("bitstore.about is empty, so file is not present");
                        if (deleteDbRecords) {
                            log.debug("deleting record");
                            if (verbose) {
                                System.out.println(" - Deleting bitstream information (ID: " + bid + ")");
                            }
                            checksumHistoryService.deleteByBitstream(context, bitstream);
                            if (verbose) {
                                System.out.println(" - Deleting bitstream record from database (ID: " + bid + ")");
                            }
                            bitstreamService.expunge(context, bitstream);
                        }
                        context.uncacheEntity(bitstream);
                        continue;
                    }

                    // This is a small chance that this is a file which is
                    // being stored -- get it next time.
                    if (isRecent(Long.valueOf(receivedMetadata.get("modified").toString()))) {
                        log.debug("file is recent");
                        context.uncacheEntity(bitstream);
                        continue;
                    }

                    if (deleteDbRecords) {
                        log.debug("deleting db record");
                        if (verbose) {
                            System.out.println(" - Deleting bitstream information (ID: " + bid + ")");
                        }
                        checksumHistoryService.deleteByBitstream(context, bitstream);
                        if (verbose) {
                            System.out.println(" - Deleting bitstream record from database (ID: " + bid + ")");
                        }
                        bitstreamService.expunge(context, bitstream);
                    }

                    if (isRegisteredBitstream(bitstream.getInternalId())) {
                        context.uncacheEntity(bitstream);
                        continue; // do not delete registered bitstreams
                    }


                    // Since versioning allows for multiple bitstreams, check if the internal
                    // identifier isn't used on
                    // another place
                    if (bitstreamService.findDuplicateInternalIdentifier(context, bitstream).isEmpty()) {
                        this.getStore(storeNumber).remove(bitstream);

                        String message = ("Deleted bitstreamID " + bid + ", internalID " + bitstream.getInternalId());
                        if (log.isDebugEnabled()) {
                            log.debug(message);
                        }
                        if (verbose) {
                            System.out.println(message);
                        }
                    }

                    context.uncacheEntity(bitstream);
                }

                // Commit actual changes to DB after dispatch events
                System.out.print("Performing incremental commit to the database...");
                context.commit();
                System.out.println(" Incremental commit done!");

                cleanedBitstreamCount = cleanedBitstreamCount + storage.size();

                if (!deleteDbRecords) {
                    offset = offset + limit;
                }

            }

            System.out.print("Committing changes to the database...");
            context.complete();
            System.out.println(" Done!");
        } catch (SQLException | IOException sqle) {
            // Aborting will leave the DB objects around, even if the
            // bitstreams are deleted. This is OK; deleting them next
            // time around will be a no-op.
            if (verbose) {
                System.err.println("Error: " + sqle.getMessage());
            }
            context.abort();
            throw sqle;
        } finally {
            context.restoreAuthSystemState();
        }
    }

    @Nullable
    @Override
    public Long getLastModified(Bitstream bitstream) throws IOException {
        int storeNumber = this.whichStoreNumber(bitstream);
        Map<String, Object> metadata = this.getStore(storeNumber).about(bitstream, List.of("modified"));
        if (metadata == null || !metadata.containsKey("modified")) {
            return null;
        }
        return Long.valueOf(metadata.get("modified").toString());
    }

    /**
     * Decide which store number should be used for the given bitstream.
     * If the bitstream is synchronized (stored in to S3 and local), then the static store number is used.
     * Otherwise, the bitstream's store number is used.
     *
     * @param bitstream bitstream
     * @return store number
     */
    public int whichStoreNumber(Bitstream bitstream) {
        if (isBitstreamStoreSynchronized(bitstream)) {
            return getIncoming();
        } else {
            return bitstream.getStoreNumber();
        }
    }

    /**
     * Check if the bitstream is synchronized (stored in more stores)
     * The bitstream is synchronized if it has the static store number.
     *
     * @param bitstream to check if it is synchronized
     * @return true if the bitstream is synchronized
     */
    public boolean isBitstreamStoreSynchronized(Bitstream bitstream) {
        return bitstream.getStoreNumber() == SYNCHRONIZED_STORES_NUMBER;
    }


    /**
     * Get the store number where the bitstream is synchronized. It is not active (incoming) store.
     *
     * @param bitstream to get the synchronized store number
     * @return store number
     */
    public int getSynchronizedStoreNumber(Bitstream bitstream) {
        int storeNumber = -1;
        if (!isBitstreamStoreSynchronized(bitstream)) {
            storeNumber = bitstream.getStoreNumber();
        }

        for (Map.Entry<Integer, BitStoreService> storeEntry : getStores().entrySet()) {
            if (storeEntry.getKey() == SYNCHRONIZED_STORES_NUMBER || storeEntry.getKey() == getIncoming()) {
                continue;
            }
            storeNumber = storeEntry.getKey();
        }
        return storeNumber;
    }

}
