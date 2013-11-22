/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.eperson;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.apache.commons.cli.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * Tools for manipulating EPersons and Groups.
 *
 * @author mwood
 */
public class Groomer
{
    /**
     * Command line tool for "grooming" the EPerson collection.
     */
    static public void main(String[] argv)
            throws SQLException
    {
        final String USAGE = "EPerson -verb [option...]";

        OptionGroup verbs = new OptionGroup();
        verbs.setRequired(true);
        verbs.addOption(new Option("h", "help", false, "explain this tool"));
        verbs.addOption(new Option("a", "aging", false, "discover accounts not used recently"));
        verbs.addOption(new Option("u", "unsalted", false, "list accounts with unsalted password hashes"));

        Options options = new Options();
        options.addOptionGroup(verbs);

        options.addOption("b", "last-used-before", true, "date of last login was before this");
        options.addOption("d", "delete", false, "delete matching epersons");

        PosixParser parser = new PosixParser();
        CommandLine command = null;
        try {
            command = parser.parse(options, argv);
        } catch (ParseException ex) {
            System.err.println(ex.getMessage());
            if (! (ex instanceof MissingOptionException))
                new HelpFormatter().printHelp(USAGE, options);
            System.exit(1);
        }

         // Help the user
        if (command.hasOption('h') || command.hasOption('?'))
        {
            new HelpFormatter().printHelp(USAGE, options);
        }
        // Scan for disused accounts
        else if (command.hasOption('a'))
        {
            aging(command);
        }
        // List accounts with unsalted passwords
        else if (command.hasOption('u'))
        {
            findUnsalted(command);
        }
        // Should not happen:  verb option defined but no code!
        else
            System.err.println("Unimplemented verb:  " + verbs.getSelected());
    }

    private static void aging(CommandLine command) throws SQLException
    {
            if (!command.hasOption('b'))
            {
                System.err.println("A last login date is required.");
                System.exit(1);
            }

            Date before = null;
            try {
                before = new SimpleDateFormat().parse(command.getOptionValue('b'));
            } catch (java.text.ParseException ex) {
                System.err.println(ex.getMessage());
                System.exit(1);
            }

            boolean delete = command.hasOption('d');

            Context myContext = new Context();
            final TableRowIterator tri = DatabaseManager.queryTable(myContext,
                    "EPerson",
                    "SELECT eperson_id, email, netid FROM EPerson WHERE last_login < ?",
                    before);

            myContext.ignoreAuthorization();
            for (TableRow row = tri.next(); tri.hasNext(); row = tri.next())
            {
                int id = row.getIntColumn("eperson_id");
                EPerson account = EPerson.find(myContext, id);

                System.out.print(id);
                System.out.print('\t');
                System.out.print(account.getLastActive());
                System.out.print('\t');
                System.out.print(account.getEmail());
                System.out.print('\t');
                System.out.print(account.getNetid());
                System.out.print('\t');
                System.out.print(account.getFullName());
                System.out.println();

                if (delete)
                {
                    List<String> whyNot = account.getDeleteConstraints();
                    if (!whyNot.isEmpty())
                    {
                        System.out.print("\tCannot be deleted; referenced in");
                        for (String table : whyNot)
                        {
                            System.out.print(' ');
                            System.out.print(table);
                        }
                        System.out.println();
                    }
                    else
                        try {
                            account.delete();
                        } catch (AuthorizeException ex) {
                            // XXX SNH
                        } catch (EPersonDeletionException ex) {
                            System.err.println(ex.getMessage());
                        }
                    }
            }

            myContext.restoreAuthSystemState();
            myContext.complete();
    }

    private static void findUnsalted(CommandLine command)
            throws SQLException
    {
        Context myContext = new Context();
        final TableRowIterator tri = DatabaseManager.query(myContext,
                "SELECT email FROM EPerson WHERE password IS NOT NULL AND digest_algorithm IS NULL");
        for (TableRow row = tri.next(); tri.hasNext(); row = tri.next())
            System.out.println(row.getStringColumn("email"));
        myContext.abort(); // No changes to commit
    }
}
