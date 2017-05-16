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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import org.dspace.authorize.AuthorizeException;
import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.elasticsearch.common.collect.Lists;
import org.junit.*;
import static org.junit.Assert.* ;
import static org.hamcrest.CoreMatchers.*;
import mockit.NonStrictExpectations;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Constants;
import org.dspace.core.LicenseManager;

/**
 * Unit Tests for class Collection
 * @author pvillega
 */
public class CollectionTest extends AbstractDSpaceObjectTest
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(CollectionTest.class);

    /**
     * Collection instance for the tests
     */
    private Collection c;

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
            this.c = Collection.create(context);
            this.dspaceObject = c;
            //we need to commit the changes so we don't block the table for testing
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
     * Test of find method, of class Collection.
     */
    @Test
    public void testCollectionFind() throws Exception
    {
        int id = c.getID();
        Collection found =  Collection.find(context, id);
        assertThat("testCollectionFind 0", found, notNullValue());
        assertThat("testCollectionFind 1", found.getID(), equalTo(id));
        //the community created by default has no name
        assertThat("testCollectionFind 2", found.getName(), equalTo(""));
    }

    /**
     * Test of create method, of class Collection.
     */
    @Test
    public void testCreate() throws Exception
    {
        Collection created = Collection.create(context);
        assertThat("testCreate 0", created, notNullValue());
        assertThat("testCreate 1", created.getName(), equalTo(""));
    }

     /**
     * Test of create method (with specified valid handle), of class Collection
     */
    @Test
    public void testCreateWithValidHandle() throws Exception
    {
        // test creating collection with a specified handle which is NOT already in use
        // (this handle should not already be used by system, as it doesn't start with "1234567689" prefix)
        Collection created = Collection.create(context, "987654321/100");

        // check that collection was created, and that its handle was set to proper value
        assertThat("testCreateWithValidHandle 0", created, notNullValue());
        assertThat("testCreateWithValidHandle 1", created.getHandle(), equalTo("987654321/100"));
    }


     /**
     * Test of create method (with specified invalid handle), of class Collection.
     */
    @Test(expected=IllegalStateException.class)
    public void testCreateWithInvalidHandle() throws Exception
    {
        //get handle of our default created collection
        String inUseHandle = c.getHandle();

        // test creating collection with a specified handle which IS already in use
        // This should throw an exception
        Collection created = Collection.create(context, inUseHandle);
        fail("Exception expected");
    }


    /**
     * Test of findAll method, of class Collection.
     */
    @Test
    public void testFindAll() throws Exception
    {
        Collection[] all = Collection.findAll(context);
        assertThat("testFindAll 0", all, notNullValue());
        assertTrue("testFindAll 1", all.length >= 1);

        boolean added = false;
        for(Collection cl: all)
        {
            if(cl.equals(c))
            {
                added = true;
            }
        }
        assertTrue("testFindAll 2",added);
    }

    /**
     * Test of getItems method, of class Collection.
     */
    @Test
    public void testGetItems() throws Exception
    {
        ItemIterator items = c.getItems();
        assertThat("testGetItems 0", items, notNullValue());
        //by default is empty
        assertFalse("testGetItems 1", items.hasNext());
        assertThat("testGetItems 2", items.next(), nullValue());
        assertThat("testGetItems 3", items.nextID(), equalTo(-1));
    }

    /**
     * Test of getAllItems method, of class Collection.
     */
    @Test
    public void testGetAllItems() throws Exception
    {
        ItemIterator items = c.getAllItems();
        assertThat("testGetAllItems 0", items, notNullValue());
        //by default is empty
        assertFalse("testGetAllItems 1", items.hasNext());
        assertThat("testGetAllItems 2", items.next(), nullValue());
        assertThat("testGetAllItems 3", items.nextID(), equalTo(-1));
    }

    /**
     * Test of getID method, of class Collection.
     */
    @Test
    @Override
    public void testGetID()
    {
        assertTrue("testGetID 0", c.getID() >= 1);
    }

    /**
     * Test of getHandle method, of class Collection.
     */
    @Test
    @Override
    public void testGetHandle()
    {
        //default instance has a random handle
        assertTrue("testGetHandle 0", c.getHandle().contains("123456789/"));
    }

    /**
     * Test of getMetadata method, of class Collection.
     */
    @Test
    public void testGetMetadata()
    {
        //by default all empty values will return ""
        assertThat("testGetMetadata 0",c.getMetadata("name"), equalTo(""));
        assertThat("testGetMetadata 1",c.getMetadata("short_description"), equalTo(""));
        assertThat("testGetMetadata 2",c.getMetadata("introductory_text"), equalTo(""));
        assertThat("testGetMetadata 4",c.getMetadata("copyright_text"), equalTo(""));
        assertThat("testGetMetadata 6",c.getMetadata("provenance_description"), equalTo(""));
        assertThat("testGetMetadata 7",c.getMetadata("side_bar_text"), equalTo(""));
        assertThat("testGetMetadata 8",c.getMetadata("license"), equalTo(""));
    }

    /**
     * Test of setMetadata method, of class Collection.
     */
    @Test
    public void testSetMetadata() throws SQLException {
        String name = "name";
        String sdesc = "short description";
        String itext = "introductory text";
        String copy = "copyright declaration";
        String sidebar = "side bar text";
        String tempItem = "3";
        String provDesc = "provenance description";
        String license = "license text";

        c.setMetadata("name", name);
        c.setMetadata("short_description", sdesc);
        c.setMetadata("introductory_text", itext);
        c.setMetadata("copyright_text", copy);
        c.setMetadata("side_bar_text", sidebar);
        c.setMetadata("provenance_description", provDesc);
        c.setMetadata("license", license);

        assertThat("testSetMetadata 0",c.getMetadata("name"), equalTo(name));
        assertThat("testSetMetadata 1",c.getMetadata("short_description"), equalTo(sdesc));
        assertThat("testSetMetadata 2",c.getMetadata("introductory_text"), equalTo(itext));
        assertThat("testSetMetadata 4",c.getMetadata("copyright_text"), equalTo(copy));
        assertThat("testSetMetadata 5",c.getMetadata("side_bar_text"), equalTo(sidebar));
        assertThat("testGetMetadata 7",c.getMetadata("provenance_description"), equalTo(provDesc));
        assertThat("testGetMetadata 8",c.getMetadata("license"), equalTo(license));
    }

    /**
     * Test of getName method, of class Collection.
     */
    @Test
    @Override
    public void testGetName()
    {
        //by default is empty
        assertThat("testGetName 0",c.getName(), equalTo(""));
    }

    /**
     * Test of getLogo method, of class Collection.
     */
    @Test
    public void testGetLogo()
    {
        //by default is empty
        assertThat("testGetLogo 0",c.getLogo(), nullValue());
    }

    /**
     * Test of setLogo method, of class Collection.
     */
    @Test
    public void testSetLogoAuth() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = true;
            // Allow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = true;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = null;
        }};

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream logo = c.setLogo(new FileInputStream(f));
        assertThat("testSetLogoAuth 0",c.getLogo(), equalTo(logo));

        c.setLogo(null);
        assertThat("testSetLogoAuth 1",c.getLogo(), nullValue());
    }

    /**
     * Test of setLogo method, of class Collection.
     */
    @Test
    public void testSetLogoAuth2() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = false;
            // Allow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = true;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = null;
        }};

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream logo = c.setLogo(new FileInputStream(f));
        assertThat("testSetLogoAuth2 0",c.getLogo(), equalTo(logo));

        c.setLogo(null);
        assertThat("testSetLogoAuth2 1",c.getLogo(), nullValue());
    }

    /**
     * Test of setLogo method, of class Collection.
     */
    @Test
    public void testSetLogoAuth3() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = true;
            // Disallow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = false;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = null;
        }};

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream logo = c.setLogo(new FileInputStream(f));
        assertThat("testSetLogoAuth3 0",c.getLogo(), equalTo(logo));

        c.setLogo(null);
        assertThat("testSetLogoAuth3 1",c.getLogo(), nullValue());
    }

    /**
     * Test of setLogo method, of class Collection.
     */
    @Test
    public void testSetLogoAuth4() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = false;
            // Disallow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = false;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = null;
        }};

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream logo = c.setLogo(new FileInputStream(f));
        assertThat("testSetLogoAuth4 0",c.getLogo(), equalTo(logo));

        c.setLogo(null);
        assertThat("testSetLogoAuth4 1",c.getLogo(), nullValue());
    }

    /**
     * Test of setLogo method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testSetLogoNoAuth() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = false;
            // Disallow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = false;
            // Disallow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = new AuthorizeException();
        }};

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream logo = c.setLogo(new FileInputStream(f));
        fail("EXception expected");
    }

    /**
     * Test of createWorkflowGroup method, of class Collection.
     */
    @Test
    public void testCreateWorkflowGroupAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeUtil.class)
        {{
            // Allow manage WorkflowsGroup perms
            AuthorizeUtil.authorizeManageWorkflowsGroup((Context) any, (Collection) any);
                result = null;
        }};

        int step = 1;
        Group result = c.createWorkflowGroup(step);
        assertThat("testCreateWorkflowGroupAuth 0", result, notNullValue());
    }

    /**
     * Test of createWorkflowGroup method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateWorkflowGroupNoAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeUtil.class)
        {{
            // Disallow manage WorkflowsGroup perms
            AuthorizeUtil.authorizeManageWorkflowsGroup((Context) any, (Collection) any);
                result = new AuthorizeException();
        }};

        int step = 1;
        Group result = c.createWorkflowGroup(step);
        fail("Exception expected");
    }

    /**
     * Test of setWorkflowGroup method, of class Collection.
     */
    @Test
    public void testSetWorkflowGroup() throws SQLException, AuthorizeException
    {
        context.turnOffAuthorisationSystem(); //must be an Admin to create a Group
        int step = 1;
        Group g = Group.create(context);
        context.commit();
        context.restoreAuthSystemState();
        c.setWorkflowGroup(step, g);
        assertThat("testSetWorkflowGroup 0",c.getWorkflowGroup(step), notNullValue());
        assertThat("testSetWorkflowGroup 1",c.getWorkflowGroup(step), equalTo(g));
    }

    /**
     * Test of setWorkflowGroup method, of class Collection.
     * The setWorkflowGroup adjusts the policies for the basic Workflow. This test
     * shall assure that no exception (e.g. ConcurrentModificationException) is
     * thrown during these adjustments.
     * @throws java.sql.SQLException passed through.
     * @throws org.dspace.authorize.AuthorizeException passed through.
     */
    @Test
    public void testChangeWorkflowGroup()
            throws SQLException, AuthorizeException
    {
        context.turnOffAuthorisationSystem(); //must be an Admin to create a Group
        int step = 1;
        Group g1 = Group.create(context);
        Group g2 = Group.create(context);
        context.restoreAuthSystemState();
        c.setWorkflowGroup(step, g1);
        c.setWorkflowGroup(step, g2);
        assertThat("testSetWorkflowGroup 0", c.getWorkflowGroup(step), notNullValue());
        assertThat("testSetWorkflowGroup 1", c.getWorkflowGroup(step), equalTo(g2));
    }

    /**
     * Test of getWorkflowGroup method, of class Collection.
     */
    @Test
    public void testGetWorkflowGroup()
    {
        //null by default
        int step = 1;
        assertThat("testGetWorkflowGroup 0",c.getWorkflowGroup(step), nullValue());
    }

    /**
     * Test of createSubmitters method, of class Collection.
     */
    @Test
    public void testCreateSubmittersAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeUtil.class)
        {{
            // Allow manage SubmittersGroup perms
            AuthorizeUtil.authorizeManageSubmittersGroup((Context) any, (Collection) any);
                result = null;
        }};

        Group result = c.createSubmitters();
        assertThat("testCreateSubmittersAuth 0",result, notNullValue());
    }

    /**
     * Test of createSubmitters method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateSubmittersNoAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeUtil.class)
        {{
            // Disallow manage SubmittersGroup perms
            AuthorizeUtil.authorizeManageSubmittersGroup((Context) any, (Collection) any);
                result = new AuthorizeException();
        }};

        Group result = c.createSubmitters();
        fail("Exception expected");
    }

    /**
     * Test of removeSubmitters method, of class Collection.
     */
    @Test
    public void testRemoveSubmittersAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeUtil.class)
        {{
            // Allow manage SubmittersGroup perms
            AuthorizeUtil.authorizeManageSubmittersGroup((Context) any, (Collection) any);
                result = null;
        }};

        c.removeSubmitters();
        assertThat("testRemoveSubmittersAuth 0", c.getSubmitters(), nullValue());
    }

    /**
     * Test of removeSubmitters method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testRemoveSubmittersNoAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeUtil.class)
        {{
            // Disallow manage SubmittersGroup perms
            AuthorizeUtil.authorizeManageSubmittersGroup((Context) any, (Collection) any);
                result = new AuthorizeException();
        }};

        c.removeSubmitters();
        fail("Exception expected");
    }

    /**
     * Test of getSubmitters method, of class Collection.
     */
    @Test
    public void testGetSubmitters()
    {
        assertThat("testGetSubmitters 0", c.getSubmitters(), nullValue());
    }

    /**
     * Test of createAdministrators method, of class Collection.
     */
    @Test
    public void testCreateAdministratorsAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeUtil.class)
        {{
            // Allow manage AdminGroup perms
            AuthorizeUtil.authorizeManageAdminGroup((Context) any, (Collection) any);
                result = null;
        }};

        Group result = c.createAdministrators();
        assertThat("testCreateAdministratorsAuth 0", result, notNullValue());
    }

    /**
     * Test of createAdministrators method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateAdministratorsNoAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeUtil.class)
        {{
            // Disallow manage AdminGroup perms
            AuthorizeUtil.authorizeManageAdminGroup((Context) any, (Collection) any);
                result = new AuthorizeException();
        }};

        Group result = c.createAdministrators();
        fail("Exception expected");
    }

    /**
     * Test of removeAdministrators method, of class Collection.
     */
    @Test
    public void testRemoveAdministratorsAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeUtil.class)
        {{
            // Allow manage AdminGroup perms (needed to possibly create group first)
            AuthorizeUtil.authorizeManageAdminGroup((Context) any, (Collection) any);
                result = null;
            // Allow remove AdminGroup perms
            AuthorizeUtil.authorizeRemoveAdminGroup((Context) any, (Collection) any);
                result = null;
        }};

        // Ensure admin group is created first
        Group result = c.createAdministrators();
        assertThat("testRemoveAdministratorsAuth 0",c.getAdministrators(), notNullValue());
        assertThat("testRemoveAdministratorsAuth 1",c.getAdministrators(), equalTo(result));
        c.removeAdministrators();
        assertThat("testRemoveAdministratorsAuth 2", c.getAdministrators(), nullValue());
    }

    /**
     * Test of removeAdministrators method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testRemoveAdministratorsNoAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeUtil.class)
        {{
            // Allow manage AdminGroup perms (needed to possibly create group first)
            AuthorizeUtil.authorizeManageAdminGroup((Context) any, (Collection) any);
                result = null;
            // Disallow remove AdminGroup perms
            AuthorizeUtil.authorizeRemoveAdminGroup((Context) any, (Collection) any);
                result = new AuthorizeException();
        }};

        // Ensure admin group is created first
        Group result = c.createAdministrators();
        assertThat("testRemoveAdministratorsAuth 0",c.getAdministrators(), notNullValue());
        assertThat("testRemoveAdministratorsAuth 1",c.getAdministrators(), equalTo(result));
        c.removeAdministrators();
        fail("Exception expected");
    }

    /**
     * Test of getAdministrators method, of class Collection.
     */
    @Test
    public void testGetAdministrators()
    {
        assertThat("testGetAdministrators 0", c.getAdministrators(), nullValue());
    }

    /**
     * Test of getLicense method, of class Collection.
     */
    @Test
    public void testGetLicense()
    {
        assertThat("testGetLicense 0", c.getLicense(), notNullValue());
        assertThat("testGetLicense 1", c.getLicense(), equalTo(LicenseManager.getDefaultSubmissionLicense()));
    }

    /**
     * Test of getLicenseCollection method, of class Collection.
     */
    @Test
    public void testGetLicenseCollection()
    {
        assertThat("testGetLicenseCollection 0", c.getLicenseCollection(), notNullValue());
        assertThat("testGetLicenseCollection 1", c.getLicenseCollection(), equalTo(""));
    }

    /**
     * Test of hasCustomLicense method, of class Collection.
     */
    @Test
    public void testHasCustomLicense()
    {
        assertFalse("testHasCustomLicense 0", c.hasCustomLicense());
    }

    /**
     * Test of setLicense method, of class Collection.
     */
    @Test
    public void testSetLicense() throws SQLException {
        String license = "license for test";
        c.setLicense(license);
        assertThat("testSetLicense 0", c.getLicense(), notNullValue());
        assertThat("testSetLicense 1", c.getLicense(), equalTo(license));
        assertThat("testSetLicense 2", c.getLicenseCollection(), notNullValue());
        assertThat("testSetLicense 3", c.getLicenseCollection(), equalTo(license));
    }

    /**
     * Test of getTemplateItem method, of class Collection.
     */
    @Test
    public void testGetTemplateItem() throws Exception
    {
        assertThat("testGetTemplateItem 0", c.getTemplateItem(), nullValue());
    }

    /**
     * Test of createTemplateItem method, of class Collection.
     */
    @Test
    public void testCreateTemplateItemAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeUtil.class)
        {{
            // Allow manage TemplateItem  perms
            AuthorizeUtil.authorizeManageTemplateItem((Context) any, (Collection) any);
                result = null;
        }};

        c.createTemplateItem();
        assertThat("testCreateTemplateItemAuth 0",c.getTemplateItem(), notNullValue());
    }

    /**
     * Test of createTemplateItem method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateTemplateItemNoAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeUtil.class)
        {{
            // Disallow manage TemplateItem  perms
            AuthorizeUtil.authorizeManageTemplateItem((Context) any, (Collection) any);
                result = new AuthorizeException();
        }};

        c.createTemplateItem();
        fail("Exception expected");
    }

    /**
     * Test of removeTemplateItem method, of class Collection.
     */
    @Test
    public void testRemoveTemplateItemAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeUtil.class)
        {{
            // Allow manage TemplateItem  perms
            AuthorizeUtil.authorizeManageTemplateItem((Context) any, (Collection) any);
                result = null;
        }};

        c.removeTemplateItem();
        assertThat("testRemoveTemplateItemAuth 0",c.getTemplateItem(), nullValue());
    }

    /**
     * Test of removeTemplateItem method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testRemoveTemplateItemNoAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeUtil.class)
        {{
            // Disallow manage TemplateItem  perms
            AuthorizeUtil.authorizeManageTemplateItem((Context) any, (Collection) any);
                result = new AuthorizeException();
        }};

        c.removeTemplateItem();
        fail("Exception expected");
    }

    /**
     * Test of addItem method, of class Collection.
     */
    @Test
    public void testAddItemAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow Collection ADD permissions
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.ADD); result = null;
        }};

        Item item = Item.create(context);
        c.addItem(item);
        boolean added = false;
        ItemIterator ii = c.getAllItems();
        while(ii.hasNext())
        {
            if(ii.next().equals(item))
            {
                added = true;
            }
        }
        assertTrue("testAddItemAuth 0",added);
    }

    /**
     * Test of addItem method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testAddItemNoAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow Collection ADD permissions
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.ADD); result = new AuthorizeException();
        }};

        Item item = Item.create(context);
        c.addItem(item);
        fail("Exception expected");
    }

    /**
     * Test of removeItem method, of class Collection.
     */
    @Test
    public void testRemoveItemAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow Collection ADD/REMOVE permissions
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.ADD); result = null;
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.REMOVE); result = null;
        }};

        Item item = Item.create(context);
        c.addItem(item);

        c.removeItem(item);
        boolean isthere = false;
        ItemIterator ii = c.getAllItems();
        while(ii.hasNext())
        {
            if(ii.next().equals(item))
            {
                isthere = true;
            }
        }
        assertFalse("testRemoveItemAuth 0",isthere);
    }

    /**
     * Test of removeItem method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testRemoveItemNoAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow Collection ADD permissions
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.ADD); result = null;
            // Disallow Collection REMOVE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.REMOVE); result = new AuthorizeException();
        }};

        Item item = Item.create(context);
        c.addItem(item);

        c.removeItem(item);
        fail("Exception expected");
    }

    /**
     * Test of update method, of class Collection.
     */
    @Test
    public void testUpdateAuth() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = true;
            // Allow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = true;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = null;
        }};

        //TODO: how to check update?
        c.update();
    }

    /**
     * Test of update method, of class Collection.
     */
    @Test
    public void testUpdateAuth2() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = false;
            // Allow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = true;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = null;
        }};

        //TODO: how to check update?
        c.update();
    }

    /**
     * Test of update method, of class Collection.
     */
    @Test
    public void testUpdateAuth3() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = true;
            // Disallow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = false;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = null;
        }};

        //TODO: how to check update?
        c.update();
    }

    /**
     * Test of update method, of class Collection.
     */
    @Test
    public void testUpdateAuth4() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = false;
            // Disallow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = false;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = null;
        }};

        //TODO: how to check update?
        c.update();
    }

    /**
     * Test of update method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testUpdateNoAuth() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = false;
            // Disallow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = false;
            // Disallow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = new AuthorizeException();
        }};

        c.update();
        fail("Exception expected");
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = true;
            // Allow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = true;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = null;
        }};

        assertTrue("testCanEditBooleanAuth 0", c.canEditBoolean());
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth2() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = false;
            // Allow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = true;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = null;
        }};

        assertTrue("testCanEditBooleanAuth2 0", c.canEditBoolean());
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth3() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = true;
            // Disallow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = false;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = null;
        }};

        assertTrue("testCanEditBooleanAuth3 0", c.canEditBoolean());
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth4() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = false;
            // Disallow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = false;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = null;
        }};

        assertTrue("testCanEditBooleanAuth4 0", c.canEditBoolean());
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanNoAuth() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = false;
            // Disallow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = false;
            // Disallow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = new AuthorizeException();
        }};

        assertFalse("testCanEditBooleanNoAuth 0", c.canEditBoolean());
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth_boolean() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = true;
            // Allow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = true;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = null;
        }};

        assertTrue("testCanEditBooleanAuth_boolean 0", c.canEditBoolean(true));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth2_boolean() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = false;
            // Allow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = true;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = null;
        }};

        assertTrue("testCanEditBooleanAuth2_boolean 0", c.canEditBoolean(true));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth3_boolean() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = true;
            // Disallow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = false;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = null;
        }};

        assertTrue("testCanEditBooleanAuth3_boolean 0", c.canEditBoolean(true));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth4_boolean() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = false;
            // Disallow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = false;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = null;
        }};

        assertTrue("testCanEditBooleanAuth4_boolean 0", c.canEditBoolean(true));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth5_boolean() throws Exception
    {
        // Test permissions with inheritance turned *OFF*
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,false); result = true;
            // Allow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,false); result = true;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,false); result = null;
        }};

        assertTrue("testCanEditBooleanAuth5_boolean 0", c.canEditBoolean(false));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth6_boolean() throws Exception
    {
        // Test permissions with inheritance turned *OFF*
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,false); result = false;
            // Allow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,false); result = true;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,false); result = null;
        }};

        assertTrue("testCanEditBooleanAuth6_boolean 0", c.canEditBoolean(false));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth7_boolean() throws Exception
    {
        // Test permissions with inheritance turned *OFF*
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,false); result = true;
            // Disallow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,false); result = false;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,false); result = null;
        }};

        assertTrue("testCanEditBooleanAuth7_boolean 0", c.canEditBoolean(false));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth8_boolean() throws Exception
    {
        // Test permissions with inheritance turned *OFF*
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,false); result = false;
            // Disallow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,false); result = false;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,false); result = null;
        }};

        assertTrue("testCanEditBooleanAuth8_boolean 0", c.canEditBoolean(false));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanNoAuth_boolean() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = false;
            // Disallow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = false;
            // Disallow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = new AuthorizeException();
        }};

        assertFalse("testCanEditBooleanNoAuth_boolean 0",c.canEditBoolean(true));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanNoAuth2_boolean() throws Exception
    {
        // Test permissions with inheritance turned *OFF*
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,false); result = false;
            // Disallow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,false); result = false;
            // Disallow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,false); result = new AuthorizeException();
        }};

        assertFalse("testCanEditBooleanNoAuth_boolean 0",c.canEditBoolean(false));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth_0args() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = true;
            // Allow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = true;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = null;
        }};

        //TODO: how to check??
        c.canEdit();
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth2_0args() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = false;
            // Allow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = true;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = null;
        }};

        //TODO: how to check??
        c.canEdit();
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth3_0args() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = true;
            // Disallow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = false;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = null;
        }};

        //TODO: how to check??
        c.canEdit();
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth4_0args() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = false;
            // Disallow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = false;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = null;
        }};

        //TODO: how to check??
        c.canEdit();
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testCanEditNoAuth_0args() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = false;
            // Disallow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = false;
            // Disallow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = new AuthorizeException();
        }};

        c.canEdit();
        fail("Exception expected");
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth_boolean() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = true;
            // Allow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = true;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = null;
        }};

        //TOO: how to check?
        c.canEdit(true);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth2_boolean() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = false;
            // Allow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = true;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = null;
        }};

        //TOO: how to check?
        c.canEdit(true);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth3_boolean() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = true;
            // Disallow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = false;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = null;
        }};

        //TOO: how to check?
        c.canEdit(true);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth4_boolean() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = false;
            // Disallow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = false;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = null;
        }};

        //TOO: how to check?
        c.canEdit(true);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth5_boolean() throws Exception
    {
        // Test permissions with inheritance turned *OFF*
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,false); result = true;
            // Allow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,false); result = true;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,false); result = null;
        }};

        //TOO: how to check?
        c.canEdit(false);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth6_boolean() throws Exception
    {
        // Test permissions with inheritance turned *OFF*
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,false); result = false;
            // Allow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,false); result = true;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,false); result = null;
        }};

        //TOO: how to check?
        c.canEdit(false);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth7_boolean() throws Exception
    {
        // Test permissions with inheritance turned *OFF*
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,false); result = true;
            // Disallow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,false); result = false;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,false); result = null;
        }};

        //TOO: how to check?
        c.canEdit(false);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth8_boolean() throws Exception
    {
        // Test permissions with inheritance turned *OFF*
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,false); result = false;
            // Disallow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,false); result = false;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,false); result = null;
        }};

        //TOO: how to check?
        c.canEdit(false);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testCanEditNoAuth_boolean() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = false;
            // Disallow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = false;
            // Disallow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = new AuthorizeException();
        }};

        //TOO: how to check?
        c.canEdit(false);
        fail("Exception expected");
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testCanEditNoAuth2_boolean() throws Exception
    {
        // Test permissions with inheritance turned *OFF*
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow parent Community ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,false); result = false;
            // Disallow parent Community WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,false); result = false;
            // Disallow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,false); result = new AuthorizeException();
        }};

        //TOO: how to check?
        c.canEdit(true);
        fail("Exception expected");
    }

    /**
     * Test of delete method, of class Collection.
     */
    @Test
    public void testDeleteAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeUtil.class, AuthorizeManager.class)
        {{
            // Allow manage TemplateItem perms
            AuthorizeUtil.authorizeManageTemplateItem((Context) any, (Collection) any);
                    result = null;
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, anyBoolean); result = null;
        }};

        int id = c.getID();
        c.delete();
        assertThat("testDelete 0",Collection.find(context, id),nullValue());
    }

    /**
     * Test of delete method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testDeleteNoAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeUtil.class, AuthorizeManager.class)
        {{
            // Disallow manage TemplateItem perms
            AuthorizeUtil.authorizeManageTemplateItem((Context) any, (Collection) any);
                    result = new AuthorizeException();
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, anyBoolean); result = null;
        }};

        c.delete();
        fail("Exception expected");
    }

     /**
     * Test of delete method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testDeleteNoAuth2() throws Exception
    {
        new NonStrictExpectations(AuthorizeUtil.class, AuthorizeManager.class)
        {{
            // Allow manage TemplateItem perms
            AuthorizeUtil.authorizeManageTemplateItem((Context) any, (Collection) any);
                    result = null;
            // Disallow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, anyBoolean); result = new AuthorizeException();
        }};

        c.delete();
        fail("Exception expected");
    }

    /**
     * Test of getCommunities method, of class Collection.
     */
    @Test
    public void testGetCommunities() throws Exception
    {
        assertThat("testGetCommunities 0",c.getCommunities(), notNullValue());
        assertTrue("testGetCommunities 1",c.getCommunities().length == 0);
    }

    /**
     * Test of equals method, of class Collection.
     */
    @Test
    @SuppressWarnings("ObjectEqualsNull")
    public void testEquals() throws SQLException, AuthorizeException
    {
        assertFalse("testEquals 0",c.equals(null));
        assertFalse("testEquals 1",c.equals(Collection.create(context)));
        assertTrue("testEquals 2", c.equals(c));
    }

    /**
     * Test of getType method, of class Collection.
     */
    @Test
    @Override
    public void testGetType()
    {
        assertThat("testGetType 0", c.getType(), equalTo(Constants.COLLECTION));
    }

    /**
     * Test of findAuthorized method, of class Collection.
     */
    @Test
    public void testFindAuthorized() throws Exception
    {
        context.turnOffAuthorisationSystem();
        Community com = Community.create(null, context);
        context.restoreAuthSystemState();

        Collection[] found = Collection.findAuthorized(context, com, Constants.WRITE);
        assertThat("testFindAuthorized 0",found,notNullValue());
        assertTrue("testFindAuthorized 1",found.length == 0);

        found = Collection.findAuthorized(context, null, Constants.WRITE);
        assertThat("testFindAuthorized 2",found,notNullValue());
        assertTrue("testFindAuthorized 3",found.length == 0);

        found = Collection.findAuthorized(context, com, Constants.ADD);
        assertThat("testFindAuthorized 3",found,notNullValue());
        assertTrue("testFindAuthorized 4",found.length == 0);

        found = Collection.findAuthorized(context, null, Constants.ADD);
        assertThat("testFindAuthorized 5",found,notNullValue());
        assertTrue("testFindAuthorized 6",found.length == 0);

        found = Collection.findAuthorized(context, com, Constants.READ);
        assertThat("testFindAuthorized 7",found,notNullValue());
        assertTrue("testFindAuthorized 8",found.length == 0);

        found = Collection.findAuthorized(context, null, Constants.READ);
        assertThat("testFindAuthorized 9",found,notNullValue());
        assertTrue("testFindAuthorized 10",found.length >= 1);
    }

    /**
     * Test of findAuthorizedOptimized method, of class Collection.
     * We create some collections and some users with varying auth, and ensure we can access them all properly.
     */
    @Test
    public void testFindAuthorizedOptimized() throws Exception
    {
        context.turnOffAuthorisationSystem();
        Community com = Community.create(null, context);
        Collection collectionA = Collection.create(context);
        Collection collectionB = Collection.create(context);
        Collection collectionC = Collection.create(context);

        com.addCollection(collectionA);
        com.addCollection(collectionB);
        com.addCollection(collectionC);

        EPerson epersonA = EPerson.create(context);
        EPerson epersonB = EPerson.create(context);
        EPerson epersonC = EPerson.create(context);
        EPerson epersonD = EPerson.create(context);

        //personA can submit to collectionA and collectionC
        AuthorizeManager.addPolicy(context, collectionA, Constants.ADD, epersonA);
        AuthorizeManager.addPolicy(context, collectionC, Constants.ADD, epersonA);

        //personB can submit to collectionB and collectionC
        AuthorizeManager.addPolicy(context, collectionB, Constants.ADD, epersonB);
        AuthorizeManager.addPolicy(context, collectionC, Constants.ADD, epersonB);

        //personC can only submit to collectionC
        AuthorizeManager.addPolicy(context, collectionC, Constants.ADD, epersonC);

        //personD no submission powers

        context.restoreAuthSystemState();

        context.setCurrentUser(epersonA);
        Collection[] personACollections = Collection.findAuthorizedOptimized(context, Constants.ADD);
        assertTrue("testFindAuthorizeOptimized A", personACollections.length == 2);
        List<Collection> aList = Arrays.asList(personACollections);
        assertTrue("testFindAuthorizeOptimized A.A", aList.contains(collectionA));
        assertFalse("testFindAuthorizeOptimized A.A", aList.contains(collectionB));
        assertTrue("testFindAuthorizeOptimized A.A", aList.contains(collectionC));

        context.setCurrentUser(epersonB);
        Collection[] personBCollections = Collection.findAuthorizedOptimized(context, Constants.ADD);
        assertTrue("testFindAuthorizeOptimized B", personBCollections.length == 2);
        List<Collection> bList = Arrays.asList(personBCollections);
        assertFalse("testFindAuthorizeOptimized B.A", bList.contains(collectionA));
        assertTrue("testFindAuthorizeOptimized B.B", bList.contains(collectionB));
        assertTrue("testFindAuthorizeOptimized B.C", bList.contains(collectionC));

        context.setCurrentUser(epersonC);
        Collection[] personCCollections = Collection.findAuthorizedOptimized(context, Constants.ADD);
        assertTrue("testFindAuthorizeOptimized C", personCCollections.length == 1);
        List<Collection> cList = Arrays.asList(personCCollections);
        assertFalse("testFindAuthorizeOptimized C.A", cList.contains(collectionA));
        assertFalse("testFindAuthorizeOptimized C.B", cList.contains(collectionB));
        assertTrue("testFindAuthorizeOptimized C.C", cList.contains(collectionC));

        context.setCurrentUser(epersonD);
        Collection[] personDCollections = Collection.findAuthorizedOptimized(context, Constants.ADD);
        assertTrue("testFindAuthorizeOptimized D", personDCollections.length == 0);
        List<Collection> dList = Arrays.asList(personDCollections);
        assertFalse("testFindAuthorizeOptimized D.A", dList.contains(collectionA));
        assertFalse("testFindAuthorizeOptimized D.B", dList.contains(collectionB));
        assertFalse("testFindAuthorizeOptimized D.C", dList.contains(collectionC));
    }

    /**
     * Test of countItems method, of class Collection.
     */
    @Test
    public void testCountItems() throws Exception
    {
        //0 by default
        assertTrue("testCountItems 0", c.countItems() == 0);
    }

    /**
     * Test of getAdminObject method, of class Collection.
     */
    @Test
    @Override
    public void testGetAdminObject() throws SQLException
    {
        //default community has no admin object
        assertThat("testGetAdminObject 0", (Collection)c.getAdminObject(Constants.REMOVE), equalTo(c));
        assertThat("testGetAdminObject 1", (Collection)c.getAdminObject(Constants.ADD), equalTo(c));
        assertThat("testGetAdminObject 2", c.getAdminObject(Constants.DELETE), nullValue());
        assertThat("testGetAdminObject 3", (Collection)c.getAdminObject(Constants.ADMIN), equalTo(c));
    }

    /**
     * Test of getParentObject method, of class Collection.
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
            Community parent = Community.create(null, context);
            parent.addCollection(c);
            context.commit();
            context.restoreAuthSystemState();
            assertThat("testGetParentObject 1", c.getParentObject(), notNullValue());
            assertThat("testGetParentObject 2", (Community)c.getParentObject(), equalTo(parent));
        }
        catch(AuthorizeException ex)
        {
            fail("Authorize exception catched");
        }
    }

}
