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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.MockUtil;
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
    @BeforeEach
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
            if (!MockUtil.isMock(authorizeService)) {
                authorizeServiceSpy = spy(authorizeService);
            } else {
                authorizeServiceSpy = authorizeService;
            }
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
    @AfterEach
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
        assertTrue(added, "testFindAll 1");
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
        assertTrue(added, "testFindBySubmitter 1");

        context.turnOffAuthorisationSystem();
        all = itemService.findBySubmitter(context, ePersonService.create(context));
        context.restoreAuthSystemState();

        assertThat("testFindBySubmitter 2", all, notNullValue());
        assertFalse(all.hasNext(), "testFindBySubmitter 3");
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
                it.getLastModified().plus(1, ChronoUnit.DAYS));
        assertThat("Returned list should not be null", all, notNullValue());
        boolean added = false;
        while (all.hasNext()) {
            Item tmp = all.next();
            if (tmp.equals(it)) {
                added = true;
            }
        }
         // Test 1: we should NOT find our item in this list
        assertFalse(added, "List should not contain item when passing a date newer than item last-modified date");
         // Test 2: Using a past 'modified since' date, we should get a non-null list containing our item
        all = itemService.findInArchiveOrWithdrawnDiscoverableModifiedSince(context,
                it.getLastModified().minus(1, ChronoUnit.DAYS));
        assertThat("Returned list should not be null", all, notNullValue());
        added = false;
        while (all.hasNext()) {
            Item tmp = all.next();
            if (tmp.equals(it)) {
                added = true;
            }
        }
        // Test 3: we should find our item in this list
        assertTrue(added, "List should contain item when passing a date older than item last-modified date");
         // Repeat Tests 2, 3 with withdrawn = false and archived = true as this should result in same behaviour
        it.setWithdrawn(false);
        it.setArchived(true);
         // Test 4: Using a past 'modified since' date, we should get a non-null list containing our item
        all = itemService.findInArchiveOrWithdrawnDiscoverableModifiedSince(context,
                it.getLastModified().minus(1, ChronoUnit.DAYS));
        assertThat("Returned list should not be null", all, notNullValue());
        added = false;
        while (all.hasNext()) {
            Item tmp = all.next();
            if (tmp.equals(it)) {
                added = true;
            }
        }
        // Test 5: We should find our item in this list
        assertTrue(added, "List should contain item when passing a date older than item last-modified date");
         // Test 6: Make sure non-discoverable items are not returned, regardless of archived/withdrawn state
        it.setDiscoverable(false);
        all = itemService.findInArchiveOrWithdrawnDiscoverableModifiedSince(context,
                it.getLastModified().minus(1, ChronoUnit.DAYS));
        assertThat("Returned list should not be null", all, notNullValue());
        added = false;
        while (all.hasNext()) {
            Item tmp = all.next();
            if (tmp.equals(it)) {
                added = true;
            }
        }
        // Test 7: We should not find our item in this list
        assertFalse(added, "List should not contain non-discoverable items");
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
                it.getLastModified().plus(1, ChronoUnit.DAYS));
        assertThat("Returned list should not be null", all, notNullValue());
        boolean added = false;
        while (all.hasNext()) {
            Item tmp = all.next();
            if (tmp.equals(it)) {
                added = true;
            }
        }
         // Test 1: We should NOT find our item in this list
        assertFalse(added, "List should not contain item when passing a date newer than item last-modified date");
         // Test 2: Using a past 'modified since' date, we should get a non-null list containing our item
        all = itemService.findInArchiveOrWithdrawnNonDiscoverableModifiedSince(context,
                it.getLastModified().minus(1, ChronoUnit.DAYS));
        assertThat("Returned list should not be null", all, notNullValue());
        added = false;
        while (all.hasNext()) {
            Item tmp = all.next();
            if (tmp.equals(it)) {
                added = true;
            }
        }
         // Test 3: We should find our item in this list
        assertTrue(added, "List should contain item when passing a date older than item last-modified date");
         // Repeat Tests 2, 3 with discoverable = true
        it.setDiscoverable(true);
         // Test 4: Now we should still get a non-null list with NO items since item is discoverable
        all = itemService.findInArchiveOrWithdrawnNonDiscoverableModifiedSince(context,
                it.getLastModified().minus(1, ChronoUnit.DAYS));
        assertThat("Returned list should not be null", all, notNullValue());
        added = false;
        while (all.hasNext()) {
            Item tmp = all.next();
            if (tmp.equals(it)) {
                added = true;
            }
        }
         // Test 5: We should NOT find our item in this list
        assertFalse(added, "List should not contain discoverable items");
    }

    /**
     * Test of getID method, of class Item.
     */
    @Override
    @Test
    public void testGetID() {
        assertTrue(it.getID() != null, "testGetID 0");
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
        assertTrue(it.isArchived(), "testIsArchived 0");

        //false by default
        context.turnOffAuthorisationSystem();
        Item tmp = createItem();
        context.restoreAuthSystemState();
        assertTrue(tmp.isArchived(), "testIsArchived 1");
    }

    /**
     * Test of isWithdrawn method, of class Item.
     */
    @Test
    public void testIsWithdrawn() {
        assertFalse(it.isWithdrawn(), "testIsWithdrawn 0");
    }

    /**
     * Test of getLastModified method, of class Item.
     */
    @Test
    public void testGetLastModified() {
        assertThat("testGetLastModified 0", it.getLastModified(), notNullValue());
        assertEquals(it.getLastModified().atZone(ZoneOffset.UTC).toLocalDate(),
                     LocalDate.now(ZoneOffset.UTC),
                     "testGetLastModified is same day");
    }

    /**
     * Test of setArchived method, of class Item.
     */
    @Test
    public void testSetArchived() {
        it.setArchived(true);
        assertTrue(it.isArchived(), "testSetArchived 0");
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
        assertEquals(it.getOwningCollection(), collection, "testGetOwningCollection 1");
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
        assertTrue(dc.size() == 0, "testGetMetadata_4args 1");
    }

    /**
     * Test of getMetadataByMetadataString method, of class Item.
     */
    @Test
    public void testGetMetadata_String() {
        String mdString = "dc.contributor.author";
        List<MetadataValue> dc = itemService.getMetadataByMetadataString(it, mdString);
        assertThat("testGetMetadata_String 0", dc, notNullValue());
        assertTrue(dc.size() == 0, "testGetMetadata_String 1");

        mdString = "dc.contributor.*";
        dc = itemService.getMetadataByMetadataString(it, mdString);
        assertThat("testGetMetadata_String 2", dc, notNullValue());
        assertTrue(dc.size() == 0, "testGetMetadata_String 3");

        mdString = "dc.contributor";
        dc = itemService.getMetadataByMetadataString(it, mdString);
        assertThat("testGetMetadata_String 4", dc, notNullValue());
        assertTrue(dc.size() == 0, "testGetMetadata_String 5");
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
        assertTrue(values.size() == 1, "Return results");

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
        String lang = null;
        String[] values = {"value0", "value1"};
        itemService.addMetadata(context, it, schema, element, qualifier, lang, Arrays.asList(values));

        List<MetadataValue> dc = itemService.getMetadata(it, schema, element, qualifier, Item.ANY);
        assertThat("testAddMetadata_5args_1 0", dc, notNullValue());
        assertTrue(dc.size() == 2, "testAddMetadata_5args_1 1");
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

    @Test
    public void testAddMetadata_5args_no_values() {
        assertThrows(IllegalArgumentException.class, () -> {
            String schema = "dc";
            String element = "contributor";
            String qualifier = "author";
            String lang = null;
            String[] values = {};
            itemService.addMetadata(context, it, schema, element, qualifier, lang, Arrays.asList(values));
            fail("IllegalArgumentException expected");
        });
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
        String lang = null;
        List<String> values = Arrays.asList("en_US", "en");
        List<String> authorities = Arrays.asList("accepted", "uncertain");
        List<Integer> confidences = Arrays.asList(0, 0);
        itemService.addMetadata(context, it, schema, element, qualifier, lang, values, authorities, confidences);

        List<MetadataValue> dc = itemService.getMetadata(it, schema, element, qualifier, Item.ANY);
        assertThat("testAddMetadata_7args_1 0", dc, notNullValue());
        assertTrue(dc.size() == 2, "testAddMetadata_7args_1 1");
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
        String lang = null;
        List<String> values = Arrays.asList("value0", "value1");
        List<String> authorities = Arrays.asList("auth0", "auth2");
        List<Integer> confidences = Arrays.asList(0, 0);
        itemService.addMetadata(context, it, schema, element, qualifier, lang, values, authorities, confidences);

        List<MetadataValue> dc = itemService.getMetadata(it, schema, element, qualifier, Item.ANY);
        assertThat("testAddMetadata_7args_1 0", dc, notNullValue());
        assertTrue(dc.size() == 2, "testAddMetadata_7args_1 1");
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

    @Test
    public void testAddMetadata_7args_no_values() {
        assertThrows(IllegalArgumentException.class, () -> {
            String schema = "dc";
            String element = "contributor";
            String qualifier = "author";
            String lang = null;
            List<String> values = new ArrayList();
            List<String> authorities = new ArrayList();
            List<Integer> confidences = new ArrayList();
            itemService.addMetadata(context, it, schema, element, qualifier, lang, values, authorities, confidences);
            fail("IllegalArgumentException expected");
        });
    }

    @Test
    public void testAddMetadata_list_with_virtual_metadata() throws Exception {
        String schema = "dc";
        String element = "contributor";
        String qualifier = "author";
        String lang = null;
        // Create two fake virtual metadata ("virtual::[relationship-id]") values
        List<String> values = new ArrayList<>(Arrays.asList("uuid-1", "uuid-2"));
        List<String> authorities = new ArrayList<>(Arrays.asList(Constants.VIRTUAL_AUTHORITY_PREFIX + "relationship-1",
                                                 Constants.VIRTUAL_AUTHORITY_PREFIX + "relationship-2"));
        List<Integer> confidences = new ArrayList<>(Arrays.asList(-1, -1));

        // Virtual metadata values will be IGNORED. No metadata should be added as we are calling addMetadata()
        // with two virtual metadata values.
        List<MetadataValue> valuesAdded = itemService.addMetadata(context, it, schema, element, qualifier, lang,
                                                                  values, authorities, confidences);
        assertNotNull(valuesAdded);
        assertTrue(valuesAdded.isEmpty());

        // Now, update tests values to append a third value which is NOT virtual metadata
        String newValue = "new-metadata-value";
        String newAuthority = "auth0";
        Integer newConfidence = 0;
        values.add(newValue);
        authorities.add(newAuthority);
        confidences.add(newConfidence);

        // Call addMetadata again, and this time only one value (the new, non-virtual metadata) should be added
        valuesAdded = itemService.addMetadata(context, it, schema, element, qualifier, lang,
                                              values, authorities, confidences);
        assertNotNull(valuesAdded);
        assertEquals(1, valuesAdded.size());

        // Get metadata and ensure new value is the ONLY ONE for this metadata field
        List<MetadataValue> dc = itemService.getMetadata(it, schema, element, qualifier, Item.ANY);
        assertNotNull(dc);
        assertEquals(1, dc.size());
        assertEquals(schema, dc.get(0).getMetadataField().getMetadataSchema().getName());
        assertEquals(element, dc.get(0).getMetadataField().getElement());
        assertEquals(qualifier, dc.get(0).getMetadataField().getQualifier());
        assertEquals(newValue, dc.get(0).getValue());
        assertNull(dc.get(0).getAuthority());
        assertEquals(-1, dc.get(0).getConfidence());
    }

    /**
     * This is the same as testAddMetadata_5args_1 except it is adding a *single* value as a String, not a List.
     */
    @Test
    public void testAddMetadata_5args_2() throws SQLException {
        String schema = "dc";
        String element = "contributor";
        String qualifier = "author";
        String lang = null;
        String value = "value0";
        itemService.addMetadata(context, it, schema, element, qualifier, lang, value);

        List<MetadataValue> dc = itemService.getMetadata(it, schema, element, qualifier, Item.ANY);
        assertThat("testAddMetadata_5args_2 0", dc, notNullValue());
        assertTrue(dc.size() == 1, "testAddMetadata_5args_2 1");
        assertThat("testAddMetadata_5args_2 2", dc.get(0).getMetadataField().getMetadataSchema().getName(),
                   equalTo(schema));
        assertThat("testAddMetadata_5args_2 3", dc.get(0).getMetadataField().getElement(), equalTo(element));
        assertThat("testAddMetadata_5args_2 4", dc.get(0).getMetadataField().getQualifier(), equalTo(qualifier));
        assertThat("testAddMetadata_5args_2 5", dc.get(0).getLanguage(), equalTo(lang));
        assertThat("testAddMetadata_5args_2 6", dc.get(0).getValue(), equalTo(value));
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
        String lang = null;
        String values = "en";
        String authorities = "accepted";
        int confidences = 0;
        itemService.addMetadata(context, it, schema, element, qualifier, lang, values, authorities, confidences);

        List<MetadataValue> dc = itemService.getMetadata(it, schema, element, qualifier, Item.ANY);
        assertThat("testAddMetadata_7args_2 0", dc, notNullValue());
        assertTrue(dc.size() == 1, "testAddMetadata_7args_2 1");
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
        String lang = null;
        String values = "value0";
        String authorities = "auth0";
        int confidences = 0;
        itemService.addMetadata(context, it, schema, element, qualifier, lang, values, authorities, confidences);

        List<MetadataValue> dc = itemService.getMetadata(it, schema, element, qualifier, Item.ANY);
        assertThat("testAddMetadata_7args_2 0", dc, notNullValue());
        assertTrue(dc.size() == 1, "testAddMetadata_7args_2 1");
        assertThat("testAddMetadata_7args_2 2", dc.get(0).getMetadataField().getMetadataSchema().getName(),
                   equalTo(schema));
        assertThat("testAddMetadata_7args_2 3", dc.get(0).getMetadataField().getElement(), equalTo(element));
        assertThat("testAddMetadata_7args_2 4", dc.get(0).getMetadataField().getQualifier(), equalTo(qualifier));
        assertThat("testAddMetadata_7args_2 5", dc.get(0).getLanguage(), equalTo(lang));
        assertThat("testAddMetadata_7args_2 6", dc.get(0).getValue(), equalTo(values));
        assertThat("testAddMetadata_7args_2 7", dc.get(0).getAuthority(), nullValue());
        assertThat("testAddMetadata_7args_2 8", dc.get(0).getConfidence(), equalTo(-1));
    }

    @Test
    public void testAddMetadata_single_virtual_metadata() throws Exception {
        String schema = "dc";
        String element = "contributor";
        String qualifier = "author";
        String lang = null;
        // Create a single fake virtual metadata ("virtual::[relationship-id]") value
        String value = "uuid-1";
        String authority = Constants.VIRTUAL_AUTHORITY_PREFIX + "relationship-1";
        Integer confidence = -1;

        // Virtual metadata values will be IGNORED. No metadata should be added as we are calling addMetadata()
        // with a virtual metadata value.
        MetadataValue valuesAdded = itemService.addMetadata(context, it, schema, element, qualifier, lang,
                                                            value, authority, confidence);
        // Returned object will be null when no metadata was added
        assertNull(valuesAdded);

        // Verify this metadata field does NOT exist on the item
        List<MetadataValue> mv = itemService.getMetadata(it, schema, element, qualifier, Item.ANY);
        assertNotNull(mv);
        assertTrue(mv.isEmpty());

        // Also try calling addMetadata() with MetadataField object
        MetadataField metadataField = metadataFieldService.findByElement(context, schema, element, qualifier);
        valuesAdded = itemService.addMetadata(context, it, metadataField, lang, value, authority, confidence);
        // Returned object should still be null
        assertNull(valuesAdded);

        // Verify this metadata field does NOT exist on the item
        mv = itemService.getMetadata(it, schema, element, qualifier, Item.ANY);
        assertNotNull(mv);
        assertTrue(mv.isEmpty());
    }


    /**
     * Test of clearMetadata method, of class Item.
     */
    @Test
    public void testClearMetadata() throws SQLException {
        String schema = "dc";
        String element = "contributor";
        String qualifier = "author";
        String lang = null;
        String values = "value0";
        itemService.addMetadata(context, it, schema, element, qualifier, lang, values);

        itemService.clearMetadata(context, it, schema, element, qualifier, Item.ANY);

        List<MetadataValue> dc = itemService.getMetadata(it, schema, element, qualifier, Item.ANY);
        assertThat("testClearMetadata 0", dc, notNullValue());
        assertTrue(dc.size() == 0, "testClearMetadata 1");
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
        assertEquals(tmp.getSubmitter(), context.getCurrentUser(), "testGetSubmitter 1");
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
                                                 "title", null, null, "collection B");
        it.addCollection(collection);
        collection = collectionService.create(context, owningCommunity);
        collectionService.setMetadataSingleValue(context, collection, MetadataSchemaEnum.DC.getName(),
                                                 "title", null, null, "collection A");
        it.addCollection(collection);
        context.restoreAuthSystemState();
        assertThat("testGetCollections 0", it.getCollections(), notNullValue());
        assertTrue(it.getCollections().size() == 3, "testGetCollections 1");
        assertTrue(it.getCollections().get(1).getName().equals("collection A"), "testGetCollections 2");
        assertTrue(it.getCollections().get(2).getName().equals("collection B"), "testGetCollections 3");
    }

    /**
     * Test of getCommunities method, of class Item.
     */
    @Test
    public void testGetCommunities() throws Exception {
        assertThat("testGetCommunities 0", itemService.getCommunities(context, it), notNullValue());
        assertTrue(itemService.getCommunities(context, it).size() == 1, "testGetCommunities 1");
    }

    /**
     * Test of getBundles method, of class Item.
     */
    @Test
    public void testGetBundles_0args() throws Exception {
        assertThat("testGetBundles_0args 0", it.getBundles(), notNullValue());
        assertTrue(it.getBundles().size() == 0, "testGetBundles_0args 1");
    }

    /**
     * Test of getBundles method, of class Item.
     */
    @Test
    public void testGetBundles_String() throws Exception {
        String name = "name";
        assertThat("testGetBundles_String 0", itemService.getBundles(it, name), notNullValue());
        assertTrue(itemService.getBundles(it, name).size() == 0, "testGetBundles_String 1");
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
        assertTrue(itemService.getBundles(it, name).size() == 1, "testCreateBundleAuth 3");
    }

    /**
     * Test of createBundle method, of class Item.
     */
    @Test
    public void testCreateBundleNoName() {
        assertThrows(SQLException.class, () -> {
            bundleService.create(context, it, "");
            fail("Exception expected");
        });
    }

    /**
     * Test of createBundle method, of class Item.
     */
    @Test
    public void testCreateBundleNullName() {
        assertThrows(SQLException.class, () -> {
            bundleService.create(context, it, null);
            fail("Exception expected");
        });
    }

    /**
     * Test of createBundle method, of class Item.
     */
    @Test
    public void testCreateBundleNoAuth() {
        assertThrows(AuthorizeException.class, () -> {
            String name = "bundle";
            bundleService.create(context, it, name);
            fail("Exception expected");
        });
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
        assertTrue(itemService.getBundles(it, name).size() == 1, "testAddBundleAuth 1");
        assertThat("testAddBundleAuth 2", itemService.getBundles(it, name).get(0), equalTo(created));
    }

    /**
     * Test of addBundle method, of class Item.
     */
    @Test
    public void testAddBundleNoAuth() {
        assertThrows(AuthorizeException.class, () -> {
            String name = "bundle";
            Bundle created = bundleService.create(context, it, name);
            created.setName(context, name);
            itemService.addBundle(context, it, created);
            fail("Exception expected");
        });
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
        assertTrue(itemService.getBundles(it, name).size() == 0, "testRemoveBundleAuth 1");
    }

    /**
     * Test of removeBundle method, of class Item.
     */
    @Test
    public void testRemoveBundleNoAuth() {
        assertThrows(AuthorizeException.class, () -> {
            // First create a bundle for test
            context.turnOffAuthorisationSystem();
            String name = "bundle";
            Bundle created = bundleService.create(context, it, name);
            created.setName(context, name);
            itemService.addBundle(context, it, created);
            context.restoreAuthSystemState();

            itemService.removeBundle(context, it, created);
            fail("Exception expected");
        });
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
    @Test
    public void testCreateSingleBitstream_InputStream_StringNoAuth() {
        assertThrows(AuthorizeException.class, () -> {
            String name = "new bundle";
            File f = new File(testProps.get("test.bitstream").toString());
            itemService.createSingleBitstream(context, new FileInputStream(f), it, name);
            fail("Exception expected");
        });
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
    @Test
    public void testCreateSingleBitstream_InputStreamNoAuth() {
        assertThrows(AuthorizeException.class, () -> {
            File f = new File(testProps.get("test.bitstream").toString());
            itemService.createSingleBitstream(context, new FileInputStream(f), it);
            fail("Expected exception");
        });
    }

    /**
     * Test of getNonInternalBitstreams method, of class Item.
     */
    @Test
    public void testGetNonInternalBitstreams() throws Exception {
        assertThat("testGetNonInternalBitstreams 0", itemService.getNonInternalBitstreams(context, it), notNullValue());
        assertTrue(itemService.getNonInternalBitstreams(context, it).size() == 0, "testGetNonInternalBitstreams 1");
    }

    /**
     * Test of removeDSpaceLicense method, of class Item.
     */
    @Test
    public void testRemoveDSpaceLicenseAuth() throws Exception {
        // First create a bundle for test
        context.turnOffAuthorisationSystem();
        String name = Constants.LICENSE_BUNDLE_NAME;
        Bundle created = bundleService.create(context, it, name);
        created.setName(context, name);
        context.restoreAuthSystemState();

        // Allow Item REMOVE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, it, Constants.REMOVE);
        // Allow Bundle DELETE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, created, Constants.DELETE);

        itemService.removeDSpaceLicense(context, it);
        assertThat("testRemoveDSpaceLicenseAuth 0", itemService.getBundles(it, name), notNullValue());
        assertTrue(itemService.getBundles(it, name).size() == 0, "testRemoveDSpaceLicenseAuth 1");
    }

    /**
     * Test of removeDSpaceLicense method, of class Item.
     */
    @Test
    public void testRemoveDSpaceLicenseNoAuth() {
        assertThrows(AuthorizeException.class, () -> {
            // First create a bundle for test
            context.turnOffAuthorisationSystem();
            String name = Constants.LICENSE_BUNDLE_NAME;
            Bundle created = bundleService.create(context, it, name);
            created.setName(context, name);
            context.restoreAuthSystemState();

            itemService.removeDSpaceLicense(context, it);
            fail("Exception expected");
        });
    }

    /**
     * Test of removeLicenses method, of class Item.
     */
    @Test
    public void testRemoveLicensesAuth() throws Exception {
        // First create test content
        context.turnOffAuthorisationSystem();
        String name = Constants.LICENSE_BUNDLE_NAME;
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
        assertTrue(itemService.getBundles(it, name).size() == 0, "testRemoveLicensesAuth 1");
    }

    /**
     * Test of removeLicenses method, of class Item.
     */
    @Test
    public void testRemoveLicensesNoAuth() {
        assertThrows(AuthorizeException.class, () -> {
            // First create test content
            context.turnOffAuthorisationSystem();
            String name = Constants.LICENSE_BUNDLE_NAME;
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
        });
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
    @Test
    public void testUpdateNoAuth() {
        assertThrows(AuthorizeException.class, () -> {
            context.turnOffAuthorisationSystem();
            Collection c = createCollection();
            it.setOwningCollection(c);
            context.restoreAuthSystemState();

            itemService.update(context, it);
        });
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
        assertTrue(it.isWithdrawn(), "testWithdrawAuth 0");
    }

    /**
     * Test of withdraw method, of class Item.
     */
    @Test
    public void testWithdrawNoAuth() {
        assertThrows(AuthorizeException.class, () -> {
            itemService.withdraw(context, it);
            fail("Exception expected");
        });
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
        assertFalse(it.isWithdrawn(), "testReinstate 0");
    }

    /**
     * Test of reinstate method, of class Item.
     */
    @Test
    public void testReinstateNoAuth() {
        assertThrows(AuthorizeException.class, () -> {
            // initialize item as withdrawn
            context.turnOffAuthorisationSystem();
            itemService.withdraw(context, it);
            context.restoreAuthSystemState();

            itemService.reinstate(context, it);
            fail("Exception expected");
        });
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

        UUID id = item.getID();
        itemService.delete(context, item);
        Item found = itemService.find(context, id);
        assertThat("testDeleteAuth 0", found, nullValue());
    }

    /**
     * Test of delete method, of class Item.
     */
    @Test
    public void testDeleteNoAuth() {
        assertThrows(AuthorizeException.class, () -> {
            itemService.delete(context, it);
            fail("Exception expected");
        });
    }

    /**
     * Test of equals method, of class Item.
     */
    @Test
    @SuppressWarnings("ObjectEqualsNull")
    public void testEquals() throws SQLException, AuthorizeException, IOException, IllegalAccessException {
        assertFalse(it.equals(null), "testEquals 0");

        // create a new item to test against
        context.turnOffAuthorisationSystem();
        Item item = createItem();
        context.restoreAuthSystemState();

        try {
            assertFalse(it.equals(item), "testEquals 1");
            assertTrue(it.equals(it), "testEquals 2");
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
        assertFalse(result, "testIsOwningCollection 0");
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
        ResourcePolicy pol1 = resourcePolicyService.create(context, eperson, null);
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
        String name = Constants.LICENSE_BUNDLE_NAME;
        Bundle created = bundleService.create(context, it, name);
        created.setName(context, name);

        String bsname = "License";
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream result = itemService.createSingleBitstream(context, new FileInputStream(f), it, bsname);
        bitstreamService.setFormat(context, result, bitstreamFormatService.findByShortDescription(context, bsname));
        bundleService.addBitstream(context, created, result);

        List<ResourcePolicy> newpolicies = new ArrayList<ResourcePolicy>();
        newpolicies.add(resourcePolicyService.create(context, eperson, null));
        newpolicies.add(resourcePolicyService.create(context, eperson, null));
        newpolicies.add(resourcePolicyService.create(context, eperson, null));
        context.restoreAuthSystemState();

        itemService.replaceAllBitstreamPolicies(context, it, newpolicies);

        List<ResourcePolicy> retrieved = new ArrayList<ResourcePolicy>();
        List<Bundle> bundles = it.getBundles();
        for (Bundle b : bundles) {
            retrieved.addAll(authorizeService.getPolicies(context, b));
            retrieved.addAll(bundleService.getBitstreamPolicies(context, b));
        }
        assertFalse(retrieved.isEmpty(), "testReplaceAllBitstreamPolicies 0");

        boolean equals = true;
        for (int i = 0; i < newpolicies.size() && equals; i++) {
            if (!newpolicies.contains(retrieved.get(i))) {
                equals = false;
            }
        }
        assertTrue(equals, "testReplaceAllBitstreamPolicies 1");
    }

    /**
     * Test of removeGroupPolicies method, of class Item.
     */
    @Test
    public void testRemoveGroupPolicies() throws Exception {
        context.turnOffAuthorisationSystem();
        List<ResourcePolicy> newpolicies = new ArrayList<ResourcePolicy>();
        Group g = groupService.create(context);
        ResourcePolicy pol1 = resourcePolicyService.create(context, null, g);
        newpolicies.add(pol1);
        itemService.replaceAllItemPolicies(context, it, newpolicies);

        itemService.removeGroupPolicies(context, it, g);
        context.restoreAuthSystemState();

        List<ResourcePolicy> retrieved = authorizeService.getPolicies(context, it);
        assertThat("testRemoveGroupPolicies 0", retrieved, notNullValue());
        assertTrue(retrieved.isEmpty(), "testRemoveGroupPolicies 1");
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
        String name = Constants.LICENSE_BUNDLE_NAME;
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
        assertTrue(equals, "testInheritCollectionDefaultPolicies 0");

        retrieved = new ArrayList<ResourcePolicy>();
        List<Bundle> bundles = it.getBundles();
        for (Bundle b : bundles) {
            retrieved.addAll(authorizeService.getPolicies(context, b));
            retrieved.addAll(bundleService.getBitstreamPolicies(context, b));
        }
        assertFalse(retrieved.isEmpty(), "testInheritCollectionDefaultPolicies 1");

        equals = true;
        for (int i = 0; i < newPolicies.size() && equals; i++) {
            if (!newPolicies.contains(retrieved.get(i))) {
                equals = false;
            }
        }
        assertTrue(equals, "testInheritCollectionDefaultPolicies 2");
    }

    // Test to verify DEFAULT_*_READ policies on collection inherit properly to Item/Bundle/Bitstream
    @Test
    public void testInheritCollectionDefaultPolicies_custom_default_groups() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create a new collection
        Collection c = createCollection();
        // Create a custom group with DEFAULT_ITEM_READ privileges in this Collection
        Group item_read_role = collectionService.createDefaultReadGroup(context, c, "ITEM",
                                                                        Constants.DEFAULT_ITEM_READ);
        // Create a custom group with DEFAULT_BITSTREAM_READ privileges in this Collection
        Group bitstream_read_role = collectionService.createDefaultReadGroup(context, c, "BITSTREAM",
                                                                        Constants.DEFAULT_BITSTREAM_READ);
        context.restoreAuthSystemState();

        // Verify that Collection's DEFAULT_ITEM_READ now uses the newly created group.
        List<ResourcePolicy> defaultItemReadPolicies =
            authorizeService.getPoliciesActionFilter(context, c, Constants.DEFAULT_ITEM_READ);
        assertEquals(1, defaultItemReadPolicies.size(), "One DEFAULT_ITEM_READ policy");
        assertEquals(item_read_role.getName(),
                     defaultItemReadPolicies.get(0).getGroup().getName(),
                     "DEFAULT_ITEM_READ group");

        // Verify that Collection's DEFAULT_BITSTREAM_READ now uses the newly created group.
        List<ResourcePolicy> defaultBitstreamReadPolicies =
            authorizeService.getPoliciesActionFilter(context, c, Constants.DEFAULT_BITSTREAM_READ);
        assertEquals(1, defaultBitstreamReadPolicies.size(), "One DEFAULT_BITSTREAM_READ policy on Collection");
        assertEquals(bitstream_read_role.getName(),
                     defaultBitstreamReadPolicies.get(0).getGroup().getName(),
                     "DEFAULT_BITSTREAM_READ group");

        context.turnOffAuthorisationSystem();
        // Create a new Item in this Collection
        WorkspaceItem workspaceItem = workspaceItemService.create(context, c, false);
        Item item = workspaceItem.getItem();
        // Add a single Bitstream to the ORIGINAL bundle
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream bitstream = itemService.createSingleBitstream(context, new FileInputStream(f), item);
        context.restoreAuthSystemState();

        // Allow Item WRITE perms
        doNothing().when(authorizeServiceSpy).authorizeAction(context, item, Constants.WRITE, true);
        // Inherit all default policies from Collection down to new Item
        itemService.inheritCollectionDefaultPolicies(context, item, c);

        // Verify Item inherits DEFAULT_ITEM_READ group from Collection
        List<ResourcePolicy> itemReadPolicies = authorizeService.getPoliciesActionFilter(context, item, Constants.READ);
        assertEquals(1, itemReadPolicies.size(), "One READ policy on Item");
        assertEquals(item_read_role.getName(),
                     itemReadPolicies.get(0).getGroup().getName(),
                     "Item's READ group");

        // Verify Bitstream inherits DEFAULT_BITSTREAM_READ group from Collection
        List<ResourcePolicy> bitstreamReadPolicies = authorizeService.getPoliciesActionFilter(context, bitstream,
                                                                                              Constants.READ);
        assertEquals(1, bitstreamReadPolicies.size(), "One READ policy on Bitstream");
        assertEquals(bitstream_read_role.getName(),
                     bitstreamReadPolicies.get(0).getGroup().getName(),
                     "Bitstream's READ group");

        // Verify ORIGINAL Bundle inherits DEFAULT_ITEM_READ group from Collection
        // Bundles should inherit from DEFAULT_ITEM_READ so that if the item is readable, the files
        // can be listed (even if files are access restricted or embargoed)
        List<Bundle> bundles = item.getBundles(Constants.DEFAULT_BUNDLE_NAME);
        Bundle originalBundle = bundles.get(0);
        List<ResourcePolicy> bundleReadPolicies = authorizeService.getPoliciesActionFilter(context, originalBundle,
                                                                                           Constants.READ);
        assertEquals(1, bundleReadPolicies.size(), "One READ policy on Bundle");
        assertEquals(item_read_role.getName(),
                     bundleReadPolicies.get(0).getGroup().getName(),
                     "Bundles's READ group");

        // Cleanup after ourselves. Delete created collection & all content under it
        context.turnOffAuthorisationSystem();
        collectionService.delete(context, c);
        context.restoreAuthSystemState();
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
        assertFalse(itemService.hasUploadedFiles(it), "testHasUploadedFiles 0");
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
        assertFalse(isin, "testGetCollectionsNotLinked 0");
    }

    /**
     * Test of canEdit method, of class Item.
     */
    @Test
    public void testCanEditBooleanAuth() throws Exception {
        // Allow Item WRITE perms
        when(authorizeServiceSpy.authorizeActionBoolean(context, it, Constants.WRITE)).thenReturn(true);

        assertTrue(itemService.canEdit(context, it), "testCanEditBooleanAuth 0");
    }

    /**
     * Test of canEdit method, of class Item.
     */
    @Test
    public void testCanEditBooleanAuth2() throws Exception {
        // Allow parent Community WRITE perms (test inheritance from community)
        when(authorizeServiceSpy.authorizeActionBoolean(context, owningCommunity, Constants.WRITE, false))
            .thenReturn(true);

        assertTrue(itemService.canEdit(context, it), "testCanEditBooleanAuth2 0");
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
        assertTrue(itemService.canEdit(context, it), "testCanEditBooleanAuth3 0");
    }

    /**
     * Test of canEdit method, of class Item.
     */
    @Test
    public void testCanEditBooleanNoAuth() throws Exception {
        assertFalse(itemService.canEdit(context, it), "testCanEditBooleanNoAuth 0");
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

        assertFalse(itemService.canEdit(context, item), "testCanEditBooleanNoAuth2 0");
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
        assertTrue(itemService.isInProgressSubmission(context, wi.getItem()), "testIsInProgressSubmission 0");
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
        assertFalse(itemService.isInProgressSubmission(context, item), "testIsInProgressSubmissionFalse 0");
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
        assertFalse(itemService.isInProgressSubmission(context, item), "testIsInProgressSubmissionFalse2 0");
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
        assertFalse(result.hasNext(), "testFindByMetadataField 1");

        // add new metadata to item
        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, it, schema, element, qualifier, null, value);
        itemService.update(context, it);
        context.restoreAuthSystemState();

        result = itemService.findByMetadataField(context, schema, element, qualifier, value);
        assertThat("testFindByMetadataField 3", result, notNullValue());
        assertTrue(result.hasNext(), "testFindByMetadataField 4");
        assertTrue(result.next().equals(it), "testFindByMetadataField 5");
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
        assertFalse(result.hasNext(), "testFindByAuthorityValue 1");

        // add new metadata (with authority) to item
        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, it, schema, element, qualifier, null, value, authority, confidence);
        itemService.update(context, it);
        context.restoreAuthSystemState();

        result = itemService.findByAuthorityValue(context, schema, element, qualifier, authority);
        assertThat("testFindByAuthorityValue 3", result, notNullValue());
        assertTrue(result.hasNext(), "testFindByAuthorityValue 4");
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
        assertFalse(result.hasNext(), "testFindByCollectionMapping 1");

        //map item1 to colToMapTO
        collectionService.addItem(context, colToMapTo, item1);
        collectionService.update(context, colToMapTo);
        context.restoreAuthSystemState();

        result = itemService.findByCollectionMapping(context, colToMapTo, limit, offset);
        assertThat("testFindByCollectionMapping 3", result, notNullValue());
        assertTrue(result.hasNext(), "testFindByCollectionMapping 4");
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
        assertTrue(secondItemMapped.equals(item1) || secondItemMapped.equals(item2), "testFindByCollectionMapping 7");
        assertFalse(result.hasNext(), "testFindByCollectionMapping 8");
        limit = 1;
        offset = 0;
        result = itemService.findByCollectionMapping(context, colToMapTo, limit, offset);
        Item onlyItemFound = result.next();
        assertTrue(onlyItemFound .equals(item1) || onlyItemFound .equals(item2), "testFindByCollectionMapping 9");
        assertFalse(result.hasNext(), "testFindByCollectionMapping 10");
        limit = 5;
        offset = 3;
        result = itemService.findByCollectionMapping(context, colToMapTo, limit, offset);
        assertFalse(result.hasNext(), "testFindByCollectionMapping 11");

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
        assertTrue(result == 0, "testFindByCollectionMapping 1");

        //map items to colToMapTO
        collectionService.addItem(context, colToMapTo, item1);
        collectionService.addItem(context, colToMapTo, item2);
        collectionService.update(context, colToMapTo);
        context.restoreAuthSystemState();

        result = itemService.countByCollectionMapping(context, colToMapTo);
        assertThat("testFindByCollectionMapping 3", result, notNullValue());
        assertTrue(result == 2, "testFindByCollectionMapping 1");
    }

    protected Collection createCollection() throws SQLException, AuthorizeException {
        return collectionService.create(context, owningCommunity);
    }

    protected Item createItem() throws SQLException, IOException, AuthorizeException, IllegalAccessException {
        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
        return installItemService.installItem(context, workspaceItem);
    }

}
