/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;

/**
 * Service interface class for the DSpaceObject.
 * All DSpaceObject service classes should implement this class since it offers some basic methods which all
 * DSpaceObjects
 * are required to have.
 *
 * @param <T> class type
 * @author kevinvandevelde at atmire.com
 */
public interface DSpaceObjectService<T extends DSpaceObject> {

    /**
     * Generic find for when the precise type of an Entity is not known
     *
     * @param context - the context
     * @param uuid      - uuid within table of type'd dspace objects
     * @return the dspace object found, or null if it does not exist.
     * @throws SQLException only upon failure accessing the database.
     */
    public T find(Context context, UUID uuid) throws SQLException;

    /**
     * Get a proper name for the object. This may return <code>null</code>.
     * Name should be suitable for display in a user interface.
     *
     * @param dso DSpaceObject
     * @return Name for the object, or <code>null</code> if it doesn't have
     * one
     */
    public abstract String getName(T dso);

    /**
     * Tries to lookup all Identifiers of this DSpaceObject.
     *
     * @param context DSpace context
     * @param dso     DSpaceObject
     * @return An array containing all found identifiers or an array with a length of 0.
     */
    public ArrayList<String> getIdentifiers(Context context, T dso);

    /**
     * Return the dspace object that "own" the current object in the hierarchy.
     * Note that this method has a meaning slightly different from the
     * getAdminObject because it is independent of the action but it is in a way
     * related to it. It defines the "first" dspace object <b>OTHER</b> then the
     * current one, where allowed ADMIN actions imply allowed ADMIN actions on
     * the object self.
     *
     * @param context DSpace context
     * @param dso     DSpaceObject
     * @return the dspace object that "own" the current object in
     * the hierarchy
     * @throws SQLException if database error
     */
    public DSpaceObject getParentObject(Context context, T dso) throws SQLException;

    /**
     * Return the dspace object where an ADMIN action right is sufficient to
     * grant the initial authorize check.
     * <p>
     * Default behaviour is ADMIN right on the object grant right on all other
     * action on the object itself. Subclass should override this method as
     * needed.
     *
     * @param context DSpace context
     * @param dso     DSpaceObject
     * @param action  ID of action being attempted, from
     *                <code>org.dspace.core.Constants</code>. The ADMIN action is
     *                not a valid parameter for this method, an
     *                IllegalArgumentException should be thrown
     * @return the dspace object, if any, where an ADMIN action is sufficient to
     * grant the original action
     * @throws SQLException             if database error
     * @throws IllegalArgumentException if the ADMIN action is supplied as parameter of the method
     *                                  call
     */
    public DSpaceObject getAdminObject(Context context, T dso, int action) throws SQLException;

    /**
     * Provide the text name of the type of this DSpaceObject. It is most likely all uppercase.
     *
     * @param dso DSpaceObject
     * @return Object type as text
     */
    public String getTypeText(T dso);


    /**
     * Get metadata for the DSpace Object in a chosen schema.
     * See <code>MetadataSchema</code> for more information about schemas.
     * Passing in a <code>null</code> value for <code>qualifier</code>
     * or <code>lang</code> only matches metadata fields where that
     * qualifier or languages is actually <code>null</code>.
     * Passing in <code>DSpaceObject.ANY</code>
     * retrieves all metadata fields with any value for the qualifier or
     * language, including <code>null</code>
     * <P>
     * Examples:
     * <P>
     * Return values of the unqualified "title" field, in any language.
     * Qualified title fields (e.g. "title.uniform") are NOT returned:
     * <P>
     * <code>dspaceobject.getMetadataByMetadataString("dc", "title", null, DSpaceObject.ANY );</code>
     * <P>
     * Return all US English values of the "title" element, with any qualifier
     * (including unqualified):
     * <P>
     * <code>dspaceobject.getMetadataByMetadataString("dc, "title", DSpaceObject.ANY, "en_US" );</code>
     * <P>
     * The ordering of values of a particular element/qualifier/language
     * combination is significant. When retrieving with wildcards, values of a
     * particular element/qualifier/language combinations will be adjacent, but
     * the overall ordering of the combinations is indeterminate.
     *
     * @param dSpaceObject DSpaceObject
     * @param schema       the schema for the metadata field. <em>Must</em> match
     *                     the <code>name</code> of an existing metadata schema.
     * @param element      the element name. <code>DSpaceObject.ANY</code> matches any
     *                     element. <code>null</code> doesn't really make sense as all
     *                     metadata must have an element.
     * @param qualifier    the qualifier. <code>null</code> means unqualified, and
     *                     <code>DSpaceObject.ANY</code> means any qualifier (including
     *                     unqualified.)
     * @param lang         the ISO639 language code, optionally followed by an underscore
     *                     and the ISO3166 country code. <code>null</code> means only
     *                     values with no language are returned, and
     *                     <code>DSpaceObject.ANY</code> means values with any country code or
     *                     no country code are returned.
     * @return metadata fields that match the parameters
     */
    public List<MetadataValue> getMetadata(T dSpaceObject, String schema, String element, String qualifier,
                                           String lang);

    /**
     * Retrieve metadata field values from a given metadata string
     * of the form {@code <schema prefix>.<element>[.<qualifier>|.*]}
     *
     * @param dSpaceObject DSpaceObject
     * @param mdString     The metadata string of the form
     *                     {@code <schema prefix>.<element>[.<qualifier>|.*]}
     * @return metadata fields that match the parameters
     */
    public List<MetadataValue> getMetadataByMetadataString(T dSpaceObject, String mdString);


    /**
     * Get the value of a metadata field
     *
     * @param dSpaceObject DSpaceObject
     * @param value        the name of the metadata field to get
     * @return the value of the metadata field (or null if the column is an SQL NULL)
     * @throws IllegalArgumentException if the requested metadata field doesn't exist
     */
    public String getMetadata(T dSpaceObject, String value);


    public List<MetadataValue> getMetadata(T dSpaceObject, String mdString, String authority);

    public List<MetadataValue> getMetadata(T dSpaceObject, String schema, String element, String qualifier, String lang,
                                           String authority);

    /**
     * Add metadata fields. These are appended to existing values.
     * Use <code>clearMetadata</code> to remove values. The ordering of values
     * passed in is maintained.
     * <p>
     * If metadata authority control is available, try to get authority
     * values.  The authority confidence depends on whether authority is
     * <em>required</em> or not.
     *
     * @param context   DSpace context
     * @param dso       DSpaceObject
     * @param schema    the schema for the metadata field. <em>Must</em> match
     *                  the <code>name</code> of an existing metadata schema.
     * @param element   the metadata element name
     * @param qualifier the metadata qualifier name, or <code>null</code> for
     *                  unqualified
     * @param lang      the ISO639 language code, optionally followed by an underscore
     *                  and the ISO3166 country code. <code>null</code> means the
     *                  value has no language (for example, a date).
     * @param values    the values to add.
     * @throws SQLException if database error
     */
    public void addMetadata(Context context, T dso, String schema, String element, String qualifier, String lang,
                            List<String> values) throws SQLException;

    /**
     * Add metadata fields. These are appended to existing values.
     * Use <code>clearMetadata</code> to remove values. The ordering of values
     * passed in is maintained.
     *
     * @param context     DSpace context
     * @param dso         DSpaceObject
     * @param schema      the schema for the metadata field. <em>Must</em> match
     *                    the <code>name</code> of an existing metadata schema.
     * @param element     the metadata element name
     * @param qualifier   the metadata qualifier name, or <code>null</code> for
     *                    unqualified
     * @param lang        the ISO639 language code, optionally followed by an underscore
     *                    and the ISO3166 country code. <code>null</code> means the
     *                    value has no language (for example, a date).
     * @param values      the values to add.
     * @param authorities the external authority key for this value (or null)
     * @param confidences the authority confidence (default 0)
     * @throws SQLException if database error
     */
    public void addMetadata(Context context, T dso, String schema, String element, String qualifier, String lang,
                            List<String> values, List<String> authorities, List<Integer> confidences)
        throws SQLException;

    /**
     * Add metadata fields. These are appended to existing values.
     * Use <code>clearMetadata</code> to remove values. The ordering of values
     * passed in is maintained.
     *
     * @param context       DSpace context
     * @param dso           DSpaceObject
     * @param metadataField the metadata field to which the value is to be set
     * @param lang          the ISO639 language code, optionally followed by an underscore
     *                      and the ISO3166 country code. <code>null</code> means the
     *                      value has no language (for example, a date).
     * @param values        the values to add.
     * @param authorities   the external authority key for this value (or null)
     * @param confidences   the authority confidence (default 0)
     * @throws SQLException if database error
     */
    public void addMetadata(Context context, T dso, MetadataField metadataField, String lang, List<String> values,
            List<String> authorities, List<Integer> confidences) throws SQLException;

    /**
     * Shortcut for {@link #addMetadata(Context, DSpaceObject, MetadataField, String, List, List, List)} when a single
     * value need to be added
     * 
     * @param context
     * @param dso
     * @param metadataField
     * @param language
     * @param value
     * @param authority
     * @param confidence
     * @throws SQLException
     */
    public void addMetadata(Context context, T dso, MetadataField metadataField, String language, String value,
            String authority, int confidence) throws SQLException;

    public void addMetadata(Context context, T dso, MetadataField metadataField, String language, String value)
        throws SQLException;

    public void addMetadata(Context context, T dso, MetadataField metadataField, String language, List<String> values)
        throws SQLException;

    /**
     * Add a single metadata field. This is appended to existing
     * values. Use <code>clearMetadata</code> to remove values.
     *
     * @param context   DSpace context
     * @param dso       DSpaceObject
     * @param schema    the schema for the metadata field. <em>Must</em> match
     *                  the <code>name</code> of an existing metadata schema.
     * @param element   the metadata element name
     * @param qualifier the metadata qualifier, or <code>null</code> for
     *                  unqualified
     * @param lang      the ISO639 language code, optionally followed by an underscore
     *                  and the ISO3166 country code. <code>null</code> means the
     *                  value has no language (for example, a date).
     * @param value     the value to add.
     * @throws SQLException if database error
     */
    public void addMetadata(Context context, T dso, String schema, String element, String qualifier, String lang,
                            String value) throws SQLException;

    /**
     * Add a single metadata field. This is appended to existing
     * values. Use <code>clearMetadata</code> to remove values.
     *
     * @param context    DSpace context
     * @param dso        DSpaceObject
     * @param schema     the schema for the metadata field. <em>Must</em> match
     *                   the <code>name</code> of an existing metadata schema.
     * @param element    the metadata element name
     * @param qualifier  the metadata qualifier, or <code>null</code> for
     *                   unqualified
     * @param lang       the ISO639 language code, optionally followed by an underscore
     *                   and the ISO3166 country code. <code>null</code> means the
     *                   value has no language (for example, a date).
     * @param value      the value to add.
     * @param authority  the external authority key for this value (or null)
     * @param confidence the authority confidence (default 0)
     * @throws SQLException if database error
     */
    public void addMetadata(Context context, T dso, String schema, String element, String qualifier, String lang,
                            String value, String authority, int confidence) throws SQLException;

    /**
     * Clear metadata values. As with <code>getDC</code> above,
     * passing in <code>null</code> only matches fields where the qualifier orr
     * language is actually <code>null</code>.<code>Item.ANY</code> will
     * match any element, qualifier or language, including <code>null</code>.
     * Thus, <code>dspaceobject.clearMetadata(Item.ANY, Item.ANY, Item.ANY)</code> will
     * remove all Dublin Core metadata associated with an DSpaceObject.
     *
     * @param context   DSpace context
     * @param dso       DSpaceObject
     * @param schema    the schema for the metadata field. <em>Must</em> match
     *                  the <code>name</code> of an existing metadata schema.
     * @param element   the Dublin Core element to remove, or <code>Item.ANY</code>
     * @param qualifier the qualifier. <code>null</code> means unqualified, and
     *                  <code>Item.ANY</code> means any qualifier (including
     *                  unqualified.)
     * @param lang      the ISO639 language code, optionally followed by an underscore
     *                  and the ISO3166 country code. <code>null</code> means only
     *                  values with no language are removed, and <code>Item.ANY</code>
     *                  means values with any country code or no country code are
     *                  removed.
     * @throws SQLException if database error
     */
    public void clearMetadata(Context context, T dso, String schema, String element, String qualifier, String lang)
        throws SQLException;

    public void removeMetadataValues(Context context, T dso, List<MetadataValue> values) throws SQLException;

    public String getMetadataFirstValue(T dso, String schema, String element, String qualifier, String language);

    /**
     * Set first metadata field value
     *
     * @param context   DSpace context
     * @param dso       DSpaceObject
     * @param schema    the schema for the metadata field. <em>Must</em> match
     *                  the <code>name</code> of an existing metadata schema.
     * @param element   the Dublin Core element to remove, or <code>Item.ANY</code>
     * @param qualifier the qualifier. <code>null</code> means unqualified, and
     *                  <code>Item.ANY</code> means any qualifier (including
     *                  unqualified.)
     * @param language  the ISO639 language code, optionally followed by an underscore
     *                  and the ISO3166 country code. <code>null</code> means only
     *                  values with no language are removed, and <code>Item.ANY</code>
     *                  means values with any country code or no country code are
     *                  removed.
     * @param value     metadata value
     * @throws SQLException if database error
     */
    public void setMetadataSingleValue(Context context, T dso, String schema, String element, String qualifier,
                                       String language, String value) throws SQLException;

    public void updateLastModified(Context context, T dso) throws SQLException, AuthorizeException;

    public void update(Context context, T dso) throws SQLException, AuthorizeException;

    public void delete(Context context, T dso) throws SQLException, AuthorizeException, IOException;

    /**
     * Add a single metadata field. Whether it's appended or prepended depends on index parameter.
     * Use <code>clearMetadata</code> to remove values.
     *
     * @param context    DSpace context
     * @param dso        DSpaceObject
     * @param schema     the schema for the metadata field. <em>Must</em> match
     *                   the <code>name</code> of an existing metadata schema.
     * @param element    the metadata element name
     * @param qualifier  the metadata qualifier, or <code>null</code> for
     *                   unqualified
     * @param lang       the ISO639 language code, optionally followed by an underscore
     *                   and the ISO3166 country code. <code>null</code> means the
     *                   value has no language (for example, a date).
     * @param value      the value to add.
     * @param authority  the external authority key for this value (or null)
     * @param confidence the authority confidence (default 0)
     * @param index      the index at which this metadata is added (0: first place, -1 for last)
     * @throws SQLException if database error
     */
    void addAndShiftRightMetadata(Context context, T dso, String schema, String element, String qualifier, String lang,
                                  String value, String authority, int confidence, int index) throws SQLException;

    void replaceMetadata(Context context, T dso, String schema, String element, String qualifier, String lang,
                         String value, String authority, int confidence, int index) throws SQLException;

    void moveMetadata(Context context, T dso, String schema, String element, String qualifier, int from, int to)
        throws SQLException;

    /**
     * Returns the Constants which this service supports
     *
     * @return a org.dspace.core.Constants that represents a IndexableObject type
     */
    public int getSupportsTypeConstant();

    /**
     * Trigger the modifiedMetadata variable in DSpaceObject
     * @param dso   DSpaceObject whose metadata has been modified
     */
    public void setMetadataModified(T dso);
}
