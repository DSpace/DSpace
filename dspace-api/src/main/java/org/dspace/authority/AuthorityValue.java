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
import org.dspace.content.Item;
import org.dspace.utils.DSpace;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.*;
import org.dspace.content.Metadatum;

/**
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class AuthorityValue {


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

    /**
     * Generate a solr record from this instance
     */
    public SolrInputDocument getSolrInputDocument() {

        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("id", getId());
        doc.addField("field", getField());
        doc.addField("value", getValue());
        doc.addField("deleted", isDeleted());
        doc.addField("creation_date", getCreationDate());
        doc.addField("last_modified_date", getLastModified());
        doc.addField("authority_type", getAuthorityType());
        return doc;
    }

    /**
     * Initialize this instance based on a solr record
     */
    public void setValues(SolrDocument document) {
        this.id = String.valueOf(document.getFieldValue("id"));
        this.field = String.valueOf(document.getFieldValue("field"));
        this.value = String.valueOf(document.getFieldValue("value"));
        this.deleted = (Boolean) document.getFieldValue("deleted");
        this.creationDate = (Date) document.getFieldValue("creation_date");
        this.lastModified = (Date) document.getFieldValue("last_modified_date");
    }

    /**
     * Replace an item's DCValue with this authority
     */
    public void updateItem(Item currentItem, Metadatum value) {
        Metadatum newValue = value.copy();
        newValue.value = getValue();
        newValue.authority = getId();
        currentItem.replaceMetadataValue(value,newValue);
    }

    /**
     * Information that can be used the choice ui
     */
    public Map<String, String> choiceSelectMap() {
        return new HashMap<String, String>();
    }


    public List<DateTimeFormatter> getDateFormatters() {
        List<DateTimeFormatter> list = new ArrayList<DateTimeFormatter>();
        list.add(ISODateTimeFormat.dateTime());
        list.add(ISODateTimeFormat.dateTimeNoMillis());
        return list;
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

    public String getAuthorityType() {
        return "internal";
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

        return true;
    }
}
