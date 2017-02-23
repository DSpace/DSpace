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
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

import org.apache.commons.lang.time.DateUtils;
import org.dspace.authorize.AuthorizeException;
import org.apache.log4j.Logger;

import java.util.*;

import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.junit.*;
import static org.junit.Assert.* ;
import static org.hamcrest.CoreMatchers.*;
import mockit.*;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.core.Constants;

/**
 * Unit Tests for class Item
 * @author pvillega
 */
public class ItemTest  extends AbstractDSpaceObjectTest
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(ItemTest.class);

    /**
     * Item instance for the tests
     */
    private Item it;

    private MetadataSchemaService metadataSchemaService = ContentServiceFactory.getInstance().getMetadataSchemaService();
    private BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance().getBitstreamFormatService();
    private MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();

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
            WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
            this.it = installItemService.installItem(context, workspaceItem);

            it.setSubmitter(context.getCurrentUser());
            itemService.update(context, it);
            this.dspaceObject = it;
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
//        try {
            context.turnOffAuthorisationSystem();
            //Get new instances, god knows what happended before
//            it = itemService.find(context, it.getID());
//            collection = collectionService.find(context, collection.getID());
//            owningCommunity = communityService.find(context, owningCommunity.getID());
//
//            communityService.delete(context, owningCommunity);
//            context.commit();
//            context.restoreAuthSystemState();
            it = null;
            collection = null;
            owningCommunity = null;
            super.destroy();
//        } catch (SQLException | AuthorizeException | IOException ex) {
//            if(context.isValid())
//            {
//                context.abort();
//            }
//            log.error("Error in destroy", ex);
//            fail("Error in destroy: " + ex.getMessage());
//        }
    }


    /**
     * Test of find method, of class Item.
     */
    @Test
    public void testItemFind() throws Exception
    {
        // Get ID of item created in init()
        UUID id = it.getID();
        // Make sure we can find it via its ID
        Item found =  itemService.find(context, id);
        assertThat("testItemFind 0", found, notNullValue());
        assertThat("testItemFind 1", found.getID(), equalTo(id));
        assertThat("testItemFind 2", found.getName(), nullValue());
    }

    /**
     * Test of create method, of class Item.
     */
    @Test
    public void testCreate() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Item ADD perms (needed to create an Item)
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.ADD); result = null;
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE); result = null;
        }};
        Item created = createItem();
        assertThat("testCreate 0", created, notNullValue());
        assertThat("testCreate 1", created.getName(), nullValue());
    }

    /**
     * Test of findAll method, of class Item.
     */
    @Test
    public void testFindAll() throws Exception
    {
        Iterator<Item> all = itemService.findAll(context);
        assertThat("testFindAll 0", all, notNullValue());

        boolean added = false;
        while(all.hasNext())
        {
            Item tmp = all.next();
            if(tmp.equals(it))
            {
                added = true;
            }
        }
        assertTrue("testFindAll 1",added);
    }

    /**
     * Test of findBySubmitter method, of class Item.
     */
    @Test
    public void testFindBySubmitter() throws Exception 
    {
        Iterator<Item> all = itemService.findBySubmitter(context, context.getCurrentUser());
        assertThat("testFindBySubmitter 0", all, notNullValue());

        boolean added = false;
        while(all.hasNext())
        {
            Item tmp = all.next();
            if(tmp.equals(it))
            {
                added = true;
            }
        }
        assertTrue("testFindBySubmitter 1",added);

        context.turnOffAuthorisationSystem();
        all = itemService.findBySubmitter(context, ePersonService.create(context));
        context.restoreAuthSystemState();

        assertThat("testFindBySubmitter 2", all, notNullValue());
        assertFalse("testFindBySubmitter 3", all.hasNext());
    }

    /**
     * Test of getID method, of class Item.
     */
    @Override
    @Test
    public void testGetID()
    {
        assertTrue("testGetID 0", it.getID() != null);
    }

    /**
     * Test of getHandle method, of class Item.
     */
    @Override
    @Test
    public void testGetHandle()
    {
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
    public void testIsWithdrawn()
    {
        assertFalse("testIsWithdrawn 0", it.isWithdrawn());
    }

    /**
     * Test of getLastModified method, of class Item.
     */
    @Test
    public void testGetLastModified()
    {
        assertThat("testGetLastModified 0", it.getLastModified(), notNullValue());
        assertTrue("testGetLastModified 1", DateUtils.isSameDay(it.getLastModified(), new Date()));
    }

    /**
     * Test of setArchived method, of class Item.
     */
    @Test
    public void testSetArchived()
    {
        it.setArchived(true);
        assertTrue("testSetArchived 0", it.isArchived());
    }

    /**
     * Test of setOwningCollection method, of class Item.
     */
    @Test
    public void testSetOwningCollection() throws SQLException, AuthorizeException
    {
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
    public void testGetOwningCollection() throws Exception
    {
        assertThat("testGetOwningCollection 0", it.getOwningCollection(), notNullValue());
        assertEquals("testGetOwningCollection 1", it.getOwningCollection(), collection);
    }

    /**
     * Test of getMetadata method, of class Item.
     */
    @Test
    public void testGetMetadata_4args()
    {
        String schema = "dc";
        String element = "contributor";
        String qualifier = "author";
        String lang = Item.ANY;
        List<MetadataValue> dc = itemService.getMetadata(it, schema, element, qualifier, lang);
        assertThat("testGetMetadata_4args 0",dc,notNullValue());
        assertTrue("testGetMetadata_4args 1",dc.size() == 0);
    }

    /**
     * Test of getMetadataByMetadataString method, of class Item.
     */
    @Test
    public void testGetMetadata_String()
    {
        String mdString = "dc.contributor.author";
        List<MetadataValue> dc = itemService.getMetadataByMetadataString(it, mdString);
        assertThat("testGetMetadata_String 0",dc,notNullValue());
        assertTrue("testGetMetadata_String 1",dc.size() == 0);

        mdString = "dc.contributor.*";
        dc = itemService.getMetadataByMetadataString(it, mdString);
        assertThat("testGetMetadata_String 2",dc,notNullValue());
        assertTrue("testGetMetadata_String 3",dc.size() == 0);

        mdString = "dc.contributor";
        dc = itemService.getMetadataByMetadataString(it, mdString);
        assertThat("testGetMetadata_String 4",dc,notNullValue());
        assertTrue("testGetMetadata_String 5",dc.size() == 0);
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
    public void testAddMetadata_5args_1() throws SQLException
    {
        String schema = "dc";
        String element = "contributor";
        String qualifier = "author";
        String lang = Item.ANY;
        String[] values = {"value0","value1"};
        itemService.addMetadata(context, it, schema, element, qualifier, lang, Arrays.asList(values));

        List<MetadataValue> dc = itemService.getMetadata(it, schema, element, qualifier, lang);
        assertThat("testAddMetadata_5args_1 0",dc,notNullValue());
        assertTrue("testAddMetadata_5args_1 1",dc.size() == 2);
        assertThat("testAddMetadata_5args_1 2",dc.get(0).getMetadataField().getMetadataSchema().getName(),equalTo(schema));
        assertThat("testAddMetadata_5args_1 3",dc.get(0).getMetadataField().getElement(),equalTo(element));
        assertThat("testAddMetadata_5args_1 4",dc.get(0).getMetadataField().getQualifier(),equalTo(qualifier));
        assertThat("testAddMetadata_5args_1 5",dc.get(0).getLanguage(),equalTo(lang));
        assertThat("testAddMetadata_5args_1 6",dc.get(0).getValue(),equalTo(values[0]));
        assertThat("testAddMetadata_5args_1 7",dc.get(1).getMetadataField().getMetadataSchema().getName(),equalTo(schema));
        assertThat("testAddMetadata_5args_1 8",dc.get(1).getMetadataField().getElement(),equalTo(element));
        assertThat("testAddMetadata_5args_1 9",dc.get(1).getMetadataField().getQualifier(),equalTo(qualifier));
        assertThat("testAddMetadata_5args_1 10",dc.get(1).getLanguage(),equalTo(lang));
        assertThat("testAddMetadata_5args_1 11",dc.get(1).getValue(),equalTo(values[1]));
    }

    /**
     * Test of addMetadata method, of class Item.
     */
    @Test
    public void testAddMetadata_7args_1_authority() throws InstantiationException, IllegalAccessException, NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException, SQLException {
        //we have enabled an authority control in our test local.cfg to run this test
        //as MetadataAuthorityManager can't be mocked properly

        String schema = "dc";
        String element = "language";
        String qualifier = "iso";
        String lang = Item.ANY;
        List<String> values = Arrays.asList("en_US","en");
        List<String> authorities = Arrays.asList("accepted","uncertain");
        List<Integer> confidences = Arrays.asList(0,0);
        itemService.addMetadata(context, it, schema, element, qualifier, lang, values, authorities, confidences);

        List<MetadataValue> dc = itemService.getMetadata(it, schema, element, qualifier, lang);
        assertThat("testAddMetadata_7args_1 0",dc,notNullValue());
        assertTrue("testAddMetadata_7args_1 1",dc.size() == 2);
        assertThat("testAddMetadata_7args_1 2",dc.get(0).getMetadataField().getMetadataSchema().getName(),equalTo(schema));
        assertThat("testAddMetadata_7args_1 3",dc.get(0).getMetadataField().getElement(),equalTo(element));
        assertThat("testAddMetadata_7args_1 4",dc.get(0).getMetadataField().getQualifier(),equalTo(qualifier));
        assertThat("testAddMetadata_7args_1 5",dc.get(0).getLanguage(),equalTo(lang));
        assertThat("testAddMetadata_7args_1 6",dc.get(0).getValue(),equalTo(values.get(0)));
        assertThat("testAddMetadata_7args_1 7",dc.get(0).getAuthority(),equalTo(authorities.get(0)));
        assertThat("testAddMetadata_7args_1 8",dc.get(0).getConfidence(),equalTo(confidences.get(0)));
        assertThat("testAddMetadata_7args_1 9",dc.get(1).getMetadataField().getMetadataSchema().getName(),equalTo(schema));
        assertThat("testAddMetadata_7args_1 10",dc.get(1).getMetadataField().getElement(),equalTo(element));
        assertThat("testAddMetadata_7args_1 11",dc.get(1).getMetadataField().getQualifier(),equalTo(qualifier));
        assertThat("testAddMetadata_7args_1 12",dc.get(1).getLanguage(),equalTo(lang));
        assertThat("testAddMetadata_7args_1 13",dc.get(1).getValue(),equalTo(values.get(1)));
        assertThat("testAddMetadata_7args_1 14",dc.get(1).getAuthority(),equalTo(authorities.get(1)));
        assertThat("testAddMetadata_7args_1 15",dc.get(1).getConfidence(),equalTo(confidences.get(1)));
    }

     /**
     * Test of addMetadata method, of class Item.
     */
    @Test
    public void testAddMetadata_7args_1_noauthority() throws SQLException
    {
        //by default has no authority

        String schema = "dc";
        String element = "contributor";
        String qualifier = "author";
        String lang = Item.ANY;
        List<String> values = Arrays.asList("value0","value1");
        List<String> authorities = Arrays.asList("auth0","auth2");
        List<Integer> confidences = Arrays.asList(0,0);
        itemService.addMetadata(context, it, schema, element, qualifier, lang, values, authorities, confidences);

        List<MetadataValue> dc = itemService.getMetadata(it, schema, element, qualifier, lang);
        assertThat("testAddMetadata_7args_1 0",dc,notNullValue());
        assertTrue("testAddMetadata_7args_1 1",dc.size() == 2);
        assertThat("testAddMetadata_7args_1 2",dc.get(0).getMetadataField().getMetadataSchema().getName(),equalTo(schema));
        assertThat("testAddMetadata_7args_1 3",dc.get(0).getMetadataField().getElement(),equalTo(element));
        assertThat("testAddMetadata_7args_1 4",dc.get(0).getMetadataField().getQualifier(),equalTo(qualifier));
        assertThat("testAddMetadata_7args_1 5",dc.get(0).getLanguage(),equalTo(lang));
        assertThat("testAddMetadata_7args_1 6",dc.get(0).getValue(),equalTo(values.get(0)));
        assertThat("testAddMetadata_7args_1 7",dc.get(0).getAuthority(),nullValue());
        assertThat("testAddMetadata_7args_1 8",dc.get(0).getConfidence(),equalTo(-1));
        assertThat("testAddMetadata_7args_1 9",dc.get(1).getMetadataField().getMetadataSchema().getName(),equalTo(schema));
        assertThat("testAddMetadata_7args_1 10",dc.get(1).getMetadataField().getElement(),equalTo(element));
        assertThat("testAddMetadata_7args_1 11",dc.get(1).getMetadataField().getQualifier(),equalTo(qualifier));
        assertThat("testAddMetadata_7args_1 12",dc.get(1).getLanguage(),equalTo(lang));
        assertThat("testAddMetadata_7args_1 13",dc.get(1).getValue(),equalTo(values.get(1)));
        assertThat("testAddMetadata_7args_1 14",dc.get(1).getAuthority(),nullValue());
        assertThat("testAddMetadata_7args_1 15",dc.get(1).getConfidence(),equalTo(-1));
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
        List<String> values = Arrays.asList("value0","value1");
        itemService.addMetadata(context, it, schema, element, qualifier, lang, values);

        List<MetadataValue> dc = itemService.getMetadata(it, schema, element, qualifier, lang);
        assertThat("testAddMetadata_5args_2 0",dc,notNullValue());
        assertTrue("testAddMetadata_5args_2 1",dc.size() == 2);
        assertThat("testAddMetadata_5args_2 2",dc.get(0).getMetadataField().getMetadataSchema().getName(),equalTo(schema));
        assertThat("testAddMetadata_5args_2 3",dc.get(0).getMetadataField().getElement(),equalTo(element));
        assertThat("testAddMetadata_5args_2 4",dc.get(0).getMetadataField().getQualifier(),equalTo(qualifier));
        assertThat("testAddMetadata_5args_2 5",dc.get(0).getLanguage(),equalTo(lang));
        assertThat("testAddMetadata_5args_2 6",dc.get(0).getValue(),equalTo(values.get(0)));
        assertThat("testAddMetadata_5args_2 7",dc.get(1).getMetadataField().getMetadataSchema().getName(),equalTo(schema));
        assertThat("testAddMetadata_5args_2 8",dc.get(1).getMetadataField().getElement(),equalTo(element));
        assertThat("testAddMetadata_5args_2 9",dc.get(1).getMetadataField().getQualifier(),equalTo(qualifier));
        assertThat("testAddMetadata_5args_2 10",dc.get(1).getLanguage(),equalTo(lang));
        assertThat("testAddMetadata_5args_2 11",dc.get(1).getValue(),equalTo(values.get(1)));
    }

    /**
     * Test of addMetadata method, of class Item.
     */
    @Test
    public void testAddMetadata_7args_2_authority() throws SQLException
    {
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
        assertThat("testAddMetadata_7args_2 0",dc,notNullValue());
        assertTrue("testAddMetadata_7args_2 1",dc.size() == 1);
        assertThat("testAddMetadata_7args_2 2",dc.get(0).getMetadataField().getMetadataSchema().getName(),equalTo(schema));
        assertThat("testAddMetadata_7args_2 3",dc.get(0).getMetadataField().getElement(),equalTo(element));
        assertThat("testAddMetadata_7args_2 4",dc.get(0).getMetadataField().getQualifier(),equalTo(qualifier));
        assertThat("testAddMetadata_7args_2 5",dc.get(0).getLanguage(),equalTo(lang));
        assertThat("testAddMetadata_7args_2 6",dc.get(0).getValue(),equalTo(values));
        assertThat("testAddMetadata_7args_2 7",dc.get(0).getAuthority(),equalTo(authorities));
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
        String qualifier = "author";
        String lang = Item.ANY;
        String values = "value0";
        String authorities = "auth0";
        int confidences = 0;
        itemService.addMetadata(context, it, schema, element, qualifier, lang, values, authorities, confidences);

        List<MetadataValue> dc = itemService.getMetadata(it, schema, element, qualifier, lang);
        assertThat("testAddMetadata_7args_2 0",dc,notNullValue());
        assertTrue("testAddMetadata_7args_2 1",dc.size() == 1);
        assertThat("testAddMetadata_7args_2 2",dc.get(0).getMetadataField().getMetadataSchema().getName(),equalTo(schema));
        assertThat("testAddMetadata_7args_2 3",dc.get(0).getMetadataField().getElement(),equalTo(element));
        assertThat("testAddMetadata_7args_2 4",dc.get(0).getMetadataField().getQualifier(),equalTo(qualifier));
        assertThat("testAddMetadata_7args_2 5",dc.get(0).getLanguage(),equalTo(lang));
        assertThat("testAddMetadata_7args_2 6",dc.get(0).getValue(),equalTo(values));
        assertThat("testAddMetadata_7args_2 7",dc.get(0).getAuthority(),nullValue());
        assertThat("testAddMetadata_7args_2 8",dc.get(0).getConfidence(),equalTo(-1));
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
        assertThat("testClearMetadata 0",dc,notNullValue());
        assertTrue("testClearMetadata 1", dc.size() == 0);
    }

    /**
     * Test of getSubmitter method, of class Item.
     */
    @Test
    public void testGetSubmitter() throws Exception
    {
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
    public void testSetSubmitter() throws SQLException, AuthorizeException
    {
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
    public void testGetCollections() throws Exception
    {
        assertThat("testGetCollections 0", it.getCollections(), notNullValue());
        assertTrue("testGetCollections 1", it.getCollections().size() == 1);
    }

    /**
     * Test of getCommunities method, of class Item.
     */
    @Test
    public void testGetCommunities() throws Exception 
    {
        assertThat("testGetCommunities 0", itemService.getCommunities(context, it), notNullValue());
        assertTrue("testGetCommunities 1", itemService.getCommunities(context, it).size() == 1);
    }

    /**
     * Test of getBundles method, of class Item.
     */
    @Test
    public void testGetBundles_0args() throws Exception
    {
        assertThat("testGetBundles_0args 0", it.getBundles(), notNullValue());
        assertTrue("testGetBundles_0args 1", it.getBundles().size() == 0);
    }

    /**
     * Test of getBundles method, of class Item.
     */
    @Test
    public void testGetBundles_String() throws Exception
    {
        String name = "name";
        assertThat("testGetBundles_String 0", itemService.getBundles(it, name), notNullValue());
        assertTrue("testGetBundles_String 1", itemService.getBundles(it, name).size() == 0);
    }

    /**
     * Test of createBundle method, of class Item.
     */
    @Test
    public void testCreateBundleAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Item ADD perms
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.ADD); result = null;

        }};

        String name = "bundle";
        Bundle created = bundleService.create(context, it, name);
        assertThat("testCreateBundleAuth 0",created, notNullValue());
        assertThat("testCreateBundleAuth 1",created.getName(), equalTo(name));
        assertThat("testCreateBundleAuth 2", itemService.getBundles(it, name), notNullValue());
        assertTrue("testCreateBundleAuth 3", itemService.getBundles(it, name).size() == 1);
    }

    /**
     * Test of createBundle method, of class Item.
     */
    @Test(expected=SQLException.class)
    public void testCreateBundleNoName() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Item ADD perms
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.ADD); result = null;

        }};

        String name = "";
        Bundle created = bundleService.create(context, it, name);
        fail("Exception expected");
    }

    /**
     * Test of createBundle method, of class Item.
     */
    @Test(expected=SQLException.class)
    public void testCreateBundleNoName2() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Item ADD perms
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.ADD); result = null;

        }};

        String name = null;
        Bundle created = bundleService.create(context, it, name);
        fail("Exception expected");
    }


    /**
     * Test of createBundle method, of class Item.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateBundleNoAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow Item ADD perms
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.ADD); result = new AuthorizeException();

        }};

        String name = "bundle";
        Bundle created = bundleService.create(context, it, name);
        fail("Exception expected");
    }

    /**
     * Test of addBundle method, of class Item.
     */
    @Test
    public void testAddBundleAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Item ADD perms
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.ADD); result = null;

        }};

        String name = "bundle";
        Bundle created = bundleService.create(context, it, name);
        created.setName(context, name);

        assertThat("testAddBundleAuth 0", itemService.getBundles(it, name), notNullValue());
        assertTrue("testAddBundleAuth 1", itemService.getBundles(it, name).size() == 1);
        assertThat("testAddBundleAuth 2", itemService.getBundles(it, name).get(0), equalTo(created));
    }

    /**
     * Test of addBundle method, of class Item.
     */
    @Test(expected=AuthorizeException.class)
    public void testAddBundleNoAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow Item ADD perms
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.ADD); result = new AuthorizeException();

        }};

        String name = "bundle";
        Bundle created = bundleService.create(context, it, name);
        created.setName(context, name);
        
        it.addBundle(created);
        fail("Exception expected");
    }

    /**
     * Test of removeBundle method, of class Item.
     */
    @Test
    public void testRemoveBundleAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Item ADD and REMOVE perms
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.ADD); result = null;
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.REMOVE); result = null;
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.DELETE); result = null;
        }};

        String name = "bundle";
        Bundle created = bundleService.create(context, it, name);
        created.setName(context, name);
        itemService.addBundle(context, it, created);
        
        itemService.removeBundle(context, it, created);
        assertThat("testRemoveBundleAuth 0", itemService.getBundles(it, name), notNullValue());
        assertTrue("testRemoveBundleAuth 1", itemService.getBundles(it, name).size() == 0);
    }

    /**
     * Test of removeBundle method, of class Item.
     */
    @Test(expected=AuthorizeException.class)
    public void testRemoveBundleNoAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Item ADD perms
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.ADD); result = null;
            // Disallow Item REMOVE perms
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.REMOVE); result = new AuthorizeException();
        }};

        String name = "bundle";
        Bundle created = bundleService.create(context, it, name);
        created.setName(context, name);
        it.addBundle(created);

        itemService.removeBundle(context, it, created);
        fail("Exception expected");
    }

    /**
     * Test of createSingleBitstream method, of class Item.
     */
    @Test
    public void testCreateSingleBitstream_InputStream_StringAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Item ADD perms
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.ADD); result = null;
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.WRITE, true); result = null;
        }};

        String name = "new bundle";
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream result = itemService.createSingleBitstream(context, new FileInputStream(f), it, name);
        assertThat("testCreateSingleBitstream_InputStream_StringAuth 0", result, notNullValue());
    }

    /**
     * Test of createSingleBitstream method, of class Item.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateSingleBitstream_InputStream_StringNoAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow Item ADD perms
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.ADD); result = new AuthorizeException();

        }};

        String name = "new bundle";
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream result = itemService.createSingleBitstream(context, new FileInputStream(f), it, name);
        fail("Exception expected");
    }

    /**
     * Test of createSingleBitstream method, of class Item.
     */
    @Test
    public void testCreateSingleBitstream_InputStreamAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Item ADD perms
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.ADD); result = null;
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.WRITE, true); result = null;

        }};

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream result = itemService.createSingleBitstream(context, new FileInputStream(f), it);
        assertThat("testCreateSingleBitstream_InputStreamAuth 0", result, notNullValue());
    }

    /**
     * Test of createSingleBitstream method, of class Item.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateSingleBitstream_InputStreamNoAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow Item ADD perms
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.ADD); result = new AuthorizeException();

        }};

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream result = itemService.createSingleBitstream(context, new FileInputStream(f), it);
        fail("Expected exception");
    }

    /**
     * Test of getNonInternalBitstreams method, of class Item.
     */
    @Test
    public void testGetNonInternalBitstreams() throws Exception
    {
        assertThat("testGetNonInternalBitstreams 0", itemService.getNonInternalBitstreams(context, it), notNullValue());
        assertTrue("testGetNonInternalBitstreams 1", itemService.getNonInternalBitstreams(context, it).size() == 0);
    }

    /**
     * Test of removeDSpaceLicense method, of class Item.
     */
    @Test
    public void testRemoveDSpaceLicenseAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Item ADD and REMOVE perms
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.ADD); result = null;
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.REMOVE); result = null;
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.DELETE); result = null;
        }};

        String name = "LICENSE";
        Bundle created = bundleService.create(context, it, name);
        created.setName(context, name);
//        it.addBundle(created);

        itemService.removeDSpaceLicense(context, it);
        assertThat("testRemoveDSpaceLicenseAuth 0", itemService.getBundles(it, name), notNullValue());
        assertTrue("testRemoveDSpaceLicenseAuth 1", itemService.getBundles(it, name).size() == 0);
    }

    /**
     * Test of removeDSpaceLicense method, of class Item.
     */
    @Test(expected=AuthorizeException.class)
    public void testRemoveDSpaceLicenseNoAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Item ADD perms
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.ADD); result = null;
            // Disallow Item REMOVE perms
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.REMOVE); result = new AuthorizeException();
        }};

        String name = "LICENSE";
        Bundle created = bundleService.create(context, it, name);
        created.setName(context, name);

        itemService.removeDSpaceLicense(context, it);
        fail("Exception expected");
    }

    /**
     * Test of removeLicenses method, of class Item.
     */
    @Test
    public void testRemoveLicensesAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Item ADD and REMOVE perms
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.ADD); result = null;
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.REMOVE); result = null;
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.WRITE); result = null;
            authorizeService.authorizeAction(context, (Bitstream) any,
                    Constants.DELETE); result = null;

        }};

        String name = "LICENSE";
        Bundle created = bundleService.create(context, it, name);
        created.setName(context, name);

        String bsname = "License";
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream result = itemService.createSingleBitstream(context, new FileInputStream(f), it, bsname);
        bitstreamService.setFormat(context, result, bitstreamFormatService.findByShortDescription(context, bsname));
        bundleService.addBitstream(context, created, result);



        itemService.removeLicenses(context, it);
        assertThat("testRemoveLicensesAuth 0", itemService.getBundles(it, name), notNullValue());
        assertTrue("testRemoveLicensesAuth 1", itemService.getBundles(it, name).size() == 0);
    }

    /**
     * Test of removeLicenses method, of class Item.
     */
    @Test(expected=AuthorizeException.class)
    public void testRemoveLicensesNoAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {
            {
                authorizeService.authorizeAction((Context) any, (Item) any,
                        Constants.ADD); result = null;
                authorizeService.authorizeAction((Context) any, (Item) any,
                        Constants.REMOVE); result = new AuthorizeException();
            }
        };

        String name = "LICENSE";
        Bundle created = bundleService.create(context, it, name);
        created.setName(context, name);

        String bsname = "License";
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream result = itemService.createSingleBitstream(context, new FileInputStream(f), it, bsname);
        bitstreamService.setFormat(context, result, bitstreamFormatService.findByShortDescription(context, bsname));
        bundleService.addBitstream(context, created, result);

        itemService.removeLicenses(context, it);
        fail("Exception expected");
    }

    /**
     * Test of update method, of class Item.
     */
    @Test
    public void testUpdateAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Item WRITE perms
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.WRITE); result = null;

        }};

        //TOOD: how to test?
        itemService.update(context, it);
    }

    /**
     * Test of update method, of class Item.
     */
    @Test
    public void testUpdateAuth2() throws Exception
    {
        // Test permission inheritence
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow Item WRITE perms
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.WRITE); result = new AuthorizeException();
            // Allow parent Community WRITE and ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = true;
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = true;
            // Disallow parent Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = new AuthorizeException();

        }};

        context.turnOffAuthorisationSystem();
        Collection c = createCollection();
        it.setOwningCollection(c);
        context.restoreAuthSystemState();

        //TOOD: how to test?
        itemService.update(context, it);
    }

    /**
     * Test of update method, of class Item.
     */
    @Test(expected=AuthorizeException.class)
    public void testUpdateNoAuth() throws Exception
    {
        // Test permission inheritence
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow Item WRITE perms
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.WRITE); result = new AuthorizeException();
            // Disallow parent Community WRITE or ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, anyBoolean); result = false;
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, anyBoolean); result = false;
            // Disallow parent Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, anyBoolean); result = new AuthorizeException();
        }};

        context.turnOffAuthorisationSystem();
        Collection c = createCollection();
        it.setOwningCollection(c);
        context.restoreAuthSystemState();

        //TOOD: how to test?
        itemService.update(context, it);
    }

    /**
     * Test of withdraw method, of class Item.
     */
    @Test
    public void testWithdrawAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeUtil.class)
        {{
            // Allow Item withdraw permissions
            AuthorizeUtil.authorizeWithdrawItem((Context) any, (Item) any);
                result = null;
        }};

        new NonStrictExpectations(authorizeService.getClass())
        {{
                authorizeService.authorizeAction((Context) any, (Item) any,
                            Constants.WRITE); result = null;

        }};

        itemService.withdraw(context, it);
        assertTrue("testWithdrawAuth 0", it.isWithdrawn());
    }

    /**
     * Test of withdraw method, of class Item.
     */
    @Test(expected=AuthorizeException.class)
    public void testWithdrawNoAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeUtil.class)
        {{
            // Disallow Item withdraw permissions
            AuthorizeUtil.authorizeWithdrawItem((Context) any, (Item) any);
                result = new AuthorizeException();

        }};

        itemService.withdraw(context, it);
        fail("Exception expected");
    }

    /**
     * Test of reinstate method, of class Item.
     */
    @Test
    public void testReinstateAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeUtil.class)
        {{
            AuthorizeUtil.authorizeWithdrawItem((Context) any, (Item) any);
                result = null;
            AuthorizeUtil.authorizeReinstateItem((Context) any, (Item) any);
                result = null;

        }};
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Item withdraw and reinstate permissions
            authorizeService.authorizeAction((Context) any, (Item) any,
                Constants.WRITE); result = null;
        }};

        itemService.withdraw(context, it);
        itemService.reinstate(context, it);
        assertFalse("testReinstate 0",it.isWithdrawn());
    }

    /**
     * Test of reinstate method, of class Item.
     */
    @Test(expected=AuthorizeException.class)
    public void testReinstateNoAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeUtil.class)
        {{
            // Allow Item withdraw permissions
            AuthorizeUtil.authorizeWithdrawItem((Context) any, (Item) any);
                result = null;
            // Disallow Item reinstate permissions
            AuthorizeUtil.authorizeReinstateItem((Context) any, (Item) any);
                result = new AuthorizeException();
        }};
        new NonStrictExpectations(authorizeService.getClass())
        {{
            authorizeService.authorizeAction((Context) any, (Item) any,
                Constants.WRITE); result = null;
        }};

        itemService.withdraw(context, it);
        itemService.reinstate(context, it);
        fail("Exception expected");
    }

    /**
     * Test of delete method, of class Item.
     */
    @Test
    public void testDeleteAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Item REMOVE perms
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.REMOVE, true); result = null;
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.DELETE, true); result = null;
            authorizeService.authorizeAction((Context) any, (Item) any,
                Constants.WRITE); result = null;
        }};

        UUID id = it.getID();
        itemService.delete(context,  it);
        Item found = itemService.find(context, id);
        assertThat("testDeleteAuth 0",found,nullValue());
    }

    /**
     * Test of delete method, of class Item.
     */
    @Test(expected=AuthorizeException.class)
    public void testDeleteNoAuth() throws Exception
    {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow Item REMOVE perms
            authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.REMOVE); result = new AuthorizeException();
        }};
        
        itemService.delete(context, it);
        fail("Exception expected");
    }

    /**
     * Test of equals method, of class Item.
     */
    @Test
    @SuppressWarnings("ObjectEqualsNull")
    public void testEquals() throws SQLException, AuthorizeException, IOException, IllegalAccessException {
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Item ADD perms (needed to create an Item)
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.ADD); result = null;
            authorizeService.authorizeAction((Context) any, (Item) any,
                Constants.WRITE); result = null;
            authorizeService.authorizeAction((Context) any, (Item) any,
                Constants.REMOVE); result = null;
            authorizeService.authorizeAction((Context) any, (Item) any,
                Constants.DELETE); result = null;

        }};

        assertFalse("testEquals 0", it.equals(null));
        Item item = createItem();
        try {
            assertFalse("testEquals 1",it.equals(item));
            assertTrue("testEquals 2", it.equals(it));
        } finally {
            itemService.delete(context, item);
        }
    }

    /**
     * Test of isOwningCollection method, of class Item.
     */
    @Test
    public void testIsOwningCollection() throws SQLException, AuthorizeException
    {
        context.turnOffAuthorisationSystem();
        Collection c = createCollection();
        context.restoreAuthSystemState();
        
        boolean result = itemService.isOwningCollection(it, c);
        assertFalse("testIsOwningCollection 0",result);
    }

    /**
     * Test of getType method, of class Item.
     */
    @Override
    @Test
    public void testGetType()
    {
        assertThat("testGetType 0", it.getType(), equalTo(Constants.ITEM));
    }

    /**
     * Test of replaceAllItemPolicies method, of class Item.
     */
    @Test
    public void testReplaceAllItemPolicies() throws Exception
    {
        List<ResourcePolicy> newpolicies = new ArrayList<ResourcePolicy>();
        ResourcePolicy pol1 = resourcePolicyService.create(context);
        newpolicies.add(pol1);
        new NonStrictExpectations(authorizeService.getClass())
        {
            {
                authorizeService.authorizeAction((Context) any, (Item) any,
                        Constants.WRITE); result = null;
            }
        };
        itemService.replaceAllItemPolicies(context, it, newpolicies);

        List<ResourcePolicy> retrieved = authorizeService.getPolicies(context, it);
        assertThat("testReplaceAllItemPolicies 0",retrieved, notNullValue());
        assertThat("testReplaceAllItemPolicies 1", retrieved.size(), equalTo(newpolicies.size()));
    }

    /**
     * Test of replaceAllBitstreamPolicies method, of class Item.
     */
    @Test
    public void testReplaceAllBitstreamPolicies() throws Exception
    {
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
        for(Bundle b: bundles)
        {
            retrieved.addAll(authorizeService.getPolicies(context, b));
            retrieved.addAll(bundleService.getBitstreamPolicies(context, b));
        }
        assertFalse("testReplaceAllBitstreamPolicies 0",retrieved.isEmpty());

        boolean equals = true;
        for(int i=0; i < newpolicies.size() && equals; i++)
        {
            if(!newpolicies.contains(retrieved.get(i)))
            {
                equals = false;
            }
        }
        assertTrue("testReplaceAllBitstreamPolicies 1", equals);    }

    /**
     * Test of removeGroupPolicies method, of class Item.
     */
    @Test
    public void testRemoveGroupPolicies() throws Exception
    {
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
        assertThat("testRemoveGroupPolicies 0",retrieved, notNullValue());
        assertTrue("testRemoveGroupPolicies 1", retrieved.isEmpty());
    }

    /**
     * Test of inheritCollectionDefaultPolicies method, of class Item.
     */
    @Test
    public void testInheritCollectionDefaultPolicies() throws Exception 
    {
        context.turnOffAuthorisationSystem();

        Collection c = createCollection();

        List<ResourcePolicy> defaultCollectionPolicies = authorizeService.getPoliciesActionFilter(context, c,
                Constants.DEFAULT_BITSTREAM_READ);
        List<ResourcePolicy> newPolicies = new ArrayList<ResourcePolicy>();
        for(ResourcePolicy collRp : defaultCollectionPolicies)
        {
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

        new NonStrictExpectations(authorizeService.getClass())
        {
            {
                authorizeService.authorizeAction((Context) any, (Item) any,
                        Constants.WRITE, true); result = null;
            }
        };

        itemService.inheritCollectionDefaultPolicies(context, it, c);

        //test item policies
        List<ResourcePolicy> retrieved = authorizeService.getPolicies(context, it);
        boolean equals = true;
        for(int i=0; i < retrieved.size() && equals; i++)
        {
            if(!newPolicies.contains(retrieved.get(i)))
            {
                equals = false;
            }
        }
        assertTrue("testInheritCollectionDefaultPolicies 0", equals);

        retrieved = new ArrayList<ResourcePolicy>();
        List<Bundle> bundles = it.getBundles();
        for(Bundle b: bundles)
        {
            retrieved.addAll(authorizeService.getPolicies(context, b));
            retrieved.addAll(bundleService.getBitstreamPolicies(context, b));
        }
        assertFalse("testInheritCollectionDefaultPolicies 1",retrieved.isEmpty());

        equals = true;
        for(int i=0; i < newPolicies.size() && equals; i++)
        {
            if(!newPolicies.contains(retrieved.get(i)))
            {
                equals = false;
            }
        }
        assertTrue("testInheritCollectionDefaultPolicies 2", equals);
    }

    /**
     * Test of move method, of class Item.
     */
    @Test
    public void testMove() throws Exception
    {
        //we disable the permission testing as it's shared with other methods where it's already tested (can edit)
        context.turnOffAuthorisationSystem();
        Collection from = createCollection();
        Collection to = createCollection();
        it.setOwningCollection(from);

        itemService.move(context, it, from, to);
        context.restoreAuthSystemState();
        assertThat("testMove 0",it.getOwningCollection(), notNullValue());
        assertThat("testMove 1", it.getOwningCollection(), equalTo(to));
    }

    /**
     * Test of hasUploadedFiles method, of class Item.
     */
    @Test
    public void testHasUploadedFiles() throws Exception
    {
        assertFalse("testHasUploadedFiles 0",itemService.hasUploadedFiles(it));
    }

    /**
     * Test of getCollectionsNotLinked method, of class Item.
     */
    @Test
    public void testGetCollectionsNotLinked() throws Exception
    {
        List<Collection> result = itemService.getCollectionsNotLinked(context, it);
        boolean isin = false;
        for(Collection c: result)
        {
            Iterator<Item> iit = itemService.findByCollection(context, c);
            while(iit.hasNext())
            {
                if(iit.next().getID().equals(it.getID()))
                {
                    isin = true;
                }
            }
        }
        assertFalse("testGetCollectionsNotLinked 0",isin);
    }

    /**
     * Test of canEdit method, of class Item.
     */
    @Test
    public void testCanEditBooleanAuth() throws Exception
    {
        // Test Inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Allow Item WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Item) any,
                    Constants.WRITE); result = true;
            // Allow parent Community WRITE and ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = true;
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = true;
            // Allow parent Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = null;
        }};

        assertTrue("testCanEditBooleanAuth 0", itemService.canEdit(context, it));
    }

    /**
     * Test of canEdit method, of class Item.
     */
    @Test
    public void testCanEditBooleanAuth2() throws Exception
    {
        // Test Inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow Item WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Item) any,
                    Constants.WRITE); result = false;
            // Allow parent Community WRITE and ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = true;
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, true); result = true;
            // Allow parent Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, false); result = null;
        }};

        assertTrue("testCanEditBooleanAuth2 0", itemService.canEdit(context, it));
    }

    /**
     * Test of canEdit method, of class Item.
     */
    @Test
    public void testCanEditBooleanAuth3() throws Exception
    {
        // Test Inheritance of permissions for owning collection
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow Item WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Item) any,
                    Constants.WRITE); result = false;
            // Allow parent Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, false); result = null;
        }};

        // Create a new Collection and assign it as the owner
        context.turnOffAuthorisationSystem();
        Collection c = createCollection();
        it.setOwningCollection(c);
        context.restoreAuthSystemState();

        // Ensure person with WRITE perms on the Collection can edit item
        assertTrue("testCanEditBooleanAuth3 0", itemService.canEdit(context, it));
    }

    /**
     * Test of canEdit method, of class Item.
     */
    @Test
    public void testCanEditBooleanAuth4() throws Exception
    {
        // Test Inheritance of permissions for Community Admins
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow Item WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Item) any,
                    Constants.WRITE); result = false;
            // Allow parent Community WRITE and ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, true); result = true;
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, false); result = true;
            // Disallow parent Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, true); result = new AuthorizeException();
        }};

        // Ensure person with WRITE perms on the Collection can edit item
        assertTrue("testCanEditBooleanAuth4 0", itemService.canEdit(context, it));
    }

    /**
     * Test of canEdit method, of class Item.
     */
    @Test
    public void testCanEditBooleanAuth5() throws Exception
    {
        // Test Inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow Item WRITE perms
        	authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.WRITE); result = new AuthorizeException();
            // Allow Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,anyBoolean); result = null;
        }};

        collectionService.createTemplateItem(context, collection);
        collectionService.update(context, collection);
        assertTrue("testCanEditBooleanNoAuth5 0", itemService.canEdit(context, collection.getTemplateItem()));
    }

    /**
     * Test of canEdit method, of class Item.
     */
    @Test
    public void testCanEditBooleanNoAuth() throws Exception
    {
        // Test Inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow Item WRITE perms
            authorizeService.authorizeActionBoolean((Context) any, (Item) any,
                    Constants.WRITE); result = false;
            // Disallow parent Community WRITE and ADD perms
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE, anyBoolean); result = false;
            authorizeService.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD, anyBoolean); result = false;
            // Disallow parent Collection WRITE perms
            authorizeService.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, anyBoolean); result = new AuthorizeException();
        }};

        context.turnOffAuthorisationSystem();
        Collection c = createCollection();
        it.setOwningCollection(c);
        context.restoreAuthSystemState();

        assertFalse("testCanEditBooleanNoAuth 0", itemService.canEdit(context, it));
    }

    /**
     * Test of canEdit method, of class Item.
     */
    @Test
    public void testCanEditBooleanNoAuth2() throws Exception
    {
        context.turnOffAuthorisationSystem();
        WorkspaceItem wi = workspaceItemService.create(context, collection, true);
        context.restoreAuthSystemState();
        // Test Inheritance of permissions
        new NonStrictExpectations(authorizeService.getClass())
        {{
            // Disallow Item WRITE perms
        	authorizeService.authorizeAction((Context) any, (Item) any,
                    Constants.WRITE, anyBoolean); result = new AuthorizeException();
        }};
        assertFalse("testCanEditBooleanNoAuth2 0", itemService.canEdit(context, wi.getItem()));
    }

    /**
     * Test of isInProgressSubmission method, of class Item.
     * @throws AuthorizeException 
     * @throws SQLException 
     * @throws IOException 
     * 
     */
    @Test
    public void testIsInProgressSubmission() throws SQLException, AuthorizeException, IOException
    {
    	context.turnOffAuthorisationSystem();
    	Collection c = createCollection();
        WorkspaceItem wi = workspaceItemService.create(context, c, true);
    	context.restoreAuthSystemState();
        assertTrue("testIsInProgressSubmission 0", itemService.isInProgressSubmission(context, wi.getItem()));
    }
    
    /**
     * Test of isInProgressSubmission method, of class Item.
     * @throws AuthorizeException 
     * @throws SQLException 
     * @throws IOException 
     * 
     */
    @Test
    public void testIsInProgressSubmissionFalse() throws SQLException, AuthorizeException, IOException
    {
    	context.turnOffAuthorisationSystem();
    	Collection c = createCollection();
        WorkspaceItem wi = workspaceItemService.create(context, c, true);
        Item item = installItemService.installItem(context, wi);
    	context.restoreAuthSystemState();
        assertFalse("testIsInProgressSubmissionFalse 0", itemService.isInProgressSubmission(context, item));
    }

    /**
     * Test of isInProgressSubmission method, of class Item.
     * @throws AuthorizeException 
     * @throws SQLException 
     * @throws IOException 
     * 
     */
    @Test
    public void testIsInProgressSubmissionFalse2() throws SQLException, AuthorizeException, IOException
    {
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
    public void testGetName()
    {
        assertThat("testGetName 0",it.getName(),nullValue());
    }

    /**
     * Test of findByMetadataField method, of class Item.
     */
    @Test
    public void testFindByMetadataField() throws Exception
    {
        String schema = "dc";
        String element = "contributor";
        String qualifier = "author";
        String value = "value";

        Iterator<Item> result = itemService.findByMetadataField(context, schema, element, qualifier, value);
        assertThat("testFindByMetadataField 0",result,notNullValue());
        assertFalse("testFindByMetadataField 1",result.hasNext());

        itemService.addMetadata(context, it, schema,element, qualifier, Item.ANY, value);
        new NonStrictExpectations(authorizeService.getClass())
        {
            {
                authorizeService.authorizeAction((Context) any, (Item) any,
                        Constants.WRITE); result = null;
            }
        };
        itemService.update(context, it);

        result = itemService.findByMetadataField(context, schema, element, qualifier, value);
        assertThat("testFindByMetadataField 3",result,notNullValue());        
        assertTrue("testFindByMetadataField 4",result.hasNext());
        assertTrue("testFindByMetadataField 5",result.next().equals(it));
    }

    /**
     * Test of getAdminObject method, of class Item.
     */
    @Test
    @Override
    public void testGetAdminObject() throws SQLException
    {
        //default community has no admin object
        assertThat("testGetAdminObject 0", (Item) itemService.getAdminObject(context, it, Constants.REMOVE), equalTo(it));
        assertThat("testGetAdminObject 1", (Item) itemService.getAdminObject(context, it, Constants.ADD), equalTo(it));
        assertThat("testGetAdminObject 2", (Collection) itemService.getAdminObject(context, it, Constants.DELETE), equalTo(collection));
        assertThat("testGetAdminObject 3", (Item) itemService.getAdminObject(context, it, Constants.ADMIN), equalTo(it));
    }

    /**
     * Test of getParentObject method, of class Item.
     */
    @Test
    @Override
    public void testGetParentObject() throws SQLException
    {
        try
        {
            //default has no parent
            assertThat("testGetParentObject 0", itemService.getParentObject(context, it), notNullValue());

            context.turnOffAuthorisationSystem();
            Collection parent = createCollection();
            it.setOwningCollection(parent);
            context.restoreAuthSystemState();
            assertThat("testGetParentObject 1", itemService.getParentObject(context, it), notNullValue());
            assertThat("testGetParentObject 2", (Collection) itemService.getParentObject(context, it), equalTo(parent));
        }
        catch(AuthorizeException ex)
        {
            fail("Authorize exception catched");
        }
    }

    /**
     * Test of findByAuthorityValue method, of class Item.
     */
    @Test
    public void testFindByAuthorityValue() throws Exception
    {
        String schema = "dc";
        String element = "language";
        String qualifier = "iso";
        String value = "en";
        String authority = "accepted";
        int confidence = 0;

        Iterator<Item> result = itemService.findByAuthorityValue(context, schema, element, qualifier, value);
        assertThat("testFindByAuthorityValue 0", result, notNullValue());
        assertFalse("testFindByAuthorityValue 1", result.hasNext());

        itemService.addMetadata(context, it, schema, element, qualifier, Item.ANY, value, authority, confidence);
        //Ensure that the current user can update the item
        new NonStrictExpectations(authorizeService.getClass())
        {
            {
                authorizeService.authorizeAction((Context) any, (Item) any,
                        Constants.WRITE); result = null;
            }
        };
        itemService.update(context, it);

        result = itemService.findByAuthorityValue(context, schema, element, qualifier, authority);
        assertThat("testFindByAuthorityValue 3",result,notNullValue());
        assertTrue("testFindByAuthorityValue 4",result.hasNext());
        assertThat("testFindByAuthorityValue 5",result.next(),equalTo(it));
    }

    protected Collection createCollection() throws SQLException, AuthorizeException {
        return collectionService.create(context, owningCommunity);
    }

    protected Item createItem() throws SQLException, IOException, AuthorizeException, IllegalAccessException {
        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
        return installItemService.installItem(context, workspaceItem);
    }

}
