/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle.factory;

import org.dspace.handle.service.HandleClarinService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the handle package, use HandleClarinServiceFactory.getInstance()
 * to retrieve an implementation
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class HandleClarinServiceFactoryImpl extends HandleClarinServiceFactory {

    @Autowired(required = true)
    private HandleClarinService handleClarinService;

    @Override
    public HandleClarinService getHandleClarinService() {
        return handleClarinService;
    }
}
