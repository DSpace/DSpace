/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.administer;

import java.io.Console;
import java.util.Arrays;
import java.util.Locale;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

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
 * @version $Revision$
 */
public final class CreateAdministrator {
    /**
     * DSpace Context object
     */
    private final Context context;

    protected EPersonService ePersonService;
    protected GroupService groupService;

    /**
     * For invoking via the command line. If called with no command line arguments,
     * it will negotiate with the user for the administrator details
     *
     * @param argv the command line arguments given
     * @throws Exception if error
     */
    public static void main(String[] argv)
            throws Exception {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();

        CreateAdministrator ca = new CreateAdministrator();

        options.addOption("e", "email", true, "administrator email address");
        options.addOption("f", "first", true, "administrator first name");
        options.addOption("h", "help", false, "explain create-administrator options");
        options.addOption("l", "last", true, "administrator last name");
        options.addOption("c", "language", true, "administrator language");
        options.addOption("p", "password", true, "administrator password");

        CommandLine line = null;

        try {

            line = parser.parse(options, argv);

        } catch (Exception e) {

            System.out.println(e.getMessage() + "\nTry \"dspace create-administrator -h\" to print help information.");
            System.exit(1);

        }

        if (line.hasOption("e") && line.hasOption("f") && line.hasOption("l") &&
                line.hasOption("c") && line.hasOption("p")) {
            ca.createAdministrator(line.getOptionValue("e"),
                    line.getOptionValue("f"), line.getOptionValue("l"),
                    line.getOptionValue("c"), line.getOptionValue("p"));
        } else if (line.hasOption("h")) {
            String header = "\nA command-line tool for creating an initial administrator for setting up a" +
                    " DSpace site. Unless all the required parameters are passed it will" +
                    " prompt for an e-mail address, last name, first name and password from" +
                    " standard input.. An administrator group is then created and the data passed" +
                    "  in used to create an e-person in that group.\n\n";
            String footer = "\n";
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("dspace create-administrator", header, options, footer, true);
            return;
        } else {
            ca.negotiateAdministratorDetails(line);
        }
    }

    /**
     * constructor, which just creates and object with a ready context
     *
     * @throws Exception if error
     */
    protected CreateAdministrator()
            throws Exception {
        context = new Context();
        try {
            context.getDBConfig();
        } catch (NullPointerException npr) {
            // if database is null, there is no point in continuing. Prior to this exception and catch,
            // NullPointerException was thrown, that wasn't very helpful.
            throw new IllegalStateException("Problem connecting to database. This " +
                    "indicates issue with either network or version (or possibly some other). " +
                    "If you are running this in docker-compose, please make sure dspace-cli was " +
                    "built from the same sources as running dspace container AND that they are in " +
                    "the same project/network.");
        }
        groupService = EPersonServiceFactory.getInstance().getGroupService();
        ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    }

    /**
     * Method which will negotiate with the user via the command line to
     * obtain the administrator's details
     *
     * @throws Exception if error
     */
    protected void negotiateAdministratorDetails(CommandLine line)
            throws Exception {
        Console console = System.console();

        System.out.println("Creating an initial administrator account");

        String email = line.getOptionValue('e');
        String firstName = line.getOptionValue('f');
        String lastName = line.getOptionValue('l');
        String language = I18nUtil.getDefaultLocale().getLanguage();
        ConfigurationService cfg = DSpaceServicesFactory.getInstance().getConfigurationService();
        boolean flag = line.hasOption('p');
        char[] password = null;
        boolean dataOK = line.hasOption('f') && line.hasOption('e') && line.hasOption('l');

        while (!dataOK) {
            System.out.print("E-mail address: ");
            System.out.flush();

            email = console.readLine();
            if (!StringUtils.isBlank(email)) {
                email = email.trim();
            } else {
                System.out.println("Please provide an email address.");
                continue;
            }

            System.out.print("First name: ");
            System.out.flush();

            firstName = console.readLine();

            if (firstName != null) {
                firstName = firstName.trim();
            }

            System.out.print("Last name: ");
            System.out.flush();

            lastName = console.readLine();

            if (lastName != null) {
                lastName = lastName.trim();
            }
            if (cfg.hasProperty("webui.supported.locales")) {
                System.out.println("Select one of the following languages: "
                        + cfg.getProperty("webui.supported.locales"));
                System.out.print("Language: ");
                System.out.flush();

                language = console.readLine();

                if (language != null) {
                    language = language.trim();
                    language = I18nUtil.getSupportedLocale(new Locale(language)).getLanguage();
                }
            }

            System.out.print("Is the above data correct? (y or n): ");
            System.out.flush();

            String s = console.readLine();

            if (s != null) {
                s = s.trim();
                if (s.toLowerCase().startsWith("y")) {
                    dataOK = true;
                }
            }

        }
        if (!flag) {
            password = getPassword(console);
            if (password == null) {
                return;
            }
        } else {
            password = line.getOptionValue("p").toCharArray();
        }
        // if we make it to here, we are ready to create an administrator
        createAdministrator(email, firstName, lastName, language, String.valueOf(password));

    }

    private char[] getPassword(Console console) {
        char[] password1 = null;
        char[] password2 = null;
        System.out.println("Password will not display on screen.");
        System.out.print("Password: ");
        System.out.flush();

        password1 = console.readPassword();

        System.out.print("Again to confirm: ");
        System.out.flush();

        password2 = console.readPassword();

        // TODO real password validation
        if (password1.length > 1 && Arrays.equals(password1, password2)) {
            // password OK
            Arrays.fill(password2, ' ');
            return password1;
        } else {
            System.out.println("Passwords don't match");
            return null;
        }
    }

    /**
     * Create the administrator with the given details. If the user
     * already exists then they are simply upped to administrator status
     *
     * @param email    the email for the user
     * @param first    user's first name
     * @param last     user's last name
     * @param language preferred language
     * @param pw       desired password
     * @throws Exception if error
     */
    protected void createAdministrator(String email, String first, String last,
            String language, String pw)
            throws Exception {
        // Of course we aren't an administrator yet so we need to
        // circumvent authorisation
        context.turnOffAuthorisationSystem();

        // Find administrator group
        Group admins = groupService.findByName(context, Group.ADMIN);

        if (admins == null) {
            throw new IllegalStateException("Error, no admin group (group 1) found");
        }

        // Create the administrator e-person
        EPerson eperson = ePersonService.findByEmail(context, email);

        // check if the email belongs to a registered user,
        // if not create a new user with this email
        if (eperson == null) {
            eperson = ePersonService.create(context);
            eperson.setEmail(email);
            eperson.setCanLogIn(true);
            eperson.setRequireCertificate(false);
            eperson.setSelfRegistered(false);
        }

        eperson.setLastName(context, last);
        eperson.setFirstName(context, first);
        eperson.setLanguage(context, language);
        ePersonService.setPassword(eperson, pw);
        ePersonService.update(context, eperson);

        groupService.addMember(context, admins, eperson);
        groupService.update(context, admins);

        context.complete();

        System.out.println("Administrator account created");
    }
}
