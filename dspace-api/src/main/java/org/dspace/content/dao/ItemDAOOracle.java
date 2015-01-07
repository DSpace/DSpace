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
import org.dspace.content.MetadataSchema;
import org.dspace.core.Constants;

public class ItemDAOOracle extends ItemDAO
{
    private static final String selectPrimaryBitstreamID =
        "SELECT bundle.primary_bitstream_id"
            + " FROM item2bundle"
            + "  JOIN bundle USING (bundle_id)"
            + "  JOIN metadatavalue MD1 ON (MD1.resource_type_id = " +  Constants.BUNDLE
            + "   AND MD1.resource_id = bundle_id"
            + "   AND MD1.metadata_field_id ="
            + "   (SELECT metadata_field_id FROM MetadataFieldRegistry"
            + "     WHERE metadata_schema_id = " + MetadataSchema.DC_SCHEMA_ID
            + "      AND element = 'title' AND qualifier IS NULL)"
            + "   )"
            + " WHERE item2bundle.item_id=?"
            + "  AND MD1.text_value=?";

    private static final String selectFirstBitstreamID =
        "SELECT bundle2bitstream.bitstream_id"
            + " FROM item2bundle"
            + "  JOIN bundle USING (bundle_id)"
            + "  JOIN bundle2bitstream USING (bundle_id)"
            + "  JOIN metadatavalue MD1 ON (MD1.resource_type_id = " +  Constants.BUNDLE
            + "   AND MD1.resource_id = bundle_id"
            + "   AND MD1.metadata_field_id ="
            + "   (SELECT metadata_field_id FROM MetadataFieldRegistry"
            + "     WHERE metadata_schema_id = " + MetadataSchema.DC_SCHEMA_ID
            + "      AND element = 'title' AND qualifier IS NULL)"
            + "   )"
            + " WHERE item2bundle.item_id=?"
            + "  AND MD1.text_value=?";

    private static final String selectNamedBitstreamID =
        "SELECT bitstream.bitstream_id"
            + " FROM item2bundle"
            + "  JOIN bundle USING (bundle_id)"
            + "  JOIN bundle2bitstream USING (bundle_id)"
            + "  JOIN bitstream USING (bitstream_id)"
            + "  JOIN metadatavalue MD1 ON (MD1.resource_type_id = " +  Constants.BUNDLE
            + "   AND MD1.resource_id = bundle_id"
            + "   AND MD1.metadata_field_id ="
            + "   (SELECT metadata_field_id FROM MetadataFieldRegistry"
            + "     WHERE metadata_schema_id = " + MetadataSchema.DC_SCHEMA_ID
            + "      AND element = 'title' AND qualifier IS NULL)"
            + "   )"
            + "  JOIN metadatavalue MD2 ON (MD2.resource_type_id = " +  Constants.BITSTREAM
            + "   AND MD2.resource_id = bitstream_id"
            + "   AND MD2.metadata_field_id ="
            + "   (SELECT metadata_field_id FROM MetadataFieldRegistry"
            + "     WHERE metadata_schema_id = " + MetadataSchema.DC_SCHEMA_ID
            + "      AND element = 'title' AND qualifier IS NULL)"
            + "   )"
            + " WHERE item2bundle.item_id=?"
            + "  AND MD1.text_value=? "
            + "  AND MD2.text_value=?";

    ItemDAOOracle(Context ctx)
    {
        super(ctx);
    }

    @Override
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

    @Override
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

    @Override
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
