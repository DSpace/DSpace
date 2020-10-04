/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkimport.service.impl;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.bulkimport.service.ItemSearcher;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableItem;

public class ItemSearcherByMetadata implements ItemSearcher {

    private String metadata;

    private SearchService searchService;

    public ItemSearcherByMetadata(SearchService searchService, String metadata) {
        this.searchService = searchService;
        this.metadata = metadata;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Item searchBy(Context context, String searchParam) throws Exception {
        String query = metadata + ":" + searchParam;
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.addFilterQueries(query);

        DiscoverResult discoverResult = searchService.search(context, discoverQuery);
        List<IndexableObject> indexableObjects = discoverResult.getIndexableObjects();

        if (CollectionUtils.isEmpty(indexableObjects)) {
            return null;
        }

        if (indexableObjects.size() > 1) {
            throw new SearchServiceException("Multiple item found for search param " + query);
        }

        return (Item) indexableObjects.get(0).getIndexedObject();
    }

}
