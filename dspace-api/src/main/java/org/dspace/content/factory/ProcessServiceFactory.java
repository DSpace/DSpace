/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.factory;

import org.dspace.scripts.service.ProcessService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the Process workload, use ProcessServiceFactory.getInstance() to retrieve an
 * implementation
 *
 */
public abstract class ProcessServiceFactory {

    /**
     * This method will return an instance of the ProcessService
     * @return  An instance of the ProcessService
     */
    public abstract ProcessService getProcessService();

    /**
     * Use this method to retrieve an implementation of the ProcessServiceFactory to use to retrieve the different beans
     * @return  An implementation of the ProcessServiceFactory
     */
    public static ProcessServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                                    .getServiceByName("processServiceFactory", ProcessServiceFactory.class);
    }
}
