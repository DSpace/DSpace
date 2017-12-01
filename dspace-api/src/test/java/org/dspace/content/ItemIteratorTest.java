/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.content;

import java.sql.SQLException;
import java.util.ArrayList;
import org.junit.*;
import static org.junit.Assert.* ;
import static org.hamcrest.CoreMatchers.*;
import org.apache.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;

/**
 * Unit Tests for class ItemIterator
 * @author pvillega
 */
public class ItemIteratorTest extends AbstractUnitTest
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(ItemIteratorTest.class);

    /**
     * ItemIterator instance for the tests using a TableRow
     */
    private ItemIterator iitr;

    /**
     * ItemIterator instance for the tests using an ID array
     */
    private ItemIterator iitid;

    /**
     * ItemIterator instance for the tests, empty
     */
    private ItemIterator iitnone;


    /**
     * Number of items in the test
     */
    private int numitems;

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
        try
        {
            super.init();

            //create items for the iterator
            ArrayList<Integer> list = new ArrayList<Integer>();
            numitems = 1;
            context.turnOffAuthorisationSystem();
            for(int i = 0; i < numitems; i++)
            {
                Item it = Item.create(context);
                list.add(it.getID());
                it.setArchived(true);
                it.update();                
            }
            context.restoreAuthSystemState();

            iitr = Item.findAll(context);
            iitid = new ItemIterator(context, list);
            iitnone = new ItemIterator(context, new ArrayList<Integer>());
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
        super.destroy();
    }

    /**
     * Test of hasNext method, of class ItemIterator.
     */
    @Test
    public void testHasNext() throws Exception
    {
        assertFalse("testHasNext iitnone 0", iitnone.hasNext());

        for(int i = 0; i < numitems; i++) 
        {
            assertTrue("testHasNext iitr "+i, iitr.hasNext());
            iitr.next();
        }

        for(int i = 0; i < numitems; i++)
        {
            assertTrue("testHasNext iitid "+i, iitid.hasNext());
            iitid.next();
        }
    }

    /**
     * Test of next method, of class ItemIterator.
     */
    @Test
    public void testNext() throws Exception
    {
        assertThat("testNext iitnone 0", iitnone.next(), nullValue());

        for(int i = 0; i < numitems; i++)
        {
            assertThat("testNext iitr "+i, iitr.next(), notNullValue());
        }

        for(int i = 0; i < numitems; i++)
        {
            assertThat("testNext iitid "+i, iitid.next(), notNullValue());
        }
    }

    /**
     * Test of nextID method, of class ItemIterator.
     */
    @Test
    public void testNextID() throws Exception
    {
        assertThat("testNextID iitnone 0", iitnone.nextID(), equalTo(-1));

        for(int i = 0; i < numitems; i++)
        {
            assertTrue("testNextID iitr "+i, iitr.nextID() >= 0);
        }

        for(int i = 0; i < numitems; i++)
        {
            assertTrue("testNextID iitid "+i, iitid.nextID() >= 0);
        }
    }

    /**
     * Test of close method, of class ItemIterator.
     */
    @Test
    public void testClose()
    {
        //TODO: we can't verified it's been closed
        iitnone.close();
        iitr.close();
        iitid.close();
    }

}