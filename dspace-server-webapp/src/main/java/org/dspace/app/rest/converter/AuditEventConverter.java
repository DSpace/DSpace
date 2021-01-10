/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.audit.AuditEvent;
import org.dspace.app.rest.model.AuditEventRest;
import org.dspace.app.rest.projection.Projection;
import org.springframework.stereotype.Component;

/**
 * This class provides the method to convert an AuditEvent to its REST representation, the
 * AuditEventRest
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class AuditEventConverter
        implements DSpaceConverter<AuditEvent, AuditEventRest> {

    @Override
    public AuditEventRest convert(AuditEvent audit, Projection projection) {
        AuditEventRest auditRest = new AuditEventRest();
        auditRest.setProjection(projection);
        if (audit != null) {
            auditRest.setId(audit.getUuid());
            auditRest.setEpersonUUID(audit.getEpersonUUID());
            auditRest.setDetail(audit.getDetail());
            auditRest.setEventType(audit.getEventType());
            auditRest.setObjectType(audit.getObjectType());
            auditRest.setObjectUUID(audit.getObjectUUID());
            auditRest.setSubjectType(audit.getSubjectType());
            auditRest.setSubjectUUID(audit.getSubjectUUID());
            auditRest.setTimeStamp(audit.getDatetime());
        }
        return auditRest;
    }

    @Override
    public Class<AuditEvent> getModelClass() {
        return AuditEvent.class;
    }

}
