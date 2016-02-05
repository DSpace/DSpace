/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.event.Event;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.*;

/**
 * Base Class supporting all DSpace Authority Model
 * Objects. Provides Common Metadata Support to any
 * Object that inherits this class. Ideally, this class
 * is a model for providing metadata across all DSpace
 * Objects. Eventual this code may be pushed up into DSpaceObject
 * and new tables created for Community, Collection, Bundle, Bitstream
 * and so on.
 *
 * @author Lantian Gai, Mark Diggory
 */
public abstract class AuthorityObject extends DSpaceObject {

    // findAll sortby types
    public static final int ID = 0; // sort by ID

    public static final int NAME = 1; // sort by NAME (default)

    /** log4j logger */
    private static Logger log = Logger.getLogger(AuthorityObject.class);

    /** Our context */
    protected Context myContext;

    /** The row in the table representing this object */
    protected TableRow myRow;

    /** lists that need to be written out again */
    protected boolean modified = false;

    private boolean metadataModified=false;

    ArrayList<AuthorityMetadataValue>  metadataValues;

    AuthorityObject(Context context, TableRow row) throws SQLException
    {
        myContext = context;
        myRow = row;


        // Cache ourselves
        context.cache(this, row.getIntColumn("id"));
        metadataValues = getMetadata();
        modified = false;
        clearDetails();
    }

    public String getIdentifier()
    {
        return myRow.getStringColumn("identifier");

    }

    protected void setIdentifier(String identifier)
    {
        myRow.setColumn("identifier", identifier);
        modified = true;
    }
    public void updateLastModified() {
        try {
            Date lastModified = new java.sql.Timestamp(new Date().getTime());
            myRow.setColumn("modified", lastModified);
            DatabaseManager.updateQuery(myContext, "UPDATE "+getType()+" SET modified = ? WHERE id= ? ", lastModified, getID());
            //Also fire a modified event since the item HAS been modified
            //ourContext.addEvent(new Event(Event.MODIFY, Constants.ITEM, getID(), null));
        } catch (SQLException e) {
            log.error(LogManager.getHeader(myContext, "Error while updating modified timestamp", getType()+": " + getID()));
        }
    }

    /**
     * Used to Identify the Table that Metadata Is stored in for this Authority Object.
     * @return
     */
    public abstract String getMetadataTable();

    public ArrayList<AuthorityMetadataValue> getMetadata()
    {
        try
        {
            if(metadataValues==null){

                metadataValues = new ArrayList<AuthorityMetadataValue>();

                // Get Dublin Core metadata
                TableRowIterator tri = retrieveMetadata();

                if (tri != null)
                {
                    try
                    {
                        while (tri.hasNext())
                        {
                            TableRow resultRow = tri.next();

                            // Get the associated metadata field and schema information
                            int fieldID = resultRow.getIntColumn("field_id");
                            MetadataField field = MetadataField.find(myContext, fieldID);

                            if (field == null)
                            {
                                log.error("Loading - cannot find metadata field " + fieldID);
                            }
                            else
                            {
                                MetadataSchema schema = MetadataSchema.find(myContext, field.getSchemaID());
                                if (schema == null)
                                {
                                    log.error("Loading - cannot find metadata schema " + field.getSchemaID() + ", field " + fieldID);
                                }
                                else
                                {
                                    // Make a DCValue object
                                    AuthorityMetadataValue dcv = new AuthorityMetadataValue(resultRow);
                                    dcv.element = field.getElement();
                                    //dcv.namespace = schema.getNamespace();
                                    dcv.schema = schema.getName();
                                    dcv.qualifier = field.getQualifier();

                                    // Add it to the list
                                    metadataValues.add(dcv);
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

            return metadataValues;
        }
        catch (SQLException e)
        {
            log.error("Loading item - cannot load metadata");
        }

        return new ArrayList<AuthorityMetadataValue>();
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
     * @param schema
     *            the schema for the metadata field. <em>Must</em> match
     *            the <code>name</code> of an existing metadata schema.
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
    public AuthorityMetadataValue[] getMetadata(String schema, String element, String qualifier,
                                                String lang)
    {
        // Build up list of matching values
        List<AuthorityMetadataValue> values = new ArrayList<AuthorityMetadataValue>();
        ArrayList<AuthorityMetadataValue> amv = getMetadata();
        for (AuthorityMetadataValue dcv : amv)
        {
            if (match(schema, element, qualifier, lang, dcv))
            {
                // We will return a copy of the object in case it is altered
                AuthorityMetadataValue copy = new AuthorityMetadataValue(this.getMetadataTable());
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
        AuthorityMetadataValue[] valueArray = new AuthorityMetadataValue[values.size()];
        valueArray = (AuthorityMetadataValue[]) values.toArray(valueArray);

        return valueArray;
    }


    TableRowIterator retrieveMetadata() throws SQLException
    {
        if (getID() > 0)
        {
            return DatabaseManager.queryTable(myContext, this.getMetadataTable() ,
                    "SELECT * FROM " + this.getMetadataTable() + " WHERE parent_id= ? ORDER BY field_id",
                    getID());
        }

        return null;
    }

    public void clearMetadata(String schema, String element, String qualifier,
                              String lang)
    {
        // We will build a list of values NOT matching the values to clear
        ArrayList<AuthorityMetadataValue> values = new ArrayList<AuthorityMetadataValue>();
        for (AuthorityMetadataValue dcv : getMetadata())
        {
            boolean match = match(schema, element, qualifier, lang, dcv);
            if (!match)
            {
                values.add(dcv);
            }
        }

        // Now swap the old list of values for the new, unremoved values
        setMetadata(values);
        metadataModified = true;
        myContext.addEvent(new Event(Event.MODIFY_METADATA, getType(), getID(), null));
    }
    private void setMetadata(ArrayList<AuthorityMetadataValue> metadata)
    {
        metadataValues =metadata;
        metadataModified = true;
    }
    private boolean match(String schema, String element, String qualifier,
                          String language, AuthorityMetadataValue dcv)
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
    public void addMetadata(String schema, String element, String qualifier, String lang,
                            String[] values, String authorities[], int confidences[])
    {

        MetadataAuthorityManager mam = MetadataAuthorityManager.getManager();
        boolean authorityControlled = mam.isAuthorityControlled(schema, element, qualifier);
        boolean authorityRequired = mam.isAuthorityRequired(schema, element, qualifier);
        String fieldName = schema+"."+element+((qualifier==null)? "": "."+qualifier);

        // We will not verify that they are valid entries in the registry
        // until update() is called.
        for (int i = 0; i < values.length; i++)
        {
            AuthorityMetadataValue dcv = new AuthorityMetadataValue(this.getMetadataTable());
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
            metadataValues.add(dcv);
            addDetails(fieldName);
        }

        if (values.length > 0)
        {
            metadataModified = true;
        }
    }
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

    private transient MetadataField[] allMetadataFields = null;

    protected MetadataField getMetadataField(AuthorityMetadataValue dcv) throws SQLException, AuthorizeException
    {
        if (allMetadataFields == null)
        {
            allMetadataFields = MetadataField.findAll(myContext);
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
    private int getMetadataSchemaID(AuthorityMetadataValue dcv) throws SQLException
    {
        int schemaID;
        MetadataSchema schema = MetadataSchema.find(myContext,dcv.schema);
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



    Map<String,Integer> elementCount = new HashMap<String,Integer>();
    /**
     * Update the scheme - writing out scheme object and Concept list if necessary
     */
    public void update() throws SQLException, AuthorizeException
    {
        if(metadataModified)
        {
            metadataModified = false;

            // Arrays to store the working information required
            int[]     placeNum = new int[getMetadata().size()];
            boolean[] storedDC = new boolean[getMetadata().size()];
            MetadataField[] dcFields = new MetadataField[getMetadata().size()];

            // Work out the place numbers for the in memory DC
            for (int dcIdx = 0; dcIdx < getMetadata().size(); dcIdx++)
            {
                AuthorityMetadataValue dcv = getMetadata().get(dcIdx);

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
                    log.warn(LogManager
                            .getHeader(myContext, "bad_dc",
                                    "Bad DC field. schema=" + dcv.schema
                                            + ", element: \""
                                            + ((dcv.element == null) ? "null"
                                            : dcv.element)
                                            + "\" qualifier: \""
                                            + ((dcv.qualifier == null) ? "null"
                                            : dcv.qualifier)
                                            + "\" value: \""
                                            + ((dcv.value == null) ? "null"
                                            : dcv.value) + "\""));

                    throw new SQLException("bad_dublin_core "
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
                        for (int dcIdx = 0; dcIdx < getMetadata().size() && removeRow; dcIdx++)
                        {
                            // Only process if this metadata has not already been matched to something in the DB
                            if (!storedDC[dcIdx])
                            {
                                boolean matched = true;
                                AuthorityMetadataValue dcv   = getMetadata().get(dcIdx);

                                // Check the metadata field is the same
                                if (matched && dcFields[dcIdx].getFieldID() != tr.getIntColumn("field_id"))
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
                            DatabaseManager.delete(myContext, tr);
                            metadataModified = true;
                            modified = true;
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
                    AuthorityMetadataValue dcv = getMetadata().get(dcIdx);

                    // Write DCValue
                    AuthorityMetadataValue metadata = new AuthorityMetadataValue(this.getMetadataTable());
                    metadata.setParentId(getID());
                    metadata.setFieldId(dcFields[dcIdx].getFieldID());
                    metadata.setValue(dcv.value);
                    metadata.setLanguage(dcv.language);
                    metadata.setPlace(placeNum[dcIdx]);
                    metadata.setAuthority(dcv.authority);
                    metadata.setConfidence(dcv.confidence);
                    metadata.create(myContext);
                    metadataModified = true;
                    modified = true;
                }
            }


            metadataModified=false;
            myContext.addEvent(new Event(Event.MODIFY_METADATA, this.getType(), getID(), null));
        }


        if (metadataModified || modified)
        {
            // Set the last modified date
            myRow.setColumn("modified", new Date());
            DatabaseManager.update(myContext, myRow);

            if (metadataModified)
            {
                myContext.addEvent(new Event(Event.MODIFY_METADATA, this.getType(), getID(), getDetails()));
                clearDetails();
                metadataModified = false;
            }

            myContext.addEvent(new Event(Event.MODIFY, this.getType(), getID(), null));
            modified = false;
        }

    }

    public static AuthorityObject find(Context context, int type, int id)
            throws SQLException
    {
        switch (type)
        {
            case Constants.SCHEME : return Scheme.find(context, id);
            case Constants.CONCEPT    : return Concept.find(context, id);
            case Constants.TERM      : return Term.find(context, id);
        }
        return null;
    }

    public static String createIdentifier(){
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String hash(String input) {

        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            byte[] data = input.getBytes();
            m.update(data, 0, data.length);
            BigInteger i = new BigInteger(1, m.digest());
            return String.format("%1$032X", i);
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage(),e);
            throw new RuntimeException(e.getMessage(),e);
        }

    }

}
