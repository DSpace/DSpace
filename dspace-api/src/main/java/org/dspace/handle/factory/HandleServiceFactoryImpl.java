/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle.factory;

import org.dspace.handle.service.HandleService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the handle package, use HandleServiceFactory.getInstance() to retrieve an implementation
 *
 * @author kevinvandevelde at atmire.com
 */
public class HandleServiceFactoryImpl extends HandleServiceFactory {

    @Autowired(required = true)
    private HandleService handleService;

    @Override
    public HandleService getHandleService() {
        return handleService;
    }
}
