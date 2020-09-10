/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.factory;

import org.dspace.app.orcid.service.OrcidQueueService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the orcid package, use
 * OrcidQueueServiceFactory.getInstance() to retrieve an implementation.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public abstract class OrcidQueueServiceFactory {

    public abstract OrcidQueueService getOrcidQueueService();

    public static OrcidQueueServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
            .getServiceByName("orcidQueueServiceFactory", OrcidQueueServiceFactory.class);
    }
}
