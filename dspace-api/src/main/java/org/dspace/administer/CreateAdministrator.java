/*
 * CreateAdministrator.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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
import java.util.Locale;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.dao.EPersonDAO;
import org.dspace.eperson.dao.EPersonDAOFactory;
import org.dspace.eperson.Group;

/**
 * A command-line tool for creating an initial administrator for setting up a
 * DSpace site. Prompts for an e-mail address, last name, first name and
 * password from standard input. An administrator group is then created and the
 * data passed in used to create an e-person in that group.
 * <P>
 * Alternatively, it can be used to take the email, first name, last name and
 * desired password as arguments thus:
 * 
 * CreateAdministrator -e [email] -f [first name] -l [last name] -p [password]
 * 
 * This is particularly convenient for automated deploy scripts that require an 
 * initial administrator, for example, before deployment can be completed
 * 
 * @author Robert Tansley
 * @author Richard Jones
 * 
 * @version $Revision$
 */
public class CreateAdministrator
{
	/** DSpace Context object */
	private Context context;
    private EPersonDAO dao;
	
    /**
     * For invoking via the command line.  If called with no command line arguments,
     * it will negotiate with the user for the administrator details
     * 
     * @param argv
     *            command-line arguments
     */
    public static void main(String[] argv)
    	throws Exception
    {
    	CommandLineParser parser = new PosixParser();
    	Options options = new Options();
    	
    	CreateAdministrator ca = new CreateAdministrator();
    	
    	options.addOption("e", "email", true, "administrator email address");
    	options.addOption("f", "first", true, "administrator first name");
    	options.addOption("l", "last", true, "administrator lastt name");
    	options.addOption("c", "language", true, "administrator language");
    	options.addOption("p", "password", true, "administrator password");
    	
    	CommandLine line = parser.parse(options, argv);
    	
    	if (line.hasOption("e") && line.hasOption("f") && line.hasOption("l") &&
    			line.hasOption("c") && line.hasOption("p"))
    	{
    		ca.createAdministrator(line.getOptionValue("e"),
    				line.getOptionValue("f"), line.getOptionValue("l"),
    				line.getOptionValue("c"), line.getOptionValue("p"));
    	}
    	else
    	{
    		ca.negotiateAdministratorDetails();
    	}
    }
    
    /** 
     * constructor, which just creates and object with a ready context
     * 
     * @throws Exception
     */
    private CreateAdministrator()
    	throws Exception
    {
    	context = new Context();
        dao = EPersonDAOFactory.getInstance(context);
    }
    
    /**
     * Method which will negotiate with the user via the command line to 
     * obtain the administrator's details
     * 
     * @throws Exception
     */
    private void negotiateAdministratorDetails()
    	throws Exception
    {
    	// For easier reading of typing
    	BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    	
    	System.out.println("Creating an initial administrator account");
    	
    	boolean dataOK = false;
    	
    	String email = null;
    	String firstName = null;
    	String lastName = null;
    	String password1 = null;
    	String password2 = null;
    	String language = I18nUtil.DEFAULTLOCALE.getLanguage();
    	
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
   		
            if (ConfigurationManager.getProperty("webui.supported.locales") != null)
            {
                System.out.println("Select one of the following languages: " + ConfigurationManager.getProperty("webui.supported.locales"));
                System.out.print("Language: ");
                System.out.flush();
            
    		    language = input.readLine().trim();
    		    language = I18nUtil.getSupportedLocale(new Locale(language)).getLanguage();
            }
            
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
    	
    	// if we make it to here, we are ready to create an administrator
    	createAdministrator(email, firstName, lastName, language, password1);
    }
    
    /**
     * Create the administrator with the given details.  If the user
     * already exists then they are simply upped to administrator status
     * 
     * @param email	the email for the user
     * @param first	user's first name
     * @param last	user's last name
     * @param pw	desired password
     * 
     * @throws Exception
     */
    private void createAdministrator(String email, String first, String last,
    		String language, String pw)
    	throws Exception
    {
    	// Of course we aren't an administrator yet so we need to
    	// circumvent authorisation
    	context.setIgnoreAuthorization(true);
    	
    	// Find administrator group
    	Group admins = Group.find(context, 1);
    	
    	if (admins == null)
    	{
    		throw new Exception("Error, no admin group (group 1) found");
    	}
    	
    	// Create the administrator e-person
        EPerson eperson = EPerson.findByEmail(context,email);
        
        // check if the email belongs to a registered user,
        // if not create a new user with this email
        if (eperson == null)
        {
            eperson = dao.create();
            eperson.setEmail(email);
            eperson.setCanLogIn(true);
            eperson.setRequireCertificate(false);
            eperson.setSelfRegistered(false);
        }
    	
    	eperson.setLastName(last);
    	eperson.setFirstName(first);
    	eperson.setLanguage(language);
    	eperson.setPassword(pw);
    	dao.update(eperson);
    	
    	admins.addMember(eperson);
    	admins.update();
    	
    	context.complete();
    	
    	System.out.println("Administrator account created");
    }
}
