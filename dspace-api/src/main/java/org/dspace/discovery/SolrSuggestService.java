/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Query the /suggest Solr search request handler to provide
 * weighted suggestions from dictionaries based on search index
 * documents and/or file-based word lists
 *
 * @author Kim Shepherd
 */
@Service
public class SolrSuggestService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger();

    @Autowired
    protected SolrSearchCore solrSearchCore;
    @Autowired
    protected ConfigurationService configurationService;

    protected SolrSuggestService() {

    }

    /**
     * Check whether the given dictionary name is in the configured allowlist.
     * If no allowlist is configured (empty or null), no dictionaries are allowed.
     * Otherwise, only explicitly listed dictionary names are allowed.
     *
     * @param dictionary the dictionary name to check
     * @return true if allowed, false otherwise
     */
    public boolean isAllowedDictionary(String dictionary) {
        String[] allowed = configurationService.getArrayProperty(
                "discovery.suggest.allowed-dictionaries");
        if (allowed == null || allowed.length == 0) {
            return false;
        }
        return Arrays.asList(allowed).contains(dictionary);
    }

    /**
     * Get a list of suggested terms from the Solr suggest request handler
     *
     * @param query      the current text input
     * @param dictionary the name of the Solr suggest dictionary to search
     * @return simple serialised JSON containing Solr suggest results
     */
    public Map getSuggestions(String query, String dictionary) {

        SolrClient solrClient = solrSearchCore.getSolr();

        try {
            SolrQuery solrQuery = createSuggestQuery(dictionary);
            solrQuery.set("suggest.q", query);

            QueryResponse response = solrClient.query(solrQuery);
            ObjectMapper mapper = new ObjectMapper();
            String json = response.jsonStr();
            return mapper.readValue(json, new TypeReference<Map<String, Object>>() {});

        } catch (SolrServerException | IOException e) {
            throw new RuntimeException("Unable to retrieve suggest response.", e);
        }
    }

    public void rebuildDictionary(String dictionary) {
        if (isAllowedDictionary(dictionary)) {
            sendBuildRequest(dictionary);
        }
    }

    public void rebuildAllDictionaries() {
        log.debug("Rebuilding all dictionaries");
        String[] allowed = configurationService.getArrayProperty("discovery.suggest.allowed-dictionaries");
        if (allowed == null || allowed.length == 0) {
            log.debug("No allowed dictionaries configured, skipping rebuild");
            return;
        }
        for (String dictionary : allowed) {
            sendBuildRequest(dictionary);
        }
    }

    private SolrQuery createSuggestQuery(String dictionary) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.set("suggest", true);
        solrQuery.set("suggest.dictionary", dictionary);
        solrQuery.setRequestHandler("/suggest");
        return solrQuery;
    }

    private void sendBuildRequest(String dictionary) {
        SolrClient solrClient = solrSearchCore.getSolr();
        try {
            SolrQuery solrQuery = createSuggestQuery(dictionary);
            solrQuery.set("suggest.build", true);
            solrClient.query(solrQuery);
        } catch (SolrServerException | IOException e) {
            log.error("Unable to rebuild dictionary {}: {}", dictionary, e.getMessage());
        }
    }

}
