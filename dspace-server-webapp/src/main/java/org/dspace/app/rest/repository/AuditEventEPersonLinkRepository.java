/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.UUID;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.audit.AuditEvent;
import org.dspace.app.audit.AuditSolrServiceImpl;
import org.dspace.app.rest.model.AuditEventRest;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "eperson" subresource of an audit event.
 */
@Component(AuditEventRest.CATEGORY + "." + AuditEventRest.NAME_PLURAL + "." + AuditEventRest.EPERSON)
public class AuditEventEPersonLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    @Autowired
    private AuditSolrServiceImpl auditSolrService;

    @Autowired
    private EPersonService epersonService;

    @PreAuthorize("hasAuthority('ADMIN')")
    public EPersonRest getEperson(@Nullable HttpServletRequest request,
                                  UUID auditId,
                                  @Nullable Pageable optionalPageable,
                                  Projection projection) {
        try {
            Context context = obtainContext();
            AuditEvent audit = auditSolrService.findEvent(context, auditId);
            if (audit == null) {
                throw new ResourceNotFoundException("No such audit event: " + auditId.toString());
            }
            UUID objUUID = audit.getEpersonUUID();
            EPerson eperson  = null;
            if (objUUID != null) {
                eperson = epersonService.find(context, objUUID);
            }
            if (eperson != null) {
                return converter.toRest(eperson, utils.obtainProjection());
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
