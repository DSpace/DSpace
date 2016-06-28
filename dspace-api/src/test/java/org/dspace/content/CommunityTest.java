/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.content;

import java.io.File;
import java.io.FileInputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.junit.*;
import static org.junit.Assert.* ;
import static org.hamcrest.CoreMatchers.*;
import mockit.NonStrictExpectations;

/**
 * Unit Tests for class Community
 * @author pvillega
 */
public class CommunityTest extends AbstractDSpaceObjectTest
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(CommunityTest.class);

    /**
     * Community instance for the tests
     */
    private Community c;

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    @Override
    public void init()
    {
        super.init();
        try
        {   
            // we have to create a new community in the database
            // and only Admins can create a top-level community
            context.turnOffAuthorisationSystem();
            this.c = communityService.create(null, context);
            this.dspaceObject = c;
            //we need to commit the changes so we don't block the table for testing
            context.restoreAuthSystemState();
        }
        catch (AuthorizeException ex)
        {
            log.error("Authorization Error in init", ex);
            fail("Authorization Error in init: " + ex.getMessage());
        }
        catch (SQLException ex)
        {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init: " + ex.getMessage());
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
    @Override
    public void destroy()
    {
        c = null;
        super.destroy();
    }

    /**
     * Test of find method, of class Community.
     */
    @Test
    public void testCommunityFind() throws Exception
    {
        UUID id = c.getID();
        Community found =  communityService.find(context, id);
        assertThat("testCommunityFind 0", found, notNullValue());
        assertThat("testCommunityFind 1", found.getID(), equalTo(id));
        //the community created by default has no name
        assertThat("testCommunityFind 2", found.getName(), equalTo(""));
    }

    /**
     * Test of create method, of class Community.
     */
    @Test
    public void testCreateAuth() throws Exception
    {
        //Default to Community-Admin Rights (but not full Admin rights)
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow parent Community ADD perms (needed for addSubCommunity functionality)
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.ADD); result = null;
            // Allow current Community ADD perms (needed to just create community)
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD); result = true;
            // Disallow full Admin perms
            authorizeService.isAdmin((Context) any); result = false;
        }};

        // test that a Community Admin can create a Community with parent (Sub-Community)
        Community sub = communityService.create(c, context);
        //the item created by default has no name set
        assertThat("testCreate 2", sub, notNullValue());
        assertThat("testCreate 3", sub.getName(), equalTo(""));
        assertTrue("testCreate 4", communityService.getAllParents(context, sub).size() == 1);
        assertThat("testCreate 5", communityService.getAllParents(context, sub).get(0), equalTo(c));
    }


     /**
     * Test of create method, of class Community.
     */
    @Test
    public void testCreateAuth2() throws Exception
    {
        //Default to Admin Rights, but NOT Community Admin Rights
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD); result = false;
            // Allow full Admin perms
            authorizeService.isAdmin((Context) any); result = true;
        }};

        //Test that a full Admin can create a Community without a parent (Top-Level Community)
        Community created = communityService.create(null, context);
        //the item created by default has no name set
        assertThat("testCreate 0", created, notNullValue());
        assertThat("testCreate 1", created.getName(), equalTo(""));

        //Test that a full Admin can also create a Community with a parent (Sub-Community)
        Community son = communityService.create(created, context);
        //the item created by default has no name set
        assertThat("testCreate 2", son, notNullValue());
        assertThat("testCreate 3", son.getName(), equalTo(""));
        assertTrue("testCreate 4", communityService.getAllParents(context, son).size() == 1);
        assertThat("testCreate 5", communityService.getAllParents(context, son).get(0), equalTo(created));
    }

    /**
     * Test of create method, of class Community.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateNoAuth() throws Exception
    {
        //Default to NO Admin Rights, and NO Community Admin Rights
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD); result = false;
            // Disallow full Admin perms
            authorizeService.isAdmin((Context) any); result = false;
        }};

        // test creating community with no parent (as a non-admin & non-Community Admin)
        // this should throw an exception
        Community created = communityService.create(null, context);
        fail("Exception expected");
    }

     /**
     * Test of create method, of class Community.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateNoAuth2() throws Exception
    {
        //Default to Community-Admin Rights (but not full Admin rights)
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD); result = true;
            // Disallow full Admin perms
            authorizeService.isAdmin((Context) any); result = false;
        }};

        // test creating community with no parent (as a non-admin, but with Community Admin rights)
        // this should throw an exception, as only admins can create Top Level communities
        Community created = communityService.create(null, context);
        fail("Exception expected");
    }
    
    /**
     * Test of create method (with specified valid handle), of class Community.
     */
    @Test
    public void testCreateWithValidHandle() throws Exception
    {
        //Default to Admin Rights, but NOT Community Admin Rights
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD); result = false;
            // Allow full Admin perms
            authorizeService.isAdmin((Context) any); result = true;
        }};

        // test creating community with a specified handle which is NOT already in use
        // (this handle should not already be used by system, as it doesn't start with "1234567689" prefix)
        Community created = communityService.create(null, context, "987654321/100c");

        // check that community was created, and that its handle was set to proper value
        assertThat("testCreateWithValidHandle 0", created, notNullValue());
        assertThat("testCreateWithValidHandle 1", created.getHandle(), equalTo("987654321/100c"));
    }
    
    
     /**
     * Test of create method (with specified invalid handle), of class Community.
     */
    @Test(expected=IllegalStateException.class)
    public void testCreateWithInvalidHandle() throws Exception
    {
        //Default to Admin Rights, but NOT Community Admin Rights
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD); result = false;
            // Allow full Admin perms
            authorizeService.isAdmin((Context) any); result = true;
        }};

        //get handle of our default created community
        String inUseHandle = c.getHandle();

        // test creating community with a specified handle which IS already in use
        // This should throw an exception
        Community created = communityService.create(null, context, inUseHandle);
        fail("Exception expected");
    }

    /**
     * Test of findAll method, of class Community.
     */
    @Test
    public void testFindAll() throws Exception
    {
        List<Community> all = communityService.findAll(context);
        assertThat("testFindAll 0", all, notNullValue());
        assertTrue("testFindAll 1", all.size() >= 1);

        boolean added = false;
        for(Community cm: all)
        {
            if(cm.equals(c))
            {
                added = true;
            }
        }
        assertTrue("testFindAll 2",added);
    }

    /**
     * Test of findAllTop method, of class Community.
     */
    @Test
    public void testFindAllTop() throws Exception
    {
        List<Community> all = communityService.findAllTop(context);
        assertThat("testFindAllTop 0", all, notNullValue());
        assertTrue("testFindAllTop 1", all.size() >= 1);
        for(Community cm: all)
        {
            assertThat("testFindAllTop for", communityService.getAllParents(context, cm).size(), equalTo(0));
        }

        boolean added = false;
        for(Community cm: all)
        {
            if(cm.equals(c))
            {
                added = true;
            }
        }
        assertTrue("testFindAllTop 2",added);
    }

    /**
     * Test of getID method, of class Community.
     */
    @Test
    @Override
    public void testGetID()
    {
        assertTrue("testGetID 0", c.getID() != null);
    }

    @Test
    public void testLegacyID() { assertTrue("testGetLegacyID 0", c.getLegacyId() == null);}
    
    /**
     * Test of getHandle method, of class Community.
     */
    @Test
    @Override
    public void testGetHandle() 
    {
        //default instance has a random handle
        assertTrue("testGetHandle 0", c.getHandle().contains("123456789/"));
    }

    /**
     * Test of getMetadata method, of class Community.
     */
    @Test
    public void testGetMetadata()
    {
        //by default all empty values will return ""
        assertThat("testGetMetadata 0",communityService.getMetadata(c, "name"), equalTo(""));
        assertThat("testGetMetadata 1",communityService.getMetadata(c, "short_description"), equalTo(""));
        assertThat("testGetMetadata 2",communityService.getMetadata(c, "introductory_text"), equalTo(""));
        assertThat("testGetMetadata 4",communityService.getMetadata(c, "copyright_text"), equalTo(""));
        assertThat("testGetMetadata 5",communityService.getMetadata(c, "side_bar_text"), equalTo(""));
    }

    /**
     * Test of setMetadata method, of class Community.
     */
    @Test
    public void testSetMetadata() throws SQLException 
    {
        String name = "name";
        String sdesc = "short description";
        String itext = "introductory text";
        String copy = "copyright declaration";
        String sidebar = "side bar text";

        communityService.setMetadata(context, c, "name", name);
        communityService.setMetadata(context, c, "short_description", sdesc);
        communityService.setMetadata(context, c, "introductory_text", itext);
        communityService.setMetadata(context, c, "copyright_text", copy);
        communityService.setMetadata(context, c, "side_bar_text", sidebar);

        assertThat("testSetMetadata 0",communityService.getMetadata(c, "name"), equalTo(name));
        assertThat("testSetMetadata 1",communityService.getMetadata(c, "short_description"), equalTo(sdesc));
        assertThat("testSetMetadata 2",communityService.getMetadata(c, "introductory_text"), equalTo(itext));
        assertThat("testSetMetadata 4",communityService.getMetadata(c, "copyright_text"), equalTo(copy));
        assertThat("testSetMetadata 5",communityService.getMetadata(c, "side_bar_text"), equalTo(sidebar));
    }

    /**
     * Test of getName method, of class Community.
     */
    @Test
    @Override
    public void testGetName()
    {
        //by default is empty
        assertThat("testGetName 0",c.getName(), equalTo(""));
    }

    /**
     * Test of getLogo method, of class Community.
     */
    @Test
    public void testGetLogo()
    {
       //by default is empty
       assertThat("testGetLogo 0",c.getLogo(), nullValue());
    }

    /**
     * Test of setLogo method, of class Community.
     */
    @Test
    public void testSetLogoAuth() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD); result = true;
            // Allow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE); result = true;
            // Allow current Community WRITE perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.WRITE); result = null;
        }};

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream logo = communityService.setLogo(context, c, new FileInputStream(f));
        assertThat("testSetLogoAuth 0",c.getLogo(), equalTo(logo));

        c.setLogo(null);
        assertThat("testSetLogoAuth 1",c.getLogo(), nullValue());
    }

    /**
     * Test of setLogo method, of class Community.
     */
    @Test
    public void testSetLogoAuth2() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD); result = false;
            // Allow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE); result = true;
            // Allow current Community WRITE perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.WRITE); result = null;
        }};

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream logo = communityService.setLogo(context, c, new FileInputStream(f));
        assertThat("testSetLogoAuth2 0",c.getLogo(), equalTo(logo));

        c.setLogo(null);
        assertThat("testSetLogoAuth2 1",c.getLogo(), nullValue());
    }

    /**
     * Test of setLogo method, of class Community.
     */
    @Test
    public void testSetLogoAuth3() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD); result = true;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE); result = false;
            // Allow current Community WRITE perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.WRITE); result = null;
        }};

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream logo = communityService.setLogo(context, c, new FileInputStream(f));
        assertThat("testSetLogoAuth3 0",c.getLogo(), equalTo(logo));

        c.setLogo(null);
        assertThat("testSetLogoAuth3 1",c.getLogo(), nullValue());
    }

    /**
     * Test of setLogo method, of class Community.
     */
    @Test
    public void testSetLogoAuth4() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD); result = false;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE); result = false;
            // Allow current Community WRITE perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.WRITE); result = null;
        }};

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream logo = communityService.setLogo(context, c, new FileInputStream(f));
        assertThat("testSetLogoAuth4 0",c.getLogo(), equalTo(logo));

        c.setLogo(null);
        assertThat("testSetLogoAuth4 1",c.getLogo(), nullValue());
    }

    /**
     * Test of setLogo method, of class Community.
     */
    @Test(expected=AuthorizeException.class)
    public void testSetLogoNoAuth() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD); result = false;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE); result = false;
            // Disallow current Community WRITE perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.WRITE); result = new AuthorizeException();
        }};

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream logo = communityService.setLogo(context, c, new FileInputStream(f));
        fail("EXception expected");
    }

    /**
     * Test of update method, of class Community.
     */
    @Test(expected=AuthorizeException.class)
    public void testUpdateNoAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow current Community ADD perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.ADD); result = new AuthorizeException();
            // Disallow current Community WRITE perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.WRITE); result = new AuthorizeException();
        }};

        //TODO: we need to verify the update, how?
        communityService.update(context, c);
        fail("Exception must be thrown");
    }

    /**
     * Test of update method, of class Community.
     */
    @Test
    public void testUpdateAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow current Community ADD perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.ADD); result = null;
            // Allow current Community WRITE perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.WRITE); result = null;
        }};

        //TODO: we need to verify the update, how?
        communityService.update(context, c);
    }

    /**
     * Test of createAdministrators method, of class Community.
     */
    @Test
    public void testCreateAdministratorsAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeUtil.class)
        {{
            // Allow manage AdminGroup perms
            AuthorizeUtil.authorizeManageAdminGroup((Context) any, (Community) any);
                result = null;
        }};

        Group result = communityService.createAdministrators(context, c);
        assertThat("testCreateAdministratorsAuth 0",c.getAdministrators(), notNullValue());
        assertThat("testCreateAdministratorsAuth 1",c.getAdministrators(), equalTo(result));
    }

    /**
     * Test of createAdministrators method, of class Community.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateAdministratorsNoAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeUtil.class)
        {{
            // Disallow manage AdminGroup perms
            AuthorizeUtil.authorizeManageAdminGroup((Context) any, (Community) any);
                result = new AuthorizeException();
           
        }};

        Group result = communityService.createAdministrators(context, c);
        fail("Exception should have been thrown");
    }


    /**
     * Test of removeAdministrators method, of class Community.
     */
    @Test
    public void testRemoveAdministratorsAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeUtil.class)
        {{
            // Allow manage AdminGroup perms (needed to possibly create group first)
            AuthorizeUtil.authorizeManageAdminGroup((Context) any, (Community) any);
                result = null;
            // Allow remove AdminGroup perms
            AuthorizeUtil.authorizeRemoveAdminGroup((Context) any, (Community) any);
                result = null;
        }};

        // Ensure admin group is created first
        Group result = communityService.createAdministrators(context, c);
        assertThat("testRemoveAdministratorsAuth 0",c.getAdministrators(), notNullValue());
        assertThat("testRemoveAdministratorsAuth 1",c.getAdministrators(), equalTo(result));
        communityService.removeAdministrators(context, c);
        assertThat("testRemoveAdministratorsAuth 2",c.getAdministrators(), nullValue());
    }

    /**
     * Test of removeAdministrators method, of class Community.
     */
    @Test(expected=AuthorizeException.class)
    public void testRemoveAdministratorsNoAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeUtil.class)
        {{
            // Allow manage AdminGroup perms (needed to possibly create group first)
            AuthorizeUtil.authorizeManageAdminGroup((Context) any, (Community) any);
                result = null;
            // Disallow remove AdminGroup perms
            AuthorizeUtil.authorizeRemoveAdminGroup((Context) any, (Community) any);
                result = new AuthorizeException();
        }};

        // Ensure admin group is created first
        Group result = communityService.createAdministrators(context, c);
        assertThat("testRemoveAdministratorsAuth 0",c.getAdministrators(), notNullValue());
        assertThat("testRemoveAdministratorsAuth 1",c.getAdministrators(), equalTo(result));
        communityService.removeAdministrators(context, c);
        fail("Should have thrown exception");
    }

    /**
     * Test of getAdministrators method, of class Community.
     */
    @Test
    public void testGetAdministrators() 
    {
        //null by default
        assertThat("testGetAdministrators 0",c.getAdministrators(), nullValue());
    }

    /**
     * Test of getCollections method, of class Community.
     */
    @Test
    public void testGetCollections() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow current Community ADD perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.ADD); result = null;
            // Allow current Community WRITE perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.WRITE); result = null;
        }};

        //empty by default
        assertThat("testGetCollections 0",c.getCollections(), notNullValue());
        assertTrue("testGetCollections 1", c.getCollections().size() == 0);

        Collection result = collectionService.create(context, c);
        assertThat("testGetCollections 2",c.getCollections(), notNullValue());
        assertTrue("testGetCollections 3", c.getCollections().size() == 1);
        assertThat("testGetCollections 4",c.getCollections().get(0), equalTo(result));
    }

    /**
     * Test of getSubcommunities method, of class Community.
     */
    @Test
    public void testGetSubcommunities() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow current Community ADD perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.ADD); result = null;
            // Allow *parent* Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD); result = true;
        }};

        //empty by default
        assertThat("testGetSubcommunities 0",c.getSubcommunities(), notNullValue());
        assertTrue("testGetSubcommunities 1", c.getSubcommunities().size() == 0);

        //community with parent
        Community son = communityService.create(c, context);
        assertThat("testGetSubcommunities 2",c.getSubcommunities(), notNullValue());
        assertTrue("testGetSubcommunities 3", c.getSubcommunities().size() == 1);
        assertThat("testGetSubcommunities 4", c.getSubcommunities().get(0), equalTo(son));
    }

    /**
     * Test of getParentCommunity method, of class Community.
     */
    @Test
    public void testGetParentCommunity() throws Exception 
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow current Community ADD perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.ADD); result = null;
            // Allow *parent* Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD); result = true;
        }};

        //null by default
        assertEquals("testGetParentCommunity 0", c.getParentCommunities().size(), 0);

        //community with  parent
        Community son = communityService.create(c, context);
        assertThat("testGetParentCommunity 1",son.getParentCommunities().size(), not(0));
        assertThat("testGetParentCommunity 2", son.getParentCommunities().get(0), equalTo(c));
    }

    /**
     * Test of getAllParents method, of class Community.
     */
    @Test
    public void testGetAllParents() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow current Community ADD perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.ADD); result = null;
            // Allow *parent* Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD); result = true;
        }};

        //empty by default
        assertThat("testGetAllParents 0",communityService.getAllParents(context, c), notNullValue());
        assertTrue("testGetAllParents 1", communityService.getAllParents(context, c).size() == 0);

        //community with  parent
        Community son = communityService.create(c, context);
        assertThat("testGetAllParents 2",communityService.getAllParents(context, son), notNullValue());
        assertTrue("testGetAllParents 3", communityService.getAllParents(context, son).size() == 1);
        assertThat("testGetAllParents 4", communityService.getAllParents(context, son).get(0), equalTo(c));
    }

    /**
     * Test of getAllCollections method, of class Community.
     */
    @Test
    public void testGetAllCollections() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow current Community ADD perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.ADD); result = null;
            // Allow *parent* Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD); result = true;
        }};

        //empty by default
        assertThat("testGetAllCollections 0",communityService.getAllCollections(context, c), notNullValue());
        assertTrue("testGetAllCollections 1", communityService.getAllCollections(context, c).size() == 0);

        //community has a collection and a subcommunity, subcommunity has a collection
        Collection collOfC = collectionService.create(context, c);
        Community sub = communityService.create(c, context);
        Collection collOfSub = collectionService.create(context, sub);
        assertThat("testGetAllCollections 2",communityService.getAllCollections(context, c), notNullValue());
        assertTrue("testGetAllCollections 3", communityService.getAllCollections(context, c).size() == 2);
        assertThat("testGetAllCollections 4", communityService.getAllCollections(context, c).get(0), equalTo(collOfSub));
        assertThat("testGetAllCollections 5", communityService.getAllCollections(context, c).get(1), equalTo(collOfC));
    }

    /**
     * Test of createCollection method, of class Community.
     */
    @Test
    public void testCreateCollectionAuth() throws Exception
    {
        // Need current Community ADD permissions in order to create a Collection
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow current Community ADD perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.ADD); result = null;
        }};

        Collection result = collectionService.create(context, c);
        assertThat("testCreateCollectionAuth 0", result, notNullValue());
        assertThat("testCreateCollectionAuth 1", c.getCollections(), notNullValue());
        assertThat("testCreateCollectionAuth 2", c.getCollections().get(0), equalTo(result));
    }

    /**
     * Test of createCollection method, of class Community.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateCollectionNoAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow current Community ADD perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.ADD); result = new AuthorizeException();
        }};

        Collection result = collectionService.create(context, c);
        fail("Exception expected");
    }

    /**
     * Test of addCollection method, of class Community.
     */
    @Test
    public void testAddCollectionAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow current Community ADD perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.ADD); result = null;
        }};

        Collection col = collectionService.create(context, c);
        c.addCollection(col);
        assertThat("testAddCollectionAuth 0", c.getCollections(), notNullValue());
        assertThat("testAddCollectionAuth 1", c.getCollections().get(0), equalTo(col));
    }

    /**
     * Test of addCollection method, of class Community.
     */
    @Test(expected=AuthorizeException.class)
    public void testAddCollectionNoAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow current Community ADD perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.ADD); result = new AuthorizeException();
        }};

        Collection col = collectionService.create(context, c);
        c.addCollection(col);
        fail("Exception expected");
    }

    /**
     * Test of createSubcommunity method, of class Community.
     */
    @Test
    public void testCreateSubcommunityAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow current Community ADD perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.ADD); result = null;
            // Allow *parent* Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD); result = true;
        }};

        Community result = communityService.createSubcommunity(context, c);
        assertThat("testCreateSubcommunityAuth 0",c.getSubcommunities(), notNullValue());
        assertTrue("testCreateSubcommunityAuth 1", c.getSubcommunities().size() == 1);
        assertThat("testCreateSubcommunityAuth 2", c.getSubcommunities().get(0), equalTo(result));
    }

    /**
     * Test of createSubcommunity method, of class Community.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateSubcommunityNoAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow current Community ADD perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.ADD); result = new AuthorizeException();
            // Allow *parent* Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD); result = true;
        }};

        Community result = communityService.createSubcommunity(context, c);
        fail("Exception expected");
    }

    /**
     * Test of addSubcommunity method, of class Community.
     */
    @Test
    public void testAddSubcommunityAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow current Community ADD perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.ADD); result = null;
            // Allow *parent* Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD); result = true;
        }};

        // Turn off authorization temporarily to create a new top-level community
        context.turnOffAuthorisationSystem();
        Community result = communityService.create(null,context);
        context.restoreAuthSystemState();

        communityService.addSubcommunity(context, c, result);
        assertThat("testAddSubcommunityAuth 0",c.getSubcommunities(), notNullValue());
        assertTrue("testAddSubcommunityAuth 1", c.getSubcommunities().size() == 1);
        assertThat("testAddSubcommunityAuth 2", c.getSubcommunities().get(0), equalTo(result));
    }

    /**
     * Test of addSubcommunity method, of class Community.
     */
    @Test(expected=AuthorizeException.class)
    public void testAddSubcommunityNoAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow current Community ADD perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.ADD); result = new AuthorizeException();
            // Allow *parent* Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD); result = true;
        }};

        Community result = communityService.create(null, context);
        communityService.addSubcommunity(context, c, result);
        fail("Exception expected");
    }

    /**
     * Test of removeCollection method, of class Community.
     */
    @Test
    public void testRemoveCollectionAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass(), AuthorizeUtil.class)
        {{
            // Allow current Community ADD perms (to add Collection)
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.ADD); result = null;
            // Allow current Community REMOVE perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.REMOVE); result = null;
            // Allow Collection ManageTemplateItem perms (needed to delete Collection)
            AuthorizeUtil.authorizeManageTemplateItem((Context) any, (Collection) any);
                    result = null;
            // Allow Collection WRITE perms (needed to delete Collection)
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = null;
        }};

        Collection col = collectionService.create(context, c);
        assertThat("testRemoveCollectionAuth 0", c.getCollections(), notNullValue());
        assertTrue("testRemoveCollectionAuth 1", c.getCollections().size() == 1);
        assertThat("testRemoveCollectionAuth 2", c.getCollections().get(0), equalTo(col));
        
        c.removeCollection(col);
        assertThat("testRemoveCollectionAuth 3", c.getCollections(), notNullValue());
        assertTrue("testRemoveCollectionAuth 4", c.getCollections().size() == 0);
    }

    /**
     * Test of removeCollection method, of class Community.
     */
    @Test(expected=AuthorizeException.class)
    public void testRemoveCollectionNoAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow current Community ADD perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.ADD); result = null;
            // Disallow current Community REMOVE perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.REMOVE); result = new AuthorizeException();
        }};

        Collection col = collectionService.create(context, c);
        assertThat("testRemoveCollectionNoAuth 0", c.getCollections(), notNullValue());
        assertTrue("testRemoveCollectionNoAuth 1", c.getCollections().size() == 1);
        assertThat("testRemoveCollectionNoAuth 2", c.getCollections().get(0), equalTo(col));

        communityService.removeCollection(context, c, col);
        fail("Exception expected");
    }

    /**
     * Test of removeSubcommunity method, of class Community.
     */
    @Test
    public void testRemoveSubcommunityAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Community ADD perms (in order to add a new subcommunity to parent)
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.ADD, true); result = null;
            // Allow Community REMOVE perms (needed to unmap/remove subcommunity)
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.REMOVE, true); result = null;
            // Allow Community DELETE perms (needed to actually delete subcommunity)
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.DELETE, true); result = null;
        }};

        // Turn off authorization temporarily to create a new top-level community
        context.turnOffAuthorisationSystem();
        Community com = communityService.create(null,context);
        context.restoreAuthSystemState();

        communityService.addSubcommunity(context, c, com);
        assertThat("testRemoveSubcommunityAuth 0", c.getSubcommunities(), notNullValue());
        assertTrue("testRemoveSubcommunityAuth 1", c.getSubcommunities().size() == 1);
        assertThat("testRemoveSubcommunityAuth 2", c.getSubcommunities().get(0), equalTo(com));

        communityService.removeSubcommunity(context, c, com);
        assertThat("testRemoveSubcommunityAuth 3", c.getSubcommunities(), notNullValue());
        assertTrue("testRemoveSubcommunityAuth 4", c.getSubcommunities().size() == 0);
    }

    /**
     * Test of delete method, of class Community.
     */
    @Test
    public void testDeleteAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow current Community ADD perms (to create content to be deleted)
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.ADD); result = null;
            // Allow current Community WRITE perms (to create content to be deleted)
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.WRITE); result = null;
            // Allow current Community DELETE perms (needed to delete community)
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.DELETE); result = null;
            // Disallow *parent* Community REMOVE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.REMOVE); result = false;
        }};

        // Turn off authorization temporarily to create a new top-level community
        context.turnOffAuthorisationSystem();
        Community todelete = communityService.create(null,context);
        context.restoreAuthSystemState();

        // Now, test deletion
        UUID id = todelete.getID();
        communityService.delete(context, todelete);
        Community found = communityService.find(context, id);
        assertThat("testDeleteAuth 0",found, nullValue());
    }

    /**
     * Test of delete method, of class Community.
     */
    @Test
    public void testDeleteAuth2() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow current Community ADD perms (to create content to be deleted)
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.ADD); result = null;
            // Allow current Community WRITE perms (to create content to be deleted)
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.WRITE); result = null;
            // Allow current Community DELETE perms (needed to delete community)
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.DELETE); result = null;
            // Allow *parent* Community REMOVE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.REMOVE, true); result = true;
        }};

        // Turn off authorization temporarily to create a new top-level community
        context.turnOffAuthorisationSystem();
        Community todelete = communityService.create(null,context);
        context.restoreAuthSystemState();

        // Now, test deletion
        UUID id = todelete.getID();
        communityService.delete(context, todelete);
        Community found = communityService.find(context, id);
        assertThat("testDeleteAuth2 0",found, nullValue());
    }

    /**
     * Test of delete method, of class Community, using a hierarchy of Communities and Collections.
     */
    @Test
    public void testDeleteHierachyAuth() throws Exception
    {
        System.out.println("testDeleteHierarchyAuth");
        new NonStrictExpectations(authorizeService.getClass(), AuthorizeUtil.class)
        {{
            // Allow current Community DELETE perms (needed to delete a community)
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.DELETE, true); result = null;
            // Allow current Community REMOVE perms (needed to remove a sub-community from a community)
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.REMOVE, true); result = null;
            // Allow Collection ManageTemplateItem perms (needed to delete a collection)
            AuthorizeUtil.authorizeManageTemplateItem((Context) any, (Collection) any);
                    result = null;
            // Allow current Collection DELETE perms (needed to delete a Collection)
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.DELETE, true); result = null;
            // Allow current Item WRITE perms (needed to remove identifiers from an Item prior to deletion)
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.WRITE, true); result = null;
        }};

        // Create a dummy Community hierarchy to test delete with
        // Turn off authorization temporarily to create some test objects.
        context.turnOffAuthorisationSystem();
        Community parent = communityService.create(null,context);

        // Create a hierachy of sub-Communities and Collections and Items.
        Community child = communityService.createSubcommunity(context, parent);
        Community grandchild = communityService.createSubcommunity(context, child);
        Collection childCol = collectionService.create(context, child);
        Collection grandchildCol = collectionService.create(context, grandchild);
        // Create two separate items
        WorkspaceItem wsItem = workspaceItemService.create(context, childCol, false);
        Item item = installItemService.installItem(context, wsItem);
        wsItem = workspaceItemService.create(context, childCol, false);
        item = installItemService.installItem(context, wsItem);

        // Done creating the objects. Turn auth system back on
        context.restoreAuthSystemState();

        // Now, test hierarchical deletion
        UUID parentId = parent.getID();
        UUID childId = child.getID();
        UUID grandchildId = grandchild.getID();
        UUID childColId = childCol.getID();
        UUID grandchildColId = grandchildCol.getID();
        UUID itemId = item.getID();

        // Delete the parent of this entire hierarchy
        communityService.delete(context, parent);

        // Test that everything created here is deleted.
        assertThat("top-level Community not deleted",
                communityService.find(context, parentId), nullValue());
        assertThat("child Community not deleted",
                communityService.find(context, childId), nullValue());
        assertThat("grandchild Community not deleted",
                communityService.find(context, grandchildId), nullValue());
        assertThat("Collection of child Community not deleted",
                collectionService.find(context, childColId), nullValue());
        assertThat("Collection of grandchild Community not deleted",
                collectionService.find(context, grandchildColId), nullValue());
        assertThat("Item not deleted",
                itemService.find(context, itemId), nullValue());
    }

    /**
     * Test of delete method, of class Community.
     */
    @Test(expected=AuthorizeException.class)
    public void testDeleteNoAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow current Community DELETE perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.DELETE); result = new AuthorizeException();
            // Disallow *parent* Community REMOVE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.REMOVE); result = false;
        }};

        communityService.delete(context, c);
        fail("Exception expected");
    }

    /**
     * Test of equals method, of class Community.
     */
    @Test
    @SuppressWarnings("ObjectEqualsNull")
    public void testEquals() throws SQLException, AuthorizeException
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow full Admin perms (just to create top-level community)
            authorizeService.isAdmin((Context) any); result = true;
        }};

        assertFalse("testEquals 0",c.equals(null));
        assertFalse("testEquals 1",c.equals(communityService.create(null, context)));
        assertTrue("testEquals 2", c.equals(c));
    }

    /**
     * Test of getType method, of class Community.
     */
    @Test
    @Override
    public void testGetType()
    {
        assertThat("testGetType 0", c.getType(), equalTo(Constants.COMMUNITY));
    }

    /**
     * Test of canEditBoolean method, of class Community.
     */
    @Test
    public void testCanEditBooleanAuth() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD); result = true;
            // Allow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE); result = true;
            // Allow current Community WRITE perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.WRITE); result = null;
        }};

        assertTrue("testCanEditBooleanAuth 0", communityService.canEditBoolean(context, c));
    }

    /**
     * Test of canEditBoolean method, of class Community.
     */
    @Test
    public void testCanEditBooleanAuth2() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD); result = false;
            // Allow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE); result = true;
            // Allow current Community WRITE perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.WRITE); result = null;
        }};

        assertTrue("testCanEditBooleanAuth2 0", communityService.canEditBoolean(context, c));
    }

    /**
     * Test of canEditBoolean method, of class Community.
     */
    @Test
    public void testCanEditBooleanAuth3() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD); result = true;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE); result = false;
            // Allow current Community WRITE perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.WRITE); result = null;
        }};

        assertTrue("testCanEditBooleanAuth3 0", communityService.canEditBoolean(context, c));
    }

    /**
     * Test of canEditBoolean method, of class Community.
     */
    @Test
    public void testCanEditBooleanAuth4() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD); result = false;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE); result = false;
            // Allow current Community WRITE perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.WRITE); result = null;
        }};

        assertTrue("testCanEditBooleanAuth4 0", communityService.canEditBoolean(context, c));
    }

    /**
     * Test of canEditBoolean method, of class Community.
     */
    @Test
    public void testCanEditBooleanNoAuth() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD); result = false;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE); result = false;
            // Disallow current Community WRITE perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.WRITE); result = new AuthorizeException();
        }};

        assertFalse("testCanEditBooleanNoAuth 0", communityService.canEditBoolean(context, c));
    }    

    /**
     * Test of canEdit method, of class Community.
     */
    @Test
    public void testCanEditAuth() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD); result = true;
            // Allow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE); result = true;
            // Allow current Community WRITE perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.WRITE); result = null;
        }};

        communityService.canEdit(context, c);
    }

    /**
     * Test of canEdit method, of class Community.
     */
    @Test
    public void testCanEditAuth1() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD); result = false;
            // Allow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE); result = true;
            // Allow current Community WRITE perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.WRITE); result = null;
        }};

        communityService.canEdit(context, c);
    }

    /**
     * Test of canEdit method, of class Community.
     */
    @Test
    public void testCanEditAuth2() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD); result = true;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE); result = false;
            // Allow current Community WRITE perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.WRITE); result = null;
        }};

        communityService.canEdit(context, c);
    }

    /**
     * Test of canEdit method, of class Community.
     */
    @Test
    public void testCanEditAuth3() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD); result = false;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE); result = false;
            // Allow current Community WRITE perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.WRITE); result = null;
        }};

        communityService.canEdit(context, c);
    }

    /**
     * Test of canEdit method, of class Community.
     */
    @Test(expected=AuthorizeException.class)
    public void testCanEditNoAuth() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD); result = false;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE); result = false;
            // Disallow current Community WRITE perms
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.WRITE); result = new AuthorizeException();
        }};

        communityService.canEdit(context, c);
        fail("Exception expected");
    }

    /**
     * Test of countItems method, of class Community.
     */
    @Test
    public void testCountItems() throws Exception 
    {
        //0 by default
        assertTrue("testCountItems 0", itemService.countItems(context, c) == 0);
        
        //NOTE: a more thorough test of item counting is in ITCommunityCollection integration test
    }

    /**
     * Test of getAdminObject method, of class Community.
     */
    @Test
    @Override
    public void testGetAdminObject() throws SQLException
    {
        //default community has no admin object
        assertThat("testGetAdminObject 0", (Community)communityService.getAdminObject(context, c, Constants.REMOVE), equalTo(c));
        assertThat("testGetAdminObject 1", (Community)communityService.getAdminObject(context, c, Constants.ADD), equalTo(c));
        assertThat("testGetAdminObject 2", communityService.getAdminObject(context, c, Constants.DELETE), nullValue());
        assertThat("testGetAdminObject 3", (Community)communityService.getAdminObject(context, c, Constants.ADMIN), equalTo(c));
    }

    /**
     * Test of getParentObject method, of class Community.
     */
    @Test
    @Override
    public void testGetParentObject() throws SQLException
    {
        try
        {
            //default has no parent
            assertThat("testGetParentObject 0", communityService.getParentObject(context, c), nullValue());

            context.turnOffAuthorisationSystem();
            Community son = communityService.createSubcommunity(context, c);
            context.restoreAuthSystemState();
            assertThat("testGetParentObject 1", communityService.getParentObject(context, son), notNullValue());
            assertThat("testGetParentObject 2", (Community)communityService.getParentObject(context, son), equalTo(c));
        }
        catch(AuthorizeException ex)
        {
            fail("Authorize exception catched");
        }
    }

}
