package org.dspace.content;

import org.apache.log4j.Logger;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * DSpace object that represents a metadata field, which is
 * defined by a combination of schema, element, and qualifier.  Every
 * metadata element belongs in a field.
 *
 * @author Martin Hald
 * @version $Revision: 1.3 $
 * @see MetadataValue, org.dspace.content.MetadataSchema
 */
public class ResourceMetadataField {
    private int fieldID = 0;
    private int resourceType = -1;
    private String element;
    private String qualifier;
    private String scopeNote;

    /** log4j logger */
    private static Logger log = Logger.getLogger(ResourceMetadataField.class);

    /** The row in the table representing this type */
    private TableRow row;

    // cache of field by ID (Integer)
    private static HashMap id2field = null;

    private boolean oracle;

    /**
     * Default constructor.
     */
    public ResourceMetadataField()
    {
        oracle = "oracle".equals(ConfigurationManager.getProperty("db.name"));
    }

//    /**
//     * Full construtor for existing metadata field elements.
//     *
//     * @param schemaID schema to which the field belongs
//     * @param fieldID dataabse ID of field.
//     * @param element element of the field
//     * @param qualifier qualifier of the field
//     * @param scopeNote scope note of the field
//     */
//    public ResourceMetadataField(int schemaID, int fieldID, String element,
//            String qualifier, String scopeNote)
//    {
//        this.schemaID = schemaID;
//        this.fieldID = fieldID;
//        this.element = element;
//        this.qualifier = qualifier;
//        this.scopeNote = scopeNote;
//    }

    /**
     * Constructor to load the object from the database.
     *
     * @param row database row from which to populate object.
     */
    public ResourceMetadataField(TableRow row)
    {
        if (row != null)
        {
            this.fieldID = row.getIntColumn("metadata_field_id");
            this.resourceType = row.getIntColumn("resource_type_id");
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


    public int getResourceType() {
        return resourceType;
    }

    public void setResourceType(int resourceType) {
        this.resourceType = resourceType;
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
     * @throws java.io.IOException
     * @throws org.dspace.authorize.AuthorizeException
     * @throws java.sql.SQLException
     * @throws org.dspace.content.NonUniqueMetadataException
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
        if (!unique(context, resourceType, element, qualifier))
        {
            throw new NonUniqueMetadataException("Please make " + element + "."
                    + qualifier + " unique within resourcetype #" + resourceType);
        }

        // Create a table row and update it with the values
        row = DatabaseManager.row((oracle ? "RMetadataFieldRegistry" : "ResourceMetadataFieldRegistry"));
        row.setColumn("resource_type_id", resourceType);
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
     * @param resourceTypeId schema by ID
     * @param element element name
     * @param qualifier qualifier (may be ANY or null)
     * @return recalled metadata field
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     */
    public static ResourceMetadataField findByElement(Context context, int resourceTypeId,
                                                      String element, String qualifier) throws SQLException,
            AuthorizeException
    {
        boolean oracle = "oracle".equals(ConfigurationManager.getProperty("db.name"));
        // Grab rows from DB
        TableRowIterator tri;
        if (qualifier == null)
        {
            tri = DatabaseManager.queryTable(context,(oracle ? "RMetadataFieldRegistry" : "ResourceMetadataFieldRegistry"),
                    "SELECT * FROM " + (oracle ? "RMetadataFieldRegistry" : "ResourceMetadataFieldRegistry") + " WHERE resource_type_id= ? " +
                            "AND element= ?  AND qualifier is NULL ",
                    resourceTypeId, element);
        }
        else
        {
            tri = DatabaseManager.queryTable(context, (oracle ? "RMetadataFieldRegistry" : "ResourceMetadataFieldRegistry"),
                    "SELECT * FROM " + (oracle ? "RMetadataFieldRegistry" : "ResourceMetadataFieldRegistry") + " WHERE resource_type_id= ? " +
                            "AND element= ?  AND qualifier= ? ",
                    resourceTypeId, element, qualifier);
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
            return new ResourceMetadataField(row);
        }
    }

    /**
     * Retrieve all Dublin Core types from the registry
     *
     * @param context dspace context
     * @return an array of all the Dublin Core types
     * @throws java.sql.SQLException
     */
    public static ResourceMetadataField[] findAll(Context context) throws SQLException
    {
        List fields = new ArrayList();
        boolean oracle = "oracle".equals(ConfigurationManager.getProperty("db.name"));

        // Get all the metadatafieldregistry rows
        //TODO: this don't work cause there is NO table ResourceMetadataSchemaRegistry
        TableRowIterator tri = DatabaseManager.queryTable(context, (oracle ? "RMetadataFieldRegistry" : "ResourceMetadataFieldRegistry"),
                "SELECT mfr.* FROM " + (oracle ? "RMetadataFieldRegistry" : "ResourceMetadataFieldRegistry") + " mfr, ResourceMetadataSchemaRegistry msr where mfr.resource_type_id= msr.resource_type_id ORDER BY msr.short_id,  mfr.element, mfr.qualifier");

        // Make into DC Type objects
        while (tri.hasNext())
        {
            fields.add(new ResourceMetadataField(tri.next()));
        }
        // close the TableRowIterator to free up resources
        tri.close();

        // Convert list into an array
        ResourceMetadataField[] typeArray = new ResourceMetadataField[fields.size()];
        return (ResourceMetadataField[]) fields.toArray(typeArray);
    }

    /**
     * Return all metadata fields that are found in a given schema.
     *
     * @param context dspace context
     * @param resourceTypeId schema by db ID
     * @return array of metadata fields
     * @throws java.sql.SQLException
     */
    public static ResourceMetadataField[] findAllInSchema(Context context, int resourceTypeId)
            throws SQLException
    {
        List fields = new ArrayList();
        boolean oracle = "oracle".equals(ConfigurationManager.getProperty("db.name"));

        // Get all the metadatafieldregistry rows
        TableRowIterator tri = DatabaseManager.queryTable(context,(oracle ? "RMetadataFieldRegistry" : "ResourceMetadataFieldRegistry"),
                "SELECT * FROM " + (oracle ? "RMetadataFieldRegistry" : "ResourceMetadataFieldRegistry") + " WHERE resource_type_id= ? " +
                        " ORDER BY element, qualifier", resourceTypeId);

        // Make into DC Type objects
        while (tri.hasNext())
        {
            fields.add(new ResourceMetadataField(tri.next()));
        }
        // close the TableRowIterator to free up resources
        tri.close();

        // Convert list into an array
        ResourceMetadataField[] typeArray = new ResourceMetadataField[fields.size()];
        return (ResourceMetadataField[]) fields.toArray(typeArray);
    }

    /**
     * Return all metadata fields that are found in a given schema.
     *
     * @param context dspace context
     * @param resourceTypeId schema by db ID
     * @return array of metadata fields
     * @throws java.sql.SQLException
     */
    public static ResourceMetadataField[] findAllInSchemaIdOrder(Context context, int resourceTypeId)
            throws SQLException
    {
        List fields = new ArrayList();
        boolean oracle = "oracle".equals(ConfigurationManager.getProperty("db.name"));

        // Get all the metadatafieldregistry rows
        TableRowIterator tri = DatabaseManager.queryTable(context, (oracle ? "RMetadataFieldRegistry" : "ResourceMetadataFieldRegistry"),
                "SELECT * FROM " + (oracle ? "RMetadataFieldRegistry" : "ResourceMetadataFieldRegistry") +" WHERE resource_type_id= ? " +
                        " ORDER BY metadata_field_id", resourceTypeId);

        // Make into DC Type objects
        while (tri.hasNext())
        {
            fields.add(new ResourceMetadataField(tri.next()));
        }
        // close the TableRowIterator to free up resources
        tri.close();

        // Convert list into an array
        ResourceMetadataField[] typeArray = new ResourceMetadataField[fields.size()];
        return (ResourceMetadataField[]) fields.toArray(typeArray);
    }

    /**
     * Update the metadata field in the database.
     *
     * @param context dspace context
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     * @throws org.dspace.content.NonUniqueMetadataException
     * @throws java.io.IOException
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
        if (row.getIntColumn("resource_type_id") != resourceType)
        {
            if (hasElement(context, resourceType, element, qualifier))
            {
                throw new NonUniqueMetadataException(
                        "Duplcate field name found in target schema");
            }
        }

        // Ensure the element and qualifier are unique within a given schema.
        if (!unique(context, resourceType, element, qualifier))
        {
            throw new NonUniqueMetadataException("Please make " + element + "."
                    + qualifier);
        }

        row.setColumn("resource_type_id", resourceType);
        row.setColumn("element", element);
        row.setColumn("qualifier", qualifier);
        row.setColumn("scope_note", scopeNote);
        DatabaseManager.update(context, row);
        decache();

        log.info(LogManager.getHeader(context, "update_resourcemetadatafieldregistry",
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
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     */
    private static boolean hasElement(Context context, int schemaID,
                                      String element, String qualifier) throws SQLException,
            AuthorizeException
    {
        return findByElement(context, schemaID, element,
                qualifier) != null;
    }

    /**
     * Delete the metadata field.
     *
     * @param context dspace context
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
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
     * @param resourceTypeId
     * @param element
     * @param qualifier
     * @return true if unique
     * @throws org.dspace.authorize.AuthorizeException
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     */
    private boolean unique(Context context, int resourceTypeId, String element,
                           String qualifier) throws IOException, SQLException,
            AuthorizeException
    {
        Connection con = context.getDBConnection();
        TableRow reg = DatabaseManager.row((oracle ? "RMetadataFieldRegistry" : "ResourceMetadataFieldRegistry"));

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
                + " WHERE resource_type_id= ? "
                + " and metadata_field_id != ? "
                + " and element= ? " + qualifierClause;

        PreparedStatement statement = con.prepareStatement(query);
        statement.setInt(1,resourceTypeId);
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
     * @throws java.sql.SQLException
     */
    public static ResourceMetadataField find(Context context, int id)
            throws SQLException
    {
        initCache(context);

        // 'sanity check' first.
        Integer iid = new Integer(id);
        if (!id2field.containsKey(iid))
            return null;

        return (ResourceMetadataField) id2field.get(iid);
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
        boolean oracle = "oracle".equals(ConfigurationManager.getProperty("db.name"));

        // Grab rows from DB
        TableRowIterator tri = DatabaseManager.queryTable(context,(oracle ? "RMetadataFieldRegistry" : "ResourceMetadataFieldRegistry"),
                "SELECT * from "+ (oracle ? "RMetadataFieldRegistry" : "ResourceMetadataFieldRegistry"));

        while (tri.hasNext())
        {
            TableRow row = tri.next();
            int fieldID = row.getIntColumn("metadata_field_id");
            id2field.put(new Integer(fieldID), new ResourceMetadataField(row));
        }

        // close the TableRowIterator to free up resources
        tri.close();
    }
}
