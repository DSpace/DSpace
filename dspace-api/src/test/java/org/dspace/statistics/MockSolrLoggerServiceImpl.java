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
import org.springframework.beans.factory.InitializingBean;

/**
 * Mock service that uses an embedded SOLR server for the statistics core.
 * <p>
 * <strong>NOTE:</strong>  this class is overridden by one <em>of the same name</em>
 * defined in dspace-server-webapp and declared as a bean there.
 * See {@code config/spring/api/Z-mock-services.xml}.  Some kind of classpath
 * magic makes this work.
 */
public class MockSolrLoggerServiceImpl
        extends SolrLoggerServiceImpl
        implements InitializingBean {

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
    }

}
