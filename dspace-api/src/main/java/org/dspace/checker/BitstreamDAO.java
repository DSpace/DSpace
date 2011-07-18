/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.storage.bitstore.BitstreamStorageManager;

/**
 * <p>
 * Data Access Object for Bitstreams.
 * </p>
 * 
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 * 
 */
public class BitstreamDAO
{
    /**
     * Default Constructor
     */
    public BitstreamDAO()
    {
    }

    /**
     * Retrieves the bitstream from the bitstore.
     * 
     * @param id
     *            the bitstream id.
     * 
     * @return Bitstream as an InputStream
     * 
     * @throws IOException
     *             Rethrown from BitstreamStorageManager
     * @throws SQLException
     *             Rethrown from BitstreamStorageManager
     * 
     * @see org.dspace.storage.bitstore.BitstreamStorageManager#retrieve(Context,
     *      int)
     */
    public InputStream getBitstream(int id) throws IOException, SQLException
    {

        Context context = null;
        InputStream is = null;
        try
        {
            context = new Context();
            is = BitstreamStorageManager.retrieve(context, id);
        }
        finally
        {
            context.abort();
        }

        return is;
    }
}
