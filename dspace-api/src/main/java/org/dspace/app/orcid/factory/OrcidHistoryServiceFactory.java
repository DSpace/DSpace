package org.dspace.app.orcid.factory;

import org.dspace.app.orcid.service.OrcidHistoryService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the orcid package, use
 * OrcidHistoryServiceFactory.getInstance() to retrieve an implementation.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 *
 */
public abstract class OrcidHistoryServiceFactory {

    public abstract OrcidHistoryService getOrcidHistoryService();

    public static OrcidHistoryServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(
                      "orcidHistoryServiceFactory", OrcidHistoryServiceFactory.class);
    }
}
