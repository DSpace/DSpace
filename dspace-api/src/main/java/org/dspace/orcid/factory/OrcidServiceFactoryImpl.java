/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.factory;

import org.dspace.orcid.client.OrcidClient;
import org.dspace.orcid.client.OrcidConfiguration;
import org.dspace.orcid.service.MetadataSignatureGenerator;
import org.dspace.orcid.service.OrcidEntityFactoryService;
import org.dspace.orcid.service.OrcidHistoryService;
import org.dspace.orcid.service.OrcidProfileSectionFactoryService;
import org.dspace.orcid.service.OrcidQueueService;
import org.dspace.orcid.service.OrcidSynchronizationService;
import org.dspace.orcid.service.OrcidTokenService;
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
    private OrcidProfileSectionFactoryService orcidProfileSectionFactoryService;

    @Autowired
    private OrcidEntityFactoryService orcidEntityFactoryService;

    @Autowired
    private MetadataSignatureGenerator metadataSignatureGenerator;

    @Autowired
    private OrcidClient orcidClient;

    @Autowired
    private OrcidConfiguration orcidConfiguration;

    @Autowired
    private OrcidTokenService orcidTokenService;

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
    public OrcidProfileSectionFactoryService getOrcidProfileSectionFactoryService() {
        return orcidProfileSectionFactoryService;
    }

    @Override
    public MetadataSignatureGenerator getMetadataSignatureGenerator() {
        return metadataSignatureGenerator;
    }

    @Override
    public OrcidEntityFactoryService getOrcidEntityFactoryService() {
        return orcidEntityFactoryService;
    }

    @Override
    public OrcidTokenService getOrcidTokenService() {
        return orcidTokenService;
    }

    @Override
    public OrcidClient getOrcidClient() {
        return orcidClient;
    }

    @Override
    public OrcidConfiguration getOrcidConfiguration() {
        return orcidConfiguration;
    }

    public void setOrcidClient(OrcidClient orcidClient) {
        this.orcidClient = orcidClient;
    }

}
