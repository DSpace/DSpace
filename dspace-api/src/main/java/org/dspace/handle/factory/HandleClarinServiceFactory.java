/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle.factory;

import org.dspace.handle.service.HandleClarinService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the handle package, use HandleClarinServiceFactory.getInstance() to retrieve an
 * implementation
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public abstract class HandleClarinServiceFactory {

    public abstract HandleClarinService getHandleClarinService();

    public static HandleClarinServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                .getServiceByName("handleClarinServiceFactory", HandleClarinServiceFactory.class);
    }
}
