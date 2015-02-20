/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.checker;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Date;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.checker.BitstreamInfo;
import org.dspace.checker.BitstreamInfoDAO;
import org.dspace.checker.CheckBitstreamIterator;
import org.dspace.checker.CheckerCommand;
import org.dspace.checker.ChecksumCheckResults;
import org.dspace.checker.ChecksumHistory;
import org.dspace.checker.ChecksumHistoryDAO;
import org.dspace.checker.ChecksumHistoryIterator;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.storage.bitstore.BitstreamStorageManager;

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

    private static final String[] ACTION_LIST = {"check", "print", "history", "delete", "count"};
    private static final int CHECK = 0;
    private static final int PRINT = 1;
    private static final int HISTORY = 2;
    private static final int COUNT = 4;
    private static final int DEFAULT_ACTION = CHECK;

    private Context context;
    private CheckBitstreamIterator iter;
    private BitstreamInfoDAO bitstreamInfoDAO = null;
    private ChecksumHistoryDAO checksumHistoryDAO = null;

    private ChecksumWorker(Context context, CheckBitstreamIterator iterator)
    {
        this.context = context;
        this.iter = iterator;
        bitstreamInfoDAO = new BitstreamInfoDAO(context.getDBConnection());
        checksumHistoryDAO = new ChecksumHistoryDAO(context.getDBConnection());
    }

    private void process(int action, int count, boolean verbose) throws SQLException
    {
        System.out.println("# " + iter);
        System.out.println("# Action " + ACTION_LIST[action]);
        if (!verbose && action == CHECK)
        {
            System.out.println("# Printing  m for " + ChecksumCheckResults.CHECKSUM_MATCH +
                    ", d for " + ChecksumCheckResults.BITSTREAM_MARKED_DELETED  +
                    ", and e in all other cases");
        }

        if (verbose)
        {
            System.out.println("# Checking for new bitstreams: " + new Date());
        }
        bitstreamInfoDAO.updateMissingBitstreams();
        if (verbose)
        {
            System.out.println("# Done Checking for new bitstreams " + new Date());
        }

        if (action == COUNT)
        {
            countMatches(verbose);
        } else
        {
            int row = 1;
            while (iter.next())
            {
                switch (action)
                {
                    case PRINT:
                        printBitstream(row, verbose);
                        break;
                    case CHECK:
                        checkBitstream(row, verbose);

                        break;
                    case HISTORY:
                        printHistory(verbose);
                        break;
                    default:
                        System.out.println("" + row + ": " + iter.bitstream_id() + "\t" + " TODO " + ACTION_LIST[action]);
                }

                row++;
                count--;
                if (count == 0)
                {
                    break;
                }
            }
            iter.close();

            System.out.println();
            System.out.println("# worked on " + (row-1) + " bitstreams");
        }
    }

    private void printBitstream(int row, boolean verbose) throws SQLException
    {
        System.out.println("" + row + " BITSTREAM." + iter.bitstream_id() + " " + iter.result() +
                " internalId=" + iter.internalId() + " " +
                " delete=" + iter.deleted() + " " +
                " lastDate=" + iter.last_process_end_date() + " ");
        if (verbose)
        {
            System.out.println("" + row + " BITSTREAM." + iter.bitstream_id() + " " + iter.result() +
                    " algo=" + iter.checksumAlgo() +
                    " expected=" + iter.storedChecksum() +
                    " calculated=" + iter.checksum());

            System.out.print("" + row + " BITSTREAM." + iter.bitstream_id() + " " + iter.result() + " ");
            Bitstream bitstream = Bitstream.find(context, iter.bitstream_id());
            for (DSpaceObject parent = bitstream.getParentObject(); parent != null; parent = parent.getParentObject())
            {
                System.out.print(Constants.typeText[parent.getType()] + ":" + parent.getHandle() + " ");
            }
            System.out.println();
        }
    }

    private void printHistory(boolean verbose) throws SQLException
    {
        ChecksumHistoryIterator histIter = new ChecksumHistoryIterator(context, iter.bitstream_id());
        for (ChecksumHistory cur = histIter.next(); cur != null; cur = histIter.next())
        {
            System.out.print("BITSTREAM." + cur.getBitstreamId());
            System.out.print(" result=" + cur.getResultCode());
            if (verbose)
            {
                System.out.print(" expected=" + cur.getChecksumExpected());
                System.out.print(" calculated=" + cur.getChecksumCalculated());
            }
            System.out.print(" lastDate=" + cur.getProcessEndDate());
            System.out.println();
        }
        histIter.close();
    }

    private void countMatches(boolean verbose) throws SQLException
    {
        System.out.println("match_count=" + iter.count() + "\t" + iter.propertyString("\t"));
    }

    private void checkBitstream(int row, boolean verbose)
    {
        int id = -1;
        BitstreamInfo calcInfo = null;
        String result = null;
        InputStream bitstream = null;
        try
        {
            id = iter.bitstream_id();
            calcInfo = new BitstreamInfo(id);
        } catch (SQLException e)
        {
            result = ChecksumCheckResults.BITSTREAM_INFO_NOT_FOUND;
        }

        if (calcInfo != null)
        {
            calcInfo.setProcessStartDate(new Date());
            try
            {
                calcInfo.setChecksumAlgorithm(iter.checksumAlgo());
                String storedChecksum = iter.storedChecksum();
                calcInfo.setStoredChecksum(calcInfo.getBitstream(context).getChecksum());
                if (!iter.deleted())
                {
                    bitstream = BitstreamStorageManager.retrieve(context, id);
                    String checksum = CheckerCommand.digestStream(bitstream, calcInfo.getChecksumAlgorithm());
                    if (LOG.isDebugEnabled())
                    {
                        LOG.debug("BITSTREAM." + iter.bitstream_id() + " checksum " + checksum + " storedCheckSum " + storedChecksum);
                    }
                    calcInfo.setCalculatedChecksum(checksum);
                    result = CheckerCommand.compareChecksums(storedChecksum, checksum);
                } else
                {
                    calcInfo.setToBeProcessed(false);  // to be compatible with CheckerCommand.processDeletedBitstream
                    result = ChecksumCheckResults.BITSTREAM_MARKED_DELETED;
                }

            } catch (IOException e)
            {
                result = ChecksumCheckResults.BITSTREAM_NOT_FOUND;
                if (LOG.isDebugEnabled())
                    LOG.debug(result + " " + e.getMessage());
            } catch (NoSuchAlgorithmException e)
            {
                result = ChecksumCheckResults.CHECKSUM_ALGORITHM_INVALID;
                if (LOG.isDebugEnabled())
                    LOG.debug(result + " " + e.getMessage());
            } catch (SQLException e)
            {
                result = ChecksumCheckResults.BITSTREAM_NOT_PROCESSED;
                if (LOG.isDebugEnabled())
                    LOG.debug(result + " " + e.getMessage());
            }
        }

        calcInfo.setChecksumCheckResult(result);
        calcInfo.setProcessEndDate(new Date());

        // record new checksum and comparison result in db
        LOG.debug("> update bitstreamInfoDAO " + calcInfo.toLongString());
        bitstreamInfoDAO.update(calcInfo);
        LOG.debug("< update bitstreamInfoDAO");
        LOG.debug("> update checksumHistoryDAO " + calcInfo.toLongString());
        checksumHistoryDAO.insertHistory(calcInfo);
        LOG.debug("< update checksumHistoryDAO");

        if (verbose)
        {
            System.out.println("" + row + " " + calcInfo.toLongString());
        } else
        {
            char resultCode = 'E';
            if ( result == ChecksumCheckResults.CHECKSUM_MATCH)
            {
                resultCode = 'm';
            } else if (result == ChecksumCheckResults.BITSTREAM_MARKED_DELETED)
            {
                resultCode = 'd';
            }
            System.out.print( resultCode);
            if (row % 80 == 0)
            {
                System.out.println("");
            }
        }

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
        boolean verbose = false;

        // create an options object and populate it
        Options options = new Options();

        options.addOption("d", "do", true, "action to apply to bitstreams");
        options.addOption("l", "loop", false, "Work on bitstreams whose last result is not one of " + DEFAULT_EXCLUDES);
        options.addOption("i", "include_result", true, "Work on bitstreams whose last result matches the given result");
        options.addOption("x", "exclude_result", true, "Work on bitstreams whose last result is not one of the given results (use a comma separated list)");
        options.addOption("r", "root", true, "Work on bitstream in given Community, Collection, Item, or on the given Bitstream, give root as handle or TYPE.ID)");
        options.addOption("b", "before", true, "Work on bitstreams last checked before (current time minus given duration)");
        options.addOption("a", "after", true, "Work on bitstreams last checked after (current time minus given duration) ");
        options.addOption("c", "count", true, "Work on at most the given number of bitstreams");
        options.addOption("v", "verbose", false, "Be verbose");
        options.addOption("h", "help", false, "Print this help");

        try
        {
            line = parser.parse(options, args);
            if (line.hasOption('h'))
            {
                printHelp(options);
                return;
            }

            verbose = line.hasOption('v');

            String with_result = line.hasOption('i') ? line.getOptionValue('i') : null;
            // TODO check whether valid check_sum_result
            Boolean loop = line.hasOption('l');
            String excludes = line.hasOption('x') ? line.getOptionValue('x') : null;
            if ((loop && (with_result != null || excludes != null)) || (with_result != null && excludes != null))
            {
                throw new RuntimeException("-x, -i, and -l options are mutually exclusive");
            }

            if (loop)
            {
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
            CheckBitstreamIterator iter = new CheckBitstreamIterator(context, with_result, exclude_results, before, after, root);
            if (iter == null)
            {
                throw new RuntimeException("not enough data to create iterator");
            }

            int count = line.hasOption('c') ? new Integer(line.getOptionValue('c')) : -1;

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

            new ChecksumWorker(context, iter).process(action, count, verbose);


        } catch (Exception e)
        {
            System.err.println(e.getMessage());
            printHelp(options);
            if (verbose)
            {
                System.err.println();
                e.printStackTrace();
            }
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
        System.err.println();
        System.err.println("Available do actions that may be applied to selected bitstreams:");
        System.err.println("\t" + StringUtils.join(ACTION_LIST, ", "));
        System.err.println("\t" + "default action: " + ACTION_LIST[DEFAULT_ACTION]);
        System.err.println("\tcheck  \tcomputes a checkum for selected bitstreams and compares with the epected checksum");
        System.err.println("\tprint  \tprints selected bitstreams, the last check result, its internal id, and check date");
        System.err.println("\tcount  \tcounts how may bitstreams match with the given parameters");
        System.err.println("\thistory\tprints the complete checksum history of selected bitstreams");
        System.err.println();
        System.err.println("Available checksum results: ");
        System.err.println("\t" + StringUtils.join(ChecksumCheckResults.RESULTS_LIST, "\n\t"));
        System.err.println();
        System.err.println("Give duration using y(year) w(week), d(days), h(hours) m(minutes):");
        System.err.println("\t4w for 4 weeks, 30d for 30 days, or 15m for 15 minutes");

    }

}
