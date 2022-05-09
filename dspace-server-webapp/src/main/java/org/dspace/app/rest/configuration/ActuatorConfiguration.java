/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.configuration;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.app.rest.DiscoverableEndpointsService;
import org.dspace.app.rest.health.GeoIpHealthIndicator;
import org.dspace.authority.AuthoritySolrServiceImpl;
import org.dspace.discovery.SolrSearchCore;
import org.dspace.statistics.SolrStatisticsCore;
import org.dspace.xoai.services.api.solr.SolrServerResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.solr.SolrHealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.hateoas.Link;

/**
 * Configuration class related to the actuator endpoints.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@Configuration
public class ActuatorConfiguration {

    public static final Status UP_WITH_ISSUES_STATUS = new Status("UP_WITH_ISSUES");

    @Autowired
    private DiscoverableEndpointsService discoverableEndpointsService;

    @Value("${management.endpoints.web.base-path:/actuator}")
    private String actuatorBasePath;

    @EventListener(ApplicationReadyEvent.class)
    public void registerActuatorEndpoints() {
        discoverableEndpointsService.register(this, Arrays.asList(Link.of(actuatorBasePath, "actuator")));
    }

    @Bean
    @ConditionalOnEnabledHealthIndicator("solrSearch")
    @ConditionalOnProperty("discovery.search.server")
    public SolrHealthIndicator solrSearchCoreHealthIndicator(SolrSearchCore solrSearchCore) {
        return new SolrHealthIndicator(solrSearchCore.getSolr());
    }

    @Bean
    @ConditionalOnEnabledHealthIndicator("solrStatistics")
    @ConditionalOnProperty("solr-statistics.server")
    public SolrHealthIndicator solrStatisticsCoreHealthIndicator(SolrStatisticsCore solrStatisticsCore) {
        return new SolrHealthIndicator(solrStatisticsCore.getSolr());
    }

    @Bean
    @ConditionalOnEnabledHealthIndicator("solrAuthority")
    @ConditionalOnProperty("solr.authority.server")
    public SolrHealthIndicator solrAuthorityCoreHealthIndicator(AuthoritySolrServiceImpl authoritySolrService)
        throws MalformedURLException, SolrServerException, IOException {
        return new SolrHealthIndicator(authoritySolrService.getSolr());
    }

    @Bean
    @ConditionalOnEnabledHealthIndicator("solrOai")
    @ConditionalOnProperty("oai.solr.url")
    public SolrHealthIndicator solrOaiCoreHealthIndicator(SolrServerResolver solrServerResolver)
        throws SolrServerException {
        return new SolrHealthIndicator(solrServerResolver.getServer());
    }

    @Bean
    @ConditionalOnEnabledHealthIndicator("geoIp")
    public GeoIpHealthIndicator geoIpHealthIndicator() {
        return new GeoIpHealthIndicator();
    }

    public String getActuatorBasePath() {
        return actuatorBasePath;
    }

}
