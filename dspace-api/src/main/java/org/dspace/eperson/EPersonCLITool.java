/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import org.apache.commons.cli.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;

public class EPersonCLITool {

    /*
     * Commandline tool for manipulating EPersons.
     */

    private static final Option VERB_ADD = new Option("a", "add", false, "create a new EPerson");
    private static final Option VERB_DELETE = new Option("d", "delete", false, "delete an existing EPerson");
    private static final Option VERB_LIST = new Option("L", "list", false, "list EPersons");
    private static final Option VERB_MODIFY = new Option("M", "modify", false, "modify an EPerson");

    private static final Option OPT_GIVENNAME = new Option("g", "givenname", true, "the person's actual first or personal name");
    private static final Option OPT_SURNAME = new Option("s", "surname", true, "the person's actual last or family name");
    private static final Option OPT_PHONE = new Option("t", "telephone", true, "telephone number, empty for none");
    private static final Option OPT_LANGUAGE = new Option("l", "language", true, "the person's preferred language");
    private static final Option OPT_REQUIRE_CERTIFICATE = new Option("c", "requireCertificate", true, "if 'true', an X.509 certificate will be required for login");
    private static final Option OPT_CAN_LOGIN = new Option("C", "canLogIn", true, "'true' if the user can log in");

    private static final Option OPT_EMAIL = new Option("m", "email", true, "the user's email address, empty for none");
    private static final Option OPT_NETID = new Option("n", "netid", true, "network ID associated with the person, empty for none");

    private static final Option OPT_NEW_EMAIL = new Option("i", "newEmail", true, "new email address");
    private static final Option OPT_NEW_NETID = new Option("I", "newNetid", true, "new network ID");

    private static final EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

    /**
     * Tool for manipulating user accounts.
     */
    public static void main(String argv[])
            throws ParseException, SQLException, AuthorizeException {
        final OptionGroup VERBS = new OptionGroup();
        VERBS.addOption(VERB_ADD);
        VERBS.addOption(VERB_DELETE);
        VERBS.addOption(VERB_LIST);
        VERBS.addOption(VERB_MODIFY);

        final Options globalOptions = new Options();
        globalOptions.addOptionGroup(VERBS);
        globalOptions.addOption("h", "help", false, "explain options");

        GnuParser parser = new GnuParser();
        CommandLine command = parser.parse(globalOptions, argv, true);

        Context context = new Context();

        // Disable authorization since this only runs from the local commandline.
        context.turnOffAuthorisationSystem();

        int status = 0;
        if (command.hasOption(VERB_ADD.getOpt()))
        {
            status = cmdAdd(context, argv);
        }
        else if (command.hasOption(VERB_DELETE.getOpt()))
        {
            status = cmdDelete(context, argv);
        }
        else if (command.hasOption(VERB_MODIFY.getOpt()))
        {
            status = cmdModify(context, argv);
        }
        else if (command.hasOption(VERB_LIST.getOpt()))
        {
            status = cmdList(context, argv);
        }
        else if (command.hasOption('h'))
        {
            new HelpFormatter().printHelp("user [options]", globalOptions);
        }
        else
        {
            System.err.println("Unknown operation.");
            new HelpFormatter().printHelp("user [options]", globalOptions);
            context.abort();
            status = 1;
            throw new IllegalArgumentException();
        }

        if (context.isValid())
        {
            try {
                context.complete();
            } catch (SQLException ex) {
                System.err.println(ex.getMessage());
            }
        }
    }

    /** Command to create an EPerson. */
    private static int cmdAdd(Context context, String[] argv) throws AuthorizeException, SQLException {
        Options options = new Options();

        options.addOption(VERB_ADD);

        final OptionGroup identityOptions = new OptionGroup();
        identityOptions.addOption(OPT_EMAIL);
        identityOptions.addOption(OPT_NETID);

        options.addOptionGroup(identityOptions);

        options.addOption(OPT_GIVENNAME);
        options.addOption(OPT_SURNAME);
        options.addOption(OPT_PHONE);
        options.addOption(OPT_LANGUAGE);
        options.addOption(OPT_REQUIRE_CERTIFICATE);

        Option option = new Option("p", "password", true, "password to match the EPerson name");
        options.addOption(option);

        options.addOption("h", "help", false, "explain --add options");

        // Rescan the command for more details.
        GnuParser parser = new GnuParser();
        CommandLine command;
        try {
            command = parser.parse(options, argv);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            return 1;
        }

        if (command.hasOption('h'))
        {
            new HelpFormatter().printHelp("user --add [options]", options);
            return 0;
        }

        // Check that we got sufficient credentials to define a user.
        if ((!command.hasOption(OPT_EMAIL.getOpt())) && (!command.hasOption(OPT_NETID.getOpt())))
        {
            System.err.println("You must provide an email address or a netid to identify the new user.");
            return 1;
        }

        if (!command.hasOption('p'))
        {
            System.err.println("You must provide a password for the new user.");
            return 1;
        }

        // Create!
        EPerson eperson = null;
        try {
            eperson = ePersonService.create(context);
        } catch (SQLException ex) {
            context.abort();
            System.err.println(ex.getMessage());
            return 1;
        } catch (AuthorizeException ex) { /* XXX SNH */ }
        eperson.setCanLogIn(true);
        eperson.setSelfRegistered(false);

        eperson.setEmail(command.getOptionValue(OPT_EMAIL.getOpt()));
        eperson.setFirstName(context, command.getOptionValue(OPT_GIVENNAME.getOpt()));
        eperson.setLastName(context, command.getOptionValue(OPT_SURNAME.getOpt()));
        eperson.setLanguage(context, command.getOptionValue(OPT_LANGUAGE.getOpt(),
                Locale.getDefault().getLanguage()));
        ePersonService.setMetadata(context, eperson, "phone", command.getOptionValue(OPT_PHONE.getOpt()));
        eperson.setNetid(command.getOptionValue(OPT_NETID.getOpt()));
        ePersonService.setPassword(eperson, command.getOptionValue('p'));
        if (command.hasOption(OPT_REQUIRE_CERTIFICATE.getOpt()))
        {
            eperson.setRequireCertificate(Boolean.valueOf(command.getOptionValue(
                    OPT_REQUIRE_CERTIFICATE.getOpt())));
        }
        else
        {
            eperson.setRequireCertificate(false);
        }

        try {
            ePersonService.update(context, eperson);
            context.complete();
            System.out.printf("Created EPerson %s\n", eperson.getID().toString());
        } catch (SQLException ex) {
            context.abort();
            System.err.println(ex.getMessage());
            return 1;
        } catch (AuthorizeException ex) { /* XXX SNH */ }

        return 0;
    }

    /** Command to delete an EPerson. */
    private static int cmdDelete(Context context, String[] argv)
    {
        Options options = new Options();

        options.addOption(VERB_DELETE);

        final OptionGroup identityOptions = new OptionGroup();
        identityOptions.addOption(OPT_EMAIL);
        identityOptions.addOption(OPT_NETID);

        options.addOptionGroup(identityOptions);

        options.addOption("h", "help", false, "explain --delete options");

        GnuParser parser = new GnuParser();
        CommandLine command;
        try {
            command = parser.parse(options, argv);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            return 1;
        }

        if (command.hasOption('h'))
        {
            new HelpFormatter().printHelp("user --delete [options]", options);
            return 0;
        }

        // Delete!
        EPerson eperson = null;
        try {
            if (command.hasOption(OPT_NETID.getOpt()))
            {
                eperson = ePersonService.findByNetid(context, command.getOptionValue(OPT_NETID.getOpt()));
            }
            else if (command.hasOption(OPT_EMAIL.getOpt()))
            {
                eperson = ePersonService.findByEmail(context, command.getOptionValue(OPT_EMAIL.getOpt()));
            }
            else
            {
                System.err.println("You must specify the user's email address or netid.");
                return 1;
            }
        } catch (SQLException e) {
            System.err.append(e.getMessage());
            return 1;
        }

        if (null == eperson)
        {
            System.err.println("No such EPerson");
            return 1;
        }

        try {
            ePersonService.delete(context, eperson);
            context.complete();
            System.out.printf("Deleted EPerson %s\n", eperson.getID().toString());
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
            return 1;
        } catch (AuthorizeException ex) {
            System.err.println(ex.getMessage());
            return 1;
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
            return 1;
        }

        return 0;
    }

    /** Command to modify an EPerson. */
    private static int cmdModify(Context context, String[] argv) throws AuthorizeException, SQLException {
        Options options = new Options();

        options.addOption(VERB_MODIFY);

        final OptionGroup identityOptions = new OptionGroup();
        identityOptions.addOption(OPT_EMAIL);
        identityOptions.addOption(OPT_NETID);

        options.addOptionGroup(identityOptions);

        options.addOption(OPT_GIVENNAME);
        options.addOption(OPT_SURNAME);
        options.addOption(OPT_PHONE);
        options.addOption(OPT_LANGUAGE);
        options.addOption(OPT_REQUIRE_CERTIFICATE);

        options.addOption(OPT_CAN_LOGIN);
        options.addOption(OPT_NEW_EMAIL);
        options.addOption(OPT_NEW_NETID);

        options.addOption("h", "help", false, "explain --modify options");

        GnuParser parser = new GnuParser();
        CommandLine command;
        try {
            command = parser.parse(options, argv);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            return 1;
        }

        if (command.hasOption('h'))
        {
            new HelpFormatter().printHelp("user --modify [options]", options);
            return 0;
        }

        // Modify!
        EPerson eperson = null;
        try {
            if (command.hasOption(OPT_NETID.getOpt()))
            {
                eperson = ePersonService.findByNetid(context, command.getOptionValue(OPT_NETID.getOpt()));
            }
            else if (command.hasOption(OPT_EMAIL.getOpt()))
            {
                eperson = ePersonService.findByEmail(context, command.getOptionValue(OPT_EMAIL.getOpt()));
            }
            else
            {
                System.err.println("No EPerson selected");
                return 1;
            }
        } catch (SQLException e) {
            System.err.append(e.getMessage());
            return 1;
        }

        boolean modified = false;
        if (null == eperson)
        {
            System.err.println("No such EPerson");
            return 1;
        }
        else
        {
            if (command.hasOption(OPT_NEW_EMAIL.getOpt()))
            {
                eperson.setEmail(command.getOptionValue(OPT_NEW_EMAIL.getOpt()));
                modified = true;
            }
            if (command.hasOption(OPT_NEW_NETID.getOpt()))
            {
                eperson.setNetid(command.getOptionValue(OPT_NEW_NETID.getOpt()));
                modified = true;
            }
            if (command.hasOption(OPT_GIVENNAME.getOpt()))
            {
                eperson.setFirstName(context, command.getOptionValue(OPT_GIVENNAME.getOpt()));
                modified = true;
            }
            if (command.hasOption(OPT_SURNAME.getOpt()))
            {
                eperson.setLastName(context, command.getOptionValue(OPT_SURNAME.getOpt()));
                modified = true;
            }
            if (command.hasOption(OPT_PHONE.getOpt()))
            {
                ePersonService.setMetadata(context, eperson, "phone", command.getOptionValue(OPT_PHONE.getOpt()));
                modified = true;
            }
            if (command.hasOption(OPT_LANGUAGE.getOpt()))
            {
                eperson.setLanguage(context, command.getOptionValue(OPT_LANGUAGE.getOpt()));
                modified = true;
            }
            if (command.hasOption(OPT_REQUIRE_CERTIFICATE.getOpt()))
            {
                eperson.setRequireCertificate(Boolean.valueOf(command.getOptionValue(
                        OPT_REQUIRE_CERTIFICATE.getOpt())));
                modified = true;
            }
            if (command.hasOption(OPT_CAN_LOGIN.getOpt()))
            {
                eperson.setCanLogIn(Boolean.valueOf(command.getOptionValue(OPT_CAN_LOGIN.getOpt())));
                modified = true;
            }
            if (modified)
            {
                try {
                    ePersonService.update(context, eperson);
                    context.complete();
                    System.out.printf("Modified EPerson %s\n", eperson.getID().toString());
                } catch (SQLException ex) {
                    context.abort();
                    System.err.println(ex.getMessage());
                    return 1;
                } catch (AuthorizeException ex) { /* XXX SNH */ }
            }
            else
            {
                System.out.println("No changes.");
            }
        }

        return 0;
    }

    /** Command to list known EPersons. */
    private static int cmdList(Context context, String[] argv)
    {
        // XXX ideas:
        // specific user/netid
        // wild or regex match user/netid
        // select details (pseudo-format string)
        try {
            for (EPerson person : ePersonService.findAll(context, EPerson.EMAIL))
            {
                System.out.printf("%s\t%s/%s\t%s, %s\n",
                        person.getID().toString(),
                        person.getEmail(),
                        person.getNetid(),
                        person.getLastName(), person.getFirstName()); // TODO more user details
            }
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
            return 1;
        }

        return 0;
    }
}
