/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl.xoai;

import com.lyncode.xoai.dataprovider.services.api.SetRepository;
import org.dspace.xoai.services.api.context.ContextService;
import org.dspace.xoai.services.api.context.ContextServiceException;
import org.dspace.xoai.services.api.xoai.SetRepositoryResolver;
import org.springframework.beans.factory.annotation.Autowired;

public class DSpaceSetRepositoryResolver implements SetRepositoryResolver {
    @Autowired
    private ContextService contextService;

    @Override
    public SetRepository getSetRepository() throws ContextServiceException {
        return new DSpaceSetRepository(contextService.getContext());
    }
}
