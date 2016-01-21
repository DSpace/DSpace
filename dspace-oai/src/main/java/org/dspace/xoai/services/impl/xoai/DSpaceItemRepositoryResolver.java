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
import org.dspace.xoai.services.api.config.ConfigurationService;
import org.dspace.xoai.services.api.context.ContextService;
import org.dspace.xoai.services.api.context.ContextServiceException;
import org.dspace.xoai.services.api.CollectionsService;
import org.dspace.xoai.services.api.HandleResolver;
import org.dspace.xoai.services.api.solr.SolrQueryResolver;
import org.dspace.xoai.services.api.solr.SolrServerResolver;
import org.dspace.xoai.services.api.xoai.ItemRepositoryResolver;
import org.springframework.beans.factory.annotation.Autowired;

public class DSpaceItemRepositoryResolver implements ItemRepositoryResolver {
    @Autowired
    ContextService contextService;
    @Autowired
    ConfigurationService configurationService;
    @Autowired
    SolrServerResolver solrServerResolver;
    @Autowired
    SolrQueryResolver solrQueryResolver;
    @Autowired
    CollectionsService collectionsService;
    @Autowired
    private HandleResolver handleResolver;

    private ItemRepository itemRepository;


    @Override
    public ItemRepository getItemRepository() throws ContextServiceException {
        if (itemRepository == null) {
            try {
                itemRepository = new DSpaceItemSolrRepository(
                        solrServerResolver.getServer(),
                        collectionsService,
                        handleResolver,
                        solrQueryResolver);
            } catch (SolrServerException e) {
                throw new ContextServiceException(e.getMessage(), e);
            }
        }

        return itemRepository;
    }
}
