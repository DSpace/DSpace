/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.factory;

import org.dspace.external.service.ExternalDataService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the External package. Use ExternalServiceFactory.getInstance() to retrieve
 * an implementation
 */
public abstract class ExternalServiceFactory {

    /**
     * Calling this method will provide an ExternalDataService bean
     * @return  An implementation of the ExternalDataService
     */
    public abstract ExternalDataService getExternalDataService();


    /**
     * This method will provide you with an implementation of this class to work with
     * @return  An implementation of this class to work with
     */
    public static ExternalServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                                    .getServiceByName("externalServiceFactory", ExternalServiceFactory.class);
    }
}
