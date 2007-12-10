/*
 * MetadataValueDAOPostgres.java
 *
 * Version: $Revision: 427 $
 *
 * Date: $Date: 2007-08-07 17:32:39 +0100 (Tue, 07 Aug 2007) $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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
package org.dspace.content.dao.postgres;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.dao.MetadataValueDAO;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

public class MetadataValueDAOPostgres extends MetadataValueDAO
{
    public MetadataValueDAOPostgres(Context context)
    {
        super(context);
    }

    @Override
    public MetadataValue create() throws AuthorizeException
    {
        UUID uuid = UUID.randomUUID();

        try
        {
            TableRow row = DatabaseManager.create(context, "metadatavalue");
            row.setColumn("uuid", uuid.toString());
            DatabaseManager.update(context, row);

            int id = row.getIntColumn("metadata_value_id");
            MetadataValue value = new MetadataValue(context, id);

            return value;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public MetadataValue retrieve(int id)
    {
        try
        {
            TableRow row = DatabaseManager.find(context, "metadatavalue", id);

            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public MetadataValue retrieve(UUID uuid)
    {
        try
        {
            TableRow row = DatabaseManager.findByUnique(context,
                    "metadatavalue", "uuid", uuid.toString());

            return retrieve(row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public void update(MetadataValue value) throws AuthorizeException
    {
        try
        {
            int id = value.getID();
            TableRow row = DatabaseManager.find(context, "metadatavalue", id);

            if (row == null)
            {
                log.warn("Couldn't find metadata value " + id);
            }
            else
            {
                int itemID = value.getItemID();
                int fieldID = value.getFieldID();
                String textValue = value.getValue();
                String language = value.getLanguage();
                int place = value.getPlace();

                if (itemID <= 0)
                {
                    throw new RuntimeException("item_id cannot be null");
                }
                else
                {
                    row.setColumn("item_id", itemID);
                }

                if (fieldID <= 0)
                {
                    throw new RuntimeException("metadata_field_id cannot be null");
                }
                else
                {
                    row.setColumn("metadata_field_id", fieldID);
                }

                if ((textValue == null) || (textValue.equals("")))
                {
                    throw new RuntimeException("text_value cannot be null");
                }
                else
                {
                    row.setColumn("text_value", textValue);
                }

                if ((language == null) || (language.equals("")))
                {
                    row.setColumnNull("text_lang");
                }
                else
                {
                    row.setColumn("text_lang", language);
                }

                if (place <= 0)
                {
                    throw new RuntimeException("place cannot be null");
                }
                else
                {
                    row.setColumn("place", place);
                }

                DatabaseManager.update(context, row);
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public void delete(int id) throws AuthorizeException
    {
        try
        {
            DatabaseManager.delete(context, "metadatavalue", id);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<MetadataValue> getMetadataValues(MetadataField field)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "metadatavalue",
                    "SELECT metadata_value_id FROM metadatavalue " +
                    "WHERE metadata_field_id = ? ",
                    field.getID());

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<MetadataValue> getMetadataValues(MetadataField field,
                                                 String value)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "metadatavalue",
                    "SELECT metadata_value_id FROM metadatavalue " +
                            "WHERE metadata_field_id = ? " +
                            "AND text_value LIKE ?",
                    field.getID(), value);

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<MetadataValue> getMetadataValues(MetadataField field,
            String value, String language)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "metadatavalue",
                    "SELECT metadata_value_id FROM metadatavalue " +
                    "WHERE metadata_field_id = ? " +
                    "AND text_value LIKE ? AND text_lang LIKE ?",
                    field.getID(), value, language);

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public List<MetadataValue> getMetadataValues(Item item)
    {
        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "metadatavalue",
                    "SELECT metadata_value_id FROM metadatavalue " +
                    "WHERE item_id = ? ",
                    item.getID());

            return returnAsList(tri);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    private MetadataValue retrieve(TableRow row) throws SQLException
    {
        if (row == null)
        {
            return null;
        }

        int id = row.getIntColumn("metadata_value_id");
        int fieldID = row.getIntColumn("metadata_field_id");
        int itemID = row.getIntColumn("item_id");
        String textValue = row.getStringColumn("text_value");
        String language = row.getStringColumn("text_lang");
        int place = row.getIntColumn("place");

        MetadataValue value = new MetadataValue(context, id);
        value.setFieldID(fieldID);
        value.setItemID(itemID);
        value.setValue(textValue);
        value.setLanguage(language);
        value.setPlace(place);

        return value;
    }

    private List<MetadataValue> returnAsList(TableRowIterator tri)
            throws SQLException
    {
        List<MetadataValue> values = new ArrayList<MetadataValue>();

        for (TableRow row : tri.toList())
        {
            int id = row.getIntColumn("metadata_value_id");
            values.add(retrieve(id));
        }

        return values;
    }
}
