/*
 * BitstreamStorageManager.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
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

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.sql.SQLException;
import java.util.*;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.storage.rdbms.*;

import org.apache.log4j.Logger;

/**
 * Manages bitstream storage
 *
 * @author  Peter Breton
 * @version $Revision$
 */
public class BitstreamStorageManager
{
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

    /** Root directory (as String) */
    private static String root = ConfigurationManager.getProperty("assetstore.dir");

    /** Root directory (as File) */
    private static File rootDirectory;

    /** Initialization flag */
    private static boolean initialized = false;

    /** Algorithm for the MessageDigest */
    private static final String DIGEST_ALGORITHM = "MD5";

    /** log4j log */
    private static Logger log = Logger.getLogger(BitstreamStorageManager.class);

    /**
     * Store a stream of bits.
     *
     * If this method returns successfully, the bits have been stored,
     * and RDBMS metadata entries are in place (the context still
     * needs to be completed to finalize the transaction).
     *
     * If this method returns successfully and the context is aborted,
     * then the bits will be stored in the asset store and the RDBMS
     * metadata entries will exist, but with the deleted flag set.
     *
     * If this method throws an exception, then any of the following
     * may be true:
     * <ul>
     *    <li>Neither bits nor RDBMS metadata entries have been stored.
     *    <li>RDBMS metadata entries with the deleted flag set have been
     *        stored, but no bits.
     *    <li>RDBMS metadata entries with the deleted flag set have been
     *        stored, and some or all of the bits have also been stored.
     * </ul>
     *
     * @param context -   the current context
     * @param is -       the stream of bits to store
     * @exception IOException - If a problem occurs while storing the bits
     * @exception SQLException - If a problem occurs accessing the RDBMS
     *
     * @return - the ID of the stored bitstream
     */
    public static int store(Context context, InputStream is)
        throws SQLException, IOException
    {
        initialize();

        String id = Utils.generateKey();

        TableRow bitstream = createDeletedBitstream(id);

        File file = forId(id, true);
        String fullname = file.getAbsolutePath();
        FileOutputStream fos = new FileOutputStream(file);

        NumberBytesInputStream nbis = new NumberBytesInputStream(is);
        DigestInputStream dis = null;

        try
        {
            dis = new DigestInputStream(nbis, MessageDigest.getInstance(DIGEST_ALGORITHM));
        }
        // Should never happen
        catch (NoSuchAlgorithmException nsae)
        {
            if (log.isDebugEnabled())
                log.debug("Caught NoSuchAlgorithmException", nsae);
        }

        Utils.bufferedCopy(dis, fos);
        fos.close();
        is.close();

        bitstream.setColumn("size",
                            nbis.getNumberOfBytesRead());
        bitstream.setColumn("checksum",
                            Utils.toHex(dis.getMessageDigest().digest()));
        bitstream.setColumn("checksum_algorithm",
                            DIGEST_ALGORITHM);
        bitstream.setColumn("deleted",
                            false);
        DatabaseManager.update(context, bitstream);

        int bitstream_id = bitstream.getIntColumn("bitstream_id");

        if (log.isDebugEnabled())
            log.debug("Stored bitstream " + bitstream_id + " in file " + fullname);

        return bitstream_id;
    }

    /**
     * Retrieve the bits for the bitstream with ID. If the bitstream
     * does not exist, or is marked deleted, returns null.
     *
     * @param context -   the current context
     * @param id -       the ID of the bitstream to retrieve
     * @exception IOException - If a problem occurs while retrieving the bits
     * @exception SQLException - If a problem occurs accessing the RDBMS
     *
     * @return -  the stream of bits, or null
     */
    public static InputStream retrieve(Context context, int id)
        throws SQLException, IOException
    {
        initialize();

        File file = forId(context, id, false);

        return (file != null) ? new FileInputStream(file) : null;
    }

    /**
     * Remove a bitstream from the asset store. This method does
     * not delete any bits, but simply marks the bitstreams as deleted
     * (the context still needs to be completed to finalize the transaction).
     *
     * If the context is aborted, the bitstreams deletion status
     * remains unchanged.
     *
     * @param context -   the current context
     * @param id -       the ID of the bitstream to delete
     * @exception IOException - If a problem occurs while deleting the bits
     * @exception SQLException - If a problem occurs accessing the RDBMS
     */
    public static void delete(Context context, int id)
        throws SQLException, IOException
    {
        initialize();

        DatabaseManager.updateQuery
            (context,
             "update Bitstream set deleted = 't' where bitstream_id = " + id);
    }

    /**
     * Clean up the bitstream storage area.
     * This method deletes any bitstreams which are more than 1 hour
     * old and marked deleted. The deletions cannot be undone.
     *
     * @exception IOException - If a problem occurs while cleaning up
     * @exception SQLException - If a problem occurs accessing the RDBMS
     */
    public static void cleanup()
        throws SQLException, IOException
    {
        initialize();

        Context context = new Context();

        try
        {
            List storage = DatabaseManager.query
                (context,
                 "Bitstream",
                 "select * from Bitstream where deleted = 't'").toList();

            for (Iterator iterator = storage.iterator(); iterator.hasNext(); )
            {
                TableRow row = (TableRow) iterator.next();
                int bid = row.getIntColumn("bitstream_id");
                File file = forId(getInternalId(context, bid));

                // Make sure entries which do not exist are removed
                if (file == null)
                {
                    DatabaseManager.delete (context, "Bitstream", bid);
                    continue;
                }

                // This is a small chance that this is a file which is
                // being stored -- get it next time.
                 if (isRecent(file))
                     continue;

                 DatabaseManager.delete (context, "Bitstream", bid);
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
     * Set the assetstore root (for testing)
     *
     * @param dir - The new asset store directory root
     * @return - The previous assetstore root
     * @exception IOException - If there is a problem with the asset store
     * directory
     */
    protected static String setRoot(String dir)
        throws IOException
    {
        String previous = root;
        root = dir;

        initializeInternal();

        return previous;
    }

    /**
     * Set the assetstore root to its default value (for testing)
     *
     * @return - The previous assetstore root
     * @exception IOException - If there is a problem with the asset store
     * directory
     */
    protected static String setRoot()
        throws IOException
    {
        return setRoot(ConfigurationManager.getProperty("assetstore.dir"));
    }

    /**
     * Return true if this file is too recent to be deleted,
     * false otherwise.
     *
     * @param file - The file to check
     * @return - true if this file is too recent to be deleted
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
     * Delete empty parent directories
     *
     * @param file - The file with parent directories to delete
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
     * Initialize the storage area. Calling this method multiple times
     * only initializes once.
     *
     * @exception IOException - If there is a problem with the asset store
     * directory
     */
    private synchronized static void initialize()
        throws IOException
    {
        if (initialized)
            return;

        initializeInternal();

        initialized = true;
    }

    /**
     * Initialize the storage area
     *
     * @exception IOException - If there is a problem with the asset store
     * directory
     */
    private synchronized static void initializeInternal()
        throws IOException
    {
        File dir = new File(root);

        // Sanity checks
        if (!dir.exists())
            throw new IOException("Asset store directory \"" + root + "\" does not exist");
        if (!dir.isDirectory())
            throw new IOException("\"" + root + "\" is not a directory");
        if (!dir.canRead())
            throw new IOException("Cannot read from \"" + root + "\"");
        if (!dir.canWrite())
            throw new IOException("Cannot write to \"" + root + "\"");

        rootDirectory = dir;
    }

    /**
     * Return the file corresponding to ID, or null.
     *
     * @param context - the current context
     * @param id - the ID of the bitstream
     * @param includeDeleted - If true, deleted bitstreams will be considered.
     * @return - The file corresponding to ID, or null
     * @exception IOException - If a problem occurs while determining the file
     * @exception SQLException - If a problem occurs accessing the RDBMS
     */
    protected static File forId(Context context, int id, boolean includeDeleted)
        throws IOException, SQLException
    {
        String sql = new StringBuffer()
            .append("select * from Bitstream where Bitstream.bitstream_id = ")
            .append(id)
            .append(includeDeleted  ? "" : " and deleted = 'f'")
            .toString();

        TableRow row = DatabaseManager.querySingle (context, "Bitstream", sql);

        return row == null ? null :
            forId(row.getStringColumn("internal_id"), false);
    }

    /**
     * Returns the file corresponding to ID, or null.
     *
     * @param id - the internal storage ID
     * @return - The file corresponding to ID, or null
     * @exception IOException - If a problem occurs while determining the file
     */
    protected static File forId(String id)
        throws IOException
    {
        return forId(id, false);
    }

    /**
     * Returns the file corresponding to ID.
     * If CREATE is true and the file does not exist, it is created.
     * Otherwise, null is returned.
     *
     * @param id - the internal storage ID
     * @param create - If true, and the file does not exist, it will be
     * created.
     * @return - The file corresponding to ID, or null
     * @exception IOException - If a problem occurs while determining the file
     */
    private static File forId(String id, boolean create)
        throws IOException
    {
        File file = new File(id2Filename(id));

        if (file.exists())
            return file;

        if (!create)
            return null;

        File parent = file.getParentFile();

        if (!parent.exists())
            parent.mkdirs();

        file.createNewFile();

        return file;
    }

    /**
     * Maps ID to full filename
     *
     * @param id - the internal storage ID
     * @return - the full filename
     * @exception IOException - If a problem occurs while determining the file name
     */
    private static String id2Filename(String id)
        throws IOException
    {
        BigInteger bigint = new BigInteger(id);

        StringBuffer result = new StringBuffer().append(rootDirectory.getCanonicalPath());

        // Split the id into groups
        for (int i = 0; i < directoryLevels; i++)
        {
            int digits = i * digitsPerLevel;

            result.append(File.separator).append(id.substring(digits, digits + digitsPerLevel));
        }

        String theName = result.append(File.separator).append(id).toString();

        if (log.isDebugEnabled())
            log.debug("Filename for " + id + " is " + theName);

        return theName;
    }

    ////////////////////////////////////////
    // RDBMS methods
    ////////////////////////////////////////

    /**
     * Create and return a bitstream with ID which is marked deleted.
     */
    private static TableRow createDeletedBitstream(String id)
        throws SQLException, IOException
    {
        Context context = new Context();

        TableRow bitstream = DatabaseManager.create(context, "Bitstream");
        bitstream.setColumn("deleted", true);
        bitstream.setColumn("internal_id", id);
        DatabaseManager.update(context, bitstream);

        context.complete();

        return bitstream;
    }

    /**
     * Return the internal storage id for the bitstream with ID,
     * or null.
     */
    private static String getInternalId(Context context, int id)
        throws SQLException
    {
        TableRow row = DatabaseManager.querySingle
            (context, "Bitstream",
             "select * from Bitstream where bitstream_id = " + id);

        return row == null ? null : row.getStringColumn("internal_id");
    }
}

/**
 * Simple filter which counts the number of bytes read
 */
class NumberBytesInputStream extends FilterInputStream
{

    /**
     * Number of bytes read
     */
    private int count = 0;

    /**
     * Constructor
     */
    public NumberBytesInputStream (InputStream is)
    {
        super(is);
    }

    ////////////////////////////////////////
    // FilterInputStream methods
    ////////////////////////////////////////

    public int read()
        throws java.io.IOException
    {
        int result = in.read();

        if (result != -1)
            count++;
        return result;
    }

    public int read(byte[] data)
        throws java.io.IOException
    {
        int result = in.read(data);

        if (result != -1)
            count += result;
        return result;
    }

    public int read(byte[] data, int start, int length)
        throws java.io.IOException
    {
        int result = in.read(data, start, length);

        if (result != -1)
            count += result;
        return result;
    }

    public long skip(long length)
        throws java.io.IOException
    {
        long result = in.skip(length);

        count += (int) result;
        return result;
    }

    public boolean markSupported()
    {
        return false;
    }

    ////////////////////////////////////////
    // non-interface methods
    ////////////////////////////////////////
    public int getNumberOfBytesRead()
    {
        return count;
    }
}
