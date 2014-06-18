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
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.junit.*;
import static org.junit.Assert.* ;
import static org.hamcrest.CoreMatchers.*;
import mockit.*;
import org.apache.log4j.Logger;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Constants;

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
            //we have to create a new community in the database
            context.turnOffAuthorisationSystem();
            this.c = Community.create(null, context);
            this.dspaceObject = c;
            //we need to commit the changes so we don't block the table for testing
            context.restoreAuthSystemState();
            context.commit();
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
        int id = c.getID();
        Community found =  Community.find(context, id);
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
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD); result = true;
                AuthorizeManager.isAdmin((Context) any); result = false;
            }
        };

        // test that a Community Admin can create a Community with parent (Sub-Community)
        Community son = Community.create(c, context);
        //the item created by default has no name set
        assertThat("testCreate 2", son, notNullValue());        
        assertThat("testCreate 3", son.getName(), equalTo(""));        
        assertTrue("testCreate 4", son.getAllParents().length == 1);
        assertThat("testCreate 5", son.getAllParents()[0], equalTo(c));
    }


     /**
     * Test of create method, of class Community.
     */
    @Test
    public void testCreateAuth2() throws Exception
    {
        //Default to Admin Rights, but NOT Community Admin Rights
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD); result = false;
                AuthorizeManager.isAdmin((Context) any); result = true;
            }
        };

        //Test that a full Admin can create a Community without a parent (Top-Level Community)
        Community created = Community.create(null, context);
        //the item created by default has no name set
        assertThat("testCreate 0", created, notNullValue());
        assertThat("testCreate 1", created.getName(), equalTo(""));

        //Test that a full Admin can also create a Community with a parent (Sub-Community)
        Community son = Community.create(created, context);
        //the item created by default has no name set
        assertThat("testCreate 2", son, notNullValue());
        assertThat("testCreate 3", son.getName(), equalTo(""));
        assertTrue("testCreate 4", son.getAllParents().length == 1);
        assertThat("testCreate 5", son.getAllParents()[0], equalTo(created));
    }

    /**
     * Test of create method, of class Community.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateNoAuth() throws Exception
    {
        //Default to NO Admin Rights, and NO Community Admin Rights
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD); result = false;
                AuthorizeManager.isAdmin((Context) any); result = false;
            }
        };

        // test creating community with no parent (as a non-admin & non-Community Admin)
        // this should throw an exception
        Community created = Community.create(null, context);
        fail("Exception expected");
    }

    /**
     * Test of create method (with specified valid handle), of class Community.
     */
    @Test
    public void testCreateWithValidHandle() throws Exception
    {
        //Default to Community Admin Rights, but NO full-Admin rights
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD); result = false;
                AuthorizeManager.isAdmin((Context) any); result = true;
            }
        };

        // test creating community with a specified handle which is NOT already in use
        // (this handle should not already be used by system, as it doesn't start with "1234567689" prefix)
        Community created = Community.create(null, context, "987654321/100");

        // check that community was created, and that its handle was set to proper value
        assertThat("testCreateWithValidHandle 0", created, notNullValue());
        assertThat("testCreateWithValidHandle 1", created.getHandle(), equalTo("987654321/100"));
    }
    
    
     /**
     * Test of create method (with specified invalid handle), of class Community.
     */
    @Test(expected=IllegalStateException.class)
    public void testCreateWithInvalidHandle() throws Exception
    {
        //Default to Community Admin Rights, but NO full-Admin rights
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD); result = false;
                AuthorizeManager.isAdmin((Context) any); result = true;
            }
        };

        //get handle of our default created community
        String inUseHandle = c.getHandle();

        // test creating community with a specified handle which IS already in use
        // This should throw an exception
        Community created = Community.create(null, context, inUseHandle);
        fail("Exception expected");
    }

    /**
     * Test of create method, of class Community.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateNoAuth2() throws Exception
    {
        //Default to Community Admin Rights, but NO full-Admin rights
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD); result = true;
                AuthorizeManager.isAdmin((Context) any); result = false;
            }
        };

        // test creating community with no parent (as a non-admin, but with Community Admin rights)
        // this should throw an exception, as only admins can create Top Level communities
        Community created = Community.create(null, context);
        fail("Exception expected");
    }

    /**
     * Test of findAll method, of class Community.
     */
    @Test
    public void testFindAll() throws Exception
    {
        Community[] all = Community.findAll(context);
        assertThat("testFindAll 0", all, notNullValue());
        assertTrue("testFindAll 1", all.length >= 1);

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
        Community[] all = Community.findAllTop(context);
        assertThat("testFindAllTop 0", all, notNullValue());
        assertTrue("testFindAllTop 1", all.length >= 1);
        for(Community cm: all)
        {
            assertThat("testFindAllTop for", cm.getAllParents().length, equalTo(0));
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
        assertTrue("testGetID 0", c.getID() >= 1);
    }

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
        assertThat("testGetMetadata 0",c.getMetadata("name"), equalTo(""));
        assertThat("testGetMetadata 1",c.getMetadata("short_description"), equalTo(""));
        assertThat("testGetMetadata 2",c.getMetadata("introductory_text"), equalTo(""));
        assertThat("testGetMetadata 3",c.getMetadata("logo_bitstream_id"), equalTo(""));
        assertThat("testGetMetadata 4",c.getMetadata("copyright_text"), equalTo(""));
        assertThat("testGetMetadata 5",c.getMetadata("side_bar_text"), equalTo(""));
    }

    /**
     * Test of setMetadata method, of class Community.
     */
    @Test
    public void testSetMetadata()
    {
        String name = "name";
        String sdesc = "short description";
        String itext = "introductory text";
        String logo = "1";
        String copy = "copyright declaration";
        String sidebar = "side bar text";

        c.setMetadata("name", name);
        c.setMetadata("short_description", sdesc);
        c.setMetadata("introductory_text", itext);
        c.setMetadata("logo_bitstream_id", logo);
        c.setMetadata("copyright_text", copy);
        c.setMetadata("side_bar_text", sidebar);

        assertThat("testSetMetadata 0",c.getMetadata("name"), equalTo(name));
        assertThat("testSetMetadata 1",c.getMetadata("short_description"), equalTo(sdesc));
        assertThat("testSetMetadata 2",c.getMetadata("introductory_text"), equalTo(itext));
        assertThat("testSetMetadata 3",c.getMetadata("logo_bitstream_id"), equalTo(logo));
        assertThat("testSetMetadata 4",c.getMetadata("copyright_text"), equalTo(copy));
        assertThat("testSetMetadata 5",c.getMetadata("side_bar_text"), equalTo(sidebar));
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
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD); result = true;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE); result = true;
                 AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.WRITE); result = null;
            }
        };

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream logo = c.setLogo(new FileInputStream(f));
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
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD); result = false;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE); result = true;
                 AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.WRITE); result = null;
            }
        };

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream logo = c.setLogo(new FileInputStream(f));
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
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD); result = true;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE); result = false;
                 AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.WRITE); result = null;
            }
        };

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream logo = c.setLogo(new FileInputStream(f));
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
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD); result = false;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE); result = false;
                 AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.WRITE); result = null;
            }
        };

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream logo = c.setLogo(new FileInputStream(f));
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
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD); result = false;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE); result = false;
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.WRITE); result = new AuthorizeException();
            }
        };

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream logo = c.setLogo(new FileInputStream(f));
        fail("EXception expected");
    }

    /**
     * Test of update method, of class Community.
     */
    @Test(expected=AuthorizeException.class)
    public void testUpdateNoAuth() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.WRITE); result = new AuthorizeException();
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.ADD); result = new AuthorizeException();
            }
        };

        //TODO: we need to verify the update, how?
        c.update();
        fail("Exception must be thrown");
    }

    /**
     * Test of update method, of class Community.
     */
    @Test
    public void testUpdateAuth() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.WRITE); result = null;
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.ADD); result = null;
            }
        };

        //TODO: we need to verify the update, how?
        c.update();
    }

    /**
     * Test of createAdministrators method, of class Community.
     */
    @Test
    public void testCreateAdministratorsAuth() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeUtil authManager;
            {
                AuthorizeUtil.authorizeManageAdminGroup((Context) any, (Community) any);
                result = null;                
            }
        };

        Group result = c.createAdministrators();
        assertThat("testCreateAdministratorsAuth 0",c.getAdministrators(), notNullValue());
        assertThat("testCreateAdministratorsAuth 1",c.getAdministrators(), equalTo(result));
    }

    /**
     * Test of createAdministrators method, of class Community.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateAdministratorsNoAuth() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeUtil authManager;
            {
                AuthorizeUtil.authorizeManageAdminGroup((Context) any, (Community) any);
                result = new AuthorizeException();
            }
        };

        Group result = c.createAdministrators();
        fail("Exception should have been thrown");
    }


    /**
     * Test of removeAdministrators method, of class Community.
     */
    @Test
    public void testRemoveAdministratorsAuth() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeUtil authManager;
            {
                AuthorizeUtil.authorizeManageAdminGroup((Context) any, (Community) any);
                result = null;
            }
        };

        Group result = c.createAdministrators();
        assertThat("testRemoveAdministratorsAuth 0",c.getAdministrators(), notNullValue());
        assertThat("testRemoveAdministratorsAuth 1",c.getAdministrators(), equalTo(result));
        c.removeAdministrators();
        assertThat("testRemoveAdministratorsAuth 2",c.getAdministrators(), nullValue());
    }

    /**
     * Test of removeAdministrators method, of class Community.
     */
    @Test(expected=AuthorizeException.class)
    public void testRemoveAdministratorsNoAuth() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeUtil authManager;
            {
                AuthorizeUtil.authorizeRemoveAdminGroup((Context) any, (Community) any);
                    result = new AuthorizeException();
            }
        };

        c.removeAdministrators();
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
         new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.WRITE); result = null;
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.ADD); result = null;
            }
        };

        //empty by default
        assertThat("testGetCollections 0",c.getCollections(), notNullValue());
        assertTrue("testGetCollections 1", c.getCollections().length == 0);

        Collection result = c.createCollection();
        assertThat("testGetCollections 2",c.getCollections(), notNullValue());
        assertTrue("testGetCollections 3", c.getCollections().length == 1);
        assertThat("testGetCollections 4",c.getCollections()[0], equalTo(result));
    }

    /**
     * Test of getSubcommunities method, of class Community.
     */
    @Test
    public void testGetSubcommunities() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.ADD); result = null;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD); result = true;
            }
        };

        //empty by default
        assertThat("testGetSubcommunities 0",c.getSubcommunities(), notNullValue());
        assertTrue("testGetSubcommunities 1", c.getSubcommunities().length == 0);

        //community with  parent
        Community son = Community.create(c, context);
        assertThat("testGetSubcommunities 2",c.getSubcommunities(), notNullValue());
        assertTrue("testGetSubcommunities 3", c.getSubcommunities().length == 1);
        assertThat("testGetSubcommunities 4", c.getSubcommunities()[0], equalTo(son));
    }

    /**
     * Test of getParentCommunity method, of class Community.
     */
    @Test
    public void testGetParentCommunity() throws Exception 
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.ADD); result = null;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD); result = true;
            }
        };

        //null by default
        assertThat("testGetParentCommunity 0",c.getParentCommunity(), nullValue());

        //community with  parent
        Community son = Community.create(c, context);
        assertThat("testGetParentCommunity 1",son.getParentCommunity(), notNullValue());
        assertThat("testGetParentCommunity 2", son.getParentCommunity(), equalTo(c));
    }

    /**
     * Test of getAllParents method, of class Community.
     */
    @Test
    public void testGetAllParents() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.ADD); result = null;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD); result = true;
            }
        };

        //empty by default
        assertThat("testGetAllParents 0",c.getAllParents(), notNullValue());
        assertTrue("testGetAllParents 1", c.getAllParents().length == 0);

        //community with  parent
        Community son = Community.create(c, context);
        assertThat("testGetAllParents 2",son.getAllParents(), notNullValue());
        assertTrue("testGetAllParents 3", son.getAllParents().length == 1);
        assertThat("testGetAllParents 4", son.getAllParents()[0], equalTo(c));
    }

    /**
     * Test of getAllCollections method, of class Community.
     */
    @Test
    public void testGetAllCollections() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.ADD); result = null;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD); result = true;
            }
        };

        //empty by default
        assertThat("testGetAllCollections 0",c.getAllCollections(), notNullValue());
        assertTrue("testGetAllCollections 1", c.getAllCollections().length == 0);

        //community has a collection and a subcommunity, subcommunity has a collection
        Collection collOfC = c.createCollection();
        Community sub = Community.create(c, context);
        Collection collOfSub = sub.createCollection();
        assertThat("testGetAllCollections 2",c.getAllCollections(), notNullValue());
        assertTrue("testGetAllCollections 3", c.getAllCollections().length == 2);
        assertThat("testGetAllCollections 4", c.getAllCollections()[0], equalTo(collOfSub));
        assertThat("testGetAllCollections 5", c.getAllCollections()[1], equalTo(collOfC));
    }

    /**
     * Test of createCollection method, of class Community.
     */
    @Test
    public void testCreateCollectionAuth() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.ADD); result = null;
            }
        };

        Collection result = c.createCollection();
        assertThat("testCreateCollectionAuth 0", result, notNullValue());
        assertThat("testCreateCollectionAuth 1", c.getCollections(), notNullValue());
        assertThat("testCreateCollectionAuth 2", c.getCollections()[0], equalTo(result));
    }

    /**
     * Test of createCollection method, of class Community.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateCollectionNoAuth() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.ADD); result = new AuthorizeException();
            }
        };

        Collection result = c.createCollection();
        fail("Exception expected");
    }

    /**
     * Test of addCollection method, of class Community.
     */
    @Test
    public void testAddCollectionAuth() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.ADD); result = null;
            }
        };

        Collection col = Collection.create(context);
        c.addCollection(col);
        assertThat("testAddCollectionAuth 0", c.getCollections(), notNullValue());
        assertThat("testAddCollectionAuth 1", c.getCollections()[0], equalTo(col));
    }

    /**
     * Test of addCollection method, of class Community.
     */
    @Test(expected=AuthorizeException.class)
    public void testAddCollectionNoAuth() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.ADD); result = new AuthorizeException();
            }
        };

        Collection col = Collection.create(context);
        c.addCollection(col);
        fail("Exception expected");
    }

    /**
     * Test of createSubcommunity method, of class Community.
     */
    @Test
    public void testCreateSubcommunityAuth() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.ADD); result = null;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD); result = true;
            }
        };

        Community result = c.createSubcommunity();
        assertThat("testCreateSubcommunityAuth 0",c.getSubcommunities(), notNullValue());
        assertTrue("testCreateSubcommunityAuth 1", c.getSubcommunities().length == 1);
        assertThat("testCreateSubcommunityAuth 2", c.getSubcommunities()[0], equalTo(result));
    }

    /**
     * Test of createSubcommunity method, of class Community.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateSubcommunityNoAuth() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.ADD); result = new AuthorizeException();
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD); result = true;
            }
        };

        Community result = c.createSubcommunity();
        fail("Exception expected");
    }

    /**
     * Test of addSubcommunity method, of class Community.
     */
    @Test
    public void testAddSubcommunityAuth() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.ADD); result = null;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD); result = true;
                AuthorizeManager.isAdmin((Context) any); result = true;
            }
        };

        Community result = Community.create(null, context);
        c.addSubcommunity(result);
        assertThat("testAddSubcommunityAuth 0",c.getSubcommunities(), notNullValue());
        assertTrue("testAddSubcommunityAuth 1", c.getSubcommunities().length == 1);
        assertThat("testAddSubcommunityAuth 2", c.getSubcommunities()[0], equalTo(result));
    }

    /**
     * Test of addSubcommunity method, of class Community.
     */
    @Test(expected=AuthorizeException.class)
    public void testAddSubcommunityNoAuth() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.ADD); result = new AuthorizeException();
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD); result = true;
            }
        };

        Community result = Community.create(null, context);
        c.addSubcommunity(result);
        fail("Exception expected");
    }

    /**
     * Test of removeCollection method, of class Community.
     */
    @Test
    public void testRemoveCollectionAuth() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            AuthorizeUtil authUtil;
            {
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.ADD); result = null;
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.REMOVE); result = null;
                AuthorizeUtil.authorizeManageTemplateItem((Context) any, (Collection) any);
                        result = null;
            }
        };

        Collection col = Collection.create(context);
        c.addCollection(col);
        assertThat("testRemoveCollectionAuth 0", c.getCollections(), notNullValue());
        assertTrue("testRemoveCollectionAuth 1", c.getCollections().length == 1);
        assertThat("testRemoveCollectionAuth 2", c.getCollections()[0], equalTo(col));
        
        c.removeCollection(col);
        assertThat("testRemoveCollectionAuth 3", c.getCollections(), notNullValue());
        assertTrue("testRemoveCollectionAuth 4", c.getCollections().length == 0);
    }

    /**
     * Test of removeCollection method, of class Community.
     */
    @Test(expected=AuthorizeException.class)
    public void testRemoveCollectionNoAuth() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.ADD); result = null;
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.REMOVE); result = new AuthorizeException();
            }
        };

        Collection col = Collection.create(context);
        c.addCollection(col);
        assertThat("testRemoveCollectionNoAuth 0", c.getCollections(), notNullValue());
        assertTrue("testRemoveCollectionNoAuth 1", c.getCollections().length == 1);
        assertThat("testRemoveCollectionNoAuth 2", c.getCollections()[0], equalTo(col));

        c.removeCollection(col);
        fail("Exception expected");
    }

    /**
     * Test of removeSubcommunity method, of class Community.
     */
    @Test
    public void testRemoveSubcommunityAuth() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.ADD); result = null;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD); result = true;
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.REMOVE); result = null;
                AuthorizeManager.isAdmin((Context) any); result = true;
            }
        };

        Community com = Community.create(null,context);
        c.addSubcommunity(com);
        assertThat("testRemoveSubcommunityAuth 0", c.getSubcommunities(), notNullValue());
        assertTrue("testRemoveSubcommunityAuth 1", c.getSubcommunities().length == 1);
        assertThat("testRemoveSubcommunityAuth 2", c.getSubcommunities()[0], equalTo(com));

        c.removeSubcommunity(com);
        assertThat("testRemoveSubcommunityAuth 3", c.getSubcommunities(), notNullValue());
        assertTrue("testRemoveSubcommunityAuth 4", c.getSubcommunities().length == 0);
    }

    /**
     * Test of delete method, of class Community.
     */
    @Test
    public void testDeleteAuth() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.DELETE); result = null;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.REMOVE); result = false;
            }
        };

        int id = c.getID();
        c.delete();
        Community found = Community.find(context, id);
        assertThat("testDeleteAuth 0",found, nullValue());
    }

    /**
     * Test of delete method, of class Community.
     */
    @Test
    public void testDeleteAuth2() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.DELETE); result = null;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.REMOVE); result = true;
            }
        };

        int id = c.getID();
        c.delete();
        Community found = Community.find(context, id);
        assertThat("testDeleteAuth2 0",found, nullValue());
    }

    /**
     * Test of delete method, of class Community.
     */
    @Test(expected=AuthorizeException.class)
    public void testDeleteNoAuth() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.DELETE); result = new AuthorizeException();
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.REMOVE); result = false;
            }
        };

        int id = c.getID();
        c.delete();
        fail("Exception expected");
    }

    /**
     * Test of equals method, of class Community.
     */
    @Test
    @SuppressWarnings("ObjectEqualsNull")
    public void testEquals() throws SQLException, AuthorizeException
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.isAdmin((Context) any); result = true;
            }
        };

        assertFalse("testEquals 0",c.equals(null));
        assertFalse("testEquals 1",c.equals(Community.create(null, context)));
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
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE); result = true;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD); result = true;
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.WRITE); result = null;
            }
        };

        assertTrue("testCanEditBooleanAuth 0", c.canEditBoolean());
    }

    /**
     * Test of canEditBoolean method, of class Community.
     */
    @Test
    public void testCanEditBooleanAuth2() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE); result = false;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD); result = true;
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.WRITE); result = null;
            }
        };

        assertTrue("testCanEditBooleanAuth2 0", c.canEditBoolean());
    }

    /**
     * Test of canEditBoolean method, of class Community.
     */
    @Test
    public void testCanEditBooleanAuth3() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE); result = true;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD); result = false;
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.WRITE); result = null;
            }
        };

        assertTrue("testCanEditBooleanAuth3 0", c.canEditBoolean());
    }

    /**
     * Test of canEditBoolean method, of class Community.
     */
    @Test
    public void testCanEditBooleanAuth4() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE); result = false;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD); result = false;
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.WRITE); result = null;
            }
        };

        assertTrue("testCanEditBooleanAuth4 0", c.canEditBoolean());
    }

    /**
     * Test of canEditBoolean method, of class Community.
     */
    @Test
    public void testCanEditBooleanNoAuth() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE); result = false;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD); result = false;
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.WRITE); result = new AuthorizeException();
            }
        };

        assertFalse("testCanEditBooleanNoAuth 0", c.canEditBoolean());
    }    

    /**
     * Test of canEdit method, of class Community.
     */
    @Test
    public void testCanEditAuth() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE); result = false;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD); result = false;
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.WRITE); result = null;
            }
        };

        c.canEdit();
    }

    /**
     * Test of canEdit method, of class Community.
     */
    @Test
    public void testCanEditAuth1() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE); result = true;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD); result = false;
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.WRITE); result = null;
            }
        };

        c.canEdit();
    }

    /**
     * Test of canEdit method, of class Community.
     */
    @Test
    public void testCanEditAuth2() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE); result = false;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD); result = true;
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.WRITE); result = null;
            }
        };

        c.canEdit();
    }

    /**
     * Test of canEdit method, of class Community.
     */
    @Test(expected=AuthorizeException.class)
    public void testCanEditNoAuth() throws Exception
    {
        new NonStrictExpectations()
        {
            AuthorizeManager authManager;
            {
                 AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.WRITE); result = false;
                AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                        Constants.ADD); result = false;
                AuthorizeManager.authorizeAction((Context) any, (Community) any,
                        Constants.WRITE); result = new AuthorizeException();
            }
        };

        c.canEdit();
        fail("Exception expected");
    }

    /**
     * Test of countItems method, of class Community.
     */
    @Test
    public void testCountItems() throws Exception 
    {
        //0 by default
        assertTrue("testCountItems 0", c.countItems() == 0);
    }

    /**
     * Test of getAdminObject method, of class Community.
     */
    @Test
    @Override
    public void testGetAdminObject() throws SQLException
    {
        //default community has no admin object
        assertThat("testGetAdminObject 0", (Community)c.getAdminObject(Constants.REMOVE), equalTo(c));
        assertThat("testGetAdminObject 1", (Community)c.getAdminObject(Constants.ADD), equalTo(c));
        assertThat("testGetAdminObject 2", c.getAdminObject(Constants.DELETE), nullValue());
        assertThat("testGetAdminObject 3", (Community)c.getAdminObject(Constants.ADMIN), equalTo(c));
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
            assertThat("testGetParentObject 0", c.getParentObject(), nullValue());

            context.turnOffAuthorisationSystem();
            Community son = c.createSubcommunity();
            context.restoreAuthSystemState();
            assertThat("testGetParentObject 1", son.getParentObject(), notNullValue());
            assertThat("testGetParentObject 2", (Community)son.getParentObject(), equalTo(c));
        }
        catch(AuthorizeException ex)
        {
            fail("Authorize exception catched");
        }
    }

}
