/*
 * ContentTest.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
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
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
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


package org.dspace.content.test;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import junit.framework.*;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.*;
import org.dspace.storage.rdbms.*;

/**
 * JUnit test for content API
 *
 * @author  Peter Breton
 * @version $Revision$
 */
public class ContentTest extends TestCase
{

    private static final String TEST_NAME_1 =
        "Test Community 1 created by " + ContentTest.class.getName();
    private static final String TEST_NAME_2 =
        "Test Community 2 created by " + ContentTest.class.getName();

    /**
     * Constructor
     *
     * @param name - The name of the TestCase
     */
    public ContentTest (String name)
    {
        super(name);
    }

    public void setUp()
    {
        try
        {

            Context context = new Context();
            assertNull("Test community has been deleted",
                       DatabaseManager.findByUnique(context, "Community",
                                                    "name", TEST_NAME_1));
            assertNull("Test community has been deleted",
                       DatabaseManager.findByUnique(context, "Community",
                                                    "name", TEST_NAME_2));
            context.complete();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    /**
     * Test of community
     */
    public void testCommunity()
    {
        Context context = null;

        try
        {
            // Create context
            context = new Context();

            // Create a community
            Community c1 = createCommunity(context);
            int id = c1.getID();

            // Check we can get it
            Community c2 = Community.find (context, c1.getID());
            assertNotNull("Found community", c2);
            assertEquals("Found community has correct name",
                         c2.getMetadata("name"), TEST_NAME_1);
            assertTrue("Found community in getAllCommunities array",
                       contains(Community.getAllCommunities(context), c1));

            // FIXME: Start a new transaction - this is a workaround for
            // a PostgreSQL 7.1 bug.

            context.complete();
            context = new Context();

            c1 = Community.find(context, id);
            assertNotNull("Found community", c1);

            // Delete it
            c1.delete();

            Community c3 = Community.find (context, id);
            assertNull("Did not find deleted community", c3);
            context.complete();
        }
        // Clean up context or the test hangs
        catch (AssertionFailedError afe)
        {
            if (context != null)
                context.abort();
            throw afe;
        }
        catch (Exception e)
        {
            if (context != null)
                context.abort();
            e.printStackTrace();
            System.out.println("Got exception: " + e);
            fail("Exception while running test: " + e);
        }
    }

    /**
     * Test of community and collection relationships
     */
    public void testCommunityAndCollection()
    {
        Context context = null;

        try
        {
            context = new Context();
            Community c1 = createCommunity(context);
            Collection collection = c1.createCollection();
            int cid = collection.getID();
            c1.addCollection(collection);
            assertNotNull("Found new collection",
                          Collection.find(context, cid));
            assertTrue("Community includes this collection",
                       contains(collection.getCommunities(), c1));
            assertTrue("Collection is present in getAllCollections ",
                       contains(Collection.getAllCollections(context),
                                collection));
            Community c2 = Community.create(context);
            c2.setMetadata("name", TEST_NAME_2);
            c2.update();
            c2.addCollection(collection);
            assertTrue("Community includes this collection",
                       contains(collection.getCommunities(), c1));
            assertTrue("Community includes this collection",
                       contains(collection.getCommunities(), c2));
            c2.delete();
            assertNotNull("Collection still exists after one containing community is deleted",
                          Collection.find(context, cid));

             c1.deleteWithContents();
             assertNull("Collection does not exist after all containing communities are deleted",
                         Collection.find(context, cid));
            context.complete();
        }
        // Clean up context or the test hangs
        catch (AssertionFailedError afe)
        {
            if (context != null)
                context.abort();
            throw afe;
        }
        catch (Exception e)
        {
            if (context != null)
                context.abort();
            e.printStackTrace();
            System.out.println("Got exception: " + e);
            fail("Exception while running test: " + e);
        }
    }

    /**
     * Test of collection
     */
    public void testCollection()
    {
        Context context = null;

        try
        {
            context = new Context();
            Community community = createCommunity(context);
            Collection c1 = community.createCollection();
            c1.setMetadata("name", TEST_NAME_1);
            c1.update();
            Collection c2 = Collection.find (context, c1.getID());
            assertNotNull("Found collection", c2);
            assertEquals("Found collection has correct name",
                         c2.getMetadata("name"), TEST_NAME_1);
            assertTrue("Found collection in getAllCollections array",
                       contains(Collection.getAllCollections(context), c1));
            c1.delete();
            Collection c3 = Collection.find (context, c1.getID());
            assertNull("Did not find deleted collection", c3);
            context.complete();
        }
        // Clean up context or the test hangs
        catch (AssertionFailedError afe)
        {
            if (context != null)
                context.abort();

            throw afe;
        }
        catch (Exception e)
        {
            if (context != null)
                context.abort();
            e.printStackTrace();
            System.out.println("Got exception: " + e);
            fail("Exception while running test: " + e);
        }
    }

    /**
     * Test of item
     */
    public void testItemDCValues()
    {
        Context context = null;

        List TITLES = Arrays.asList( new String[] {
            "Test Title",
            "Warehouse Songs and Stories",
            "Rainbow Soup"
        });

        List ALTERNATIVE_TITLES = Arrays.asList( new String[] {
            "Test Title 2",
            "Being and Nothingness",
            "Jungle Book",
            "Perls before Swine"
        });

        String LANG = "en";
        String now = new DCDate(new java.util.Date()).toString();

        try
        {
            context = new Context();

            Community community = Community.create(context);
            Collection collection = community.createCollection();

            WorkspaceItem wi = WorkspaceItem.create(context, collection, null);

            Item item = wi.getItem();
            int id = item.getID();

            // Add some DC fields
            item.addDC("title", null, LANG,
                       toStringArray(TITLES));
            item.addDC("title", null, LANG,
                       (String) TITLES.get(0));
            item.addDC("title", "alternative", LANG,
                       toStringArray(ALTERNATIVE_TITLES));
            item.addDC("date","accessioned", null, now);
            item.update();

            // Retrieve titles
            String[] titles = item.getDC("title", null, LANG);
            assertNotNull("Retrieved titles", titles);
            assertEquals("Got correct number of titles",
                         titles.length,
                         TITLES.size() + 1);
            assertTrue("Got correct titles",
                       containsAll(titles, TITLES));

            // Retrieve alternative titles
            String[] alt = item.getDC("title", "alternative", LANG);
            assertNotNull("Retrieved titles", alt);
            assertEquals("Got correct number of titles",
                         alt.length,
                         ALTERNATIVE_TITLES.size());
            assertTrue("Got correct titles",
                       containsAll(alt, ALTERNATIVE_TITLES));

            // Retrieve dates
            String[] dates = item.getDC("date", "accessioned", null);
            assertNotNull("Retrieved dates", dates);
            assertEquals("Got correct number of dates",
                         dates.length, 1);
            assertEquals("Got correct date", now, dates[0]);

            // Retrieve wildcard
            String[] all = item.getDC(Item.ANY, Item.ANY, Item.ANY);
            assertNotNull("Got values", all);
            assertEquals("Got correct number of values",
                         all.length,
                         TITLES.size() + ALTERNATIVE_TITLES.size() + 1 + 1);

            // Retrieve wildcarded titles
            String[] all_titles = item.getDC("title", Item.ANY, Item.ANY);
            assertNotNull("Got values", all_titles);
            assertEquals("Got correct number of values",
                         all_titles.length,
                         TITLES.size() + ALTERNATIVE_TITLES.size() + 1);

            // Clear just the alternative title
            item.clearDC("title", "alternative", LANG);
            alt = item.getDC("title", "alternative", LANG);
            assertNotNull("Retrieved titles", alt);
            assertEquals("Got correct number of titles",
                         alt.length, 0);

            item.deleteWithContents();
            assertNull("Item cannot be found after deletion",
                       Item.find(context, id));
            context.abort();
        }
        // Clean up context or the test hangs
        catch (AssertionFailedError afe)
        {
            if (context != null)
                context.abort();

            throw afe;
        }
        catch (Exception e)
        {
            if (context != null)
                context.abort();
            e.printStackTrace();
            System.out.println("Got exception: " + e);
            fail("Exception while running test: " + e);
        }
    }

    ////////////////////////////////////////
    // Helper methods
    ////////////////////////////////////////

    /**
     * True if OBJ is in ARRAY, false otherwise
     */
    private static boolean contains(Object[] array, Object obj)
    {
        for (int i = 0; i < array.length; i++ )
        {
            if (array[i].equals(obj))
                return true;
        }

        return false;
    }

    /**
     * True if every member of LIST is in ARRAY.
     */
    private static boolean containsAll(Object[] array, List list)
    {
    LIST:
        for (Iterator iterator = list.iterator(); iterator.hasNext(); )
        {
            Object obj = (Object) iterator.next();
            for (int i = 0; i < array.length; i++ )
            {
                if (obj.equals(array[i]))
                    continue LIST;
            }

            return false;
        }

        return true;
    }

    /**
     * Convert list to String array.
     */
    private static String[] toStringArray(List list)
    {
        return (String[]) list.toArray(new String[list.size()]);
    }

    private Community createCommunity(Context context)
        throws SQLException, AuthorizeException
    {
        Community community = Community.create(context);
        community.setMetadata("name", TEST_NAME_1);
        community.update();
        return community;
    }

    ////////////////////////////////////////
    // Static test methods
    ////////////////////////////////////////

    /**
     * Test suite
     */
    public static Test suite()
    {
        return new TestSuite(ContentTest.class);
    }

    /**
     * Embedded test harness
     *
     * @param argv - Command-line arguments
     */
    public static void main(String[] argv)
    {
        junit.textui.TestRunner.run(suite());
        System.exit(0);
    }
}
