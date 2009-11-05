/*
 * BrowseItemDAOOracle.java
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

public class BrowseItemDAOOracle implements BrowseItemDAO
{
    /** query to obtain all the items from the database */
    private String findAll = "SELECT item_id, in_archive, withdrawn FROM item WHERE in_archive = 1 OR withdrawn = 1";

    /** query to get the text value of a metadata element only (qualifier is NULL) */
    private String getByMetadataElement = "SELECT text_value,text_lang,element,qualifier FROM metadatavalue, metadatafieldregistry, metadataschemaregistry " +
                                    "WHERE metadatavalue.item_id = ? " +
                                    " AND metadatavalue.metadata_field_id = metadatafieldregistry.metadata_field_id " +
                                    " AND metadatafieldregistry.element = ? " +
                                    " AND metadatafieldregistry.qualifier IS NULL " +
                                    " AND metadatafieldregistry.metadata_schema_id=metadataschemaregistry.metadata_schema_id " +
                                    " AND metadataschemaregistry.short_id = ? " +
                                    " ORDER BY metadatavalue.metadata_field_id, metadatavalue.place";

    /** query to get the text value of a metadata element and qualifier */
    private String getByMetadata = "SELECT text_value,text_lang,element,qualifier FROM metadatavalue, metadatafieldregistry, metadataschemaregistry " +
                                    "WHERE metadatavalue.item_id = ? " +
                                    " AND metadatavalue.metadata_field_id = metadatafieldregistry.metadata_field_id " +
                                    " AND metadatafieldregistry.element = ? " +
                                    " AND metadatafieldregistry.qualifier = ? " +
                                    " AND metadatafieldregistry.metadata_schema_id=metadataschemaregistry.metadata_schema_id " +
                                    " AND metadataschemaregistry.short_id = ? " +
                                    " ORDER BY metadatavalue.metadata_field_id, metadatavalue.place";

    /** query to get the text value of a metadata element with the wildcard qualifier (*) */
    private String getByMetadataAnyQualifier = "SELECT text_value,text_lang,element,qualifier FROM metadatavalue, metadatafieldregistry, metadataschemaregistry " +
                                    "WHERE metadatavalue.item_id = ? " +
                                    " AND metadatavalue.metadata_field_id = metadatafieldregistry.metadata_field_id " +
                                    " AND metadatafieldregistry.element = ? " +
                                    " AND metadatafieldregistry.metadata_schema_id=metadataschemaregistry.metadata_schema_id " +
                                    " AND metadataschemaregistry.short_id = ? " +
                                    " ORDER BY metadatavalue.metadata_field_id, metadatavalue.place";

    /** DSpace context */
	private Context context;

    public BrowseItemDAOOracle(Context context)
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
                                                  row.getBooleanColumn("withdrawn")));
            }
        }
        finally
        {
            if (tri != null)
                tri.close();
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
                Object[] params = { new Integer(itemId), element, schema };
                tri = DatabaseManager.query(context, getByMetadataElement, params);
            }
            else if (Item.ANY.equals(qualifier))
            {
                Object[] params = { new Integer(itemId), element, schema };
                tri = DatabaseManager.query(context, getByMetadataAnyQualifier, params);
            }
            else
            {
                Object[] params = { new Integer(itemId), element, qualifier, schema };
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
                values.add(dcv);
            }
        }
        finally
        {
            if (tri != null)
                tri.close();
        }

        DCValue[] dcvs = new DCValue[values.size()];
        return values.toArray(dcvs);
    }
}
