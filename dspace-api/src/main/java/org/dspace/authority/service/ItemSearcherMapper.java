/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.service;

import java.util.Collection;
import java.util.Map;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.springframework.util.Assert;

public class ItemSearcherMapper {

    private final Map<String, ItemSearcher> itemSearcherMap;

    private final ItemSearcher defaultItemSearcher;

    public ItemSearcherMapper(Map<String, ItemSearcher> itemSearcherMap, ItemSearcher defaultItemSearcher) {
        Assert.notNull(itemSearcherMap, "Item Searcher map must be non null");
        Assert.notNull(defaultItemSearcher, "Default Item Searcher must be non null");
        this.itemSearcherMap = itemSearcherMap;
        this.defaultItemSearcher = defaultItemSearcher;
    }

    public Collection<String> getAllowedSearchType() {
        return this.itemSearcherMap.keySet();
    }

    public Item search(Context context, String searchType, String searchParam, Item source) {
        ItemSearcher itemSearcher = this.itemSearcherMap.get(searchType);
        if (itemSearcher == null) {
            itemSearcher = this.defaultItemSearcher;
        }
        return itemSearcher.searchBy(context, searchParam, source);
    }

}
