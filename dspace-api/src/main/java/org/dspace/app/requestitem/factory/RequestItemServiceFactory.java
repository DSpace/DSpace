/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem.factory;

import org.dspace.app.requestitem.service.RequestItemService;
import org.dspace.handle.service.HandleService;
import org.dspace.utils.DSpace;

/**
 * Abstract factory to get services for the requestitem package, use RequestItemServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public abstract class RequestItemServiceFactory {

    public abstract RequestItemService getRequestItemService();

    public static RequestItemServiceFactory getInstance()
    {
        return new DSpace().getServiceManager().getServiceByName("requestItemServiceFactory", RequestItemServiceFactory.class);
    }
}
