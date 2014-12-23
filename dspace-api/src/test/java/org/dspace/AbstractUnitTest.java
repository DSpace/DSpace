/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.dspace.app.util.MockUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.discovery.MockIndexEventConsumer;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.servicemanager.DSpaceKernelImpl;
import org.dspace.servicemanager.DSpaceKernelInit;
import org.dspace.storage.rdbms.MockDatabaseManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;



/**
 * This is the base class for Unit Tests. It contains some generic mocks and
 * utilities that are needed by most of the unit tests developed for DSpace.
 *
 * @author pvillega
 */
public class AbstractUnitTest
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(AbstractUnitTest.class);

    //Below there are static variables shared by all the instances of the class
    
    /**
     * Test properties.
     */
    protected static Properties testProps;

    //Below there are variables used in each test

    /**
     * Context mock object to use in the tests.
     */
    protected Context context;

    /**
     * EPerson mock object to use in the tests.
     */
    protected EPerson eperson;

    protected static DSpaceKernelImpl kernelImpl;

    /** 
     * This method will be run before the first test as per @BeforeClass. It will
     * initialize resources required for the tests.
     *
     * Due to the way Maven works, unit tests can't be run from a POM package,
     * which forbids us to run the tests from the Assembly and Configuration
     * package. On the other hand we need a structure of folders to run the tests,
     * like "solr", "report", etc.  This will be provided by a JAR assembly
     * built out of files from various modules -- see the dspace-parent POM.
     *
     * This method will load a few properties for derived test classes.
     * 
     * The ConfigurationManager will be initialized to load the test
     * "dspace.cfg".
     */
    @BeforeClass
    public static void initOnce()
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

            //load the test configuration file
            ConfigurationManager.loadConfig(null);

            // Initialise the service manager kernel
            kernelImpl = DSpaceKernelInit.getKernel(null);
            if (!kernelImpl.isRunning())
            {
                kernelImpl.start(ConfigurationManager.getProperty("dspace.dir"));
            }
            
            // Applies/initializes our mock database by invoking its constructor
            // (NOTE: This also initializes the DatabaseManager, which in turn
            // calls DatabaseUtils to initialize the entire DB via Flyway)
            new MockDatabaseManager();
            
            // Initialize mock indexer (which does nothing, since Solr isn't running)
            new MockIndexEventConsumer();
            
            // Initialize mock Util class
            new MockUtil();
        } 
        catch (IOException ex)
        {
            log.error("Error initializing tests", ex);
            fail("Error initializing tests");
        }
    }

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    public void init()
    {        
        try
        {
            //Start a new context
            context = new Context();
            context.turnOffAuthorisationSystem();

            //Find our global test EPerson account. If it doesn't exist, create it.
            eperson = EPerson.findByEmail(context, "test@email.com");
            if(eperson == null)
            {
                // This EPerson creation should only happen once (i.e. for first test run)
                log.info("Creating initial EPerson (email=test@email.com) for Unit Tests");
                eperson = EPerson.create(context);
                eperson.setFirstName("first");
                eperson.setLastName("last");
                eperson.setEmail("test@email.com");
                eperson.setCanLogIn(true);
                eperson.setLanguage(I18nUtil.getDefaultLocale().getLanguage());
                // actually save the eperson to unit testing DB
                eperson.update();
            }
            // Set our global test EPerson as the current user in DSpace
            context.setCurrentUser(eperson);

            // If our Anonymous/Administrator groups aren't initialized, initialize them as well
            Group.initDefaultGroupNames(context);

            context.restoreAuthSystemState();
            context.commit();
        }
        catch (AuthorizeException ex)
        {
            log.error("Error creating initial eperson or default groups", ex);
            fail("Error creating initial eperson or default groups in AbstractUnitTest init()");
        }
        catch (SQLException ex) 
        {
            log.error(ex.getMessage(),ex);
            fail("SQL Error on AbstractUnitTest init()");
        }
    }

    /**
     * This method will be run after every test as per @After. It will
     * clean resources initialized by the @Before methods.
     *
     * Other methods can be annotated with @After here or in subclasses
     * but no execution order is guaranteed
     */
    @After
    public void destroy()
    {
        // Cleanup our global context object
        cleanupContext(context);
    }

    /**
     * This method will be run after all tests finish as per @AfterClass. It will
     * clean resources initialized by the @BeforeClass methods.
     *
     */
    @AfterClass
    public static void destroyOnce()
    {
        //we clear the properties
        testProps.clear();
        testProps = null;
        
        //Also clear out the kernel & nullify (so JUnit will clean it up)
        if (kernelImpl!=null)
            kernelImpl.destroy();
        kernelImpl = null;
    }

    /**
     * This method checks the configuration for Surefire has been done properly
     * and classes that start with Abstract are ignored. It is also required
     * to be able to run this class directly from and IDE (we need one test)
     */
    /*
    @Test
    public void testValidationShouldBeIgnored()
    {
        assertTrue(5 != 0.67) ;
    }
    */

    /**
     * This method expects and exception to be thrown. It also has a time
     * constraint, failing if the test takes more than 15 ms.
     */
    /*
    @Test(expected=java.lang.Exception.class, timeout=15)
    public void getException() throws Exception
    {
        throw new Exception("Fail!");
    }
    */

    /**
     *  Utility method to cleanup a created Context object (to save memory).
     *  This can also be used by individual tests to cleanup context objects they create.
     */
    protected void cleanupContext(Context c)
    {
        // If context still valid, abort it
        if(c!=null && c.isValid())
           c.abort();

        // Cleanup Context object by setting it to null
        if(c!=null)
           c = null;
    }
}
