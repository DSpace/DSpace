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
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;

import java.lang.reflect.Array;
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

    public static String TABLE = "no_table";

    /** log4j logger */
    private static Logger log = Logger.getLogger(AuthorityObject.class);

    /** The row in the table representing this object */
    protected TableRow myRow;

    AuthorityObject(Context context, TableRow row) throws SQLException
    {
        myRow = row;
        DatabaseManager.update(context, myRow);
        clearDetails();
    }

    public String getIdentifier()
    {
        return myRow.getStringColumn("identifier");

    }

    protected void setIdentifier(Context context, String identifier) throws SQLException
    {
        myRow.setColumn("identifier", identifier);
        DatabaseManager.update(context, myRow);
    }

    public Date getCreated()
    {
        Date myDate = myRow.getDateColumn("created");

        if (myDate == null)
        {
            myDate = new Date();
        }

        return myDate;
    }
    public void setCreated(Context context, Date date) throws SQLException
    {
        Date myDate = myRow.getDateColumn("created");

        if (date != null)
        {
            myRow.setColumn("created", date);
            DatabaseManager.update(context, myRow);
        }
    }

    public String getStatus()
    {
        return myRow.getStringColumn("status");

    }

    public void setStatus(Context context, String status) throws SQLException
    {
        if(status == null)
        {
            if(getStatus() != null)
            {
                myRow.setColumnNull("status");
                DatabaseManager.update(context, myRow);
            }
        }
        else if (!status.equals(getStatus()))
        {
            myRow.setColumn("status", status);
            DatabaseManager.update(context, myRow);
        }
    }

    public String getSource()
    {
        return myRow.getStringColumn("source");

    }
    public void setSource(Context context, String source) throws SQLException
    {
        myRow.setColumn("source", source);
        DatabaseManager.update(context, myRow);
    }

    public String getLang()
    {
        return myRow.getStringColumn("lang");

    }
    public void setLang(Context context, String lang) throws SQLException
    {
        myRow.setColumn("lang", lang);
        DatabaseManager.update(context, myRow);
    }

    public Boolean getTopConcept()
    {
        return myRow.getBooleanColumn("topConcept");

    }
    public void setTopConcept(Context context, Boolean topConcept) throws SQLException
    {
        myRow.setColumn("topConcept", topConcept);
        DatabaseManager.update(context, myRow);
    }
    protected Context getContext() {
        Context context = null;
        try {
            context = new Context();
        } catch (SQLException ex) {
            log.error("Unable to instantiate DSpace context", ex);
        }
        return context;
    }

    protected void completeContext(Context context) throws SQLException {
        try {
            context.complete();
        } catch (SQLException ex) {
            // Abort the context to force a new connection
            abortContext(context);
            throw ex;
        }
    }

    protected void abortContext(Context context) {
        if (context != null) {
            log.error("aborting context");
            context.abort();
        }
    }

    public Date getLastModified()
    {
        Date myDate = myRow.getDateColumn("modified");

        if (myDate == null)
        {
            myDate = new Date();
        }

        return myDate;
    }
    public void setLastModified(Context context, Date date) throws SQLException
    {
        Date myDate = myRow.getDateColumn("modified");

        if (date != null)
        {
            myRow.setColumn("modified", date);
            DatabaseManager.update(context, myRow);
        }
    }

    public void updateLastModified() {
        Context context = getContext();
        try {
            Date lastModified = new java.sql.Timestamp(new Date().getTime());
            myRow.setColumn("modified", lastModified);
            DatabaseManager.updateQuery(context, "UPDATE " + getType() + " SET modified = ? WHERE id= ? ", lastModified, getID());
            completeContext(context);
        } catch (SQLException e) {
            abortContext(context);
            log.error("Error while updating modified timestamp" + getType()+": " + getID());
        }
    }

    /**
     * Used to Identify the Table that Metadata Is stored in for this Authority Object.
     * @return
     */
    public abstract String getMetadataTable();

    public ArrayList<AuthorityMetadataValue> getMetadata(Context context, String schemaName, String element, String qualifier) {
        ArrayList<AuthorityMetadataValue> metadataValues;
        metadataValues = new ArrayList<AuthorityMetadataValue>();

        try {
            MetadataField field = null;
            if (Item.ANY.equals(schemaName)) {
                if (Item.ANY.equals(element)) {
                    // return all metadata for this item
                    return getMetadata();
                }
                MetadataSchema[] schemas = MetadataSchema.findAll(context);
                for (int i=0; i<schemas.length; i++) {
                    field = MetadataField.findByElement(context, schemas[i].getName(), element, qualifier);
                    if (field != null) {
                        break;
                    }
                }
            } else {
                field = MetadataField.findByElement(context, schemaName, element, qualifier);
            }
            if (field == null) {
                throw new SQLException("no such field " + schemaName + "." + element + "." + qualifier);
            }
            TableRowIterator tri = DatabaseManager.queryTable(context, this.getMetadataTable() ,
                        "SELECT * FROM " + this.getMetadataTable() + " WHERE parent_id= ? AND field_id= ?",
                        getID(), field.getFieldID());

            if (tri != null) {
                try {
                    while (tri.hasNext()) {
                        TableRow resultRow = tri.next();
                        AuthorityMetadataValue amv = new AuthorityMetadataValue(context, resultRow);
                        metadataValues.add(amv);
                    }
                } finally {
                    // close the TableRowIterator to free up resources
                    if (tri != null) {
                        tri.close();
                    }
                }
            }
        } catch (SQLException e) {
            log.error("cannot load metadata: " + e.getMessage());
        }

        return metadataValues;
    }

    public ArrayList<AuthorityMetadataValue> getMetadata() {
        ArrayList<AuthorityMetadataValue> metadataValues;
        metadataValues = new ArrayList<AuthorityMetadataValue>();
        Context context = getContext();
        try {
            TableRowIterator tri = DatabaseManager.queryTable(context, this.getMetadataTable() ,
                    "SELECT * FROM " + this.getMetadataTable() + " WHERE parent_id= ? ORDER BY field_id",
                    getID());

            if (tri != null) {
                try {
                    while (tri.hasNext()) {
                        TableRow resultRow = tri.next();
                        AuthorityMetadataValue amv = new AuthorityMetadataValue(context, resultRow);
                        metadataValues.add(amv);
                    }
                } finally {
                    // close the TableRowIterator to free up resources
                    if (tri != null) {
                        tri.close();
                    }
                }
            }
            completeContext(context);
        } catch (SQLException e) {
            log.error("cannot load metadata: " + e.getMessage());
            abortContext(context);
        }

        return metadataValues;
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
    public AuthorityMetadataValue[] getMetadata(String schema, String element, String qualifier, String lang) {
        Context context = getContext();
        try {
            // Build up list of matching values
            List<AuthorityMetadataValue> values = new ArrayList<AuthorityMetadataValue>();
            ArrayList<AuthorityMetadataValue> amvList = getMetadata(context, schema, element, qualifier);
            for (AuthorityMetadataValue amv : amvList) {
                if (match(schema, element, qualifier, lang, amv)) {
                    values.add(amv);
                }
            }

            // Create an array of matching values
            AuthorityMetadataValue[] valueArray = new AuthorityMetadataValue[values.size()];
            valueArray = (AuthorityMetadataValue[]) values.toArray(valueArray);
            completeContext(context);
            return valueArray;
        } catch (SQLException e) {
            abortContext(context);
        }
        return new AuthorityMetadataValue[0];
    }

    /**
     * Retrieve metadata field values from a given metadata string
     * of the form <schema prefix>.<element>[.<qualifier>|.*]
     *
     * (this is modified from Item.java)
     *
     * @param mdString
     *            The metadata string of the form
     *            <schema prefix>.<element>[.<qualifier>|.*]
     */
    public AuthorityMetadataValue[] getMetadata(String mdString) {
        StringTokenizer dcf = new StringTokenizer(mdString, ".");

        String[] tokens = { "", "", "" };
        int i = 0;
        while(dcf.hasMoreTokens()) {
            tokens[i] = dcf.nextToken().trim();
            i++;
        }
        String schema = tokens[0];
        String element = tokens[1];
        String qualifier = tokens[2];

        AuthorityMetadataValue[] values;
        if ("*".equals(qualifier)) {
            qualifier = Item.ANY;
        } else if ("".equals(qualifier)) {
            qualifier = null;
        }
        values = getMetadata(schema, element, qualifier, Item.ANY);
        return values;
    }

    public void clearMetadata(Context context, String mdString) {
        StringTokenizer dcf = new StringTokenizer(mdString, ".");

        String[] tokens = { "", "", "" };
        int i = 0;
        while(dcf.hasMoreTokens()) {
            tokens[i] = dcf.nextToken().trim();
            i++;
        }
        String schema = tokens[0];
        String element = tokens[1];
        String qualifier = tokens[2];

        AuthorityMetadataValue[] values;
        if ("*".equals(qualifier)) {
            qualifier = Item.ANY;
        } else if ("".equals(qualifier)) {
            qualifier = null;
        }
        clearMetadata(context, schema, element, qualifier, Item.ANY);
    }

    public AuthorityMetadataValue getSingleMetadata(String mdString) {
        AuthorityMetadataValue[] vals = getMetadata(mdString);
        if (vals != null && vals.length > 0) {
            return vals[0];
        }
        return null;
    }

    public void clearMetadata(Context context, String schema, String element, String qualifier, String lang) {
        try {
            ArrayList<AuthorityMetadataValue> amvList = getMetadata(context, schema, element, qualifier);
            for (AuthorityMetadataValue amv : amvList) {
                DatabaseManager.updateQuery(context, "DELETE FROM " + getMetadataTable() + " WHERE id= ? ", amv.getValueId());
            }
            context.commit();
        } catch (SQLException e) {
            log.error("couldn't clear metadata for " + getID() + ": " + schema + "." + element + "." + qualifier + ", lang=" + lang);
        }
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

    public void addMetadata(Context context, String schema, String element, String qualifier, String lang,
                            String[] values, String authorities[], int confidences[]) {
        MetadataAuthorityManager mam = MetadataAuthorityManager.getManager();
        boolean authorityControlled = mam.isAuthorityControlled(schema, element, qualifier);
        boolean authorityRequired = mam.isAuthorityRequired(schema, element, qualifier);
        String fieldName = schema+"."+element+((qualifier==null)? "": "."+qualifier);
        int fieldID = -1;
        ArrayList<AuthorityMetadataValue> metadataValues = new ArrayList<AuthorityMetadataValue>();
        try {
            // verify that this is a valid schema.element.qualifier:
            MetadataField field = MetadataField.findByElement(context, schema, element, qualifier);
            if (field == null) {
                throw new SQLException("bad_dublin_core " + "schema=" + schema + ", " + element + " " + qualifier);
            }
            fieldID = field.getFieldID();

            for (int i = 0; i < values.length; i++) {
                AuthorityMetadataValue amv = new AuthorityMetadataValue(this.getMetadataTable());
                amv.schema = schema;
                amv.element = element;
                amv.qualifier = qualifier;
                amv.language = (lang == null ? null : lang.trim());

                // Logic to set Authority and Confidence:
                //  - normalize an empty string for authority to NULL.
                //  - if authority key is present, use given confidence or NOVALUE if not given
                //  - otherwise, preserve confidence if meaningful value was given since it may document a failed authority lookup
                //  - CF_UNSET signifies no authority nor meaningful confidence.
                //  - it's possible to have empty authority & CF_ACCEPTED if e.g. user deletes authority key
                if (authorityControlled) {
                    if (authorities != null && authorities[i] != null && authorities[i].length() > 0) {
                        amv.authority = authorities[i];
                        amv.confidence = confidences == null ? Choices.CF_NOVALUE : confidences[i];
                    } else {
                        amv.authority = null;
                        amv.confidence = confidences == null ? Choices.CF_UNSET : confidences[i];
                    }
                    // authority sanity check: if authority is required, was it supplied?
                    // XXX FIXME? can't throw a "real" exception here without changing all the callers to expect it, so use a runtime exception
                    if (authorityRequired && (amv.authority == null || amv.authority.length() == 0)) {
                        throw new IllegalArgumentException("The metadata field \"" + fieldName + "\" requires an authority key but none was provided. Vaue=\"" + amv.value + "\"");
                    }
                }
                if (values[i] != null) {
                    // remove control unicode char
                    String temp = values[i].trim();
                    char[] dcvalue = temp.toCharArray();
                    for (int charPos = 0; charPos < dcvalue.length; charPos++) {
                        if (Character.isISOControl(dcvalue[charPos]) &&
                                !String.valueOf(dcvalue[charPos]).equals("\u0009") &&
                                !String.valueOf(dcvalue[charPos]).equals("\n") &&
                                !String.valueOf(dcvalue[charPos]).equals("\r")) {
                            dcvalue[charPos] = ' ';
                        }
                    }
                    amv.value = String.valueOf(dcvalue);
                } else {
                    amv.value = null;
                }
                metadataValues.add(amv);
                addDetails(fieldName);
            }
            // add the new values into the metadataTable, starting with the last place.
            ArrayList<AuthorityMetadataValue> amvList = getMetadata(context, schema, element, qualifier);
            int placeNum = amvList.size() + 1;
            for (AuthorityMetadataValue amv : metadataValues) {
                AuthorityMetadataValue metadata = new AuthorityMetadataValue(this.getMetadataTable());
                metadata.setParentId(getID());
                metadata.setFieldId(fieldID);
                metadata.setValue(amv.value);
                metadata.setLanguage(amv.language);
                metadata.setPlace(placeNum++);
                metadata.setAuthority(amv.authority);
                metadata.setConfidence(amv.confidence);
                metadata.create(context);
            }
        } catch (SQLException e) {
            log.error("couldn't add metadata for " + getID() + ": " + schema + "." + element + "." + qualifier + ", lang=" + lang);
        }
    }

    public void addMetadata(Context context, String mdString, String value) {
        StringTokenizer dcf = new StringTokenizer(mdString, ".");

        String[] tokens = { "", "", "" };
        int i = 0;
        while(dcf.hasMoreTokens()) {
            tokens[i] = dcf.nextToken().trim();
            i++;
        }
        String schema = tokens[0];
        String element = tokens[1];
        String qualifier = tokens[2];

        AuthorityMetadataValue[] values;
        if ("*".equals(qualifier)) {
            qualifier = Item.ANY;
        } else if ("".equals(qualifier)) {
            qualifier = null;
        }
        addMetadata(context, schema, element, qualifier, "en", value, null, -1);
    }

    public void addMetadata(Context context, String schema, String element, String qualifier,
                            String lang, String value, String authority, int confidence) {
        String[] valArray = new String[1];
        String[] authArray = new String[1];
        int[] confArray = new int[1];
        valArray[0] = value;
        authArray[0] = authority;
        confArray[0] = confidence;

        addMetadata(context, schema, element, qualifier, lang, valArray, authArray, confArray);
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

    /**
     * Delete a generic AuthorityObject
     *
     */
    public void delete(Context context) throws SQLException, AuthorizeException {
        // authorized?
        if (!AuthorizeManager.isAdmin(context)) {
            throw new AuthorizeException("You must be an admin to modify a Concept's Parent Concept");
        }

        // Remove from cache
        context.removeCached(this, getID());
        // Remove all metadata
        DatabaseManager.updateQuery(context, "DELETE FROM " + getMetadataTable() + " WHERE parent_id= ? ", getID());
        //delete object-specific database entries
        deleteAssociatedData(context);
        //delete concept
        DatabaseManager.delete(context, myRow);

        context.commit();
        log.info(LogManager.getHeader(context, "delete_" + TABLE , "id=" + getID()));
    }

    protected void deleteAssociatedData(Context context) throws SQLException, AuthorizeException {
        return;
    }

}
