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
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the requestitem package, use RequestItemServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public class RequestItemServiceFactoryImpl extends RequestItemServiceFactory {

    @Autowired(required = true)
    private RequestItemService requestItemService;

    @Override
    public RequestItemService getRequestItemService() {
        return requestItemService;
    }
}
