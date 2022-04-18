/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.io.IOException;
import javax.inject.Named;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.service.impl.HttpConnectionPoolService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.storage.rdbms.DatabaseUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Bean containing the SolrClient for the search core.
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class SolrSearchCore {

    private final Logger log = LogManager.getLogger();
    @Autowired
    protected IndexingService indexingService;
    @Autowired
    protected ConfigurationService configurationService;
    @Autowired @Named("solrHttpConnectionPoolService")
    protected HttpConnectionPoolService httpConnectionPoolService;

    /**
     *  SolrServer for processing indexing events.
     */
    protected SolrClient solr = null;

    /**
     * Default HTTP method to use for all Solr Requests (we prefer POST).
     * This REQUEST_METHOD should be used in all Solr queries, e.g.
     *   solSearchCore.getSolr().query(myQuery, solrSearchCore.REQUEST_METHOD);
     */
    public SolrRequest.METHOD REQUEST_METHOD = SolrRequest.METHOD.POST;

    /**
     * Get access to current SolrClient. If no current SolrClient exists, a new one is initialized, see initSolr().
     * @return SolrClient Solr client
     */
    public SolrClient getSolr() {
        if (solr == null) {
            initSolr();
        }

        // If we are running Integration Tests using the EmbeddedSolrServer, we MUST override our default HTTP request
        // method to use GET instead of POST (the latter is what we prefer).  Unfortunately, EmbeddedSolrServer does not
        // current work well with POST requests (see https://issues.apache.org/jira/browse/SOLR-12858). When that bug is
        // fixed, we should remove this 'if' statement so that tests also use POST.
        if (solr.getClass().getSimpleName().equals("EmbeddedSolrServer")) {
            REQUEST_METHOD = SolrRequest.METHOD.GET;
        }
        return solr;
    }

    /**
     * Initialize the solr search core
     */
    protected void initSolr() {
        if (solr == null) {
            String solrService = DSpaceServicesFactory.getInstance().getConfigurationService()
                                                      .getProperty("discovery.search.server");

            UrlValidator urlValidator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);
            if (urlValidator.isValid(solrService) || configurationService
                .getBooleanProperty("discovery.solr.url.validation.enabled", true)) {
                try {
                    log.debug("Solr URL: " + solrService);
                    HttpSolrClient solrServer = new HttpSolrClient.Builder(solrService)
                            .withHttpClient(httpConnectionPoolService.getClient())
                            .build();

                    solrServer.setBaseURL(solrService);
                    solrServer.setUseMultiPartPost(true);
                    // Dummy/test query to search for Item (type=2) of ID=1
                    SolrQuery solrQuery = new SolrQuery()
                        .setQuery(SearchUtils.RESOURCE_TYPE_FIELD + ":" + IndexableItem.TYPE +
                                " AND " + SearchUtils.RESOURCE_ID_FIELD + ":1");
                    // Only return obj identifier fields in result doc
                    solrQuery.setFields(SearchUtils.RESOURCE_TYPE_FIELD, SearchUtils.RESOURCE_ID_FIELD);
                    solrServer.query(solrQuery, REQUEST_METHOD);

                    // As long as Solr initialized, check with DatabaseUtils to see
                    // if a reindex is in order. If so, reindex everything
                    DatabaseUtils.checkReindexDiscovery(indexingService);

                    solr = solrServer;
                } catch (SolrServerException | IOException e) {
                    log.error("Error while initializing solr server", e);
                }
            } else {
                log.error("Error while initializing solr, invalid url: " + solrService);
            }
        }
    }
}
