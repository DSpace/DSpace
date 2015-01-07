/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

import java.sql.SQLException;

/**
 * MetadataValues for Scheme, Concepts and Terms.
 *
 * @author Mark Diggory (markd at atmire dot com)
 * @author Lantian Gai (lantian at atmire dot com)
 */
public class AuthorityMetadataValue {

    /** The reference to the metadata field */
    public int fieldId = 0;

    /** The primary key for the metadata value */
    public int valueId = 0;

    /** The primary key for the authority value */
    public int parentId = 0;
    /** The value of the field */
    public String value;

    /** The language of the field, may be <code>null</code> */
    public String language;

    /** The position of the record. */
    public int place = 1;

    /** Authority key, if any */
    public String authority = null;

    /** Authority confidence value -- see Choices class for values */
    public int confidence = 0;

    /** The row in the table representing this type */
    public TableRow row;
    public String qualifier = "";
    public String element = "";
    public String schema = "";

    private String tableName = "";


    public static String generaltype = "Internal";

    /** log4j logger */
    private static Logger log = Logger.getLogger(AuthorityMetadataValue.class);

    public AuthorityMetadataValue(String tableName){
        this.tableName = tableName;
    };

    /**
     *
     *
     * Construct the metadata object from the matching database row.
     *
     * @param row database row to use for contents
     */

    public AuthorityMetadataValue(TableRow row)
    {
        if (row != null)
        {
            tableName = row.getTable();
            fieldId = row.getIntColumn("field_id");
            valueId = row.getIntColumn("id");
            parentId = row.getIntColumn("parent_id");
            value = row.getStringColumn("text_value");
            language = row.getStringColumn("text_lang");
            place = row.getIntColumn("place");
            authority = row.getStringColumn("authority");
            confidence = row.getIntColumn("confidence");
            this.row = row;
        }
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
     * Get the metadata authority
     *
     * @return metadata authority
     */
    public String getAuthority ()
    {
        return authority ;
    }

    /**
     * Set the metadata authority
     *
     * @param value new metadata authority
     */
    public void setAuthority (String value)
    {
        this.authority  = value;
    }

    /**
     * Get the metadata confidence
     *
     * @return metadata confidence
     */
    public int getConfidence()
    {
        return confidence;
    }

    /**
     * Set the metadata confidence
     *
     * @param value new metadata confidence
     */
    public void setConfidence(int value)
    {
        this.confidence = value;
    }


    /**
     * Get the parent ID.
     *
     * @return parent ID
     */
    public int getParentId()
    {
        return parentId;
    }

    /**
     * Set the item ID.
     *
     * @param parentId new authority ID
     */
    public void setParentId(int parentId)
    {
        this.parentId = parentId;
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
        log.info(LogManager.getHeader(context, "delete_metadata_value",
                " id=" + getValueId()));
        DatabaseManager.delete(context, row);
    }

    /**
     * Creates a new metadata value.
     *
     * @param context
     *            DSpace context object
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     */
    public void create(Context context) throws SQLException, AuthorizeException
    {
        // Create a table row and update it with the values
        row = DatabaseManager.row(tableName);
        row.setColumn("parent_id", parentId);
        row.setColumn("field_id", fieldId);
        row.setColumn("text_value", value);
        row.setColumn("text_lang", language);
        row.setColumn("place", place);
        row.setColumn("authority", authority);
        row.setColumn("confidence", confidence);
        DatabaseManager.insert(context, row);

        // Remember the new row number
        this.valueId = row.getIntColumn("id");

        log.info(LogManager.getHeader(context, "create_metadata_value",
                "metadata_value_id=" + valueId));
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


    /**
     * Update the metadata value in the database.
     *
     * @param context dspace context
     * @throws java.sql.SQLException
     * @throws org.dspace.authorize.AuthorizeException
     */
    public void update(Context context) throws SQLException, AuthorizeException
    {
        row.setColumn("parent_id", parentId);
        row.setColumn("field_id", fieldId);
        row.setColumn("text_value", value);
        row.setColumn("text_lang", language);
        row.setColumn("place", place);
        row.setColumn("authority", authority);
        row.setColumn("confidence", confidence);
        DatabaseManager.update(context, row);

        log.info(LogManager.getHeader(context, "update_authoritymetadatavalue",
                "id=" + getValueId()));
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same MetadataValue
     * as this object, <code>false</code> otherwise
     *
     * @param obj
     *            object to compare to
     *
     * @return <code>true</code> if object passed in represents the same
     *         MetadataValue as this object
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
        final AuthorityMetadataValue other = (AuthorityMetadataValue) obj;
        if (this.fieldId != other.fieldId)
        {
            return false;
        }
        if (this.valueId != other.valueId)
        {
            return false;
        }
        if (this.parentId != other.parentId)
        {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 47 * hash + this.fieldId;
        hash = 47 * hash + this.valueId;
        hash = 47 * hash + this.parentId;
        return hash;
    }


    public String getTableName(){
        return tableName;
    }
}