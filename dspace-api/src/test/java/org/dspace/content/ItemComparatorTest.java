/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
            fail("Authorization Error in init: " + ex.getMessage());
        }
        catch (SQLException ex)
        {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init:" + ex.getMessage());
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