/*
 * MetadataField.java
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * DSpace object that represents a metadata field, which is
 * defined by a combination of schema, element, and qualifier.  Every
 * metadata element belongs in a field.
 *
 * @author Martin Hald
 * @version $Revision$
 * @see org.dspace.content.MetadataValue, org.dspace.content.MetadataSchema
 */
public class MetadataField
{
    private int fieldID = 0;
    private int schemaID = 0;
    private String element;
    private String qualifier;
    private String scopeNote;

    /** log4j logger */
    private static Logger log = Logger.getLogger(MetadataField.class);

    /** The row in the table representing this type */
    private TableRow row;

    // cache of field by ID (Integer)
    private static HashMap id2field = null;


    /**
     * Default constructor.
     */
    public MetadataField()
    {
    }

    /**
     * Constructor creating a field within a schema.
     *
     * @param schema schema to which the field belongs
     */
    public MetadataField(MetadataSchema schema)
    {
        this.schemaID = schema.getSchemaID();
    }

    /**
     * Full contructor for new metadata field elements.
     *
     * @param schema schema to which the field belongs
     * @param element element of the field
     * @param qualifier qualifier of the field
     * @param scopeNote scope note of the field
     */
    public MetadataField(MetadataSchema schema, String element,
            String qualifier, String scopeNote)
    {
        this.schemaID = schema.getSchemaID();
        this.element = element;
        this.qualifier = qualifier;
        this.scopeNote = scopeNote;
    }

    /**
     * Full construtor for existing metadata field elements.
     *
     * @param schemaID schema to which the field belongs
     * @param fieldID dataabse ID of field.
     * @param element element of the field
     * @param qualifier qualifier of the field
     * @param scopeNote scope note of the field
     */
    public MetadataField(int schemaID, int fieldID, String element,
            String qualifier, String scopeNote)
    {
        this.schemaID = schemaID;
        this.fieldID = fieldID;
        this.element = element;
        this.qualifier = qualifier;
        this.scopeNote = scopeNote;
    }

    /**
     * Constructor to load the object from the database.
     *
     * @param row database row from which to populate object.
     */
    public MetadataField(TableRow row)
    {
        if (row != null)
        {
            this.fieldID = row.getIntColumn("metadata_field_id");
            this.schemaID = row.getIntColumn("metadata_schema_id");
            this.element = row.getStringColumn("element");
            this.qualifier = row.getStringColumn("qualifier");
            this.scopeNote = row.getStringColumn("scope_note");
            this.row = row;
        }
    }

    /**
     * Get the element name.
     *
     * @return element name
     */
    public String getElement()
    {
        return element;
    }

    /**
     * Set the element name.
     *
     * @param element new value for element
     */
    public void setElement(String element)
    {
        this.element = element;
    }

    /**
     * Get the metadata field id.
     *
     * @return metadata field id
     */
    public int getFieldID()
    {
        return fieldID;
    }

    /**
     * Get the qualifier.
     *
     * @return qualifier
     */
    public String getQualifier()
    {
        return qualifier;
    }

    /**
     * Set the qualifier.
     *
     * @param qualifier new value for qualifier
     */
    public void setQualifier(String qualifier)
    {
        this.qualifier = qualifier;
    }

    /**
     * Get the schema record key.
     *
     * @return schema record key
     */
    public int getSchemaID()
    {
        return schemaID;
    }

    /**
     * Set the schema record key.
     *
     * @param schemaID new value for key
     */
    public void setSchemaID(int schemaID)
    {
        this.schemaID = schemaID;
    }

    /**
     * Get the scope note.
     *
     * @return scope note
     */
    public String getScopeNote()
    {
        return scopeNote;
    }

    /**
     * Set the scope note.
     *
     * @param scopeNote new value for scope note
     */
    public void setScopeNote(String scopeNote)
    {
        this.scopeNote = scopeNote;
    }

    /**
     * Creates a new metadata field.
     *
     * @param context
     *            DSpace context object
     * @throws IOException
     * @throws AuthorizeException
     * @throws SQLException
     * @throws NonUniqueMetadataException
     */
    public void create(Context context) throws IOException, AuthorizeException,
            SQLException, NonUniqueMetadataException
    {
        // Check authorisation: Only admins may create DC types
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only administrators may modify the metadata registry");
        }

        // Ensure the element and qualifier are unique within a given schema.
        if (!unique(context, schemaID, element, qualifier))
        {
            throw new NonUniqueMetadataException("Please make " + element + "."
                    + qualifier + " unique within schema #" + schemaID);
        }

        // Create a table row and update it with the values
        row = DatabaseManager.row("MetadataFieldRegistry");
        row.setColumn("metadata_schema_id", schemaID);
        row.setColumn("element", element);
        row.setColumn("qualifier", qualifier);
        row.setColumn("scope_note", scopeNote);
        DatabaseManager.insert(context, row);
        decache();

        // Remember the new row number
        this.fieldID = row.getIntColumn("metadata_field_id");

        log.info(LogManager.getHeader(context, "create_metadata_field",
                "metadata_field_id=" + row.getIntColumn("metadata_field_id")));
    }

    /**
     * Retrieves the metadata field from the database.
     *
     * @param context dspace context
     * @param schemaID schema by ID
     * @param element element name
     * @param qualifier qualifier (may be ANY or null)
     * @return recalled metadata field
     * @throws SQLException
     * @throws AuthorizeException
     */
    public static MetadataField findByElement(Context context, int schemaID,
            String element, String qualifier) throws SQLException,
            AuthorizeException
    {
        // Grab rows from DB
        TableRowIterator tri;
        if (qualifier == null)
        {
        	tri = DatabaseManager.queryTable(context,"MetadataFieldRegistry",
                    "SELECT * FROM MetadataFieldRegistry WHERE metadata_schema_id= ? " + 
                    "AND element= ?  AND qualifier is NULL ",
                    schemaID, element);
        } 
        else
        {
        	tri = DatabaseManager.queryTable(context,"MetadataFieldRegistry",
                    "SELECT * FROM MetadataFieldRegistry WHERE metadata_schema_id= ? " + 
                    "AND element= ?  AND qualifier= ? ",
                    schemaID, element, qualifier);
        }
        

        TableRow row = null;
        if (tri.hasNext())
        {
            row = tri.next();
        }

        // close the TableRowIterator to free up resources
        tri.close();

        if (row == null)
        {
            return null;
        }
        else
        {
            return new MetadataField(row);
        }
    }

    /**
     * Retrieve all Dublin Core types from the registry
     *
     * @param context dspace context
     * @return an array of all the Dublin Core types
     * @throws SQLException
     */
    public static MetadataField[] findAll(Context context) throws SQLException
    {
        List fields = new ArrayList();

        // Get all the metadatafieldregistry rows
        TableRowIterator tri = DatabaseManager.queryTable(context, "MetadataFieldRegistry",
                               "SELECT mfr.* FROM MetadataFieldRegistry mfr, MetadataSchemaRegistry msr where mfr.metadata_schema_id= msr.metadata_schema_id ORDER BY msr.short_id,  mfr.element, mfr.qualifier");
 
        // Make into DC Type objects
        while (tri.hasNext())
        {
            fields.add(new MetadataField(tri.next()));
        }
        // close the TableRowIterator to free up resources
        tri.close();

        // Convert list into an array
        MetadataField[] typeArray = new MetadataField[fields.size()];
        return (MetadataField[]) fields.toArray(typeArray);
    }

    /**
     * Return all metadata fields that are found in a given schema.
     *
     * @param context dspace context
     * @param schemaID schema by db ID
     * @return array of metadata fields
     * @throws SQLException
     */
    public static MetadataField[] findAllInSchema(Context context, int schemaID)
            throws SQLException
    {
        List fields = new ArrayList();

        // Get all the metadatafieldregistry rows
        TableRowIterator tri = DatabaseManager.queryTable(context,"MetadataFieldRegistry",
                "SELECT * FROM MetadataFieldRegistry WHERE metadata_schema_id= ? " +
                " ORDER BY element, qualifier", schemaID);

        // Make into DC Type objects
        while (tri.hasNext())
        {
            fields.add(new MetadataField(tri.next()));
        }
        // close the TableRowIterator to free up resources
        tri.close();

        // Convert list into an array
        MetadataField[] typeArray = new MetadataField[fields.size()];
        return (MetadataField[]) fields.toArray(typeArray);
    }

    /**
     * Update the metadata field in the database.
     *
     * @param context dspace context
     * @throws SQLException
     * @throws AuthorizeException
     * @throws NonUniqueMetadataException
     * @throws IOException
     */
    public void update(Context context) throws SQLException,
            AuthorizeException, NonUniqueMetadataException, IOException
    {
        // Check authorisation: Only admins may update the metadata registry
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only administrators may modiffy the Dublin Core registry");
        }

        // Check to see if the schema ID was altered. If is was then we will
        // query to ensure that there is not already a duplicate name field.
        if (row.getIntColumn("metadata_schema_id") != schemaID)
        {
            if (MetadataField.hasElement(context, schemaID, element, qualifier))
            {
                throw new NonUniqueMetadataException(
                        "Duplcate field name found in target schema");
            }
        }

        // Ensure the element and qualifier are unique within a given schema.
        if (!unique(context, schemaID, element, qualifier))
        {
            throw new NonUniqueMetadataException("Please make " + element + "."
                    + qualifier);
        }

        row.setColumn("metadata_schema_id", schemaID);
        row.setColumn("element", element);
        row.setColumn("qualifier", qualifier);
        row.setColumn("scope_note", scopeNote);
        DatabaseManager.update(context, row);
        decache();

        log.info(LogManager.getHeader(context, "update_metadatafieldregistry",
                "metadata_field_id=" + getFieldID() + "element=" + getElement()
                        + "qualifier=" + getQualifier()));
    }

    /**
     * Return true if and only if the schema has a field with the given element
     * and qualifier pair.
     *
     * @param context dspace context
     * @param schemaID schema by ID
     * @param element element name
     * @param qualifier qualifier name
     * @return true if the field exists
     * @throws SQLException
     * @throws AuthorizeException
     */
    private static boolean hasElement(Context context, int schemaID,
            String element, String qualifier) throws SQLException,
            AuthorizeException
    {
        return MetadataField.findByElement(context, schemaID, element,
                qualifier) != null;
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
        // Check authorisation: Only admins may create DC types
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only administrators may modify the metadata registry");
        }

        log.info(LogManager.getHeader(context, "delete_metadata_field",
                "metadata_field_id=" + getFieldID()));

        DatabaseManager.delete(context, row);
        decache();
    }

    /**
     * A sanity check that ensures a given element and qualifier are unique
     * within a given schema. The check happens in code as we cannot use a
     * database constraint.
     *
     * @param context dspace context
     * @param schemaID
     * @param element
     * @param qualifier
     * @return true if unique
     * @throws AuthorizeException
     * @throws SQLException
     * @throws IOException
     */
    private boolean unique(Context context, int schemaID, String element,
            String qualifier) throws IOException, SQLException,
            AuthorizeException
    {
        Connection con = context.getDBConnection();
        TableRow reg = DatabaseManager.row("MetadataFieldRegistry");
        
        String qualifierClause = "";

        if (qualifier == null)
        {
            qualifierClause = "and qualifier is null";
        }
        else
        {
            qualifierClause = "and qualifier = ?";
        }

        String query = "SELECT COUNT(*) FROM " + reg.getTable()
        	+ " WHERE metadata_schema_id= ? "
        	+ " and metadata_field_id != ? "
        	+ " and element= ? " + qualifierClause;

        PreparedStatement statement = con.prepareStatement(query);
        statement.setInt(1,schemaID);
        statement.setInt(2,fieldID);
        statement.setString(3,element);
        
        if (qualifier != null)
        {
            statement.setString(4,qualifier);
        }
        
        ResultSet rs = statement.executeQuery();

        int count = 0;
        if (rs.next())
        {
            count = rs.getInt(1);
        }

        return (count == 0);
    }

    /**
     * Return the HTML FORM key for the given field.
     *
     * @param schema
     * @param element
     * @param qualifier
     * @return HTML FORM key
     */
    public static String formKey(String schema, String element, String qualifier)
    {
        if (qualifier == null)
        {
            return schema + "_" + element;
        }
        else
        {
            return schema + "_" + element + "_" + qualifier;
        }
    }

    /**
     * Find the field corresponding to the given numeric ID.  The ID is
     * a database key internal to DSpace.
     *
     * @param context
     *            context, in case we need to read it in from DB
     * @param id
     *            the metadata field ID
     * @return the metadata field object
     * @throws SQLException
     */
    public static MetadataField find(Context context, int id)
            throws SQLException
    {
        initCache(context);

        // 'sanity check' first.
        Integer iid = new Integer(id);
        if (!id2field.containsKey(iid))
            return null;

        return (MetadataField) id2field.get(iid);
    }

    // invalidate the cache e.g. after something modifies DB state.
    private static void decache()
    {
        id2field = null;
    }

    // load caches if necessary
    private static void initCache(Context context) throws SQLException
    {
        if (id2field != null)
            return;
        id2field = new HashMap();
        log.info("Loading MetadataField elements into cache.");

        // Grab rows from DB
        TableRowIterator tri = DatabaseManager.queryTable(context,"MetadataFieldRegistry",
                "SELECT * from MetadataFieldRegistry");

        while (tri.hasNext())
        {
            TableRow row = tri.next();
            int fieldID = row.getIntColumn("metadata_field_id");
            id2field.put(new Integer(fieldID), new MetadataField(row));
        }

        // close the TableRowIterator to free up resources
        tri.close();
    }
}
