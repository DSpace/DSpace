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
import org.dspace.app.rest.model.DSpaceObjectRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.DSpaceObjectUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "object" subresource of an audit event.
 */
@Component(AuditEventRest.CATEGORY + "." + AuditEventRest.NAME_PLURAL + "." + AuditEventRest.OBJECT)
public class AuditEventObjectLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    @Autowired
    private AuditSolrServiceImpl auditSolrService;

    @Autowired
    private DSpaceObjectUtils dspaceObjectUtil;

    @PreAuthorize("hasAuthority('ADMIN')")
    public DSpaceObjectRest getObject(@Nullable HttpServletRequest request,
                                               UUID auditId,
                                               @Nullable Pageable optionalPageable,
                                               Projection projection) {
        try {
            Context context = obtainContext();
            AuditEvent audit = getAuditEvent(context, auditId);
            UUID objUUID = audit.getObjectUUID();
            DSpaceObject dso = getDSpaceObject(context, objUUID);

            if (dso != null) {
                return (DSpaceObjectRest) converter.toRest(dso, utils.obtainProjection());
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve audit event object for audit ID: " + auditId.toString(), e);
        }
    }

    private AuditEvent getAuditEvent(Context context, UUID auditId) {
        AuditEvent audit = auditSolrService.findEvent(context, auditId);
        if (audit == null) {
            throw new ResourceNotFoundException("No such audit event: " + auditId.toString());
        }
        return audit;
    }

    private DSpaceObject getDSpaceObject(Context context, UUID objUUID) throws SQLException {
        return objUUID != null ? dspaceObjectUtil.findDSpaceObject(context, objUUID) : null;
    }
}
