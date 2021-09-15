/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.service;

import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Implements IIIF Search API queries and responses.
 */
@Component
@RequestScope
public class SearchService extends AbstractResourceService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(SearchService.class);

    private final String searchPlugin;

    @Autowired
    List<SearchAnnotationService> annotationService;

    public SearchService(ConfigurationService configurationService) {
        setConfiguration(configurationService);
        // The search service to use is defined in dspace configuration.
        searchPlugin = configurationService.getProperty("iiif.search.plugin");
    }

    /**
     * Executes a search query for items in the current manifest.
     *
     * @param uuid dspace item uuid
     * @param query the solr query
     * @return IIIF search result with page coordinate annotations.
     */
    public String searchWithinManifest(UUID uuid, String query) {
        for (SearchAnnotationService service : annotationService) {
            if (service.getSearchPlugin(searchPlugin)) {
                service.initializeQuerySettings(IIIF_ENDPOINT, getManifestId(uuid));
                return service.getSolrSearchResponse(uuid, query);
            }
        }
        throw new RuntimeException(
                "IIIF search plugin was not found."
        );
    }

}
