/*
 * BitstreamStorageManager.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.storage.bitstore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

/**
 * <P>Stores, retrieves and deletes bitstreams.</P>
 *
 * <P>Presently, asset stores are specified in <code>dspace.cfg</code>.  Since
 * Java does not offer a way of detecting free disk space, the asset store to
 * use for new bitstreams is also specified in a configuration property.  The
 * drawbacks to this are that the administrators are responsible for monitoring
 * available space in the asset stores, and DSpace (Tomcat) has to be restarted
 * when the asset store for new ('incoming') bitstreams is changed.</P>
 *
 * @author  Peter Breton, Robert Tansley
 * @version $Revision$
 */
public class BitstreamStorageManager
{
    /** log4j log */
    private static Logger log = Logger.getLogger(BitstreamStorageManager.class);

    /** The asset store locations */
    private static File[] assetStores;
    
    /** The asset store to use for new bitstreams */
    private static int incoming;
    
    // These settings control the way an identifier is hashed into
    // directory and file names
    //
    // With digitsPerLevel 2 and directoryLevels 3, an identifier
    // like 12345678901234567890 turns into the relative name
    // /12/34/56/12345678901234567890.
    //
    // You should not change these settings if you have data in the
    // asset store, as the BitstreamStorageManager will be unable
    // to find your existing data.
    private static final int digitsPerLevel = 2;
    private static final int directoryLevels = 3;

    /* Read in the asset stores from the config. */
    static
    {
        ArrayList stores = new ArrayList();

        // 'assetstore.dir' is always store number 0.
        stores.add(ConfigurationManager.getProperty("assetstore.dir"));

        // Read in assetstore.dir.1, assetstore.dir.2....
        for (int i = 1;
             ConfigurationManager.getProperty("assetstore.dir." + i) != null;
             i++)
        {
            stores.add(ConfigurationManager.getProperty("assetstore.dir." + i));
        }
        
        // Now make that list an array of Files.
        assetStores = new File[stores.size()];
        for (int i = 0; i < stores.size(); i++)
        {
            assetStores[i] = new File((String) stores.get(i));
        }
        
        // Read asset store to put new files in.  Default is 0.
        incoming = ConfigurationManager.getIntProperty("assetstore.incoming");
    }


    /**
     * Store a stream of bits.
     *
     * <p>If this method returns successfully, the bits have been stored,
     * and RDBMS metadata entries are in place (the context still
     * needs to be completed to finalize the transaction).</p>
     *
     * <p>If this method returns successfully and the context is aborted,
     * then the bits will be stored in the asset store and the RDBMS
     * metadata entries will exist, but with the deleted flag set.</p>
     *
     * If this method throws an exception, then any of the following
     * may be true:
     *
     * <ul>
     *    <li>Neither bits nor RDBMS metadata entries have been stored.
     *    <li>RDBMS metadata entries with the deleted flag set have been
     *        stored, but no bits.
     *    <li>RDBMS metadata entries with the deleted flag set have been
     *        stored, and some or all of the bits have also been stored.
     * </ul>
     *
     * @param context The current context
     * @param is The stream of bits to store
     * @exception IOException If a problem occurs while storing the bits
     * @exception SQLException If a problem occurs accessing the RDBMS
     *
     * @return The ID of the stored bitstream
     */
    public static int store(Context context, InputStream is)
        throws SQLException, IOException
    {
        // Create internal ID
        String id = Utils.generateKey();

        // Create a deleted bitstream row, using a separate DB connection
        TableRow bitstream;
        Context tempContext = null;

        try
        {
            tempContext = new Context();

            bitstream = DatabaseManager.create(tempContext, "Bitstream");
            bitstream.setColumn("deleted", true);
            bitstream.setColumn("internal_id", id);

            /*
             * Set the store number of the new bitstream
             * If you want to use some other method of working out where to
             * put a new bitstream, here's where it should go
             */
            bitstream.setColumn("store_number", incoming);

            DatabaseManager.update(tempContext, bitstream);

            tempContext.complete();
        }
        catch (SQLException sqle)
        {
            if (tempContext != null)
                tempContext.abort();

            throw sqle;
        }

        // Where on the file system will this new bitstream go?
        File file = getFile(bitstream);

        // Make the parent dirs if necessary
        File parent = file.getParentFile();
        if (!parent.exists()) parent.mkdirs();

        //Create the corresponding file and open it
        file.createNewFile();
        FileOutputStream fos = new FileOutputStream(file);

        // Read through a digest input stream that will work out the MD5
        DigestInputStream dis = null;
        try
        {
            dis = new DigestInputStream(is, MessageDigest.getInstance("MD5"));
        }
        // Should never happen
        catch (NoSuchAlgorithmException nsae)
        {
            log.warn("Caught NoSuchAlgorithmException", nsae);
        }

        Utils.bufferedCopy(dis, fos);
        fos.close();
        is.close();

        if( "oracle".equals(ConfigurationManager.getProperty("db.name")) )
        {
            bitstream.setColumn("size_bytes", (int) file.length());
        }
        else
        {
            // postgres default
            bitstream.setColumn("size", (int) file.length());
        }

        bitstream.setColumn("checksum",
                            Utils.toHex(dis.getMessageDigest().digest()));
        bitstream.setColumn("checksum_algorithm", "MD5");
        bitstream.setColumn("deleted", false);
        DatabaseManager.update(context, bitstream);

        int bitstream_id = bitstream.getIntColumn("bitstream_id");

        if (log.isDebugEnabled())
            log.debug("Stored bitstream " + bitstream_id + " in file " +
                      file.getAbsolutePath());

        return bitstream_id;
    }

    
    /**
     * Retrieve the bits for the bitstream with ID. If the bitstream
     * does not exist, or is marked deleted, returns null.
     *
     * @param context The current context
     * @param id The ID of the bitstream to retrieve
     * @exception IOException If a problem occurs while retrieving the bits
     * @exception SQLException If a problem occurs accessing the RDBMS
     *
     * @return The stream of bits, or null
     */
    public static InputStream retrieve(Context context, int id)
        throws SQLException, IOException
    {
        TableRow bitstream = DatabaseManager.find(context, "bitstream", id);
        File file = getFile(bitstream);
        return (file != null) ? new FileInputStream(file) : null;
    }

    
    /**
     * <p>Remove a bitstream from the asset store. This method does
     * not delete any bits, but simply marks the bitstreams as deleted
     * (the context still needs to be completed to finalize the transaction).
     * </p>
     *
     * <p>If the context is aborted, the bitstreams deletion status
     * remains unchanged.</p>
     *
     * @param context The current context
     * @param id The ID of the bitstream to delete
     * @exception SQLException If a problem occurs accessing the RDBMS
     */
    public static void delete(Context context, int id)
        throws SQLException
    {
        if( "oracle".equals(ConfigurationManager.getProperty("db.name")) )
        {
            // oracle uses 1 for true
            DatabaseManager.updateQuery
                (context,
                "update Bitstream set deleted = 1 where bitstream_id = " + id);
        }
        else
        {
            DatabaseManager.updateQuery
                (context,
                "update Bitstream set deleted = 't' where bitstream_id = " + id);
        }
    }

    /**
     * Clean up the bitstream storage area.
     * This method deletes any bitstreams which are more than 1 hour
     * old and marked deleted. The deletions cannot be undone.
     *
     * @exception IOException If a problem occurs while cleaning up
     * @exception SQLException If a problem occurs accessing the RDBMS
     */
    public static void cleanup()
        throws SQLException, IOException
    {
        Context context = null;

        try
        {
            context = new Context();
            String myQuery = null;
            
            if( "oracle".equals(ConfigurationManager.getProperty("db.name")) )
            {
                myQuery = "select * from Bitstream where deleted = 1";
            }
            else
            {
                // postgres
                myQuery = "select * from Bitstream where deleted = 't'";
            }


            List storage = DatabaseManager.query
                (context,
                 "Bitstream",
                 myQuery).toList();

            for (Iterator iterator = storage.iterator(); iterator.hasNext(); )
            {
                TableRow row = (TableRow) iterator.next();
                int bid = row.getIntColumn("bitstream_id");
                File file = getFile(row);

                // Make sure entries which do not exist are removed
                if (file == null)
                {
                    DatabaseManager.delete(context, "Bitstream", bid);
                    continue;
                }

                // This is a small chance that this is a file which is
                // being stored -- get it next time.
                 if (isRecent(file))
                     continue;

                 DatabaseManager.delete(context, "Bitstream", bid);
                 boolean success = file.delete();

                 if (log.isDebugEnabled())
                     log.debug("Deleted bitstream " + bid +
                               " (file " + file.getAbsolutePath() +
                               ") with result " + success);

                 deleteParents(file);
            }

            context.complete();
        }
        // Aborting will leave the DB objects around, even if the
        // bitstreams are deleted. This is OK; deleting them next
        // time around will be a no-op.
        catch (SQLException sqle)
        {
            context.abort();
            throw sqle;
        }
        catch (IOException ioe)
        {
            context.abort();
            throw ioe;
        }
    }

    ////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////


    /**
     * Return true if this file is too recent to be deleted,
     * false otherwise.
     *
     * @param file The file to check
     * @return True if this file is too recent to be deleted
     */
    private static boolean isRecent(File file)
    {
        long lastmod = file.lastModified();
        long now = new java.util.Date().getTime();

        if (lastmod >= now)
            return true;

        // Less than one hour old
        return now - lastmod < (1 * 60 * 1000);
    }

    /**
     * Delete empty parent directories.
     *
     * @param file The file with parent directories to delete
     */
    private synchronized static void deleteParents(File file)
    {
        if (file == null)
            return;

        File tmp = file;

        for (int i = 0; i < directoryLevels; i++)
        {
            File directory = tmp.getParentFile();
            File[] files = directory.listFiles();

            // Only delete empty directories
            if (files.length != 0)
                break;

            directory.delete();
            tmp = directory;
        }
    }


    /**
     * Return the file corresponding to a bitstream.  It's safe to pass in
     * <code>null</code>.
     *
     * @param bitstream  the database table row for the bitstream.
     *                   Can be <code>null</code>
     *
     * @return  The corresponding file in the file system, or <code>null</code>
     *
     * @exception IOException If a problem occurs while determining the file
     */
    private static File getFile(TableRow bitstream)
        throws IOException
    {
        // Check that bitstream is not null
        if (bitstream == null)
        {
            return null;
        }
        
        // Get the store to use
        int storeNumber = bitstream.getIntColumn("store_number");
        
        // Default to zero ('assetstore.dir') for backwards compatibility
        if (storeNumber == -1)
        {
            storeNumber = 0;
        }
        
        File store = assetStores[storeNumber];
        
        // Turn the internal ID into a file path relative to the asset store
        // directory
        String id = bitstream.getStringColumn("internal_id");
        BigInteger bigint = new BigInteger(id);

        StringBuffer result = new StringBuffer().append(store.getCanonicalPath());

        // Split the id into groups
        for (int i = 0; i < directoryLevels; i++)
        {
            int digits = i * digitsPerLevel;

            result.append(File.separator).append(id.substring(digits, digits + digitsPerLevel));
        }

        String theName = result.append(File.separator).append(id).toString();

        if (log.isDebugEnabled())
            log.debug("Filename for " + id + " is " + theName);

        return new File(theName);
    }
}
