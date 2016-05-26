/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore;

import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.checker.service.ChecksumHistoryService;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.storage.bitstore.service.BitstreamStorageService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.*;

/**
 * <P>
 * Stores, retrieves and deletes bitstreams.
 * </P>
 * 
 * <P>
 * Presently, asset stores are specified in <code>dspace.cfg</code>. Since
 * Java does not offer a way of detecting free disk space, the asset store to
 * use for new bitstreams is also specified in a configuration property. The
 * drawbacks to this are that the administrators are responsible for monitoring
 * available space in the asset stores, and DSpace (Tomcat) has to be restarted
 * when the asset store for new ('incoming') bitstreams is changed.
 * </P>
 * 
 * <P>
 * Mods by David Little, UCSD Libraries 12/21/04 to allow the registration of
 * files (bitstreams) into DSpace.
 * </P>
 * 
 * <p>Cleanup integration with checker package by Nate Sarr 2006-01. N.B. The 
 * dependency on the checker package isn't ideal - a Listener pattern would be 
 * better but was considered overkill for the purposes of integrating the checker.
 * It would be worth re-considering a Listener pattern if another package needs to 
 * be notified of BitstreamStorageManager actions.</p> 
 *
 * @author Peter Breton, Robert Tansley, David Little, Nathan Sarr
 * @version $Revision$
 */
public class BitstreamStorageServiceImpl implements BitstreamStorageService, InitializingBean
{
    /** log4j log */
    private static Logger log = Logger.getLogger(BitstreamStorageServiceImpl.class);

    @Autowired(required = true)
    protected BitstreamService bitstreamService;
    @Autowired(required = true)
    protected ChecksumHistoryService checksumHistoryService;

    /** asset stores */
    private Map<Integer, BitStoreService> stores = new HashMap<Integer, BitStoreService>();

    /** The index of the asset store to use for new bitstreams */
    private int incoming;

	/**
	 * This prefix string marks registered bitstreams in internal_id
	 */
	protected final String REGISTERED_FLAG = "-R";

    protected BitstreamStorageServiceImpl()
    {

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for(Map.Entry<Integer, BitStoreService> storeEntry : stores.entrySet()) {
            storeEntry.getValue().init();
        }
    }

    @Override
    public UUID store(Context context, Bitstream bitstream, InputStream is) throws SQLException, IOException
    {
        // Create internal ID
        String id = Utils.generateKey();

        bitstream.setDeleted(true);
        bitstream.setInternalId(id);

        /*
         * Set the store number of the new bitstream If you want to use some
         * other method of working out where to put a new bitstream, here's
         * where it should go
         */
        bitstream.setStoreNumber(incoming);

        //For efficiencies sake, PUT is responsible for setting bitstream size_bytes, checksum, and checksum_algorithm
        stores.get(incoming).put(bitstream, is);
        //bitstream.setSizeBytes(file.length());
        //bitstream.setChecksum(Utils.toHex(dis.getMessageDigest().digest()));
        //bitstream.setChecksumAlgorithm("MD5");
        
        bitstream.setDeleted(false);
        try {
            //Update our bitstream but turn off the authorization system since permissions haven't been set at this point in time.
            context.turnOffAuthorisationSystem();
            bitstreamService.update(context, bitstream);
        } catch (AuthorizeException e) {
            log.error(e);
            //Can never happen since we turn off authorization before we update
        } finally {
            context.restoreAuthSystemState();
        }

        UUID bitstreamId = bitstream.getID();

        if (log.isDebugEnabled())
        {
            log.debug("Stored bitstreamID " + bitstreamId);
        }

        return bitstreamId;
    }

	/**
	 * Register a bitstream already in storage.
	 *
	 * @param context
	 *            The current context
	 * @param assetstore The assetstore number for the bitstream to be
	 * 			registered
	 * @param bitstreamPath The relative path of the bitstream to be registered.
	 * 		The path is relative to the path of ths assetstore.
	 * @return The ID of the registered bitstream
	 * @exception SQLException
	 *                If a problem occurs accessing the RDBMS
	 * @throws IOException if IO error
	 */
	@Override
    public UUID register(Context context, Bitstream bitstream, int assetstore,
				String bitstreamPath) throws SQLException, IOException, AuthorizeException {

		// mark this bitstream as a registered bitstream
		String sInternalId = REGISTERED_FLAG + bitstreamPath;

		// Create a deleted bitstream row, using a separate DB connection
        bitstream.setDeleted(true);
        bitstream.setInternalId(sInternalId);
        bitstream.setStoreNumber(assetstore);
        bitstreamService.update(context, bitstream);

        Map wantedMetadata = new HashMap();
        wantedMetadata.put("size_bytes", null);
        wantedMetadata.put("checksum", null);
        wantedMetadata.put("checksum_algorithm", null);

        Map receivedMetadata = stores.get(assetstore).about(bitstream, wantedMetadata);
        if(MapUtils.isEmpty(receivedMetadata)) {
            String message = "Not able to register bitstream:" + bitstream.getID() + " at path: " + bitstreamPath;
            log.error(message);
            throw new IOException(message);
        } else {
            if(receivedMetadata.containsKey("checksum_algorithm")) {
                bitstream.setChecksumAlgorithm(receivedMetadata.get("checksum_algorithm").toString());
            }

            if(receivedMetadata.containsKey("checksum")) {
                bitstream.setChecksum(receivedMetadata.get("checksum").toString());
            }

            if(receivedMetadata.containsKey("size_bytes")) {
                bitstream.setSizeBytes(Long.valueOf(receivedMetadata.get("size_bytes").toString()));
            }
        }

		bitstream.setDeleted(false);
        bitstreamService.update(context, bitstream);

		UUID bitstreamId = bitstream.getID();
		if (log.isDebugEnabled())
		{
			log.debug("Registered bitstream " + bitstreamId + " at location " + bitstreamPath);
		}
		return bitstreamId;
	}

    @Override
    public Map computeChecksum(Context context, Bitstream bitstream) throws IOException {
        Map wantedMetadata = new HashMap();
        wantedMetadata.put("checksum", null);
        wantedMetadata.put("checksum_algorithm", null);

        Map receivedMetadata = stores.get(bitstream.getStoreNumber()).about(bitstream, wantedMetadata);
        return receivedMetadata;
    }

    @Override
    public boolean isRegisteredBitstream(String internalId) {
        return internalId.startsWith(REGISTERED_FLAG);
	}

    @Override
    public InputStream retrieve(Context context, Bitstream bitstream)
            throws SQLException, IOException
    {
        Integer storeNumber = bitstream.getStoreNumber();
        return stores.get(storeNumber).get(bitstream);
    }

    @Override
    public void cleanup(boolean deleteDbRecords, boolean verbose) throws SQLException, IOException, AuthorizeException {
        Context context = null;
        int commitCounter = 0;

        try
        {
            context = new Context();
            context.turnOffAuthorisationSystem();

            List<Bitstream> storage = bitstreamService.findDeletedBitstreams(context);
            for (Bitstream bitstream : storage)
            {
                UUID bid = bitstream.getID();
                Map wantedMetadata = new HashMap();
                wantedMetadata.put("size_bytes", null);
                wantedMetadata.put("modified", null);
                Map receivedMetadata = stores.get(bitstream.getStoreNumber()).about(bitstream, wantedMetadata);


                // Make sure entries which do not exist are removed
                if (MapUtils.isEmpty(receivedMetadata))
                {
                    log.debug("bitstore.about is empty, so file is not present");
                    if (deleteDbRecords)
                    {
                        log.debug("deleting record");
                        if (verbose)
                        {
                            System.out.println(" - Deleting bitstream information (ID: " + bid + ")");
                        }
                        checksumHistoryService.deleteByBitstream(context, bitstream);
                        if (verbose)
                        {
                            System.out.println(" - Deleting bitstream record from database (ID: " + bid + ")");
                        }
                        bitstreamService.expunge(context, bitstream);
                    }
                    continue;
                }

                // This is a small chance that this is a file which is
                // being stored -- get it next time.
                if (isRecent(Long.valueOf(receivedMetadata.get("modified").toString())))
                {
                	log.debug("file is recent");
                    continue;
                }

                if (deleteDbRecords)
                {
                    log.debug("deleting db record");
                    if (verbose)
                    {
                        System.out.println(" - Deleting bitstream information (ID: " + bid + ")");
                    }
                    checksumHistoryService.deleteByBitstream(context, bitstream);
                    if (verbose)
                    {
                        System.out.println(" - Deleting bitstream record from database (ID: " + bid + ")");
                    }
                    bitstreamService.expunge(context, bitstream);
                }

				if (isRegisteredBitstream(bitstream.getInternalId())) {
				    continue;			// do not delete registered bitstreams
				}


                // Since versioning allows for multiple bitstreams, check if the internal identifier isn't used on another place
                if(0 < bitstreamService.findDuplicateInternalIdentifier(context, bitstream).size())
                {
                    stores.get(bitstream.getStoreNumber()).remove(bitstream);

                    String message = ("Deleted bitstreamID " + bid + ", internalID " + bitstream.getInternalId());
                    if (log.isDebugEnabled())
                    {
                        log.debug(message);
                    }
                    if (verbose)
                    {
                        System.out.println(message);
                    }
                }

                // Make sure to commit our outstanding work every 100
                // iterations. Otherwise you risk losing the entire transaction
                // if we hit an exception, which isn't useful at all for large
                // amounts of bitstreams.
                commitCounter++;
                if (commitCounter % 100 == 0)
                {
                    context.dispatchEvents();
                }
            }

            System.out.print("Committing changes to the database...");
            context.complete();
            System.out.println(" Done!");
        }
        // Aborting will leave the DB objects around, even if the
        // bitstreams are deleted. This is OK; deleting them next
        // time around will be a no-op.
        catch (SQLException | IOException sqle)
        {
            if (verbose)
            {
                System.err.println("Error: " + sqle.getMessage());
            }
            context.abort();
            throw sqle;
        } finally {
            if(context != null){
                context.restoreAuthSystemState();
            }
        }
    }

    /**
     *
     * @param context
     * @param bitstream the bitstream to be cloned
     * @return id of the clone bitstream.
     * @throws SQLException if database error
     */
	@Override
    public Bitstream clone(Context context, Bitstream bitstream) throws SQLException, IOException, AuthorizeException {
		Bitstream clonedBitstream = bitstreamService.create(context, bitstreamService.retrieve(context, bitstream));
		List<MetadataValue> metadataValues = bitstreamService.getMetadata(bitstream, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
		for (MetadataValue metadataValue : metadataValues) {
			bitstreamService.addMetadata(context, clonedBitstream, metadataValue.getMetadataField(), metadataValue.getLanguage(), metadataValue.getValue(), metadataValue.getAuthority(), metadataValue.getConfidence());
		}
		return clonedBitstream;

	}

    /**
     * Migrates all assets off of one assetstore to another
     * @param assetstoreSource
     * @param assetstoreDestination
     */
    public void migrate(Context context, Integer assetstoreSource, Integer assetstoreDestination, boolean deleteOld, Integer batchCommitSize) throws IOException, SQLException, AuthorizeException {
        //Find all the bitstreams on the old source, copy it to new destination, update store_number, save, remove old
        Iterator<Bitstream> allBitstreamsInSource = bitstreamService.findByStoreNumber(context, assetstoreSource);
        Integer processedCounter = 0;

        while (allBitstreamsInSource.hasNext()) {
            Bitstream bitstream = allBitstreamsInSource.next();
            log.info("Copying bitstream:" + bitstream.getID() + " from assetstore[" + assetstoreSource + "] to assetstore[" + assetstoreDestination + "] Name:" + bitstream.getName() + ", SizeBytes:" + bitstream.getSize());

            InputStream inputStream = retrieve(context, bitstream);
            stores.get(assetstoreDestination).put(bitstream, inputStream);
            bitstream.setStoreNumber(assetstoreDestination);
            bitstreamService.update(context, bitstream);

            if (deleteOld) {
                log.info("Removing bitstream:" + bitstream.getID() + " from assetstore[" + assetstoreSource + "]");
                stores.get(assetstoreSource).remove(bitstream);
            }

            processedCounter++;
            //modulo
            if ((processedCounter % batchCommitSize) == 0) {
                log.info("Migration Commit Checkpoint: " + processedCounter);
                context.dispatchEvents();
            }
        }

        log.info("Assetstore Migration from assetstore[" + assetstoreSource + "] to assetstore[" + assetstoreDestination + "] completed. " + processedCounter + " objects were transferred.");
    }

    public void printStores(Context context) {
        try {

            for(Integer storeNumber : stores.keySet()) {
                long countBitstreams = bitstreamService.countByStoreNumber(context, storeNumber);
                System.out.println("store[" + storeNumber + "] == " + stores.get(storeNumber).getClass().getSimpleName() + ", which has " + countBitstreams + " bitstreams.");
            }
            System.out.println("Incoming assetstore is store[" + incoming + "]");
        } catch (SQLException e) {
            log.error(e);
        }
    }

    public int getIncoming() {
        return incoming;
    }

    public void setIncoming(int incoming) {
        this.incoming = incoming;
    }

    public void setStores(Map<Integer, BitStoreService> stores) {
        this.stores = stores;
    }

    public Map<Integer, BitStoreService> getStores() {
        return stores;
    }

    ////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////

    /**
     * Return true if this file is too recent to be deleted, false otherwise.
     * 
     * @param lastModified
     *            The time asset was last modified
     * @return True if this file is too recent to be deleted
     */
    protected boolean isRecent(Long lastModified)
    {
        long now = new java.util.Date().getTime();

        if (lastModified >= now)
        {
            return true;
        }

        // Less than one hour old
        return (now - lastModified) < (1 * 60 * 1000);
    }
}
