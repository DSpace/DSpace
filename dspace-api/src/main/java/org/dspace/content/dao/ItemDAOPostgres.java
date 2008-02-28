/*
 * ItemDAOPostgres.java
 *
 * Version: $Revision: 1.0 $
 *
 * Date: $Date: 2008/01/01 04:11:09 $
 *
 * Copyright (c) 2002-2007, Hewlett-Packard Company and Massachusetts
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

package org.dspace.content.dao;

import org.dspace.core.Context;
import org.dspace.content.Bitstream;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.storage.rdbms.TableRow;

import java.sql.SQLException;

public class ItemDAOPostgres extends ItemDAO
{
    private final String selectPrimaryBitstreamID =
            "SELECT bundle.primary_bitstream_id FROM item2bundle, bundle " +
            "WHERE item2bundle.item_id=? AND item2bundle.bundle_id=bundle.bundle_id AND bundle.name=? LIMIT 1";

    private final String selectFirstBitstreamID =
        "SELECT bundle2bitstream.bitstream_id FROM item2bundle, bundle, bundle2bitstream " +
        "WHERE item2bundle.item_id=? AND item2bundle.bundle_id=bundle.bundle_id AND bundle.name=? " +
        "AND bundle.bundle_id=bundle2bitstream.bundle_id LIMIT 1";

//    private final String selectFirstBitstreamID =
//        "SELECT bitstream.bitstream_id FROM item2bundle, bundle, bundle2bitstream, bitstream " +
//        "WHERE item2bundle.item_id=? AND item2bundle.bundle_id=bundle.bundle_id AND bundle.name=? " +
//        "AND bundle.bundle_id=bundle2bitstream.bundle_id AND bundle2bitstream.bitstream_id=bitstream.bitstream_id " +
//        " LIMIT 1";

//    private final String selectFirstBitstreamID =
//        "SELECT bitstream_id FROM (" +
//        "SELECT bitstream.bitstream_id FROM item2bundle, bundle, bundle2bitstream, bitstream " +
//        "WHERE item2bundle.item_id=? AND item2bundle.bundle_id=bundle.bundle_id AND bundle.name=? " +
//        "AND bundle.bundle_id=bundle2bitstream.bundle_id AND bundle2bitstream.bitstream_id=bitstream.bitstream_id " +
//        "ORDER BY bitstream.sequence_id" +
//        ") allstreams LIMIT 1";

    private final String selectNamedBitstreamID =
        "SELECT bitstream.bitstream_id FROM item2bundle, bundle, bundle2bitstream, bitstream " +
        "WHERE item2bundle.item_id=? AND item2bundle.bundle_id=bundle.bundle_id AND bundle.name=? " +
        "AND bundle.bundle_id=bundle2bitstream.bundle_id AND bundle2bitstream.bitstream_id=bitstream.bitstream_id " +
        "AND bitstream.name=?";

    ItemDAOPostgres(Context ctx)
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
                tri.close();
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
                tri.close();
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
                tri.close();
        }

        return null;
    }
}
