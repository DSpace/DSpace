/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace;

import static org.junit.Assert.fail;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.authority.AuthoritySearchService;
import org.dspace.authority.MockAuthoritySolrServiceImpl;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.AbstractBuilder;
import org.dspace.content.Community;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.discovery.MockSolrSearchCore;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.kernel.ServiceManager;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.statistics.MockSolrLoggerServiceImpl;
import org.dspace.statistics.MockSolrStatisticsCore;
import org.dspace.statistics.SolrStatisticsCore;
import org.dspace.storage.rdbms.DatabaseUtils;
import org.jdom2.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * Abstract Test class that will initialize the in-memory database
 */
public class AbstractIntegrationTestWithDatabase extends AbstractDSpaceIntegrationTest {
    /**
     * log4j category
     */
    private static final Logger log = LogManager
        .getLogger(AbstractIntegrationTestWithDatabase.class);

    /**
     * Context mock object to use in the tests.
     */
    protected Context context;

    /**
     * EPerson mock object to use in the tests.
     */
    protected EPerson eperson;

    /**
     * EPerson mock object in the Administrators group to use in the tests.
     */
    protected EPerson admin;

    /**
     * The password of our test eperson
     */
    protected String password = "mySuperS3cretP4ssW0rd";

    /**
     * The test Parent Community
     */
    protected Community parentCommunity = null;

    /**
     * This method will be run before the first test as per @BeforeClass. It will
     * initialize shared resources required for all tests of this class.
     * <p>
     * NOTE: Per JUnit, "The @BeforeClass methods of superclasses will be run before those the current class."
     * http://junit.org/apidocs/org/junit/BeforeClass.html
     * <p>
     * This method builds on the initialization in AbstractDSpaceIntegrationTest, and
     * initializes the in-memory database for tests that need it.
     */
    @BeforeClass
    public static void initDatabase() {
        try {
            // Update/Initialize the database to latest version (via Flyway)
            DatabaseUtils.updateDatabase();
        } catch (SQLException se) {
            log.error("Error initializing database", se);
            fail("Error initializing database: " + se.getMessage()
                     + (se.getCause() == null ? "" : ": " + se.getCause().getMessage()));
        }
    }

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for each individual unit test.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    public void setUp() throws Exception {
        try {
            //Start a new context
            context = new Context(Context.Mode.READ_WRITE);
            context.turnOffAuthorisationSystem();

            //Find our global test EPerson account. If it doesn't exist, create it.
            EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
            eperson = ePersonService.findByEmail(context, "test@email.com");
            if (eperson == null) {
                // This EPerson creation should only happen once (i.e. for first test run)
                log.info("Creating initial EPerson (email=test@email.com) for Unit Tests");
                eperson = ePersonService.create(context);
                eperson.setFirstName(context, "first");
                eperson.setLastName(context, "last");
                eperson.setEmail("test@email.com");
                eperson.setCanLogIn(true);
                eperson.setLanguage(context, I18nUtil.getDefaultLocale().getLanguage());
                ePersonService.setPassword(eperson, password);
                // actually save the eperson to unit testing DB
                ePersonService.update(context, eperson);
            }

            // Set our global test EPerson as the current user in DSpace
            context.setCurrentUser(eperson);

            // If our Anonymous/Administrator groups aren't initialized, initialize them as well
            EPersonServiceFactory.getInstance().getGroupService().initDefaultGroupNames(context);

            admin = ePersonService.findByEmail(context, "admin@email.com");
            if (admin == null) {
                // This EPerson creation should only happen once (i.e. for first test run)
                log.info("Creating initial EPerson (email=admin@email.com) for Unit Tests");
                admin = ePersonService.create(context);
                admin.setFirstName(context, "first (admin)");
                admin.setLastName(context, "last (admin)");
                admin.setEmail("admin@email.com");
                admin.setCanLogIn(true);
                admin.setLanguage(context, I18nUtil.getDefaultLocale().getLanguage());
                ePersonService.setPassword(admin, password);
                // actually save the eperson to unit testing DB
                ePersonService.update(context, admin);
                GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
                Group adminGroup = groupService.findByName(context, Group.ADMIN);
                groupService.addMember(context, adminGroup, admin);
            }

            context.restoreAuthSystemState();
        } catch (AuthorizeException ex) {
            log.error("Error creating initial eperson or default groups", ex);
            fail("Error creating initial eperson or default groups in AbstractUnitTest init()");
        } catch (SQLException ex) {
            log.error(ex.getMessage(), ex);
            fail("SQL Error on AbstractUnitTest init()");
        }
    }

    /**
     * This method will be run after every test as per @After. It will
     * clean resources initialized by the @Before methods.
     *
     * Other methods can be annotated with @After here or in subclasses
     * but no execution order is guaranteed.
     *
     * @throws java.lang.Exception passed through.
     */
    @After
    public void destroy() throws Exception {
        // Cleanup our global context object
        try {
            AbstractBuilder.cleanupObjects();
            parentCommunity = null;
            cleanupContext();

            ServiceManager serviceManager = DSpaceServicesFactory.getInstance().getServiceManager();
            // Clear the search core.
            MockSolrSearchCore searchService = serviceManager
                    .getServiceByName(null, MockSolrSearchCore.class);
            searchService.reset();
            // Clear the statistics core.
            serviceManager
                    .getServiceByName(SolrStatisticsCore.class.getName(), MockSolrStatisticsCore.class)
                    .reset();

            MockSolrLoggerServiceImpl statisticsService = serviceManager
                    .getServiceByName("solrLoggerService", MockSolrLoggerServiceImpl.class);
            statisticsService.reset();

            MockAuthoritySolrServiceImpl authorityService = serviceManager
                    .getServiceByName(AuthoritySearchService.class.getName(), MockAuthoritySolrServiceImpl.class);
            authorityService.reset();

            // Reload our ConfigurationService (to reset configs to defaults again)
            DSpaceServicesFactory.getInstance().getConfigurationService().reloadConfig();

            AbstractBuilder.cleanupBuilderCache();

            // NOTE: we explicitly do NOT destroy our default eperson & admin as they
            // are cached and reused for all tests. This speeds up all tests.
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Utility method to cleanup a created Context object (to save memory).
     * This can also be used by individual tests to cleanup context objects they create.
     * @throws java.sql.SQLException passed through.
     */
    protected void cleanupContext() throws SQLException {
        // If context still valid, flush all database changes and close it
        if (context != null && context.isValid()) {
            context.complete();
        }

        // Cleanup Context object by setting it to null
        if (context != null) {
            context = null;
        }
    }

    /**
     * Execute the given command and return the exit code.
     *
     * @param args the args to use for the script.
     * @return the status, 0 if success, non-zero otherwise.
     * @throws Exception if there's an error cleaning up after running the command.
     */
    public int runDSpaceScript(String... args) throws Exception {
        try {
            // Load up the ScriptLauncher's configuration
            Document commandConfigs = ScriptLauncher.getConfig(kernelImpl);

            // Check that there is at least one argument (if not display command options)
            if (args.length < 1) {
                log.error("You must provide at least one command argument");
            }

            // Look up command in the configuration, and execute.
            TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
            int status =  ScriptLauncher.handleScript(args, commandConfigs, testDSpaceRunnableHandler, kernelImpl);
            if (testDSpaceRunnableHandler.getException() != null) {
                throw testDSpaceRunnableHandler.getException();
            } else {
                return status;
            }
        } finally {
            if (!context.isValid()) {
                setUp();
            }
        }
    }
}
