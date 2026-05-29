/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.audit.factory;

import org.dspace.app.audit.AuditService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for auditing, use AuditServiceFactory.getInstance() to
 * retrieve an implementation
 *
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 */
public class AuditServiceFactoryImpl extends AuditServiceFactory {

    @Autowired(required = true)
    private AuditService auditService;

    @Override
    public AuditService getAuditService() {
        return auditService;
    }
}
