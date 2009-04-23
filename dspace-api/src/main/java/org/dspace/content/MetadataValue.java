/*
 * MetadataValue.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
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
package org.dspace.content;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * Database access class representing a Dublin Core metadata value.
 * It represents a value of a given <code>MetadataField</code> on an Item.
 * (The Item can have many values of the same field.)  It contains                                           element, qualifier, value and language.
 * the field (which names the schema, element, and qualifer), language,
 * and a value.
 *
 * @author Martin Hald
 * @see org.dspace.content.MetadataSchema, org.dspace.content.MetadataField
 */
public class MetadataValue
{
    /** The reference to the metadata field */
    private int fieldId = 0;

    /** The primary key for the metadata value */
    private int valueId = 0;

    /** The reference to the DSpace item */
    private int itemId;

    /** The value of the field */
    public String value;

    /** The language of the field, may be <code>null</code> */
    public String language;

    /** The position of the record. */
    public int place = 1;

    /** log4j logger */
    private static Logger log = Logger.getLogger(MetadataValue.class);

    /** The row in the table representing this type */
    private TableRow row;

    /**
     * Construct the metadata object from the matching database row.
     *
     * @param row database row to use for contents
     */
    public MetadataValue(TableRow row)
    {
        if (row != null)
        {
            fieldId = row.getIntColumn("metadata_field_id");
            valueId = row.getIntColumn("metadata_value_id");
            itemId = row.getIntColumn("item_id");
            value = row.getStringColumn("text_value");
            language = row.getStringColumn("text_lang");
            place = row.getIntColumn("place");
            this.row = row;
        }
    }

    /**
     * Default constructor.
     */
    public MetadataValue()
    {
    }

    /**
     * Constructor to create a value for a given field.
     *
     * @param field inital value for field
     */
    public MetadataValue(MetadataField field)
    {
        this.fieldId = field.getFieldID();
    }

    /**
     * Get the field ID the metadata value represents.
     *
     * @return metadata field ID
     */
    public int getFieldId()
    {
        return fieldId;
    }

    /**
     * Set the field ID that the metadata value represents.
     *
     * @param fieldId new field ID
     */
    public void setFieldId(int fieldId)
    {
        this.fieldId = fieldId;
    }

    /**
     * Get the item ID.
     *
     * @return item ID
     */
    public int getItemId()
    {
        return itemId;
    }

    /**
     * Set the item ID.
     *
     * @param itemId new item ID
     */
    public void setItemId(int itemId)
    {
        this.itemId = itemId;
    }

    /**
     * Get the language (e.g. "en").
     *
     * @return language
     */
    public String getLanguage()
    {
        return language;
    }

    /**
     * Set the language (e.g. "en").
     *
     * @param language new language
     */
    public void setLanguage(String language)
    {
        this.language = language;
    }

    /**
     * Get the place ordering.
     *
     * @return place ordering
     */
    public int getPlace()
    {
        return place;
    }

    /**
     * Set the place ordering.
     *
     * @param place new place (relative order in series of values)
     */
    public void setPlace(int place)
    {
        this.place = place;
    }

    /**
     * Get the value ID.
     *
     * @return value ID
     */
    public int getValueId()
    {
        return valueId;
    }

    /**
     * Get the metadata value.
     *
     * @return metadata value
     */
    public String getValue()
    {
        return value;
    }

    /**
     * Set the metadata value
     *
     * @param value new metadata value
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    /**
     * Creates a new metadata value.
     *
     * @param context
     *            DSpace context object
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void create(Context context) throws SQLException, AuthorizeException
    {
        // Create a table row and update it with the values
        row = DatabaseManager.row("MetadataValue");
        row.setColumn("item_id", itemId);
        row.setColumn("metadata_field_id", fieldId);
        row.setColumn("text_value", value);
        row.setColumn("text_lang", language);
        row.setColumn("place", place);
        DatabaseManager.insert(context, row);

        // Remember the new row number
        this.valueId = row.getIntColumn("metadata_value_id");

//        log.info(LogManager.getHeader(context, "create_metadata_value",
//                "metadata_value_id=" + valueId));
    }

    /**
     * Retrieves the metadata value from the database.
     *
     * @param context dspace context
     * @param valueId database key id of value
     * @return recalled metadata value
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     */
    public static MetadataValue find(Context context, int valueId)
            throws IOException, SQLException, AuthorizeException
    {
        // Grab rows from DB
        TableRowIterator tri = DatabaseManager.queryTable(context, "MetadataValue",
                "SELECT * FROM MetadataValue where metadata_value_id= ? ",
                valueId);

        TableRow row = null;
        try
        {
            if (tri.hasNext())
            {
                row = tri.next();
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
                tri.close();
        }

        if (row == null)
        {
            return null;
        }
        else
        {
            return new MetadataValue(row);
        }
    }

    /**
     * Retrieves the metadata values for a given field from the database.
     *
     * @param context dspace context
     * @param fieldId field whose values to look for
     * @return a collection of metadata values
     * @throws IOException
     * @throws SQLException
     * @throws AuthorizeException
     */
    public static java.util.Collection findByField(Context context, int fieldId)
            throws IOException, SQLException, AuthorizeException
    {
        // Grab rows from DB
        TableRowIterator tri = DatabaseManager.queryTable(context, "MetadataValue",
                "SELECT * FROM MetadataValue WHERE metadata_field_id= ? ",
                fieldId);

        TableRow row = null;
        java.util.Collection ret = new ArrayList();
        try
        {
            while (tri.hasNext())
            {
                row = tri.next();
                ret.add(new MetadataValue(row));
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
                tri.close();
        }

        return ret;
    }

    /**
     * Update the metadata value in the database.
     *
     * @param context dspace context
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void update(Context context) throws SQLException, AuthorizeException
    {
        row.setColumn("item_id", itemId);
        row.setColumn("metadata_field_id", fieldId);
        row.setColumn("text_value", value);
        row.setColumn("text_lang", language);
        row.setColumn("place", place);
        DatabaseManager.update(context, row);

        log.info(LogManager.getHeader(context, "update_metadatavalue",
                "metadata_value_id=" + getValueId()));
    }

    /**
     * Delete the metadata field.
     *
     * @param context dspace context
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void delete(Context context) throws SQLException, AuthorizeException
    {
        log.info(LogManager.getHeader(context, "delete_metadata_value",
                " metadata_value_id=" + getValueId()));
        DatabaseManager.delete(context, row);
    }
}
