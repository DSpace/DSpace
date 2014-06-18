/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import org.dspace.core.Context;
import org.dspace.content.Bitstream;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.storage.rdbms.TableRow;

import java.sql.SQLException;

public class ItemDAOOracle extends ItemDAO
{		
		private static final String selectPrimaryBitstreamID =
            "SELECT bundle.primary_bitstream_id FROM item2bundle, bundle " +
            "WHERE item2bundle.item_id=? AND item2bundle.bundle_id=bundle.bundle_id AND bundle.name=?";

    private static final String selectFirstBitstreamID =
        "SELECT bundle2bitstream.bitstream_id FROM item2bundle, bundle, bundle2bitstream " +
        "WHERE item2bundle.item_id=? AND item2bundle.bundle_id=bundle.bundle_id AND bundle.name=? " +
        "AND bundle.bundle_id=bundle2bitstream.bundle_id";
    
		private static final String selectNamedBitstreamID =
        "SELECT bitstream.bitstream_id FROM item2bundle, bundle, bundle2bitstream, bitstream " +
        "WHERE item2bundle.item_id=? AND item2bundle.bundle_id=bundle.bundle_id AND bundle.name=? " +
        "AND bundle.bundle_id=bundle2bitstream.bundle_id AND bundle2bitstream.bitstream_id=bitstream.bitstream_id " +
        "AND bitstream.name=?";
    
    ItemDAOOracle(Context ctx)
    {
        super(ctx);
    }

    public Bitstream getPrimaryBitstream(int itemId, String bundleName) throws SQLException
    {
        TableRowIterator tri = null;

        try
        {
            tri = DatabaseManager.query(context, selectPrimaryBitstreamID, itemId, bundleName);

            if (tri.hasNext())
            {
                TableRow row = tri.next();
                int bid = row.getIntColumn("primary_bitstream_id");
                return Bitstream.find(context, bid);
            }
        }        
        finally
        {
            if (tri != null)
            {
                tri.close();
            }
        }

        return null;
    }

    public Bitstream getFirstBitstream(int itemId, String bundleName) throws SQLException
    {
        TableRowIterator tri = null;

        try
        {
            tri = DatabaseManager.query(context, selectFirstBitstreamID, itemId, bundleName);
            if (tri.hasNext())
            {
                TableRow row = tri.next();
                int bid = row.getIntColumn("bitstream_id");
                return Bitstream.find(context, bid);
            }
        }
        finally
        {
            if (tri != null)
            {
                tri.close();
            }
        }

        return null;
    }

    public Bitstream getNamedBitstream(int itemId, String bundleName, String fileName) throws SQLException
    {
        TableRowIterator tri = null;

        try
        {
            tri = DatabaseManager.query(context, selectNamedBitstreamID, itemId, bundleName, fileName);
            if (tri.hasNext())
            {
                TableRow row = tri.next();
                int bid = row.getIntColumn("bitstream_id");
                return Bitstream.find(context, bid);
            }
        }
        finally
        {
            if (tri != null)
            {
                tri.close();
            }
        }

        return null;
    }
}
