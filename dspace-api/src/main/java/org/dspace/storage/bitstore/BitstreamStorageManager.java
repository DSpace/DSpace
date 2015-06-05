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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import org.dspace.checker.BitstreamInfoDAO;
import org.dspace.content.Bitstream;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

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
 * @author Peter Breton, Robert Tansley, David Little, Nathan Sarr, Richard Rodgers, Peter Dietz
 * @version $Revision: 1.17 $
 */
public class BitstreamStorageManager
{
    /** log4j log */
    private static Logger log = Logger.getLogger(BitstreamStorageManager.class);
	
    /** asset stores */
	private static BitStore[] stores;

    /** The index of the asset store to use for new bitstreams */
    private static int incoming;

	/**
	 * This prefix string marks registered bitstreams in internal_id
	 */
	private static final String REGISTERED_FLAG = "-R";
	
	/** default asset store implementation */
	private static final String DEFAULT_STORE_PREFIX = "ds:";
	private static final String DEFAULT_STORE_IMPL = "org.dspace.storage.bitstore.impl.DSBitStore";

    /* Read in the asset stores from the config. */
    static
    {
    	ArrayList list = new ArrayList();

        // Load the backwards compatible assetstore.dir's
		list = initLegacyAssetstore(list);
		
		// if not already configured, configure asset stores
		for (int j = 0; j < 100; j++)
		{
			String assetCfg = ConfigurationManager.getProperty("assetstore." + j);
            log.info("Looking for config for assetstore." + j);
			if (assetCfg == null)
			{
				// no more stores configured - assumes sequential assignment
				break;
			}
			if (!list.contains(j))
			{
				initStore(assetCfg, list);
			}
		}

        log.info("LIST: " + ArrayUtils.toString(list));
		stores = (BitStore[])list.toArray(new BitStore[list.size()]);
        // Read asset store to put new files in. Default is 0.
        incoming = ConfigurationManager.getIntProperty("assetstore.incoming");
    }

    /**
     * Backwards compatibility for legacy assetstore.dir and assetstore.dir.#
     * @param list
     * @return
     */
    private static ArrayList initLegacyAssetstore(ArrayList list) {
        String storeDir = ConfigurationManager.getProperty("assetstore.dir");
        if (storeDir == null)
        {
            log.info("No default assetstore.dir");
        }
        else
        {
            initStore(DEFAULT_STORE_PREFIX + storeDir, list);
            // read any further ones, starting at 1, since assetstore.dir is 0
            for (int i = 1;; i++)
            {
                storeDir = ConfigurationManager.getProperty("assetstore.dir." + i);
                if (storeDir == null)
                {
                    break;
                }
                initStore(DEFAULT_STORE_PREFIX + storeDir, list);
            }
        }

        return list;
    }

    /**
     *
     * assetstore.<#>=<short-name>:<param>
     * bitstore.<short-name>.class=<implementation-class-canonical-name>
     * @param storeConfig
     * @param list
     */
    private static void initStore(String storeConfig, List list)
    {
        log.info("Init BitStore:" + storeConfig);
		// create and initialize an asset store
    	int split = storeConfig.indexOf(":");
    	if (split != -1)
    	{
    		String prefix = storeConfig.substring(0,split);
    		String config = storeConfig.substring(split+1);
    		String className = ConfigurationManager.getProperty("bitstore." + prefix + ".class");
    		if (className == null && DEFAULT_STORE_PREFIX.equals(prefix))
    		{
    			// use default implementation class if none explicitly defined
    			className = DEFAULT_STORE_IMPL;
    		}

            log.info("BitStore name: " + className);
    	    try
		    {
    		    BitStore store = (BitStore)Class.forName(className).newInstance();
    		    store.init(config);
    		    list.add(store);
		    }
    	    catch (Exception e)
		    {
    	    	log.error("Cannot instantiate store class: " + className + ", exception: " + e.getMessage(), e);
		    }
    	}
    }
    
    private static void updateBitstream(TableRow bitstream, Map attrs)
            throws IOException
	{
    	Iterator iter = attrs.keySet().iterator();
    	while (iter.hasNext())
        {
    		String column = (String)iter.next();
            Object value = attrs.get(column);
            if(value == null)
            {
                continue;
            }

            switch (column) {
                //int's
                case Bitstream.BITSTREAM_ID:
                case Bitstream.STORE_NUMBER:
                case Bitstream.SEQUENCE_ID:
                    bitstream.setColumn(column, (int) value);
                    break;
                //long's
                case Bitstream.SIZE_BYTES:
                    bitstream.setColumn(column, (long) value);
                    break;
                //String's
                case Bitstream.CHECKSUM:
                case Bitstream.CHECKSUM_ALGORITHM:
                case Bitstream.INTERNAL_ID:
                    bitstream.setColumn(column, (String) value);
                    break;
                //boolean's
                case Bitstream.DELETED:
                    bitstream.setColumn(column, (boolean) value);
                    break;
                default:
                    log.info("update bitstream didn't have a switch for col:" + column + " val:" + value);
                    break;
            }
    	}
	}

    /**
     * Store a stream of bits.
     * 
     * <p>
     * If this method returns successfully, the bits have been stored, and RDBMS
     * metadata entries are in place (the context still needs to be completed to
     * finalize the transaction).
     * </p>
     * 
     * <p>
     * If this method returns successfully and the context is aborted, then the
     * bits will be stored in the asset store and the RDBMS metadata entries
     * will exist, but with the deleted flag set.
     * </p>
     * 
     * If this method throws an exception, then any of the following may be
     * true:
     * 
     * <ul>
     * <li>Neither bits nor RDBMS metadata entries have been stored.
     * <li>RDBMS metadata entries with the deleted flag set have been stored,
     * but no bits.
     * <li>RDBMS metadata entries with the deleted flag set have been stored,
     * and some or all of the bits have also been stored.
     * </ul>
     * 
     * @param context
     *            The current context
     * @param is
     *            The stream of bits to store
     * @exception IOException
     *                If a problem occurs while storing the bits
     * @exception SQLException
     *                If a problem occurs accessing the RDBMS
     * 
     * @return The ID of the stored bitstream
     */
    public static int store(Context context, InputStream is)
            throws SQLException, IOException
    {    	
        // Create internal ID according to system used by current incoming store
        String id = stores[incoming].generateId();

        // Create a deleted bitstream row, using a separate DB connection
        TableRow bitstream;
        Context tempContext = null;

        try
        {
            tempContext = new Context();

            bitstream = DatabaseManager.create(tempContext, "Bitstream");
            bitstream.setColumn(Bitstream.DELETED, true);
            bitstream.setColumn(Bitstream.INTERNAL_ID, id);

            /*
             * Set the store number of the new bitstream If you want to use some
             * other method of working out where to put a new bitstream, here's
             * where it should go
             */
            bitstream.setColumn(Bitstream.STORE_NUMBER, incoming);

            DatabaseManager.update(tempContext, bitstream);

            tempContext.complete();
        }
        catch (SQLException sqle)
        {
            if (tempContext != null)
            {
                tempContext.abort();
            }

            throw sqle;
        }

        // write bits to underlying asset store
        Map attrs = stores[incoming].put(is, id);
        // update DB
        updateBitstream(bitstream, attrs);
        bitstream.setColumn(Bitstream.DELETED, false);
        DatabaseManager.update(context, bitstream);

        int bitstream_id = bitstream.getIntColumn(Bitstream.BITSTREAM_ID);

        if (log.isDebugEnabled())
        {
            log.debug("Stored bitstream " + bitstream_id + " under id " + id );
        }

        return bitstream_id;
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
	 * @throws IOException
	 */
	public static int register(Context context, int assetstore,	String bitstreamPath)
	    throws SQLException, IOException
	{
		// mark this bitstream as a registered bitstream
		String sInternalId = REGISTERED_FLAG + bitstreamPath;

		// Create a deleted bitstream row, using a separate DB connection
		TableRow bitstream;
		Context tempContext = null;

		try
		{
			tempContext = new Context();

			bitstream = DatabaseManager.create(tempContext, "Bitstream");
			bitstream.setColumn(Bitstream.DELETED, true);
			bitstream.setColumn(Bitstream.INTERNAL_ID, sInternalId);
			bitstream.setColumn(Bitstream.STORE_NUMBER, assetstore);
			DatabaseManager.update(tempContext, bitstream);

			tempContext.complete();
		}
		catch (SQLException sqle)
		{
			if (tempContext != null)
			{
				tempContext.abort();
			}
			throw sqle;
		}

		// get description of asset to put in database
		Map want = new HashMap();
		// set the names of the attributes we want a description of
		want.put(Bitstream.SIZE_BYTES, null);
		want.put(Bitstream.CHECKSUM, null);
		want.put(Bitstream.CHECKSUM_ALGORITHM, null);
		Map attrs = stores[assetstore].about(bitstreamPath, want);
		if (attrs != null)
		{
			updateBitstream(bitstream, attrs);
		    bitstream.setColumn(Bitstream.DELETED, false);
		    DatabaseManager.update(context, bitstream);
		}

		int bitstream_id = bitstream.getIntColumn(Bitstream.BITSTREAM_ID);
		if (log.isDebugEnabled()) 
		{
			log.debug("Registered bitstream " + bitstream_id + " under id " + bitstreamPath);
		}
		return bitstream_id;
	}

	/**
	 * Does the internal_id column in the bitstream row indicate the bitstream
	 * is a registered file
	 *
	 * @param internalId the value of the internal_id column
	 * @return true if the bitstream is a registered file
	 */
	public static boolean isRegisteredBitstream(String internalId)
	{
		return internalId.startsWith(REGISTERED_FLAG);
	}

    /**
     * Retrieve the bits for the bitstream with ID. If the bitstream does not
     * exist, or is marked deleted, returns null.
     * 
     * @param context
     *            The current context
     * @param id
     *            The ID of the bitstream to retrieve
     * @exception IOException
     *                If a problem occurs while retrieving the bits
     * @exception SQLException
     *                If a problem occurs accessing the RDBMS
     * 
     * @return The stream of bits, or null
     */
    public static InputStream retrieve(Context context, int id)
            throws SQLException, IOException
    {
        TableRow bitstream = DatabaseManager.find(context, "bitstream", id);
        if (bitstream != null)
        {
            int storeNo = bitstream.getIntColumn(Bitstream.STORE_NUMBER);
            // Default to zero ('assetstore.dir') for backwards compatibility
            if (storeNo == -1)
            {
                storeNo = 0;
            }
		    return stores[storeNo].get(bitstream.getStringColumn(Bitstream.INTERNAL_ID));
        }
        return null;
    }

    /**
     * <p>
     * Remove a bitstream from the asset store. This method does not delete any
     * bits, but simply marks the bitstreams as deleted (the context still needs
     * to be completed to finalize the transaction).
     * </p>
     * 
     * <p>
     * If the context is aborted, the bitstreams deletion status remains
     * unchanged.
     * </p>
     * 
     * @param context
     *            The current context
     * @param id
     *            The ID of the bitstream to delete
     * @exception SQLException
     *                If a problem occurs accessing the RDBMS
     */
    public static void delete(Context context, int id) throws SQLException
    {
        DatabaseManager
                .updateQuery(context,
                        "update Bitstream set deleted = '1' where bitstream_id = "
                                + id);
    }

    /**
     * Clean up the bitstream storage area. This method deletes any bitstreams
     * which are more than 1 hour old and marked deleted. The deletions cannot
     * be undone.
     *
     * @param deleteDbRecords if true deletes the database records otherwise it
     * 	           only deletes the files and directories in the assetstore  
     * @exception IOException
     *                If a problem occurs while cleaning up
     * @exception SQLException
     *                If a problem occurs accessing the RDBMS
     */
    public static void cleanup(boolean deleteDbRecords, boolean verbose) throws SQLException, IOException
    {
        Context context = null;
        BitstreamInfoDAO bitstreamInfoDAO = new BitstreamInfoDAO();
        int commitCounter = 0;

        try
        {
            context = new Context();

            String myQuery = "select * from Bitstream where deleted = '1'";

            List<TableRow> storage = DatabaseManager.queryTable(context, "Bitstream", myQuery)
                    .toList();

            for (Iterator<TableRow> iterator = storage.iterator(); iterator.hasNext();)
            {
                TableRow row = iterator.next();
                int bid = row.getIntColumn(Bitstream.BITSTREAM_ID);
                int storeNo = row.getIntColumn(Bitstream.STORE_NUMBER);
                String id = row.getStringColumn(Bitstream.INTERNAL_ID);

                // all we care about is last modified time
                Map want = new HashMap();
                want.put("modified", null);
                Map attrs = stores[storeNo].about(id, want);

                // Make sure entries which do not exist are removed
                if (attrs == null)
                {
                    log.debug("file is null");
                    if (deleteDbRecords)
                    {
                        log.debug("deleting record");
                        if (verbose)
                        {
                            System.out.println(" - Deleting bitstream information (ID: " + bid + ")");
                        }
                        bitstreamInfoDAO.deleteBitstreamInfoWithHistory(bid);
                        if (verbose)
                        {
                            System.out.println(" - Deleting bitstream record from database (ID: " + bid + ")");
                        }
                        DatabaseManager.delete(context, "Bitstream", bid);
                    }
                    continue;
                }

                // This is a small chance that this is a file which is
                // being stored -- get it next time.
                long lastmod = Long.valueOf((String)attrs.get("modified")).longValue();
                long now = new java.util.Date().getTime();
                // Skip if less than one hour old
                if (lastmod >= now || (now - lastmod) < (1 * 60 * 1000) )
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
                    bitstreamInfoDAO.deleteBitstreamInfoWithHistory(bid);
                    if (verbose)
                    {
                        System.out.println(" - Deleting bitstream record from database (ID: " + bid + ")");
                    }
                    DatabaseManager.delete(context, "Bitstream", bid);
                }

				if (isRegisteredBitstream(row.getStringColumn("internal_id"))) {
				    continue;			// do not delete registered bitstreams
				}


                // Since versioning allows for multiple bitstreams, check if the internal identifier isn't used on another place
                TableRow duplicateBitRow = DatabaseManager.querySingleTable(context, "Bitstream", "SELECT * FROM Bitstream WHERE internal_id = ? AND bitstream_id <> ?", row.getStringColumn("internal_id"), bid);
                if(duplicateBitRow == null)
                {
                    stores[storeNo].remove(id);
                    String message = "Deleted bitstream " + bid + " (id " + id + " )";
                    if (log.isDebugEnabled())
                    {
                        log.debug(message);
                    }
                    if(verbose)
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
                    System.out.print("Committing changes to the database...");
                    context.commit();
                    System.out.println(" Done!");
                }
            }

            context.complete();
        }
        // Aborting will leave the DB objects around, even if the
        // bitstreams are deleted. This is OK; deleting them next
        // time around will be a no-op.
        catch (SQLException sqle)
        {
            if (verbose)
            {
                System.err.println("Error: " + sqle.getMessage());
            }
            context.abort();
            throw sqle;
        }
        catch (IOException ioe)
        {
            if (verbose)
            {
                System.err.println("Error: " + ioe.getMessage());
            }
            context.abort();
            throw ioe;
        }
    }

    /**
     *
     * @param context
     * @param id of the bitstream to clone.
     * @return id of the clone bitstream.
     * @throws SQLException
     */
    public static int clone(Context context, int id) throws SQLException
    {
        TableRow row = DatabaseManager.find(context, "bitstream", id);
        row.setColumn(Bitstream.BITSTREAM_ID, -1);
        DatabaseManager.insert(context, row);
        return row.getIntColumn(Bitstream.BITSTREAM_ID);
    }
}
