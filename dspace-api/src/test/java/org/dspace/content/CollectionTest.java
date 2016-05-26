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
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;


import org.dspace.authorize.AuthorizeException;
import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.LicenseService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.junit.*;
import static org.junit.Assert.* ;
import static org.hamcrest.CoreMatchers.*;
import mockit.NonStrictExpectations;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.core.Constants;

/**
 * Unit Tests for class Collection
 * @author pvillega
 */
public class CollectionTest extends AbstractDSpaceObjectTest
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(CollectionTest.class);

    private LicenseService licenseService = CoreServiceFactory.getInstance().getLicenseService();

    /**
     * Collection instance for the tests
     */
    private Collection collection;

    private Community owningCommunity;

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
            this.owningCommunity = communityService.create(null, context);
            this.collection = collectionService.create(context, owningCommunity);
            this.dspaceObject = collection;
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
        try {
            if(collection != null){
                context.turnOffAuthorisationSystem();
                collectionService.update(context, collection);
                communityService.update(context, owningCommunity);
                collection = collectionService.find(context, collection.getID());
                if(collection != null)
                {
                    collectionService.delete(context, collection);
                    communityService.delete(context, communityService.find(context, owningCommunity.getID()));
                }
                context.restoreAuthSystemState();
            }

        } catch (SQLException ex) {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init: " + ex.getMessage());
        } catch (AuthorizeException ex) {
            log.error("Authorization Error in init", ex);
            fail("Authorization Error in init: " + ex.getMessage());
        } catch (IOException ex) {
            log.error("IO Error in init", ex);
            fail("IO Error in init: " + ex.getMessage());
        }
        super.destroy();
    }

    /**
     * Test of find method, of class Collection.
     */
    @Test
    public void testCollectionFind() throws Exception
    {
        UUID id = collection.getID();
        Collection found =  collectionService.find(context, id);
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
        new NonStrictExpectations(authorizeService.getClass())
        {{
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.ADD); result = null;

        }};
        Collection created = collectionService.create(context, owningCommunity);
        assertThat("testCreate 0", created, notNullValue());
        assertThat("testCreate 1", created.getName(), equalTo(""));
    }

     /**
     * Test of create method (with specified valid handle), of class Collection
     */
    @Test
    public void testCreateWithValidHandle() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.ADD); result = null;

        }};
        // test creating collection with a specified handle which is NOT already in use
        // (this handle should not already be used by system, as it doesn't start with "1234567689" prefix)
        Collection created = collectionService.create(context, owningCommunity, "987654321/100");

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
        new NonStrictExpectations(authorizeService.getClass())
        {{
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.ADD); result = null;

        }};
        //get handle of our default created collection
        String inUseHandle = collection.getHandle();

        // test creating collection with a specified handle which IS already in use
        // This should throw an exception
        Collection created = collectionService.create(context, owningCommunity, inUseHandle);
        fail("Exception expected");
    }


    /**
     * Test of findAll method, of class Collection.
     */
    @Test
    public void testFindAll() throws Exception
    {
        List<Collection> all = collectionService.findAll(context);
        assertThat("testFindAll 0", all, notNullValue());
        assertTrue("testFindAll 1", all.size() >= 1);

        boolean added = false;
        for(Collection cl: all)
        {
            if(cl.equals(collection))
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
        Iterator<Item> items = itemService.findByCollection(context, collection);
        assertThat("testGetItems 0", items, notNullValue());
        //by default is empty
        assertFalse("testGetItems 1", items.hasNext());
    }

    /**
     * Test of getAllItems method, of class Collection.
     */
    @Test
    public void testGetAllItems() throws Exception
    {
        Iterator<Item> items = itemService.findByCollection(context, collection);
        assertThat("testGetAllItems 0", items, notNullValue());
        //by default is empty
        assertFalse("testGetAllItems 1", items.hasNext());
    }

    /**
     * Test of getID method, of class Collection.
     */
    @Test
    @Override
    public void testGetID()
    {
        assertTrue("testGetID 0", collection.getID() != null);
    }

    @Test
    public void testLegacyID() { assertTrue("testGetLegacyID 0", collection.getLegacyId() == null);}

    /**
     * Test of getHandle method, of class Collection.
     */
    @Test
    @Override
    public void testGetHandle()
    {
        //default instance has a random handle
        assertTrue("testGetHandle 0", collection.getHandle().contains("123456789/"));
    }

    /**
     * Test of getMetadata method, of class Collection.
     */
    @Test
    public void testGetMetadata()
    {
        //by default all empty values will return ""
        assertThat("testGetMetadata 0",collectionService.getMetadata(collection, "name"), equalTo(""));
        assertThat("testGetMetadata 1",collectionService.getMetadata(collection, "short_description"), equalTo(""));
        assertThat("testGetMetadata 2",collectionService.getMetadata(collection, "introductory_text"), equalTo(""));
        assertThat("testGetMetadata 4",collectionService.getMetadata(collection, "copyright_text"), equalTo(""));
        assertThat("testGetMetadata 6",collectionService.getMetadata(collection, "provenance_description"), equalTo(""));
        assertThat("testGetMetadata 7",collectionService.getMetadata(collection, "side_bar_text"), equalTo(""));
        assertThat("testGetMetadata 8",collectionService.getMetadata(collection, "license"), equalTo(""));
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

        collectionService.setMetadata(context, collection, "name", name);
        collectionService.setMetadata(context, collection, "short_description", sdesc);
        collectionService.setMetadata(context, collection, "introductory_text", itext);
        collectionService.setMetadata(context, collection, "copyright_text", copy);
        collectionService.setMetadata(context, collection, "side_bar_text", sidebar);
        collectionService.setMetadata(context, collection, "provenance_description", provDesc);
        collectionService.setMetadata(context, collection, "license", license);

        assertThat("testSetMetadata 0",collectionService.getMetadata(collection, "name"), equalTo(name));
        assertThat("testSetMetadata 1",collectionService.getMetadata(collection, "short_description"), equalTo(sdesc));
        assertThat("testSetMetadata 2",collectionService.getMetadata(collection, "introductory_text"), equalTo(itext));
        assertThat("testSetMetadata 4",collectionService.getMetadata(collection, "copyright_text"), equalTo(copy));
        assertThat("testSetMetadata 5",collectionService.getMetadata(collection, "side_bar_text"), equalTo(sidebar));
        assertThat("testGetMetadata 7",collectionService.getMetadata(collection, "provenance_description"), equalTo(provDesc));
        assertThat("testGetMetadata 8",collectionService.getMetadata(collection, "license"), equalTo(license));
    }

    /**
     * Test of getName method, of class Collection.
     */
    @Test
    @Override
    public void testGetName()
    {
        //by default is empty
        assertThat("testGetName 0",collection.getName(), equalTo(""));
    }

    /**
     * Test of getLogo method, of class Collection.
     */
    @Test
    public void testGetLogo()
    {
        //by default is empty
        assertThat("testGetLogo 0",collection.getLogo(), nullValue());
    }

    /**
     * Test of setLogo method, of class Collection.
     */
    @Test
    public void testSetLogoAuth() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = true;
            // Allow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = true;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = null;
        }};

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream logo = collectionService.setLogo(context, collection, new FileInputStream(f));
        assertThat("testSetLogoAuth 0",collection.getLogo(), equalTo(logo));

        collection.setLogo(null);
        assertThat("testSetLogoAuth 1",collection.getLogo(), nullValue());
    }

    /**
     * Test of setLogo method, of class Collection.
     */
    @Test
    public void testSetLogoAuth2() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = false;
            // Allow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = true;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = null;
        }};

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream logo = collectionService.setLogo(context, collection, new FileInputStream(f));
        assertThat("testSetLogoAuth2 0",collection.getLogo(), equalTo(logo));

        collection.setLogo(null);
        assertThat("testSetLogoAuth2 1",collection.getLogo(), nullValue());
    }

    /**
     * Test of setLogo method, of class Collection.
     */
    @Test
    public void testSetLogoAuth3() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = true;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = false;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = null;
        }};

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream logo = collectionService.setLogo(context, collection, new FileInputStream(f));
        assertThat("testSetLogoAuth3 0",collection.getLogo(), equalTo(logo));

        collection.setLogo(null);
        assertThat("testSetLogoAuth3 1",collection.getLogo(), nullValue());
    }

    /**
     * Test of setLogo method, of class Collection.
     */
    @Test
    public void testSetLogoAuth4() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = false;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = false;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = null;
        }};

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream logo = collectionService.setLogo(context, collection, new FileInputStream(f));
        assertThat("testSetLogoAuth4 0",collection.getLogo(), equalTo(logo));

        collection.setLogo(null);
        assertThat("testSetLogoAuth4 1",collection.getLogo(), nullValue());
    }

    /**
     * Test of setLogo method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testSetLogoNoAuth() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = false;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = false;
            // Disallow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = new AuthorizeException();
        }};

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream logo = collectionService.setLogo(context, collection, new FileInputStream(f));
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
        Group result = collectionService.createWorkflowGroup(context, collection, step);
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
        Group result = collectionService.createWorkflowGroup(context, collection, step);
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
        Group g = groupService.create(context);
        context.restoreAuthSystemState();
        collection.setWorkflowGroup(step, g);
        assertThat("testSetWorkflowGroup 0",collectionService.getWorkflowGroup(collection, step), notNullValue());
        assertThat("testSetWorkflowGroup 1",collectionService.getWorkflowGroup(collection, step), equalTo(g));
    }

    /**
     * Test of getWorkflowGroup method, of class Collection.
     */
    @Test
    public void testGetWorkflowGroup()
    {
        //null by default
        int step = 1;
        assertThat("testGetWorkflowGroup 0",collectionService.getWorkflowGroup(collection, step), nullValue());
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

        Group result = collectionService.createSubmitters(context, collection);
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

        Group result = collectionService.createSubmitters(context, collection);
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

        collectionService.removeSubmitters(context, collection);
        assertThat("testRemoveSubmittersAuth 0", collection.getSubmitters(), nullValue());
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

        collectionService.removeSubmitters(context, collection);
        fail("Exception expected");
    }

    /**
     * Test of getSubmitters method, of class Collection.
     */
    @Test
    public void testGetSubmitters()
    {
        assertThat("testGetSubmitters 0", collection.getSubmitters(), nullValue());
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

        Group result = collectionService.createAdministrators(context, collection);
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

        Group result = collectionService.createAdministrators(context, collection);
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
        Group result = collectionService.createAdministrators(context, collection);
        assertThat("testRemoveAdministratorsAuth 0",collection.getAdministrators(), notNullValue());
        assertThat("testRemoveAdministratorsAuth 1",collection.getAdministrators(), equalTo(result));
        collectionService.removeAdministrators(context, collection);
        assertThat("testRemoveAdministratorsAuth 2", collection.getAdministrators(), nullValue());
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
        Group result = collectionService.createAdministrators(context, collection);
        assertThat("testRemoveAdministratorsAuth 0",collection.getAdministrators(), notNullValue());
        assertThat("testRemoveAdministratorsAuth 1",collection.getAdministrators(), equalTo(result));
        collectionService.removeAdministrators(context, collection);
        fail("Exception expected");
    }

    /**
     * Test of getAdministrators method, of class Collection.
     */
    @Test
    public void testGetAdministrators()
    {
        assertThat("testGetAdministrators 0", collection.getAdministrators(), nullValue());
    }

    /**
     * Test of getLicense method, of class Collection.
     */
    @Test
    public void testGetLicense()
    {
        assertThat("testGetLicense 0", collectionService.getLicense(collection), notNullValue());
        assertThat("testGetLicense 1", collectionService.getLicense(collection), equalTo(licenseService.getDefaultSubmissionLicense()));
    }

    /**
     * Test of getLicenseCollection method, of class Collection.
     */
    @Test
    public void testGetLicenseCollection()
    {
        assertThat("testGetLicenseCollection 0", collection.getLicenseCollection(), notNullValue());
        assertThat("testGetLicenseCollection 1", collection.getLicenseCollection(), equalTo(""));
    }

    /**
     * Test of hasCustomLicense method, of class Collection.
     */
    @Test
    public void testHasCustomLicense()
    {
        assertFalse("testHasCustomLicense 0", collectionService.hasCustomLicense(collection));
    }

    /**
     * Test of setLicense method, of class Collection.
     */
    @Test
    public void testSetLicense() throws SQLException {
        String license = "license for test";
        collection.setLicense(context, license);
        assertThat("testSetLicense 0", collectionService.getLicense(collection), notNullValue());
        assertThat("testSetLicense 1", collectionService.getLicense(collection), equalTo(license));
        assertThat("testSetLicense 2", collection.getLicenseCollection(), notNullValue());
        assertThat("testSetLicense 3", collection.getLicenseCollection(), equalTo(license));
    }

    /**
     * Test of getTemplateItem method, of class Collection.
     */
    @Test
    public void testGetTemplateItem() throws Exception
    {
        assertThat("testGetTemplateItem 0", collection.getTemplateItem(), nullValue());
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

        itemService.createTemplateItem(context, collection);
        assertThat("testCreateTemplateItemAuth 0",collection.getTemplateItem(), notNullValue());
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

        itemService.createTemplateItem(context, collection);
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

        collectionService.removeTemplateItem(context, collection);
        assertThat("testRemoveTemplateItemAuth 0",collection.getTemplateItem(), nullValue());
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

        collectionService.removeTemplateItem(context, collection);
        fail("Exception expected");
    }

    /**
     * Test of addItem method, of class Collection.
     */
    @Test
    public void testAddItemAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Collection ADD permissions
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.ADD); result = null;
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.WRITE); result = null;
        }};

        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
        Item item = installItemService.installItem(context, workspaceItem);
        collectionService.addItem(context, collection, item);
        boolean added = false;
        Iterator<Item> ii = itemService.findByCollection(context, collection);
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
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow Collection ADD permissions
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.ADD); result = new AuthorizeException();
        }};

        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
        Item item = installItemService.installItem(context, workspaceItem);
        collectionService.addItem(context, collection, item);
        fail("Exception expected");
    }

    /**
     * Test of removeItem method, of class Collection.
     */
    @Test
    public void testRemoveItemAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Collection ADD/REMOVE permissions
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.ADD); result = null;
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.REMOVE); result = null;
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.WRITE); result = null;
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.DELETE); result = null;
        }};

        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
        Item item = installItemService.installItem(context, workspaceItem);
        collectionService.addItem(context, collection, item);

        collectionService.removeItem(context, collection, item);
        boolean isthere = false;
        Iterator<Item> ii = itemService.findByCollection(context, collection);
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
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Collection ADD permissions
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.ADD); result = null;
            // Disallow Collection REMOVE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.REMOVE); result = new AuthorizeException();
        }};

        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
        Item item = installItemService.installItem(context, workspaceItem);
        collectionService.addItem(context, collection, item);

        collectionService.removeItem(context, collection, item);
        fail("Exception expected");
    }

    /**
     * Test of update method, of class Collection.
     */
    @Test
    public void testUpdateAuth() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = true;
            // Allow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = true;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = null;
        }};

        //TODO: how to check update?
        collectionService.update(context, collection);
    }

    /**
     * Test of update method, of class Collection.
     */
    @Test
    public void testUpdateAuth2() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = false;
            // Allow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = true;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = null;
        }};

        //TODO: how to check update?
        collectionService.update(context, collection);
    }

    /**
     * Test of update method, of class Collection.
     */
    @Test
    public void testUpdateAuth3() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = true;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = false;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = null;
        }};

        //TODO: how to check update?
        collectionService.update(context, collection);
    }

    /**
     * Test of update method, of class Collection.
     */
    @Test
    public void testUpdateAuth4() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = false;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = false;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = null;
        }};

        //TODO: how to check update?
        collectionService.update(context, collection);
    }

    /**
     * Test of update method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testUpdateNoAuth() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = false;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = false;
            // Disallow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = new AuthorizeException();
        }};

        collectionService.update(context, collection);
        fail("Exception expected");
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = true;
            // Allow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = true;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = null;
        }};

        assertTrue("testCanEditBooleanAuth 0", collectionService.canEditBoolean(context, collection));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth2() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = false;
            // Allow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = true;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = null;
        }};

        assertTrue("testCanEditBooleanAuth2 0", collectionService.canEditBoolean(context, collection));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth3() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = true;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = false;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = null;
        }};

        assertTrue("testCanEditBooleanAuth3 0", collectionService.canEditBoolean(context, collection));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth4() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = false;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = false;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = null;
        }};

        assertTrue("testCanEditBooleanAuth4 0", collectionService.canEditBoolean(context, collection));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanNoAuth() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = false;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = false;
            // Disallow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = new AuthorizeException();
        }};

        assertFalse("testCanEditBooleanNoAuth 0", collectionService.canEditBoolean(context, collection));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth_boolean() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = true;
            // Allow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = true;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = null;
        }};

        assertTrue("testCanEditBooleanAuth_boolean 0", collectionService.canEditBoolean(context, collection, true));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth2_boolean() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = false;
            // Allow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = true;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = null;
        }};

        assertTrue("testCanEditBooleanAuth2_boolean 0", collectionService.canEditBoolean(context, collection, true));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth3_boolean() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = true;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = false;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = null;
        }};

        assertTrue("testCanEditBooleanAuth3_boolean 0", collectionService.canEditBoolean(context, collection, true));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth4_boolean() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = false;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = false;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = null;
        }};

        assertTrue("testCanEditBooleanAuth4_boolean 0", collectionService.canEditBoolean(context, collection, true));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth5_boolean() throws Exception
    {
        // Test permissions with inheritance turned *OFF*
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, false); result = true;
            // Allow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, false); result = true;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, false); result = null;
        }};

        assertTrue("testCanEditBooleanAuth5_boolean 0", collectionService.canEditBoolean(context, collection, false));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth6_boolean() throws Exception
    {
        // Test permissions with inheritance turned *OFF*
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, false); result = false;
            // Allow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, false); result = true;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, false); result = null;
        }};

        assertTrue("testCanEditBooleanAuth6_boolean 0", collectionService.canEditBoolean(context, collection, false));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth7_boolean() throws Exception
    {
        // Test permissions with inheritance turned *OFF*
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, false); result = true;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, false); result = false;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, false); result = null;
        }};

        assertTrue("testCanEditBooleanAuth7_boolean 0", collectionService.canEditBoolean(context, collection, false));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth8_boolean() throws Exception
    {
        // Test permissions with inheritance turned *OFF*
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, false); result = false;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, false); result = false;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, false); result = null;
        }};

        assertTrue("testCanEditBooleanAuth8_boolean 0", collectionService.canEditBoolean(context, collection, false));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanNoAuth_boolean() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = false;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = false;
            // Disallow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = new AuthorizeException();
        }};

        assertFalse("testCanEditBooleanNoAuth_boolean 0",collectionService.canEditBoolean(context, collection, true));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanNoAuth2_boolean() throws Exception
    {
        // Test permissions with inheritance turned *OFF*
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, false); result = false;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, false); result = false;
            // Disallow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, false); result = new AuthorizeException();
        }};

        assertFalse("testCanEditBooleanNoAuth_boolean 0",collectionService.canEditBoolean(context, collection, false));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth_0args() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = true;
            // Allow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = true;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = null;
        }};

        //TODO: how to check??
        collectionService.canEdit(context, collection);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth2_0args() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = false;
            // Allow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = true;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = null;
        }};

        //TODO: how to check??
        collectionService.canEdit(context, collection);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth3_0args() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = true;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = false;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = null;
        }};

        //TODO: how to check??
        collectionService.canEdit(context, collection);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth4_0args() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = false;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = false;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = null;
        }};

        //TODO: how to check??
        collectionService.canEdit(context, collection);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testCanEditNoAuth_0args() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = false;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = false;
            // Disallow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = new AuthorizeException();
        }};

        collectionService.canEdit(context, collection);
        fail("Exception expected");
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth_boolean() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = true;
            // Allow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = true;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = null;
        }};

        //TOO: how to check?
        collectionService.canEdit(context, collection, true);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth2_boolean() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = false;
            // Allow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = true;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = null;
        }};

        //TOO: how to check?
        collectionService.canEdit(context, collection, true);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth3_boolean() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = true;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = false;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = null;
        }};

        //TOO: how to check?
        collectionService.canEdit(context, collection, true);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth4_boolean() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = false;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = false;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = null;
        }};

        //TOO: how to check?
        collectionService.canEdit(context, collection, true);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth5_boolean() throws Exception
    {
        // Test permissions with inheritance turned *OFF*
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, false); result = true;
            // Allow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, false); result = true;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, false); result = null;
        }};

        //TOO: how to check?
        collectionService.canEdit(context, collection, false);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth6_boolean() throws Exception
    {
        // Test permissions with inheritance turned *OFF*
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, false); result = false;
            // Allow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, false); result = true;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, false); result = null;
        }};

        //TOO: how to check?
        collectionService.canEdit(context, collection, false);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth7_boolean() throws Exception
    {
        // Test permissions with inheritance turned *OFF*
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, false); result = true;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, false); result = false;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, false); result = null;
        }};

        //TOO: how to check?
        collectionService.canEdit(context, collection, false);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth8_boolean() throws Exception
    {
        // Test permissions with inheritance turned *OFF*
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, false); result = false;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, false); result = false;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, false); result = null;
        }};

        //TOO: how to check?
        collectionService.canEdit(context, collection, false);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testCanEditNoAuth_boolean() throws Exception
    {
        // Test inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = false;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = false;
            // Disallow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = new AuthorizeException();
        }};

        //TOO: how to check?
        collectionService.canEdit(context, collection, false);
        fail("Exception expected");
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testCanEditNoAuth2_boolean() throws Exception
    {
        // Test permissions with inheritance turned *OFF*
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow parent Community ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, false); result = false;
            // Disallow parent Community WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, false); result = false;
            // Disallow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, false); result = new AuthorizeException();
        }};

        //TOO: how to check?
        collectionService.canEdit(context, collection, true);
        fail("Exception expected");
    }

    /**
     * Test of delete method, of class Collection.
     */
    @Test
    public void testDeleteAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeUtil.class, authorizeService.getClass())
        {{
            // Allow manage TemplateItem perms
            AuthorizeUtil.authorizeManageTemplateItem((Context) any, (Collection) any);
                    result = null;
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, anyBoolean); result = null;
        }};

        UUID id = collection.getID();
        collectionService.delete(context, collection);
        collection = collectionService.find(context, id);
        assertThat("testDelete 0", collection,nullValue());
    }

    /**
     * Test of delete method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testDeleteNoAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeUtil.class, authorizeService.getClass())
        {{
            // Disallow manage TemplateItem perms
            AuthorizeUtil.authorizeManageTemplateItem((Context) any, (Collection) any);
                    result = new AuthorizeException();
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, anyBoolean); result = null;
        }};

        collectionService.delete(context, collection);
        fail("Exception expected");
    }

     /**
     * Test of delete method, of class Collection.
     */
    @Test(expected=AuthorizeException.class)
    public void testDeleteNoAuth2() throws Exception
    {
        new NonStrictExpectations(AuthorizeUtil.class, authorizeService.getClass())
        {{
            // Allow manage TemplateItem perms
            AuthorizeUtil.authorizeManageTemplateItem((Context) any, (Collection) any);
                    result = null;
            // Disallow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, anyBoolean); result = new AuthorizeException();
        }};

        collectionService.delete(context, collection);
        fail("Exception expected");
    }

    /**
     * Test of getCommunities method, of class Collection.
     */
    @Test
    public void testGetCommunities() throws Exception
    {
        assertThat("testGetCommunities 0",collection.getCommunities(), notNullValue());
        assertTrue("testGetCommunities 1",collection.getCommunities().size() == 1);
    }

    /**
     * Test of equals method, of class Collection.
     */
    @Test
    @SuppressWarnings("ObjectEqualsNull")
    public void testEquals() throws SQLException, AuthorizeException
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            authorizeService.authorizeAction((Context) any, (Community) any,
                    Constants.ADD); result = null;
        }};
        assertFalse("testEquals 0",collection.equals(null));
        assertFalse("testEquals 1",collection.equals(collectionService.create(context, owningCommunity)));
        assertTrue("testEquals 2", collection.equals(collection));
    }

    /**
     * Test of getType method, of class Collection.
     */
    @Test
    @Override
    public void testGetType()
    {
        assertThat("testGetType 0", collection.getType(), equalTo(Constants.COLLECTION));
    }

    /**
     * Test of findAuthorized method, of class Collection.
     */
    @Test
    public void testFindAuthorized() throws Exception
    {
        context.turnOffAuthorisationSystem();
        Community com = communityService.create(null, context);
        context.restoreAuthSystemState();

        List<Collection> found = collectionService.findAuthorized(context, com, Constants.WRITE);
        assertThat("testFindAuthorized 0",found,notNullValue());
        assertTrue("testFindAuthorized 1",found.size() == 0);

        found = collectionService.findAuthorized(context, null, Constants.WRITE);
        assertThat("testFindAuthorized 2",found,notNullValue());
        assertTrue("testFindAuthorized 3",found.size() == 0);

        found = collectionService.findAuthorized(context, com, Constants.ADD);
        assertThat("testFindAuthorized 3",found,notNullValue());
        assertTrue("testFindAuthorized 4",found.size() == 0);

        found = collectionService.findAuthorized(context, null, Constants.ADD);
        assertThat("testFindAuthorized 5",found,notNullValue());
        assertTrue("testFindAuthorized 6",found.size() == 0);

        found = collectionService.findAuthorized(context, com, Constants.READ);
        assertThat("testFindAuthorized 7",found,notNullValue());
        assertTrue("testFindAuthorized 8",found.size() == 0);

        found = collectionService.findAuthorized(context, null, Constants.READ);
        assertThat("testFindAuthorized 9",found,notNullValue());
        assertTrue("testFindAuthorized 10",found.size() >= 1);
    }

    /**
     * Test of findAuthorizedOptimized method, of class Collection.
     * We create some collections and some users with varying auth, and ensure we can access them all properly.
     */
    @Test
    public void testFindAuthorizedOptimized() throws Exception
    {
        context.turnOffAuthorisationSystem();
        Community com = communityService.create(null, context);
        Collection collectionA = collectionService.create(context, com);
        Collection collectionB = collectionService.create(context, com);
        Collection collectionC = collectionService.create(context, com);

        com.addCollection(collectionA);
        com.addCollection(collectionB);
        com.addCollection(collectionC);

        EPerson epersonA = ePersonService.create(context);
        EPerson epersonB = ePersonService.create(context);
        EPerson epersonC = ePersonService.create(context);
        EPerson epersonD = ePersonService.create(context);

        //personA can submit to collectionA and collectionC
        authorizeService.addPolicy(context, collectionA, Constants.ADD, epersonA);
        authorizeService.addPolicy(context, collectionC, Constants.ADD, epersonA);

        //personB can submit to collectionB and collectionC
        authorizeService.addPolicy(context, collectionB, Constants.ADD, epersonB);
        authorizeService.addPolicy(context, collectionC, Constants.ADD, epersonB);

        //personC can only submit to collectionC
        authorizeService.addPolicy(context, collectionC, Constants.ADD, epersonC);

        //personD no submission powers

        context.restoreAuthSystemState();

        context.setCurrentUser(epersonA);
        List<Collection> personACollections = collectionService.findAuthorizedOptimized(context, Constants.ADD);
        assertTrue("testFindAuthorizeOptimized A", personACollections.size() == 2);
        assertTrue("testFindAuthorizeOptimized A.A", personACollections.contains(collectionA));
        assertFalse("testFindAuthorizeOptimized A.A", personACollections.contains(collectionB));
        assertTrue("testFindAuthorizeOptimized A.A", personACollections.contains(collectionC));

        context.setCurrentUser(epersonB);
        List<Collection> personBCollections = collectionService.findAuthorizedOptimized(context, Constants.ADD);
        assertTrue("testFindAuthorizeOptimized B", personBCollections.size() == 2);
        assertFalse("testFindAuthorizeOptimized B.A", personBCollections.contains(collectionA));
        assertTrue("testFindAuthorizeOptimized B.B", personBCollections.contains(collectionB));
        assertTrue("testFindAuthorizeOptimized B.C", personBCollections.contains(collectionC));

        context.setCurrentUser(epersonC);
        List<Collection> personCCollections = collectionService.findAuthorizedOptimized(context, Constants.ADD);
        assertTrue("testFindAuthorizeOptimized C", personCCollections.size() == 1);
        assertFalse("testFindAuthorizeOptimized collection.A", personCCollections.contains(collectionA));
        assertFalse("testFindAuthorizeOptimized collection.B", personCCollections.contains(collectionB));
        assertTrue("testFindAuthorizeOptimized collection.C", personCCollections.contains(collectionC));

        context.setCurrentUser(epersonD);
        List<Collection> personDCollections = collectionService.findAuthorizedOptimized(context, Constants.ADD);
        assertTrue("testFindAuthorizeOptimized D", personDCollections.size() == 0);
        assertFalse("testFindAuthorizeOptimized D.A", personDCollections.contains(collectionA));
        assertFalse("testFindAuthorizeOptimized D.B", personDCollections.contains(collectionB));
        assertFalse("testFindAuthorizeOptimized D.C", personDCollections.contains(collectionC));
    }

    /**
     * Test of countItems method, of class Collection.
     */
    @Test
    public void testCountItems() throws Exception
    {
        //0 by default
        assertTrue("testCountItems 0", itemService.countItems(context, collection) == 0);
        
        //NOTE: a more thorough test of item counting is in ITCommunityCollection integration test
    }

    /**
     * Test of getAdminObject method, of class Collection.
     */
    @Test
    @Override
    public void testGetAdminObject() throws SQLException
    {
        //default community has no admin object
        assertThat("testGetAdminObject 0", (Collection)collectionService.getAdminObject(context, collection, Constants.REMOVE), equalTo(collection));
        assertThat("testGetAdminObject 1", (Collection)collectionService.getAdminObject(context, collection, Constants.ADD), equalTo(collection));
        assertThat("testGetAdminObject 2", collectionService.getAdminObject(context, collection, Constants.DELETE), instanceOf(Community.class));
        assertThat("testGetAdminObject 3", collectionService.getAdminObject(context, collection, Constants.ADMIN), instanceOf(Collection.class));
    }

    /**
     * Test of getParentObject method, of class Collection.
     */
    @Test
    @Override
    public void testGetParentObject() throws SQLException
    {
        assertThat("testGetParentObject 1", collectionService.getParentObject(context, collection), notNullValue());
        assertThat("testGetParentObject 2", (Community)collectionService.getParentObject(context, collection), equalTo(owningCommunity));
    }

}
