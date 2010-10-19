/*
 * ItemComparatorTest.java
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
import org.apache.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.junit.*;
import static org.junit.Assert.* ;

/**
 * Unit Tests for class ItemComparator
 * @author pvillega
 */
public class ItemComparatorTest extends AbstractUnitTest
{

    /** log4j category */
    private static final Logger log = Logger.getLogger(ItemComparatorTest.class);

    /**
     * Item instance for the tests
     */
    private Item one;

    /**
     * Item instance for the tests
     */
    private Item two;

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

            context.turnOffAuthorisationSystem();
            one = Item.create(context);
            context.commit();
            two = Item.create(context);
            context.commit();
            context.restoreAuthSystemState();
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
     * Test of compare method, of class ItemComparator.
     */
    @Test
    public void testCompare() 
    {
        int result = 0;
        ItemComparator ic = null;

        //one of the tiems has no value
        ic = new ItemComparator("test", "one", Item.ANY, true);
        result = ic.compare(one, two);
        assertTrue("testCompare 0",result == 0);

        ic = new ItemComparator("test", "one", Item.ANY, true);
        one.addMetadata("dc", "test", "one", Item.ANY, "1");        
        result = ic.compare(one, two);
        assertTrue("testCompare 1",result >= 1);
        one.clearMetadata("dc", "test", "one", Item.ANY);
        
        ic = new ItemComparator("test", "one", Item.ANY, true);
        two.addMetadata("dc", "test", "one", Item.ANY, "1");        
        result = ic.compare(one, two);
        assertTrue("testCompare 2",result <= -1);
        two.clearMetadata("dc", "test", "one", Item.ANY);
        
        //value in both items
        ic = new ItemComparator("test", "one", Item.ANY, true);
        one.addMetadata("dc", "test", "one", Item.ANY, "1");
        two.addMetadata("dc", "test", "one", Item.ANY, "2");
        result = ic.compare(one, two);
        assertTrue("testCompare 3",result <= -1);
        one.clearMetadata("dc", "test", "one", Item.ANY);
        two.clearMetadata("dc", "test", "one", Item.ANY);
        
        ic = new ItemComparator("test", "one", Item.ANY, true);
        one.addMetadata("dc", "test", "one", Item.ANY, "1");
        two.addMetadata("dc", "test", "one", Item.ANY, "1");
        result = ic.compare(one, two);
        assertTrue("testCompare 4",result == 0);
        one.clearMetadata("dc", "test", "one", Item.ANY);
        two.clearMetadata("dc", "test", "one", Item.ANY);
        
        ic = new ItemComparator("test", "one", Item.ANY, true);
        one.addMetadata("dc", "test", "one", Item.ANY, "2");
        two.addMetadata("dc", "test", "one", Item.ANY, "1");
        result = ic.compare(one, two);
        assertTrue("testCompare 5",result >= 1);
        one.clearMetadata("dc", "test", "one", Item.ANY);
        two.clearMetadata("dc", "test", "one", Item.ANY);

        //multiple values (min, max)
        ic = new ItemComparator("test", "one", Item.ANY, true);
        one.addMetadata("dc", "test", "one", Item.ANY, "0");
        one.addMetadata("dc", "test", "one", Item.ANY, "1");
        two.addMetadata("dc", "test", "one", Item.ANY, "2");
        two.addMetadata("dc", "test", "one", Item.ANY, "3");
        result = ic.compare(one, two);
        assertTrue("testCompare 3",result <= -1);
        one.clearMetadata("dc", "test", "one", Item.ANY);
        two.clearMetadata("dc", "test", "one", Item.ANY);

        ic = new ItemComparator("test", "one", Item.ANY, true);
        one.addMetadata("dc", "test", "one", Item.ANY, "0");
        one.addMetadata("dc", "test", "one", Item.ANY, "1");
        two.addMetadata("dc", "test", "one", Item.ANY, "-1");
        two.addMetadata("dc", "test", "one", Item.ANY, "1");
        result = ic.compare(one, two);
        assertTrue("testCompare 4",result == 0);
        one.clearMetadata("dc", "test", "one", Item.ANY);
        two.clearMetadata("dc", "test", "one", Item.ANY);

        ic = new ItemComparator("test", "one", Item.ANY, true);
        one.addMetadata("dc", "test", "one", Item.ANY, "1");
        one.addMetadata("dc", "test", "one", Item.ANY, "2");
        two.addMetadata("dc", "test", "one", Item.ANY, "1");
        two.addMetadata("dc", "test", "one", Item.ANY, "-1");
        result = ic.compare(one, two);
        assertTrue("testCompare 5",result >= 1);
        one.clearMetadata("dc", "test", "one", Item.ANY);
        two.clearMetadata("dc", "test", "one", Item.ANY);

        ic = new ItemComparator("test", "one", Item.ANY, false);
        one.addMetadata("dc", "test", "one", Item.ANY, "1");
        one.addMetadata("dc", "test", "one", Item.ANY, "2");
        two.addMetadata("dc", "test", "one", Item.ANY, "2");
        two.addMetadata("dc", "test", "one", Item.ANY, "3");
        result = ic.compare(one, two);
        assertTrue("testCompare 3",result <= -1);
        one.clearMetadata("dc", "test", "one", Item.ANY);
        two.clearMetadata("dc", "test", "one", Item.ANY);

        ic = new ItemComparator("test", "one", Item.ANY, false);
        one.addMetadata("dc", "test", "one", Item.ANY, "1");
        one.addMetadata("dc", "test", "one", Item.ANY, "2");
        two.addMetadata("dc", "test", "one", Item.ANY, "1");
        two.addMetadata("dc", "test", "one", Item.ANY, "5");
        result = ic.compare(one, two);
        assertTrue("testCompare 4",result == 0);
        one.clearMetadata("dc", "test", "one", Item.ANY);
        two.clearMetadata("dc", "test", "one", Item.ANY);

        ic = new ItemComparator("test", "one", Item.ANY, false);
        one.addMetadata("dc", "test", "one", Item.ANY, "2");
        one.addMetadata("dc", "test", "one", Item.ANY, "3");
        two.addMetadata("dc", "test", "one", Item.ANY, "1");
        two.addMetadata("dc", "test", "one", Item.ANY, "4");
        result = ic.compare(one, two);
        assertTrue("testCompare 5",result >= 1);
        one.clearMetadata("dc", "test", "one", Item.ANY);
        two.clearMetadata("dc", "test", "one", Item.ANY);
    }

    /**
     * Test of equals method, of class ItemComparator.
     */
    @Test
    @SuppressWarnings({"ObjectEqualsNull", "IncompatibleEquals"})
    public void testEquals()
    {
        ItemComparator ic = new ItemComparator("test", "one", Item.ANY, true);
        ItemComparator target = null;

        assertFalse("testEquals 0", ic.equals(null));
        assertFalse("testEquals 1", ic.equals("test one"));

        target = new ItemComparator("test1", "one", Item.ANY, true);
        assertFalse("testEquals 2", ic.equals(target));

        target = new ItemComparator("test", "one1", Item.ANY, true);
        assertFalse("testEquals 3", ic.equals(target));

        target = new ItemComparator("test", "one", "es", true);
        assertFalse("testEquals 4", ic.equals(target));

        target = new ItemComparator("test1", "one", Item.ANY, false);
        assertFalse("testEquals 5", ic.equals(target));

        target = new ItemComparator("test", "one", Item.ANY, true);
        assertTrue("testEquals 6", ic.equals(target));
    }

}