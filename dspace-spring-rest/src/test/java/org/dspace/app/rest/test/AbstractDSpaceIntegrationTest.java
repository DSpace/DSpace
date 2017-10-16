/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.test;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.dspace.app.rest.builder.AbstractBuilder;
import org.dspace.servicemanager.DSpaceKernelImpl;
import org.dspace.servicemanager.DSpaceKernelInit;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Abstract Test class copied from DSpace API
 */
public class AbstractDSpaceIntegrationTest
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(AbstractDSpaceIntegrationTest.class);

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
            URL properties = AbstractDSpaceIntegrationTest.class.getClassLoader()
                    .getResource("test-config.properties");
            testProps.load(properties.openStream());

            // Initialise the service manager kernel
            kernelImpl = DSpaceKernelInit.getKernel(null);
            if (!kernelImpl.isRunning())
            {
                // NOTE: the "dspace.dir" system property MUST be specified via Maven
                kernelImpl.start(getDspaceDir()); // init the kernel
            }
            AbstractBuilder.init();
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

        AbstractBuilder.destroy();

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

