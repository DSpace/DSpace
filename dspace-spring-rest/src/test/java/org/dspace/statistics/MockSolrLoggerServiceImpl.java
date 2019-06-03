/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.maxmind.geoip2.DatabaseReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.solr.MockSolrServer;
import org.dspace.usage.UsageWorkflowEvent;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Mock service that uses an embedded SOLR server for the statistics core.
 *
 * <p>
 * <strong>NOTE:</strong>  this class overrides one <em>of the same name</em>
 * defined in dspace-api and declared as a bean there.
 * See {@code config/spring/api/Z-mock-services.xml}.  Some kind of classpath
 * magic makes this work.
 */
@Service
public class MockSolrLoggerServiceImpl
        extends SolrLoggerServiceImpl
        implements InitializingBean, DisposableBean {

    private static final Logger log = LogManager.getLogger();

    private MockSolrServer mockSolrServer;

    @Autowired(required = true)
    private ConfigurationService configurationService;

    public MockSolrLoggerServiceImpl() {
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // Initialize our service with a Mock Solr statistics core
        mockSolrServer = new MockSolrServer("statistics");
        solr = mockSolrServer.getSolrServer();
    }

    @Override
    public void postView(DSpaceObject dspaceObject, HttpServletRequest request,
                         EPerson currentUser) {
        // Load our FakeDatabaseReader before each view event is logged
        loadFakeDatabaseReader();
        // Call overridden method
        super.postView(dspaceObject, request, currentUser);
    }

    @Override
    public void postView(DSpaceObject dspaceObject,
                         String ip, String userAgent, String xforwardedfor, EPerson currentUser) {
        // Load our FakeDatabaseReader before each view event is logged
        loadFakeDatabaseReader();
        // Call overridden method
        super.postView(dspaceObject, ip, userAgent, xforwardedfor, currentUser);
    }

    @Override
    public void postSearch(DSpaceObject resultObject, HttpServletRequest request, EPerson currentUser,
                           List<String> queries, int rpp, String sortBy, String order, int page, DSpaceObject scope) {
        // Load our FakeDatabaseReader before each search event is logged
        loadFakeDatabaseReader();
        // Call overridden method
        super.postSearch(resultObject, request, currentUser, queries, rpp, sortBy, order, page, scope);
    }

    @Override
    public void postWorkflow(UsageWorkflowEvent usageWorkflowEvent) throws SQLException {
        // Load our FakeDatabaseReader before each workflow event is logged
        loadFakeDatabaseReader();
        // Call overridden method
        super.postWorkflow(usageWorkflowEvent);
    }

    /** Remove all records. */
    public void reset() {
        mockSolrServer.reset();
    }

    @Override
    public void destroy() throws Exception {
        mockSolrServer.destroy();
    }

    /**
     * Load / activate our FakeDatabaseReader which uses JMockit to replace specific
     * methods of the DatabaseReader. This MUST be called for each method that uses
     * DatabaseReader as JMockit fake classes are only in effect for tests in which
     * they are defined: http://jmockit.github.io/tutorial/Faking.html
     */
    private void loadFakeDatabaseReader() {
        try {
            new FakeDatabaseReader(); // Activate fake
            new FakeDatabaseReader.Builder(); // Activate fake
            String locationDbPath = configurationService.getProperty("usage-statistics.dbfile");
            File locationDb = new File(locationDbPath);
            locationDb.createNewFile();
            locationService = new DatabaseReader.Builder(locationDb).build();
        } catch (Exception e) {
            log.error("Unable to load FakeDatabaseReader", e);
        }
    }
}
