/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;

import org.dspace.AbstractUnitTest;
import org.dspace.eperson.EPerson;
import org.dspace.utils.DSpace;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    @BeforeEach
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
        assertNotNull(connection, "DB connection should not be null");
        // Connection should begin with an active transaction
        assertTrue(connection.getTransaction().isActive(), "A transaction should be open by default");

        // Rollback current transaction
        connection.getTransaction().rollback();

        // Transaction should be closed
        assertFalse(connection.getTransaction().isActive(), "Transaction should be closed after rollback");

        //Now call getSession(), saving a reference to the session
        Session currentSession = connection.getSession();

        // New transaction should be initialized
        assertTrue(connection.getTransaction().isActive(),
                   "New transaction should be open after getSession() call");

        // Call getSession again. The same Session should still be returned
        assertEquals(currentSession,
                     connection.getSession(),
                     "Multiple calls to getSession should return same Session");
    }

    /**
     * Test of isTransactionAlive method
     */
    @Test
    public void testIsTransactionAlive() {
        assertNotNull(connection, "DB connection should not be null");
        assertNotNull(connection.getTransaction(), "Transaction should not be null");
        // Connection should begin with a transaction
        assertTrue(connection.isTransActionAlive(), "A transaction should be open by default");

        // Rollback current transaction
        connection.getTransaction().rollback();

        // Transaction should be closed
        assertFalse(connection.isTransActionAlive(), "Transaction should be closed after rollback");
    }

    /**
     * Test of isSessionAlive method
     */
    @Test
    public void testIsSessionAlive() throws SQLException {
        assertNotNull(connection, "DB connection should not be null");
        assertNotNull(connection.getSession(), "Session should not be null");
        assertTrue(connection.isSessionAlive(), "A Session should be alive by default");

        // Rollback current transaction, closing it
        connection.getTransaction().rollback();

        // Session should still be alive even after transaction closes
        assertTrue(connection.isSessionAlive(), "A Session should still be alive if transaction closes");

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
        assertNotEquals(initialSession, newSession, "New Session expected");
    }

    /**
     * Test of commit method
     */
    @Test
    public void testCommit() throws SQLException {
        // Ensure a transaction exists
        connection.getSession();
        assertTrue(connection.getTransaction().isActive(), "Transaction should be active");

        connection.commit();
        assertFalse(connection.getTransaction().isActive(), "Commit should close transaction");

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
        assertTrue(connection.getTransaction().isActive(), "Transaction should be active");

        connection.rollback();
        assertFalse(connection.getTransaction().isActive(), "Rollback should close transaction");

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

        assertTrue(dbConnection.getSession()
                                                                           .contains(person), "Current user should be cached in session");

        dbConnection.rollback();
        assertFalse(dbConnection.getSession()
                                                                           .contains(person), "Current user should be gone from cache");

        person = dbConnection.reloadEntity(person);
        assertTrue(dbConnection.getSession()
                                                                           .contains(person), "Current user should be cached back in session");
    }

    /**
     * Test of reloadEntity method
     */
    @Test
    public void testReloadEntityAfterCommit() throws SQLException {
        // Get DBConnection associated with DSpace Context
        HibernateDBConnection dbConnection = (HibernateDBConnection) context.getDBConnection();
        EPerson person = context.getCurrentUser();

        assertTrue(dbConnection.getSession()
                                                                           .contains(person), "Current user should be cached in session");

        dbConnection.commit();
        assertFalse(dbConnection.getSession()
                                                                          .contains(person), "Current user should be gone from cache");

        person = dbConnection.reloadEntity(person);
        assertTrue(dbConnection.getSession()
                                                                                .contains(person), "Current user should be cached back in session");
    }

    /**
     * Test of uncacheEntities method
     */
    @Test
    public void testUncacheEntities() throws SQLException {
        // Get DBConnection associated with DSpace Context
        HibernateDBConnection dbConnection = (HibernateDBConnection) context.getDBConnection();
        EPerson person = context.getCurrentUser();

        assertTrue(dbConnection.getSession()
                .contains(person), "Current user should be cached in session");

        dbConnection.uncacheEntities();
        assertFalse(dbConnection.getSession()
                .contains(person), "Current user should be gone from cache");

        // Test ability to reload an uncached entity
        person = dbConnection.reloadEntity(person);
        assertTrue(dbConnection.getSession()
                .contains(person), "Current user should be cached back in session");
    }

    /**
     * Test of uncacheEntity method
     */
    @Test
    public void testUncacheEntity() throws SQLException {
        // Get DBConnection associated with DSpace Context
        HibernateDBConnection dbConnection = (HibernateDBConnection) context.getDBConnection();
        EPerson person = context.getCurrentUser();

        assertTrue(dbConnection.getSession()
                                                                           .contains(person), "Current user should be cached in session");

        dbConnection.uncacheEntity(person);
        assertFalse(dbConnection.getSession()
                                                                          .contains(person), "Current user should be gone from cache");

        // Test ability to reload an uncached entity
        person = dbConnection.reloadEntity(person);
        assertTrue(dbConnection.getSession()
                                                                                .contains(person), "Current user should be cached back in session");
    }
}
