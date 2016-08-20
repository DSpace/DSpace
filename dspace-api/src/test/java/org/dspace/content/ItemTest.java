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
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.workflow.WorkflowItem;
import org.junit.*;
import static org.junit.Assert.* ;
import static org.hamcrest.CoreMatchers.*;
import mockit.*;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeManager;
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
            this.it = Item.create(context);
            it.setArchived(true);
            it.setSubmitter(context.getCurrentUser());
            it.update();
            this.dspaceObject = it;
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
        it = null;
        super.destroy();
    }


    /**
     * Test of find method, of class Item.
     */
    @Test
    public void testItemFind() throws Exception
    {
        // Get ID of item created in init()
        int id = this.it.getID();
        // Make sure we can find it via its ID
        Item found =  Item.find(context, id);
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
        Item created = Item.create(context);
        assertThat("testCreate 0", created, notNullValue());
        assertThat("testCreate 1", created.getName(), nullValue());
    }

    /**
     * Test of findAll method, of class Item.
     */
    @Test
    public void testFindAll() throws Exception
    {
        ItemIterator all = Item.findAll(context);
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
        ItemIterator all = Item.findBySubmitter(context, context.getCurrentUser());
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
        all = Item.findBySubmitter(context, EPerson.create(context));
        context.restoreAuthSystemState();

        assertThat("testFindBySubmitter 2", all, notNullValue());
        assertFalse("testFindBySubmitter 3", all.hasNext());
        assertThat("testFindBySubmitter 4", all.next(), nullValue());
    }

    /**
     * Test of getID method, of class Item.
     */
    @Test
    public void testGetID()
    {
        assertTrue("testGetID 0", it.getID() >= 1);
    }

    /**
     * Test of getHandle method, of class Item.
     */
    @Test
    public void testGetHandle()
    {
        //default instance has a random handle
        assertThat("testGetHandle 0", it.getHandle(), nullValue());
    }

    /**
     * Test of isArchived method, of class Item.
     */
    @Test
    public void testIsArchived() throws SQLException, AuthorizeException
    {
        //we are archiving items in the test by default so other tests run
        assertTrue("testIsArchived 0", it.isArchived());

        //false by default
        context.turnOffAuthorisationSystem();
        Item tmp = Item.create(context);
        context.restoreAuthSystemState();
        assertFalse("testIsArchived 1", tmp.isArchived());        
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
        Collection c = Collection.create(context);
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
        assertThat("testGetOwningCollection 0", it.getOwningCollection(), nullValue());
    }

    /**
     * Test of getDC method, of class Item.
     */
    @Test
    public void testGetDC()
    {
        String element = "contributor";
        String qualifier = "author";
        String lang = Item.ANY;
        Metadatum[] dc = it.getDC(element, qualifier, lang);
        assertThat("testGetDC 0",dc,notNullValue());
        assertTrue("testGetDC 1",dc.length == 0);
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
        Metadatum[] dc = it.getMetadata(schema, element, qualifier, lang);
        assertThat("testGetMetadata_4args 0",dc,notNullValue());
        assertTrue("testGetMetadata_4args 1",dc.length == 0);
    }

    /**
     * Test of getMetadataByMetadataString method, of class Item.
     */
    @Test
    public void testGetMetadata_String()
    {
        String mdString = "dc.contributor.author";
        Metadatum[] dc = it.getMetadataByMetadataString(mdString);
        assertThat("testGetMetadata_String 0",dc,notNullValue());
        assertTrue("testGetMetadata_String 1",dc.length == 0);

        mdString = "dc.contributor.*";
        dc = it.getMetadataByMetadataString(mdString);
        assertThat("testGetMetadata_String 2",dc,notNullValue());
        assertTrue("testGetMetadata_String 3",dc.length == 0);

        mdString = "dc.contributor";
        dc = it.getMetadataByMetadataString(mdString);
        assertThat("testGetMetadata_String 4",dc,notNullValue());
        assertTrue("testGetMetadata_String 5",dc.length == 0);
    }

    /**
     * A test for DS-806: Item.match() incorrect logic for schema testing
     */
    @Test
    public void testDS806()
    {
        // Set the item to have two pieces of metadata for dc.type and dc2.type
        String dcType = "DC-TYPE";
        String testType = "TEST-TYPE";
        it.addMetadata("dc", "type", null, null, dcType);
        it.addMetadata("test", "type", null, null, testType);

        // Check that only one is returned when we ask for all dc.type values
        Metadatum[] values = it.getMetadata("dc", "type", null, null);
        assertTrue("Return results", values.length == 1);
    }

    /**
     * Test of addDC method, of class Item.
     */
    @Test
    public void testAddDC_4args_1()
    {
        String element = "contributor";
        String qualifier = "author";
        String lang = Item.ANY;
        String[] values = {"value0","value1"};
        it.addDC(element, qualifier, lang, values);

        Metadatum[] dc = it.getDC(element, qualifier, lang);
        assertThat("testAddDC_4args_1 0",dc,notNullValue());
        assertTrue("testAddDC_4args_1 1",dc.length == 2);
        assertThat("testAddDC_4args_1 2",dc[0].element,equalTo(element));
        assertThat("testAddDC_4args_1 3",dc[0].qualifier,equalTo(qualifier));
        assertThat("testAddDC_4args_1 4",dc[0].language,equalTo(lang));
        assertThat("testAddDC_4args_1 5",dc[0].value,equalTo(values[0]));
        assertThat("testAddDC_4args_1 6",dc[1].element,equalTo(element));
        assertThat("testAddDC_4args_1 7",dc[1].qualifier,equalTo(qualifier));
        assertThat("testAddDC_4args_1 8",dc[1].language,equalTo(lang));
        assertThat("testAddDC_4args_1 9",dc[1].value,equalTo(values[1]));
    }

    /**
     * Test of addDC method, of class Item.
     */
    @Test
    public void testAddDC_4args_2()
    {
        String element = "contributor";
        String qualifier = "author";
        String lang = Item.ANY;
        String value = "value";
        it.addDC(element, qualifier, lang, value);

        Metadatum[] dc = it.getDC(element, qualifier, lang);
        assertThat("testAddDC_4args_2 0",dc,notNullValue());
        assertTrue("testAddDC_4args_2 1",dc.length == 1);
        assertThat("testAddDC_4args_2 2",dc[0].element,equalTo(element));
        assertThat("testAddDC_4args_2 3",dc[0].qualifier,equalTo(qualifier));
        assertThat("testAddDC_4args_2 4",dc[0].language,equalTo(lang));
        assertThat("testAddDC_4args_2 5",dc[0].value,equalTo(value));
    }

    /**
     * Test of addMetadata method, of class Item.
     */
    @Test
    public void testAddMetadata_5args_1()
    {
        String schema = "dc";
        String element = "contributor";
        String qualifier = "author";
        String lang = Item.ANY;
        String[] values = {"value0","value1"};
        it.addMetadata(schema, element, qualifier, lang, values);

        Metadatum[] dc = it.getMetadata(schema, element, qualifier, lang);
        assertThat("testAddMetadata_5args_1 0",dc,notNullValue());
        assertTrue("testAddMetadata_5args_1 1",dc.length == 2);
        assertThat("testAddMetadata_5args_1 2",dc[0].schema,equalTo(schema));
        assertThat("testAddMetadata_5args_1 3",dc[0].element,equalTo(element));
        assertThat("testAddMetadata_5args_1 4",dc[0].qualifier,equalTo(qualifier));
        assertThat("testAddMetadata_5args_1 5",dc[0].language,equalTo(lang));
        assertThat("testAddMetadata_5args_1 6",dc[0].value,equalTo(values[0]));
        assertThat("testAddMetadata_5args_1 7",dc[1].schema,equalTo(schema));
        assertThat("testAddMetadata_5args_1 8",dc[1].element,equalTo(element));
        assertThat("testAddMetadata_5args_1 9",dc[1].qualifier,equalTo(qualifier));
        assertThat("testAddMetadata_5args_1 10",dc[1].language,equalTo(lang));
        assertThat("testAddMetadata_5args_1 11",dc[1].value,equalTo(values[1]));
    }

    /**
     * Test of addMetadata method, of class Item.
     */
    @Test
    public void testAddMetadata_7args_1_authority() throws InstantiationException, IllegalAccessException, NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException
    {
        //we have enabled an authority control in dspace-test.cfg to run this test
        //as MetadataAuthorityManager can't be mocked properly

        String schema = "dc";
        String element = "language";
        String qualifier = "iso";
        String lang = Item.ANY;
        String[] values = {"en_US","en"};
        String[] authorities = {"accepted","uncertain"};
        int[] confidences = {0,0};
        it.addMetadata(schema, element, qualifier, lang, values, authorities, confidences);

        Metadatum[] dc = it.getMetadata(schema, element, qualifier, lang);
        assertThat("testAddMetadata_7args_1 0",dc,notNullValue());
        assertTrue("testAddMetadata_7args_1 1",dc.length == 2);
        assertThat("testAddMetadata_7args_1 2",dc[0].schema,equalTo(schema));
        assertThat("testAddMetadata_7args_1 3",dc[0].element,equalTo(element));
        assertThat("testAddMetadata_7args_1 4",dc[0].qualifier,equalTo(qualifier));
        assertThat("testAddMetadata_7args_1 5",dc[0].language,equalTo(lang));
        assertThat("testAddMetadata_7args_1 6",dc[0].value,equalTo(values[0]));
        assertThat("testAddMetadata_7args_1 7",dc[0].authority,equalTo(authorities[0]));
        assertThat("testAddMetadata_7args_1 8",dc[0].confidence,equalTo(confidences[0]));
        assertThat("testAddMetadata_7args_1 9",dc[1].schema,equalTo(schema));
        assertThat("testAddMetadata_7args_1 10",dc[1].element,equalTo(element));
        assertThat("testAddMetadata_7args_1 11",dc[1].qualifier,equalTo(qualifier));
        assertThat("testAddMetadata_7args_1 12",dc[1].language,equalTo(lang));
        assertThat("testAddMetadata_7args_1 13",dc[1].value,equalTo(values[1]));
        assertThat("testAddMetadata_7args_1 14",dc[1].authority,equalTo(authorities[1]));
        assertThat("testAddMetadata_7args_1 15",dc[1].confidence,equalTo(confidences[1]));
    }

     /**
     * Test of addMetadata method, of class Item.
     */
    @Test
    public void testAddMetadata_7args_1_noauthority()
    {
        //by default has no authority

        String schema = "dc";
        String element = "contributor";
        String qualifier = "author";
        String lang = Item.ANY;
        String[] values = {"value0","value1"};
        String[] authorities = {"auth0","auth2"};
        int[] confidences = {0,0};
        it.addMetadata(schema, element, qualifier, lang, values, authorities, confidences);

        Metadatum[] dc = it.getMetadata(schema, element, qualifier, lang);
        assertThat("testAddMetadata_7args_1 0",dc,notNullValue());
        assertTrue("testAddMetadata_7args_1 1",dc.length == 2);
        assertThat("testAddMetadata_7args_1 2",dc[0].schema,equalTo(schema));
        assertThat("testAddMetadata_7args_1 3",dc[0].element,equalTo(element));
        assertThat("testAddMetadata_7args_1 4",dc[0].qualifier,equalTo(qualifier));
        assertThat("testAddMetadata_7args_1 5",dc[0].language,equalTo(lang));
        assertThat("testAddMetadata_7args_1 6",dc[0].value,equalTo(values[0]));
        assertThat("testAddMetadata_7args_1 7",dc[0].authority,nullValue());
        assertThat("testAddMetadata_7args_1 8",dc[0].confidence,equalTo(-1));
        assertThat("testAddMetadata_7args_1 9",dc[1].schema,equalTo(schema));
        assertThat("testAddMetadata_7args_1 10",dc[1].element,equalTo(element));
        assertThat("testAddMetadata_7args_1 11",dc[1].qualifier,equalTo(qualifier));
        assertThat("testAddMetadata_7args_1 12",dc[1].language,equalTo(lang));
        assertThat("testAddMetadata_7args_1 13",dc[1].value,equalTo(values[1]));
        assertThat("testAddMetadata_7args_1 14",dc[1].authority,nullValue());
        assertThat("testAddMetadata_7args_1 15",dc[1].confidence,equalTo(-1));
    }

    /**
     * Test of addMetadata method, of class Item.
     */
    @Test
    public void testAddMetadata_5args_2() 
    {
         String schema = "dc";
        String element = "contributor";
        String qualifier = "author";
        String lang = Item.ANY;
        String[] values = {"value0","value1"};
        it.addMetadata(schema, element, qualifier, lang, values);

        Metadatum[] dc = it.getMetadata(schema, element, qualifier, lang);
        assertThat("testAddMetadata_5args_2 0",dc,notNullValue());
        assertTrue("testAddMetadata_5args_2 1",dc.length == 2);
        assertThat("testAddMetadata_5args_2 2",dc[0].schema,equalTo(schema));
        assertThat("testAddMetadata_5args_2 3",dc[0].element,equalTo(element));
        assertThat("testAddMetadata_5args_2 4",dc[0].qualifier,equalTo(qualifier));
        assertThat("testAddMetadata_5args_2 5",dc[0].language,equalTo(lang));
        assertThat("testAddMetadata_5args_2 6",dc[0].value,equalTo(values[0]));
        assertThat("testAddMetadata_5args_2 7",dc[1].schema,equalTo(schema));
        assertThat("testAddMetadata_5args_2 8",dc[1].element,equalTo(element));
        assertThat("testAddMetadata_5args_2 9",dc[1].qualifier,equalTo(qualifier));
        assertThat("testAddMetadata_5args_2 10",dc[1].language,equalTo(lang));
        assertThat("testAddMetadata_5args_2 11",dc[1].value,equalTo(values[1]));
    }

    /**
     * Test of addMetadata method, of class Item.
     */
    @Test
    public void testAddMetadata_7args_2_authority()
    {
        //we have enabled an authority control in dspace-test.cfg to run this test
        //as MetadataAuthorityManager can't be mocked properly

        String schema = "dc";
        String element = "language";
        String qualifier = "iso";
        String lang = Item.ANY;
        String values = "en";
        String authorities = "accepted";
        int confidences = 0;
        it.addMetadata(schema, element, qualifier, lang, values, authorities, confidences);

        Metadatum[] dc = it.getMetadata(schema, element, qualifier, lang);
        assertThat("testAddMetadata_7args_2 0",dc,notNullValue());
        assertTrue("testAddMetadata_7args_2 1",dc.length == 1);
        assertThat("testAddMetadata_7args_2 2",dc[0].schema,equalTo(schema));
        assertThat("testAddMetadata_7args_2 3",dc[0].element,equalTo(element));
        assertThat("testAddMetadata_7args_2 4",dc[0].qualifier,equalTo(qualifier));
        assertThat("testAddMetadata_7args_2 5",dc[0].language,equalTo(lang));
        assertThat("testAddMetadata_7args_2 6",dc[0].value,equalTo(values));
        assertThat("testAddMetadata_7args_2 7",dc[0].authority,equalTo(authorities));
        assertThat("testAddMetadata_7args_2 8",dc[0].confidence,equalTo(confidences));
    }

    /**
     * Test of addMetadata method, of class Item.
     */
    @Test
    public void testAddMetadata_7args_2_noauthority()
    {
        //by default has no authority

        String schema = "dc";
        String element = "contributor";
        String qualifier = "author";
        String lang = Item.ANY;
        String values = "value0";
        String authorities = "auth0";
        int confidences = 0;
        it.addMetadata(schema, element, qualifier, lang, values, authorities, confidences);

        Metadatum[] dc = it.getMetadata(schema, element, qualifier, lang);
        assertThat("testAddMetadata_7args_2 0",dc,notNullValue());
        assertTrue("testAddMetadata_7args_2 1",dc.length == 1);
        assertThat("testAddMetadata_7args_2 2",dc[0].schema,equalTo(schema));
        assertThat("testAddMetadata_7args_2 3",dc[0].element,equalTo(element));
        assertThat("testAddMetadata_7args_2 4",dc[0].qualifier,equalTo(qualifier));
        assertThat("testAddMetadata_7args_2 5",dc[0].language,equalTo(lang));
        assertThat("testAddMetadata_7args_2 6",dc[0].value,equalTo(values));
        assertThat("testAddMetadata_7args_2 7",dc[0].authority,nullValue());
        assertThat("testAddMetadata_7args_2 8",dc[0].confidence,equalTo(-1));
    }

    /**
     * Test of clearDC method, of class Item.
     */
    @Test
    public void testClearDC() 
    {
        String element = "contributor";
        String qualifier = "author";
        String lang = Item.ANY;
        String value = "value";
        it.addDC(element, qualifier, lang, value);

        it.clearDC(element, qualifier, lang);

        Metadatum[] dc = it.getDC(element, qualifier, lang);
        assertThat("testClearDC 0",dc,notNullValue());
        assertTrue("testClearDC 1",dc.length == 0);
    }

    /**
     * Test of clearMetadata method, of class Item.
     */
    @Test
    public void testClearMetadata() 
    {
        String schema = "dc";
        String element = "contributor";
        String qualifier = "author";
        String lang = Item.ANY;
        String values = "value0";
        it.addMetadata(schema, element, qualifier, lang, values);

        it.clearMetadata(schema, element, qualifier, lang);

        Metadatum[] dc = it.getMetadata(schema, element, qualifier, lang);
        assertThat("testClearMetadata 0",dc,notNullValue());
        assertTrue("testClearMetadata 1",dc.length == 0);
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
        Item tmp = Item.create(context);
        context.restoreAuthSystemState();
        assertThat("testGetSubmitter 1", tmp.getSubmitter(), nullValue());
    }

    /**
     * Test of setSubmitter method, of class Item.
     */
    @Test
    public void testSetSubmitter() throws SQLException, AuthorizeException
    {
        context.turnOffAuthorisationSystem();
        EPerson sub = EPerson.create(context);
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
        assertTrue("testGetCollections 1", it.getCollections().length == 0);
    }

    /**
     * Test of getCommunities method, of class Item.
     */
    @Test
    public void testGetCommunities() throws Exception 
    {
        assertThat("testGetCommunities 0", it.getCommunities(), notNullValue());
        assertTrue("testGetCommunities 1", it.getCommunities().length == 0);
    }

    /**
     * Test of getBundles method, of class Item.
     */
    @Test
    public void testGetBundles_0args() throws Exception
    {
        assertThat("testGetBundles_0args 0", it.getBundles(), notNullValue());
        assertTrue("testGetBundles_0args 1", it.getBundles().length == 0);
    }

    /**
     * Test of getBundles method, of class Item.
     */
    @Test
    public void testGetBundles_String() throws Exception
    {
        String name = "name";
        assertThat("testGetBundles_String 0", it.getBundles(name), notNullValue());
        assertTrue("testGetBundles_String 1", it.getBundles(name).length == 0);
    }

    /**
     * Test of createBundle method, of class Item.
     */
    @Test
    public void testCreateBundleAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow Item ADD perms
            AuthorizeManager.authorizeAction((Context) any, (Item) any,
                    Constants.ADD); result = null;

        }};

        String name = "bundle";
        Bundle created = it.createBundle(name);
        assertThat("testCreateBundleAuth 0",created, notNullValue());
        assertThat("testCreateBundleAuth 1",created.getName(), equalTo(name));
        assertThat("testCreateBundleAuth 2", it.getBundles(name), notNullValue());
        assertTrue("testCreateBundleAuth 3", it.getBundles(name).length == 1);
    }

    /**
     * Test of createBundle method, of class Item.
     */
    @Test(expected=SQLException.class)
    public void testCreateBundleNoName() throws Exception
    {
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow Item ADD perms
            AuthorizeManager.authorizeAction((Context) any, (Item) any,
                    Constants.ADD); result = null;

        }};

        String name = "";
        Bundle created = it.createBundle(name);
        fail("Exception expected");
    }

    /**
     * Test of createBundle method, of class Item.
     */
    @Test(expected=SQLException.class)
    public void testCreateBundleNoName2() throws Exception
    {
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow Item ADD perms
            AuthorizeManager.authorizeAction((Context) any, (Item) any,
                    Constants.ADD); result = null;

        }};

        String name = null;
        Bundle created = it.createBundle(name);
        fail("Exception expected");
    }


    /**
     * Test of createBundle method, of class Item.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateBundleNoAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow Item ADD perms
            AuthorizeManager.authorizeAction((Context) any, (Item) any,
                    Constants.ADD); result = new AuthorizeException();

        }};

        String name = "bundle";
        Bundle created = it.createBundle(name);
        fail("Exception expected");
    }

    /**
     * Test of addBundle method, of class Item.
     */
    @Test
    public void testAddBundleAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow Item ADD perms
            AuthorizeManager.authorizeAction((Context) any, (Item) any,
                    Constants.ADD); result = null;

        }};

        String name = "bundle";
        Bundle created = Bundle.create(context);
        created.setName(name);
        it.addBundle(created);
        
        assertThat("testAddBundleAuth 0", it.getBundles(name), notNullValue());
        assertTrue("testAddBundleAuth 1", it.getBundles(name).length == 1);
        assertThat("testAddBundleAuth 2", it.getBundles(name)[0], equalTo(created));
    }

    /**
     * Test of addBundle method, of class Item.
     */
    @Test(expected=AuthorizeException.class)
    public void testAddBundleNoAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow Item ADD perms
            AuthorizeManager.authorizeAction((Context) any, (Item) any,
                    Constants.ADD); result = new AuthorizeException();

        }};

        String name = "bundle";
        Bundle created = Bundle.create(context);
        created.setName(name);
        
        it.addBundle(created);
        fail("Exception expected");
    }

    /**
     * Test of removeBundle method, of class Item.
     */
    @Test
    public void testRemoveBundleAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow Item ADD and REMOVE perms
            AuthorizeManager.authorizeAction((Context) any, (Item) any,
                    Constants.ADD); result = null;
            AuthorizeManager.authorizeAction((Context) any, (Item) any,
                    Constants.REMOVE); result = null;
        }};

        String name = "bundle";
        Bundle created = Bundle.create(context);
        created.setName(name);
        it.addBundle(created);
        
        it.removeBundle(created);
        assertThat("testRemoveBundleAuth 0", it.getBundles(name), notNullValue());
        assertTrue("testRemoveBundleAuth 1", it.getBundles(name).length == 0);
    }

    /**
     * Test of removeBundle method, of class Item.
     */
    @Test(expected=AuthorizeException.class)
    public void testRemoveBundleNoAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow Item ADD perms
            AuthorizeManager.authorizeAction((Context) any, (Item) any,
                    Constants.ADD); result = null;
            // Disallow Item REMOVE perms
            AuthorizeManager.authorizeAction((Context) any, (Item) any,
                    Constants.REMOVE); result = new AuthorizeException();
        }};

        String name = "bundle";
        Bundle created = Bundle.create(context);
        created.setName(name);
        it.addBundle(created);

        it.removeBundle(created);
        fail("Exception expected");
    }

    /**
     * Test of createSingleBitstream method, of class Item.
     */
    @Test
    public void testCreateSingleBitstream_InputStream_StringAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow Item ADD perms
            AuthorizeManager.authorizeAction((Context) any, (Item) any,
                    Constants.ADD); result = null;

        }};

        String name = "new bundle";
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream result = it.createSingleBitstream(new FileInputStream(f), name);
        assertThat("testCreateSingleBitstream_InputStream_StringAuth 0", result, notNullValue());
    }

    /**
     * Test of createSingleBitstream method, of class Item.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateSingleBitstream_InputStream_StringNoAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow Item ADD perms
            AuthorizeManager.authorizeAction((Context) any, (Item) any,
                    Constants.ADD); result = new AuthorizeException();

        }};

        String name = "new bundle";
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream result = it.createSingleBitstream(new FileInputStream(f), name);
        fail("Exception expected");
    }

    /**
     * Test of createSingleBitstream method, of class Item.
     */
    @Test
    public void testCreateSingleBitstream_InputStreamAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow Item ADD perms
            AuthorizeManager.authorizeAction((Context) any, (Item) any,
                    Constants.ADD); result = null;

        }};

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream result = it.createSingleBitstream(new FileInputStream(f));
        assertThat("testCreateSingleBitstream_InputStreamAuth 0", result, notNullValue());
    }

    /**
     * Test of createSingleBitstream method, of class Item.
     */
    @Test(expected=AuthorizeException.class)
    public void testCreateSingleBitstream_InputStreamNoAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow Item ADD perms
            AuthorizeManager.authorizeAction((Context) any, (Item) any,
                    Constants.ADD); result = new AuthorizeException();

        }};

        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream result = it.createSingleBitstream(new FileInputStream(f));
        fail("Expected exception");
    }

    /**
     * Test of getNonInternalBitstreams method, of class Item.
     */
    @Test
    public void testGetNonInternalBitstreams() throws Exception
    {
        assertThat("testGetNonInternalBitstreams 0", it.getNonInternalBitstreams(), notNullValue());
        assertTrue("testGetNonInternalBitstreams 1", it.getNonInternalBitstreams().length == 0);
    }

    /**
     * Test of removeDSpaceLicense method, of class Item.
     */
    @Test
    public void testRemoveDSpaceLicenseAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow Item ADD and REMOVE perms
            AuthorizeManager.authorizeAction((Context) any, (Item) any,
                    Constants.ADD); result = null;
            AuthorizeManager.authorizeAction((Context) any, (Item) any,
                    Constants.REMOVE); result = null;
        }};

        String name = "LICENSE";
        Bundle created = Bundle.create(context);
        created.setName(name);
        it.addBundle(created);

        it.removeDSpaceLicense();
        assertThat("testRemoveDSpaceLicenseAuth 0", it.getBundles(name), notNullValue());
        assertTrue("testRemoveDSpaceLicenseAuth 1", it.getBundles(name).length == 0);
    }

    /**
     * Test of removeDSpaceLicense method, of class Item.
     */
    @Test(expected=AuthorizeException.class)
    public void testRemoveDSpaceLicenseNoAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow Item ADD perms
            AuthorizeManager.authorizeAction((Context) any, (Item) any,
                   Constants.ADD); result = null;
            // Disallow Item REMOVE perms
            AuthorizeManager.authorizeAction((Context) any, (Item) any,
                    Constants.REMOVE); result = new AuthorizeException();
        }};

        String name = "LICENSE";
        Bundle created = Bundle.create(context);
        created.setName(name);
        it.addBundle(created);

        it.removeDSpaceLicense();
        fail("Exception expected");
    }

    /**
     * Test of removeLicenses method, of class Item.
     */
    @Test
    public void testRemoveLicensesAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow Item ADD and REMOVE perms
            AuthorizeManager.authorizeAction((Context) any, (Item) any,
                    Constants.ADD); result = null;
            AuthorizeManager.authorizeAction((Context) any, (Item) any,
                    Constants.REMOVE); result = null;
        }};

        String name = "LICENSE";
        Bundle created = Bundle.create(context);
        created.setName(name);

        String bsname = "License";
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream result = it.createSingleBitstream(new FileInputStream(f), bsname);
        result.setFormat(BitstreamFormat.findByShortDescription(context, bsname));
        created.addBitstream(result);

        it.addBundle(created);
        

        it.removeLicenses();
        assertThat("testRemoveLicensesAuth 0", it.getBundles(name), notNullValue());
        assertTrue("testRemoveLicensesAuth 1", it.getBundles(name).length == 0);
    }

    /**
     * Test of removeLicenses method, of class Item.
     */
    @Test(expected=AuthorizeException.class)
    public void testRemoveLicensesNoAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow Item ADD perms
            AuthorizeManager.authorizeAction((Context) any, (Item) any,
                   Constants.ADD); result = null;
            // Disallow Item REMOVE perms
            AuthorizeManager.authorizeAction((Context) any, (Item) any,
                    Constants.REMOVE); result = new AuthorizeException();
        }};

        String name = "LICENSE";
        Bundle created = Bundle.create(context);
        created.setName(name);

        String bsname = "License";
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream result = it.createSingleBitstream(new FileInputStream(f), bsname);
        result.setFormat(BitstreamFormat.findByShortDescription(context, bsname));
        created.addBitstream(result);

        it.addBundle(created);

        it.removeLicenses();
        fail("Exception expected");
    }

    /**
     * Test of update method, of class Item.
     */
    @Test
    public void testUpdateAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow Item WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Item) any,
                    Constants.WRITE); result = null;

        }};

        //TOOD: how to test?
        it.update();
    }

    /**
     * Test of update method, of class Item.
     */
    @Test
    public void testUpdateAuth2() throws Exception
    {
        // Test permission inheritence
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow Item WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Item) any,
                    Constants.WRITE); result = new AuthorizeException();
            // Allow parent Community WRITE and ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = true;
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = true;
            // Disallow parent Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = new AuthorizeException();

        }};

        context.turnOffAuthorisationSystem();
        Collection c = Collection.create(context);
        it.setOwningCollection(c);
        context.restoreAuthSystemState();

        //TOOD: how to test?
        it.update();
    }

    /**
     * Test of update method, of class Item.
     */
    @Test(expected=AuthorizeException.class)
    public void testUpdateNoAuth() throws Exception
    {
        // Test permission inheritence
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow Item WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Item) any,
                    Constants.WRITE); result = new AuthorizeException();
            // Disallow parent Community WRITE or ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,anyBoolean); result = false;
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,anyBoolean); result = false;
            // Disallow parent Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,anyBoolean); result = new AuthorizeException();
        }};

        context.turnOffAuthorisationSystem();
        Collection c = Collection.create(context);
        it.setOwningCollection(c);
        context.restoreAuthSystemState();

        //TOOD: how to test?
        it.update();
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

        it.withdraw();
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

        it.withdraw();
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
            // Allow Item withdraw and reinstate permissions
            AuthorizeUtil.authorizeWithdrawItem((Context) any, (Item) any);
                result = null;
            AuthorizeUtil.authorizeReinstateItem((Context) any, (Item) any);
                result = null;
        }};

        it.withdraw();
        it.reinstate();
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

        it.withdraw();
        it.reinstate();
        fail("Exception expected");
    }

    /**
     * Test of delete method, of class Item.
     */
    @Test
    public void testDeleteAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow Item REMOVE perms
            AuthorizeManager.authorizeAction((Context) any, (Item) any,
                    Constants.REMOVE, true); result = null;
        }};

        int id = it.getID();
        it.delete();
        Item found = Item.find(context, id);
        assertThat("testDeleteAuth 0",found,nullValue());
    }

    /**
     * Test of delete method, of class Item.
     */
    @Test(expected=AuthorizeException.class)
    public void testDeleteNoAuth() throws Exception
    {
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow Item REMOVE perms
            AuthorizeManager.authorizeAction((Context) any, (Item) any,
                    Constants.REMOVE); result = new AuthorizeException();
        }};
        
        it.delete();
        fail("Exception expected");
    }

    /**
     * Test of decache method, of class Item.
     */
    @Test
    public void testDecache() throws Exception
    {
        int id = it.getID();
        it.decache();
        Item found = (Item) context.fromCache(Item.class, id);
        assertThat("testDecache 0",found,nullValue());
    }

    /**
     * Test of equals method, of class Item.
     */
    @Test
    @SuppressWarnings("ObjectEqualsNull")
    public void testEquals() throws SQLException, AuthorizeException
    {
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow Item ADD perms (needed to create an Item)
            AuthorizeManager.authorizeActionBoolean((Context) any, (Item) any,
                    Constants.ADD); result = true;
        }};

        assertFalse("testEquals 0",it.equals(null));
        assertFalse("testEquals 1",it.equals(Item.create(context)));
        assertTrue("testEquals 2", it.equals(it));
    }

    /**
     * Test of isOwningCollection method, of class Item.
     */
    @Test
    public void testIsOwningCollection() throws SQLException, AuthorizeException
    {
        context.turnOffAuthorisationSystem();
        Collection c = Collection.create(context);
        context.restoreAuthSystemState();
        
        boolean result = it.isOwningCollection(c);
        assertFalse("testIsOwningCollection 0",result);
    }

    /**
     * Test of getType method, of class Item.
     */
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
        ResourcePolicy pol1 = ResourcePolicy.create(context);
        newpolicies.add(pol1);
        it.replaceAllItemPolicies(newpolicies);

        List<ResourcePolicy> retrieved = AuthorizeManager.getPolicies(context, it);
        assertThat("testReplaceAllItemPolicies 0",retrieved, notNullValue());
        assertThat("testReplaceAllItemPolicies 1",retrieved.size(), equalTo(newpolicies.size()));
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
        Bundle created = Bundle.create(context);
        created.setName(name);

        String bsname = "License";
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream result = it.createSingleBitstream(new FileInputStream(f), bsname);
        result.setFormat(BitstreamFormat.findByShortDescription(context, bsname));
        created.addBitstream(result);

        it.addBundle(created);

        List<ResourcePolicy> newpolicies = new ArrayList<ResourcePolicy>();
        newpolicies.add(ResourcePolicy.create(context));
        newpolicies.add(ResourcePolicy.create(context));
        newpolicies.add(ResourcePolicy.create(context));
        context.restoreAuthSystemState();

        it.replaceAllBitstreamPolicies(newpolicies);

        List<ResourcePolicy> retrieved = new ArrayList<ResourcePolicy>();
        Bundle[] bundles = it.getBundles();
        for(Bundle b: bundles)
        {
            retrieved.addAll(b.getBundlePolicies());
            retrieved.addAll(b.getBitstreamPolicies());
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
        assertTrue("testReplaceAllBitstreamPolicies 1", equals);
    }

    /**
     * Test of removeGroupPolicies method, of class Item.
     */
    @Test
    public void testRemoveGroupPolicies() throws Exception
    {
        context.turnOffAuthorisationSystem();
        List<ResourcePolicy> newpolicies = new ArrayList<ResourcePolicy>();
        Group g = Group.create(context);
        ResourcePolicy pol1 = ResourcePolicy.create(context);
        newpolicies.add(pol1);
        pol1.setGroup(g);
        it.replaceAllBitstreamPolicies(newpolicies);
        context.restoreAuthSystemState();

        it.removeGroupPolicies(g);

        List<ResourcePolicy> retrieved = AuthorizeManager.getPolicies(context, it);
        assertThat("testRemoveGroupPolicies 0",retrieved, notNullValue());
        assertTrue("testRemoveGroupPolicies 1",retrieved.isEmpty());
    }

    /**
     * Test of inheritCollectionDefaultPolicies method, of class Item.
     */
    @Test
    public void testInheritCollectionDefaultPolicies() throws Exception 
    {
        context.turnOffAuthorisationSystem();

        Collection c = Collection.create(context);

        //TODO: we would need a method to get policies from collection, probably better!
        List<ResourcePolicy> newpolicies = AuthorizeManager.getPoliciesActionFilter(context, c,
                Constants.DEFAULT_BITSTREAM_READ);
        Iterator<ResourcePolicy> iter = newpolicies.iterator();
        while (iter.hasNext())
        {
            ResourcePolicy rp = (ResourcePolicy) iter.next();
            rp.setAction(Constants.READ);
        }

        //we add some bundles for the test
        String name = "LICENSE";
        Bundle created = Bundle.create(context);
        created.setName(name);

        String bsname = "License";
        File f = new File(testProps.get("test.bitstream").toString());
        Bitstream result = it.createSingleBitstream(new FileInputStream(f), bsname);
        result.setFormat(BitstreamFormat.findByShortDescription(context, bsname));
        created.addBitstream(result);

        it.addBundle(created);
        context.restoreAuthSystemState();
        
        it.inheritCollectionDefaultPolicies(c);
        
        //test item policies
        List<ResourcePolicy> retrieved = AuthorizeManager.getPolicies(context, it);
        boolean equals = true;
        for(int i=0; i < retrieved.size() && equals; i++)
        {
            if(!newpolicies.contains(retrieved.get(i)))
            {
                equals = false;
            }
        }        
        assertTrue("testInheritCollectionDefaultPolicies 0", equals);

        retrieved = new ArrayList<ResourcePolicy>();
        Bundle[] bundles = it.getBundles();
        for(Bundle b: bundles)
        {
            retrieved.addAll(b.getBundlePolicies());
            retrieved.addAll(b.getBitstreamPolicies());
        }
        assertFalse("testInheritCollectionDefaultPolicies 1",retrieved.isEmpty());

        equals = true;
        for(int i=0; i < newpolicies.size() && equals; i++)
        {
            if(!newpolicies.contains(retrieved.get(i)))
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

        // Create two new collections to test with
        Collection from = Collection.create(context);
        Collection to = Collection.create(context);

        // Create a new item to test with
        // (Ensures the item is not already mapped to another collection by a different test)
        Item item = Item.create(context);
        item.setOwningCollection(from);
        from.addItem(item);
        assertThat("testMove 0",item.getOwningCollection(), equalTo(from));

        // Now, test the move
        item.move(from, to);
        context.restoreAuthSystemState();

        assertThat("testMove 1",item.getOwningCollection(), notNullValue());
        assertThat("testMove 2",item.getOwningCollection(), equalTo(to));
    }

    /**
     * Test of hasUploadedFiles method, of class Item.
     */
    @Test
    public void testHasUploadedFiles() throws Exception
    {
        assertFalse("testHasUploadedFiles 0",it.hasUploadedFiles());
    }

    /**
     * Test of getCollectionsNotLinked method, of class Item.
     */
    @Test
    public void testGetCollectionsNotLinked() throws Exception
    {
        Collection[] result = it.getCollectionsNotLinked();
        boolean isin = false;
        for(Collection c: result)
        {
            ItemIterator iit = c.getAllItems();
            while(iit.hasNext())
            {
                if(iit.next().getID() == it.getID())
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
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Allow Item WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Item) any,
                    Constants.WRITE); result = true;
            // Allow parent Community WRITE and ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = true;
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = true;
            // Allow parent Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = null;
        }};

        assertTrue("testCanEditBooleanAuth 0", it.canEdit());
    }

    /**
     * Test of canEdit method, of class Item.
     */
    @Test
    public void testCanEditBooleanAuth2() throws Exception
    {
        // Test Inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow Item WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Item) any,
                    Constants.WRITE); result = false;
            // Allow parent Community WRITE and ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = true;
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = true;
            // Allow parent Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = null;
        }};

        assertTrue("testCanEditBooleanAuth2 0", it.canEdit());
    }

    /**
     * Test of canEdit method, of class Item.
     */
    @Test
    public void testCanEditBooleanAuth3() throws Exception
    {
        // Test Inheritance of permissions for owning collection
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow Item WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Item) any,
                    Constants.WRITE); result = false;
            // Allow parent Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE, false); result = null;
        }};

        // Create a new Collection and assign it as the owner
        context.turnOffAuthorisationSystem();
        Collection c = Collection.create(context);
        it.setOwningCollection(c);
        context.restoreAuthSystemState();

        // Ensure person with WRITE perms on the Collection can edit item
        assertTrue("testCanEditBooleanAuth3 0", it.canEdit());
    }

    /**
     * Test of canEdit method, of class Item.
     */
    @Test
    public void testCanEditBooleanAuth4() throws Exception
    {
        // Test Inheritance of permissions for Community Admins
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow Item WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Item) any,
                    Constants.WRITE); result = false;
            // Allow parent Community WRITE and ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,true); result = true;
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,true); result = true;
            // Disallow parent Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,true); result = new AuthorizeException();
        }};

        // Ensure person with WRITE perms on the Collection can edit item
        assertTrue("testCanEditBooleanAuth4 0", it.canEdit());
    }

    /**
     * Test of canEdit method, of class Item.
     */
    @Test
    public void testCanEditBooleanAuth5() throws Exception
    {
        // Test Inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow Item WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Item) any,
                    Constants.WRITE); result = new AuthorizeException();
            // Allow Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,anyBoolean); result = null;
        }};

        Collection c = Collection.create(context);
        c.createTemplateItem();
        c.update();
        assertTrue("testCanEditBooleanNoAuth5 0", c.getTemplateItem().canEdit());
    }
    
    /**
     * Test of canEdit method, of class Item.
     */
    @Test
    public void testCanEditBooleanNoAuth() throws Exception
    {
        // Test Inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow Item WRITE perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Item) any,
                    Constants.WRITE); result = false;
            // Disallow parent Community WRITE and ADD perms
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.WRITE,anyBoolean); result = false;
            AuthorizeManager.authorizeActionBoolean((Context) any, (Community) any,
                    Constants.ADD,anyBoolean); result = false;
            // Disallow parent Collection WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.WRITE,anyBoolean); result = new AuthorizeException();
        }};

        context.turnOffAuthorisationSystem();
        Collection c = Collection.create(context);
        it.setOwningCollection(c);
        context.restoreAuthSystemState();

        assertFalse("testCanEditBooleanNoAuth 0", it.canEdit());
    }

    /**
     * Test of canEdit method, of class Item.
     */
    @Test
    public void testCanEditBooleanNoAuth2() throws Exception
    {
        // Test Inheritance of permissions
        new NonStrictExpectations(AuthorizeManager.class)
        {{
            // Disallow Item WRITE perms
            AuthorizeManager.authorizeAction((Context) any, (Item) any,
                    Constants.WRITE); result = new AuthorizeException();
            // Disallow parent Community WRITE and ADD perms
            AuthorizeManager.authorizeAction((Context) any, (Community) any,
                    Constants.WRITE,anyBoolean); result = new AuthorizeException();
            AuthorizeManager.authorizeAction((Context) any, (Community) any,
                    Constants.ADD,anyBoolean); result = new AuthorizeException();
            // Allow parent Collection ADD perms
            AuthorizeManager.authorizeAction((Context) any, (Collection) any,
                    Constants.ADD,anyBoolean); result = null;
        }};

        Collection c = Collection.create(context);
        WorkspaceItem wi = WorkspaceItem.create(context, c, true);
        assertFalse("testCanEditBooleanNoAuth2 0", wi.getItem().canEdit());
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
    	Collection c = Collection.create(context);
        WorkspaceItem wi = WorkspaceItem.create(context, c, true);
    	context.restoreAuthSystemState();
        assertTrue("testIsInProgressSubmission 0", wi.getItem().isInProgressSubmission());
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
    	Collection c = Collection.create(context);
        WorkspaceItem wi = WorkspaceItem.create(context, c, true);
        Item item = InstallItem.installItem(context, wi);
    	context.restoreAuthSystemState();
        assertFalse("testIsInProgressSubmissionFalse 0", item.isInProgressSubmission());
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
    	Collection c = Collection.create(context);
        c.createTemplateItem();
        c.update();
        Item item = c.getTemplateItem();
    	context.restoreAuthSystemState();
        assertFalse("testIsInProgressSubmissionFalse2 0", item.isInProgressSubmission());
    }

    /**
     * Test of getName method, of class Item.
     */
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

        ItemIterator result = Item.findByMetadataField(context, schema, element, qualifier, value);
        assertThat("testFindByMetadataField 0",result,notNullValue());
        assertFalse("testFindByMetadataField 1",result.hasNext());
        assertThat("testFindByMetadataField 2",result.next(), nullValue());

        it.addMetadata(schema,element, qualifier, Item.ANY, value);
        it.update();

        result = Item.findByMetadataField(context, schema, element, qualifier, value);
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
        assertThat("testGetAdminObject 0", (Item)it.getAdminObject(Constants.REMOVE), equalTo(it));
        assertThat("testGetAdminObject 1", (Item)it.getAdminObject(Constants.ADD), equalTo(it));
        assertThat("testGetAdminObject 2", it.getAdminObject(Constants.DELETE), nullValue());
        assertThat("testGetAdminObject 3", (Item)it.getAdminObject(Constants.ADMIN), equalTo(it));
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
            assertThat("testGetParentObject 0", it.getParentObject(), nullValue());

            context.turnOffAuthorisationSystem();
            Collection parent = Collection.create(context);
            it.setOwningCollection(parent);
            context.restoreAuthSystemState();
            assertThat("testGetParentObject 1", it.getParentObject(), notNullValue());
            assertThat("testGetParentObject 2", (Collection)it.getParentObject(), equalTo(parent));
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

        ItemIterator result = Item.findByAuthorityValue(context, schema, element, qualifier, value);
        assertThat("testFindByAuthorityValue 0",result,notNullValue());
        assertFalse("testFindByAuthorityValue 1",result.hasNext());
        assertThat("testFindByAuthorityValue 2",result.next(), nullValue());

        it.addMetadata(schema, element, qualifier, Item.ANY, value, authority, confidence);
        it.update();

        result = Item.findByAuthorityValue(context, schema, element, qualifier, authority);
        assertThat("testFindByAuthorityValue 3",result,notNullValue());
        assertTrue("testFindByAuthorityValue 4",result.hasNext());
        assertThat("testFindByAuthorityValue 5",result.next(),equalTo(it));
    }

}
