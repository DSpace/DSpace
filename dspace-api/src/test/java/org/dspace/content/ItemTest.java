/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.hamcrest.CoreMatchers.equalTo;
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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit Tests for class Item
 *
 * @author pvillega
 */
public class ItemTest extends AbstractDSpaceObjectTest {

    /**
     * log4j category
     */
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ItemTest.class);

    /**
     * Item instance for the tests
     */
    private Item it;

    private MetadataSchemaService metadataSchemaService = ContentServiceFactory.getInstance()
                                                                               .getMetadataSchemaService();
    private BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance()
                                                                                 .getBitstreamFormatService();
    private MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();

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
            WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, true);
            this.it = installItemService.installItem(context, workspaceItem);
            this.dspaceObject = it;
            context.restoreAuthSystemState();

            // Initialize our spy of the autowired (global) authorizeService bean.
            // This allows us to customize the bean's method return values in tests below
            authorizeServiceSpy = spy(authorizeService);
            // "Wire" our spy to be used by the current loaded object services
            // (To ensure these services use the spy instead of the real service)
            ReflectionTestUtils.setField(collectionService, "authorizeService", authorizeServiceSpy);
            ReflectionTestUtils.setField(itemService, "authorizeService", authorizeServiceSpy);
            ReflectionTestUtils.setField(workspaceItemService, "authorizeService", authorizeServiceSpy);
            ReflectionTestUtils.setField(bundleService, "authorizeService", authorizeServiceSpy);
            ReflectionTestUtils.setField(bitstreamService, "authorizeService", authorizeServiceSpy);
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
        try {
            itemService.delete(context, it);
        } catch (Exception e) {
            // ignore
        }

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
        it = null;
        collection = null;
        owningCommunity = null;
        try {
            super.destroy();
        } catch (Exception e) {
            // ignore
        }
    }


    /**
     * Test of find method, of class Item.
     */
    @Test
    public void testItemFind() throws Exception {
        // Get ID of item created in init()
        UUID id = it.getID();
        // Make sure we can find it via its ID
        Item found = itemService.find(context, id);
        assertThat("testItemFind 0", found, notNullValue());
        assertThat("testItemFind 1", found.getID(), equalTo(id));
        assertThat("testItemFind 2", found.getName(), nullValue());
    }

    /**
     * Test of create method, of class Item.
     */
    @Test
    public void testCreate() throws Exception {
        // Allow Collection WRITE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, collection, Constants.ADD);

        Item created = createItem();
        assertThat("testCreate 0", created, notNullValue());
        assertThat("testCreate 1", created.getName(), nullValue());
    }

    /**
     * Test of findAll method, of class Item.
     */
    @Test
    public void testFindAll() throws Exception {
        Iterator<Item> all = itemService.findAll(context);
        assertThat("testFindAll 0", all, notNullValue());

        boolean added = false;
        while (all.hasNext()) {
            Item tmp = all.next();
            if (tmp.equals(it)) {
                added = true;
            }
        }
        assertTrue("testFindAll 1", added);
    }

    /**
     * Test of findBySubmitter method, of class Item.
     */
    @Test
    public void testFindBySubmitter() throws Exception {
        Iterator<Item> all = itemService.findBySubmitter(context, context.getCurrentUser());
        assertThat("testFindBySubmitter 0", all, notNullValue());

        boolean added = false;
        while (all.hasNext()) {
            Item tmp = all.next();
            if (tmp.equals(it)) {
                added = true;
            }
        }
        assertTrue("testFindBySubmitter 1", added);

        context.turnOffAuthorisationSystem();
        all = itemService.findBySubmitter(context, ePersonService.create(context));
        context.restoreAuthSystemState();

        assertThat("testFindBySubmitter 2", all, notNullValue());
        assertFalse("testFindBySubmitter 3", all.hasNext());
    }

    /**
     * Test of findInArchiveOrWithdrawnDiscoverableModifiedSince method, of class Item.
     */
    @Test
    public void testFindInArchiveOrWithdrawnDiscoverableModifiedSince() throws Exception {
        // Init item to be both withdrawn and discoverable
        it.setWithdrawn(true);
        it.setArchived(false);
        it.setDiscoverable(true);
         // Test 0: Using a future 'modified since' date, we should get non-null list, with no items
        Iterator<Item> all = itemService.findInArchiveOrWithdrawnDiscoverableModifiedSince(context,
                DateUtils.addDays(it.getLastModified(),1));
        assertThat("Returned list should not be null", all, notNullValue());
        boolean added = false;
        while (all.hasNext()) {
            Item tmp = all.next();
            if (tmp.equals(it)) {
                added = true;
            }
        }
         // Test 1: we should NOT find our item in this list
        assertFalse("List should not contain item when passing a date newer than item last-modified date", added);
         // Test 2: Using a past 'modified since' date, we should get a non-null list containing our item
        all = itemService.findInArchiveOrWithdrawnDiscoverableModifiedSince(context,
                DateUtils.addDays(it.getLastModified(),-1));
        assertThat("Returned list should not be null", all, notNullValue());
        added = false;
        while (all.hasNext()) {
            Item tmp = all.next();
            if (tmp.equals(it)) {
                added = true;
            }
        }
        // Test 3: we should find our item in this list
        assertTrue("List should contain item when passing a date older than item last-modified date", added);
         // Repeat Tests 2, 3 with withdrawn = false and archived = true as this should result in same behaviour
        it.setWithdrawn(false);
        it.setArchived(true);
         // Test 4: Using a past 'modified since' date, we should get a non-null list containing our item
        all = itemService.findInArchiveOrWithdrawnDiscoverableModifiedSince(context,
                DateUtils.addDays(it.getLastModified(),-1));
        assertThat("Returned list should not be null", all, notNullValue());
        added = false;
        while (all.hasNext()) {
            Item tmp = all.next();
            if (tmp.equals(it)) {
                added = true;
            }
        }
        // Test 5: We should find our item in this list
        assertTrue("List should contain item when passing a date older than item last-modified date", added);
         // Test 6: Make sure non-discoverable items are not returned, regardless of archived/withdrawn state
        it.setDiscoverable(false);
        all = itemService.findInArchiveOrWithdrawnDiscoverableModifiedSince(context,
                DateUtils.addDays(it.getLastModified(),-1));
        assertThat("Returned list should not be null", all, notNullValue());
        added = false;
        while (all.hasNext()) {
            Item tmp = all.next();
            if (tmp.equals(it)) {
                added = true;
            }
        }
        // Test 7: We should not find our item in this list
        assertFalse("List should not contain non-discoverable items", added);
    }

     /**
     * Test of findInArchiveOrWithdrawnNonDiscoverableModifiedSince method, of class Item.
     */
    @Test
    public void testFindInArchiveOrWithdrawnNonDiscoverableModifiedSince() throws Exception {
        // Init item to be both withdrawn and discoverable
        it.setWithdrawn(true);
        it.setArchived(false);
        it.setDiscoverable(false);
         // Test 0: Using a future 'modified since' date, we should get non-null list, with no items
        Iterator<Item> all = itemService.findInArchiveOrWithdrawnNonDiscoverableModifiedSince(context,
                DateUtils.addDays(it.getLastModified(),1));
        assertThat("Returned list should not be null", all, notNullValue());
        boolean added = false;
        while (all.hasNext()) {
            Item tmp = all.next();
            if (tmp.equals(it)) {
                added = true;
            }
        }
         // Test 1: We should NOT find our item in this list
        assertFalse("List should not contain item when passing a date newer than item last-modified date", added);
         // Test 2: Using a past 'modified since' date, we should get a non-null list containing our item
        all = itemService.findInArchiveOrWithdrawnNonDiscoverableModifiedSince(context,
                DateUtils.addDays(it.getLastModified(),-1));
        assertThat("Returned list should not be null", all, notNullValue());
        added = false;
        while (all.hasNext()) {
            Item tmp = all.next();
            if (tmp.equals(it)) {
                added = true;
            }
        }
         // Test 3: We should find our item in this list
        assertTrue("List should contain item when passing a date older than item last-modified date", added);
         // Repeat Tests 2, 3 with discoverable = true
        it.setDiscoverable(true);
         // Test 4: Now we should still get a non-null list with NO items since item is discoverable
        all = itemService.findInArchiveOrWithdrawnNonDiscoverableModifiedSince(context,
                DateUtils.addDays(it.getLastModified(),-1));
        assertThat("Returned list should not be null", all, notNullValue());
        added = false;
        while (all.hasNext()) {
            Item tmp = all.next();
            if (tmp.equals(it)) {
                added = true;
            }
        }
         // Test 5: We should NOT find our item in this list
        assertFalse("List should not contain discoverable items", added);
    }

    /**
     * Test of getID method, of class Item.
     */
    @Override
    @Test
    public void testGetID() {
        assertTrue("testGetID 0", it.getID() != null);
    }

    /**
     * Test of getHandle method, of class Item.
     */
    @Override
    @Test
    public void testGetHandle() {
        //default instance has a random handle
        assertThat("testGetHandle 0", it.getHandle(), notNullValue());
    }

    /**
     * Test of isArchived method, of class Item.
     */
    @Test
    public void testIsArchived() throws SQLException, AuthorizeException, IOException, IllegalAccessException {
        //we are archiving items in the test by default so other tests run
        assertTrue("testIsArchived 0", it.isArchived());

        //false by default
        context.turnOffAuthorisationSystem();
        Item tmp = createItem();
        context.restoreAuthSystemState();
        assertTrue("testIsArchived 1", tmp.isArchived());
    }

    /**
     * Test of isWithdrawn method, of class Item.
     */
    @Test
    public void testIsWithdrawn() {
        assertFalse("testIsWithdrawn 0", it.isWithdrawn());
    }

    /**
     * Test of getLastModified method, of class Item.
     */
    @Test
    public void testGetLastModified() {
        assertThat("testGetLastModified 0", it.getLastModified(), notNullValue());
        assertTrue("testGetLastModified 1", DateUtils.isSameDay(it.getLastModified(), new Date()));
    }

    /**
     * Test of setArchived method, of class Item.
     */
    @Test
    public void testSetArchived() {
        it.setArchived(true);
        assertTrue("testSetArchived 0", it.isArchived());
    }

    /**
     * Test of setOwningCollection method, of class Item.
     */
    @Test
    public void testSetOwningCollection() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        Collection c = createCollection();
        context.restoreAuthSystemState();

        it.setOwningCollection(c);
        assertThat("testSetOwningCollection 0", it.getOwningCollection(), notNullValue());
        assertThat("testSetOwningCollection 1", it.getOwningCollection(), equalTo(c));
    }

    /**
     * Test of getOwningCollection method, of class Item.
     */
    @Test
    public void testGetOwningCollection() throws Exception {
        assertThat("testGetOwningCollection 0", it.getOwningCollection(), notNullValue());
        assertEquals("testGetOwningCollection 1", it.getOwningCollection(), collection);
    }

    /**
     * Test of getMetadata method, of class Item.
     */
    @Test
    public void testGetMetadata_4args() {
        String schema = "dc";
        String element = "contributor";
        String qualifier = "author";
        String lang = Item.ANY;
        List<MetadataValue> dc = itemService.getMetadata(it, schema, element, qualifier, lang);
        assertThat("testGetMetadata_4args 0", dc, notNullValue());
        assertTrue("testGetMetadata_4args 1", dc.size() == 0);
    }

    /**
     * Test of getMetadataByMetadataString method, of class Item.
     */
    @Test
    public void testGetMetadata_String() {
        String mdString = "dc.contributor.author";
        List<MetadataValue> dc = itemService.getMetadataByMetadataString(it, mdString);
        assertThat("testGetMetadata_String 0", dc, notNullValue());
        assertTrue("testGetMetadata_String 1", dc.size() == 0);

        mdString = "dc.contributor.*";
        dc = itemService.getMetadataByMetadataString(it, mdString);
        assertThat("testGetMetadata_String 2", dc, notNullValue());
        assertTrue("testGetMetadata_String 3", dc.size() == 0);

        mdString = "dc.contributor";
        dc = itemService.getMetadataByMetadataString(it, mdString);
        assertThat("testGetMetadata_String 4", dc, notNullValue());
        assertTrue("testGetMetadata_String 5", dc.size() == 0);
    }

    /**
     * A test for DS-806: Item.match() incorrect logic for schema testing
     */
    @Test
    public void testDS806() throws SQLException, AuthorizeException, NonUniqueMetadataException {
        //Create our "test" metadata field
        context.turnOffAuthorisationSystem();
        MetadataSchema metadataSchema = metadataSchemaService.create(context, "test", "test");
        MetadataField metadataField = metadataFieldService.create(context, metadataSchema, "type", null, null);
        context.restoreAuthSystemState();

        // Set the item to have two pieces of metadata for dc.type and dc2.type
        String dcType = "DC-TYPE";
        String testType = "TEST-TYPE";
        itemService.addMetadata(context, it, "dc", "type", null, null, dcType);
        itemService.addMetadata(context, it, "test", "type", null, null, testType);

        // Check that only one is returned when we ask for all dc.type values
        List<MetadataValue> values = itemService.getMetadata(it, "dc", "type", null, null);
        assertTrue("Return results", values.size() == 1);

        //Delete the field & schema
        context.turnOffAuthorisationSystem();
        itemService.clearMetadata(context, it, "test", "type", null, Item.ANY);
        metadataFieldService.delete(context, metadataField);
        metadataSchemaService.delete(context, metadataSchema);
        context.restoreAuthSystemState();
    }

    /**
     * Test of addMetadata method, of class Item.
     */
    @Test
    public void testAddMetadata_5args_1() throws SQLException {
        String schema = "dc";
        String element = "contributor";
        String qualifier = "author";
        String lang = Item.ANY;
        String[] values = {"value0", "value1"};
        itemService.addMetadata(context, it, schema, element, qualifier, lang, Arrays.asList(values));

        List<MetadataValue> dc = itemService.getMetadata(it, schema, element, qualifier, lang);
        assertThat("testAddMetadata_5args_1 0", dc, notNullValue());
        assertTrue("testAddMetadata_5args_1 1", dc.size() == 2);
        assertThat("testAddMetadata_5args_1 2", dc.get(0).getMetadataField().getMetadataSchema().getName(),
                   equalTo(schema));
        assertThat("testAddMetadata_5args_1 3", dc.get(0).getMetadataField().getElement(), equalTo(element));
        assertThat("testAddMetadata_5args_1 4", dc.get(0).getMetadataField().getQualifier(), equalTo(qualifier));
        assertThat("testAddMetadata_5args_1 5", dc.get(0).getLanguage(), equalTo(lang));
        assertThat("testAddMetadata_5args_1 6", dc.get(0).getValue(), equalTo(values[0]));
        assertThat("testAddMetadata_5args_1 7", dc.get(1).getMetadataField().getMetadataSchema().getName(),
                   equalTo(schema));
        assertThat("testAddMetadata_5args_1 8", dc.get(1).getMetadataField().getElement(), equalTo(element));
        assertThat("testAddMetadata_5args_1 9", dc.get(1).getMetadataField().getQualifier(), equalTo(qualifier));
        assertThat("testAddMetadata_5args_1 10", dc.get(1).getLanguage(), equalTo(lang));
        assertThat("testAddMetadata_5args_1 11", dc.get(1).getValue(), equalTo(values[1]));
    }

    /**
     * Test of addMetadata method, of class Item.
     */
    @Test
    public void testAddMetadata_7args_1_authority()
        throws InstantiationException, IllegalAccessException, NoSuchMethodException, SecurityException,
        IllegalArgumentException, InvocationTargetException, SQLException {
        //we have enabled an authority control in our test local.cfg to run this test
        //as MetadataAuthorityManager can't be mocked properly

        String schema = "dc";
        String element = "language";
        String qualifier = "iso";
        String lang = Item.ANY;
        List<String> values = Arrays.asList("en_US", "en");
        List<String> authorities = Arrays.asList("accepted", "uncertain");
        List<Integer> confidences = Arrays.asList(0, 0);
        itemService.addMetadata(context, it, schema, element, qualifier, lang, values, authorities, confidences);

        List<MetadataValue> dc = itemService.getMetadata(it, schema, element, qualifier, lang);
        assertThat("testAddMetadata_7args_1 0", dc, notNullValue());
        assertTrue("testAddMetadata_7args_1 1", dc.size() == 2);
        assertThat("testAddMetadata_7args_1 2", dc.get(0).getMetadataField().getMetadataSchema().getName(),
                   equalTo(schema));
        assertThat("testAddMetadata_7args_1 3", dc.get(0).getMetadataField().getElement(), equalTo(element));
        assertThat("testAddMetadata_7args_1 4", dc.get(0).getMetadataField().getQualifier(), equalTo(qualifier));
        assertThat("testAddMetadata_7args_1 5", dc.get(0).getLanguage(), equalTo(lang));
        assertThat("testAddMetadata_7args_1 6", dc.get(0).getValue(), equalTo(values.get(0)));
        assertThat("testAddMetadata_7args_1 7", dc.get(0).getAuthority(), equalTo(authorities.get(0)));
        assertThat("testAddMetadata_7args_1 8", dc.get(0).getConfidence(), equalTo(confidences.get(0)));
        assertThat("testAddMetadata_7args_1 9", dc.get(1).getMetadataField().getMetadataSchema().getName(),
                   equalTo(schema));
        assertThat("testAddMetadata_7args_1 10", dc.get(1).getMetadataField().getElement(), equalTo(element));
        assertThat("testAddMetadata_7args_1 11", dc.get(1).getMetadataField().getQualifier(), equalTo(qualifier));
        assertThat("testAddMetadata_7args_1 12", dc.get(1).getLanguage(), equalTo(lang));
        assertThat("testAddMetadata_7args_1 13", dc.get(1).getValue(), equalTo(values.get(1)));
        assertThat("testAddMetadata_7args_1 14", dc.get(1).getAuthority(), equalTo(authorities.get(1)));
        assertThat("testAddMetadata_7args_1 15", dc.get(1).getConfidence(), equalTo(confidences.get(1)));
    }

    /**
     * Test of addMetadata method, of class Item.
     */
    @Test
    public void testAddMetadata_7args_1_noauthority() throws SQLException {
        //by default has no authority

        String schema = "dc";
        String element = "contributor";
        String qualifier = "author";
        String lang = Item.ANY;
        List<String> values = Arrays.asList("value0", "value1");
        List<String> authorities = Arrays.asList("auth0", "auth2");
        List<Integer> confidences = Arrays.asList(0, 0);
        itemService.addMetadata(context, it, schema, element, qualifier, lang, values, authorities, confidences);

        List<MetadataValue> dc = itemService.getMetadata(it, schema, element, qualifier, lang);
        assertThat("testAddMetadata_7args_1 0", dc, notNullValue());
        assertTrue("testAddMetadata_7args_1 1", dc.size() == 2);
        assertThat("testAddMetadata_7args_1 2", dc.get(0).getMetadataField().getMetadataSchema().getName(),
                   equalTo(schema));
        assertThat("testAddMetadata_7args_1 3", dc.get(0).getMetadataField().getElement(), equalTo(element));
        assertThat("testAddMetadata_7args_1 4", dc.get(0).getMetadataField().getQualifier(), equalTo(qualifier));
        assertThat("testAddMetadata_7args_1 5", dc.get(0).getLanguage(), equalTo(lang));
        assertThat("testAddMetadata_7args_1 6", dc.get(0).getValue(), equalTo(values.get(0)));
        assertThat("testAddMetadata_7args_1 7", dc.get(0).getAuthority(), nullValue());
        assertThat("testAddMetadata_7args_1 8", dc.get(0).getConfidence(), equalTo(-1));
        assertThat("testAddMetadata_7args_1 9", dc.get(1).getMetadataField().getMetadataSchema().getName(),
                   equalTo(schema));
        assertThat("testAddMetadata_7args_1 10", dc.get(1).getMetadataField().getElement(), equalTo(element));
        assertThat("testAddMetadata_7args_1 11", dc.get(1).getMetadataField().getQualifier(), equalTo(qualifier));
        assertThat("testAddMetadata_7args_1 12", dc.get(1).getLanguage(), equalTo(lang));
        assertThat("testAddMetadata_7args_1 13", dc.get(1).getValue(), equalTo(values.get(1)));
        assertThat("testAddMetadata_7args_1 14", dc.get(1).getAuthority(), nullValue());
        assertThat("testAddMetadata_7args_1 15", dc.get(1).getConfidence(), equalTo(-1));
    }

    /**
     * Test of addMetadata method, of class Item.
     */
    @Test
    public void testAddMetadata_5args_2() throws SQLException {
        String schema = "dc";
        String element = "contributor";
        String qualifier = "author";
        String lang = Item.ANY;
        List<String> values = Arrays.asList("value0", "value1");
        itemService.addMetadata(context, it, schema, element, qualifier, lang, values);

        List<MetadataValue> dc = itemService.getMetadata(it, schema, element, qualifier, lang);
        assertThat("testAddMetadata_5args_2 0", dc, notNullValue());
        assertTrue("testAddMetadata_5args_2 1", dc.size() == 2);
        assertThat("testAddMetadata_5args_2 2", dc.get(0).getMetadataField().getMetadataSchema().getName(),
                   equalTo(schema));
        assertThat("testAddMetadata_5args_2 3", dc.get(0).getMetadataField().getElement(), equalTo(element));
        assertThat("testAddMetadata_5args_2 4", dc.get(0).getMetadataField().getQualifier(), equalTo(qualifier));
        assertThat("testAddMetadata_5args_2 5", dc.get(0).getLanguage(), equalTo(lang));
        assertThat("testAddMetadata_5args_2 6", dc.get(0).getValue(), equalTo(values.get(0)));
        assertThat("testAddMetadata_5args_2 7", dc.get(1).getMetadataField().getMetadataSchema().getName(),
                   equalTo(schema));
        assertThat("testAddMetadata_5args_2 8", dc.get(1).getMetadataField().getElement(), equalTo(element));
        assertThat("testAddMetadata_5args_2 9", dc.get(1).getMetadataField().getQualifier(), equalTo(qualifier));
        assertThat("testAddMetadata_5args_2 10", dc.get(1).getLanguage(), equalTo(lang));
        assertThat("testAddMetadata_5args_2 11", dc.get(1).getValue(), equalTo(values.get(1)));
    }

    /**
     * Test of addMetadata method, of class Item.
     */
    @Test
    public void testAddMetadata_7args_2_authority() throws SQLException {
        //we have enabled an authority control in our test local.cfg to run this test
        //as MetadataAuthorityManager can't be mocked properly

        String schema = "dc";
        String element = "language";
        String qualifier = "iso";
        String lang = Item.ANY;
        String values = "en";
        String authorities = "accepted";
        int confidences = 0;
        itemService.addMetadata(context, it, schema, element, qualifier, lang, values, authorities, confidences);

        List<MetadataValue> dc = itemService.getMetadata(it, schema, element, qualifier, lang);
        assertThat("testAddMetadata_7args_2 0", dc, notNullValue());
        assertTrue("testAddMetadata_7args_2 1", dc.size() == 1);
        assertThat("testAddMetadata_7args_2 2", dc.get(0).getMetadataField().getMetadataSchema().getName(),
                   equalTo(schema));
        assertThat("testAddMetadata_7args_2 3", dc.get(0).getMetadataField().getElement(), equalTo(element));
        assertThat("testAddMetadata_7args_2 4", dc.get(0).getMetadataField().getQualifier(), equalTo(qualifier));
        assertThat("testAddMetadata_7args_2 5", dc.get(0).getLanguage(), equalTo(lang));
        assertThat("testAddMetadata_7args_2 6", dc.get(0).getValue(), equalTo(values));
        assertThat("testAddMetadata_7args_2 7", dc.get(0).getAuthority(), equalTo(authorities));
        assertThat("testAddMetadata_7args_2 8", dc.get(0).getConfidence(), equalTo(confidences));
    }

    /**
     * Test of addMetadata method, of class Item.
     */
    @Test
    public void testAddMetadata_7args_2_noauthority() throws SQLException {
        //by default has no authority

        String schema = "dc";
        String element = "contributor";
        String qualifier = "editor";
        String lang = Item.ANY;
        String values = "value0";
        String authorities = "auth0";
        int confidences = 0;
        itemService.addMetadata(context, it, schema, element, qualifier, lang, values, authorities, confidences);

        List<MetadataValue> dc = itemService.getMetadata(it, schema, element, qualifier, lang);
        assertThat("testAddMetadata_7args_2 0", dc, notNullValue());
        assertTrue("testAddMetadata_7args_2 1", dc.size() == 1);
        assertThat("testAddMetadata_7args_2 2", dc.get(0).getMetadataField().getMetadataSchema().getName(),
                   equalTo(schema));
        assertThat("testAddMetadata_7args_2 3", dc.get(0).getMetadataField().getElement(), equalTo(element));
        assertThat("testAddMetadata_7args_2 4", dc.get(0).getMetadataField().getQualifier(), equalTo(qualifier));
        assertThat("testAddMetadata_7args_2 5", dc.get(0).getLanguage(), equalTo(lang));
        assertThat("testAddMetadata_7args_2 6", dc.get(0).getValue(), equalTo(values));
        assertThat("testAddMetadata_7args_2 7", dc.get(0).getAuthority(), nullValue());
        assertThat("testAddMetadata_7args_2 8", dc.get(0).getConfidence(), equalTo(-1));
    }

    /**
     * Test of clearMetadata method, of class Item.
     */
    @Test
    public void testClearMetadata() throws SQLException {
        String schema = "dc";
        String element = "contributor";
        String qualifier = "author";
        String lang = Item.ANY;
        String values = "value0";
        itemService.addMetadata(context, it, schema, element, qualifier, lang, values);

        itemService.clearMetadata(context, it, schema, element, qualifier, lang);

        List<MetadataValue> dc = itemService.getMetadata(it, schema, element, qualifier, lang);
        assertThat("testClearMetadata 0", dc, notNullValue());
        assertTrue("testClearMetadata 1", dc.size() == 0);
    }

    /**
     * Test of getSubmitter method, of class Item.
     */
    @Test
    public void testGetSubmitter() throws Exception {
        assertThat("testGetSubmitter 0", it.getSubmitter(), notNullValue());

        //null by default
        context.turnOffAuthorisationSystem();
        Item tmp = createItem();
        context.restoreAuthSystemState();
        assertEquals("testGetSubmitter 1", tmp.getSubmitter(), context.getCurrentUser());
    }

    /**
     * Test of setSubmitter method, of class Item.
     */
    @Test
    public void testSetSubmitter() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        EPerson sub = ePersonService.create(context);
        context.restoreAuthSystemState();

        it.setSubmitter(sub);

        assertThat("testSetSubmitter 0", it.getSubmitter(), notNullValue());
        assertThat("testSetSubmitter 1", it.getSubmitter().getID(), equalTo(sub.getID()));
    }

    /**
     * Test of getCollections method, of class Item.
     */
    @Test
    public void testGetCollections() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection collection = collectionService.create(context, owningCommunity);
        collectionService.setMetadataSingleValue(context, collection, MetadataSchemaEnum.DC.getName(),
                                                 "title", null, Item.ANY, "collection B");
        it.addCollection(collection);
        collection = collectionService.create(context, owningCommunity);
        collectionService.setMetadataSingleValue(context, collection, MetadataSchemaEnum.DC.getName(),
                                                 "title", null, Item.ANY, "collection A");
        it.addCollection(collection);
        context.restoreAuthSystemState();
        assertThat("testGetCollections 0", it.getCollections(), notNullValue());
        assertTrue("testGetCollections 1", it.getCollections().size() == 3);
        assertTrue("testGetCollections 2", it.getCollections().get(1).getName().equals("collection A"));
        assertTrue("testGetCollections 3", it.getCollections().get(2).getName().equals("collection B"));
    }

    /**
     * Test of getCommunities method, of class Item.
     */
    @Test
    public void testGetCommunities() throws Exception {
        assertThat("testGetCommunities 0", itemService.getCommunities(context, it), notNullValue());
        assertTrue("testGetCommunities 1", itemService.getCommunities(context, it).size() == 1);
    }

    /**
     * Test of getBundles method, of class Item.
     */
    @Test
    public void testGetBundles_0args() throws Exception {
        assertThat("testGetBundles_0args 0", it.getBundles(), notNullValue());
        assertTrue("testGetBundles_0args 1", it.getBundles().size() == 0);
    }

    /**
     * Test of getBundles method, of class Item.
     */
    @Test
    public void testGetBundles_String() throws Exception {
        String name = "name";
        assertThat("testGetBundles_String 0", itemService.getBundles(it, name), notNullValue());
        assertTrue("testGetBundles_String 1", itemService.getBundles(it, name).size() == 0);
    }

    /**
     * Test of createBundle method, of class Item.
     */
    @Test
    public void testCreateBundleAuth() throws Exception {
        // Allow Item ADD perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, it, Constants.ADD);

        String name = "bundle";
        Bundle created = bundleService.create(context, it, name);
        assertThat("testCreateBundleAuth 0", created, notNullValue());
        assertThat("testCreateBundleAuth 1", created.getName(), equalTo(name));
        assertThat("testCreateBundleAuth 2", itemService.getBundles(it, name), notNullValue());
        assertTrue("testCreateBundleAuth 3", itemService.getBundles(it, name).size() == 1);
    }

    /**
     * Test of createBundle method, of class Item.
     */
    @Test(expected = SQLException.class)
    public void testCreateBundleNoName() throws Exception {
        bundleService.create(context, it, "");
        fail("Exception expected");
    }

    /**
     * Test of createBundle method, of class Item.
     */
    @Test(expected = SQLException.class)
    public void testCreateBundleNullName() throws Exception {
        bundleService.create(context, it, null);
        fail("Exception expected");
    }

    /**
     * Test of createBundle method, of class Item.
     */
    @Test(expected = AuthorizeException.class)
    public void testCreateBundleNoAuth() throws Exception {
        String name = "bundle";
        bundleService.create(context, it, name);
        fail("Exception expected");
    }

    /**
     * Test of addBundle method, of class Item.
     */
    @Test
    public void testAddBundleAuth() throws Exception {
        // Allow Item ADD perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, it, Constants.ADD);

        String name = "bundle";
        Bundle created = bundleService.create(context, it, name);
        created.setName(context, name);
        itemService.addBundle(context, it, created);

        assertThat("testAddBundleAuth 0", itemService.getBundles(it, name), notNullValue());
        assertTrue("testAddBundleAuth 1", itemService.getBundles(it, name).size() == 1);
        assertThat("testAddBundleAuth 2", itemService.getBundles(it, name).get(0), equalTo(created));
    }

    /**
     * Test of addBundle method, of class Item.
     */
    @Test(expected = AuthorizeException.class)
    public void testAddBundleNoAuth() throws Exception {
        String name = "bundle";
        Bundle created = bundleService.create(context, it, name);
        created.setName(context, name);
        itemService.addBundle(context, it, created);
        fail("Exception expected");
    }

    /**
     * Test of removeBundle method, of class Item.
     */
    @Test
    public void testRemoveBundleAuth() throws Exception {
        // First create a bundle for test
        context.turnOffAuthorisationSystem();
        String name = "bundle";
        Bundle created = bundleService.create(context, it, name);
        created.setName(context, name);
        itemService.addBundle(context, it, created);
        context.restoreAuthSystemState();

        // Allow Item REMOVE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, it, Constants.REMOVE);
        // Allow Bundle DELETE
        doNothing().when(authorizeServiceSpy).authorizeAction(context, created, Constants.DELETE);

        itemService.removeBundle(context, it, created);
        assertThat("testRemoveBundleAuth 0", itemService.getBundles(it, name), notNullValue());
        assertTrue("testRemoveBundleAuth 1", itemService.getBundles(it, name).size() == 0);
    }

    /**
     * Test of removeBundle method, of class Item.
     */
    @Test(expected = AuthorizeException.class)
    public void testRemoveBundleNoAuth() throws Exception {
        // First create a bundle for test
        context.turnOffAuthorisationSystem();
        String name = "bundle";
        Bundle created = bundleService.create(context, it, name);
        created.setName(context, name);
        itemService.addBundle(context, it, created);
        context.restoreAuthSystemState();

        itemService.removeBundle(context, it, created);
        fail("Exception expected");
    }

    /**
     * Test of createSingleBitstream method, of class Item.
     */
    @Test
    public void testCreateSingleBitstream_InputStream_StringAuth() throws Exception {
        // Allow Item ADD perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, it, Constants.ADD);
        // Allow Item WRITE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, it, Constants.WRITE, true);
        // Allow Bundle ADD perms
        doNothing().when(authorizeServiceSpy).authorizeAction(any(Context.class), any(Bundle.class), eq(Constants.ADD));
        // Allow Bitstream WRITE perms
        doNothing().when(authorizeServiceSpy)
                   .authorizeAction(any(Context.class), any(Bitstream.class), eq(Constants.WRITE));

        String name = "new bundle";
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream result = itemService.createSingleBitstream(context, new FileInputStream(f), it, name);
        assertThat("testCreateSingleBitstream_InputStream_StringAuth 0", result, notNullValue());
    }

    /**
     * Test of createSingleBitstream method, of class Item.
     */
    @Test(expected = AuthorizeException.class)
    public void testCreateSingleBitstream_InputStream_StringNoAuth() throws Exception {
        String name = "new bundle";
        File f = new File(testProps.get("test.bitstream").toString());
        itemService.createSingleBitstream(context, new FileInputStream(f), it, name);
        fail("Exception expected");
    }

    /**
     * Test of createSingleBitstream method, of class Item.
     */
    @Test
    public void testCreateSingleBitstream_InputStreamAuth() throws Exception {
        // Allow Item ADD perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, it, Constants.ADD);
        // Allow Item WRITE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, it, Constants.WRITE, true);
        // Allow Bundle ADD perms
        doNothing().when(authorizeServiceSpy).authorizeAction(any(Context.class), any(Bundle.class), eq(Constants.ADD));
        // Allow Bitstream WRITE perms
        doNothing().when(authorizeServiceSpy)
                   .authorizeAction(any(Context.class), any(Bitstream.class), eq(Constants.WRITE));


        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream result = itemService.createSingleBitstream(context, new FileInputStream(f), it);
        assertThat("testCreateSingleBitstream_InputStreamAuth 0", result, notNullValue());
    }

    /**
     * Test of createSingleBitstream method, of class Item.
     */
    @Test(expected = AuthorizeException.class)
    public void testCreateSingleBitstream_InputStreamNoAuth() throws Exception {
        File f = new File(testProps.get("test.bitstream").toString());
        itemService.createSingleBitstream(context, new FileInputStream(f), it);
        fail("Expected exception");
    }

    /**
     * Test of getNonInternalBitstreams method, of class Item.
     */
    @Test
    public void testGetNonInternalBitstreams() throws Exception {
        assertThat("testGetNonInternalBitstreams 0", itemService.getNonInternalBitstreams(context, it), notNullValue());
        assertTrue("testGetNonInternalBitstreams 1", itemService.getNonInternalBitstreams(context, it).size() == 0);
    }

    /**
     * Test of removeDSpaceLicense method, of class Item.
     */
    @Test
    public void testRemoveDSpaceLicenseAuth() throws Exception {
        // First create a bundle for test
        context.turnOffAuthorisationSystem();
        String name = "LICENSE";
        Bundle created = bundleService.create(context, it, name);
        created.setName(context, name);
        context.restoreAuthSystemState();

        // Allow Item REMOVE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, it, Constants.REMOVE);
        // Allow Bundle DELETE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, created, Constants.DELETE);

        itemService.removeDSpaceLicense(context, it);
        assertThat("testRemoveDSpaceLicenseAuth 0", itemService.getBundles(it, name), notNullValue());
        assertTrue("testRemoveDSpaceLicenseAuth 1", itemService.getBundles(it, name).size() == 0);
    }

    /**
     * Test of removeDSpaceLicense method, of class Item.
     */
    @Test(expected = AuthorizeException.class)
    public void testRemoveDSpaceLicenseNoAuth() throws Exception {
        // First create a bundle for test
        context.turnOffAuthorisationSystem();
        String name = "LICENSE";
        Bundle created = bundleService.create(context, it, name);
        created.setName(context, name);
        context.restoreAuthSystemState();

        itemService.removeDSpaceLicense(context, it);
        fail("Exception expected");
    }

    /**
     * Test of removeLicenses method, of class Item.
     */
    @Test
    public void testRemoveLicensesAuth() throws Exception {
        // First create test content
        context.turnOffAuthorisationSystem();
        String name = "LICENSE";
        Bundle created = bundleService.create(context, it, name);
        created.setName(context, name);

        String bsname = "License";
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream result = itemService.createSingleBitstream(context, new FileInputStream(f), it, bsname);
        bitstreamService.setFormat(context, result, bitstreamFormatService.findByShortDescription(context, bsname));
        bundleService.addBitstream(context, created, result);
        context.restoreAuthSystemState();

        // Allow Item REMOVE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, it, Constants.REMOVE);
        // Allow Item WRITE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, it, Constants.WRITE);
        // Allow Bundle REMOVE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, created, Constants.REMOVE);
        // Allow Bundle DELETE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, created, Constants.DELETE);
        // Allow Bitstream DELETE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, result, Constants.DELETE);

        itemService.removeLicenses(context, it);
        assertThat("testRemoveLicensesAuth 0", itemService.getBundles(it, name), notNullValue());
        assertTrue("testRemoveLicensesAuth 1", itemService.getBundles(it, name).size() == 0);
    }

    /**
     * Test of removeLicenses method, of class Item.
     */
    @Test(expected = AuthorizeException.class)
    public void testRemoveLicensesNoAuth() throws Exception {
        // First create test content
        context.turnOffAuthorisationSystem();
        String name = "LICENSE";
        Bundle created = bundleService.create(context, it, name);
        created.setName(context, name);

        String bsname = "License";
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream result = itemService.createSingleBitstream(context, new FileInputStream(f), it, bsname);
        bitstreamService.setFormat(context, result, bitstreamFormatService.findByShortDescription(context, bsname));
        bundleService.addBitstream(context, created, result);
        context.restoreAuthSystemState();

        itemService.removeLicenses(context, it);
        fail("Exception expected");
    }

    /**
     * Test of update method, of class Item.
     */
    @Test
    public void testUpdateAuth() throws Exception {
        // Allow Item WRITE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, it, Constants.WRITE);

        itemService.update(context, it);
    }

    /**
     * Test of update method, of class Item.
     */
    @Test
    public void testUpdateAuth2() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection c = createCollection();
        it.setOwningCollection(c);
        context.restoreAuthSystemState();

        // Allow parent Collection WRITE perms (to test inheritance)
        doNothing().when(authorizeServiceSpy).authorizeAction(context, c, Constants.WRITE, false);

        itemService.update(context, it);
    }

    /**
     * Test of update method, of class Item.
     */
    @Test(expected = AuthorizeException.class)
    public void testUpdateNoAuth() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection c = createCollection();
        it.setOwningCollection(c);
        context.restoreAuthSystemState();

        itemService.update(context, it);
    }

    /**
     * Test of withdraw method, of class Item.
     */
    @Test
    public void testWithdrawAuth() throws Exception {
        // Allow Item WRITE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, it, Constants.WRITE);
        // Allow Collection ADMIN perms
        when(authorizeServiceSpy.authorizeActionBoolean(context, collection, Constants.ADMIN)).thenReturn(true);

        itemService.withdraw(context, it);
        assertTrue("testWithdrawAuth 0", it.isWithdrawn());
    }

    /**
     * Test of withdraw method, of class Item.
     */
    @Test(expected = AuthorizeException.class)
    public void testWithdrawNoAuth() throws Exception {
        itemService.withdraw(context, it);
        fail("Exception expected");
    }

    /**
     * Test of reinstate method, of class Item.
     */
    @Test
    public void testReinstateAuth() throws Exception {
        // Allow Item WRITE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, it, Constants.WRITE);
        // Allow Collection ADD perms (needed to reinstate)
        doNothing().when(authorizeServiceSpy).authorizeAction(context, collection, Constants.ADD);

        // initialize item as withdrawn
        context.turnOffAuthorisationSystem();
        itemService.withdraw(context, it);
        context.restoreAuthSystemState();

        itemService.reinstate(context, it);
        assertFalse("testReinstate 0", it.isWithdrawn());
    }

    /**
     * Test of reinstate method, of class Item.
     */
    @Test(expected = AuthorizeException.class)
    public void testReinstateNoAuth() throws Exception {
        // initialize item as withdrawn
        context.turnOffAuthorisationSystem();
        itemService.withdraw(context, it);
        context.restoreAuthSystemState();

        itemService.reinstate(context, it);
        fail("Exception expected");
    }

    /**
     * Test of delete method, of class Item.
     */
    @Test
    public void testDeleteAuth() throws Exception {
        // create a new item to delete
        context.turnOffAuthorisationSystem();
        Item item = createItem();
        context.restoreAuthSystemState();

        // Allow Item REMOVE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, item, Constants.REMOVE, true);
        // Allow Item DELETE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, item, Constants.DELETE);
        // Allow Item WRITE perms (required to first delete identifiers)
        doNothing().when(authorizeServiceSpy).authorizeAction(context, item, Constants.WRITE);

        UUID id = item.getID();
        itemService.delete(context, item);
        Item found = itemService.find(context, id);
        assertThat("testDeleteAuth 0", found, nullValue());
    }

    /**
     * Test of delete method, of class Item.
     */
    @Test(expected = AuthorizeException.class)
    public void testDeleteNoAuth() throws Exception {
        itemService.delete(context, it);
        fail("Exception expected");
    }

    /**
     * Test of equals method, of class Item.
     */
    @Test
    @SuppressWarnings("ObjectEqualsNull")
    public void testEquals() throws SQLException, AuthorizeException, IOException, IllegalAccessException {
        assertFalse("testEquals 0", it.equals(null));

        // create a new item to test against
        context.turnOffAuthorisationSystem();
        Item item = createItem();
        context.restoreAuthSystemState();

        try {
            assertFalse("testEquals 1", it.equals(item));
            assertTrue("testEquals 2", it.equals(it));
        } finally {
            //delete item we created
            context.turnOffAuthorisationSystem();
            itemService.delete(context, item);
            context.restoreAuthSystemState();
        }
    }

    /**
     * Test of isOwningCollection method, of class Item.
     */
    @Test
    public void testIsOwningCollection() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        Collection c = createCollection();
        context.restoreAuthSystemState();

        boolean result = itemService.isOwningCollection(it, c);
        assertFalse("testIsOwningCollection 0", result);
    }

    /**
     * Test of getType method, of class Item.
     */
    @Override
    @Test
    public void testGetType() {
        assertThat("testGetType 0", it.getType(), equalTo(Constants.ITEM));
    }

    /**
     * Test of replaceAllItemPolicies method, of class Item.
     */
    @Test
    public void testReplaceAllItemPolicies() throws Exception {
        List<ResourcePolicy> newpolicies = new ArrayList<ResourcePolicy>();
        ResourcePolicy pol1 = resourcePolicyService.create(context);
        newpolicies.add(pol1);
        itemService.replaceAllItemPolicies(context, it, newpolicies);

        List<ResourcePolicy> retrieved = authorizeService.getPolicies(context, it);
        assertThat("testReplaceAllItemPolicies 0", retrieved, notNullValue());
        assertThat("testReplaceAllItemPolicies 1", retrieved.size(), equalTo(newpolicies.size()));
    }

    /**
     * Test of replaceAllBitstreamPolicies method, of class Item.
     */
    @Test
    public void testReplaceAllBitstreamPolicies() throws Exception {
        context.turnOffAuthorisationSystem();
        //we add some bundles for the test
        String name = "LICENSE";
        Bundle created = bundleService.create(context, it, name);
        created.setName(context, name);

        String bsname = "License";
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream result = itemService.createSingleBitstream(context, new FileInputStream(f), it, bsname);
        bitstreamService.setFormat(context, result, bitstreamFormatService.findByShortDescription(context, bsname));
        bundleService.addBitstream(context, created, result);

        List<ResourcePolicy> newpolicies = new ArrayList<ResourcePolicy>();
        newpolicies.add(resourcePolicyService.create(context));
        newpolicies.add(resourcePolicyService.create(context));
        newpolicies.add(resourcePolicyService.create(context));
        context.restoreAuthSystemState();

        itemService.replaceAllBitstreamPolicies(context, it, newpolicies);

        List<ResourcePolicy> retrieved = new ArrayList<ResourcePolicy>();
        List<Bundle> bundles = it.getBundles();
        for (Bundle b : bundles) {
            retrieved.addAll(authorizeService.getPolicies(context, b));
            retrieved.addAll(bundleService.getBitstreamPolicies(context, b));
        }
        assertFalse("testReplaceAllBitstreamPolicies 0", retrieved.isEmpty());

        boolean equals = true;
        for (int i = 0; i < newpolicies.size() && equals; i++) {
            if (!newpolicies.contains(retrieved.get(i))) {
                equals = false;
            }
        }
        assertTrue("testReplaceAllBitstreamPolicies 1", equals);
    }

    /**
     * Test of removeGroupPolicies method, of class Item.
     */
    @Test
    public void testRemoveGroupPolicies() throws Exception {
        context.turnOffAuthorisationSystem();
        List<ResourcePolicy> newpolicies = new ArrayList<ResourcePolicy>();
        Group g = groupService.create(context);
        ResourcePolicy pol1 = resourcePolicyService.create(context);
        newpolicies.add(pol1);
        pol1.setGroup(g);
        itemService.replaceAllItemPolicies(context, it, newpolicies);

        itemService.removeGroupPolicies(context, it, g);
        context.restoreAuthSystemState();

        List<ResourcePolicy> retrieved = authorizeService.getPolicies(context, it);
        assertThat("testRemoveGroupPolicies 0", retrieved, notNullValue());
        assertTrue("testRemoveGroupPolicies 1", retrieved.isEmpty());
    }

    /**
     * Test of inheritCollectionDefaultPolicies method, of class Item.
     */
    @Test
    public void testInheritCollectionDefaultPolicies() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection c = createCollection();

        List<ResourcePolicy> defaultCollectionPolicies =
            authorizeService.getPoliciesActionFilter(context, c, Constants.DEFAULT_BITSTREAM_READ);
        List<ResourcePolicy> newPolicies = new ArrayList<ResourcePolicy>();
        for (ResourcePolicy collRp : defaultCollectionPolicies) {
            ResourcePolicy rp = resourcePolicyService.clone(context, collRp);
            rp.setAction(Constants.READ);
            rp.setRpType(ResourcePolicy.TYPE_INHERITED);
            newPolicies.add(rp);
        }

        //we add some bundles for the test
        String name = "LICENSE";
        Bundle created = bundleService.create(context, it, name);
        created.setName(context, name);

        String bsname = "License";
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream result = itemService.createSingleBitstream(context, new FileInputStream(f), it, bsname);
        bitstreamService.setFormat(context, result, bitstreamFormatService.findByShortDescription(context, bsname));
        bundleService.addBitstream(context, created, result);

        context.restoreAuthSystemState();

        // Allow Item WRITE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, it, Constants.WRITE, true);

        itemService.inheritCollectionDefaultPolicies(context, it, c);

        //test item policies
        List<ResourcePolicy> retrieved = authorizeService.getPolicies(context, it);
        boolean equals = true;
        for (int i = 0; i < retrieved.size() && equals; i++) {
            if (!newPolicies.contains(retrieved.get(i))) {
                equals = false;
            }
        }
        assertTrue("testInheritCollectionDefaultPolicies 0", equals);

        retrieved = new ArrayList<ResourcePolicy>();
        List<Bundle> bundles = it.getBundles();
        for (Bundle b : bundles) {
            retrieved.addAll(authorizeService.getPolicies(context, b));
            retrieved.addAll(bundleService.getBitstreamPolicies(context, b));
        }
        assertFalse("testInheritCollectionDefaultPolicies 1", retrieved.isEmpty());

        equals = true;
        for (int i = 0; i < newPolicies.size() && equals; i++) {
            if (!newPolicies.contains(retrieved.get(i))) {
                equals = false;
            }
        }
        assertTrue("testInheritCollectionDefaultPolicies 2", equals);
    }

    /**
     * Test of move method, of class Item.
     */
    @Test
    public void testMove() throws Exception {
        //we disable the permission testing as it's shared with other methods where it's already tested (can edit)
        context.turnOffAuthorisationSystem();
        Collection from = createCollection();
        Collection to = createCollection();
        it.addCollection(from);
        it.setOwningCollection(from);

        itemService.move(context, it, from, to);
        context.restoreAuthSystemState();
        assertThat("testMove 0", it.getOwningCollection(), notNullValue());
        assertThat("testMove 1", it.getOwningCollection(), equalTo(to));
    }

    /**
     * Test of move method, of class Item, where both Collections are the same.
     */
    @Test
    public void testMoveSameCollection() throws Exception {
        context.turnOffAuthorisationSystem();
        while (it.getCollections().size() > 1) {
            it.removeCollection(it.getCollections().get(0));
        }

        Collection collection = it.getCollections().get(0);
        it.setOwningCollection(collection);
        ItemService itemServiceSpy = spy(itemService);

        itemService.move(context, it, collection, collection);
        context.restoreAuthSystemState();
        assertThat("testMoveSameCollection 0", it.getOwningCollection(), notNullValue());
        assertThat("testMoveSameCollection 1", it.getOwningCollection(), equalTo(collection));
        verify(itemServiceSpy, times(0)).delete(context, it);
    }

    /**
     * Test of hasUploadedFiles method, of class Item.
     */
    @Test
    public void testHasUploadedFiles() throws Exception {
        assertFalse("testHasUploadedFiles 0", itemService.hasUploadedFiles(it));
    }

    /**
     * Test of getCollectionsNotLinked method, of class Item.
     */
    @Test
    public void testGetCollectionsNotLinked() throws Exception {
        List<Collection> result = itemService.getCollectionsNotLinked(context, it);
        boolean isin = false;
        for (Collection c : result) {
            Iterator<Item> iit = itemService.findByCollection(context, c);
            while (iit.hasNext()) {
                if (iit.next().getID().equals(it.getID())) {
                    isin = true;
                }
            }
        }
        assertFalse("testGetCollectionsNotLinked 0", isin);
    }

    /**
     * Test of canEdit method, of class Item.
     */
    @Test
    public void testCanEditBooleanAuth() throws Exception {
        // Allow Item WRITE perms
        when(authorizeServiceSpy.authorizeActionBoolean(context, it, Constants.WRITE)).thenReturn(true);

        assertTrue("testCanEditBooleanAuth 0", itemService.canEdit(context, it));
    }

    /**
     * Test of canEdit method, of class Item.
     */
    @Test
    public void testCanEditBooleanAuth2() throws Exception {
        // Allow parent Community WRITE perms (test inheritance from community)
        when(authorizeServiceSpy.authorizeActionBoolean(context, owningCommunity, Constants.WRITE, false))
            .thenReturn(true);

        assertTrue("testCanEditBooleanAuth2 0", itemService.canEdit(context, it));
    }

    /**
     * Test of canEdit method, of class Item.
     */
    @Test
    public void testCanEditBooleanAuth3() throws Exception {
        // Create a new Collection and assign it as the owner
        context.turnOffAuthorisationSystem();
        Collection c = createCollection();
        it.setOwningCollection(c);
        context.restoreAuthSystemState();

        // Allow parent Collection WRITE perms (test inheritance from new collection)
        doNothing().when(authorizeServiceSpy).authorizeAction(context, c, Constants.WRITE, false);

        // Ensure person with WRITE perms on the Collection can edit item
        assertTrue("testCanEditBooleanAuth3 0", itemService.canEdit(context, it));
    }

    /**
     * Test of canEdit method, of class Item.
     */
    @Test
    public void testCanEditBooleanNoAuth() throws Exception {
        assertFalse("testCanEditBooleanNoAuth 0", itemService.canEdit(context, it));
    }

    /**
     * Test of canEdit method, of class Item.
     */
    @Test
    public void testCanEditBooleanNoAuth2() throws Exception {
        // Test that a new Item cannot be edited by default
        context.turnOffAuthorisationSystem();
        WorkspaceItem wi = workspaceItemService.create(context, collection, true);
        context.restoreAuthSystemState();
        Item item = wi.getItem();

        // Disallow Item WRITE perms
        when(authorizeServiceSpy.authorizeActionBoolean(context, item, Constants.WRITE)).thenReturn(false);

        assertFalse("testCanEditBooleanNoAuth2 0", itemService.canEdit(context, item));
    }

    /**
     * Test of isInProgressSubmission method, of class Item.
     *
     * @throws AuthorizeException
     * @throws SQLException
     * @throws IOException
     */
    @Test
    public void testIsInProgressSubmission() throws SQLException, AuthorizeException, IOException {
        context.turnOffAuthorisationSystem();
        Collection c = createCollection();
        WorkspaceItem wi = workspaceItemService.create(context, c, true);
        context.restoreAuthSystemState();
        assertTrue("testIsInProgressSubmission 0", itemService.isInProgressSubmission(context, wi.getItem()));
    }

    /**
     * Test of isInProgressSubmission method, of class Item.
     *
     * @throws AuthorizeException
     * @throws SQLException
     * @throws IOException
     */
    @Test
    public void testIsInProgressSubmissionFalse() throws SQLException, AuthorizeException, IOException {
        context.turnOffAuthorisationSystem();
        Collection c = createCollection();
        WorkspaceItem wi = workspaceItemService.create(context, c, true);
        Item item = installItemService.installItem(context, wi);
        context.restoreAuthSystemState();
        assertFalse("testIsInProgressSubmissionFalse 0", itemService.isInProgressSubmission(context, item));
    }

    /**
     * Test of isInProgressSubmission method, of class Item.
     *
     * @throws AuthorizeException
     * @throws SQLException
     * @throws IOException
     */
    @Test
    public void testIsInProgressSubmissionFalse2() throws SQLException, AuthorizeException, IOException {
        context.turnOffAuthorisationSystem();
        Collection c = createCollection();
        collectionService.createTemplateItem(context, c);
        collectionService.update(context, c);
        Item item = c.getTemplateItem();
        context.restoreAuthSystemState();
        assertFalse("testIsInProgressSubmissionFalse2 0", itemService.isInProgressSubmission(context, item));
    }

    /**
     * Test of getName method, of class Item.
     */
    @Override
    @Test
    public void testGetName() {
        assertThat("testGetName 0", it.getName(), nullValue());
    }

    /**
     * Test of findByMetadataField method, of class Item.
     */
    @Test
    public void testFindByMetadataField() throws Exception {
        String schema = "dc";
        String element = "contributor";
        String qualifier = "author";
        String value = "value";

        Iterator<Item> result = itemService.findByMetadataField(context, schema, element, qualifier, value);
        assertThat("testFindByMetadataField 0", result, notNullValue());
        assertFalse("testFindByMetadataField 1", result.hasNext());

        // add new metadata to item
        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, it, schema, element, qualifier, Item.ANY, value);
        itemService.update(context, it);
        context.restoreAuthSystemState();

        result = itemService.findByMetadataField(context, schema, element, qualifier, value);
        assertThat("testFindByMetadataField 3", result, notNullValue());
        assertTrue("testFindByMetadataField 4", result.hasNext());
        assertTrue("testFindByMetadataField 5", result.next().equals(it));
    }

    /**
     * Test of getAdminObject method, of class Item.
     */
    @Test
    @Override
    public void testGetAdminObject() throws SQLException {
        //default community has no admin object
        assertThat("testGetAdminObject 0", (Item) itemService.getAdminObject(context, it, Constants.REMOVE),
                   equalTo(it));
        assertThat("testGetAdminObject 1", (Item) itemService.getAdminObject(context, it, Constants.ADD), equalTo(it));
        assertThat("testGetAdminObject 2", (Item) itemService.getAdminObject(context, it, Constants.DELETE),
                   equalTo(it));
        assertThat("testGetAdminObject 3", (Item) itemService.getAdminObject(context, it, Constants.ADMIN),
                   equalTo(it));
    }

    /**
     * Test of getParentObject method, of class Item.
     */
    @Test
    @Override
    public void testGetParentObject() throws SQLException {
        try {
            //default has no parent
            assertThat("testGetParentObject 0", itemService.getParentObject(context, it), notNullValue());

            context.turnOffAuthorisationSystem();
            Collection parent = createCollection();
            it.setOwningCollection(parent);
            context.restoreAuthSystemState();
            assertThat("testGetParentObject 1", itemService.getParentObject(context, it), notNullValue());
            assertThat("testGetParentObject 2", (Collection) itemService.getParentObject(context, it), equalTo(parent));
        } catch (AuthorizeException ex) {
            throw new AssertionError("Authorize Exception occurred", ex);
        }
    }

    /**
     * Test of findByAuthorityValue method, of class Item.
     */
    @Test
    public void testFindByAuthorityValue() throws Exception {
        String schema = "dc";
        String element = "language";
        String qualifier = "iso";
        String value = "en";
        String authority = "accepted";
        int confidence = 0;

        Iterator<Item> result = itemService.findByAuthorityValue(context, schema, element, qualifier, value);
        assertThat("testFindByAuthorityValue 0", result, notNullValue());
        assertFalse("testFindByAuthorityValue 1", result.hasNext());

        // add new metadata (with authority) to item
        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, it, schema, element, qualifier, Item.ANY, value, authority, confidence);
        itemService.update(context, it);
        context.restoreAuthSystemState();

        result = itemService.findByAuthorityValue(context, schema, element, qualifier, authority);
        assertThat("testFindByAuthorityValue 3", result, notNullValue());
        assertTrue("testFindByAuthorityValue 4", result.hasNext());
        assertThat("testFindByAuthorityValue 5", result.next(), equalTo(it));
    }

    /**
     * Test of countByCollectionMapping method, of ItemService
     */
    @Test
    public void testFindByCollectionMapping() throws Exception {
        int limit = 5;
        int offset = 0;
        context.turnOffAuthorisationSystem();
        Collection colToMapTo = this.createCollection();
        Item item1 = this.createItem();

        Iterator<Item> result = itemService.findByCollectionMapping(context, colToMapTo, limit, offset);
        assertThat("testFindByCollectionMapping 0", result, notNullValue());
        assertFalse("testFindByCollectionMapping 1", result.hasNext());

        //map item1 to colToMapTO
        collectionService.addItem(context, colToMapTo, item1);
        collectionService.update(context, colToMapTo);
        context.restoreAuthSystemState();

        result = itemService.findByCollectionMapping(context, colToMapTo, limit, offset);
        assertThat("testFindByCollectionMapping 3", result, notNullValue());
        assertTrue("testFindByCollectionMapping 4", result.hasNext());
        assertThat("testFindByCollectionMapping 5", result.next(), equalTo(item1));

        //Pagination tests
        //map item2 to colToMapTO
        context.turnOffAuthorisationSystem();
        Item item2 = this.createItem();
        collectionService.addItem(context, colToMapTo, item2);
        context.restoreAuthSystemState();

        limit = 5;
        offset = 1;
        result = itemService.findByCollectionMapping(context, colToMapTo, limit, offset);
        Item secondItemMapped = result.next();
        assertTrue("testFindByCollectionMapping 7", secondItemMapped.equals(item1) || secondItemMapped.equals(item2));
        assertFalse("testFindByCollectionMapping 8", result.hasNext());
        limit = 1;
        offset = 0;
        result = itemService.findByCollectionMapping(context, colToMapTo, limit, offset);
        Item onlyItemFound = result.next();
        assertTrue("testFindByCollectionMapping 9", onlyItemFound .equals(item1) || onlyItemFound .equals(item2));
        assertFalse("testFindByCollectionMapping 10", result.hasNext());
        limit = 5;
        offset = 3;
        result = itemService.findByCollectionMapping(context, colToMapTo, limit, offset);
        assertFalse("testFindByCollectionMapping 11", result.hasNext());

    }

    /**
     * Test of countByCollectionMapping method, of ItemService
     */
    @Test
    public void testCountByCollectionMapping() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection colToMapTo = this.createCollection();
        Item item1 = this.createItem();
        Item item2 = this.createItem();

        int result = itemService.countByCollectionMapping(context, colToMapTo);
        assertThat("testFindByCollectionMapping 0", result, notNullValue());
        assertTrue("testFindByCollectionMapping 1", result == 0);

        //map items to colToMapTO
        collectionService.addItem(context, colToMapTo, item1);
        collectionService.addItem(context, colToMapTo, item2);
        collectionService.update(context, colToMapTo);
        context.restoreAuthSystemState();

        result = itemService.countByCollectionMapping(context, colToMapTo);
        assertThat("testFindByCollectionMapping 3", result, notNullValue());
        assertTrue("testFindByCollectionMapping 1", result == 2);
    }

    protected Collection createCollection() throws SQLException, AuthorizeException {
        return collectionService.create(context, owningCommunity);
    }

    protected Item createItem() throws SQLException, IOException, AuthorizeException, IllegalAccessException {
        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
        return installItemService.installItem(context, workspaceItem);
    }

}
