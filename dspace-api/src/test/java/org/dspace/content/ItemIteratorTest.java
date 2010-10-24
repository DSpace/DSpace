/*
 * ItemIteratorTest.java
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the DSpace Foundation nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
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
            fail("Authorization Error in init");
        }
        catch (SQLException ex)
        {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init");
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