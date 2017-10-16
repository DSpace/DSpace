/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Properties;
import java.util.TimeZone;
import org.apache.log4j.Logger;
import org.dspace.app.util.MockUtil;
import org.dspace.servicemanager.DSpaceKernelImpl;
import org.dspace.servicemanager.DSpaceKernelInit;
import org.junit.AfterClass;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.runner.RunWith;

import mockit.integration.junit4.JMockit;

/**
 * DSpace Unit Tests need to initialize the DSpace Kernel / Service Mgr
 * in order to have access to configurations, etc. This Abstract class only
 * initializes the Kernel (without full in-memory DB initialization).
 * <P>
 * Tests which just need the Kernel (or configs) can extend this class.
 * <P>
 * Tests which also need an in-memory DB should extend AbstractUnitTest or AbstractIntegrationTest
 *
 * @see AbstractUnitTest
 * @see AbstractIntegrationTest
 * @author Tim
 */
@Ignore
@RunWith(JMockit.class)
public class AbstractDSpaceTest
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(AbstractDSpaceTest.class);

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
     * This method will be run before the first test as per @BeforeClass. It will
     * initialize shared resources required for all tests of this class.
     *
     * This method loads our test properties to initialize our test environment,
     * and then starts the DSpace Kernel (which allows access to services).
     */
    @BeforeClass
    public static void initKernel()
    {
        try
        {
            //set a standard time zone for the tests
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Dublin"));

            //load the properties of the tests
            testProps = new Properties();
            URL properties = AbstractUnitTest.class.getClassLoader()
                    .getResource("test-config.properties");
            testProps.load(properties.openStream());

            // Initialise the service manager kernel
            kernelImpl = DSpaceKernelInit.getKernel(null);
            if (!kernelImpl.isRunning())
            {
                // NOTE: the "dspace.dir" system property MUST be specified via Maven
                kernelImpl.start(getDspaceDir()); // init the kernel
            }

            // Initialize mock Util class (allows Util.getSourceVersion() to work in Unit tests)
            new MockUtil();
        }
        catch (IOException ex)
        {
            log.error("Error initializing tests", ex);
            fail("Error initializing tests: " + ex.getMessage());
        }
    }


    /**
     * This method will be run after all tests finish as per @AfterClass. It
     * will clean resources initialized by the @BeforeClass methods.
     */
    @AfterClass
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
    public static String getDspaceDir(){
        return System.getProperty("dspace.dir");

    }
}
