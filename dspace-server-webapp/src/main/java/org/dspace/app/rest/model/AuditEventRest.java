/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import org.dspace.app.rest.RestResourceController;

/**
 * The Audit Event REST Resource.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@LinksRest(links = {
    @LinkRest(method = "getEperson", name = AuditEventRest.EPERSON),
    @LinkRest(method = "getObject", name = AuditEventRest.OBJECT),
    @LinkRest(method = "getSubject", name = AuditEventRest.SUBJECT)
})
public class AuditEventRest extends BaseObjectRest<UUID> {
    public static final String NAME = "auditevent";
    public static final String NAME_PLURAL = "auditevents";
    public static final String CATEGORY = RestAddressableModel.SYSTEM;

    public static final String EPERSON = "eperson";
    public static final String SUBJECT = "subject";
    public static final String OBJECT = "object";

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

    @Override
    @JsonProperty(access = Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return NAME_PLURAL;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    public UUID getEpersonUUID() {
        return epersonUUID;
    }

    public void setEpersonUUID(UUID epersonUUID) {
        this.epersonUUID = epersonUUID;
    }

    public UUID getObjectUUID() {
        return objectUUID;
    }

    public void setObjectUUID(UUID objectUUID) {
        this.objectUUID = objectUUID;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public UUID getSubjectUUID() {
        return subjectUUID;
    }

    public void setSubjectUUID(UUID subjectUUID) {
        this.subjectUUID = subjectUUID;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getMetadataField() {
        return metadataField;
    }

    public void setMetadataField(String metadataField) {
        this.metadataField = metadataField;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public Integer getConfidence() {
        return confidence;
    }

    public void setConfidence(Integer confidence) {
        this.confidence = confidence;
    }

    public Integer getPlace() {
        return place;
    }

    public void setPlace(Integer place) {
        this.place = place;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
}
