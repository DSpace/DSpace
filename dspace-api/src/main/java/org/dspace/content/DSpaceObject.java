/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.MetadataAuthorityManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.event.Event;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * Abstract base class for DSpace objects
 */
public abstract class DSpaceObject
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(DSpaceObject.class);

    /**
     * Wild card for Dublin Core metadata qualifiers/languages
     */
    public static final String ANY = "*";

    /** Our context */
    protected final Context ourContext;

    /** The table row corresponding to this object */
    protected final TableRow ourRow;

    /** The Dublin Core metadata - inner class for lazy loading */
    private final MetadataCache dublinCore = new MetadataCache();

    /**
     * True if the metadata have changed since reading from the DB or the last
     * update()
     */
    protected boolean metadataChanged;

    protected transient MetadataField[] allMetadataFields = null;

    /**
     * True if anything else was changed since last update()
     * (to drive event mechanism)
     */
    protected boolean modified;

    private DSpaceObject()
    {
        ourContext = null;
        ourRow = null;
    }

    protected DSpaceObject(Context context, TableRow row)
    {
        ourContext = context;
        ourRow = row;
        metadataChanged = false;
    }

    /**
     * Accumulates information to add to "detail" element of content Event.
     * E.g. to document metadata fields touched, etc.
     */
    private StringBuffer eventDetails = null;

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
     * Provide the text name of the type of this DSpaceObject. It is most likely all uppercase.
     * @return Object type as text
     */
    public String getTypeText()
    {
        return Constants.typeText[this.getType()];
    }

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

    /**
     * Return the dspace object where an ADMIN action right is sufficient to
     * grant the initial authorize check.
     * <p>
     * Default behaviour is ADMIN right on the object grant right on all other
     * action on the object itself. Subclass should override this method as
     * needed.
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

    /**
     * Update the object in the database.  Does not update metadata; subclasses
     * should call {@link updateMetadata()} first.
     *
     * @throws SQLException
     * @throws AuthorizeException
     */
    public void update() throws SQLException, AuthorizeException
    {
        DatabaseManager.update(ourContext, ourRow);

        if (metadataChanged)
        {
            ourContext.addEvent(new Event(Event.MODIFY_METADATA, getType(), getID(), getDetails()));
            clearDetails();
            metadataChanged = false;
        }

        ourContext.addEvent(new Event(Event.MODIFY, getType(), getID(), null));
        modified = false;
    }

    /**
     * Update the object's metadata in the database.
     * 
     * @throws SQLException
     * @throws AuthorizeException 
     */
    void updateMetadata() throws SQLException, AuthorizeException
    {
        // Map counting number of values for each element/qualifier.
        // Keys are Strings: "element" or "element.qualifier"
        // Values are Integers indicating number of values written for a
        // element/qualifier
        Map<String,Integer> elementCount = new HashMap<String,Integer>();

        metadataChanged = false;

        // Arrays to store the working information required
        int[]     placeNum = new int[getMetadata().size()];
        boolean[] storedMD = new boolean[getMetadata().size()];
        MetadataField[] mdFields = new MetadataField[getMetadata().size()];

        // Work out the place numbers for the in memory metadata
        for (int mdIdx = 0; mdIdx < getMetadata().size(); mdIdx++)
        {
            DCValue dcv = getMetadata().get(mdIdx);

            // Work out the place number for ordering
            int current = 0;

            // Key into map is "element" or "element.qualifier"
            String key = dcv.element + ((dcv.qualifier == null) ? "" : ("." + dcv.qualifier));

            Integer currentInteger = elementCount.get(key);
            if (currentInteger != null)
            {
                current = currentInteger.intValue();
            }

            current++;
            elementCount.put(key, Integer.valueOf(current));

            // Store the calculated place number, reset the stored flag, and cache the metadatafield
            placeNum[mdIdx] = current;
            storedMD[mdIdx] = false;
            mdFields[mdIdx] = getMetadataField(dcv);
            if (mdFields[mdIdx] == null)
            {
                // Bad metadata field, log and throw exception
                log.warn(LogManager
                        .getHeader(ourContext, "bad_meta",
                                "Bad metadata field. schema="+dcv.schema
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
                        + "schema="+dcv.schema+", "
                        + dcv.element
                        + " " + dcv.qualifier);
            }
        }

        // Now the precalculations are done, iterate through the existing metadata
        // looking for matches
        TableRowIterator tri = retrieveMetadata();
        if (tri != null)
        {
            try
            {
                while (tri.hasNext())
                {
                    TableRow tr = tri.next();
                    // Assume that we will remove this row, unless we get a match
                    boolean removeRow = true;

                    // Go through the in-memory metadata, unless we've already decided to keep this row
                    for (int mdIdx = 0; mdIdx < getMetadata().size() && removeRow; mdIdx++)
                    {
                        // Only process if this metadata has not already been matched to something in the DB
                        if (!storedMD[mdIdx])
                        {
                            boolean matched = true;
                            DCValue dcv   = getMetadata().get(mdIdx);

                            // Check the metadata field is the same
                            if (matched && mdFields[mdIdx].getFieldID() != tr.getIntColumn("metadata_field_id"))
                            {
                                matched = false;
                            }

                            // Check the place is the same
                            if (matched && placeNum[mdIdx] != tr.getIntColumn("place"))
                            {
                                matched = false;
                            }

                            // Check the text is the same
                            if (matched)
                            {
                                String text = tr.getStringColumn("text_value");
                                if (dcv.value == null && text == null)
                                {
                                    matched = true;
                                }
                                else if (dcv.value != null && dcv.value.equals(text))
                                {
                                    matched = true;
                                }
                                else
                                {
                                    matched = false;
                                }
                            }

                            // Check the language is the same
                            if (matched)
                            {
                                String lang = tr.getStringColumn("text_lang");
                                if (dcv.language == null && lang == null)
                                {
                                    matched = true;
                                }
                                else if (dcv.language != null && dcv.language.equals(lang))
                                {
                                    matched = true;
                                }
                                else
                                {
                                    matched = false;
                                }
                            }

                            // check that authority and confidence match
                            if (matched)
                            {
                                String auth = tr.getStringColumn("authority");
                                int conf = tr.getIntColumn("confidence");
                                if (!((dcv.authority == null && auth == null) ||
                                  (dcv.authority != null && auth != null && dcv.authority.equals(auth))
                                 && dcv.confidence == conf))
                                {
                                    matched = false;
                                }
                            }

                            // If the db record is identical to the in memory values
                            if (matched)
                            {
                                // Flag that the metadata is already in the DB
                                storedMD[mdIdx] = true;

                                // Flag that we are not going to remove the row
                                removeRow = false;
                            }
                        }
                    }

                    // If after processing all the metadata values, we didn't find a match
                    // delete this row from the DB
                    if (removeRow)
                    {
                        DatabaseManager.delete(ourContext, tr);
                        metadataChanged = true;
                        modified = true;
                    }
                }
            }
            finally
            {
                tri.close();
            }
        }

        // Add missing in-memory metadata
        for (int mdIdx = 0; mdIdx < getMetadata().size(); mdIdx++)
        {
            // Only write values that are not already in the db
            if (!storedMD[mdIdx])
            {
                DCValue dcv = getMetadata().get(mdIdx);

                // Write DCValue
                MetadataValue metadata = new MetadataValue();
                metadata.setObjectType(getType());
                metadata.setObjectId(getID());
                metadata.setFieldId(mdFields[mdIdx].getFieldID());
                metadata.setValue(dcv.value);
                metadata.setLanguage(dcv.language);
                metadata.setPlace(placeNum[mdIdx]);
                metadata.setAuthority(dcv.authority);
                metadata.setConfidence(dcv.confidence);
                metadata.create(ourContext);
                metadataChanged = true;
                modified = true;
            }
        }
    }

    /**
     * Update the last-modified date of the object.
     */
    public abstract void updateLastModified();

    /**
     * Add metadata fields. These are appended to existing values.
     * Use {@link clearMetadata} to remove values. The ordering of values
     * passed in is maintained.
     * <p>
     * If metadata authority control is available, try to get authority
     * values.  The authority confidence depends on whether authority is
     * <em>required</em> or not.
     * @param schema
     *            the schema for the metadata field. <em>Must</em> match
     *            the <code>name</code> of an existing metadata schema.
     * @param element
     *            the metadata element name
     * @param qualifier
     *            the metadata qualifier name, or <code>null</code> for
     *            unqualified
     * @param lang
     *            the ISO639 language code, optionally followed by an underscore
     *            and the ISO3166 country code. <code>null</code> means the
     *            value has no language (for example, a date).
     * @param values
     *            the values to add.
     */
    public void addMetadata(String schema, String element, String qualifier, String lang,
            String[] values)
    {
        addMetadata(schema, element, qualifier, lang, values, null, null);
    }

    /**
     * Add a single metadata field. This is appended to existing
     * values. Use {@link clearMetadata} to remove values.
     *
     * @param schema
     *            the schema for the metadata field. <em>Must</em> match
     *            the <code>name</code> of an existing metadata schema.
     * @param element
     *            the metadata element name
     * @param qualifier
     *            the metadata qualifier, or <code>null</code> for
     *            unqualified
     * @param lang
     *            the ISO639 language code, optionally followed by an underscore
     *            and the ISO3166 country code. <code>null</code> means the
     *            value has no language (for example, a date).
     * @param value
     *            the value to add.
     */
    public void addMetadata(String schema, String element, String qualifier,
            String lang, String value)
    {
        String[] valArray = new String[1];
        valArray[0] = value;

        addMetadata(schema, element, qualifier, lang, valArray);
    }

    /**
     * Add a single metadata field. This is appended to existing
     * values. Use {@link clearMetadata} to remove values.
     *
     * @param schema
     *            the schema for the metadata field. <em>Must</em> match
     *            the <code>name</code> of an existing metadata schema.
     * @param element
     *            the metadata element name
     * @param qualifier
     *            the metadata qualifier, or <code>null</code> for
     *            unqualified
     * @param lang
     *            the ISO639 language code, optionally followed by an underscore
     *            and the ISO3166 country code. <code>null</code> means the
     *            value has no language (for example, a date).
     * @param value
     *            the value to add.
     * @param authority
     *            the external authority key for this value (or null)
     * @param confidence
     *            the authority confidence (default 0)
     */
    public void addMetadata(String schema, String element, String qualifier,
            String lang, String value, String authority, int confidence)
    {
        String[] valArray = new String[1];
        String[] authArray = new String[1];
        int[] confArray = new int[1];
        valArray[0] = value;
        authArray[0] = authority;
        confArray[0] = confidence;

        addMetadata(schema, element, qualifier, lang, valArray, authArray, confArray);
    }

    /**
     * Add metadata fields. These are appended to existing values.
     * Use {@link clearMetadata} to remove values. The ordering of values
     * passed in is maintained.
     * @param schema
     *            the schema for the metadata field. <em>Must</em> match
     *            the <code>name</code> of an existing metadata schema.
     * @param element
     *            the metadata element name
     * @param qualifier
     *            the metadata qualifier name, or <code>null</code> for
     *            unqualified
     * @param lang
     *            the ISO639 language code, optionally followed by an underscore
     *            and the ISO3166 country code. <code>null</code> means the
     *            value has no language (for example, a date).
     * @param values
     *            the values to add.
     * @param authorities
     *            the external authority key for this value (or null)
     * @param confidences
     *            the authority confidence (default 0)
     */
    public void addMetadata(String schema, String element, String qualifier, String lang,
            String[] values, String authorities[], int confidences[])
    {
        List<DCValue> metadata = getMetadata(); // XXX NB this refers to cache manager internals!
        MetadataAuthorityManager mam = MetadataAuthorityManager.getManager();
        boolean authorityControlled = mam.isAuthorityControlled(schema, element, qualifier);
        boolean authorityRequired = mam.isAuthorityRequired(schema, element, qualifier);
        String fieldName = schema+"."+element+((qualifier==null)? "": "."+qualifier);

        // We will not verify that they are valid entries in the registry
        // until update() is called.
        for (int i = 0; i < values.length; i++)
        {
            DCValue dcv = new DCValue();
            dcv.schema = schema;
            dcv.element = element;
            dcv.qualifier = qualifier;
            dcv.language = (lang == null ? null : lang.trim());

            // Logic to set Authority and Confidence:
            //  - normalize an empty string for authority to NULL.
            //  - if authority key is present, use given confidence or NOVALUE if not given
            //  - otherwise, preserve confidence if meaningful value was given since it may document a failed authority lookup
            //  - CF_UNSET signifies no authority nor meaningful confidence.
            //  - it's possible to have empty authority & CF_ACCEPTED if e.g. user deletes authority key
            if (authorityControlled)
            {
                if (authorities != null && authorities[i] != null && authorities[i].length() > 0)
                {
                    dcv.authority = authorities[i];
                    dcv.confidence = confidences == null ? Choices.CF_NOVALUE : confidences[i];
                }
                else
                {
                    dcv.authority = null;
                    dcv.confidence = confidences == null ? Choices.CF_UNSET : confidences[i];
                }
                // authority sanity check: if authority is required, was it supplied?
                // XXX FIXME? can't throw a "real" exception here without changing all the callers to expect it, so use a runtime exception
                if (authorityRequired && (dcv.authority == null || dcv.authority.length() == 0))
                {
                    throw new IllegalArgumentException("The metadata field \"" + fieldName + "\" requires an authority key but none was provided. Vaue=\"" + dcv.value + "\"");
                }
            }
            if (values[i] != null)
            {
                // remove control unicode char
                String temp = values[i].trim();
                char[] dcvalue = temp.toCharArray();
                for (int charPos = 0; charPos < dcvalue.length; charPos++)
                {
                    if (Character.isISOControl(dcvalue[charPos]) &&
                        !String.valueOf(dcvalue[charPos]).equals("\u0009") &&
                        !String.valueOf(dcvalue[charPos]).equals("\n") &&
                        !String.valueOf(dcvalue[charPos]).equals("\r"))
                    {
                        dcvalue[charPos] = ' ';
                    }
                }
                dcv.value = String.valueOf(dcvalue);
            }
            else
            {
                dcv.value = null;
            }
            metadata.add(dcv); // XXX NB this modifies an internal field of DublinCore!
            addDetails(fieldName);
        }

        if (values.length > 0)
        {
            metadataChanged = true;
        }
    }

    /**
     * Clear metadata values. As with {@link getMetadata} below,
     * passing in <code>null</code> only matches fields where the qualifier or
     * language is actually <code>null</code>.  <code>DSpaceObject.ANY</code> will
     * match any element, qualifier or language, including <code>null</code>.
     * Thus, <code>object.clearMetadata(DSpaceObject.ANY, DSpaceObject.ANY, DSpaceObject.ANY)</code> will
     * remove all Dublin Core metadata associated with an object.
     *
     * @param schema
     *            the schema for the metadata field. <em>Must</em> match
     *            the <code>name</code> of an existing metadata schema.
     * @param element
     *            the metadata element to remove, or <code>DSpaceObject.ANY</code>
     * @param qualifier
     *            the qualifier. <code>null</code> means unqualified, and
     *            <code>DSpaceObject.ANY</code> means any qualifier (including
     *            unqualified.)
     * @param lang
     *            the ISO639 language code, optionally followed by an underscore
     *            and the ISO3166 country code. <code>null</code> means only
     *            values with no language are removed, and <code>DSpaceObject.ANY</code>
     *            means values with any country code or no country code are
     *            removed.
     */
    public void clearMetadata(String schema, String element, String qualifier,
            String lang)
    {
        // We will build a list of values NOT matching the values to clear
        List<DCValue> values = new ArrayList<DCValue>();
        for (DCValue dcv : getMetadata())
        {
            if (!match(schema, element, qualifier, lang, dcv))
            {
                values.add(dcv);
            }
        }

        // Now swap the old list of values for the new, unremoved values
        setMetadata(values);
        metadataChanged = true;
    }

    /**
     * Get metadata for the object in a chosen schema.
     * See {@link MetadataSchema} for more information about schemas.
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
     * <code>item.getMetadata("dc", "title", null, DSpaceObject.ANY );</code>
     * <P>
     * Return all US English values of the "title" element, with any qualifier
     * (including unqualified):
     * <P>
     * <code>item.getMetadata("dc, "title", DSpaceObject.ANY, "en_US" );</code>
     * <P>
     * The ordering of values of a particular element/qualifier/language
     * combination is significant. When retrieving with wildcards, values of a
     * particular element/qualifier/language combinations will be adjacent, but
     * the overall ordering of the combinations is indeterminate.
     *
     * @param schema
     *            the schema for the metadata field. <em>Must</em> match
     *            the <code>name</code> of an existing metadata schema.
     * @param element
     *            the element name. <code>DSpaceObject.ANY</code> matches any
     *            element. <code>null</code> doesn't really make sense as all
     *            metadata must have an element.
     * @param qualifier
     *            the qualifier. <code>null</code> means unqualified, and
     *            <code>DSpaceObject.ANY</code> means any qualifier (including
     *            unqualified.)
     * @param lang
     *            the ISO639 language code, optionally followed by an underscore
     *            and the ISO3166 country code. <code>null</code> means only
     *            values with no language are returned, and
     *            <code>DSpaceObject.ANY</code> means values with any country code or
     *            no country code are returned.
     * @return metadata fields that match the parameters
     */
    public DCValue[] getMetadata(String schema, String element, String qualifier,
            String lang)
    {
        // Build up list of matching values
        List<DCValue> values = new ArrayList<DCValue>();
        for (DCValue dcv : getMetadata())
        {
            if (match(schema, element, qualifier, lang, dcv))
            {
                // We will return a copy of the object in case it is altered
                DCValue copy = new DCValue();
                copy.element = dcv.element;
                copy.qualifier = dcv.qualifier;
                copy.value = dcv.value;
                copy.language = dcv.language;
                copy.schema = dcv.schema;
                copy.authority = dcv.authority;
                copy.confidence = dcv.confidence;
                values.add(copy);
            }
        }

        // Create an array of matching values
        DCValue[] valueArray = new DCValue[values.size()];
        valueArray = (DCValue[]) values.toArray(valueArray);

        return valueArray;
    }

    /**
     * Retrieve metadata field values from a given metadata string
     * of the form <schema prefix>.<element>[.<qualifier>|.*]
     *
     * @param mdString
     *            The metadata string of the form
     *            <schema prefix>.<element>[.<qualifier>|.*]
     */
    public DCValue[] getMetadata(String mdString)
    {
        StringTokenizer dcf = new StringTokenizer(mdString, ".");

        String[] tokens = { "", "", "" };
        int i = 0;
        while(dcf.hasMoreTokens())
        {
            tokens[i] = dcf.nextToken().trim();
            i++;
        }
        String schema = tokens[0];
        String element = tokens[1];
        String qualifier = tokens[2];

        DCValue[] values;
        if ("*".equals(qualifier))
        {
            values = getMetadata(schema, element, ANY, ANY);
        }
        else if ("".equals(qualifier))
        {
            values = getMetadata(schema, element, null, ANY);
        }
        else
        {
            values = getMetadata(schema, element, qualifier, ANY);
        }

        return values;
    }

    /**
     * Get all metadata for this object.
     *
     * @return an empty list if the metadata could not be loaded.
     */
    protected List<DCValue> getMetadata()
    {
        try
        {
            return dublinCore.get(ourContext, getType(), getID(), log);
        }
        catch (SQLException e)
        {
            log.error("Loading object - cannot load metadata");
        }

        return new ArrayList<DCValue>();
    }

    /**
     * Replace the cached metadata.
     *
     * @param metadata new cache content.
     */
    protected void setMetadata(List<DCValue> metadata)
    {
        dublinCore.set(metadata);
        metadataChanged = true;
    }

    /**
     * Utility method for pattern-matching metadata elements.  This
     * method will return <code>true</code> if the given schema,
     * element, qualifier and language match the schema, element,
     * qualifier and language of the <code>DCValue</code> object passed
     * in.  Any or all of the element, qualifier and language passed
     * in can be the <code>DSpaceObject.ANY</code> wildcard.
     *
     * @param schema
     *            the schema for the metadata field. <em>Must</em> match
     *            the <code>name</code> of an existing metadata schema.
     * @param element
     *            the element to match, or <code>DSpaceObject.ANY</code>
     * @param qualifier
     *            the qualifier to match, or <code>DSpaceObject.ANY</code>
     * @param language
     *            the language to match, or <code>DSpaceObject.ANY</code>
     * @param dcv
     *            the Dublin Core value
     * @return <code>true</code> if there is a match
     */
    private boolean match(String schema, String element, String qualifier,
            String language, DCValue dcv)
    {
        // We will attempt to disprove a match - if we can't we have a match
        if (!element.equals(ANY) && !element.equals(dcv.element))
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
        else if (!qualifier.equals(ANY))
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
        else if (!language.equals(ANY))
        {
            // Not a wildcard, so language must match exactly
            if (!language.equals(dcv.language))
            {
                return false;
            }
        }

        if (!schema.equals(ANY))
        {
            if (dcv.schema != null && !dcv.schema.equals(schema))
            {
                // The namespace doesn't match
                return false;
            }
        }

        // If we get this far, we have a match
        return true;
    }

    /**
     * Select all metadata belonging to this object.
     *
     * @throws SQLException
     */
    protected TableRowIterator retrieveMetadata()
            throws SQLException
    {
        return DatabaseManager.queryTable(ourContext, "MetadataValue",
                "SELECT * FROM MetadataValue WHERE object_type = ? AND object_id= ? ORDER BY metadata_field_id, place",
                getType(), ourRow.getIntColumn("item_id"));
    }

    protected MetadataField getMetadataField(DCValue dcv)
            throws SQLException, AuthorizeException
    {
        if (allMetadataFields == null)
        {
            allMetadataFields = MetadataField.findAll(ourContext);
        }
        if (allMetadataFields != null)
        {
            int schemaID = getMetadataSchemaID(dcv);
            for (MetadataField field : allMetadataFields)
            {
                if (field.getSchemaID() == schemaID &&
                        StringUtils.equals(field.getElement(), dcv.element) &&
                        StringUtils.equals(field.getQualifier(), dcv.qualifier))
                {
                    return field;
                }
            }
        }
        return null;
    }

    private int getMetadataSchemaID(DCValue dcv)
            throws SQLException
    {
        int schemaID;
        MetadataSchema schema = MetadataSchema.find(ourContext, dcv.schema);
        if (schema == null)
        {
            schemaID = MetadataSchema.DC_SCHEMA_ID;
        }
        else
        {
            schemaID = schema.getSchemaID();
        }
        return schemaID;
    }

    /**
     * Cache all metadata on this object from the database.
     */
    protected class MetadataCache
    {
        /** The cached metadata */
        List<DCValue> metadata = null;

        /**
         * Return reference to the cache of all metadata for a given object ID,
         * loading them from the database as needed.
         *
         * @param c
         * @param objectType
         * @param objectId
         * @param log
         * @return
         * @throws SQLException
         */
        List<DCValue> get(Context c, int objectType, int objectId, Logger log)
                throws SQLException
        {
            if (metadata == null)
            {
                metadata = new ArrayList<DCValue>();

                // Get Dublin Core metadata
                TableRowIterator tri = retrieveMetadata(objectType, objectId);

                if (tri != null)
                {
                    try
                    {
                        while (tri.hasNext())
                        {
                            TableRow resultRow = tri.next();

                            // Get the associated metadata field and schema information
                            int fieldID = resultRow.getIntColumn("metadata_field_id");
                            MetadataField field = MetadataField.find(c, fieldID);

                            if (field == null)
                            {
                                log.error("Loading object - cannot find metadata field " + fieldID);
                            }
                            else
                            {
                                MetadataSchema schema = MetadataSchema.find(c, field.getSchemaID());
                                if (schema == null)
                                {
                                    log.error("Loading object - cannot find metadata schema " + field.getSchemaID() + ", field " + fieldID);
                                }
                                else
                                {
                                    // Make a DCValue object
                                    DCValue dcv = new DCValue();
                                    dcv.element = field.getElement();
                                    dcv.qualifier = field.getQualifier();
                                    dcv.value = resultRow.getStringColumn("text_value");
                                    dcv.language = resultRow.getStringColumn("text_lang");
                                    //dcv.namespace = schema.getNamespace();
                                    dcv.schema = schema.getName();
                                    dcv.authority = resultRow.getStringColumn("authority");
                                    dcv.confidence = resultRow.getIntColumn("confidence");

                                    // Add it to the list
                                    metadata.add(dcv);
                                }
                            }
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
                }
            }

            return metadata;
        }

        /**
         * Replace the cache content.
         *
         * @param m new metadata to cache.
         */
        void set(List<DCValue> m)
        {
            metadata = m;
        }

        /**
         * Select from the database all metadata of a given object.
         *
         * @param objectType
         * @param objectId
         * @return iterator over the given object's metadata, or null if objectId = 0.
         * @throws SQLException
         */
        TableRowIterator retrieveMetadata(int objectType, int objectId)
                throws SQLException
        {
            if (objectId > 0)
            {
                return DatabaseManager.queryTable(ourContext, "MetadataValue",
                        "SELECT * FROM MetadataValue" +
                        " WHERE object_type = ? AND object_id= ?" +
                        " ORDER BY metadata_field_id, place",
                        objectType, objectId);
            }

            return null;
        }
    }
}
