/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.factory;

import org.dspace.app.orcid.service.MetadataSignatureGenerator;
import org.dspace.app.orcid.service.OrcidHistoryService;
import org.dspace.app.orcid.service.OrcidProfileSectionConfigurationHandler;
import org.dspace.app.orcid.service.OrcidQueueService;
import org.dspace.app.orcid.service.OrcidSynchronizationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link OrcidServiceFactory}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4Science.it)
 *
 */
public class OrcidServiceFactoryImpl extends OrcidServiceFactory {

    @Autowired
    private OrcidHistoryService orcidHistoryService;

    @Autowired
    private OrcidSynchronizationService orcidSynchronizationService;

    @Autowired
    private OrcidQueueService orcidQueueService;

    @Autowired
    private OrcidProfileSectionConfigurationHandler orcidProfileSectionConfigurationHandler;

    @Autowired
    private MetadataSignatureGenerator metadataSignatureGenerator;

    @Override
    public OrcidHistoryService getOrcidHistoryService() {
        return orcidHistoryService;
    }

    @Override
    public OrcidQueueService getOrcidQueueService() {
        return orcidQueueService;
    }

    @Override
    public OrcidSynchronizationService getOrcidSynchronizationService() {
        return orcidSynchronizationService;
    }

    @Override
    public OrcidProfileSectionConfigurationHandler getOrcidProfileSectionConfigurationHandler() {
        return orcidProfileSectionConfigurationHandler;
    }

    @Override
    public MetadataSignatureGenerator getMetadataSignatureGenerator() {
        return metadataSignatureGenerator;
    }

}
