/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.authority.config.AuthorityTypeConfiguration;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.authority.AuthorityMetadataValue;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Term;
import org.dspace.utils.DSpace;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class AuthorityValue {

    public static final String LABELPREFIX = "meta_";

    public static final String ALTERNATE_LABEL = "alternate_label";

    /**
     * The id of the record in solr
     */
    private String id;

    /**
     * The metadata field that this authority value is for
     */
    private String field;

    /**
     * The text value of this authority
     */
    private String value;

    /**
     * When this authority record has been created
     */
    private Date creationDate;

    /**
     * If this authority has been removed
     */
    private boolean deleted;

    /**
     * represents the last time that DSpace got updated information from its external source
     */
    private Date lastModified;

    private String fullText;

    /** Alternate Terms */
    private List<String> nameVariants = new ArrayList<String>();


    private Map<String, List<String>> otherMetadata = new HashMap<String, List<String>>();

    private String source = "LOCAL";

    public AuthorityValue() {
    }

    public AuthorityValue(SolrDocument document) {
        setValues(document);
    }

    public String getId() {
        return id;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }

    public String getFullText() {
        return fullText == null ? value : fullText;
    }

    public void setFullText(String fullText) {
        this.fullText = fullText;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setField(String field) {
        this.field = field;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = stringToDate(creationDate);
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = stringToDate(lastModified);
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    protected void updateLastModifiedDate() {
        this.lastModified = new Date();
    }

    public void update() {
        updateLastModifiedDate();
    }

    public void delete() {
        setDeleted(true);
        updateLastModifiedDate();
    }


    public List<String> getNameVariants() {
        return nameVariants;
    }

    public void addNameVariant(String name) {
        if (StringUtils.isNotBlank(name)) {
            nameVariants.add(name);
        }
    }


    protected void clearNameVariants() {
        nameVariants.clear();
    }


    public Map<String, List<String>> getOtherMetadata() {
        return otherMetadata;
    }

    public void addOtherMetadata(String label, String data) {
        List<String> strings = otherMetadata.get(label);
        if (strings == null) {
            strings = new ArrayList<String>();
        }
        strings.add(data);
        otherMetadata.put(label, strings);
    }

    public void clearOtherMetadata()
    {
        otherMetadata.clear();
    }

    /**
     * Generate a solr record from this instance
     */
    public SolrInputDocument getSolrInputDocument() {

        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("id", getId());
        doc.addField("field", getField());
        doc.addField("value", getValue());
        doc.addField("display-value", getValue());
        doc.addField("full-text", getFullText());
        doc.addField("source", getSource());
        doc.addField("deleted", isDeleted());
        doc.addField("creation_date", dateToString(getCreationDate()));
        doc.addField("last_modified_date", dateToString(getLastModified()));
        doc.addField("authority_type", getAuthorityType());
        for(String name:nameVariants)
        {
            doc.addField(ALTERNATE_LABEL,name);
        }
        for(String key:otherMetadata.keySet())
        {
            for(String v:otherMetadata.get(key))
            {
                doc.addField(LABELPREFIX+key,v);
            }

        }
        return doc;
    }

    /**
     * Initialize this instance based on a solr record
     */
    public void setValues(SolrDocument document) {
        this.id = String.valueOf(document.getFieldValue("id"));
        this.field = String.valueOf(document.getFieldValue("field"));
        this.value = String.valueOf(document.getFieldValue("value"));
        if(document.getFieldValue("deleted")!=null)
            this.deleted = (Boolean) document.getFieldValue("deleted");
        this.creationDate = (Date) document.getFieldValue("creation_date");
        this.lastModified = (Date) document.getFieldValue("last_modified_date");
        this.fullText = String.valueOf(document.getFieldValue("full-text"));

        clearNameVariants();

        Collection<Object> document_name_variant = document.getFieldValues(ALTERNATE_LABEL);
        if (document_name_variant != null) {
            for (Object name_variants : document_name_variant) {
                addNameVariant(name_variants.toString());
            }
        }

        clearOtherMetadata();

        for (String fieldName : document.getFieldNames()) {
            String labelPrefix = LABELPREFIX;
            if (fieldName.startsWith(labelPrefix)) {
                String label = fieldName.substring(labelPrefix.length()).replace("_", ".");
                List<String> list = new ArrayList<String>();
                Collection<Object> fieldValues = document.getFieldValues(fieldName);
                for (Object o : fieldValues) {
                    list.add(o.toString());
                }
                otherMetadata.put(label, list);
            }
        }
    }

    /**
     * Replace an item's DCValue with this authority
     */
    public void updateItem(Item currentItem, DCValue value) {
        DCValue newValue = value.copy();
        newValue.value = getValue();
        newValue.authority = getId();
        currentItem.replaceMetadataValue(value,newValue);
    }

    /**
     * Information that can be used the choice ui
     */
    public Map<String, String> choiceSelectMap() {
        return getChoiceSelectMap("Internal");
    }

    protected Map<String, String> getChoiceSelectMap(String type)
    {
        HashMap<String,String> map =  new HashMap<String, String>();

        //todo:add choiceSelectFields
        AuthorityTypeConfiguration config = getAuthorityTypes().getConfigForType(type);

        if(config != null  && config.getChoiceSelectFields() != null)
        {
            Map<String, String> choiceSelectFields = config.getChoiceSelectFields();
            for(Object key:choiceSelectFields.keySet())
            {
                String keyValue = (String)key;
                String metadataField = (String) choiceSelectFields.get(key);
                String metadataValue = null;
                if(this.getOtherMetadata().get(metadataField)!=null&&this.getOtherMetadata().get(metadataField).size()>0)
                {
                    ArrayList<String> metadataValues = (ArrayList<String>) this.getOtherMetadata().get(metadataField);
                    metadataValue = metadataValues.get(0);
                }
                if (metadataValue!=null) {
                    map.put(keyValue,metadataValue );
                } else {
                    map.put(keyValue, "/");
                }
            }
        }
        return map;
    }


    public List<DateTimeFormatter> getDateFormatters() {
        List<DateTimeFormatter> list = new ArrayList<DateTimeFormatter>();
        list.add(ISODateTimeFormat.dateTime());
        list.add(ISODateTimeFormat.dateTimeNoMillis());
        return list;
    }

    public String dateToString(Date date) {
        String string = null;
        if (date != null) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            string = format.format(date);
        }
        return string;
    }

    public Date stringToDate(String date) {
        Date result = null;
        if (StringUtils.isNotBlank(date)) {
            List<DateTimeFormatter> dateFormatters = getDateFormatters();
            boolean converted = false;
            int formatter = 0;
            while(!converted) {
                try {
                    DateTimeFormatter dateTimeFormatter = dateFormatters.get(formatter);
                    DateTime dateTime = dateTimeFormatter.parseDateTime(date);
                    result = dateTime.toDate();
                    converted = true;
                } catch (IllegalArgumentException e) {
                    formatter++;
                    if (formatter > dateFormatters.size()) {
                        converted = true;
                    }
                    log.error("Could not find a valid date format for: \""+date+"\"", e);
                }
            }
        }
        return result;
    }

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(AuthorityValue.class);

    @Override
    public String toString() {
        return "AuthorityValue{" +
                "id='" + id + '\'' +
                ", field='" + field + '\'' +
                ", value='" + value + '\'' +
                ", creationDate=" + creationDate +
                ", deleted=" + deleted +
                ", lastModified=" + lastModified +
                ", nameVariants=" + nameVariants +
                ", otherMetadata=" + otherMetadata +
                '}';
    }

    /**
     * Provides a string that will be allow a this AuthorityType to be recognized and provides information to create a new instance to be created using public AuthorityValue newInstance(String info).
     * See the implementation of com.atmire.org.dspace.authority.AuthorityValueGenerator#generateRaw(java.lang.String, java.lang.String) for more precisions.
     */
    public String generateString() {
        return AuthorityValueGenerator.GENERATE;
    }

    /**
     * Makes an instance of the AuthorityValue with the given information.
     */
    public AuthorityValue newInstance(String info) {
        return new AuthorityValue();
    }

    private String authorityType = AuthorityMetadataValue.generaltype;

    public String getAuthorityType() {
        return  authorityType;
    }

    private static AuthorityTypes authorityTypes;
    public static AuthorityTypes getAuthorityTypes() {
        if (authorityTypes == null) {
            authorityTypes = new DSpace().getServiceManager().getServiceByName("AuthorityTypes", AuthorityTypes.class);
        }
        return authorityTypes;
    }

    public static AuthorityValue fromSolr(SolrDocument solrDocument) {
        String type = (String) solrDocument.getFieldValue("authority_type");
        AuthorityValue value = getAuthorityTypes().getEmptyAuthorityValue(type);
        value.setValues(solrDocument);
        return value;
    }


    public static AuthorityValue fromConcept(Concept concept) throws SQLException {
        AuthorityValue value = getAuthorityTypes().getEmptyAuthorityValue(concept.getSource());
        value.setValues(concept);
        return value;
    }

    protected void setValues(Concept concept) throws SQLException {

        this.setId(concept.getIdentifier());
        this.setCreationDate(concept.getCreated());
        this.setLastModified(concept.getLastModified());
        this.setDeleted(false);
        this.setValue(concept.getPreferredLabel());

        // Will be the same as hardcoded value in ORCIDAuthorityValue and PersonAuthorityValue
        this.setAuthorityType(concept.getSource());

        // TODO: Name Variants : Set all terms as full text for search and term completion.
        String fullText = "";
        for(Term term : concept.getTerms() )
        {
            fullText = fullText +" , "+ term.getLiteralForm();
        }
        this.setFullText(fullText);

        if(concept.getScheme()!=null)
            this.setField(concept.getScheme().getIdentifier().replace(".", "_"));


        clearOtherMetadata();

        ArrayList<AuthorityMetadataValue> authorityMetadataValues = concept.getMetadata();
        for(AuthorityMetadataValue authorityMetadataValue : authorityMetadataValues)
        {
            String key = authorityMetadataValue.schema+"_"+authorityMetadataValue.element;
            if(authorityMetadataValue.qualifier!=null)
                key=key+"_"+authorityMetadataValue.qualifier;
            this.addOtherMetadata(key, authorityMetadataValue.getValue());
        }
    }

    public void updateConceptFromAuthorityValue(Concept concept) throws SQLException,AuthorizeException{

        for(String name : otherMetadata.keySet())
        {
            if(name.startsWith(LABELPREFIX)){
                String key = name.replace(LABELPREFIX,"");
                String[] keys = key.split("_");
                String schema = keys[0];
                String element = keys[1];
                String qualifier = null;
                if(keys.length>2)
                 qualifier = keys[2];
                for(String value : otherMetadata.get(name))
                {
                    // Add it to the list
                    concept.addMetadata(schema,element,qualifier,"",value,null,-1);
                }

            }
        }
        for(String name : nameVariants)
        {
            //add alternate terms
            Term term = concept.createTerm(name,Term.alternate_term);
            term.update();
        }
    }

    protected void setAuthorityType(String source) {
        this.authorityType = source;
    }

    /**
     * The regular equals() only checks if both AuthorityValues describe the same authority.
     * This method checks if the AuthorityValues have different information
     * E.g. it is used to decide when lastModified should be updated.
     */
    public boolean hasTheSameInformationAs(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AuthorityValue that = (AuthorityValue) o;

        if (deleted != that.deleted) {
            return false;
        }
        if (field != null ? !field.equals(that.field) : that.field != null) {
            return false;
        }
        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (value != null ? !value.equals(that.value) : that.value != null) {
            return false;
        }

        if (nameVariants != null ? !nameVariants.equals(that.nameVariants) : that.nameVariants != null) {
            return false;
        }

        for (String key : getOtherMetadata().keySet()) {
            if(getOtherMetadata().get(key) != null){
                List<String> metadata = getOtherMetadata().get(key);
                List<String> otherMetadata = that.getOtherMetadata().get(key);
                if (otherMetadata == null) {
                    return false;
                } else {
                    HashSet<String> metadataSet = new HashSet<String>(metadata);
                    HashSet<String> otherMetadataSet = new HashSet<String>(otherMetadata);
                    if (!metadataSet.equals(otherMetadataSet)) {
                        return false;
                    }
                }
            }else{
                if(that.getOtherMetadata().get(key) != null){
                    return false;
                }
            }
        }


        return true;
    }


    public void setSource(String source) {
        this.source = source;
    }

    public String getSource() {
        return source;
    }

}
