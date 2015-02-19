/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.checker;

import java.sql.SQLException;
import java.util.Date;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.checker.ChecksumCheckResults;
import org.dspace.checker.ChecksumHistory;
import org.dspace.checker.ChecksumHistoryIterator;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.core.Utils;

/**
 * <p>
 * Use to iterate over bitstreams selected based on information stored in MOST_RECENT_CHECKSUM
 * </p>
 *
 * @author Monika Mevenkamp
 */
public final class ChecksumHistoryWorker
{
    private static final Logger LOG = Logger.getLogger(ChecksumHistoryWorker.class);

    private static final String[] ACTION_LIST = {"delete", "print", "count"};
    private static final int DELETE = 0;
    private static final int PRINT = 1;
    private static final int COUNT = 2;
    private static final int DEFAULT_ACTION = COUNT;

    private Context context;
    private ChecksumHistoryIterator iter;

    private ChecksumHistoryWorker(Context context, ChecksumHistoryIterator iterator)
    {
        this.context = context;
        this.iter = iterator;
    }

    private void process(int action) throws SQLException
    {
        System.out.println("# " + iter);
        System.out.println("# Action " + ACTION_LIST[action]);
        switch (action) {
        case COUNT:
                count();
            break;
            case DELETE:
                delete();
                break;
            case PRINT:
            {
                int row = 1;
                ChecksumHistory hist;
                for (hist = iter.next(); hist != null; hist = iter.next())
                {
                    print(hist, row);
                    row++;
                }
                System.out.println();
                System.out.println("# worked on " + (row - 1) + " bitstreams");
            }
        }
    }

    private void print(ChecksumHistory hist, int row) throws SQLException
    {
        System.out.println("" + row +
                        " BITSTREAM." + hist.getBitstreamId() + " " + hist.getResultCode() +
                        " calulatedCheckSum=" + hist.getChecksumCalculated() + " " +
                        " expectedCheckSum=" + hist.getChecksumExpected() + " " +
                        " startDate=" + hist.getProcessStartDate() + " " +
                        " endDate=" + hist.getProcessEndDate() );
    }

    private void count() throws SQLException
    {
        System.out.println("match_count=" + iter.count() + "\t" + iter.propertyString("\t"));
    }

    private void delete() throws SQLException
    {
        System.out.println("NOT IMPLEMENTED");
    }

    public static void main(String[] args)
    {
        Context context = null;
        try
        {
            context = new Context();
        } catch (SQLException e)
        {
            System.err.println("could not open database connection");
            System.err.println(e.getStackTrace());
            System.exit(1);
        }

        // set up command line parser
        CommandLineParser parser = new PosixParser();
        CommandLine line = null;

        // create an options object and populate it
        Options options = new Options();

        options.addOption("d", "do", true, "action to apply to bitstreams");
        options.addOption("i", "include_result", true, "Work on bitstreams whose last result matches the given result");
        options.addOption("x", "exclude_result", true, "Work on bitstreams whose last result is not one of the given results (use a comma separated list)");
        options.addOption("r", "root", true, "Work on bitstream in given Community, Collection, Item, or on the given Bitstream, give root as handle or TYPE.ID)");
        options.addOption("b", "before", true, "Work on bitstreams last checked before (current time minus given duration)");
        options.addOption("a", "after", true, "Work on bitstreams last checked after (current time minus given duration) ");
        options.addOption("c", "count", true, "Work on at most the given number of bitstreams");
        options.addOption("h", "help", false, "Print this help");

        try
        {
            line = parser.parse(options, args);
            if (line.hasOption('h'))
            {
                printHelp(options);
                return;
            }

            String with_result = line.hasOption('i') ? line.getOptionValue('i') : null;
            // TODO check whether valid check_sum_result
            Boolean loop = line.hasOption('l');
            String excludes = line.hasOption('x') ? line.getOptionValue('x') : null;
            if ((loop && (with_result != null || excludes != null)) || (with_result != null && excludes != null))
            {
                throw new RuntimeException("-x, -i, and -l options are mutually exclusive");
            }

            String[] exclude_results = null;
            if (excludes != null)
            {
                exclude_results = excludes.split(",");
                for (int i = 0; i < exclude_results.length; i++)
                {
                    exclude_results[i] = exclude_results[i].trim();
                    // TODO check whether valid check_sum_result
                }
            }


            Date after = null, before = null;
            if (line.hasOption('b'))
            {
                before = new Date(System.currentTimeMillis() - Utils.parseDuration(line.getOptionValue('b')));
            }
            if (line.hasOption('a'))
            {
                after = new Date(System.currentTimeMillis() - Utils.parseDuration(line.getOptionValue('a')));
            }

            DSpaceObject root = null;
            if (line.hasOption('r'))
            {
                root = DSpaceObject.fromString(context, line.getOptionValue('r'));
                if (root == null)
                {
                    throw new RuntimeException("No such DSpaceObject " + line.getOptionValue('r'));
                }
            }
            ChecksumHistoryIterator iter = new ChecksumHistoryIterator(context, with_result, exclude_results, before, after, root);
            if (iter == null)
            {
                throw new RuntimeException("not enough data to create iterator");
            }

            String actionStr = line.hasOption('d') ? line.getOptionValue('d') : ACTION_LIST[DEFAULT_ACTION];
            int action = -1;
            for (int i = 0; i < ACTION_LIST.length; i++)
            {
                if (actionStr.equals(ACTION_LIST[i]))
                {
                    action = i;
                }
            }
            if (action < 0)
            {
                throw new RuntimeException("Unknown do action " + actionStr);
            }

            new ChecksumHistoryWorker(context, iter).process(action);


        } catch (Exception e)
        {
            System.err.println(e.getMessage());
            printHelp(options);
            System.err.println();
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Print the help options for the user
     *
     * @param options that are available for the user
     */
    private static void printHelp(Options options)
    {
        HelpFormatter myhelp = new HelpFormatter();

        myhelp.printHelp("ChecksumHistoryWorker:\n", options);
        System.err.println();
        System.err.println("Available do actions that may be applied to selected bitstreams:");
        System.err.println("\t" + StringUtils.join(ACTION_LIST, ", "));
        System.err.println("\t" + "default action: " + ACTION_LIST[DEFAULT_ACTION]);
        System.err.println("\tprint  \tprints selected checksum history entries");
        System.err.println("\tcount  \tcounts how may checksum history entries match with the given parameters");
        System.err.println("\tdelete \tdeletes selected checksum history entries\"");
        System.err.println();
        System.err.println("Available checksum results: ");
        System.err.println("\t" + StringUtils.join(ChecksumCheckResults.RESULTS_LIST, "\n\t"));
        System.err.println();
        System.err.println("Give duration using y(year) w(week), d(days), h(hours) m(minutes):");
        System.err.println("\t4w for 4 weeks, 30d for 30 days, or 15m for 15 minutes");

    }

}
