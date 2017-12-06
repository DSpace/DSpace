/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import mockit.NonStrictExpectations;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.junit.*;
import static org.junit.Assert.* ;
import static org.hamcrest.CoreMatchers.*;

/**
 * Perform some basic unit tests for Context Class
 * @author tdonohue
 */
public class ContextTest extends AbstractUnitTest
{
    /**
     * Test of getDBConnection method, of class Context.
     */
    @Test
    public void testGetDBConnection() throws SQLException
    {
        Connection connection = context.getDBConnection();
        
        assertThat("testGetDBConnection 0", connection, notNullValue());
        assertThat("testGetDBConnection 1", connection.isClosed(), equalTo(false));
    }

    /**
     * Test of setCurrentUser method, of class Context.
     */
    @Test
    public void testSetCurrentUser() throws SQLException, AuthorizeException
    {
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow Admin permissions - needed to create a new EPerson
            AuthorizeManager.isAdmin((Context) any); result = true;
        }};
        
        EPerson oldUser = context.getCurrentUser();
        
        // Create a dummy EPerson to set as current user
        EPerson newUser = EPerson.create(context);
        newUser.setFirstName("Jane");
        newUser.setLastName("Doe");
        newUser.setEmail("jane@email.com");
        newUser.setCanLogIn(true);
        newUser.setLanguage(I18nUtil.getDefaultLocale().getLanguage());
        
        context.setCurrentUser(newUser);
        
        assertThat("testSetCurrentUser 0", context.getCurrentUser(), notNullValue());
        assertThat("testSetCurrentUser 1", context.getCurrentUser(), equalTo(newUser));
        
        // Restore the previous current user
        context.setCurrentUser(oldUser);
    }

    /**
     * Test of getCurrentUser method, of class Context.
     */
    @Test
    public void testGetCurrentUser() 
    {
        //NOTE: 'eperson' is initialized by AbstractUnitTest & set as the "currentUser" there
        assertThat("testGetCurrentUser 0", context.getCurrentUser(), notNullValue());
        assertThat("testGetCurrentUser 1", context.getCurrentUser(), equalTo(eperson));
    }

    /**
     * Test of getCurrentLocale method, of class Context.
     */
    @Test
    public void testGetCurrentLocale() 
    {
        //NOTE: CurrentLocale is not initialized in AbstractUnitTest. So it should be DEFAULTLOCALE
        assertThat("testGetCurrentLocale 0", context.getCurrentLocale(), notNullValue());
        assertThat("testGetCurrentLocale 1", context.getCurrentLocale(), equalTo(I18nUtil.DEFAULTLOCALE));
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
    public void testIgnoreAuthorization() 
    {
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
    public void testSetExtraLogInfo() 
    {
        // Get the previous value
        String oldValue = context.getExtraLogInfo();
       
        // Set a new value
        String newValue = "This is some extra log info";
        context.setExtraLogInfo(newValue);
        
        assertThat("testSetExtraLogInfo 0", context.getExtraLogInfo(), notNullValue());
        assertThat("testSetExtraLogInfo 1", context.getExtraLogInfo(), equalTo(newValue));
    }

    /**
     * Test of getExtraLogInfo method, of class Context.
     */
    @Test
    public void testGetExtraLogInfo() 
    {
        // Extra log info has a default value of "", and AbstractUnitTest doesn't change it
        String defaultValue = "";
        
        assertThat("testGetExtraLogInfo 0", context.getExtraLogInfo(), notNullValue());
        assertThat("testGetExtraLogInfo 1", context.getExtraLogInfo(), equalTo(defaultValue));
    }

    /**
     * Test of complete method, of class Context.
     */
    @Test
    public void testComplete() throws SQLException 
    {
        // To test complete() we need a new Context object
        Context instance = new Context();
        
        // By default, we should have a new DB connection, so let's make sure it is there
        assertThat("testComplete 0", instance.getDBConnection(), notNullValue());
        assertThat("testComplete 1", instance.getDBConnection().isClosed(), equalTo(false));
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
    public void testComplete2() throws SQLException 
    {
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
    public void testCommit() throws Exception 
    {
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow Admin permissions - needed to create a new EPerson
            AuthorizeManager.isAdmin((Context) any); result = true;
        }};
        
        // Create a new EPerson & commit it
        String createdEmail = "jimmy@email.com";
        EPerson newUser = EPerson.create(context);
        newUser.setFirstName("Jimmy");
        newUser.setLastName("Doe");
        newUser.setEmail(createdEmail);
        newUser.setCanLogIn(true);
        newUser.setLanguage(I18nUtil.getDefaultLocale().getLanguage());
        // Ensure EPerson is committed
        newUser.update();
        context.commit();
        
        //Now, open a new context, and see if this eperson can be found!
        Context newInstance = new Context();
        EPerson found = EPerson.findByEmail(newInstance, createdEmail);
        assertThat("testCommit 0", found, notNullValue());

        // Cleanup our new context
        cleanupContext(newInstance);
    }
    
    /**
     * Test of commit method, of class Context.
     */
    @Test(expected=IllegalStateException.class)
    public void testCommitReadOnlyContext() throws Exception
    {
        // Create a read-only Context
        Context instance = new Context(Context.READ_ONLY);

        try
        {
            // Attempt to commit to it - should throw an exception
            instance.commit();
        }
        finally
        {
            // Cleanup our context
            cleanupContext(instance);
        }
    }
    
    /**
     * Test of commit method, of class Context.
     */
    @Test(expected=IllegalStateException.class)
    public void testCommitInvalidContext() throws Exception
    {
        // Create a new Context
        Context instance = new Context();

        // Close context (invalidating it)
        instance.abort();

        try
        {
            // Attempt to commit to it - should throw an exception
            instance.commit();
        }
        finally
        {
            // Cleanup our context
            cleanupContext(instance);
        }
    }

    /**
     * Test of abort method, of class Context.
     */
    @Test
    public void testAbort() throws SQLException, AuthorizeException
    {
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow Admin permissions - needed to create a new EPerson
            AuthorizeManager.isAdmin((Context) any); result = true;
        }};
        
        // To test abort() we need a new Context object
        Context instance = new Context();
        
        // Create a new EPerson (DO NOT COMMIT IT)
        String createdEmail = "susie@email.com";
        EPerson newUser = EPerson.create(instance);
        newUser.setFirstName("Susan");
        newUser.setLastName("Doe");
        newUser.setEmail(createdEmail);
        newUser.setCanLogIn(true);
        newUser.setLanguage(I18nUtil.getDefaultLocale().getLanguage());
        
        // Abort our context
        instance.abort();
        // Ensure the context is no longer valid
        assertThat("testAbort 0", instance.isValid(), equalTo(false));
        
        // Open a new context, let's make sure that EPerson isn't there
        Context newInstance = new Context();
        EPerson found = EPerson.findByEmail(newInstance, createdEmail);
        assertThat("testAbort 1", found, nullValue());

        // Cleanup our contexts
        cleanupContext(instance);
        cleanupContext(newInstance);
    }
    
    /**
     * Test of abort method, of class Context.
     */
    @Test
    public void testAbort2() throws SQLException 
    {
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
    public void testIsReadOnly() throws SQLException
    {
        // Our default context should NOT be read only
        assertThat("testIsReadOnly 0", context.isReadOnly(), equalTo(false));
        
        // Create a new read-only context
        Context instance = new Context(Context.READ_ONLY);
        assertThat("testIsReadOnly 1", instance.isReadOnly(), equalTo(true));

        // Cleanup our context
        cleanupContext(instance);
    }

    /**
     * Test of fromCache method, of class Context.
     */
    @Test
    public void testFromCache() throws SQLException, AuthorizeException
    {
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow Admin permissions - needed to create a new EPerson
            AuthorizeManager.isAdmin((Context) any); result = true;
        }};
        
        // To test caching we need a new Context object
        Context instance = new Context();
        
        // Create a new Eperson object
        EPerson newEperson = EPerson.create(instance);
        newEperson.setFirstName("Sam");
        newEperson.setLastName("Smith");
        newEperson.setEmail("sammy@smith.com");
        newEperson.setCanLogIn(true);
        newEperson.setLanguage(I18nUtil.getDefaultLocale().getLanguage());
        
        // Cache the object
        instance.cache(newEperson, newEperson.getID());
        
        // Now, pull the object out of the cache
        EPerson fromCache = (EPerson) instance.fromCache(EPerson.class, newEperson.getID());
        assertThat("testFromCache 0", fromCache, notNullValue());
        assertThat("testFromCache 1", fromCache, equalTo(newEperson));

        // Cleanup our context
        cleanupContext(instance);
    }

    /**
     * Test of cache method, of class Context.
     */
    @Test
    public void testCache() throws SQLException
    {
        // To test caching we need a new Context object
        Context instance = new Context();
        
        // Create a simple object to cache
        String cacheMe = "Look for me in your local cache!";
        int cacheMeID = 9999999;
        
        // Cache the object
        instance.cache(cacheMe, cacheMeID);
        
        // Now, can we get it back?
        String fromCache = (String) instance.fromCache(String.class, cacheMeID);
        assertThat("testCache 0", fromCache, notNullValue());
        assertThat("testCache 1", fromCache, equalTo(cacheMe));

        // Cleanup our context
        cleanupContext(instance);
    }

    /**
     * Test of removeCached method, of class Context.
     */
    @Test
    public void testRemoveCached() throws SQLException
    {
        // To test caching we need a new Context object
        Context instance = new Context();
        
        // Create a simple object to cache
        String cacheMe = "Look for me in your local cache!";
        int cacheMeID = 9999999;
        
        // Cache the object
        instance.cache(cacheMe, cacheMeID);
        
        // Can we get it back?
        String fromCache = (String) instance.fromCache(String.class, cacheMeID);
        assertThat("testRemoveCache 0", fromCache, notNullValue());
        assertThat("testRemoveCache 1", fromCache, equalTo(cacheMe));
        
        // Now, can we remove it?
        instance.removeCached(cacheMe, cacheMeID);
        assertThat("testRemoveCache 3", instance.fromCache(String.class, cacheMeID), nullValue());

        // Cleanup our context
        cleanupContext(instance);
    }

    /**
     * Test of clearCache method, of class Context.
     */
    @Test
    public void testClearCache() throws SQLException
    {
        // To test caching we need a new Context object
        Context instance = new Context();
        
        // Create a simple object to cache
        String cacheMe = "Look for me in your local cache!";
        int cacheMeID = 9999999;
        
        // Cache the object
        instance.cache(cacheMe, cacheMeID);
        
         // Ensure cache is non-empty
        assertThat("testClearCache 0", instance.getCacheSize(), equalTo(1));
        
        // Clear our cache
        instance.clearCache();
        
        // Ensure cache is empty
        assertThat("testClearCache 1", instance.getCacheSize(), equalTo(0));

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
    public void testSetSpecialGroup() throws SQLException
    {
        // To test special groups we need a new Context object
        Context instance = new Context();
        
        // Pass in random integers (need not be valid group IDs)
        instance.setSpecialGroup(10000);
        instance.setSpecialGroup(10001);
        
        assertThat("testSetSpecialGroup 0", instance.inSpecialGroup(10000), equalTo(true));
        assertThat("testSetSpecialGroup 1", instance.inSpecialGroup(10001), equalTo(true));
        assertThat("testSetSpecialGroup 2", instance.inSpecialGroup(20000), equalTo(false));

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
    public void testGetSpecialGroups() throws SQLException, AuthorizeException
    {
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow Admin permissions - needed to create a new Group
            AuthorizeManager.isAdmin((Context) any); result = true;
        }};
        
        // To test special groups we need a new Context object
        Context instance = new Context();
        
        // Create a new group & add it as a special group
        Group group = Group.create(instance);
        int groupID = group.getID();
        instance.setSpecialGroup(groupID);
        
        // Also add Administrator group as a special group
        Group adminGroup = Group.find(instance, Group.ADMIN_ID);
        int adminGroupID = adminGroup.getID();
        instance.setSpecialGroup(adminGroupID);
        
        // Now get our special groups
        Group[] specialGroups = instance.getSpecialGroups();
        assertThat("testGetSpecialGroup 0", specialGroups.length, equalTo(2));
        assertThat("testGetSpecialGroup 1", specialGroups[0], equalTo(group));
        assertThat("testGetSpecialGroup 1", specialGroups[1], equalTo(adminGroup));

        // Cleanup our context
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
}
