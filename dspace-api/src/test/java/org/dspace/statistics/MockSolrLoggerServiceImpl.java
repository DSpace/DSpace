/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import java.io.File;

import com.maxmind.geoip2.DatabaseReader;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Mock service that uses an embedded SOLR server for the statistics core.
 */
public class MockSolrLoggerServiceImpl
        extends SolrLoggerServiceImpl
        implements InitializingBean {

    @Autowired(required = true)
    private ConfigurationService configurationService;

    public MockSolrLoggerServiceImpl() {
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        //We don't use SOLR in the tests of this module
        solr = null;

        new FakeDatabaseReader(); // Activate fake
        new FakeDatabaseReader.Builder(); // Activate fake
        File locationDb = File.createTempFile("GeoIP", ".db");
        locationDb.deleteOnExit();
        locationService = new DatabaseReader.Builder(locationDb).build();
        useProxies = configurationService.getBooleanProperty("useProxies");
    }

}
