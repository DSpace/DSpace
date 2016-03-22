/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.checker;

import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.dspace.checker.BitstreamDispatcher;
import org.dspace.checker.BitstreamInfoDAO;
import org.dspace.checker.CheckerCommand;
import org.dspace.checker.HandleDispatcher;
import org.dspace.checker.LimitedCountDispatcher;
import org.dspace.checker.LimitedDurationDispatcher;
import org.dspace.checker.ListDispatcher;
import org.dspace.checker.ResultsLogger;
import org.dspace.checker.ResultsPruner;
import org.dspace.checker.SimpleDispatcher;
import org.dspace.core.Context;
import org.dspace.core.Utils;

/**
 * Command line access to the checksum checker. Options are listed in the 
 * documentation for the main method.</p>
 * 
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 */
public final class ChecksumChecker
{
    private static final Logger LOG = Logger.getLogger(ChecksumChecker.class);

    /**
     * Blanked off constructor, this class should be used as a command line
     * tool.
     * 
     */
    private ChecksumChecker()
    {
    }

    /**
     * Command line access to the checksum package.
     * 
     * @param args
     *            <dl>
     *            <dt>-h</dt>
     *            <dd>Print help on command line options</dd>
     *            <dt>-l</dt>
     *            <dd>loop through bitstreams once</dd>
     *            <dt>-L</dt>
     *            <dd>loop continuously through bitstreams</dd>
     *            <dt>-d</dt>
     *            <dd>specify duration of process run</dd>
     *            <dt>-b</dt>
     *            <dd>specify bitstream IDs</dd>
     *            <dt>-a [handle_id]</dt>
     *            <dd>check anything by handle</dd>
     *            <dt>-e</dt>
     *            <dd>Report only errors in the logs</dd>
     *            <dt>-p</dt>
     *            <dd>Don't prune results before running checker</dd>
     *            </dl>
     */
    public static void main(String[] args) throws SQLException {
        // set up command line parser
        CommandLineParser parser = new PosixParser();
        CommandLine line = null;

        // create an options object and populate it
        Options options = new Options();

        options.addOption("l", "looping", false, "Loop once through bitstreams");
        options.addOption("L", "continuous", false,
                "Loop continuously through bitstreams");
        options.addOption("h", "help", false, "Help");
        options.addOption("d", "duration", true, "Checking duration");
        options.addOption("c", "count", true, "Check count");
        options.addOption("a", "handle", true, "Specify a handle to check");
        options.addOption("v", "verbose", false, "Report all processing");

        OptionBuilder.withArgName("bitstream-ids").hasArgs().withDescription(
                "Space separated list of bitstream ids");
        Option useBitstreamIds = OptionBuilder.create('b');

        options.addOption(useBitstreamIds);

        options.addOption("p", "prune", false, "Prune configuration file");
        options
                .addOption(OptionBuilder
                        .withArgName("prune")
                        .hasOptionalArgs(1)
                        .withDescription(
                                "Prune old results (optionally using specified properties file for configuration)")
                        .create('p'));

        try
        {
            line = parser.parse(options, args);
        }
        catch (ParseException e)
        {
            LOG.fatal(e);
            System.exit(1);
        }

        // user asks for help
        if (line.hasOption('h'))
        {
            printHelp(options);
        }

        // Prune stage
        if (line.hasOption('p'))
        {
            ResultsPruner rp = null;
            try
            {
                rp = (line.getOptionValue('p') != null) ? ResultsPruner
                        .getPruner(line.getOptionValue('p')) : ResultsPruner
                        .getDefaultPruner();
            }
            catch (FileNotFoundException e)
            {
                LOG.error("File not found", e);
                System.exit(1);
            }
            int count = rp.prune();
            System.out.println("Pruned " + count
                    + " old results from the database.");
        }

        Date processStart = Calendar.getInstance().getTime();

        BitstreamDispatcher dispatcher = null;
        
        // process should loop infinitely through
        // most_recent_checksum table
        if (line.hasOption('l'))
        {
            dispatcher = new SimpleDispatcher(new BitstreamInfoDAO(), processStart, false); 
        }
        else if (line.hasOption('L'))
        {
            dispatcher = new SimpleDispatcher(new BitstreamInfoDAO(), processStart, true);
        }
        else if (line.hasOption('b'))
        {
            // check only specified bitstream(s)
            String[] ids = line.getOptionValues('b');
            List<Integer> idList = new ArrayList<Integer>(ids.length);

            for (int i = 0; i < ids.length; i++)
            {
                try
                {
                    idList.add(Integer.valueOf(ids[i]));
                }
                catch (NumberFormatException nfe)
                {
                    System.err.println("The following argument: " + ids[i]
                            + " is not an integer");
                    System.exit(0);
                }
            }
            dispatcher = new ListDispatcher(idList);
        }

        else if (line.hasOption('a'))
        {
            dispatcher = new HandleDispatcher(new BitstreamInfoDAO(), line.getOptionValue('a'));
        }
        else if (line.hasOption('d'))
        {
            // run checker process for specified duration
            try
            {
                dispatcher = new LimitedDurationDispatcher(
                        new SimpleDispatcher(new BitstreamInfoDAO(), processStart, true), new Date(
                                System.currentTimeMillis()
                                        + Utils.parseDuration(line
                                                .getOptionValue('d'))));
            }
            catch (Exception e)
            {
                LOG.fatal("Couldn't parse " + line.getOptionValue('d')
                        + " as a duration: ", e);
                System.exit(0);
            }
        }
        else if (line.hasOption('c'))
        {
        	int count = Integer.valueOf(line.getOptionValue('c')).intValue();
            
        	// run checker process for specified number of bitstreams
            dispatcher = new LimitedCountDispatcher(new SimpleDispatcher(
                    new BitstreamInfoDAO(), processStart, false), count);
        }
        else
        {
            dispatcher = new LimitedCountDispatcher(new SimpleDispatcher(
                    new BitstreamInfoDAO(), processStart, false), 1);
        }
        
        ResultsLogger logger = new ResultsLogger(processStart);
        CheckerCommand checker = new CheckerCommand();
        // verbose reporting
        if (line.hasOption('v'))
        {
            checker.setReportVerbose(true);
        }

        checker.setProcessStartDate(processStart);
        checker.setDispatcher(dispatcher);
        checker.setCollector(logger);
        Context context = new Context();
        try {
            checker.process(context);
        } finally {
            context.commit();
            context.complete();
        }
        System.exit(0);
    }

    /**
     * Print the help options for the user
     * 
     * @param options that are available for the user
     */
    private static void printHelp(Options options)
    {
        HelpFormatter myhelp = new HelpFormatter();

        myhelp.printHelp("Checksum Checker\n", options);
        System.out
                .println("\nSpecify a duration for checker process, using s(seconds),"
                        + "m(minutes), or h(hours): ChecksumChecker -d 30s"
                        + " OR ChecksumChecker -d 30m"
                        + " OR ChecksumChecker -d 2h");
        System.out
                .println("\nSpecify bitstream IDs: ChecksumChecker -b 13 15 17 20");
        System.out.println("\nLoop once through all bitstreams: "
                + "ChecksumChecker -l");
        System.out
                .println("\nLoop continuously through all bitstreams: ChecksumChecker -L");
        System.out
                .println("\nCheck a defined number of bitstreams: ChecksumChecker -c 10");
        System.out.println("\nReport all processing (verbose)(default reports only errors): ChecksumChecker -v");
        System.out.println("\nDefault (no arguments) is equivalent to '-c 1'");
        System.exit(0);
    }

}
