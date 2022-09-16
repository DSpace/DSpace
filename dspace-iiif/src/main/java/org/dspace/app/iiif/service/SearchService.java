/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.service;

import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.dspace.app.iiif.exception.NotImplementedException;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * This service provides methods for creating {@code Search API} response. There should be a single instance of
 * this service per request. The {@code @RequestScope} provides a single instance created and available during
 * complete lifecycle of the HTTP request.
 */
@RequestScope
@Component
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
     * Executes a search query for items in the current manifest. A
     * search plugin must be enabled.
     *
     * @param uuid dspace item uuid
     * @param query the solr query
     * @return IIIF search result with page coordinate annotations.
     */
    public String searchWithinManifest(UUID uuid, String query) throws NotImplementedException {
        if (searchPlugin != null) {
            for (SearchAnnotationService service : annotationService) {
                if (service.useSearchPlugin(searchPlugin)) {
                    service.initializeQuerySettings(IIIF_ENDPOINT, getManifestId(uuid));
                    return service.getSearchResponse(uuid, query);
                }
            }
        }
        throw new NotImplementedException(
                "The IIIF search option is not enabled for this server."
        );
    }

}
