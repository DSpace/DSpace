/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl.xoai;

import com.lyncode.xoai.dataprovider.services.api.RepositoryConfiguration;
import org.dspace.xoai.services.api.config.ConfigurationService;
import org.dspace.xoai.services.api.context.ContextService;
import org.dspace.xoai.services.api.context.ContextServiceException;
import org.dspace.xoai.services.api.EarliestDateResolver;
import org.dspace.xoai.services.api.xoai.IdentifyResolver;
import org.springframework.beans.factory.annotation.Autowired;

public class DSpaceIdentifyResolver implements IdentifyResolver {
    @Autowired
    private EarliestDateResolver earliestDateResolver;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private ContextService contextService;

    @Override
    public RepositoryConfiguration getIdentify() throws ContextServiceException {
        return new DSpaceRepositoryConfiguration(earliestDateResolver, configurationService, contextService.getContext());
    }
}
