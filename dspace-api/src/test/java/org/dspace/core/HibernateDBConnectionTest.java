/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;

import org.dspace.AbstractUnitTest;
import org.dspace.eperson.EPerson;
import org.dspace.utils.DSpace;
import org.hibernate.Session;
import org.junit.Before;
import org.junit.Test;

/**
 * Perform some basic unit tests for HibernateDBConnection
 *
 * @author tdonohue
 */
public class HibernateDBConnectionTest extends AbstractUnitTest {

    private HibernateDBConnection connection;

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    @Override
    public void init() {
        super.init();
        // Get a DB connection to test with
        connection = new DSpace().getServiceManager()
                                 .getServiceByName(null, HibernateDBConnection.class);
    }

    /**
     * Test of getSession method
     */
    @Test
    public void testGetSession() throws SQLException {
        assertNotNull("DB connection should not be null", connection);
        // Connection should begin with an active transaction
        assertTrue("A transaction should be open by default", connection.getTransaction().isActive());

        // Rollback current transaction
        connection.getTransaction().rollback();

        // Transaction should be closed
        assertFalse("Transaction should be closed after rollback", connection.getTransaction().isActive());

        //Now call getSession(), saving a reference to the session
        Session currentSession = connection.getSession();

        // New transaction should be initialized
        assertTrue("New transaction should be open after getSession() call",
                   connection.getTransaction().isActive());

        // Call getSession again. The same Session should still be returned
        assertEquals("Multiple calls to getSession should return same Session", currentSession,
                     connection.getSession());
    }

    /**
     * Test of isTransactionAlive method
     */
    @Test
    public void testIsTransactionAlive() {
        assertNotNull("DB connection should not be null", connection);
        assertNotNull("Transaction should not be null", connection.getTransaction());
        // Connection should begin with a transaction
        assertTrue("A transaction should be open by default", connection.isTransActionAlive());

        // Rollback current transaction
        connection.getTransaction().rollback();

        // Transaction should be closed
        assertFalse("Transaction should be closed after rollback", connection.isTransActionAlive());
    }

    /**
     * Test of isSessionAlive method
     */
    @Test
    public void testIsSessionAlive() throws SQLException {
        assertNotNull("DB connection should not be null", connection);
        assertNotNull("Session should not be null", connection.getSession());
        assertTrue("A Session should be alive by default", connection.isSessionAlive());

        // Rollback current transaction, closing it
        connection.getTransaction().rollback();

        // Session should still be alive even after transaction closes
        assertTrue("A Session should still be alive if transaction closes", connection.isSessionAlive());

        // NOTE: Because we configure Hibernate Session objects to be bound to a thread
        // (see 'hibernate.current_session_context_class' in hibernate.cfg.xml), a Session is ALWAYS ALIVE until
        // the thread closes (at which point Hibernate will clean it up automatically).
        // This means that essentially isSessionAlive() will always return true, unless the connection is severed
        // in some unexpected way.  See also "testCloseDBConnection()"
    }

    /**
     * Test of closeDBConnection method
     */
    @Test
    public void testCloseDBConnection() throws SQLException {
        // Get a reference to the current Session
        Session initialSession = connection.getSession();

        // Close the DB connection / Session
        // NOTE: Because of our Hibernate configuration, Hibernate automatically creates a new Session per thread.
        // Even though we "close" the connection, Hibernate will reopen a new one immediately. So, all this actually
        // does is create a *new* Session
        connection.closeDBConnection();

        Session newSession = connection.getSession();
        assertNotEquals("New Session expected",initialSession, newSession);
    }

    /**
     * Test of commit method
     */
    @Test
    public void testCommit() throws SQLException {
        // Ensure a transaction exists
        connection.getSession();
        assertTrue("Transaction should be active", connection.getTransaction().isActive());

        connection.commit();
        assertFalse("Commit should close transaction", connection.getTransaction().isActive());

        // A second commit should be a no-op (no error thrown)
        connection.commit();
    }

    /**
     * Test of rollback method
     */
    @Test
    public void testRollback() throws SQLException {
        // Ensure a transaction exists
        connection.getSession();
        assertTrue("Transaction should be active", connection.getTransaction().isActive());

        connection.rollback();
        assertFalse("Rollback should close transaction", connection.getTransaction().isActive());

        // A second rollback should be a no-op (no error thrown)
        connection.rollback();
    }

    /**
     * Test of reloadEntity method
     */
    @Test
    public void testReloadEntityAfterRollback() throws SQLException {
        // Get DBConnection  associated with DSpace Context
        HibernateDBConnection dbConnection = (HibernateDBConnection) context.getDBConnection();
        EPerson person = context.getCurrentUser();

        assertTrue("Current user should be cached in session", dbConnection.getSession()
                                                                           .contains(person));

        dbConnection.rollback();
        assertFalse("Current user should be gone from cache", dbConnection.getSession()
                                                                           .contains(person));

        person = dbConnection.reloadEntity(person);
        assertTrue("Current user should be cached back in session", dbConnection.getSession()
                                                                           .contains(person));
    }

    /**
     * Test of reloadEntity method
     */
    @Test
    public void testReloadEntityAfterCommit() throws SQLException {
        // Get DBConnection associated with DSpace Context
        HibernateDBConnection dbConnection = (HibernateDBConnection) context.getDBConnection();
        EPerson person = context.getCurrentUser();

        assertTrue("Current user should be cached in session", dbConnection.getSession()
                                                                           .contains(person));

        dbConnection.commit();
        assertFalse("Current user should be gone from cache", dbConnection.getSession()
                                                                          .contains(person));

        person = dbConnection.reloadEntity(person);
        assertTrue("Current user should be cached back in session", dbConnection.getSession()
                                                                                .contains(person));
    }

    /**
     * Test of uncacheEntity method
     */
    @Test
    public void testUncacheEntity() throws SQLException {
        // Get DBConnection associated with DSpace Context
        HibernateDBConnection dbConnection = (HibernateDBConnection) context.getDBConnection();
        EPerson person = context.getCurrentUser();

        assertTrue("Current user should be cached in session", dbConnection.getSession()
                                                                           .contains(person));

        dbConnection.uncacheEntity(person);
        assertFalse("Current user should be gone from cache", dbConnection.getSession()
                                                                          .contains(person));

        // Test ability to reload an uncached entity
        person = dbConnection.reloadEntity(person);
        assertTrue("Current user should be cached back in session", dbConnection.getSession()
                                                                                .contains(person));
    }
}
