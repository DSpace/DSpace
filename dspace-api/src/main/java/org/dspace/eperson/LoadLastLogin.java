/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.RecordManagerOptions;
import jdbm.btree.BTree;
import jdbm.helper.StringComparator;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;

/**
 * Examine a collection of DSpace log files, building a table of last login
 * times for known EPersons, and then update EPerson records with the latest
 * dates.
 *
 * @author mwood
 */
public class LoadLastLogin
{
    public static void main(String[] argv)
            throws IOException, SQLException, AuthorizeException
    {
        final String USAGE = "LoadLastLogin [options] path...path\n\n"
                + "'path's are paths to DSpace log files";

        final String loginRE =
            "([0-9-]+) ([0-9:]+)[^@]+@ " // Date(1), time(2), goop
            + "([^:]+):" // user(3)
            + "session_id=[^:]+:"
            + "ip_addr=[0-9a-f.:]+:"
            + "login:type=(implicit|explicit)";

        // Handle options, if any
        Options options = new Options();
        options.addOption("h", "help", false, "Explain options");
        options.addOption("p", "pretend", false, "Output TSV instead of updating database");
        options.addOption("v", "verbose", false, "Talk more about what we are doing");

        PosixParser parser = new PosixParser();
        CommandLine command = null;
        try {
            command = parser.parse(options, argv);
        } catch (org.apache.commons.cli.ParseException ex) {
            System.err.println(ex.getMessage());
            if (! (ex instanceof MissingOptionException))
                new HelpFormatter().printHelp(USAGE, options);
            System.exit(1);
        }

        if (command.hasOption('h'))
        {
            System.out.println("Load users' last_active dates into the database from DSpace logs.");
            System.out.println();
            new HelpFormatter().printHelp(USAGE, options);
            System.exit(0);
        }

        final boolean VERBOSE = command.hasOption('v');
        final boolean PRETEND = command.hasOption('p');

        String[] args = command.getArgs();

        // Set up a "table" that can overflow to storage
        final Properties rmProps = new Properties();
        rmProps.put(RecordManagerOptions.DISABLE_TRANSACTIONS, "true");

        String dbname = new File(System.getProperty("java.io.tmpdir"), "lastlogindb").getCanonicalPath();
        if (VERBOSE)
            System.out.println("dbname:  " + dbname);
        RecordManager stamps = RecordManagerFactory.createRecordManager(dbname, rmProps);
        BTree stampDb = BTree.createInstance(stamps, new StringComparator());

        // Scan log files looking for login records
        final Pattern loginCracker = Pattern.compile(loginRE);
        final SimpleDateFormat dateEncoder = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (String logName : args)
        {
            BufferedReader logReader = new BufferedReader(new FileReader(logName));
            while(true)
            {
                String line = logReader.readLine();
                // End of file?
                if (null == line)
                    break;
                // Skip if definitely not a login record
                if (!line.contains(":login:"))
                    continue;

                // Try to recognize the interesting fields
                Matcher loginMatcher = loginCracker.matcher(line);
                if (!loginMatcher.matches())
                    continue;

                // Pretty sure we have a login
                String date = loginMatcher.group(1);
                String time = loginMatcher.group(2);
                String user = loginMatcher.group(3);

                String logDateTime = date + ' ' + time;
                Date stamp;
                try {
                    stamp = dateEncoder.parse(logDateTime);
                } catch (ParseException ex) {
                    System.err.println("Skipping log record:  " + ex.getMessage());
                    continue;
                }
                Date previous = (Date) stampDb.find(user);
                if (null == previous || stamp.after(previous))
                {
                    stampDb.insert(user, stamp, true); // Record this user's newest login so far
                }
            }
            logReader.close();
        }

        // Now walk the cache and update EPersons
        TupleBrowser walker = stampDb.browse();
        Tuple stamp = new Tuple();
        Context ctx = new Context();
        ctx.turnOffAuthorisationSystem();

        EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

        while(walker.getNext(stamp))
        {
            // Update an EPerson's last login
            String name = (String) stamp.getKey();
            Date date = (Date) stamp.getValue();
            EPerson ePerson;
            ePerson = ePersonService.findByEmail(ctx, name);
            if (null == ePerson)
                ePerson = ePersonService.findByNetid(ctx, name);
            if (null == ePerson)
            {
                System.err.println("Skipping unknown user:  " + name);
                continue;
            }
            Date previous = ePerson.getLastActive();
            if ((null == previous) || date.after(previous))
            {
                if (PRETEND)
                {
                    System.out.printf("%s\t%s\t%s\t%s\t%s\n",
                            ePerson.getID().toString(),
                            date,
                            ePerson.getEmail(),
                            ePerson.getNetid(),
                            ePerson.getFullName());
                }
                else
                {
                    ePerson.setLastActive(date);
                    ePersonService.update(ctx, ePerson);
                }
            }
        }

        ctx.complete();

        stamps.close();

        // Clean up external data and index files, if any
        File target;

        target = new File(dbname + ".db");
        if (target.exists())
            target.delete();

        target = new File(dbname + ".lg");
        if (target.exists())
            target.delete();
    }
}
