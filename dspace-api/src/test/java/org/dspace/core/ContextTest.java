/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Perform some basic unit tests for Context Class
 *
 * @author tdonohue
 */
public class ContextTest extends AbstractUnitTest {
    protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

    /**
     * Spy of AuthorizeService to use for tests
     * (initialized / setup in @Before method)
     */
    private AuthorizeService authorizeServiceSpy;

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

        // Initialize our spy of the autowired (global) authorizeService bean.
        // This allows us to customize the bean's method return values in tests below
        authorizeServiceSpy = spy(authorizeService);
        // "Wire" our spy to be used by the current loaded object services
        // (To ensure these services use the spy instead of the real service)
        ReflectionTestUtils.setField(ePersonService, "authorizeService", authorizeServiceSpy);
        ReflectionTestUtils.setField(groupService, "authorizeService", authorizeServiceSpy);
    }

    /**
     * Test of getDBConnection method, of class Context.
     */
    @Test
    public void testGetDBConnection() throws SQLException {
        DBConnection connection = context.getDBConnection();

        assertThat("testGetDBConnection 0", connection, notNullValue());
        assertThat("testGetDBConnection 1", connection.isSessionAlive(), equalTo(true));
    }

    /**
     * Test of setCurrentUser method, of class Context.
     */
    @Test
    public void testSetCurrentUser() throws SQLException, AuthorizeException, IOException {
        // Allow full Admin perms
        when(authorizeServiceSpy.isAdmin(context)).thenReturn(true);

        EPerson oldUser = context.getCurrentUser();

        // Create a dummy EPerson to set as current user
        EPerson newUser = ePersonService.create(context);
        newUser.setFirstName(context, "Jane");
        newUser.setLastName(context, "Doe");
        newUser.setEmail("jane@email.com");
        newUser.setCanLogIn(true);
        newUser.setLanguage(context, I18nUtil.getDefaultLocale().getLanguage());

        context.setCurrentUser(newUser);

        assertThat("testSetCurrentUser 0", context.getCurrentUser(), notNullValue());
        assertThat("testSetCurrentUser 1", context.getCurrentUser(), equalTo(newUser));

        // Restore the previous current user
        context.setCurrentUser(oldUser);

        // Cleanup our new user
        ePersonService.delete(context, newUser);
    }

    /**
     * Test of getCurrentUser method, of class Context.
     */
    @Test
    public void testGetCurrentUser() {
        //NOTE: 'eperson' is initialized by AbstractUnitTest & set as the "currentUser" there
        assertThat("testGetCurrentUser 0", context.getCurrentUser(), notNullValue());
        assertThat("testGetCurrentUser 1", context.getCurrentUser(), equalTo(eperson));
    }

    /**
     * Test of getCurrentLocale method, of class Context.
     */
    @Test
    public void testGetCurrentLocale() {
        //NOTE: CurrentLocale is not initialized in AbstractUnitTest. So it should be DEFAULTLOCALE
        assertThat("testGetCurrentLocale 0", context.getCurrentLocale(), notNullValue());
        assertThat("testGetCurrentLocale 1", context.getCurrentLocale(), equalTo(I18nUtil.getDefaultLocale()));
    }

    /**
     * Test of setCurrentLocale method, of class Context.
     */
    @Test
    public void testSetCurrentLocale() {
        //Get previous value
        Locale oldLocale = context.getCurrentLocale();

        //Set a new, non-English value
        Locale newLocale = Locale.FRENCH;
        context.setCurrentLocale(newLocale);

        assertThat("testSetCurrentLocale 0", context.getCurrentLocale(), notNullValue());
        assertThat("testSetCurrentLocale 1", context.getCurrentLocale(), equalTo(newLocale));

        // Restore previous value
        context.setCurrentLocale(oldLocale);
    }

    /**
     * Test of ignoreAuthorization method, of class Context.
     */
    @Test
    public void testIgnoreAuthorization() {
        // Turn off authorization
        context.turnOffAuthorisationSystem();
        assertThat("testIgnoreAuthorization 0", context.ignoreAuthorization(), equalTo(true));

        // Turn it back on
        context.restoreAuthSystemState();
        assertThat("testIgnoreAuthorization 1", context.ignoreAuthorization(), equalTo(false));
    }

    /**
     * Test of turnOffAuthorisationSystem method, of class Context.
     */
    /*@Test
    public void testTurnOffAuthorisationSystem() {
        // Already tested in testIgnoreAuthorization()
    }*/

    /**
     * Test of restoreAuthSystemState method, of class Context.
     */
    /*@Test
    public void testRestoreAuthSystemState() {
        // Already tested in testIgnoreAuthorization()
    }*/

    /**
     * Test of setIgnoreAuthorization method, of class Context.
     */
    /*@Test
    public void testSetIgnoreAuthorization() {
        // Deprecated method
    }*/

    /**
     * Test of setExtraLogInfo method, of class Context.
     */
    @Test
    public void testSetExtraLogInfo() {
        // Get the previous value
        String oldValue = context.getExtraLogInfo();

        // Set a new value
        String newValue = "This is some extra log info";
        context.setExtraLogInfo(newValue);

        assertThat("testSetExtraLogInfo 0", context.getExtraLogInfo(), notNullValue());
        assertThat("testSetExtraLogInfo 1", context.getExtraLogInfo(), equalTo(newValue));

        //restore old value
        context.setExtraLogInfo(oldValue);
    }

    /**
     * Test of getExtraLogInfo method, of class Context.
     */
    @Test
    public void testGetExtraLogInfo() {
        // Extra log info has a default value of "", and AbstractUnitTest doesn't change it
        String defaultValue = "";

        assertThat("testGetExtraLogInfo 0", context.getExtraLogInfo(), notNullValue());
        assertThat("testGetExtraLogInfo 1", context.getExtraLogInfo(), equalTo(defaultValue));
    }

    /**
     * Test of complete method, of class Context.
     */
    @Test
    public void testComplete() throws SQLException {
        // To test complete() we need a new Context object
        Context instance = new Context();

        // By default, we should have a new DB connection, so let's make sure it is there
        assertThat("testComplete 0", instance.getDBConnection(), notNullValue());
        assertThat("testComplete 1", instance.getDBConnection().isSessionAlive(), equalTo(true));
        assertThat("testComplete 2", instance.isValid(), equalTo(true));

        // Now, call complete(). This should set DB connection to null & invalidate context
        instance.complete();
        assertThat("testComplete 3", instance.getDBConnection(), nullValue());
        assertThat("testComplete 4", instance.isValid(), equalTo(false));

        // Cleanup our new context
        cleanupContext(instance);
        // TODO: May also want to test that complete() is calling commit()?
    }

    /**
     * Test of complete method, of class Context.
     */
    @Test
    public void testComplete2() throws SQLException {
        // To test complete() we need a new Context object
        Context instance = new Context();

        // Call complete twice. The second call should NOT throw an error
        // and effectively does nothing
        instance.complete();
        instance.complete();

        // Cleanup our new context
        cleanupContext(instance);
    }

    /**
     * Test of commit method, of class Context.
     */
    @Test
    public void testCommit() throws SQLException, AuthorizeException, IOException {
        // To test commit() we need a new Context object
        Context instance = new Context();

        // By default, we should have a new DB connection, so let's make sure it is there
        assertThat("HibernateDBConnection should exist", instance.getDBConnection(), notNullValue());
        assertTrue("Context should be valid", instance.isValid());
        assertTrue("Transaction should be open", instance.isTransactionAlive());

        // Allow full Admin perms (in new context)
        when(authorizeServiceSpy.isAdmin(instance)).thenReturn(true);

        // Create a new EPerson (to be committed)
        String createdEmail = "myfakeemail@example.com";
        EPerson newUser = ePersonService.create(instance);
        newUser.setFirstName(instance, "Tim");
        newUser.setLastName(instance, "Smith");
        newUser.setEmail(createdEmail);
        newUser.setCanLogIn(true);
        newUser.setLanguage(instance, I18nUtil.getDefaultLocale().getLanguage());

        // Now, call commit()
        instance.commit();

        // We expect our DB connection to still exist
        assertThat("HibernateDBConnection should still be open", instance.getDBConnection(), notNullValue());
        // We expect the Context to be valid
        assertTrue("Context should still be valid", instance.isValid());
        // However, the transaction should now be closed
        assertFalse("DB transaction should be closed", instance.isTransactionAlive());

        // ReloadEntity and verify changes saved
        // NOTE: reloadEntity() is required, see commit() method Javadocs
        newUser = instance.reloadEntity(newUser);
        assertEquals("New user should be created", newUser.getEmail(), createdEmail);

        // Change the email and commit again (a Context should support multiple commit() calls)
        String newEmail = "myrealemail@example.com";
        newUser.setEmail(newEmail);
        instance.commit();

        // Reload entity and new value should be there.
        newUser = instance.reloadEntity(newUser);
        assertEquals("New email address should be saved", newUser.getEmail(), newEmail);

        // Cleanup our new object & context
        ePersonService.delete(instance, newUser);
        cleanupContext(instance);
    }

    /**
     * Test of abort method, of class Context.
     */
    @Test
    public void testAbort() throws SQLException, AuthorizeException {
        // To test abort() we need a new Context object
        Context instance = new Context();

        // Allow full Admin perms (in new context)
        when(authorizeServiceSpy.isAdmin(instance)).thenReturn(true);

        // Create a new EPerson (DO NOT COMMIT IT)
        String createdEmail = "susie@email.com";
        EPerson newUser = ePersonService.create(instance);
        newUser.setFirstName(instance, "Susan");
        newUser.setLastName(instance, "Doe");
        newUser.setEmail(createdEmail);
        newUser.setCanLogIn(true);
        newUser.setLanguage(instance, I18nUtil.getDefaultLocale().getLanguage());

        // Abort our context
        instance.abort();
        // Ensure the context is no longer valid
        assertThat("testAbort 0", instance.isValid(), equalTo(false));

        // Open a new context, let's make sure that EPerson isn't there
        Context newInstance = new Context();
        EPerson found = ePersonService.findByEmail(newInstance, createdEmail);
        assertThat("testAbort 1", found, nullValue());

        // Cleanup our contexts
        cleanupContext(instance);
        cleanupContext(newInstance);
    }

    /**
     * Test of close method, of class Context.
     */
    @Test
    public void testClose() throws SQLException, AuthorizeException {
        String createdEmail = "susie@email.com";

        // To test close() we need a new Context object in a try-with-resources block
        try (Context instance = new Context()) {
            // Allow full Admin perms (in new context)
            when(authorizeServiceSpy.isAdmin(instance)).thenReturn(true);

            // Create a new EPerson (DO NOT COMMIT IT)
            EPerson newUser = ePersonService.create(instance);
            newUser.setFirstName(instance, "Susan");
            newUser.setLastName(instance, "Doe");
            newUser.setEmail(createdEmail);
            newUser.setCanLogIn(true);
            newUser.setLanguage(instance, I18nUtil.getDefaultLocale().getLanguage());
        }

        // Open a new context, let's make sure that EPerson isn't there
        Context newInstance = new Context();
        EPerson found = ePersonService.findByEmail(newInstance, createdEmail);
        assertThat("testClose 0", found, nullValue());

        // Cleanup our contexts
        cleanupContext(newInstance);

        //Calling close on a finished context should not result in errors
        newInstance.close();
    }

    /**
     * Test of abort method, of class Context.
     */
    @Test
    public void testAbort2() throws SQLException {
        // To test abort() we need a new Context object
        Context instance = new Context();

        // Call abort twice. The second call should NOT throw an error
        // and effectively does nothing
        instance.abort();
        instance.abort();

        // Cleanup our context
        cleanupContext(instance);
    }

    /**
     * Test of isValid method, of class Context.
     */
    /*@Test
    public void testIsValid() {
        // This is already tested by testComplete() and testAbort()
    }*/

    /**
     * Test of isReadOnly method, of class Context.
     */
    @Test
    public void testIsReadOnly() throws SQLException {
        // Our default context should NOT be read only
        assertThat("testIsReadOnly 0", context.isReadOnly(), equalTo(false));

        // Create a new read-only context
        Context instance = new Context(Context.Mode.READ_ONLY);
        assertThat("testIsReadOnly 1", instance.isReadOnly(), equalTo(true));

        //When in read-only, we only support abort().
        instance.abort();

        // Cleanup our context
        cleanupContext(instance);
    }

    /**
     * Test that commit cannot be called when the context is in read-only mode
     */
    @Test
    public void testIsReadOnlyCommit() throws SQLException {
        // Create a new read-only context
        Context instance = new Context(Context.Mode.READ_ONLY);
        assertThat("testIsReadOnly 1", instance.isReadOnly(), equalTo(true));

        try {
            //When in read-only, calling commit() should result in an error
            instance.commit();
            fail();
        } catch (Exception ex) {
            assertTrue(ex instanceof UnsupportedOperationException);
        }

        instance.abort();

        // Cleanup our context
        cleanupContext(instance);
    }

    /**
     * Test of getCacheSize method, of class Context.
     */
    /*@Test
    public void testGetCacheSize() {
        // Tested in testClearCache()
    }*/

    /**
     * Test of setSpecialGroup method, of class Context.
     */
    @Test
    public void testSetSpecialGroup() throws SQLException {
        // To test special groups we need a new Context object
        Context instance = new Context();

        // Pass in random integers (need not be valid group IDs)
        UUID groupID1 = UUID.randomUUID();
        UUID groupID2 = UUID.randomUUID();
        instance.setSpecialGroup(groupID1);
        instance.setSpecialGroup(groupID2);

        assertThat("testSetSpecialGroup 0", instance.inSpecialGroup(groupID1), equalTo(true));
        assertThat("testSetSpecialGroup 1", instance.inSpecialGroup(groupID2), equalTo(true));
        assertThat("testSetSpecialGroup 2", instance.inSpecialGroup(UUID.randomUUID()), equalTo(false));

        // Cleanup our context
        cleanupContext(instance);
    }

    /**
     * Test of inSpecialGroup method, of class Context.
     */
    /*@Test
    public void testInSpecialGroup() {
        // tested in testSetSpecialGroup
    }*/

    /**
     * Test of getSpecialGroups method, of class Context.
     */
    @Test
    public void testGetSpecialGroups() throws SQLException, AuthorizeException, IOException {
        // To test special groups we need a new Context object
        Context instance = new Context();

        // Allow full Admin perms (in new context)
        when(authorizeServiceSpy.isAdmin(instance)).thenReturn(true);

        // Create a new group & add it as a special group
        Group group = groupService.create(instance);
        UUID groupID = group.getID();
        instance.setSpecialGroup(groupID);

        // Also add Administrator group as a special group
        Group adminGroup = groupService.findByName(instance, Group.ADMIN);
        UUID adminGroupID = adminGroup.getID();
        instance.setSpecialGroup(adminGroupID);

        // Now get our special groups
        List<Group> specialGroups = instance.getSpecialGroups();
        assertThat("testGetSpecialGroup size", specialGroups.size(), equalTo(2));
        assertThat("testGetSpecialGroup content", specialGroups, hasItems(group, adminGroup));

        // Cleanup our context & group
        groupService.delete(instance, group);
        cleanupContext(instance);
    }

    /**
     * Test of finalize method, of class Context.
     */
    @Test
    public void testFinalize() throws Throwable {
        // We need a new Context object
        Context instance = new Context();

        instance.finalize();

        // Finalize is like abort()...should invalidate our context
        assertThat("testSetSpecialGroup 0", instance.isValid(), equalTo(false));

        // Cleanup our context
        cleanupContext(instance);
    }

    /**
     * Test of updateDatabase method, of class Context.
     */
    @Test
    public void testUpdateDatabase() throws Throwable {
        // We create a new Context object and force the databaseUpdated flag to false
        Context instance = new Context() {
            @Override
            protected void init() {
                super.init();
                databaseUpdated.set(false);
            }
        };

        // Finalize is like abort()...should invalidate our context
        assertThat("updateDatabase 0", Context.updateDatabase(), equalTo(true));

        // Cleanup our context
        cleanupContext(instance);
    }

}
