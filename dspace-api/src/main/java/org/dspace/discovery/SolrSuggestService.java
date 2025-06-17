/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.io.IOException;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.util.NamedList;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
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

    public String getSuggestions(String query, String dictionary) {
        String json = "";
        ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        String solrService = configurationService.getProperty("discovery.search.server");
        boolean validationEnabled =  configurationService
                .getBooleanProperty("discovery.solr.url.validation.enabled");
        UrlValidator urlValidator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);
        if (urlValidator.isValid(solrService) || validationEnabled) {
            HttpSolrClient solrServer = new HttpSolrClient.Builder(solrService).build();
            solrServer.setUseMultiPartPost(true);
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.set("suggest", true);
            solrQuery.set("suggest.q", query);
            solrQuery.set("suggest.dictionary", dictionary);
            solrQuery.setRequestHandler("/suggest");
            QueryRequest req = new QueryRequest(solrQuery);
            // returns raw json response.
            req.setResponseParser(new NoOpResponseParser("json"));
            NamedList<Object> resp;
            try {
                resp = solrServer.request(req);
                json =  (String) resp.get("response");
            } catch (SolrServerException | IOException e) {
                throw new RuntimeException("Unable to retrieve suggest response.", e);
            }
        } else {
            log.error("Error while initializing solr, invalid url: " + solrService);
        }
        return json;
    }
}
