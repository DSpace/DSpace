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
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.storage.rdbms.DatabaseUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Bean containing the SolrClient for the search core
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class SolrSearchCore {

    private final Logger log = Logger.getLogger(SolrSearchCore.class);
    @Autowired
    protected IndexingService indexingService;
    @Autowired
    protected ConfigurationService configurationService;

    /**
     *  SolrServer for processing indexing events.
     */
    protected SolrClient solr = null;

    public SolrClient getSolr() {
        if (solr == null) {
            initSolr();
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
                    HttpSolrClient solrServer = new HttpSolrClient.Builder(solrService).build();

                    solrServer.setBaseURL(solrService);
                    solrServer.setUseMultiPartPost(true);
                    // Dummy/test query to search for Item (type=2) of ID=1
                    SolrQuery solrQuery = new SolrQuery()
                        .setQuery(SearchUtils.RESOURCE_TYPE_FIELD + ":" + IndexableItem.TYPE +
                                " AND " + SearchUtils.RESOURCE_ID_FIELD + ":1");
                    // Only return obj identifier fields in result doc
                    solrQuery.setFields(SearchUtils.RESOURCE_TYPE_FIELD, SearchUtils.RESOURCE_ID_FIELD);
                    solrServer.query(solrQuery, SolrRequest.METHOD.POST);

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
