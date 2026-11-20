/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl.xoai;

import com.lyncode.xoai.dataprovider.services.api.ItemRepository;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.xoai.data.ResumptionCursor;
import org.dspace.xoai.services.api.CollectionsService;
import org.dspace.xoai.services.api.HandleResolver;
import org.dspace.xoai.services.api.context.ContextServiceException;
import org.dspace.xoai.services.api.solr.SolrQueryResolver;
import org.dspace.xoai.services.api.solr.SolrServerResolver;
import org.dspace.xoai.services.api.xoai.ItemRepositoryResolver;
import org.springframework.beans.factory.annotation.Autowired;

public class DSpaceItemRepositoryResolver implements ItemRepositoryResolver {
    @Autowired
    SolrServerResolver solrServerResolver;
    @Autowired
    SolrQueryResolver solrQueryResolver;
    @Autowired
    CollectionsService collectionsService;
    @Autowired
    private HandleResolver handleResolver;

    @Override
    public ItemRepository getItemRepository(ResumptionCursor cursor) throws ContextServiceException {
        try {
            // Built per request rather than cached, because the cursor it reads and moves belongs to a single
            // request. The cost is one wrapper object: the Solr client behind it stays shared, memorised by
            // {@link DSpaceSolrServerResolver}.
            return new DSpaceItemSolrRepository(
                solrServerResolver.getServer(),
                collectionsService,
                handleResolver,
                solrQueryResolver,
                cursor
            );
        } catch (SolrServerException e) {
            throw new ContextServiceException(e.getMessage(), e);
        }
    }
}
