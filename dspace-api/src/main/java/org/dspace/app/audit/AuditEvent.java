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
 * @author Andrea Petrucci (andrea.petrucci at 4science.it)
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

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
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

    public void setEventType(String changetype) {
        this.eventType = changetype;
    }

    public Date getDatetime() {
        return timeStamp;
    }

    public void setDatetime(Date datetime) {
        this.timeStamp = datetime;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

}
