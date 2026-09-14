/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.api.xoai;

import com.lyncode.xoai.dataprovider.services.api.ItemRepository;
import org.dspace.xoai.data.ResumptionCursor;
import org.dspace.xoai.services.api.context.ContextServiceException;

public interface ItemRepositoryResolver {
    /**
     * @param cursor position holder for the request being served, shared with the resumptionToken formatter
     * @return a repository bound to that request
     * @throws ContextServiceException if the repository cannot be built
     */
    ItemRepository getItemRepository(ResumptionCursor cursor) throws ContextServiceException;
}
