/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;
import java.util.*;

import org.apache.log4j.Logger;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.authorize.AuthorizeException;
import org.dspace.event.Event;

/**
 * Abstract base class for DSpace objects
 */
public abstract class DSpaceObject
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(DSpaceObject.class);

    // accumulate information to add to "detail" element of content Event,
    // e.g. to document metadata fields touched, etc.
    private StringBuffer eventDetails = null;

    /** The extra metadata - a list of MetadataValue objects. */
    private List<DCValue> extraMetadata;

    private boolean oracle;

    /**
     * True if the extra metadata has changed since reading from the DB or the last
     * update()
     */
    private boolean extraMetadataChanged;

    private Context context;

    public DSpaceObject(Context context) {
        this.context = context;
        oracle = "oracle".equals(ConfigurationManager.getProperty("db.name"));
        extraMetadataChanged = false;
        extraMetadata = new ArrayList<DCValue>();
    }


    /**
     * Reset the cache of event details.
     */
    protected void clearDetails()
    {
        eventDetails = null;
    }

    /**
     * Add a string to the cache of event details.  Automatically
     * separates entries with a comma.
     * Subclass can just start calling addDetails, since it creates
     * the cache if it needs to.
     * @param d detail string to add.
     */
    protected void addDetails(String d)
    {
        if (eventDetails == null)
        {
            eventDetails = new StringBuffer(d);
        }
        else
        {
            eventDetails.append(", ").append(d);
        }
    }

    /**
     * @return summary of event details, or null if there are none.
     */
    protected String getDetails()
    {
        return (eventDetails == null ? null : eventDetails.toString());
    }

    /**
     * Get the type of this object, found in Constants
     *
     * @return type of the object
     */
    public abstract int getType();

    /**
     * Get the internal ID (database primary key) of this object
     *
     * @return internal ID of object
     */
    public abstract int getID();

    /**
     * Get the Handle of the object. This may return <code>null</code>
     *
     * @return Handle of the object, or <code>null</code> if it doesn't have
     *         one
     */
    public abstract String getHandle();

    /**
     * Get a proper name for the object. This may return <code>null</code>.
     * Name should be suitable for display in a user interface.
     *
     * @return Name for the object, or <code>null</code> if it doesn't have
     *         one
     */
    public abstract String getName();

    /**
     * Generic find for when the precise type of a DSO is not known, just the
     * a pair of type number and database ID.
     *
     * @param context - the context
     * @param type - type number
     * @param id - id within table of type'd objects
     * @return the object found, or null if it does not exist.
     * @throws SQLException only upon failure accessing the database.
     */
    public static DSpaceObject find(Context context, int type, int id)
        throws SQLException
    {
        switch (type)
        {
            case Constants.BITSTREAM : return Bitstream.find(context, id);
            case Constants.BUNDLE    : return Bundle.find(context, id);
            case Constants.ITEM      : return Item.find(context, id);
            case Constants.COLLECTION: return Collection.find(context, id);
            case Constants.COMMUNITY : return Community.find(context, id);
            case Constants.GROUP     : return Group.find(context, id);
            case Constants.EPERSON   : return EPerson.find(context, id);
            case Constants.SITE      : return Site.find(context, id);
        }
        return null;
    }

    /** Our context */
    protected Context getContext(){
        return context;
    }

    /**
     *
     * @param context
     * @throws SQLException
     */
    protected void readExtraMetadata(Context context) throws SQLException {
        // Get Dublin Core metadata
        TableRowIterator tri = DatabaseManager.queryTable(context, (oracle ? "RMetadataValue" : "ResourceMetadataValue"),
                "SELECT * FROM " + (oracle ? "RMetadataValue" : "ResourceMetadataValue") + " WHERE resource_id= ?" +
                        " AND resource_type_id= ?" +
                        " ORDER BY metadata_field_id, place",
                getID(), getType());

        while (tri.hasNext())
        {
            TableRow resultRow = tri.next();

            // Get the associated metadata field and schema information
            int fieldID = resultRow.getIntColumn("metadata_field_id");
            ResourceMetadataField field = ResourceMetadataField.find(context, fieldID);

            if (field == null)
            {
                log.error("Loading item - cannot found metadata field "
                        + fieldID);
            }
            else
            {
//                MetadataSchema schema = MetadataSchema.find(
//                        context, field.getSchemaID());

                // Make a DCValue object
                DCValue dcv = new DCValue();
                dcv.element = field.getElement();
                dcv.qualifier = field.getQualifier();
                dcv.value = resultRow.getStringColumn("text_value");
                dcv.language = resultRow.getStringColumn("text_lang");
                //dcv.namespace = schema.getNamespace();
                dcv.schema = ""+field.getResourceType();

                // Add it to the list
                extraMetadata.add(dcv);
            }
        }
        // close the TableRowIterator to free up resources
        tri.close();
    }

    /**
     *
     * @param element
     * @param qualifier
     * @param lang
     * @return
     */
    public DCValue[] getExtraMetadata(String element, String qualifier,
                                      String lang) {
        return getExtraMetadata(getType(), element, qualifier, lang);
    }

    /**
     *
     * @param element
     * @param qualifier
     * @param lang
     * @return
     */
    public String getExtraMetadataFirstValue(String element, String qualifier, String lang) {
        try {
            return getExtraMetadata(element, qualifier, lang)[0].value;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get metadata for the item in a chosen schema.
     * See <code>MetadataSchema</code> for more information about schemas.
     * Passing in a <code>null</code> value for <code>qualifier</code>
     * or <code>lang</code> only matches metadata fields where that
     * qualifier or languages is actually <code>null</code>.
     * Passing in <code>Item.ANY</code>
     * retrieves all metadata fields with any value for the qualifier or
     * language, including <code>null</code>
     * <P>
     * Examples:
     * <P>
     * Return values of the unqualified "title" field, in any language.
     * Qualified title fields (e.g. "title.uniform") are NOT returned:
     * <P>
     * <code>item.getMetadata("dc", "title", null, Item.ANY );</code>
     * <P>
     * Return all US English values of the "title" element, with any qualifier
     * (including unqualified):
     * <P>
     * <code>item.getMetadata("dc, "title", Item.ANY, "en_US" );</code>
     * <P>
     * The ordering of values of a particular element/qualifier/language
     * combination is significant. When retrieving with wildcards, values of a
     * particular element/qualifier/language combinations will be adjacent, but
     * the overall ordering of the combinations is indeterminate.
     *
     * @param element
     *            the element name. <code>Item.ANY</code> matches any
     *            element. <code>null</code> doesn't really make sense as all
     *            metadata must have an element.
     * @param qualifier
     *            the qualifier. <code>null</code> means unqualified, and
     *            <code>Item.ANY</code> means any qualifier (including
     *            unqualified.)
     * @param lang
     *            the ISO639 language code, optionally followed by an underscore
     *            and the ISO3166 country code. <code>null</code> means only
     *            values with no language are returned, and
     *            <code>Item.ANY</code> means values with any country code or
     *            no country code are returned.
     * @return metadata fields that match the parameters
     */
    public DCValue[] getExtraMetadata(int resourceType, String element, String qualifier,
                                      String lang)
    {
        // Build up list of matching values
        List values = new ArrayList();
        Iterator i = extraMetadata.iterator();

        while (i.hasNext())
        {
            DCValue dcv = (DCValue) i.next();

            if (match(resourceType, element, qualifier, lang, dcv))
            {
                // We will return a copy of the object in case it is altered
                DCValue copy = new DCValue();
                copy.element = dcv.element;
                copy.qualifier = dcv.qualifier;
                copy.value = dcv.value;
                copy.language = dcv.language;
                copy.schema = dcv.schema;

                values.add(copy);
            }
        }

        // Create an array of matching values
        DCValue[] valueArray = new DCValue[values.size()];
        valueArray = (DCValue[]) values.toArray(valueArray);

        return valueArray;
    }

    /**
     *
     * @param element
     * @param qualifier
     * @param lang
     * @param value
     */
    public void addExtraMetadata(String element, String qualifier,
                                 String lang, String value) {
        addExtraMetadata(getType(), element, qualifier, lang, value);
    }

    /**
     * Add a single extra metadata field. This is appended to existing
     * values. Use <code>clearDC</code> to remove values.
     *
     * @param element
     *            the metadata element name
     * @param qualifier
     *            the metadata qualifer, or <code>null</code> for
     *            unqualified
     * @param lang
     *            the ISO639 language code, optionally followed by an underscore
     *            and the ISO3166 country code. <code>null</code> means the
     *            value has no language (for example, a date).
     * @param value
     *            the value to add.
     */
    public void addExtraMetadata(int resourceType, String element, String qualifier,
                                 String lang, String value)
    {
        DCValue dcv = new DCValue();
        dcv.schema = "" + resourceType;
        dcv.element = element;
        dcv.qualifier = qualifier;
        dcv.language = lang;
        dcv.value = (value == null ? null : value.trim());
        extraMetadata.add(dcv);
        extraMetadataChanged = true;
    }

    /**
     *
     * @param element
     * @param qualifier
     * @param lang
     */
    public void clearExtraMetadata(String element, String qualifier,
                                   String lang) {
        clearExtraMetadata(getType(), element, qualifier, lang);
    }

    /**
     * Clear metadata values. As with <code>getDC</code> above,
     * passing in <code>null</code> only matches fields where the qualifier or
     * language is actually <code>null</code>.<code>Item.ANY</code> will
     * match any element, qualifier or language, including <code>null</code>.
     * Thus, <code>item.clearDC(Item.ANY, Item.ANY, Item.ANY)</code> will
     * remove all Dublin Core metadata associated with an item.
     *
     * @param element
     *            the Dublin Core element to remove, or <code>Item.ANY</code>
     * @param qualifier
     *            the qualifier. <code>null</code> means unqualified, and
     *            <code>Item.ANY</code> means any qualifier (including
     *            unqualified.)
     * @param lang
     *            the ISO639 language code, optionally followed by an underscore
     *            and the ISO3166 country code. <code>null</code> means only
     *            values with no language are removed, and <code>Item.ANY</code>
     *            means values with any country code or no country code are
     *            removed.
     */
    public void clearExtraMetadata(int resourceType, String element, String qualifier,
                                   String lang)
    {
        // We will build a list of values NOT matching the values to clear
        List values = new ArrayList();
        Iterator i = extraMetadata.iterator();

        while (i.hasNext())
        {
            DCValue dcv = (DCValue) i.next();

            if (!match(resourceType, element, qualifier, lang, dcv))
            {
                values.add(dcv);
            }
        }

        // Now swap the old list of values for the new, unremoved values
        extraMetadata = values;
        extraMetadataChanged = true;
    }

    /**
     * Utility method to remove all descriptive metadata associated with the item from
     * the database (regardless of in-memory version)
     *
     * @throws java.sql.SQLException
     */
    private void removeExtraMetadataFromDatabase() throws SQLException {
        DatabaseManager.updateQuery(getContext(),
                "DELETE FROM " + (oracle ? "RMetadataValue" : "ResourceMetadataValue") + " WHERE resource_id= ? AND resource_type_id = ? ",
                getID(), getType());
    }

    protected boolean match(String element, String qualifier,
                            String language, DCValue dcv) {
        return match(getType(), element, qualifier, language, dcv);
    }

    /**
     * Utility method for pattern-matching metadata elements.  This
     * method will return <code>true</code> if the given schema,
     * element, qualifier and language match the schema, element,
     * qualifier and language of the <code>DCValue</code> object passed
     * in.  Any or all of the elemenent, qualifier and language passed
     * in can be the <code>Item.ANY</code> wildcard.
     *
     * @param element
     *            the element to match, or <code>Item.ANY</code>
     * @param qualifier
     *            the qualifier to match, or <code>Item.ANY</code>
     * @param language
     *            the language to match, or <code>Item.ANY</code>
     * @param dcv
     *            the Dublin Core value
     * @return <code>true</code> if there is a match
     */
    protected boolean match(int resourceType, String element, String qualifier,
                            String language, DCValue dcv)
    {
        // We will attempt to disprove a match - if we can't we have a match
        if (!element.equals(Item.ANY) && !element.equals(dcv.element))
        {
            // Elements do not match, no wildcard
            return false;
        }

        if (qualifier == null)
        {
            // Value must be unqualified
            if (dcv.qualifier != null)
            {
                // Value is qualified, so no match
                return false;
            }
        }
        else if (!qualifier.equals(Item.ANY))
        {
            // Not a wildcard, so qualifier must match exactly
            if (!qualifier.equals(dcv.qualifier))
            {
                return false;
            }
        }

        if (language == null)
        {
            // Value must be null language to match
            if (dcv.language != null)
            {
                // Value is qualified, so no match
                return false;
            }
        }
        else if (!language.equals(Item.ANY))
        {
            // Not a wildcard, so language must match exactly
            if (!language.equals(dcv.language))
            {
                return false;
            }
        }
        else if (resourceType >= 0)
        {
            if (dcv.schema != null && !dcv.schema.equals("" + resourceType))
            {
                // The namespace doesn't match
                return false;
            }
        }

        // If we get this far, we have a match
        return true;
    }

    /**
     *
     * @throws SQLException
     * @throws AuthorizeException
     */
    protected void updateExtraMetadata() throws SQLException, AuthorizeException {
        // Redo Dublin Core if it's changed
        if (extraMetadataChanged)
        {
            Map elementCount = new HashMap();

            // Remove existing DC
            removeExtraMetadataFromDatabase();

            // Add in-memory DC
            Iterator i = extraMetadata.iterator();

            while (i.hasNext())
            {
                DCValue dcv = (DCValue) i.next();

                // Get the DC Type

                ResourceMetadataField field = ResourceMetadataField.findByElement(getContext(),
                        getType(), dcv.element, dcv.qualifier);

                if (field == null)
                {
                    // Bad DC field, log and throw exception
                    log.warn(LogManager
                            .getHeader(getContext(), "bad_metadata",
                                    "Bad metadata field. resourceType=" + String.valueOf(getType())
                                            + ", element: \""
                                            + ((dcv.element == null) ? "null"
                                            : dcv.element)
                                            + "\" qualifier: \""
                                            + ((dcv.qualifier == null) ? "null"
                                            : dcv.qualifier)
                                            + "\" value: \""
                                            + ((dcv.value == null) ? "null"
                                            : dcv.value) + "\""));

                    throw new SQLException("bad_metadata "
                            + "resourceType="+String.valueOf(getType())+", "
                            + dcv.element
                            + " " + dcv.qualifier);
                }

                // Work out the place number for ordering
                int current = 0;

                // Key into map is "element" or "element.qualifier"
                String key = dcv.element
                        + ((dcv.qualifier == null) ? "" : ("." + dcv.qualifier));

                Integer currentInteger = (Integer) elementCount.get(key);

                if (currentInteger != null)
                {
                    current = currentInteger.intValue();
                }

                current++;
                elementCount.put(key, new Integer(current));

                // Write DCValue
                ResourceMetadataValue metadata = new ResourceMetadataValue();
                metadata.setResourceId(getID());
                metadata.setResourceTypeId(getType());
                metadata.setFieldId(field.getFieldID());
                metadata.setValue(dcv.value);
                metadata.setLanguage(dcv.language);
                metadata.setPlace(current);
                metadata.create(getContext());
            }

            extraMetadataChanged = false;
            getContext().addEvent(new Event(Event.MODIFY_METADATA, getType(), getID(), getDetails()));
        }
    }



    /**
     * Return the dspace object where an ADMIN action right is sufficient to
     * grant the initial authorize check.
     * <p>
     * Default behaviour is ADMIN right on the object grant right on all other
     * action on the object itself. Subclass should override this method as
     * need.
     *
     * @param action
     *            ID of action being attempted, from
     *            <code>org.dspace.core.Constants</code>. The ADMIN action is
     *            not a valid parameter for this method, an
     *            IllegalArgumentException should be thrown
     * @return the dspace object, if any, where an ADMIN action is sufficient to
     *         grant the original action
     * @throws SQLException
     * @throws IllegalArgumentException
     *             if the ADMIN action is supplied as parameter of the method
     *             call
     */
    public DSpaceObject getAdminObject(int action) throws SQLException
    {
        if (action == Constants.ADMIN)
        {
            throw new IllegalArgumentException("Illegal call to the DSpaceObject.getAdminObject method");
        }
        return this;
    }

    /**
     * Return the dspace object that "own" the current object in the hierarchy.
     * Note that this method has a meaning slightly different from the
     * getAdminObject because it is independent of the action but it is in a way
     * related to it. It defines the "first" dspace object <b>OTHER</b> then the
     * current one, where allowed ADMIN actions imply allowed ADMIN actions on
     * the object self.
     *
     * @return the dspace object that "own" the current object in
     *         the hierarchy
     * @throws SQLException
     */
    public DSpaceObject getParentObject() throws SQLException
    {
        return null;
    }
}
