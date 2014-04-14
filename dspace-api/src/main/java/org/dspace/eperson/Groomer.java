/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.eperson;

import java.sql.SQLException;
import org.apache.commons.cli.*;
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
        verbs.addOption(new Option("u", "unsalted", false, "list accounts with unsalted password hashes"));

        Options options = new Options();
        options.addOptionGroup(verbs);

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
        // Scan for unsalted hashes
        else if (command.hasOption('u'))
        {
            Context myContext = new Context();
            final TableRowIterator tri = DatabaseManager.query(myContext,
                    "SELECT email FROM EPerson WHERE password IS NOT NULL AND digest_algorithm IS NULL");
            for (TableRow row = tri.next(); tri.hasNext(); row = tri.next())
                System.out.println(row.getStringColumn("email"));
            myContext.abort(); // No changes to commit
        }
        // Should not happen:  verb option defined but no code!
        else
            System.err.println("Unimplemented verb:  " + verbs.getSelected());
    }
}
