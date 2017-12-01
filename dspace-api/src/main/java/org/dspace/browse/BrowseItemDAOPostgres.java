/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.core.Context;
import org.dspace.content.DCValue;
import org.dspace.content.Item;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BrowseItemDAOPostgres implements BrowseItemDAO
{
    /** query to obtain all the items from the database */
    private String findAll = "SELECT item_id, in_archive, withdrawn, discoverable FROM item WHERE in_archive = true OR withdrawn = true";

    /** query to get the text value of a metadata element only (qualifier is NULL) */
    private String getByMetadataElement = "SELECT authority, confidence, text_value,text_lang,element,qualifier FROM metadatavalue, metadatafieldregistry, metadataschemaregistry " +
                                    "WHERE metadatavalue.item_id = ? " +
                                    " AND metadatavalue.metadata_field_id = metadatafieldregistry.metadata_field_id " +
                                    " AND metadatafieldregistry.element = ? " +
                                    " AND metadatafieldregistry.qualifier IS NULL " +
                                    " AND metadatafieldregistry.metadata_schema_id=metadataschemaregistry.metadata_schema_id " +
                                    " AND metadataschemaregistry.short_id = ? " +
                                    " ORDER BY metadatavalue.metadata_field_id, metadatavalue.place";

    /** query to get the text value of a metadata element and qualifier */
    private String getByMetadata = "SELECT authority, confidence, text_value,text_lang,element,qualifier FROM metadatavalue, metadatafieldregistry, metadataschemaregistry " +
                                    "WHERE metadatavalue.item_id = ? " +
                                    " AND metadatavalue.metadata_field_id = metadatafieldregistry.metadata_field_id " +
                                    " AND metadatafieldregistry.element = ? " +
                                    " AND metadatafieldregistry.qualifier = ? " +
                                    " AND metadatafieldregistry.metadata_schema_id=metadataschemaregistry.metadata_schema_id " +
                                    " AND metadataschemaregistry.short_id = ? " +
                                    " ORDER BY metadatavalue.metadata_field_id, metadatavalue.place";

    /** query to get the text value of a metadata element with the wildcard qualifier (*) */
    private String getByMetadataAnyQualifier = "SELECT authority, confidence, text_value,text_lang,element,qualifier FROM metadatavalue, metadatafieldregistry, metadataschemaregistry " +
                                    "WHERE metadatavalue.item_id = ? " +
                                    " AND metadatavalue.metadata_field_id = metadatafieldregistry.metadata_field_id " +
                                    " AND metadatafieldregistry.element = ? " +
                                    " AND metadatafieldregistry.metadata_schema_id=metadataschemaregistry.metadata_schema_id " +
                                    " AND metadataschemaregistry.short_id = ? " +
                                    " ORDER BY metadatavalue.metadata_field_id, metadatavalue.place";

    /** DSpace context */
	private Context context;

    public BrowseItemDAOPostgres(Context context)
    	throws BrowseException
    {
        this.context = context;
    }

    public BrowseItem[] findAll() throws SQLException
    {
        TableRowIterator tri = null;
        List<BrowseItem> items = new ArrayList<BrowseItem>();

        try
        {
            tri = DatabaseManager.query(context, findAll);
            while (tri.hasNext())
            {
                TableRow row = tri.next();
                items.add(new BrowseItem(context, row.getIntColumn("item_id"),
                                                  row.getBooleanColumn("in_archive"),
                                                  row.getBooleanColumn("withdrawn"),
                                                  row.getBooleanColumn("discoverable")));
            }
        }
        finally
        {
            if (tri != null)
            {
                tri.close();
            }
        }
        
        BrowseItem[] bis = new BrowseItem[items.size()];
        return items.toArray(bis);
    }

    public DCValue[] queryMetadata(int itemId, String schema, String element, String qualifier, String lang)
    	throws SQLException
    {
    	List<DCValue> values = new ArrayList<DCValue>();
    	TableRowIterator tri = null;

        try
        {
            if (qualifier == null)
            {
                Object[] params = { Integer.valueOf(itemId), element, schema };
                tri = DatabaseManager.query(context, getByMetadataElement, params);
            }
            else if (Item.ANY.equals(qualifier))
            {
                Object[] params = { Integer.valueOf(itemId), element, schema };
                tri = DatabaseManager.query(context, getByMetadataAnyQualifier, params);
            }
            else
            {
                Object[] params = { Integer.valueOf(itemId), element, qualifier, schema };
                tri = DatabaseManager.query(context, getByMetadata, params);
            }

            if (!tri.hasNext())
            {
                return new DCValue[0];
            }

            while (tri.hasNext())
            {
                TableRow tr = tri.next();
                DCValue dcv = new DCValue();
                dcv.schema = schema;
                dcv.element = tr.getStringColumn("element");
                dcv.qualifier = tr.getStringColumn("qualifier");
                dcv.language = tr.getStringColumn("text_lang");
                dcv.value = tr.getStringColumn("text_value");
                dcv.authority = tr.getStringColumn("authority");
                dcv.confidence = tr.getIntColumn("confidence");
                values.add(dcv);
            }
        }
        finally
        {
            if (tri != null)
            {
                tri.close();
            }
        }
        
        DCValue[] dcvs = new DCValue[values.size()];
        return values.toArray(dcvs);
    }
}
