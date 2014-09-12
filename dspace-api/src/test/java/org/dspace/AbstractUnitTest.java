/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace;

import java.io.BufferedReader;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.dspace.administer.MetadataImporter;
import org.dspace.administer.RegistryImportException;
import org.dspace.administer.RegistryLoader;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataField;
import org.dspace.content.NonUniqueMetadataException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.MockIndexEventConsumer;
import org.dspace.eperson.EPerson;
import org.dspace.servicemanager.DSpaceKernelImpl;
import org.dspace.servicemanager.DSpaceKernelInit;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.MockDatabaseManager;
import org.dspace.utils.DSpace;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.xml.sax.SAXException;



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
    protected static EPerson eperson;

    protected static DSpaceKernelImpl kernelImpl;
    
    // Whether the in-memory DB has been initiailzed for testing
    protected static boolean dbInitialized = false;

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
            
            // Applies/initializes our mock database by invoking its constructor:
            new MockDatabaseManager();
            // Now, initialize our Database itself by populating it
            initializeDB();
            
            // Also initialize these mock classes for general use
            new MockIndexEventConsumer();

            // Load the default registries. This assumes the temporary
            // filesystem is working and the in-memory DB in place.
            Context ctx = new Context();
            ctx.turnOffAuthorisationSystem();

            // We can't check via a boolean value (even static) as the class is
            // destroyed by the JUnit classloader. We rely on a value that will
            // always be in the database, if it has been initialized, to avoid
            // doing the work twice.
            if(MetadataField.find(ctx, 1) == null)
            {
                String base = ConfigurationManager.getProperty("dspace.dir")
                        + File.separator + "config" + File.separator
                        + "registries" + File.separator;

                RegistryLoader.loadBitstreamFormats(ctx, base + "bitstream-formats.xml");
                MetadataImporter.loadRegistry(base + "dublin-core-types.xml", true);
                MetadataImporter.loadRegistry(base + "sword-metadata.xml", true);
                ctx.commit();

                //create eperson if required
                eperson = EPerson.find(ctx, 1);
                if(eperson == null)
                {
                    eperson = EPerson.create(ctx);
                    eperson.setFirstName("first");
                    eperson.setLastName("last");
                    eperson.setEmail("test@email.com");
                    eperson.setCanLogIn(true);
                    eperson.setLanguage(I18nUtil.getDefaultLocale().getLanguage());
                }

                //Create search and browse indexes
                DSpace dspace = new DSpace();
                IndexingService indexer = dspace.getServiceManager().getServiceByName(IndexingService.class.getName(),IndexingService.class);
                indexer.createIndex(ctx);
                ctx.commit();
                
                // Nullify resources, so Junit will clean them up
                dspace = null;
                indexer = null;
            }
            ctx.restoreAuthSystemState();
            if(ctx.isValid())
            {
                ctx.complete();
            }
            ctx = null;    
        } 
        catch (RegistryImportException ex)
        {
            log.error("Error loading default data", ex);
            fail("Error loading default data");
        }
        catch (NonUniqueMetadataException ex)
        {
            log.error("Error loading default data", ex);
            fail("Error loading default data");
        }
        catch (ParserConfigurationException ex)
        {
            log.error("Error loading default data", ex);
            fail("Error loading default data");
        }
        catch (SAXException ex)
        {
            log.error("Error loading default data", ex);
            fail("Error loading default data");
        }
        catch (TransformerException ex)
        {
            log.error("Error loading default data", ex);
            fail("Error loading default data");
        }
        catch (AuthorizeException ex)
        {
            log.error("Error loading default data", ex);
            fail("Error loading default data");
        }
        catch (SQLException ex)
        {
            log.error("Error initializing the database", ex);
            fail("Error initializing the database");
        }
        catch (IOException ex)
        {
            log.error("Error initializing tests", ex);
            fail("Error initializing tests");
        }
    }

    /**
     * Copies one directory (And its contents) into another 
     * 
     * @param from Folder to copy
     * @param to Destination
     * @throws IOException There is an error while copying the content
     */
    /*
    protected static void copyDir(File from, File to) throws IOException
    {
        if(!from.isDirectory() || !to.isDirectory())
        {
            throw new IOException("Both parameters must be directories. from is "+from.isDirectory()+", to is "+to.isDirectory());
        }

        File[] contents = from.listFiles();
        for(File f: contents)
        {
            if(f.isFile())
            {
                File copy = new File(to.getAbsolutePath() + File.separator + f.getName());
                copy.createNewFile();
                copyFile(f, copy);
            }
            else if(f.isDirectory())
            {
                File copy = new File(to.getAbsolutePath() + File.separator + f.getName());
                copy.mkdir();
                copyDir(f, copy);
            }
        }
    }
     */

    /**
     * Removes the copies of the origin files from the destination folder. Used
     * to remove the temporal copies of files done for testing
     *
     * @param from Folder to check
     * @param to Destination from which to remove contents
     * @throws IOException There is an error while copying the content
     */
    /*
    protected static void deleteDir(File from, File to) throws IOException
    {
        if(!from.isDirectory() || !to.isDirectory())
        {
            throw new IOException("Both parameters must be directories. from is "+from.isDirectory()+", to is "+to.isDirectory());
        }

        File[] contents = from.listFiles();
        for(File f: contents)
        {
            if(f.isFile())
            {
                File copy = new File(to.getAbsolutePath() + File.separator + f.getName());
                if(copy.exists())
                {
                    copy.delete();
                }
                
            }
            else if(f.isDirectory())
            {
                File copy = new File(to.getAbsolutePath() + File.separator + f.getName());
                if(copy.exists() && copy.listFiles().length > 0)
                {
                    deleteDir(f, copy);
                }
                copy.delete();
            }
        }
    }
     */

    /**
     * Copies one file into another
     *
     * @param from File to copy
     * @param to Destination of copy
     * @throws IOException There is an error while copying the content
     */
    /*
    protected static void copyFile(File from, File to) throws IOException
    {
        if(!from.isFile() || !to.isFile())
        {
            throw new IOException("Both parameters must be files. from is "+from.isFile()+", to is "+to.isFile());
        }

        FileChannel in = (new FileInputStream(from)).getChannel();
        FileChannel out = (new FileOutputStream(to)).getChannel();
        in.transferTo(0, from.length(), out);
        in.close();
        out.close();
    }
     */

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
            //we start the context
            context = new Context();
            context.setCurrentUser(eperson);
            context.commit();
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

    /**
      * Create the database tables by running the schema SQL specified
      * in the 'db.schema.path' system property
      */
     private static void initializeDB() throws SQLException
     {
        if(!dbInitialized)
        {    
            String schemaPath = "";
            Connection dbConnection = null;
            try
            {
                //preload the contents of the database
                String s = new String();
                StringBuilder sb = new StringBuilder();

                schemaPath = System.getProperty("db.schema.path");
                if (schemaPath == null)
                    throw new IllegalArgumentException(
                           "System property db.schema.path must be defined");

                log.debug("Preloading Unit Test database from " + schemaPath);

                FileReader fr = new FileReader(new File(schemaPath));
                BufferedReader br = new BufferedReader(fr);

                while((s = br.readLine()) != null)
                {
                    //we skip white lines and comments
                    if(!"".equals(s.trim()) && !s.trim().startsWith("--"))
                    {
                        sb.append(s);
                    }
                }
                br.close();

                //we use ";" as a delimiter for each request. This assumes no triggers
                //nor other calls besides CREATE TABLE, CREATE SEQUENCE and INSERT
                //exist in the file
                String[] stmts = sb.toString().split(";");

                // Get a new database connection. This also initializes underlying DatabaseManager object
                dbConnection = DatabaseManager.getConnection();
                Statement st = dbConnection.createStatement();

                for(int i = 0; i<stmts.length; i++)
                {
                    // we ensure that there is no spaces before or after the request string
                    // in order to not execute empty statements
                    if(!stmts[i].trim().equals(""))
                    {
                        st.executeUpdate(stmts[i]);
                        log.debug("Loading into database: "+stmts[i]);
                    }
                }

                // Commit all DB changes & mark DB as initialized
                dbConnection.commit();
                dbInitialized = true;
            }
            catch (IOException e)
            {
                // Re-throw as a SQL exception... but note that it's a problem with the Schema file
                throw new SQLException("Unable to create test database from file '" + schemaPath + "'", e);
            }
            finally
            { 
                if (dbConnection!=null)
                {   
                    try
                    {
                        // close out our open DB connection
                        dbConnection.close();
                    }
                    catch(SQLException se)
                    {    
                        //ignore if we cannot close
                    }
                    dbConnection = null;
                }
            }
        }
    }
}
