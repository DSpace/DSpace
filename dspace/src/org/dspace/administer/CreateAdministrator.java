/*
 * RegistryLoader.java
 *
 * $Id$
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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

package org.dspace.administer;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;


/**
 * A command-line tool for creating an initial administrator for setting up
 * a DSpace site.  Prompts for an e-mail address, last name, first name and
 * password from standard input.  An administrator group is then created and
 * the data passed in used to create an e-person in that group.
 * <P>
 * Takes no arguments. 
 *
 * @author   Robert Tansley
 * @version  $Revision$
 */
public class CreateAdministrator
{
    /**
     * For invoking via the command line
     *
     * @param argv  command-line arguments
     */
    public static void main(String argv[])
    {
        Context context = null;
        BufferedReader input = null;

        try
        {
            // For easier reading of typing
            input = new BufferedReader(new InputStreamReader(System.in));

            context = new Context();

            // Of course we aren't an administrator yet so we need to
            // circumvent authorisation
            context.setIgnoreAuthorization(true);

            System.out.println("Creating an initial administrator account");

            boolean dataOK = false;
            
            String email     = null;
            String firstName = null;
            String lastName  = null;
            String password1 = null;
            String password2 = null;

            while (!dataOK)
            {
                System.out.print("E-mail address: ");
                System.out.flush();

                email = input.readLine().trim();

                System.out.print("First name: ");
                System.out.flush();

                firstName = input.readLine().trim();

                System.out.print("Last name: ");
                System.out.flush();

                lastName = input.readLine().trim();

                System.out.println("WARNING: Password will appear on-screen.");
                System.out.print("Password: ");
                System.out.flush();

                password1 = input.readLine().trim();

                System.out.print("Again to confirm: ");
                System.out.flush();

                password2 = input.readLine().trim();

                if (!password1.equals("") && password1.equals(password2))
                {
                    // password OK
                    System.out.print("Is the above data correct? (y or n): ");
                    System.out.flush();
                    
                    String s = input.readLine().trim();
                    
                    if (s.toLowerCase().startsWith("y"))
                    {
                        dataOK = true;
                    }
                }
                else
                {
                    System.out.println("Passwords don't match");
                }
            }
            
            // Find administrator group
            Group admins = Group.find(context, 1);

            if (admins == null)
            {
                System.out.println("Error, no admin group (group 1) found");
                System.exit(1);
            }

            // Create the administrator e-person
            EPerson eperson = EPerson.create(context);
            
            eperson.setEmail    (email    );
            eperson.setLastName (lastName );
            eperson.setFirstName(firstName);
            eperson.setPassword (password1);
            eperson.setCanLogIn (true     );
            eperson.setRequireCertificate(false);
            eperson.setSelfRegistered(false);
            eperson.update();
            
            admins.addMember(eperson);
            admins.update();
            
            context.complete();

            System.out.println("Administrator account created");

            System.exit(0);
        }
        catch (Exception e)
        {
            System.err.println("Exception occurred:" + e);
            e.printStackTrace();
            
            if (context != null)
            {
                context.abort();
            }

            System.exit(1);
        }
    }
}
