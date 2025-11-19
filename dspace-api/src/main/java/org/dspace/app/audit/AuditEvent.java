/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.audit;

import java.util.Date;
import java.util.UUID;


/**
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 */
public class AuditEvent {
    private UUID uuid;
    private UUID epersonUUID;
    private UUID objectUUID;
    private String objectType;
    private UUID subjectUUID;
    private String subjectType;
    private String eventType;
    private Date timeStamp;
    private String detail;
    private String metadataField;
    private String value;
    private String authority;
    private Integer confidence;
    private Integer place;
    private String action;
    private String checksum;

    private final static String LOG_FIELD_SEPARATOR = " || ";

    /**
     * @return the uuid of the audit event
    **/
    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * @return the uuid of the eperson who generates event
     */
    public UUID getEpersonUUID() {
        return epersonUUID;
    }

    public void setEpersonUUID(UUID epersonUUID) {
        this.epersonUUID = epersonUUID;
    }

    /**
     * @return the uuid of the object involved in the event
     */
    public UUID getObjectUUID() {
        return objectUUID;
    }

    public void setObjectUUID(UUID objectUUID) {
        this.objectUUID = objectUUID;
    }

    /**
     * @return the type of the object involved in the event
     */
    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    /**
     * @return the uuid of the subject involved in the event
     */
    public UUID getSubjectUUID() {
        return subjectUUID;
    }

    public void setSubjectUUID(UUID subjectUUID) {
        this.subjectUUID = subjectUUID;
    }

    /**
    * @return the type of the subject involved in the event e.g. ITEM, COLLECTION, COMMUNITY
    */
    public String getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    /**
     * @return the type of event e.g. CREATE, MODIFY_METADATA, DELETE
     */
    public String getEventType() {
        return eventType;
    }

    public void setEventType(String changetype) {
        this.eventType = changetype;
    }

    /**
     * @return the date and time of the event
     */
    public Date getDatetime() {
        return timeStamp;
    }

    public void setDatetime(Date datetime) {
        this.timeStamp = datetime;
    }

    /**
     * @return additional detail about the event
     */
    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    /**
     * @return the metadata field involved in the event e.g. dc.contributor.author
     */
    public String getMetadataField() {
        return metadataField;
    }

    public void setMetadataField(String metadataField) {
        this.metadataField = metadataField;
    }

    /**
     * @return the metadata value involved in the event e.g. "Smith, John"
     */
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @return the authority key of the metadata value involved in the event
     */
    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    /**
     * @return the confidence level of the metadata value involved in the event
     */
    public Integer getConfidence() {
        return confidence;
    }

    public void setConfidence(Integer confidence) {
        this.confidence = confidence;
    }

    /**
     * @return the place of the metadata value involved in the event e.g. 0
     */
    public Integer getPlace() {
        return place;
    }

    public void setPlace(Integer place) {
        this.place = place;
    }

    /**
     * @return the action performed on the metadata value involved in the event e.g. ADD, REMOVE ...
     */
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    /**
     * @return the checksum of the bitstream involved in the event
     */
    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AUDIT_EVENT")
                .append(LOG_FIELD_SEPARATOR).append("uuid=").append(nullSafe(getUuid()))
                .append(LOG_FIELD_SEPARATOR).append("eventType=").append(nullSafe(getEventType()))
                .append(LOG_FIELD_SEPARATOR).append("subjectUUID=").append(nullSafe(getSubjectUUID()))
                .append(LOG_FIELD_SEPARATOR).append("subjectType=").append(nullSafe(getSubjectType()))
                .append(LOG_FIELD_SEPARATOR).append("objectUUID=").append(nullSafe(getObjectUUID()))
                .append(LOG_FIELD_SEPARATOR).append("objectType=").append(nullSafe(getObjectType()))
                .append(LOG_FIELD_SEPARATOR).append("metadataField=").append(nullSafe(getMetadataField()))
                .append(LOG_FIELD_SEPARATOR).append("value=").append(nullSafe(getValue()))
                .append(LOG_FIELD_SEPARATOR).append("authority=").append(nullSafe(getAuthority()))
                .append(LOG_FIELD_SEPARATOR).append("confidence=").append(nullSafe(getConfidence()))
                .append(LOG_FIELD_SEPARATOR).append("place=").append(nullSafe(getPlace()))
                .append(LOG_FIELD_SEPARATOR).append("action=").append(nullSafe(getAction()))
                .append(LOG_FIELD_SEPARATOR).append("checksum=").append(nullSafe(getChecksum()))
                .append(LOG_FIELD_SEPARATOR).append("detail=").append(nullSafe(getDetail()))
                .append(LOG_FIELD_SEPARATOR).append("datetime=").append(getDatetime() == null ?
                        "null" : String.valueOf(getDatetime().getTime()))
                .append(LOG_FIELD_SEPARATOR).append("epersonUUID=").append(nullSafe(getEpersonUUID()));
        return sb.toString();
    }

    /**
     * Utility to avoid NPEs in log lines.
     */
    private String nullSafe(Object o) {
        return o == null ? "" : o.toString();
    }
}
