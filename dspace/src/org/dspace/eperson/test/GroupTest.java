/*
 * GroupTest.java
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


package org.dspace.eperson.test;

import junit.framework.*;

import org.dspace.core.*;
import org.dspace.eperson.*;

/**
 * JUnit test for Group API
 *
 * @author  Peter Breton
 * @version $Revision$
 */
public class GroupTest extends TestCase
{
    /**
     * Constructor
     *
     * @param name - The name of the TestCase
     */
    public GroupTest (String name)
    {
        super(name);
    }

    /**
     * JUnit test method
     */
    public void testGroup()
    {
        Context context = null;

        try
        {
            String name = "Test Group created by " + GroupTest.class.getName();

            context = new Context();
            context.setIgnoreAuthorization(true);
            EPerson e1 = EPerson.create(context);
            EPerson e2 = EPerson.create(context);
            Group g = Group.create(context);
            g.setName(name);
            g.addMember(e1);
            g.addMember(e2);
            // Deliberately duplicate e2
            g.addMember(e2);
            // The static isMember method will not work until update
            // has been called
            g.update();

            // Find methods
            assertEquals("Found group by id",
                         Group.find(context, g.getID()), g);
            assertEquals("Found group by name",
                         Group.findByName(context, name), g);

            // Test membership
            assertTrue("EPerson is member of group",
                       g.isMember(e1));
            assertTrue("EPerson is member of group",
                       Group.isMember(context, g.getID(), e1.getID()));
            assertTrue("EPerson is member of group",
                       g.isMember(e2));
            assertTrue("EPerson is member of group",
                       Group.isMember(context, g.getID(), e2.getID()));
            EPerson[] members = g.getMembers();
            assertNotNull("Got group members", members);
            assertEquals("Group size is 2", members.length, 2);

            // Test deletion of eperson
            g.removeMember(e2);
            g.update();
            assertTrue("EPerson deleted from group",
                       ! g.isMember(e2));
            assertTrue("EPerson deleted from group",
                       ! Group.isMember(context, g.getID(), e2.getID()));

            // Test deletion of group
            g.delete();
            assertTrue("EPerson is not a member of deleted group",
                       ! g.isMember(e1));
            assertTrue("EPerson is not a member of deleted group",
                       ! Group.isMember(context, g.getID(), e1.getID()));
            assertNull("Cannot find deleted group by id",
                       Group.find(context, g.getID()));
            assertNull("Cannot find deleted group by name",
                       Group.findByName(context, name));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("Got exception: " + e);
            fail("Exception while running test: " + e);
        }
        finally
        {
            if (context != null)
                context.abort();
        }
    }

    /**
     * Test suite
     */
    public static Test suite()
    {
        return new TestSuite(GroupTest.class);
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
