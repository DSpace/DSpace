/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.factory;

import org.dspace.app.orcid.client.OrcidClient;
import org.dspace.app.orcid.client.OrcidConfiguration;
import org.dspace.app.orcid.service.MetadataSignatureGenerator;
import org.dspace.app.orcid.service.OrcidEntityFactoryService;
import org.dspace.app.orcid.service.OrcidHistoryService;
import org.dspace.app.orcid.service.OrcidProfileSectionFactoryService;
import org.dspace.app.orcid.service.OrcidQueueService;
import org.dspace.app.orcid.service.OrcidSynchronizationService;
import org.dspace.app.orcid.service.OrcidTokenService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the orcid package, use
 * OrcidHistoryServiceFactory.getInstance() to retrieve an implementation.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4Science.it)
 *
 */
public abstract class OrcidServiceFactory {

    public abstract OrcidHistoryService getOrcidHistoryService();

    public abstract OrcidQueueService getOrcidQueueService();

    public abstract OrcidSynchronizationService getOrcidSynchronizationService();

    public abstract OrcidTokenService getOrcidTokenService();

    public abstract OrcidProfileSectionFactoryService getOrcidProfileSectionFactoryService();

    public abstract MetadataSignatureGenerator getMetadataSignatureGenerator();

    public abstract OrcidEntityFactoryService getOrcidEntityFactoryService();

    public abstract OrcidClient getOrcidClient();

    public abstract OrcidConfiguration getOrcidConfiguration();

    public static OrcidServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(
            "orcidServiceFactory", OrcidServiceFactory.class);
    }


}
