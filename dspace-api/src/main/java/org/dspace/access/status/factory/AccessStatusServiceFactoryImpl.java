/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.access.status.factory;

import org.dspace.access.status.service.AccessStatusService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the access status package,
 * use AccessStatusServiceFactory.getInstance() to retrieve an implementation.
 */
public class AccessStatusServiceFactoryImpl extends AccessStatusServiceFactory {

    @Autowired(required = true)
    private AccessStatusService accessStatusService;

    @Override
    public AccessStatusService getAccessStatusService() {
        return accessStatusService;
    }
}
