/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle.factory;

import org.dspace.handle.service.HandleService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the handle package, use HandleServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public abstract class HandleServiceFactory {

    public abstract HandleService getHandleService();

    public static HandleServiceFactory getInstance()
    {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("handleServiceFactory", HandleServiceFactory.class);
    }
}
