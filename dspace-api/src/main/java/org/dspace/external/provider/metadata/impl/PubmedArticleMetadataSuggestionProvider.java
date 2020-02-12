/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.metadata.impl;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.impl.PubmedArticleDataProvider;
import org.dspace.external.provider.metadata.MetadataSuggestionProvider;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An implementation of the {@link MetadataSuggestionProvider} for the {@link PubmedArticleDataProvider}
 */
public class PubmedArticleMetadataSuggestionProvider extends MetadataSuggestionProvider<PubmedArticleDataProvider> {

    @Autowired
    private ItemService itemService;

    @Override
    public List<ExternalDataObject> metadataQuery(Item item, int start, int limit) {
        // Concatenate metadata and send to query
        String title = getQueryFromItem(item);
        if (StringUtils.isBlank(title)) {
            return Collections.emptyList();
        }
        return query(title, start, limit);
    }

    private String getQueryFromItem(Item item) {
        return itemService.getMetadataFirstValue(item, "dc", "title", null, Item.ANY);
    }

    @Override
    public List<ExternalDataObject> query(String query, int start, int limit) {
        return getExternalDataProvider().searchExternalDataObjects(query, start, limit);
    }

    @Override
    public int queryTotals(String query) {
        return getExternalDataProvider().getNumberOfResults(query);
    }

    @Override
    public int metadataQueryTotals(Item item) {
        return getExternalDataProvider().getNumberOfResults(getQueryFromItem(item));
    }
}
