/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.dspace.identifier.service.IdentifierService;
import org.dspace.utils.DSpace;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.*;

/**
 * Service implementation class for the DSpaceObject.
 * All DSpaceObject service classes should extend this class since it implements some basic methods which all DSpaceObjects
 * are required to have.
 *
 * @author kevinvandevelde at atmire.com
 * @param <T> class type
 */
public abstract class DSpaceObjectServiceImpl<T extends DSpaceObject> implements DSpaceObjectService<T> {

    /** log4j category */
    private static final Logger log = Logger.getLogger(DSpaceObjectServiceImpl.class);

    @Autowired(required = true)
    protected ChoiceAuthorityService choiceAuthorityService;
    @Autowired(required = true)
    protected HandleService handleService;
    @Autowired(required = true)
    protected MetadataValueService metadataValueService;
    @Autowired(required = true)
    protected MetadataFieldService metadataFieldService;
    @Autowired(required = true)
    protected MetadataAuthorityService metadataAuthorityService;

    public DSpaceObjectServiceImpl()
    {

    }

    @Override
    public String getName(T dso) {
        String value = getMetadataFirstValue(dso, MetadataSchema.DC_SCHEMA, "title", null, Item.ANY);
        return value == null ? "" : value;
    }

    @Override
    public ArrayList<String> getIdentifiers(Context context, T dso) {
        ArrayList<String > identifiers = new ArrayList<>();

        IdentifierService identifierService =
                new DSpace().getSingletonService(IdentifierService.class);

        if (identifierService != null)
        {
            identifiers.addAll(identifierService.lookup(context, dso));
        } else {
            log.warn("No IdentifierService found, will return an list containing "
                    + "the Handle only.");
            if (dso.getHandle() != null)
            {
                identifiers.add(handleService.getCanonicalForm(dso.getHandle()));
            }
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

    @Override
    public DSpaceObject getParentObject(Context context, T dso) throws SQLException
    {
        return null;
    }

    @Override
    public DSpaceObject getAdminObject(Context context, T dso, int action) throws SQLException {
        if (action == Constants.ADMIN)
        {
            throw new IllegalArgumentException("Illegal call to the DSpaceObject.getAdminObject method");
        }
        return dso;
    }

    @Override
    public String getTypeText(T dso) {
        return Constants.typeText[dso.getType()];
    }

    @Override
    public List<MetadataValue> getMetadata(T dso, String schema, String element, String qualifier, String lang) {
        // Build up list of matching values
        List<MetadataValue> values = new ArrayList<MetadataValue>();
        for (MetadataValue dcv : dso.getMetadata())
        {
            if (match(schema, element, qualifier, lang, dcv))
            {
                values.add(dcv);
            }
        }

        // Create an array of matching values
        return values;
    }

    @Override
    public List<MetadataValue> getMetadataByMetadataString(T dso, String mdString)
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

        List<MetadataValue> values;
        if (Item.ANY.equals(qualifier))
        {
            values = getMetadata(dso, schema, element, Item.ANY, Item.ANY);
        }
        else if ("".equals(qualifier))
        {
            values = getMetadata(dso, schema, element, null, Item.ANY);
        }
        else
        {
            values = getMetadata(dso, schema, element, qualifier, Item.ANY);
        }

        return values;
    }

    @Override
    public String getMetadata(T dso, String value) {
        List<MetadataValue> metadataValues = getMetadataByMetadataString(dso, value);

        if(CollectionUtils.isNotEmpty(metadataValues)) {
            return metadataValues.iterator().next().getValue();
        }
        return null;
    }

    @Override
    public List<MetadataValue> getMetadata(T dso, String mdString, String authority) {
        String[] elements = getElements(mdString);
        return getMetadata(dso, elements[0], elements[1], elements[2], elements[3], authority);
    }

    @Override
    public List<MetadataValue> getMetadata(T dso, String schema, String element, String qualifier, String lang, String authority){
        List<MetadataValue> metadata = getMetadata(dso, schema, element, qualifier, lang);
        List<MetadataValue> result = new ArrayList<>(metadata);
        if (!authority.equals(Item.ANY)) {
            Iterator<MetadataValue> iterator = result.iterator();
            while (iterator.hasNext()) {
                MetadataValue metadataValue = iterator.next();
                if (!authority.equals(metadataValue.getAuthority())) {
                    iterator.remove();
                }
            }
        }
        return result;
    }

    @Override
    public void addMetadata(Context context, T dso, String schema, String element, String qualifier, String lang, List<String> values) throws SQLException {
        MetadataField metadataField = metadataFieldService.findByElement(context, schema, element, qualifier);
        if (metadataField == null) {
            throw new SQLException("bad_dublin_core schema=" + schema + "." + element + "." + qualifier + ". Metadata field does not exist!");
        }

        addMetadata(context, dso, metadataField, lang, values);
    }

    @Override
    public void addMetadata(Context context, T dso, String schema, String element, String qualifier, String lang, List<String> values, List<String> authorities, List<Integer> confidences) throws SQLException {
        // We will not verify that they are valid entries in the registry
        // until update() is called.
        MetadataField metadataField = metadataFieldService.findByElement(context, schema, element, qualifier);
        if (metadataField == null) {
            throw new SQLException("bad_dublin_core schema=" + schema + "." + element + "." + qualifier + ". Metadata field does not exist!");
        }
        addMetadata(context, dso, metadataField, lang, values, authorities, confidences);
    }

    @Override
    public void addMetadata(Context context, T dso, MetadataField metadataField, String lang, List<String> values, List<String> authorities, List<Integer> confidences) throws SQLException {
        boolean authorityControlled = metadataAuthorityService.isAuthorityControlled(metadataField);
        boolean authorityRequired = metadataAuthorityService.isAuthorityRequired(metadataField);

        // We will not verify that they are valid entries in the registry
        // until update() is called.
        for (int i = 0; i < values.size(); i++)
        {

            MetadataValue metadataValue = metadataValueService.create(context, dso, metadataField);
            metadataValue.setLanguage(lang == null ? null : lang.trim());

            // Logic to set Authority and Confidence:
            //  - normalize an empty string for authority to NULL.
            //  - if authority key is present, use given confidence or NOVALUE if not given
            //  - otherwise, preserve confidence if meaningful value was given since it may document a failed authority lookup
            //  - CF_UNSET signifies no authority nor meaningful confidence.
            //  - it's possible to have empty authority & CF_ACCEPTED if e.g. user deletes authority key
            if (authorityControlled)
            {
                if (authorities != null && authorities.get(i) != null && authorities.get(i).length() > 0)
                {
                    metadataValue.setAuthority(authorities.get(i));
                    metadataValue.setConfidence(confidences == null ? Choices.CF_NOVALUE : confidences.get(i));
                }
                else
                {
                    metadataValue.setAuthority(null);
                    metadataValue.setConfidence(confidences == null ? Choices.CF_UNSET : confidences.get(i));
                }
                // authority sanity check: if authority is required, was it supplied?
                // XXX FIXME? can't throw a "real" exception here without changing all the callers to expect it, so use a runtime exception
                if (authorityRequired && (metadataValue.getAuthority() == null || metadataValue.getAuthority().length() == 0))
                {
                    throw new IllegalArgumentException("The metadata field \"" + metadataField.toString() + "\" requires an authority key but none was provided. Value=\"" + values.get(i) + "\"");
                }
            }
            if (values.get(i) != null)
            {
                // remove control unicode char
                String temp = values.get(i).trim();
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
                metadataValue.setValue(String.valueOf(dcvalue));;
            }
            else
            {
                metadataValue.setValue(null);
            }
            //An update here isn't needed, this is persited upon the merge of the owning object
//            metadataValueService.update(context, metadataValue);
            dso.addDetails(metadataField.toString());
        }
    }

    @Override
    public void addMetadata(Context context, T dso, MetadataField metadataField, String language, String value, String authority, int confidence) throws SQLException {
        addMetadata(context, dso, metadataField, language, Arrays.asList(value), Arrays.asList(authority), Arrays.asList(confidence));
    }

    @Override
    public void addMetadata(Context context, T dso, String schema, String element, String qualifier, String lang, String value) throws SQLException {
        addMetadata(context, dso, schema, element, qualifier, lang, Arrays.asList(value));
    }

    @Override
    public void addMetadata(Context context, T dso, MetadataField metadataField, String language, String value) throws SQLException {
        addMetadata(context, dso, metadataField, language, Arrays.asList(value));
    }

    @Override
    public void addMetadata(Context context, T dso, MetadataField metadataField, String language, List<String> values) throws SQLException {
        if(metadataField != null) {
            String fieldKey = metadataAuthorityService.makeFieldKey(metadataField.getMetadataSchema().getName(), metadataField.getElement(), metadataField.getQualifier());
            if (metadataAuthorityService.isAuthorityControlled(fieldKey)) {
                List<String> authorities = new ArrayList<String>();
                List<Integer> confidences = new ArrayList<Integer>();
                for (int i = 0; i < values.size(); ++i) {
                    if (dso instanceof Item) {
                        getAuthoritiesAndConfidences(fieldKey, ((Item) dso).getOwningCollection(), values, authorities, confidences, i);
                    } else {
                        getAuthoritiesAndConfidences(fieldKey, null, values, authorities, confidences, i);
                    }
                }
                addMetadata(context, dso, metadataField, language, values, authorities, confidences);
            } else {
                addMetadata(context, dso, metadataField, language, values, null, null);
            }
        }
    }

    @Override
    public void addMetadata(Context context, T dso, String schema, String element, String qualifier, String lang, String value, String authority, int confidence) throws SQLException {
        addMetadata(context, dso, schema, element, qualifier, lang, Arrays.asList(value), Arrays.asList(authority), Arrays.asList(confidence));
    }

    @Override
    public void clearMetadata(Context context, T dso, String schema, String element, String qualifier, String lang) throws SQLException {
        Iterator<MetadataValue> metadata = dso.getMetadata().iterator();
        while (metadata.hasNext())
        {
            MetadataValue metadataValue = metadata.next();
            // If this value matches, delete it
            if (match(schema, element, qualifier, lang, metadataValue))
            {
                metadata.remove();
                metadataValueService.delete(context, metadataValue);
            }
        }
        dso.setMetadataModified();
    }

    @Override
    public void removeMetadataValues(Context context, T dso, List<MetadataValue> values) throws SQLException {
        Iterator<MetadataValue> metadata = dso.getMetadata().iterator();
        while (metadata.hasNext()) {
            MetadataValue metadataValue = metadata.next();
            if(values.contains(metadataValue))
            {
                metadata.remove();
                metadataValueService.delete(context, metadataValue);
            }
        }
        dso.setMetadataModified();
    }

    /**
     * Retrieve first metadata field value
     * @param dso
     *            The DSpaceObject which we ask for metadata.
     * @param schema
     *            the schema for the metadata field. <em>Must</em> match
     *            the <code>name</code> of an existing metadata schema.
     * @param element
     *            the element to match, or <code>Item.ANY</code>
     * @param qualifier
     *            the qualifier to match, or <code>Item.ANY</code>
     * @param language
     *            the language to match, or <code>Item.ANY</code>
     * @return the first metadata field value
     */
    @Override
    public String getMetadataFirstValue(T dso, String schema, String element, String qualifier, String language){
        List<MetadataValue> metadataValues = getMetadata(dso, schema, element, qualifier, language);
        if(CollectionUtils.isNotEmpty(metadataValues)){
            return metadataValues.iterator().next().getValue();
        }
        return null;
    }

    /**
     * Set first metadata field value
     * @throws SQLException if database error
     */
    @Override
    public void setMetadataSingleValue(Context context, T dso, String schema, String element, String qualifier, String language, String value) throws SQLException {
        if(value != null)
        {
            clearMetadata(context, dso, schema, element, qualifier, language);
            addMetadata(context, dso, schema, element, qualifier, language, value);
            dso.setMetadataModified();
        }
    }

    /**
     * Utility method for pattern-matching metadata elements.  This
     * method will return <code>true</code> if the given schema,
     * element, qualifier and language match the schema, element,
     * qualifier and language of the <code>DCValue</code> object passed
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
     * @param metadataValue
     *            the Dublin Core value
     * @return <code>true</code> if there is a match
     */
    protected boolean match(String schema, String element, String qualifier,
            String language, MetadataValue metadataValue)
    {

        MetadataField metadataField = metadataValue.getMetadataField();
        MetadataSchema metadataSchema = metadataField.getMetadataSchema();
        // We will attempt to disprove a match - if we can't we have a match
        if (!element.equals(Item.ANY) && !element.equals(metadataField.getElement()))
        {
            // Elements do not match, no wildcard
            return false;
        }

        if (qualifier == null)
        {
            // Value must be unqualified
            if (metadataField.getQualifier() != null)
            {
                // Value is qualified, so no match
                return false;
            }
        }
        else if (!qualifier.equals(Item.ANY))
        {
            // Not a wildcard, so qualifier must match exactly
            if (!qualifier.equals(metadataField.getQualifier()))
            {
                return false;
            }
        }

        if (language == null)
        {
            // Value must be null language to match
            if (metadataValue.getLanguage() != null)
            {
                // Value is qualified, so no match
                return false;
            }
        }
        else if (!language.equals(Item.ANY))
        {
            // Not a wildcard, so language must match exactly
            if (!language.equals(metadataValue.getLanguage()))
            {
                return false;
            }
        }

        if (!schema.equals(Item.ANY))
        {
            if (metadataSchema != null && !metadataSchema.getName().equals(schema))
            {
                // The namespace doesn't match
                return false;
            }
        }

        // If we get this far, we have a match
        return true;
    }

    protected void getAuthoritiesAndConfidences(String fieldKey, Collection collection, List<String> values, List<String> authorities, List<Integer> confidences, int i) {
        Choices c = choiceAuthorityService.getBestMatch(fieldKey, values.get(i), null, null);
        authorities.add(c.values.length > 0 ? c.values[0].authority : null);
        confidences.add(c.confidence);
    }


    /**
     * Splits "schema.element.qualifier.language" into an array.
     * <p>
     * The returned array will always have length greater than or equal to 4
     * <p>
     * Values in the returned array can be empty or null.
     * @param fieldName field name
     * @return array
     */
    protected String[] getElements(String fieldName) {
        String[] tokens = StringUtils.split(fieldName, ".");

        int add = 4 - tokens.length;
        if (add > 0) {
            tokens = (String[]) ArrayUtils.addAll(tokens, new String[add]);
        }

        return tokens;
    }

    /**
     * Splits "schema.element.qualifier.language" into an array.
     * <p>
     * The returned array will always have length greater than or equal to 4
     * <p>
     * When @param fill is true, elements that would be empty or null are replaced by Item.ANY
     * @param fieldName field name
     * @return array
     */
    protected String[] getElementsFilled(String fieldName) {
        String[] elements = getElements(fieldName);
        for (int i = 0; i < elements.length; i++) {
            if (StringUtils.isBlank(elements[i])) {
                elements[i] = Item.ANY;
            }
        }
        return elements;
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

    @Override
    public void update(Context context, T dso) throws SQLException, AuthorizeException
    {
        if(dso.isMetadataModified())
        {
            /*
            Update the order of the metadata values
             */
                // A map created to store the latest place for each metadata field
                Map<MetadataField, Integer> fieldToLastPlace = new HashMap<>();
                List<MetadataValue> metadataValues = dso.getMetadata();
                for (MetadataValue metadataValue : metadataValues)
                {
                    //Retrieve & store the place for each metadata value
                    int mvPlace = getMetadataValuePlace(fieldToLastPlace, metadataValue);
                    metadataValue.setPlace(mvPlace);
                }
        }
    }

    /**
     * Retrieve the place of the metadata value
     * @param fieldToLastPlace the map containing the latest place of each metadata field
     * @param metadataValue the metadata value that needs to get a place
     * @return The new place for the metadata valu
     */
    protected int getMetadataValuePlace(Map<MetadataField, Integer> fieldToLastPlace, MetadataValue metadataValue) {
        MetadataField metadataField = metadataValue.getMetadataField();
        if(fieldToLastPlace.containsKey(metadataField))
        {
            fieldToLastPlace.put(metadataField, fieldToLastPlace.get(metadataField) + 1);
        }else{
            // The metadata value place starts at 0
            fieldToLastPlace.put(metadataField, 0);
        }
        return fieldToLastPlace.get(metadataField);
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
