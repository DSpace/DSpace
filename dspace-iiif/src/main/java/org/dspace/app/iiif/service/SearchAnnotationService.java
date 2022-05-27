/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.service;

import java.util.UUID;

/**
 * Interface for IIIF Search API implementations.
 */
public interface SearchAnnotationService {

    /**
     * Initializes required values.
     *
     * @param endpoint the iiif service endpoint
     * @param manifestId the id of the manifest to search within
     */
    void initializeQuerySettings(String endpoint, String manifestId);

    /**
     * Executes the Search API solr query and returns iiif search result
     * annotations.
     *
     * @param query encoded query terms
     * @return iiif json response
     */
    String getSearchResponse(UUID uuid, String query);

    /**
     * Tests to see if the plugin is configured in iiif.cfg.
     *
     * @param className the canonical name of class
     * @return true if provided value matches plugin class name
     */
    boolean useSearchPlugin(String className);

}
