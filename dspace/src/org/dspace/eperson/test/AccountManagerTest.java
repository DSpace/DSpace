/*
 * AccountManagerTest
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

import java.io.IOException;
import java.sql.SQLException;
import javax.mail.MessagingException;

import junit.framework.*;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.*;
import org.dspace.storage.rdbms.*;


/**
 * JUnit test for AccountManager
 *
 * @author  Peter Breton
 * @version $Revision$
 */
public class AccountManagerTest extends TestCase
{
    /**
     * Constructor
     */
    public AccountManagerTest (String name)
    {
        super(name);
    }

    ////////////////////////////////////////
    // Test methods
    ////////////////////////////////////////

    /**
     * JUnit test method
     */
    public void testAccountManager()
    {
        // NOTE: This test makes some assumptions about the internal API,
        // namely:
        //
        //   All the real processing for the official methods
        //   sendRegistrationInfo and sendForgotPasswordInfo is done
        //   by the internal sendInfo method.

        Context context = null;

        try
        {
            context = new Context();

            // An unlikely email address
            String email = "foo@bar.testing.DO_NOT_USE";

            EPerson ep = createEPerson(context, email);
            assertNotNull("Got an EPerson for testing", ep);
            int eid = ep.getID();

            ////////////////////
            // Registration
            ////////////////////

            // Create callback data for registration
            TableRow registration_data =
                AccountManagerShim.publicSendInfo(context, email, true, false);

            assertNotNull("Created callback data for registration",
                          registration_data);

            // Get the EPerson from the Callback
            EPerson registration_eperson =
                AccountManager.getEPerson
                (context, registration_data.getStringColumn("token"));

            assertNotNull("Got an eperson from the callback",
                registration_eperson);
            assertEquals("Got the same eperson that we registered",
                registration_eperson.getID(), eid);

            // Cleanup registration
            String registration_token = registration_data.getStringColumn("token");

            AccountManager.deleteToken(context, registration_token);
            assertNull("No EPerson corresponding to token",
                AccountManager.getEPerson(context, registration_token));

            ////////////////////
            // Change password
            ////////////////////

            // Create callback data for changing passwords
            TableRow change_password_data =
                AccountManagerShim.publicSendInfo(context, email, false, false);
            assertNotNull("Created callback data for changing password",
                change_password_data);

            // Get the EPerson from the Callback
            EPerson change_password_eperson =
                AccountManager.getEPerson
                (context, change_password_data.getStringColumn("token"));

            assertNotNull("Got an eperson from the callback",
                change_password_eperson);
            assertEquals("Got the same eperson that we registered",
                         change_password_eperson.getID(), eid);

            // Clean up change_password
            String change_password_token = change_password_data.getStringColumn("token");

            AccountManager.deleteToken(context, change_password_token);
            assertNull("No EPerson corresponding to token",
                AccountManager.getEPerson(context, change_password_token));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("Got exception: " + e);
            fail("Exception while running test: " + e);
        }
        finally
        {
            // Do not leave stray data around
            if (context != null)
                context.abort();
        }
    }

    /**
     * Find or create an eperson with the given email address
     */
    private EPerson createEPerson (Context context, String email)
        throws SQLException, AuthorizeException
    {
        EPerson ep = EPerson.findByEmail(context, email);

        if (ep != null)
            return null;

        ep = EPerson.create(context);
        ep.setEmail(email);
        ep.setFirstName("Test");
        ep.setLastName("User");
        ep.update();
        return ep;
    }

    ////////////////////////////////////////
    // Static methods
    ////////////////////////////////////////

    /**
     * Test suite
     */
    public static Test suite()
    {
        return new TestSuite(AccountManagerTest.class);
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

class AccountManagerShim extends AccountManager
{
    public static TableRow publicSendInfo(Context context,
                                          String email,
                                          boolean isRegister,
                                          boolean send)
        throws SQLException, IOException, MessagingException, AuthorizeException
    {
        return sendInfo(context, email, isRegister, send);
    }
}
