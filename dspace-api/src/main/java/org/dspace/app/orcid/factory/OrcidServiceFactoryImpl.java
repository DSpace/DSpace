/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.factory;

import org.dspace.app.orcid.service.OrcidHistoryService;
import org.dspace.app.orcid.service.OrcidQueueService;
import org.dspace.app.profile.service.ProfileOrcidSynchronizationService;
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
    private ProfileOrcidSynchronizationService orcidSynchronizationService;

    @Autowired
    private OrcidQueueService orcidQueueService;

    @Override
    public OrcidHistoryService getOrcidHistoryService() {
        return orcidHistoryService;
    }

    @Override
    public OrcidQueueService getOrcidQueueService() {
        return orcidQueueService;
    }

    @Override
    public ProfileOrcidSynchronizationService getOrcidSynchronizationService() {
        return orcidSynchronizationService;
    }

}
