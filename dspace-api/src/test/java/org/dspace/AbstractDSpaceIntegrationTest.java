/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.builder.AbstractBuilder;
import org.dspace.discovery.SearchUtils;
import org.dspace.servicemanager.DSpaceKernelImpl;
import org.dspace.servicemanager.DSpaceKernelInit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

/**
 * Abstract Test class copied from DSpace API
 */
public class AbstractDSpaceIntegrationTest {

    /**
     * log4j category
     */
    private static final Logger log = LogManager
            .getLogger(AbstractDSpaceIntegrationTest.class);

    /**
     * Test properties. These configure our general test environment
     */
    protected static Properties testProps;

    /**
     * DSpace Kernel. Must be started to initialize ConfigurationService and
     * any other services.
     */
    protected static DSpaceKernelImpl kernelImpl;

    /**
     * Obtain the TestName from JUnit, so that we can print it out in the test logs (see below)
     */
    
    public String testName;

    /**
     * Default constructor
     */
    protected AbstractDSpaceIntegrationTest() { }

    /**
     * This method will be run before the first test as per @BeforeClass. It will
     * initialize shared resources required for all tests of this class.
     *
     * This method loads our test properties for usage in test environment.
     */
    @BeforeAll
    public static void initTestEnvironment() {
        try {
            //Stops System.exit(0) throws exception instead of exiting
            //System.setSecurityManager(new NoExitSecurityManager());

            // All tests should assume UTC timezone by default (unless overridden in the test itself)
            // This ensures that Spring doesn't attempt to change the timezone of dates that are read from the
            // database (via Hibernate). We store all dates in the database as UTC.
            TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));

            //load the properties of the tests
            testProps = new Properties();
            URL properties = AbstractDSpaceIntegrationTest.class.getClassLoader()
                                                                .getResource("test-config.properties");
            testProps.load(properties.openStream());

            // Get a reference to current Kernel
            kernelImpl = DSpaceKernelInit.getKernel(null);
            // If somehow the kernel is NOT initialized, initialize it.
            // NOTE: This is likely never going to occur, as Spring Boot initializes it
            // See AbstractControllerIntegrationTest (where @SpringBootTest is defined)
            if (!kernelImpl.isRunning()) {
                // NOTE: the "dspace.dir" system property MUST be specified via Maven
                kernelImpl.start(getDspaceDir()); // init the kernel
            }

            // Initialize our builder (by loading all DSpace services)
            AbstractBuilder.init();
        } catch (IOException ex) {
            log.error("Error initializing tests", ex);
            fail("Error initializing tests: " + ex.getMessage());
        }
    }

    @BeforeEach
    public void printTestMethodBefore(TestInfo testInfo) {
        Optional<Method> testMethod = testInfo.getTestMethod();
        if (testMethod.isPresent()) {
            this.testName = testMethod.get().getName();
        }
        // Log the test method being executed. Put lines around it to make it stand out.
        log.info("---");
        log.info("Starting execution of test method: {}()", testName);
        log.info("---");
    }

    @AfterEach
    public void printTestMethodAfter() {
        // Log the test method just completed.
        log.info("Finished execution of test method: {}()", testName);
    }

    /**
     * This method will be run after all tests finish as per @AfterClass.  It
     * will clean resources initialized by the @BeforeClass methods.
     * @throws java.sql.SQLException
     */
    @AfterAll
    public static void destroyTestEnvironment() throws SQLException {
        //System.setSecurityManager(null);

        // Clear our test properties
        testProps.clear();
        testProps = null;

        // Unload DSpace services
        AbstractBuilder.destroy();
        SearchUtils.clearCachedSearchService();

        // NOTE: We explicitly do NOT stop/destroy the kernel, as it is cached
        // in the Spring ApplicationContext. By default, to speed up tests,
        // Spring caches & reuses its ApplicationContext for all tests. So,
        // similarly, our kernel is being cached & reused for all tests.
    }

    public static String getDspaceDir() {
        return System.getProperty("dspace.dir");

    }
}

