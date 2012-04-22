package org.dspace.content;

import org.apache.log4j.Logger;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.authorize.AuthorizeException;

import java.sql.SQLException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Database access class representing a Dublin Core metadata value.
 * It represents a value of a given <code>MetadataField</code> on an Item.
 * (The Item can have many values of the same field.)  It contains                                           element, qualifier, value and language.
 * the field (which names the schema, element, and qualifer), language,
 * and a value.
 *
 * @author Martin Hald
 * @see MetadataSchema, org.dspace.content.MetadataField
 */
public class ResourceMetadataValue {
    /** The reference to the metadata field */
    private int fieldId = 0;

    /** The primary key for the metadata value */
    private int valueId = 0;

    /** The reference to the DSpace resource */
    private int resourceId;

    /** The reference to the DSpace resource type */
    private int resourceTypeId;

    /** The value of the field */
    public String value;

    /** The language of the field, may be <code>null</code> */
    public String language;

    /** The position of the record. */
    public int place = 1;

    /** log4j logger */
    private static Logger log = Logger.getLogger(ResourceMetadataValue.class);

    /** The row in the table representing this type */
    private TableRow row;

    private boolean oracle;

    /**
     * Construct the metadata object from the matching database row.
     *
     * @param row database row to use for contents
     */
    public ResourceMetadataValue(TableRow row)
    {
        oracle = "oracle".equals(ConfigurationManager.getProperty("db.name"));

        if (row != null)
        {
            fieldId = row.getIntColumn("metadata_field_id");
            valueId = row.getIntColumn("metadata_value_id");
            resourceId = row.getIntColumn("resource_id");
            resourceTypeId = row.getIntColumn("resource_type_id");
            value = row.getStringColumn("text_value");
            language = row.getStringColumn("text_lang");
            place = row.getIntColumn("place");
            this.row = row;
        }
    }

    /**
     * Default constructor.
     */
    public ResourceMetadataValue()
    {
        oracle = "oracle".equals(ConfigurationManager.getProperty("db.name"));
    }

    /**
     * Constructor to create a value for a given field.
     *
     * @param field inital value for field
     */
    public ResourceMetadataValue(MetadataField field)
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
     * Get the resource ID.
     *
     * @return resource ID
     */
    public int getResourceId()
    {
        return resourceId;
    }

    /**
     * Set the resource ID.
     *
     * @param resourceId new resource ID
     */
    public void setResourceId(int resourceId)
    {
        this.resourceId = resourceId;
    }

    /**
     * Get the resource type ID.
     *
     * @return resource type ID
     */
    public int getResourceTypeId()
    {
        return resourceId;
    }

    /**
     * Set the resource type ID.
     *
     * @param resourceTypeId new resource type ID
     */
    public void setResourceTypeId(int resourceTypeId)
    {
        this.resourceTypeId = resourceTypeId;
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
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     */
    public void create(Context context) throws SQLException, AuthorizeException {
        // Create a table row and update it with the values
        row = DatabaseManager.row((oracle ? "RMetadataValue" : "ResourceMetadataValue"));
        row.setColumn("resource_id", resourceId);
        row.setColumn("resource_type_id", resourceTypeId);
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
     * @throws java.io.IOException
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     */
    public static ResourceMetadataValue find(Context context, int valueId)
            throws IOException, SQLException, AuthorizeException
    {
        boolean oracle = "oracle".equals(ConfigurationManager.getProperty("db.name"));

        // Grab rows from DB
        TableRowIterator tri = DatabaseManager.queryTable(context, (oracle ? "RMetadataValue" : "ResourceMetadataValue"),
                "SELECT * FROM " + (oracle ? "RMetadataValue" : "ResourceMetadataValue") + " where metadata_value_id= ? ",
                valueId);

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
            return new ResourceMetadataValue(row);
        }
    }

    /**
     * Retrieves the metadata values for a given field from the database.
     *
     * @param context dspace context
     * @param fieldId field whose values to look for
     * @return a collection of metadata values
     * @throws java.io.IOException
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     */
    public static java.util.Collection findByField(Context context, int fieldId)
            throws IOException, SQLException, AuthorizeException
    {
        boolean oracle = "oracle".equals(ConfigurationManager.getProperty("db.name"));

        // Grab rows from DB
        TableRowIterator tri = DatabaseManager.queryTable(context, (oracle ? "RMetadataValue" : "ResourceMetadataValue"),
                "SELECT * FROM " + (oracle ? "RMetadataValue" : "ResourceMetadataValue") + " WHERE metadata_field_id= ? ",
                fieldId);

        TableRow row = null;
        java.util.Collection ret = new ArrayList();
        while (tri.hasNext())
        {
            row = tri.next();
            ret.add(new ResourceMetadataValue(row));
        }

        // close the TableRowIterator to free up resources
        tri.close();

        return ret;
    }

    /**
     * Update the metadata value in the database.
     *
     * @param context dspace context
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     */
    public void update(Context context) throws SQLException, AuthorizeException
    {
        row.setColumn("resource_id", resourceId);
        row.setColumn("resource_type_id", resourceTypeId);
        row.setColumn("metadata_field_id", fieldId);
        row.setColumn("text_value", value);
        row.setColumn("text_lang", language);
        row.setColumn("place", place);
        DatabaseManager.update(context, row);

        log.info(LogManager.getHeader(context, "update_ResourceMetadataValue",
                "metadata_value_id=" + getValueId()));
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
        log.info(LogManager.getHeader(context, "delete_ResourceMetadataValue",
                " metadata_value_id=" + getValueId()));
        DatabaseManager.delete(context, row);
    }
}
