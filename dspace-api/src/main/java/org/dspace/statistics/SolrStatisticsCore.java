/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import static org.apache.logging.log4j.LogManager.getLogger;

import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Bean containing the {@link SolrClient} for the statistics core
 */
public class SolrStatisticsCore {

    private static Logger log = getLogger(SolrStatisticsCore.class);

    protected SolrClient solr = null;

    @Autowired
    private ConfigurationService configurationService;

    /**
     * Returns the {@link SolrClient} for the Statistics core.
     * Initializes it if needed.
     * @return The {@link SolrClient} for the Statistics core
     */
    public SolrClient getSolr() {
        if (solr == null) {
            initSolr();
        }
        return solr;
    }

    /**
     * Initializes the statistics {@link SolrClient}.
     */
    protected void initSolr() {

        String solrService = configurationService.getProperty("solr-statistics.server");

        log.info("solr-statistics.server:  {}", solrService);
        log.info("usage-statistics.dbfile:  {}", configurationService.getProperty("usage-statistics.dbfile"));

        try {
            solr = new HttpSolrClient.Builder(solrService).build();
        } catch (Exception e) {
            log.error("Error accessing Solr server configured in 'solr-statistics.server'", e);
        }
    }
}
