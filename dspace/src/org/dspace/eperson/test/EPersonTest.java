/*
 * EPersonTest.java
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
 * JUnit test for EPerson API
 *
 * @author  Peter Breton
 * @version $Revision$
 */
public class EPersonTest extends TestCase
{
    /**
     * Constructor
     *
     * @param name - The name of the TestCase
     */
    public EPersonTest (String name)
    {
        super(name);
    }

    /**
     * JUnit test method
     */
    public void testEPerson()
    {
        Context context = null;
        String email = "a.very.unlikely.address@foo.bar";

        try
        {
            context = new Context();
            context.setIgnoreAuthorization(true);
            EPerson e1 = EPerson.create(context);
            e1.setEmail(email);
            e1.setFirstName("Bob");
            e1.setLastName("Brannigan");
            e1.setPassword("password");
            e1.setCanLogIn(true);
            e1.setRequireCertificate(false);
            e1.update();

            // Find methods
            assertEquals("Found eperson by id",
                         EPerson.find(context, e1.getID()), e1);
            assertEquals("Found eperson by name",
                         EPerson.findByEmail(context, email), e1);

            // Test deletion of eperson
            e1.delete();
            assertNull("Cannot find deleted eperson by id",
                       EPerson.find(context, e1.getID()));
            assertNull("Cannot find deleted eperson by email",
                       EPerson.findByEmail(context, email));
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
        return new TestSuite(EPersonTest.class);
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
