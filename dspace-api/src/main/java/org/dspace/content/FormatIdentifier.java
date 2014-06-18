/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * This class handles the recognition of bitstream formats, using the format
 * registry in the database. For the moment, the format identifier simply uses
 * file extensions stored in the "BitstreamFormatIdentifier" table. This
 * probably isn't a particularly satisfactory long-term solution.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class FormatIdentifier
{
    /**
     * Attempt to identify the format of a particular bitstream. If the format
     * is unknown, null is returned.
     * 
     * @param bitstream
     *            the bitstream to identify the format of
     * 
     * @return a format from the bitstream format registry, or null
     */
    public static BitstreamFormat guessFormat(Context context,
            Bitstream bitstream) throws SQLException
    {
         String filename = bitstream.getName();
        // FIXME: Just setting format to first guess
        // For now just get the file name       

        // Gracefully handle the null case
        if (filename == null)
        {
            return null;
        }

        filename = filename.toLowerCase();

        // This isn't rocket science. We just get the name of the
        // bitstream, get the extension, and see if we know the type.
        String extension = filename;
        int lastDot = filename.lastIndexOf('.');

        if (lastDot != -1)
        {
            extension = filename.substring(lastDot + 1);
        }

        // If the last character was a dot, then extension will now be
        // an empty string. If this is the case, we don't know what
        // file type it is.
        if (extension.equals(""))
        {
            return null;
        }

        // See if the extension is in the fileextension table
        TableRowIterator tri = DatabaseManager.query(context,
                "SELECT bitstreamformatregistry.* FROM bitstreamformatregistry, " + 
                "fileextension WHERE fileextension.extension LIKE ? " + 
                "AND bitstreamformatregistry.bitstream_format_id=" + 
                "fileextension.bitstream_format_id",
                extension);

        BitstreamFormat retFormat = null;
        try
        {
            if (tri.hasNext())
            {
                // Return first match
                retFormat = new BitstreamFormat(context, tri.next());
            }
            else
            {
                retFormat = null;
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
            {
                tri.close();
            }
        }
        return retFormat;
    }
}
