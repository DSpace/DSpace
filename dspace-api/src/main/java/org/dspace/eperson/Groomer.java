/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.eperson;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;

/**
 * Tools for manipulating EPersons and Groups.
 *
 * @author mwood
 */
public class Groomer {
    private static final ThreadLocal<DateFormat> dateFormat = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return DateFormat.getDateInstance(DateFormat.SHORT);
        }
    };

    private static final EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

    /**
     * Default constructor
     */
    private Groomer() { }

    /**
     * Command line tool for "grooming" the EPerson collection.
     *
     * @param argv the command line arguments given
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    static public void main(String[] argv)
        throws SQLException {
        final String USAGE = "Groomer -verb [option...]";

        OptionGroup verbs = new OptionGroup();
        verbs.setRequired(true);
        verbs.addOption(new Option("h", "help", false, "explain this tool"));
        verbs.addOption(new Option("a", "aging", false, "discover accounts not used recently"));
        verbs.addOption(new Option("u", "unsalted", false, "list accounts with unsalted password hashes"));

        Options options = new Options();
        options.addOptionGroup(verbs);

        options.addOption("b", "last-used-before", true,
                          "date of last login was before this (for example:  "
                              + dateFormat.get().format(Calendar.getInstance().getTime())
                              + ')');
        options.addOption("d", "delete", false, "delete matching epersons");

        CommandLineParser parser = new DefaultParser();
        CommandLine command = null;
        try {
            command = parser.parse(options, argv);
        } catch (ParseException ex) {
            System.err.println(ex.getMessage());
            if (!(ex instanceof MissingOptionException)) {
                new HelpFormatter().printHelp(USAGE, options);
            }
            System.exit(1);
        }

        // Help the user
        if (null == command || command.hasOption('h') || command.hasOption('?')) {
            new HelpFormatter().printHelp(USAGE, options);
            System.exit(0);
        } else if (command.hasOption('a')) {
            // Scan for disused accounts
            aging(command);
        } else if (command.hasOption('u')) {
            // List accounts with unsalted passwords
            findUnsalted();
        } else {
            // Should not happen:  verb option defined but no code!
            System.err.println("Unimplemented verb:  " + verbs.getSelected());
        }
    }

    /**
     * Find and optionally delete accounts not logged in recently.
     *
     * @param command a parsed command line.
     * @throws SQLException from callees.
     */
    private static void aging(CommandLine command) throws SQLException {
        if (!command.hasOption('b')) {
            System.err.println("A last login date is required.");
            System.exit(1);
        }

        Date before = null;
        try {
            before = dateFormat.get().parse(command.getOptionValue('b'));
        } catch (java.text.ParseException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }

        boolean delete = command.hasOption('d');

        Context myContext = new Context();
        List<EPerson> epeople = ePersonService.findNotActiveSince(myContext, before);

        myContext.turnOffAuthorisationSystem();
        for (EPerson account : epeople) {
            System.out.print(account.getID());
            System.out.print('\t');
            System.out.print(account.getLastActive());
            System.out.print('\t');
            System.out.print(account.getEmail());
            System.out.print('\t');
            System.out.print(account.getNetid());
            System.out.print('\t');
            System.out.print(account.getFullName());
            System.out.println();

            if (delete) {
                try {
                    ePersonService.delete(myContext, account);
                } catch (AuthorizeException | IOException ex) {
                    System.err.println(ex.getMessage());
                }
            }
        }

        myContext.restoreAuthSystemState();
        myContext.complete();
    }

    /**
     * List accounts having no password salt.
     *
     * @throws SQLException if database error
     */
    private static void findUnsalted()
        throws SQLException {
        Context myContext = new Context();
        List<EPerson> ePersons = ePersonService.findUnsalted(myContext);
        for (EPerson ePerson : ePersons) {
            System.out.println(ePerson.getEmail());
        }
        myContext.abort(); // No changes to commit
    }
}
