/*
 * HandleTest.java
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


package org.dspace.handle.test;

import java.net.URL;
import java.sql.SQLException;

import junit.framework.*;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.content.*;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import org.dspace.storage.rdbms.*;

/**
 * Regression test for HandleManager
 *
 * @author  Peter Breton
 * @version $Revision$
 */
public class HandleTest extends TestCase
{
    /**
     * Constructor
     *
     * @param name - The name of the TestCase
     */
    public HandleTest (String name)
    {
        super(name);
    }

    /**
     * JUnit test method
     */
    public void testHandles()
    {
        Context context = null;

        try
        {
            context = new Context();
            context.setIgnoreAuthorization(true);
            Item item = createItem(context);

            String handle = HandleManager.createHandle(context, item);
            assertNotNull("Created handle", handle);

            String canonical = HandleManager.getCanonicalForm(handle);
            assertNotNull("Got canonical form", canonical);
            String url = HandleManager.resolveToURL(context, handle);
            assertNotNull("Got resolved URL", url);

            Object resolved = HandleManager.resolveToObject(context, handle);
            assertNotNull("Resolved handle " + handle, resolved);
            assertTrue("Handle is an Item", resolved instanceof Item);
            Item resolvedItem = (Item) resolved;
            assertTrue("Handle is same Item", resolvedItem.equals(item));

            String rhandle = HandleManager.findHandle(context, resolvedItem);
            assertEquals("Created and returned handles are identical",
                         handle, rhandle);

        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("Got exception: " + e);
            fail("Exception while running test: " + e);
        }
        finally
        {
            // Don't leave bogus handles (or anything else) around
            if (context != null)
                context.abort();
        }
    }

    /**
     * Create an Item.
     */
    private Item createItem(Context context)
        throws SQLException, AuthorizeException
    {
        Community community = Community.create(context);
        Collection collection = community.createCollection();
        WorkspaceItem wi = WorkspaceItem.create(context, collection);
        return wi.getItem();
    }

    /**
     * Test suite
     */
    public static Test suite()
    {
        return new TestSuite(HandleTest.class);
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
