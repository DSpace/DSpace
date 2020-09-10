/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.factory;

import org.dspace.app.orcid.service.OrcidQueueService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link OrcidQueueServiceFactory}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidQueueServiceFactoryImpl extends OrcidQueueServiceFactory {

    @Autowired
    private OrcidQueueService orcidQueueService;

    @Override
    public OrcidQueueService getOrcidQueueService() {
        return orcidQueueService;
    }

}
