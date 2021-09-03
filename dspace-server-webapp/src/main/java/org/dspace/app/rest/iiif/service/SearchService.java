/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.service;

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
    private final boolean validationEnabled;

    @Autowired
    WordHighlightSolrSearch annotationService;

    public SearchService(ConfigurationService configurationService) {
        validationEnabled = configurationService
                .getBooleanProperty("discovery.solr.url.validation.enabled", true);
    }

    /**
     * Executes a search query for items in the current manifest.
     *
     * @param uuid dspace item uuid
     * @param query the solr query
     * @return IIIF search result with page coordinate annotations.
     */
    public String searchWithinManifest(UUID uuid, String query) {
        annotationService.initializeQuery(IIIF_ENDPOINT, getManifestId(uuid), validationEnabled);
        return annotationService.getSolrSearchResponse(uuid, query);
    }

}
