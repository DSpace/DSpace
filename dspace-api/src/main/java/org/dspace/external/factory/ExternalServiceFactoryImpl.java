/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.factory;

import org.dspace.external.service.ExternalDataService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract factory to get services for the External package. Use ExternalServiceFactory.getInstance() to retrieve
 * an implementation
 */
public class ExternalServiceFactoryImpl extends ExternalServiceFactory {

    @Autowired(required = true)
    private ExternalDataService externalDataService;

    @Override
    public ExternalDataService getExternalDataService() {
        return externalDataService;
    }
}
