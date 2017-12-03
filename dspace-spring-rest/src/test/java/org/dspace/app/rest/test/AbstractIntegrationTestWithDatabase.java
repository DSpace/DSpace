/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.test;

import static org.junit.Assert.fail;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Community;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.storage.rdbms.DatabaseUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * Abstract Test class that will initialize the in-memory database
 */
public class AbstractIntegrationTestWithDatabase extends AbstractDSpaceIntegrationTest {
    /** log4j category */
    private static final Logger log = Logger.getLogger(AbstractIntegrationTestWithDatabase.class);

    /**
     * Context mock object to use in the tests.
     */
    protected Context context;

    /**
     * EPerson mock object to use in the tests.
     */
    protected EPerson eperson;

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
    public static void initDatabase()
    {
        // Clear our old flyway object. Because this DB is in-memory, its
        // data is lost when the last connection is closed. So, we need
        // to (re)start Flyway from scratch for each Unit Test class.
        DatabaseUtils.clearFlywayDBCache();

        try
        {
            // Update/Initialize the database to latest version (via Flyway)
            DatabaseUtils.updateDatabase();
        }
        catch(SQLException se)
        {
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
        try
        {
            //Start a new context
            context = new Context(Context.Mode.BATCH_EDIT);
            context.turnOffAuthorisationSystem();

            //Find our global test EPerson account. If it doesn't exist, create it.
            EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
            eperson = ePersonService.findByEmail(context, "test@email.com");
            if(eperson == null)
            {
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

            context.restoreAuthSystemState();
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
    public void destroy() throws Exception {
        // Cleanup our global context object
        try {
            if(context == null || !context.isValid()){
                context = new Context();
            }
            parentCommunity = context.reloadEntity(parentCommunity);
            eperson = context.reloadEntity(eperson);

            context.turnOffAuthorisationSystem();
            if(parentCommunity != null) {
                ContentServiceFactory.getInstance().getCommunityService().delete(context, parentCommunity);
            }
            if(eperson != null) {
                EPersonServiceFactory.getInstance().getEPersonService().delete(context, eperson);
            }

            parentCommunity = null;
            cleanupContext();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *  Utility method to cleanup a created Context object (to save memory).
     *  This can also be used by individual tests to cleanup context objects they create.
     */
    protected void cleanupContext() throws SQLException {
        // If context still valid, flush all database changes and close it
        if(context != null && context.isValid()) {
            context.complete();
        }

        // Cleanup Context object by setting it to null
        if(context !=null) {
            context = null;
        }
    }
}
