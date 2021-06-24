/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.io.FileInputStream;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.LicenseService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit Tests for class Collection
 *
 * @author pvillega
 */
public class CollectionTest extends AbstractDSpaceObjectTest {

    /**
     * log4j category
     */
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(CollectionTest.class);

    private final LicenseService licenseService = CoreServiceFactory.getInstance().getLicenseService();

    /**
     * Collection instance for the tests
     */
    private Collection collection;

    private Community owningCommunity;

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
        try {
            //we have to create a new community in the database
            context.turnOffAuthorisationSystem();
            this.owningCommunity = communityService.create(null, context);
            this.collection = collectionService.create(context, owningCommunity);
            this.dspaceObject = collection;
            //we need to commit the changes so we don't block the table for testing
            context.restoreAuthSystemState();

            // Initialize our spy of the autowired (global) authorizeService bean.
            // This allows us to customize the bean's method return values in tests below
            authorizeServiceSpy = spy(authorizeService);
            // "Wire" our spy to be used by the current loaded object services
            // (To ensure these services use the spy instead of the real service)
            ReflectionTestUtils.setField(communityService, "authorizeService", authorizeServiceSpy);
            ReflectionTestUtils.setField(collectionService, "authorizeService", authorizeServiceSpy);
            ReflectionTestUtils.setField(itemService, "authorizeService", authorizeServiceSpy);
            // Also wire into current AuthorizeServiceFactory, as that is used for some checks (e.g. AuthorizeUtil)
            ReflectionTestUtils.setField(AuthorizeServiceFactory.getInstance(), "authorizeService",
                                         authorizeServiceSpy);
        } catch (AuthorizeException ex) {
            log.error("Authorization Error in init", ex);
            fail("Authorization Error in init: " + ex.getMessage());
        } catch (SQLException ex) {
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
    public void destroy() {
        context.turnOffAuthorisationSystem();
        // Delete community & collection created in init()
        try {
            collectionService.delete(context, collection);
        } catch (Exception e) {
            // ignore
        }
        try {
            communityService.delete(context, owningCommunity);
        } catch (Exception e) {
            // ignore
        }
        context.restoreAuthSystemState();

        collection = null;
        owningCommunity = null;
        super.destroy();
    }

    /**
     * Test of find method, of class Collection.
     */
    @Test
    public void testCollectionFind() throws Exception {
        UUID id = collection.getID();
        Collection found = collectionService.find(context, id);
        assertThat("testCollectionFind 0", found, notNullValue());
        assertThat("testCollectionFind 1", found.getID(), equalTo(id));
        //the community created by default has no name
        assertThat("testCollectionFind 2", found.getName(), equalTo(""));
    }

    /**
     * Test of create method, of class Collection.
     */
    @Test
    public void testCreate() throws Exception {
        // Allow Community ADD perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, owningCommunity, Constants.ADD);
        doNothing().when(authorizeServiceSpy).authorizeAction(context, owningCommunity, Constants.ADD, true);

        Collection created = collectionService.create(context, owningCommunity);
        assertThat("testCreate 0", created, notNullValue());
        assertThat("testCreate 1", created.getName(), equalTo(""));
    }

    /**
     * Test of create method (with specified valid handle), of class Collection
     */
    @Test
    public void testCreateWithValidHandle() throws Exception {
        // Allow Community ADD perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, owningCommunity, Constants.ADD);
        doNothing().when(authorizeServiceSpy).authorizeAction(context, owningCommunity, Constants.ADD, true);

        // provide additional prefixes to the configuration in order to support them
        final ConfigurationService configurationService = new DSpace().getConfigurationService();
        String handleAdditionalPrefixes = configurationService.getProperty("handle.additional.prefixes");

        try {
        configurationService.setProperty("handle.additional.prefixes", "987654321");

        // test creating collection with a specified handle which is NOT already in use
        // (this handle should not already be used by system, as it doesn't start with "1234567689" prefix)
        Collection created = collectionService.create(context, owningCommunity, "987654321/100");

        // check that collection was created, and that its handle was set to proper value
        assertThat("testCreateWithValidHandle 0", created, notNullValue());
        assertThat("testCreateWithValidHandle 1", created.getHandle(), equalTo("987654321/100"));

        } finally {
            configurationService.setProperty("handle.additional.prefixes", handleAdditionalPrefixes);
        }
    }


    /**
     * Test of create method (with specified invalid handle), of class Collection.
     */
    @Test(expected = IllegalStateException.class)
    public void testCreateWithInvalidHandle() throws Exception {
        // Allow Community ADD perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, owningCommunity, Constants.ADD);

        //get handle of our default created collection
        String inUseHandle = collection.getHandle();

        // test creating collection with a specified handle which IS already in use
        // This should throw an exception
        collectionService.create(context, owningCommunity, inUseHandle);
        fail("Exception expected");
    }


    /**
     * Test of findAll method, of class Collection.
     */
    @Test
    public void testFindAll() throws Exception {
        List<Collection> all = collectionService.findAll(context);
        assertThat("testFindAll 0", all, notNullValue());
        assertTrue("testFindAll 1", all.size() >= 1);

        boolean added = false;
        for (Collection cl : all) {
            if (cl.equals(collection)) {
                added = true;
            }
        }
        assertTrue("testFindAll 2", added);
    }

    /**
     * Test of getItems method, of class Collection.
     */
    @Test
    public void testGetItems() throws Exception {
        Iterator<Item> items = itemService.findByCollection(context, collection);
        assertThat("testGetItems 0", items, notNullValue());
        //by default is empty
        assertFalse("testGetItems 1", items.hasNext());
    }

    /**
     * Test of getAllItems method, of class Collection.
     */
    @Test
    public void testGetAllItems() throws Exception {
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
    public void testGetID() {
        assertTrue("testGetID 0", collection.getID() != null);
    }

    @Test
    public void testLegacyID() {
        assertTrue("testGetLegacyID 0", collection.getLegacyId() == null);
    }

    /**
     * Test of getHandle method, of class Collection.
     */
    @Test
    @Override
    public void testGetHandle() {
        //default instance has a random handle
        assertTrue("testGetHandle 0", collection.getHandle().contains("123456789/"));
    }

    /**
     * Test of setMetadata method, of class Collection.
     * @throws java.sql.SQLException if metadata cannot be set.
     */
    @Test
    public void testSetMetadata() throws SQLException {
        String name = "name";
        String sdesc = "short description";
        String itext = "introductory text";
        String copy = "copyright declaration";
        String sidebar = "side bar text";
        String provDesc = "provenance description";
        String license = "license text";

        collectionService.setMetadataSingleValue(context, collection,
                CollectionService.MD_NAME, null, name);
        collectionService.setMetadataSingleValue(context, collection,
                CollectionService.MD_SHORT_DESCRIPTION, null, sdesc);
        collectionService.setMetadataSingleValue(context, collection,
                CollectionService.MD_INTRODUCTORY_TEXT, null, itext);
        collectionService.setMetadataSingleValue(context, collection,
                CollectionService.MD_COPYRIGHT_TEXT, null, copy);
        collectionService.setMetadataSingleValue(context, collection,
                CollectionService.MD_SIDEBAR_TEXT, null, sidebar);
        collectionService.setMetadataSingleValue(context, collection,
                CollectionService.MD_PROVENANCE_DESCRIPTION, null, provDesc);
        collectionService.setMetadataSingleValue(context, collection,
                CollectionService.MD_LICENSE, null, license);

        assertEquals("Name was not set properly.", name,
                collectionService.getMetadataFirstValue(collection,
                        CollectionService.MD_NAME, Item.ANY));
        assertEquals("Short description was not set properly.", sdesc,
                collectionService.getMetadataFirstValue(collection,
                        CollectionService.MD_SHORT_DESCRIPTION, Item.ANY));
        assertEquals("Introductory text was not set properly.", itext,
                collectionService.getMetadataFirstValue(collection,
                        CollectionService.MD_INTRODUCTORY_TEXT, Item.ANY));
        assertEquals("Copyright text was not set properly.", copy,
                collectionService.getMetadataFirstValue(collection,
                        CollectionService.MD_COPYRIGHT_TEXT, Item.ANY));
        assertEquals("Sidebar text was not set properly.", sidebar,
                collectionService.getMetadataFirstValue(collection,
                        CollectionService.MD_SIDEBAR_TEXT, Item.ANY));
        assertEquals("Provenance was not set properly.", provDesc,
                collectionService.getMetadataFirstValue(collection,
                        CollectionService.MD_PROVENANCE_DESCRIPTION, Item.ANY));
        assertEquals("License text was not set properly.", license,
                collectionService.getMetadataFirstValue(collection,
                        CollectionService.MD_LICENSE, Item.ANY));
    }

    /**
     * Test of getName method, of class Collection.
     */
    @Test
    @Override
    public void testGetName() {
        //by default is empty
        assertThat("testGetName 0", collection.getName(), equalTo(""));
    }

    /**
     * Test of getLogo method, of class Collection.
     */
    @Test
    public void testGetLogo() {
        //by default is empty
        assertThat("testGetLogo 0", collection.getLogo(), nullValue());
    }

    /**
     * Test of setLogo method, of class Collection.
     */
    @Test
    public void testSetLogoAuth() throws Exception {
        // Allow Collection WRITE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, collection, Constants.WRITE, true);

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream logo = collectionService.setLogo(context, collection, new FileInputStream(f));
        assertThat("testSetLogoAuth 0", collection.getLogo(), equalTo(logo));

        collection.setLogo(null);
        assertThat("testSetLogoAuth 1", collection.getLogo(), nullValue());
    }

    /**
     * Test of setLogo method, of class Collection.
     */
    @Test
    public void testSetLogoAuth2() throws Exception {
        // Allow parent Community WRITE perms (test inheritance to Collection)
        doNothing().when(authorizeServiceSpy).authorizeAction(context, owningCommunity, Constants.WRITE, true);

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream logo = collectionService.setLogo(context, collection, new FileInputStream(f));
        assertThat("testSetLogoAuth 0", collection.getLogo(), equalTo(logo));

        collection.setLogo(null);
        assertThat("testSetLogoAuth 1", collection.getLogo(), nullValue());
    }

    /**
     * Test of setLogo method, of class Collection.
     */
    @Test(expected = AuthorizeException.class)
    public void testSetLogoNoAuth() throws Exception {
        File f = new File(testProps.get("test.bitstream").toString());
        collectionService.setLogo(context, collection, new FileInputStream(f));
        fail("Exception expected");
    }

    /**
     * Test of createWorkflowGroup method, of class Collection.
     */
    @Test
    public void testCreateWorkflowGroupAuth() throws Exception {
        // Allow Collection ADMIN (to manage workflow group)
        doNothing().when(authorizeServiceSpy).authorizeAction(context, collection, Constants.ADMIN);

        int step = 1;
        Group result = collectionService.createWorkflowGroup(context, collection, step);
        assertThat("testCreateWorkflowGroupAuth 0", result, notNullValue());
    }

    /**
     * Test of createWorkflowGroup method, of class Collection.
     */
    @Test(expected = AuthorizeException.class)
    public void testCreateWorkflowGroupNoAuth() throws Exception {
        int step = 1;
        collectionService.createWorkflowGroup(context, collection, step);
        fail("Exception expected");
    }

    /**
     * Test of setWorkflowGroup method, of class Collection.
     */
    @Test
    public void testSetWorkflowGroup() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem(); //must be an Admin to create a Group
        int step = 1;
        Group g = groupService.create(context);
        context.restoreAuthSystemState();
        collection.setWorkflowGroup(context, step, g);
        assertThat("testSetWorkflowGroup 0", collectionService.getWorkflowGroup(context, collection, step),
                notNullValue());
        assertThat("testSetWorkflowGroup 1", collectionService.getWorkflowGroup(context, collection, step), equalTo(g));
    }

    /**
     * Test of setWorkflowGroup method, of class Collection.
     * The setWorkflowGroup ajust the policies for the basic Workflow. This test
     * shall assure that now exception (e.g. ConcurrentModificationException is
     * thrown during these adjustments.
     */
    @Test
    public void testChangeWorkflowGroup() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem(); //must be an Admin to create a Group
        int step = 1;
        Group g1 = groupService.create(context);
        Group g2 = groupService.create(context);
        context.restoreAuthSystemState();
        collection.setWorkflowGroup(context, step, g1);
        collection.setWorkflowGroup(context, step, g2);
        assertThat("testSetWorkflowGroup 0", collectionService.getWorkflowGroup(context, collection, step),
                notNullValue());
        assertThat("testSetWorkflowGroup 1", collectionService.getWorkflowGroup(context, collection, step),
                equalTo(g2));
    }

    /**
     * Test of getWorkflowGroup method, of class Collection.
     */
    @Test
    public void testGetWorkflowGroup() {
        //null by default
        int step = 1;
        assertThat("testGetWorkflowGroup 0", collectionService.getWorkflowGroup(context, collection, step),
                nullValue());
    }

    /**
     * Test of createSubmitters method, of class Collection.
     */
    @Test
    public void testCreateSubmittersAuth() throws Exception {
        // Allow Collection ADMIN (to manage submitter group)
        doNothing().when(authorizeServiceSpy).authorizeAction(context, collection, Constants.ADMIN);

        Group result = collectionService.createSubmitters(context, collection);
        assertThat("testCreateSubmittersAuth 0", result, notNullValue());
    }

    /**
     * Test of createSubmitters method, of class Collection.
     */
    @Test(expected = AuthorizeException.class)
    public void testCreateSubmittersNoAuth() throws Exception {
        collectionService.createSubmitters(context, collection);
        fail("Exception expected");
    }

    /**
     * Test of removeSubmitters method, of class Collection.
     */
    @Test
    public void testRemoveSubmittersAuth() throws Exception {
        // Allow Collection ADMIN (to manage submitter group)
        doNothing().when(authorizeServiceSpy).authorizeAction(context, collection, Constants.ADMIN);

        collectionService.removeSubmitters(context, collection);
        assertThat("testRemoveSubmittersAuth 0", collection.getSubmitters(), nullValue());
    }

    /**
     * Test of removeSubmitters method, of class Collection.
     */
    @Test(expected = AuthorizeException.class)
    public void testRemoveSubmittersNoAuth() throws Exception {
        collectionService.removeSubmitters(context, collection);
        fail("Exception expected");
    }

    /**
     * Test of getSubmitters method, of class Collection.
     */
    @Test
    public void testGetSubmitters() {
        assertThat("testGetSubmitters 0", collection.getSubmitters(), nullValue());
    }

    /**
     * Test of createAdministrators method, of class Collection.
     */
    @Test
    public void testCreateAdministratorsAuth() throws Exception {
        // Allow Collection ADMIN (to manage admin group)
        doNothing().when(authorizeServiceSpy).authorizeAction(context, collection, Constants.ADMIN);

        Group result = collectionService.createAdministrators(context, collection);
        assertThat("testCreateAdministratorsAuth 0", result, notNullValue());
    }

    /**
     * Test of createAdministrators method, of class Collection.
     */
    @Test(expected = AuthorizeException.class)
    public void testCreateAdministratorsNoAuth() throws Exception {
        collectionService.createAdministrators(context, collection);
        fail("Exception expected");
    }

    /**
     * Test of removeAdministrators method, of class Collection.
     */
    @Test
    public void testRemoveAdministratorsAuth() throws Exception {
        // Allow parent Community ADMIN (only Community Admins can delete a Collection Admin group)
        doNothing().when(authorizeServiceSpy).authorizeAction(context, owningCommunity, Constants.ADMIN);

        // Ensure admin group is created first
        context.turnOffAuthorisationSystem();
        Group result = collectionService.createAdministrators(context, collection);
        context.restoreAuthSystemState();

        assertThat("testRemoveAdministratorsAuth 0", collection.getAdministrators(), notNullValue());
        assertThat("testRemoveAdministratorsAuth 1", collection.getAdministrators(), equalTo(result));
        collectionService.removeAdministrators(context, collection);
        assertThat("testRemoveAdministratorsAuth 2", collection.getAdministrators(), nullValue());
    }

    /**
     * Test of removeAdministrators method, of class Collection.
     */
    @Test(expected = AuthorizeException.class)
    public void testRemoveAdministratorsNoAuth() throws Exception {
        // Ensure admin group is created first
        context.turnOffAuthorisationSystem();
        Group result = collectionService.createAdministrators(context, collection);
        context.restoreAuthSystemState();

        assertThat("testRemoveAdministratorsAuth 0", collection.getAdministrators(), notNullValue());
        assertThat("testRemoveAdministratorsAuth 1", collection.getAdministrators(), equalTo(result));
        collectionService.removeAdministrators(context, collection);
        fail("Exception expected");
    }

    /**
     * Test of getAdministrators method, of class Collection.
     */
    @Test
    public void testGetAdministrators() {
        assertThat("testGetAdministrators 0", collection.getAdministrators(), nullValue());
    }

    /**
     * Test of getLicense method, of class Collection.
     */
    @Test
    public void testGetLicense() {
        assertThat("testGetLicense 0", collectionService.getLicense(collection), notNullValue());
        assertThat("testGetLicense 1", collectionService.getLicense(collection),
                   equalTo(licenseService.getDefaultSubmissionLicense()));
    }

    /**
     * Test of getLicenseCollection method, of class Collection.
     */
    @Test
    public void testGetLicenseCollection() {
        assertThat("testGetLicenseCollection 0", collection.getLicenseCollection(), notNullValue());
        assertThat("testGetLicenseCollection 1", collection.getLicenseCollection(), equalTo(""));
    }

    /**
     * Test of hasCustomLicense method, of class Collection.
     */
    @Test
    public void testHasCustomLicense() {
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
    public void testGetTemplateItem() throws Exception {
        assertThat("testGetTemplateItem 0", collection.getTemplateItem(), nullValue());
    }

    /**
     * Test of createTemplateItem method, of class Collection.
     */
    @Test
    public void testCreateTemplateItemAuth() throws Exception {
        // Allow Collection ADMIN (to manage template item)
        doNothing().when(authorizeServiceSpy).authorizeAction(context, collection, Constants.ADMIN);

        itemService.createTemplateItem(context, collection);
        assertThat("testCreateTemplateItemAuth 0", collection.getTemplateItem(), notNullValue());
    }

    /**
     * Test of createTemplateItem method, of class Collection.
     */
    @Test(expected = AuthorizeException.class)
    public void testCreateTemplateItemNoAuth() throws Exception {
        itemService.createTemplateItem(context, collection);
        fail("Exception expected");
    }

    /**
     * Test of removeTemplateItem method, of class Collection.
     */
    @Test
    public void testRemoveTemplateItemAuth() throws Exception {
        // Allow Collection ADMIN (to manage template item)
        doNothing().when(authorizeServiceSpy).authorizeAction(context, collection, Constants.ADMIN);

        collectionService.removeTemplateItem(context, collection);
        assertThat("testRemoveTemplateItemAuth 0", collection.getTemplateItem(), nullValue());
    }

    /**
     * Test of removeTemplateItem method, of class Collection.
     */
    @Test(expected = AuthorizeException.class)
    public void testRemoveTemplateItemNoAuth() throws Exception {
        collectionService.removeTemplateItem(context, collection);
        fail("Exception expected");
    }

    /**
     * Test of addItem method, of class Collection.
     */
    @Test
    public void testAddItemAuth() throws Exception {
        // Allow Collection ADD perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, collection, Constants.ADD);

        // create item first
        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
        Item item = installItemService.installItem(context, workspaceItem);
        context.restoreAuthSystemState();

        collectionService.addItem(context, collection, item);
        boolean added = false;
        Iterator<Item> ii = itemService.findByCollection(context, collection);
        while (ii.hasNext()) {
            if (ii.next().equals(item)) {
                added = true;
            }
        }
        assertTrue("testAddItemAuth 0", added);
    }

    /**
     * Test of addItem method, of class Collection.
     */
    @Test(expected = AuthorizeException.class)
    public void testAddItemNoAuth() throws Exception {
        // Disallow Collection ADD perms
        doThrow(new AuthorizeException()).when(authorizeServiceSpy).authorizeAction(context, collection, Constants.ADD);

        // create item first
        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
        Item item = installItemService.installItem(context, workspaceItem);
        context.restoreAuthSystemState();

        collectionService.addItem(context, collection, item);
        fail("Exception expected");
    }

    /**
     * Test of removeItem method, of class Collection.
     */
    @Test
    public void testRemoveItemAuth() throws Exception {
        // Allow Collection REMOVE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, collection, Constants.REMOVE);
        // Allow Item DELETE perms
        doNothing().when(authorizeServiceSpy)
                   .authorizeAction(any(Context.class), any(Item.class), eq(Constants.DELETE));
        // Allow Item REMOVE perms
        doNothing().when(authorizeServiceSpy)
                   .authorizeAction(any(Context.class), any(Item.class), eq(Constants.REMOVE));
        // Allow Item WRITE perms (Needed to remove identifiers, e.g. DOI, before Item deletion)
        doNothing().when(authorizeServiceSpy)
                   .authorizeAction(any(Context.class), any(Item.class), eq(Constants.WRITE));

        // create & add item first
        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
        Item item = installItemService.installItem(context, workspaceItem);
        collectionService.addItem(context, collection, item);
        context.restoreAuthSystemState();

        collectionService.removeItem(context, collection, item);
        boolean isthere = false;
        Iterator<Item> ii = itemService.findByCollection(context, collection);
        while (ii.hasNext()) {
            if (ii.next().equals(item)) {
                isthere = true;
            }
        }
        assertFalse("testRemoveItemAuth 0", isthere);
    }

    /**
     * Test of removeItem method, of class Collection.
     */
    @Test(expected = AuthorizeException.class)
    public void testRemoveItemNoAuth() throws Exception {
        // Disallow Collection REMOVE perms
        doThrow(new AuthorizeException()).when(authorizeServiceSpy)
                                         .authorizeAction(context, collection, Constants.REMOVE);

        // create & add item first
        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
        Item item = installItemService.installItem(context, workspaceItem);
        collectionService.addItem(context, collection, item);
        context.restoreAuthSystemState();

        collectionService.removeItem(context, collection, item);
        fail("Exception expected");
    }

    /**
     * Test of update method, of class Collection.
     */
    @Test
    public void testUpdateAuth() throws Exception {
        // Allow Collection WRITE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, collection, Constants.WRITE, true);

        //TODO: how to check update?
        collectionService.update(context, collection);
    }

    /**
     * Test of update method, of class Collection.
     */
    @Test
    public void testUpdateAuth2() throws Exception {
        // Allow parent Community WRITE perms (test inheritance to Collection)
        doNothing().when(authorizeServiceSpy).authorizeAction(context, owningCommunity, Constants.WRITE, true);

        //TODO: how to check update?
        collectionService.update(context, collection);
    }

    /**
     * Test of update method, of class Collection.
     */
    @Test(expected = AuthorizeException.class)
    public void testUpdateNoAuth() throws Exception {
        // Disallow Collection WRITE perms
        doThrow(new AuthorizeException()).when(authorizeServiceSpy)
                                         .authorizeAction(context, collection, Constants.WRITE, true);

        collectionService.update(context, collection);
        fail("Exception expected");
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth() throws Exception {
        // Allow Collection WRITE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, collection, Constants.WRITE, true);

        assertTrue("testCanEditBooleanAuth 0", collectionService.canEditBoolean(context, collection));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth2() throws Exception {
        // Allow parent Community WRITE perms (test inheritance to Collection)
        doNothing().when(authorizeServiceSpy).authorizeAction(context, owningCommunity, Constants.WRITE, true);


        assertTrue("testCanEditBooleanAuth2 0", collectionService.canEditBoolean(context, collection));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanNoAuth() throws Exception {
        // Disallow Collection WRITE perms
        doThrow(new AuthorizeException()).when(authorizeServiceSpy)
                                         .authorizeAction(context, collection, Constants.WRITE, true);

        assertFalse("testCanEditBooleanNoAuth 0", collectionService.canEditBoolean(context, collection));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth_useInheritance() throws Exception {
        // Allow Collection WRITE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, collection, Constants.WRITE, true);

        assertTrue("testCanEditBooleanAuth_useInheritance",
                   collectionService.canEditBoolean(context, collection, true));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth2_useInheritance() throws Exception {
        // Allow parent Community WRITE perms (test inheritance to Collection)
        doNothing().when(authorizeServiceSpy).authorizeAction(context, owningCommunity, Constants.WRITE, true);

        assertTrue("testCanEditBooleanAuth2_useInheritance",
                   collectionService.canEditBoolean(context, collection, true));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth_noInheritance() throws Exception {
        // Allow Collection WRITE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, collection, Constants.WRITE, false);

        assertTrue("testCanEditBooleanAuth_noInheritance",
                   collectionService.canEditBoolean(context, collection, false));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanAuth2_noInheritance() throws Exception {
        assertFalse("testCanEditBooleanAuth_noInheritance",
                    collectionService.canEditBoolean(context, collection, false));
    }


    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanNoAuth_boolean() throws Exception {
        // Disallow Collection WRITE perms
        doThrow(new AuthorizeException()).when(authorizeServiceSpy)
                                         .authorizeAction(context, collection, Constants.WRITE, true);

        assertFalse("testCanEditBooleanNoAuth_boolean 0", collectionService.canEditBoolean(context, collection, true));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditBooleanNoAuth2_boolean() throws Exception {
        // Disallow Collection WRITE perms
        doThrow(new AuthorizeException()).when(authorizeServiceSpy)
                                         .authorizeAction(context, collection, Constants.WRITE, false);

        assertFalse("testCanEditBooleanNoAuth_boolean 0", collectionService.canEditBoolean(context, collection, false));
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth_0args() throws Exception {
        // Allow Collection WRITE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, collection, Constants.WRITE, true);

        //TODO: how to check??
        collectionService.canEdit(context, collection);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test(expected = AuthorizeException.class)
    public void testCanEditNoAuth_0args() throws Exception {
        // Disallow Collection WRITE perms
        doThrow(new AuthorizeException()).when(authorizeServiceSpy)
                                         .authorizeAction(context, collection, Constants.WRITE, true);

        collectionService.canEdit(context, collection);
        fail("Exception expected");
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth_boolean() throws Exception {
        // Allow Collection WRITE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, collection, Constants.WRITE, true);

        //TODO: how to check?
        collectionService.canEdit(context, collection, true);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth2_boolean() throws Exception {
        // Allow Collection WRITE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, collection, Constants.WRITE, false);

        //TODO: how to check?
        collectionService.canEdit(context, collection, false);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test
    public void testCanEditAuth3_boolean() throws Exception {
        // Allow parent Community WRITE perms (test inheritance to Collection)
        doNothing().when(authorizeServiceSpy).authorizeAction(context, owningCommunity, Constants.WRITE, true);

        collectionService.canEdit(context, collection, true);
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test(expected = AuthorizeException.class)
    public void testCanEditNoAuth_boolean() throws Exception {
        collectionService.canEdit(context, collection, false);
        fail("Exception expected");
    }

    /**
     * Test of canEditBoolean method, of class Collection.
     */
    @Test(expected = AuthorizeException.class)
    public void testCanEditNoAuth2_boolean() throws Exception {
        collectionService.canEdit(context, collection, true);
        fail("Exception expected");
    }

    /**
     * Test of delete method, of class Collection.
     */
    @Test
    public void testDeleteAuth() throws Exception {
        // Allow Collection WRITE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, collection, Constants.WRITE, true);
        // Allow Collection ADMIN perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, collection, Constants.ADMIN);

        UUID id = collection.getID();
        collectionService.delete(context, collection);
        collection = collectionService.find(context, id);
        assertThat("testDelete 0", collection, nullValue());
    }

    /**
     * Test of delete method, of class Collection.
     */
    @Test(expected = AuthorizeException.class)
    public void testDeleteNoAuth() throws Exception {
        collectionService.delete(context, collection);
        fail("Exception expected");
    }

    /**
     * Test of getCommunities method, of class Collection.
     */
    @Test
    public void testGetCommunities() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = communityService.create(null, context);
        communityService.setMetadataSingleValue(context, community, MetadataSchemaEnum.DC.getName(),
                                                "title", null, Item.ANY, "community 3");
        this.collection.addCommunity(community);
        community = communityService.create(null, context);
        communityService.setMetadataSingleValue(context, community, MetadataSchemaEnum.DC.getName(),
                                                "title", null, Item.ANY, "community 1");
        this.collection.addCommunity(community);
        community = communityService.create(null, context);
        communityService.setMetadataSingleValue(context, community, MetadataSchemaEnum.DC.getName(),
                                                "title", null, Item.ANY, "community 2");
        this.collection.addCommunity(community);
        context.restoreAuthSystemState();
        assertTrue("testGetCommunities 0", collection.getCommunities().size() == 4);
        //Communities should be sorted by name
        assertTrue("testGetCommunities 1", collection.getCommunities().get(1).getName().equals("community 1"));
        assertTrue("testGetCommunities 1", collection.getCommunities().get(2).getName().equals("community 2"));
        assertTrue("testGetCommunities 1", collection.getCommunities().get(3).getName().equals("community 3"));
    }

    /**
     * Test of equals method, of class Collection.
     */
    @Test
    @SuppressWarnings("ObjectEqualsNull")
    public void testEquals() throws SQLException, AuthorizeException {
        // create a new collection for testing
        context.turnOffAuthorisationSystem();
        Collection newCollection = collectionService.create(context, owningCommunity);
        context.restoreAuthSystemState();

        assertFalse("testEquals 0", collection.equals(null));
        assertFalse("testEquals 1", collection.equals(newCollection));
        assertTrue("testEquals 2", collection.equals(collection));
    }

    /**
     * Test of getType method, of class Collection.
     */
    @Test
    @Override
    public void testGetType() {
        assertThat("testGetType 0", collection.getType(), equalTo(Constants.COLLECTION));
    }

    /**
     * Test of findAuthorized method, of class Collection.
     */
    @Test
    public void testFindAuthorized() throws Exception {
        context.turnOffAuthorisationSystem();
        Community com = communityService.create(null, context);
        context.restoreAuthSystemState();

        List<Collection> found = collectionService.findAuthorized(context, com, Constants.WRITE);
        assertThat("testFindAuthorized 0", found, notNullValue());
        assertTrue("testFindAuthorized 1", found.size() == 0);

        found = collectionService.findAuthorized(context, null, Constants.WRITE);
        assertThat("testFindAuthorized 2", found, notNullValue());
        assertTrue("testFindAuthorized 3", found.size() == 0);

        found = collectionService.findAuthorized(context, com, Constants.ADD);
        assertThat("testFindAuthorized 3", found, notNullValue());
        assertTrue("testFindAuthorized 4", found.size() == 0);

        found = collectionService.findAuthorized(context, null, Constants.ADD);
        assertThat("testFindAuthorized 5", found, notNullValue());
        assertTrue("testFindAuthorized 6", found.size() == 0);

        found = collectionService.findAuthorized(context, com, Constants.READ);
        assertThat("testFindAuthorized 7", found, notNullValue());
        assertTrue("testFindAuthorized 8", found.size() == 0);

        found = collectionService.findAuthorized(context, null, Constants.READ);
        assertThat("testFindAuthorized 9", found, notNullValue());
        assertTrue("testFindAuthorized 10", found.size() >= 1);
    }

    /**
     * Test of findAuthorizedOptimized method, of class Collection.
     * We create some collections and some users with varying auth, and ensure we can access them all properly.
     */
    @Test
    public void testFindAuthorizedOptimized() throws Exception {
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
    public void testCountItems() throws Exception {
        //0 by default
        assertTrue("testCountItems 0", itemService.countItems(context, collection) == 0);

        //NOTE: a more thorough test of item counting is in ITCommunityCollection integration test
    }

    /**
     * Test of getAdminObject method, of class Collection.
     */
    @Test
    @Override
    public void testGetAdminObject() throws SQLException {
        //default community has no admin object
        assertThat("testGetAdminObject 0",
                   (Collection) collectionService.getAdminObject(context, collection, Constants.REMOVE),
                   equalTo(collection));
        assertThat("testGetAdminObject 1",
                   (Collection) collectionService.getAdminObject(context, collection, Constants.ADD),
                   equalTo(collection));
        assertThat("testGetAdminObject 2", collectionService.getAdminObject(context, collection, Constants.DELETE),
                   instanceOf(Community.class));
        assertThat("testGetAdminObject 3", collectionService.getAdminObject(context, collection, Constants.ADMIN),
                   instanceOf(Collection.class));
    }

    /**
     * Test of getParentObject method, of class Collection.
     */
    @Test
    @Override
    public void testGetParentObject() throws SQLException {
        assertThat("testGetParentObject 1", collectionService.getParentObject(context, collection), notNullValue());
        assertThat("testGetParentObject 2", (Community) collectionService.getParentObject(context, collection),
                   equalTo(owningCommunity));
    }

}
