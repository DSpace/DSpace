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

import org.apache.logging.log4j.Logger;
import org.dspace.servicemanager.DSpaceKernelImpl;
import org.dspace.servicemanager.DSpaceKernelInit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * DSpace Unit Tests need to initialize the DSpace Kernel / Service Mgr
 * in order to have access to configurations, etc. This Abstract class only
 * initializes the Kernel (without full in-memory DB initialization).
 * <P>
 * Tests which just need the Kernel (or configs) can extend this class.
 * <P>
 * Tests which also need an in-memory DB should extend AbstractUnitTest or AbstractIntegrationTest
 *
 * @author Tim
 * @see AbstractUnitTest
 * @see AbstractIntegrationTest
 */
@Disabled
@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class AbstractDSpaceTest {

    /**
     * Default constructor
     */
    protected AbstractDSpaceTest() {
    }

    /**
     * log4j category
     */
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(AbstractDSpaceTest.class);

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
     * This method will be run before the first test as per @BeforeClass. It will
     * initialize shared resources required for all tests of this class.
     *
     * This method loads our test properties to initialize our test environment,
     * and then starts the DSpace Kernel (which allows access to services).
     */
    @BeforeAll
    public static void initKernel() {
        try {
            // All tests should assume UTC timezone by default (unless overridden in the test itself)
            // This ensures that Spring doesn't attempt to change the timezone of dates that are read from the
            // database (via Hibernate). We store all dates in the database as UTC.
            TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));

            //load the properties of the tests
            testProps = new Properties();
            URL properties = AbstractDSpaceTest.class.getClassLoader()
                .getResource("test-config.properties");
            testProps.load(properties.openStream());

            // Initialise the service manager kernel
            kernelImpl = DSpaceKernelInit.getKernel(null);
            if (!kernelImpl.isRunning()) {
                // NOTE: the "dspace.dir" system property MUST be specified via Maven
                // For example: by using <systemPropertyVariables> of maven-surefire-plugin or maven-failsafe-plugin
                kernelImpl.start(getDspaceDir()); // init the kernel
            }
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
     * This method will be run after all tests finish as per @AfterClass. It
     * will clean resources initialized by the @BeforeClass methods.
     */
    @AfterAll
    public static void destroyKernel() throws SQLException {
        //we clear the properties
        testProps.clear();
        testProps = null;

        //Also clear out the kernel & nullify (so JUnit will clean it up)
        if (kernelImpl != null) {
            kernelImpl.destroy();
        }
        kernelImpl = null;
    }

    public static String getDspaceDir() {
        return System.getProperty("dspace.dir");

    }
}
