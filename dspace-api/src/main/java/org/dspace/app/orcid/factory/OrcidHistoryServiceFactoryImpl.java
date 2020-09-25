/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.factory;

import org.dspace.app.orcid.service.OrcidHistoryService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link OrcidHistoryServiceFactory}.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 *
 */
public class OrcidHistoryServiceFactoryImpl extends OrcidHistoryServiceFactory {

    @Autowired
    private OrcidHistoryService orcidHistoryService;

    @Override
    public OrcidHistoryService getOrcidHistoryService() {
        return orcidHistoryService;
    }

}
