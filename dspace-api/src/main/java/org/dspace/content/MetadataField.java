/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
import java.util.Map;

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
 * @see org.dspace.content.MetadataValue
 * @see org.dspace.content.MetadataSchema
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
    private static Map<Integer, MetadataField> id2field = null;

    /** metadatafield cache */
    private static Map<String, MetadataField> metadatafieldcache = null;


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
     * Full constructor for new metadata field elements.
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
     * Full constructor for existing metadata field elements.
     *
     * @param schemaID schema to which the field belongs
     * @param fieldID database ID of field.
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
            String element, String qualifier) throws SQLException
    {

        if (!isCacheInitialized()){
            initCache(context);
        }

        // 'sanity check' first.
        String metadataFieldKey = schemaID+"."+element+"."+qualifier;
        if(!metadatafieldcache.containsKey(metadataFieldKey)) {
            return null;
        }

        return metadatafieldcache.get(metadataFieldKey);
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
        List<MetadataField> fields = new ArrayList<MetadataField>();

        // Get all the metadatafieldregistry rows
        TableRowIterator tri = DatabaseManager.queryTable(context, "MetadataFieldRegistry",
                               "SELECT mfr.* FROM MetadataFieldRegistry mfr, MetadataSchemaRegistry msr where mfr.metadata_schema_id= msr.metadata_schema_id ORDER BY msr.short_id,  mfr.element, mfr.qualifier");

        try
        {
            // Make into DC Type objects
            while (tri.hasNext())
            {
                fields.add(new MetadataField(tri.next()));
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
        List<MetadataField> fields = new ArrayList<MetadataField>();

        // Get all the metadatafieldregistry rows
        TableRowIterator tri = DatabaseManager.queryTable(context,"MetadataFieldRegistry",
                "SELECT * FROM MetadataFieldRegistry WHERE metadata_schema_id= ? " +
                " ORDER BY element, qualifier", schemaID);

        try
        {
            // Make into DC Type objects
            while (tri.hasNext())
            {
                fields.add(new MetadataField(tri.next()));
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
        int count = 0;
        Connection con = null;
        PreparedStatement statement = null;
        ResultSet rs = null;

        try
        {
            con = context.getDBConnection();
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

            statement = con.prepareStatement(query);
            statement.setInt(1,schemaID);
            statement.setInt(2,fieldID);
            statement.setString(3,element);

            if (qualifier != null)
            {
                statement.setString(4,qualifier);
            }

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
        if (!isCacheInitialized())
        {
            initCache(context);
        }

        // 'sanity check' first.
        Integer iid = Integer.valueOf(id);
        if (!id2field.containsKey(iid))
        {
            return null;
        }

        return id2field.get(iid);
    }

    // invalidate the cache e.g. after something modifies DB state.
    private static void decache()
    {
        id2field = null;
    }

    private static boolean isCacheInitialized()
    {
        return id2field != null;
    }
    
    // load caches if necessary
    private static synchronized void initCache(Context context) throws SQLException
    {
        if (!isCacheInitialized())
        {
            Map<Integer, MetadataField> new_id2field = new HashMap<Integer, MetadataField>();
            Map<String, MetadataField> new_metadatafieldcache = new HashMap<String, MetadataField>();
            log.info("Loading MetadataField elements into cache.");

            // Grab rows from DB
            TableRowIterator tri = DatabaseManager.queryTable(context,"MetadataFieldRegistry",
                    "SELECT * from MetadataFieldRegistry");

            try
            {
                while (tri.hasNext())
                {
                    TableRow row = tri.next();
                    int fieldID = row.getIntColumn("metadata_field_id");
                    MetadataField metadataField = new MetadataField(row);
                    new_id2field.put(Integer.valueOf(fieldID), metadataField);
                    new_metadatafieldcache.put(metadataField.getSchemaID()+"."+metadataField.getElement()+"."+metadataField.getQualifier(), metadataField);
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

            id2field = new_id2field;
            metadatafieldcache = new_metadatafieldcache;
        }
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same MetadataField
     * as this object, <code>false</code> otherwise
     *
     * @param obj
     *            object to compare to
     *
     * @return <code>true</code> if object passed in represents the same
     *         MetadataField as this object
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final MetadataField other = (MetadataField) obj;
        if (this.fieldID != other.fieldID)
        {
            return false;
        }
        if (this.schemaID != other.schemaID)
        {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 47 * hash + this.fieldID;
        hash = 47 * hash + this.schemaID;
        return hash;
    }
}
