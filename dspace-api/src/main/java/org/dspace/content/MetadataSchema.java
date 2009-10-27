/*
 * MetadataSchema.java
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
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * Class representing a schema in DSpace.
 * <p>
 * The schema object exposes a name which can later be used to generate
 * namespace prefixes in RDF or XML, e.g. the core DSpace Dublin Core schema
 * would have a name of <code>'dc'</code>.
 * </p>
 *
 * @author Martin Hald
 * @version $Revision$
 * @see org.dspace.content.MetadataValue, org.dspace.content.MetadataField
 */
public class MetadataSchema
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(MetadataSchema.class);

    /** Numeric Identifier of built-in Dublin Core schema. */
    public static final int DC_SCHEMA_ID = 1;

    /** Short Name of built-in Dublin Core schema. */
    public static final String DC_SCHEMA = "dc";

    /** The row in the table representing this type */
    private TableRow row;

    private int schemaID;
    private String namespace;
    private String name;

    // cache of schema by ID (Integer)
    private static HashMap id2schema = null;

    // cache of schema by short name
    private static HashMap name2schema = null;


    /**
     * Default constructor.
     */
    public MetadataSchema()
    {
    }

    /**
     * Object constructor.
     *
     * @param schemaID  database key ID number
     * @param namespace  XML namespace URI
     * @param name  short name of schema
     */
    public MetadataSchema(int schemaID, String namespace, String name)
    {
        this.schemaID = schemaID;
        this.namespace = namespace;
        this.name = name;
    }

    /**
     * Immutable object constructor for creating a new schema.
     *
     * @param namespace  XML namespace URI
     * @param name  short name of schema
     */
    public MetadataSchema(String namespace, String name)
    {
        this.namespace = namespace;
        this.name = name;
    }

    /**
     * Constructor for loading the metadata schema from the database.
     *
     * @param row table row object from which to populate this schema.
     */
    public MetadataSchema(TableRow row)
    {
        if (row != null)
        {
            this.schemaID = row.getIntColumn("metadata_schema_id");
            this.namespace = row.getStringColumn("namespace");
            this.name = row.getStringColumn("short_id");
            this.row = row;
        }
    }

    /**
     * Get the schema namespace.
     *
     * @return namespace String
     */
    public String getNamespace()
    {
        return namespace;
    }

    /**
     * Set the schema namespace.
     *
     * @param namespace  XML namespace URI
     */
    public void setNamespace(String namespace)
    {
        this.namespace = namespace;
    }

    /**
     * Get the schema name.
     *
     * @return name String
     */
    public String getName()
    {
        return name;
    }

    /**
     * Set the schema name.
     *
     * @param name  short name of schema
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Get the schema record key number.
     *
     * @return schema record key
     */
    public int getSchemaID()
    {
        return schemaID;
    }

    /**
     * Creates a new metadata schema in the database, out of this object.
     *
     * @param context
     *            DSpace context object
     * @throws SQLException
     * @throws AuthorizeException
     * @throws NonUniqueMetadataException
     */
    public void create(Context context) throws SQLException,
            AuthorizeException, NonUniqueMetadataException
    {
        // Check authorisation: Only admins may create metadata schemas
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only administrators may modify the metadata registry");
        }

        // Ensure the schema name is unique
        if (!uniqueShortName(context, name))
        {
            throw new NonUniqueMetadataException("Please make the name " + name
                    + " unique");
        }
        
        // Ensure the schema namespace is unique
        if (!uniqueNamespace(context, namespace))
        {
            throw new NonUniqueMetadataException("Please make the namespace " + namespace
                    + " unique");
        }


        // Create a table row and update it with the values
        row = DatabaseManager.create(context, "MetadataSchemaRegistry");
        row.setColumn("namespace", namespace);
        row.setColumn("short_id", name);
        DatabaseManager.update(context, row);

        // invalidate our fast-find cache.
        decache();

        // Remember the new row number
        this.schemaID = row.getIntColumn("metadata_schema_id");

        log
                .info(LogManager.getHeader(context, "create_metadata_schema",
                        "metadata_schema_id="
                                + row.getIntColumn("metadata_schema_id")));
    }

    /**
     * Get the schema object corresponding to this namespace URI.
     *
     * @param context DSpace context
     * @param namespace namespace URI to match
     * @return metadata schema object or null if none found.
     * @throws SQLException
     */
    public static MetadataSchema findByNamespace(Context context,
            String namespace) throws SQLException
    {
        // Grab rows from DB
        TableRowIterator tri = DatabaseManager.queryTable(context,"MetadataSchemaRegistry",
                "SELECT * FROM MetadataSchemaRegistry WHERE namespace= ? ", 
                namespace);

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
            return new MetadataSchema(row);
        }
    }

    /**
     * Update the metadata schema in the database.
     *
     * @param context DSpace context
     * @throws SQLException
     * @throws AuthorizeException
     * @throws NonUniqueMetadataException
     */
    public void update(Context context) throws SQLException,
            AuthorizeException, NonUniqueMetadataException
    {
        // Check authorisation: Only admins may update the metadata registry
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only administrators may modify the metadata registry");
        }

        // Ensure the schema name is unique
        if (!uniqueShortName(context, name))
        {
            throw new NonUniqueMetadataException("Please make the name " + name
                    + " unique");
        }

        // Ensure the schema namespace is unique
        if (!uniqueNamespace(context, namespace))
        {
            throw new NonUniqueMetadataException("Please make the namespace " + namespace
                    + " unique");
        }
        
        row.setColumn("namespace", getNamespace());
        row.setColumn("short_id", getName());
        DatabaseManager.update(context, row);

        decache();

        log.info(LogManager.getHeader(context, "update_metadata_schema",
                "metadata_schema_id=" + getSchemaID() + "namespace="
                        + getNamespace() + "name=" + getName()));
    }

    /**
     * Delete the metadata schema.
     *
     * @param context DSpace context
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

        log.info(LogManager.getHeader(context, "delete_metadata_schema",
                "metadata_schema_id=" + getSchemaID()));

        DatabaseManager.delete(context, row);
    }

    /**
     * Return all metadata schemas.
     *
     * @param context DSpace context
     * @return array of metadata schemas
     * @throws SQLException
     */
    public static MetadataSchema[] findAll(Context context) throws SQLException
    {
        List schemas = new ArrayList();

        // Get all the metadataschema rows
        TableRowIterator tri = DatabaseManager.queryTable(context, "MetadataSchemaRegistry",
                        "SELECT * FROM MetadataSchemaRegistry ORDER BY metadata_schema_id");

        try
        {
            // Make into DC Type objects
            while (tri.hasNext())
            {
                schemas.add(new MetadataSchema(tri.next()));
            }
        }
        finally
        {
            // close the TableRowIterator to free up resources
            if (tri != null)
                tri.close();
        }

        // Convert list into an array
        MetadataSchema[] typeArray = new MetadataSchema[schemas.size()];
        return (MetadataSchema[]) schemas.toArray(typeArray);
    }

    /**
     * Return true if and only if the passed name appears within the allowed
     * number of times in the current schema.
     *
     * @param context DSpace context
     * @param namespace namespace URI to match
     * @return true of false
     * @throws SQLException
     */
    private boolean uniqueNamespace(Context context, String namespace)
            throws SQLException
    {
        int count = 0;
        Connection con = context.getDBConnection();
        PreparedStatement statement = null;
        ResultSet rs = null;

        try
        {
            TableRow reg = DatabaseManager.row("MetadataSchemaRegistry");

            String query = "SELECT COUNT(*) FROM " + reg.getTable() + " " +
                    "WHERE metadata_schema_id != ? " +
                    "AND namespace= ? ";

            statement = con.prepareStatement(query);
            statement.setInt(1,schemaID);
            statement.setString(2,namespace);

            rs = statement.executeQuery();

            if (rs.next())
            {
                count = rs.getInt(1);
            }
        }
        finally
        {
            if (rs != null)
            {
                try { rs.close(); } catch (SQLException sqle) { }
            }

            if (statement != null)
            {
                try { statement.close(); } catch (SQLException sqle) { }
            }
        }

        return (count == 0);
    }

    /**
     * Return true if and only if the passed name is unique.
     *
     * @param context DSpace context
     * @param name  short name of schema
     * @return true of false
     * @throws SQLException
     */
    private boolean uniqueShortName(Context context, String name)
            throws SQLException
    {
        int count = 0;
        Connection con = context.getDBConnection();
        PreparedStatement statement = null;
        ResultSet rs = null;

        try
        {
            TableRow reg = DatabaseManager.row("MetadataSchemaRegistry");

            String query = "SELECT COUNT(*) FROM " + reg.getTable() + " " +
                    "WHERE metadata_schema_id != ? " +
                    "AND short_id = ? ";

            statement = con.prepareStatement(query);
            statement.setInt(1,schemaID);
            statement.setString(2,name);

            rs = statement.executeQuery();

            if (rs.next())
            {
                count = rs.getInt(1);
            }
        }
        finally
        {
            if (rs != null)
            {
                try { rs.close(); } catch (SQLException sqle) { }
            }

            if (statement != null)
            {
                try { statement.close(); } catch (SQLException sqle) { }
            }
        }

        return (count == 0);
    }

    /**
     * Get the schema corresponding with this numeric ID.
     * The ID is a database key internal to DSpace.
     *
     * @param context
     *            context, in case we need to read it in from DB
     * @param id
     *            the schema ID
     * @return the metadata schema object
     * @throws SQLException
     */
    public static MetadataSchema find(Context context, int id)
            throws SQLException
    {
        initCache(context);
        Integer iid = new Integer(id);

        // sanity check
        if (!id2schema.containsKey(iid))
            return null;

        return (MetadataSchema) id2schema.get(iid);
    }

    /**
     * Get the schema corresponding with this short name.
     *
     * @param context
     *            context, in case we need to read it in from DB
     * @param shortName
     *            the short name for the schema
     * @return the metadata schema object
     * @throws SQLException
     */
    public static MetadataSchema find(Context context, String shortName)
        throws SQLException
    {
        // If we are not passed a valid schema name then return
        if (shortName == null)
            return null;

        initCache(context);

        if (!name2schema.containsKey(shortName))
            return null;

        return (MetadataSchema) name2schema.get(shortName);
    }

    // invalidate the cache e.g. after something modifies DB state.
    private static void decache()
    {
        id2schema = null;
        name2schema = null;
    }

    // load caches if necessary
    private static void initCache(Context context) throws SQLException
    {
        if (id2schema != null && name2schema != null)
            return;

        synchronized (MetadataSchema.class)
        {
            if (id2schema == null && name2schema == null)
            {
                log.info("Loading schema cache for fast finds");
                HashMap new_id2schema = new HashMap();
                HashMap new_name2schema = new HashMap();

                TableRowIterator tri = DatabaseManager.queryTable(context,"MetadataSchemaRegistry",
                        "SELECT * from MetadataSchemaRegistry");

                try
                {
                    while (tri.hasNext())
                    {
                        TableRow row = tri.next();

                        MetadataSchema s = new MetadataSchema(row);
                        new_id2schema.put(new Integer(s.schemaID), s);
                        new_name2schema.put(s.name, s);
                    }
                }
                finally
                {
                    // close the TableRowIterator to free up resources
                    if (tri != null)
                        tri.close();
                }

                id2schema = new_id2schema;
                name2schema = new_name2schema;
            }
        }
    }
}
