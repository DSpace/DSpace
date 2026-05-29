/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.audit.factory;

import org.dspace.app.audit.AuditService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for Audit, use AuditServiceFactory.getInstance() to retrieve an
 * implementation
 *
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 */
public abstract class AuditServiceFactory {

    public abstract AuditService getAuditService();

    public static AuditServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                .getServiceByName("auditServiceFactory", AuditServiceFactory.class);
    }
}
