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

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.authority.ChoiceAuthorityManager;
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
import org.dspace.handle.HandleManager;
import org.dspace.identifier.IdentifierService;
import org.dspace.utils.DSpace;

/**
 * Abstract base class for DSpace objects
 */
public abstract class DSpaceObject
{
    /** Our context */
    protected Context ourContext;

    /** log4j category */
    private static final Logger log = Logger.getLogger(DSpaceObject.class);

    /**
     * True if anything else was changed since last update()
     * (to drive event mechanism)
     */
    protected boolean modifiedMetadata;


    // accumulate information to add to "detail" element of content Event,
    // e.g. to document metadata fields touched, etc.
    private StringBuffer eventDetails = null;
    
    private String[] identifiers = null;

    /** The Dublin Core metadata - inner class for lazy loading */
    protected MetadataCache metadataCache = new MetadataCache();


    /**
     * Construct a DSpaceOBject with the given table row
     *
     * @throws SQLException
     */
    protected DSpaceObject()
    {
        modifiedMetadata = false;
    }

    protected DSpaceObject(Context context)
    {
        ourContext = context;
        modifiedMetadata = false;
    }


    public void updateMetadata() throws SQLException, AuthorizeException {
        // Map counting number of values for each element/qualifier.
        // Keys are Strings: "element" or "element.qualifier"
        // Values are Integers indicating number of values written for a
        // element/qualifier
        Map<String,Integer> elementCount = new HashMap<String,Integer>();

        modifiedMetadata = false;

        // Arrays to store the working information required
        int[]     placeNum = new int[getMetadata().size()];
        boolean[] storedDC = new boolean[getMetadata().size()];
        MetadataField[] dcFields = new MetadataField[getMetadata().size()];

        // Work out the place numbers for the in memory DC
        for (int dcIdx = 0; dcIdx < getMetadata().size(); dcIdx++)
        {
            Metadatum dcv = getMetadata().get(dcIdx);

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
            placeNum[dcIdx] = current;
            storedDC[dcIdx] = false;
            dcFields[dcIdx] = getMetadataField(dcv);
            if (dcFields[dcIdx] == null)
            {
                // Bad DC field, log and throw exception
                log.warn("Invalid metadata field: [" + dcv.getField() + "] : [" + dcv.value + "]");
                throw new SQLException("Invalid metadata field: [" + dcv.getField() + "]");
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
                    for (int dcIdx = 0; dcIdx < getMetadata().size() && removeRow; dcIdx++)
                    {
                        // Only process if this metadata has not already been matched to something in the DB
                        if (!storedDC[dcIdx])
                        {
                            boolean matched = true;
                            Metadatum dcv   = getMetadata().get(dcIdx);

                            // Check the metadata field is the same
                            if (matched && dcFields[dcIdx].getFieldID() != tr.getIntColumn("metadata_field_id"))
                            {
                                matched = false;
                            }

                            // Check the place is the same
                            if (matched && placeNum[dcIdx] != tr.getIntColumn("place"))
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
                                storedDC[dcIdx] = true;

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
                        modifiedMetadata = true;
                    }
                }
            }
            finally
            {
                tri.close();
            }


        }

        // Add missing in-memory DC
        for (int dcIdx = 0; dcIdx < getMetadata().size(); dcIdx++)
        {
            // Only write values that are not already in the db
            if (!storedDC[dcIdx])
            {
                Metadatum dcv = getMetadata().get(dcIdx);

                // Write Metadatum
                MetadataValue metadata = new MetadataValue();
                metadata.setResourceId(getID());
                metadata.setResourceTypeId(getType());
                metadata.setFieldId(dcFields[dcIdx].getFieldID());
                metadata.setValue(dcv.value);
                metadata.setLanguage(dcv.language);
                metadata.setPlace(placeNum[dcIdx]);
                metadata.setAuthority(dcv.authority);
                metadata.setConfidence(dcv.confidence);
                metadata.create(ourContext);
                modifiedMetadata = true;
            }
        }

        if(modifiedMetadata) {
            ourContext.addEvent(new Event(Event.MODIFY_METADATA, getType(), getID(), getDetails(), getIdentifiers(ourContext)));
            modifiedMetadata = false;
        }
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
     * Tries to lookup all Identifiers of this DSpaceObject.
     * @return An array containing all found identifiers or an array with a length of 0.
     */
    public String[] getIdentifiers(Context context)
    {
        if (identifiers == null)
        {
            log.debug("This DSO's identifiers cache is empty, looking for identifiers...");
            identifiers = new String[0];

            IdentifierService identifierService = 
                    new DSpace().getSingletonService(IdentifierService.class);

            if (identifierService != null)
            {
                identifiers = identifierService.lookup(context, this);
            } else {
                log.warn("No IdentifierService found, will return an array containing "
                        + "the Handle only.");
                if (getHandle() != null)
                {
                    identifiers = new String[] { HandleManager.getCanonicalForm(getHandle()) };
                }
            }
        }

        // it the DSO has no identifiers at all including handle, we should return an empty array.
        // G.e. items during submission (workspace items) have no handle and no other identifier.
        if (identifiers == null)
        {
            identifiers = new String[] {};
        }

        if (log.isDebugEnabled())
        {
            StringBuilder dbgMsg = new StringBuilder();
            for (String id : identifiers)
            {
                if (dbgMsg.capacity() == 0)
                {
                    dbgMsg.append("This DSO's Identifiers are: ");
                } else {
                    dbgMsg.append(", ");
                }
                dbgMsg.append(id);
            }
            dbgMsg.append(".");
            log.debug(dbgMsg.toString());
        }

        return identifiers;
    }
    
    public void resetIdentifiersCache()
    {
        identifiers = null;
    }

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

    public abstract void update() throws SQLException, AuthorizeException;

    public abstract void updateLastModified();

    private TableRowIterator retrieveMetadata() throws SQLException
    {
        return DatabaseManager.queryTable(ourContext, "MetadataValue",
                "SELECT * FROM MetadataValue WHERE resource_id= ? and resource_type_id = ? ORDER BY metadata_field_id, place",
                getID(),
                getType());
    }

    /**
     * Get Dublin Core metadata for the DSpace Object.
     * Passing in a <code>null</code> value for <code>qualifier</code>
     * or <code>lang</code> only matches Dublin Core fields where that
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
     * <code>dspaceobject.getDC( "title", null, DSpaceObject.ANY );</code>
     * <P>
     * Return all US English values of the "title" element, with any qualifier
     * (including unqualified):
     * <P>
     * <code>dspaceobject.getDC( "title", DSpaceObject.ANY, "en_US" );</code>
     * <P>
     * The ordering of values of a particular element/qualifier/language
     * combination is significant. When retrieving with wildcards, values of a
     * particular element/qualifier/language combinations will be adjacent, but
     * the overall ordering of the combinations is indeterminate.
     *
     * @param element
     *            the Dublin Core element. <code>DSpaceObject.ANY</code> matches any
     *            element. <code>null</code> doesn't really make sense as all
     *            DC must have an element.
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
     * @return Dublin Core fields that match the parameters
     */
    @Deprecated
    public Metadatum[] getDC(String element, String qualifier, String lang)
    {
        return getMetadata(MetadataSchema.DC_SCHEMA, element, qualifier, lang);
    }

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
    public Metadatum[] getMetadata(String schema, String element, String qualifier,
                                 String lang)
    {
        // Build up list of matching values
        List<Metadatum> values = new ArrayList<Metadatum>();
        for (Metadatum dcv : getMetadata())
        {
            if (match(schema, element, qualifier, lang, dcv))
            {
                // We will return a copy of the object in case it is altered
                Metadatum copy = new Metadatum();
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
        Metadatum[] valueArray = new Metadatum[values.size()];
        valueArray = (Metadatum[]) values.toArray(valueArray);

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
    public Metadatum[] getMetadataByMetadataString(String mdString)
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

        Metadatum[] values;
        if ("*".equals(qualifier))
        {
            values = getMetadata(schema, element, Item.ANY, Item.ANY);
        }
        else if ("".equals(qualifier))
        {
            values = getMetadata(schema, element, null, Item.ANY);
        }
        else
        {
            values = getMetadata(schema, element, qualifier, Item.ANY);
        }

        return values;
    }

    /**
     * Retrieve first metadata field value
     */
    protected String getMetadataFirstValue(String schema, String element, String qualifier, String language){
        Metadatum[] dcvalues = getMetadata(schema, element, qualifier, Item.ANY);
        if(dcvalues.length>0){
            return dcvalues[0].value;
        }
        return null;
    }

    /**
     * Set first metadata field value
     */
    protected void setMetadataSingleValue(String schema, String element, String qualifier, String language, String value) {
        if(value != null)
        {
            clearMetadata(schema, element, qualifier, language);
            addMetadata(schema, element, qualifier, language, value);
            modifiedMetadata = true;
        }
    }

    protected List<Metadatum> getMetadata()
    {
        try
        {
            return metadataCache.get(ourContext, getID(), getType(), log);
        }
        catch (SQLException e)
        {
            log.error("Loading item - cannot load metadata");
        }

        return new ArrayList<Metadatum>();
    }

    /**
     * Get the value of a metadata field
     *
     * @param value
     *            the name of the metadata field to get
     *
     * @return the value of the metadata field (or null if the column is an SQL NULL)
     *
     * @exception IllegalArgumentException
     *                if the requested metadata field doesn't exist
     */
    public String getMetadata(String value){
        Metadatum[] dcvalues = getMetadataByMetadataString(value);

        if(dcvalues.length>0) {
            return dcvalues[0].value;
        }
        return null;
    }

    public List<Metadatum> getMetadata(String mdString, String authority) {
        String[] elements = getElements(mdString);
        return getMetadata(elements[0], elements[1], elements[2], elements[3], authority);
    }

    public List<Metadatum> getMetadata(String schema, String element, String qualifier, String lang, String authority) {
        Metadatum[] metadata = getMetadata(schema, element, qualifier, lang);
        List<Metadatum> dcValues = Arrays.asList(metadata);
        if (!authority.equals(Item.ANY)) {
            Iterator<Metadatum> iterator = dcValues.iterator();
            while (iterator.hasNext()) {
                Metadatum dcValue = iterator.next();
                if (!authority.equals(dcValue.authority)) {
                    iterator.remove();
                }
            }
        }
        return dcValues;
    }


    /**
     * Splits "schema.element.qualifier.language" into an array.
     * <p/>
     * The returned array will always have length >= 4
     * <p/>
     * Values in the returned array can be empty or null.
     */
    public static String[] getElements(String fieldName) {
        String[] tokens = StringUtils.split(fieldName, ".");

        int add = 4 - tokens.length;
        if (add > 0) {
            tokens = (String[]) ArrayUtils.addAll(tokens, new String[add]);
        }

        return tokens;
    }

    /**
     * Splits "schema.element.qualifier.language" into an array.
     * <p/>
     * The returned array will always have length >= 4
     * <p/>
     * When @param fill is true, elements that would be empty or null are replaced by Item.ANY
     */
    public static String[] getElementsFilled(String fieldName) {
        String[] elements = getElements(fieldName);
        for (int i = 0; i < elements.length; i++) {
            if (StringUtils.isBlank(elements[i])) {
                elements[i] = Item.ANY;
            }
        }
        return elements;
    }

    public void replaceMetadataValue(Metadatum oldValue, Metadatum newValue)
    {
        // check both dcvalues are for the same field
        if (oldValue.hasSameFieldAs(newValue)) {

            String schema = oldValue.schema;
            String element = oldValue.element;
            String qualifier = oldValue.qualifier;

            // Save all metadata for this field
            Metadatum[] dcvalues = getMetadata(schema, element, qualifier, Item.ANY);
            clearMetadata(schema, element, qualifier, Item.ANY);
            for (Metadatum dcvalue : dcvalues) {
                if (dcvalue.equals(oldValue)) {
                    addMetadata(schema, element, qualifier, newValue.language, newValue.value, newValue.authority, newValue.confidence);
                } else {
                    addMetadata(schema, element, qualifier, dcvalue.language, dcvalue.value, dcvalue.authority, dcvalue.confidence);
                }
            }
        }
    }



    /**
     * Add Dublin Core metadata fields. These are appended to existing values.
     * Use <code>clearDC</code> to remove values. The ordering of values
     * passed in is maintained.
     *
     * @param element
     *            the Dublin Core element
     * @param qualifier
     *            the Dublin Core qualifier, or <code>null</code> for
     *            unqualified
     * @param lang
     *            the ISO639 language code, optionally followed by an underscore
     *            and the ISO3166 country code. <code>null</code> means the
     *            value has no language (for example, a date).
     * @param values
     *            the values to add.
     */
    @Deprecated
    public void addDC(String element, String qualifier, String lang,
                      String[] values)
    {
        addMetadata(MetadataSchema.DC_SCHEMA, element, qualifier, lang, values);
    }

    /**
     * Add a single Dublin Core metadata field. This is appended to existing
     * values. Use <code>clearDC</code> to remove values.
     *
     * @param element
     *            the Dublin Core element
     * @param qualifier
     *            the Dublin Core qualifier, or <code>null</code> for
     *            unqualified
     * @param lang
     *            the ISO639 language code, optionally followed by an underscore
     *            and the ISO3166 country code. <code>null</code> means the
     *            value has no language (for example, a date).
     * @param value
     *            the value to add.
     */
    @Deprecated
    public void addDC(String element, String qualifier, String lang,
                      String value)
    {
        addMetadata(MetadataSchema.DC_SCHEMA, element, qualifier, lang, value);
    }

    /**
     * Add metadata fields. These are appended to existing values.
     * Use <code>clearDC</code> to remove values. The ordering of values
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
        MetadataAuthorityManager mam = MetadataAuthorityManager.getManager();
        String fieldKey = MetadataAuthorityManager.makeFieldKey(schema, element, qualifier);
        if (mam.isAuthorityControlled(fieldKey))
        {
            String authorities[] = new String[values.length];
            int confidences[] = new int[values.length];
            for (int i = 0; i < values.length; ++i)
            {
                getAuthoritiesAndConfidences(fieldKey, values, authorities, confidences, i);
            }
            addMetadata(schema, element, qualifier, lang, values, authorities, confidences);
        }
        else
        {
            addMetadata(schema, element, qualifier, lang, values, null, null);
        }
    }

    protected void getAuthoritiesAndConfidences(String fieldKey, String[] values, String[] authorities, int[] confidences, int i) {
        Choices c = ChoiceAuthorityManager.getManager().getBestMatch(fieldKey, values[i], -1, null);
        authorities[i] = c.values.length > 0 ? c.values[0].authority : null;
        confidences[i] = c.confidence;
    }

    /**
     * Add metadata fields. These are appended to existing values.
     * Use <code>clearDC</code> to remove values. The ordering of values
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
        List<Metadatum> dublinCore = getMetadata();
        MetadataAuthorityManager mam = MetadataAuthorityManager.getManager();
        boolean authorityControlled = mam.isAuthorityControlled(schema, element, qualifier);
        boolean authorityRequired = mam.isAuthorityRequired(schema, element, qualifier);
        String fieldName = schema+"."+element+((qualifier==null)? "": "."+qualifier);

        // We will not verify that they are valid entries in the registry
        // until update() is called.
        for (int i = 0; i < values.length; i++)
        {
            Metadatum dcv = new Metadatum();
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
            dublinCore.add(dcv);
            addDetails(fieldName);
        }

        if (values.length > 0)
        {
            modifiedMetadata = true;
        }
    }

    /**
     * Add a single metadata field. This is appended to existing
     * values. Use <code>clearDC</code> to remove values.
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
     * values. Use <code>clearDC</code> to remove values.
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
     * Clear Dublin Core metadata values. As with <code>getDC</code> above,
     * passing in <code>null</code> only matches fields where the qualifier or
     * language is actually <code>null</code>.<code>Item.ANY</code> will
     * match any element, qualifier or language, including <code>null</code>.
     * Thus, <code>idspaceobject.clearDC(DSpaceObject.ANY, DSpaceObject.ANY, DSpaceObject.ANY)</code> will
     * remove all Dublin Core metadata associated with an DSpaceObject.
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
    @Deprecated
    public void clearDC(String element, String qualifier, String lang)
    {
        clearMetadata(MetadataSchema.DC_SCHEMA, element, qualifier, lang);
    }

    /**
     * Clear metadata values. As with <code>getDC</code> above,
     * passing in <code>null</code> only matches fields where the qualifier or
     * language is actually <code>null</code>.<code>Item.ANY</code> will
     * match any element, qualifier or language, including <code>null</code>.
     * Thus, <code>dspaceobject.clearDC(Item.ANY, Item.ANY, Item.ANY)</code> will
     * remove all Dublin Core metadata associated with an DSpaceObject.
     *
     * @param schema
     *            the schema for the metadata field. <em>Must</em> match
     *            the <code>name</code> of an existing metadata schema.
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
    public void clearMetadata(String schema, String element, String qualifier,
                              String lang)
    {
        // We will build a list of values NOT matching the values to clear
        List<Metadatum> values = new ArrayList<Metadatum>();
        for (Metadatum dcv : getMetadata())
        {
            if (!match(schema, element, qualifier, lang, dcv))
            {
                values.add(dcv);
            }
        }

        // Now swap the old list of values for the new, unremoved values
        setMetadata(values);
        modifiedMetadata = true;
    }

    /**
     * Utility method for pattern-matching metadata elements.  This
     * method will return <code>true</code> if the given schema,
     * element, qualifier and language match the schema, element,
     * qualifier and language of the <code>Metadatum</code> object passed
     * in.  Any or all of the element, qualifier and language passed
     * in can be the <code>Item.ANY</code> wildcard.
     *
     * @param schema
     *            the schema for the metadata field. <em>Must</em> match
     *            the <code>name</code> of an existing metadata schema.
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
    private boolean match(String schema, String element, String qualifier,
                          String language, Metadatum dcv)
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

        if (!schema.equals(Item.ANY))
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

    protected transient MetadataField[] allMetadataFields = null;
    protected MetadataField getMetadataField(Metadatum dcv) throws SQLException, AuthorizeException
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

    private int getMetadataSchemaID(Metadatum dcv) throws SQLException
    {
        int schemaID;
        MetadataSchema schema = MetadataSchema.find(ourContext,dcv.schema);
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
     * Utility method to remove all descriptive metadata associated with the DSpaceObject from
     * the database (regardless of in-memory version)
     *
     * @throws SQLException
     */
    protected void removeMetadataFromDatabase() throws SQLException
    {
        DatabaseManager.updateQuery(ourContext,
                "DELETE FROM MetadataValue WHERE resource_id= ? and resource_type_id=?",
                getID(),
                getType());
    }

    private void setMetadata(List<Metadatum> metadata)
    {
        metadataCache.set(metadata);
        modifiedMetadata = true;
    }

    class MetadataCache
    {
        List<Metadatum> metadata = null;

        List<Metadatum> get(Context c, int resourceId, int resourceTypeId, Logger log) throws SQLException
        {
            if (metadata == null)
            {
                metadata = new ArrayList<Metadatum>();

                // Get Dublin Core metadata
                TableRowIterator tri = retrieveMetadata(resourceId, resourceTypeId);

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
                                log.error("Loading item - cannot find metadata field " + fieldID + " for resourceType=" + resourceTypeId + " and resourceId=" + resourceId);
                            }
                            else
                            {
                                MetadataSchema schema = MetadataSchema.find(c, field.getSchemaID());
                                if (schema == null)
                                {
                                    log.error("Loading item - cannot find metadata schema " + field.getSchemaID() + ", field " + fieldID);
                                }
                                else
                                {
                                    // Make a Metadatum object
                                    Metadatum dcv = new Metadatum();
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

        void set(List<Metadatum> m)
        {
            metadata = m;
        }

        TableRowIterator retrieveMetadata(int resourceId, int resourceTypeId) throws SQLException
        {
            return DatabaseManager.queryTable(ourContext, "MetadataValue",
                    "SELECT * FROM MetadataValue WHERE resource_id= ? and resource_type_id = ? ORDER BY metadata_field_id, place",
                    resourceId,
                    resourceTypeId);
        }
    }

    protected String[] getMDValueByField(String field){
        StringTokenizer dcf = new StringTokenizer(field, ".");

        String[] tokens = { "", "", "" };
        int i = 0;
        while(dcf.hasMoreTokens()){
            tokens[i] = dcf.nextToken().trim();
            i++;
        }

        if(i!=0){
            return tokens;
        }else{
            return getMDValueByLegacyField(field);
        }
    }

    protected String[] getMDValueByLegacyField(String field){
        switch (field) {
            case "introductory_text":
                return new String[]{MetadataSchema.DC_SCHEMA, "description", null};
            case "short_description":
                return new String[]{MetadataSchema.DC_SCHEMA, "description", "abstract"};
            case "side_bar_text":
                return new String[]{MetadataSchema.DC_SCHEMA, "description", "tableofcontents"};
            case "copyright_text":
                return new String[]{MetadataSchema.DC_SCHEMA, "rights", null};
            case "name":
                return new String[]{MetadataSchema.DC_SCHEMA, "title", null};
            case "provenance_description":
                return new String[]{MetadataSchema.DC_SCHEMA,"provenance", null};
            case "license":
                return new String[]{MetadataSchema.DC_SCHEMA, "rights", "license"};
            case "user_format_description":
                return new String[]{MetadataSchema.DC_SCHEMA,"format",null};
            case "source":
                return new String[]{MetadataSchema.DC_SCHEMA,"source",null};
            case "firstname":
                return new String[]{"eperson","firstname",null};
            case "lastname":
                return new String[]{"eperson","lastname",null};
            case "phone":
                return new String[]{"eperson","phone",null};
            case "language":
                return new String[]{"eperson","language",null};
            default:
                return new String[]{null, null, null};
        }
    }
    
}