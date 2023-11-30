/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.lowerCase;

import java.util.List;
import java.util.Optional;

import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Service class for generation of front-end urls.
 */
@Component
public class FrontendUrlService {

    private static final Logger log = LoggerFactory.getLogger(FrontendUrlService.class);

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private SearchService searchService;

    /**
     * Generates front-end url for specified item.
     *
     * @param context context
     * @param item    item
     * @return front-end url
     */
    public String generateUrl(Context context, Item item) {
        String uiURL = configurationService.getProperty("dspace.ui.url");
        return generateUrlWithSearchService(item, uiURL, context)
                .orElseGet(() -> uiURL + "/items/" + item.getID());
    }

    /**
     * Generates front-end url for specified bitstream.
     *
     * @param bitstream bitstream
     * @return front-end url
     */
    public String generateUrl(Bitstream bitstream) {
        String uiURL = configurationService.getProperty("dspace.ui.url");
        return uiURL + "/bitstreams/" + bitstream.getID() + "/download";
    }

    private Optional<String> generateUrlWithSearchService(Item item, String uiURLStem, Context context) {
        DiscoverQuery entityQuery = new DiscoverQuery();
        entityQuery.setQuery("search.uniqueid:\"Item-" + item.getID() + "\" and entityType:*");
        entityQuery.addSearchField("entityType");

        try {
            DiscoverResult discoverResult = searchService.search(context, entityQuery);
            if (isNotEmpty(discoverResult.getIndexableObjects())) {
                List<String> entityTypes = discoverResult.getSearchDocument(discoverResult.getIndexableObjects()
                        .get(0)).get(0).getSearchFieldValues("entityType");
                if (isNotEmpty(entityTypes) && isNotBlank(entityTypes.get(0))) {
                    return Optional.of(uiURLStem + "/entities/" + lowerCase(entityTypes.get(0)) + "/" + item.getID());
                }
            }
        } catch (SearchServiceException e) {
            log.error("Failed getting entitytype through solr for item " + item.getID() + ": " + e.getMessage());
        }
        return Optional.empty();
    }
}
