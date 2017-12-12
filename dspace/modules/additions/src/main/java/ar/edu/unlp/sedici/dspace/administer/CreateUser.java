/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package ar.edu.unlp.sedici.dspace.administer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Locale;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import ar.edu.unlp.sedici.dspace.utils.*;

/**
 * A command-line tool for creating an initial user for setting up a
 * DSpace site. Prompts for an e-mail address, last name, first name and
 * password from standard input. An user group is then created and the
 * data passed in used to create an e-person in that group.
 * <P>
 * Alternatively, it can be used to take the email, first name, last name and
 * desired password as arguments thus:
 * 
 * CreateUser -e [email] -f [first name] -l [last name] -p [password]
 * 
 * This is particularly convenient for automated deploy scripts that require an 
 * initial user, for example, before deployment can be completed
 * 
 * @author Robert Tansley
 * @author Richard Jones
 * 
// * @file $reference CreateAdministrator
 * 
 * @version $Revision: 5844 $
 */
public final class CreateUser
{
	/** DSpace Context object */
	private Context context;
	static final Logger logger = Logger.getLogger(CreateUser.class);
    /**
     * For invoking via the command line.  If called with no command line arguments,
     * it will negotiate with the user for the user details
     * 
     * @param argv
     *            command-line arguments
     */
	
	
    public static void main(String[] argv)
    	throws Exception
    {
    	CommandLineParser parser = new PosixParser();
    	Options options = new Options();
    	
    	CreateUser cu = new CreateUser();
    	
    	options.addOption("e", "email", true, "user email address");
    	options.addOption("f", "first", true, "user first name");
    	options.addOption("l", "last", true, "user last name");
    	options.addOption("c", "language", true, "user language");
    	options.addOption("p", "password", true, "user password");
    	options.addOption("g", "group", true, "user group");
    	
    	CommandLine line = parser.parse(options, argv);
    	
    	if (line.hasOption("e") && line.hasOption("f") && line.hasOption("l") &&
    			line.hasOption("c") && line.hasOption("p"))
    	{
    		cu.createUser(line.getOptionValue("e"),
    				line.getOptionValue("f"), line.getOptionValue("l"),
    				line.getOptionValue("c"), line.getOptionValue("p"),"", line.getOptionValue("g"));
    	}
    	else
    	{
    		cu.negotiateUserDetails();
    	}
    }
    
    /** 
     * constructor, which just creates and object with a ready context
     * 
     * @throws Exception
     */
    public CreateUser()
    	throws Exception
    {
    	context = new Context();
    	
    }
    
    /**
     * Method which will negotiate with the user via the command line to 
     * obtain the user's details
     * 
     * @throws Exception
     */
    private void negotiateUserDetails()
    	throws Exception
    {
    	// For easier reading of typing
    	BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
    	
    	System.out.println("Creating an initial user account");
    	
    	boolean dataOK = false;
    	
    	String email = null;
    	String firstName = null;
    	String lastName = null;
    	String password1 = null;
    	String password2 = null;
		String groupName = null;

    	String language = I18nUtil.DEFAULTLOCALE.getLanguage();
    	
    	while (!dataOK)
    	{
    		System.out.print("E-mail address: ");
    		System.out.flush();
    		
    		email = input.readLine();
            if (email != null)
            {
                email = email.trim();
            }
    		
    		System.out.print("First name: ");
    		System.out.flush();
    		
    		firstName = input.readLine();

            if (firstName != null)
            {
                firstName = firstName.trim();
            }
    		
    		System.out.print("Last name: ");
    		System.out.flush();
    		
    		lastName = input.readLine();

            if (lastName != null)
            {
                lastName = lastName.trim();
            }
   		
            if (ConfigurationManager.getProperty("webui.supported.locales") != null)
            {
                System.out.println("Select one of the following languages: " + ConfigurationManager.getProperty("webui.supported.locales"));
                System.out.print("Language: ");
                System.out.flush();
            
    		    language = input.readLine();

                if (language != null)
                {
                    language = language.trim();
                    language = I18nUtil.getSupportedLocale(new Locale(language)).getLanguage();
                }
            }
            
    		System.out.println("WARNING: Password will appear on-screen.");
    		System.out.print("Password: ");
    		System.out.flush();
    		
    		password1 = input.readLine();

            if (password1 != null)
            {
                password1 = password1.trim();
            }
    		
    		System.out.print("Again to confirm: ");
    		System.out.flush();
    		
    		password2 = input.readLine();

            if (password2 != null)
            {
                password2 = password2.trim();
            }
    		

    		System.out.print("Default Group (empty for anonymous): ");
    		System.out.flush();
    		groupName = input.readLine();

            if (groupName != null)
            {
            	groupName = groupName.trim();
            }
            
    		if (!StringUtils.isEmpty(password1) && StringUtils.equals(password1, password2))
    		{
    			// password OK
    			System.out.print("Is the above data correct? (y or n): ");
    			System.out.flush();
    			
    			String s = input.readLine();

                if (s != null)
                {
                    s = s.trim();
                    if (s.toLowerCase().startsWith("y"))
                    {
                        dataOK = true;
                    }
                }
    		}
    		else
    		{
    			System.out.println("Passwords don't match");
    		}
    	}
    	
    	// if we make it to here, we are ready to create an user
    	createUser(email, firstName, lastName, language, password1,"", groupName);
    }
    
    /**
     * Create the user with the given details.  If the user
     * already exists then they are simply upped to user status
     * 
     * @param email	the email for the user
     * @param first	user's first name
     * @param last	user's last name
     * @param ps	desired password
     * 
     * @throws Exception
     */
    public void createUser(String email, String first, String last,
    		String language, String pw, String sedici_eperson_id, String groupName)
    	throws Exception
    {
    	// Of course we aren't an user yet so we need to
    	// circumvent authorisation
    	context.turnOffAuthorisationSystem();
    	
    	
    	// Find user group
    	Group group;
    	if (groupName == null || "".equals(groupName)){
    		groupName = "0 {anonymous}";
    		group = Group.find(context, 0);
    	}else if ("1".equals(groupName) || "administrator".equals(groupName)){
    		groupName = "1 {administrator}";
    		group = Group.find(context, 1);
    	}else{
    		group = Group.findByName(context, groupName);
    	}
    	if (group == null)
    	{
    		context.abort();
    		throw new IllegalStateException("Error, no group ("+groupName+") found");
    	}
    	// Create the user e-person
        if (email==""){
        	logger.error("El usuario no tiene mail");
        	context.abort();
        	throw new IllegalStateException("Error, Error: El usuario no tiene mail");
        }else{
        	if (!Utils.isEmail(email)){
        		logger.error("El email no es valido para el usuario: "+sedici_eperson_id);
        		context.abort();
        		throw new IllegalStateException("Error, Error: El email no es valido para el usuario: "+sedici_eperson_id);
        	}
        }
        
        if (last==""){
        	logger.error("El usuario "+sedici_eperson_id+" no tiene el apellido Cargado");
        	context.abort();
        	throw new IllegalStateException("Error, Error: El usuario "+sedici_eperson_id+" no tiene el apellido Cargado");
        }
        if (first==""){
        	logger.info("Informacion: La cuenta de usuario no tiene nombre cargado");
        	
        }
    	EPerson eperson = EPerson.findByEmail(context,email);
        
        // check if the email belongs to a registered user,
        // if not create a new user with this email
        if (eperson == null)
        {
            eperson = EPerson.create(context);
            eperson.setEmail(email);
            eperson.setCanLogIn(false);
            if ((pw!="")&&(pw.length()>5)){
            	eperson.setCanLogIn(true);	
            	eperson.setPassword(pw);
            }else{
            	logger.info("Informacion: No tenia Password Cargado o la password tiene longitud menor a 6 caracteres");
            	
            }
            
            eperson.setRequireCertificate(false);
            eperson.setSelfRegistered(false);
            eperson.setLastName(last);
        	eperson.setFirstName(first);
        	eperson.setLanguage(language);
        	
        	
        	eperson.update();
        	
        	group.addMember(eperson);
        	group.update();
        	
        	context.complete();
        	logger.info("La cuenta de usuario ha sido creada con dspace_eperson_id ="+eperson.getID()+" y sedici_eperson_id ="+sedici_eperson_id+ " y group "+groupName);
            
        	
        }else{
        	context.abort();
        	throw new IllegalStateException("Error, El usuario "+sedici_eperson_id+" tiene un email que ya se encuentra registrado");
        }
    	
    	
    }
}
