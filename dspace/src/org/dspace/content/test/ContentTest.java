/*
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

import junit.framework.*;

import org.dspace.content.*;
import org.dspace.core.*;
import org.dspace.storage.rdbms.DatabaseManager;

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
            context = new Context();
            Community c1 = Community.create(context);
            c1.setMetadata("name", TEST_NAME_1);
            c1.update();
            Community c2 = Community.find (context, c1.getID());
            assertNotNull("Found community", c2);
            assertEquals("Found community has correct name",
                         c2.getMetadata("name"), TEST_NAME_1);
            assertTrue("Found community in getAllCommunities array",
                       contains(Community.getAllCommunities(context), c1));
            c1.delete();
            Community c3 = Community.find (context, c1.getID());
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
            Community c1 = Community.create(context);
            c1.setMetadata("name", TEST_NAME_1);
            c1.update();
            Collection collection = Collection.create(context);
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
            Collection c1 = Collection.create(context);
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

    ////////////////////////////////////////
    // Helper methods
    ////////////////////////////////////////

    private static boolean contains(Object[] array, Object obj)
    {
        for (int i = 0; i < array.length; i++ )
        {
            if (array[i].equals(obj))
                return true;
        }

        return false;
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
