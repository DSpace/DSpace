/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.checker;

import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.dspace.checker.CheckBitstreamIterator;
import org.dspace.checker.ChecksumCheckResults;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

/**
 * <p>
 * Use to iterate over bitstreams selected based on information stored in MOST_RECENT_CHECKSUM
 * </p>
 *
 * @author Monika Mevenkamp
 */
public final class ChecksumWorker
{
    private static final Logger LOG = Logger.getLogger(ChecksumWorker.class);
    private static final String DEFAULT_EXCLUDES = ChecksumCheckResults.BITSTREAM_NOT_FOUND + ", " +
            ChecksumCheckResults.BITSTREAM_MARKED_DELETED + ", " +
            ChecksumCheckResults.CHECKSUM_ALGORITHM_INVALID;


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

        options.addOption("i", "include_result", true, "Work on bitstreams whose last result is <RESULT>");
        options.addOption("x", "exclude_result", true, "Work on bitstreams whose last result is not one of the given results (use a comma separated list)");
        options.addOption("l", "loop", false, "Work on bitstreams whose last result is not one of " + DEFAULT_EXCLUDES );
        options.addOption("h", "help", false, "Help");
        options.addOption("b", "before", true, "CWork on bitstreams last checked before given date");
        options.addOption("a", "after", true, "Work on bitstreams last checked after given date");
        options.addOption("c", "count", true, "Work on at most the given number of bitstreams");
        options.addOption("r", "root", true, "Work on bitstream in given Community, Collection, Item, or on the given Bitstream, give root as handle or TYPE.ID)");
        options.addOption("v", "verbose", false, "Be verbose");

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
            if ((loop && (with_result  != null  || excludes != null)  ) || (with_result != null && excludes != null))
            {
                throw new RuntimeException("-x, -i, and -l options are mutually exclusive");
            }

            if (loop) {
                excludes = DEFAULT_EXCLUDES;
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


            DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);
            Date before = line.hasOption('b') ? df.parse(line.getOptionValue('b')) : null;
            Date after = line.hasOption('a') ? df.parse(line.getOptionValue('a')) : null;
            int count = line.hasOption('c') ? new Integer(line.getOptionValue('c')) : -1;
            DSpaceObject root = null;
            if (line.hasOption('r'))
            {
                root = DSpaceObject.fromString(context, line.getOptionValue('r'));
            }
            CheckBitstreamIterator iter = CheckBitstreamIterator.create(with_result, exclude_results, context, before, after, root);
            if (iter == null)
            {
                throw new RuntimeException("not enough data to create iterator");
            }

            System.out.println("# " + iter);

            while (iter.next())
            {
                System.out.println("" + count + ": " + iter.bitstream_id() + "\t" + iter.result() + "\t" + iter.last_process_end_date());
                count--;
                if (count == 0)
                {
                    break;
                }
            }

        } catch (Exception e)
        {
            System.err.println(e.getMessage());
            e.printStackTrace();
            printHelp(options);
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

        myhelp.printHelp("ChecksumWorker:\n", options);

        System.err.println("Give dates in the american style: mm/dd/yyyy eg 2/20/2013 for Feb 20th 2013");

    }

}
