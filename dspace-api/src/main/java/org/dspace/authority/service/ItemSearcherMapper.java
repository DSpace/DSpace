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

/**
 * Mapper class that routes search requests to the appropriate {@link ItemSearcher}
 * based on the search type. This allows multiple search strategies to be configured
 * (e.g., different searchers for ORCID, DOI, SCOPUS, etc.) with a default fallback
 * for unknown search types.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class ItemSearcherMapper {

    /**
     * Map of search type identifiers to their corresponding ItemSearcher implementations.
     * For example, "orcid" might map to an ORCID-based searcher.
     */
    private final Map<String, ItemSearcher> itemSearcherMap;

    /**
     * Default ItemSearcher used when the requested search type is not found in the map.
     */
    private final ItemSearcher defaultItemSearcher;

    /**
     * Creates a new ItemSearcherMapper with the specified searcher map and default searcher.
     *
     * @param itemSearcherMap    map of search type to ItemSearcher implementations
     * @param defaultItemSearcher the default searcher to use when no specific searcher is found
     */
    public ItemSearcherMapper(Map<String, ItemSearcher> itemSearcherMap, ItemSearcher defaultItemSearcher) {
        Assert.notNull(itemSearcherMap, "Item Searcher map must be non null");
        Assert.notNull(defaultItemSearcher, "Default Item Searcher must be non null");
        this.itemSearcherMap = itemSearcherMap;
        this.defaultItemSearcher = defaultItemSearcher;
    }

    /**
     * Returns the collection of allowed search types that this mapper can handle.
     *
     * @return collection of search type identifiers
     */
    public Collection<String> getAllowedSearchType() {
        return this.itemSearcherMap.keySet();
    }

    /**
     * Searches for an item using the specified search type and parameter.
     * If no searcher is configured for the given search type, the default searcher is used.
     *
     * @param context     the DSpace context
     * @param searchType  the type of search to perform (e.g., "ORCID")
     * @param searchParam the search parameter value
     * @param source      the source item (optional, for context)
     * @return the found item, or null if no match is found
     */
    public Item search(Context context, String searchType, String searchParam, Item source) {
        ItemSearcher itemSearcher = this.itemSearcherMap.get(searchType);
        if (itemSearcher == null) {
            itemSearcher = this.defaultItemSearcher;
        }
        return itemSearcher.searchBy(context, searchParam, source);
    }

}
