/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;

/**
 * <p>
 * Use to iterate over bitstreams selected based on information stored in MOST_RECENT_CHECKSUM
 * </p>
 *
 * @author Monika Mevenkamp
 */
public final class CheckBitstreamIterator extends DAOSupport
{
    private static final Logger LOG = Logger.getLogger(CheckBitstreamIterator.class);

    private static String DEFAULT_EXCLUDES =  ChecksumCheckResults.BITSTREAM_NOT_FOUND + "," +
            ChecksumCheckResults.BITSTREAM_MARKED_DELETED + "," +
            ChecksumCheckResults.CHECKSUM_ALGORITHM_INVALID;

    public static final int SENTINEL = -1;

    static final String THIS_BITSTREAM = "BITSTREAM_ID = ?";

    static final String BITSTREAMS_IN_ITEM = "BITSTREAM_ID in \n" +
            "(SELECT BITSTREAM_ID FROM BUNDLE2BITSTREAM WHERE BUNDLE_ID in\n" +
            "   (SELECT BUNDLE_ID FROM ITEM2BUNDLE WHERE ITEM_ID = ?))";

    static final String BITSTREAMS_IN_COLLECTION = "BITSTREAM_ID in \n" +
            "(SELECT BITSTREAM_ID FROM BUNDLE2BITSTREAM WHERE BUNDLE_ID in\n" +
            "  (SELECT BUNDLE_ID FROM ITEM2BUNDLE WHERE ITEM_ID  in\n" +
            "     (SELECT ITEM_ID FROM COLLECTION2ITEM WHERE COLLECTION_ID = ?)))";

    static final String BITSTREAMS_IN_COMMUNITY = "BITSTREAM_ID in \n" +
            "(SELECT BITSTREAM_ID FROM BUNDLE2BITSTREAM WHERE BUNDLE_ID in\n" +
            "   (SELECT BUNDLE_ID FROM ITEM2BUNDLE WHERE ITEM_ID  in\n" +
            "      (SELECT ITEM_ID  FROM COLLECTION2ITEM WHERE COLLECTION_ID in \n" +
            "         (SELECT COLLECTION_ID FROM COMMUNITY2COLLECTION WHERE COMMUNITY_ID =  ?))))";


    private Context context;
    private String with_result;
    private String[] without_result;
    private Date before, after;
    private DSpaceObject root;
    private ResultSet rs;

    private CheckBitstreamIterator(Context ctxt, Date before_date, Date after_date, DSpaceObject inside)
    {
        super(ctxt.getDBConnection());
        context = ctxt;
        after = after_date;
        before = before_date;
        root = inside;
        rs = null;
    }

    public static CheckBitstreamIterator select(String result, String excludes, Context ctxt,
                                                          Date before_date, Date after_date, DSpaceObject root) throws SQLException
    {
        CheckBitstreamIterator iter = new CheckBitstreamIterator(ctxt, before_date, after_date, root);
        iter.rs = iter.select_with_result_stmt(result, excludes);
        return iter;
    }

    public boolean next() throws SQLException
    {
        return rs.next();
    }

    public int bitstream_id() throws SQLException
    {
        return rs.getInt(1);
    }

    public String result() throws SQLException
    {
        return rs.getString(2);
    }

    public Timestamp last_process_end_date() throws SQLException
    {
        return rs.getTimestamp(3);
    }

    private ResultSet select_with_result_stmt(String res, String without_res) throws SQLException
    {
        with_result = res;
        without_result = without_res.split(",");
        String stmt = "SELECT bitstream_id, result, last_process_end_date FROM MOST_RECENT_CHECKSUM ";
        String operator = " WHERE ";
        if (before != null)
        {
            stmt = stmt + operator + " LAST_PROCESS_END_DATE < ? ";
            LOG.debug("last_check before " + before);
            operator = " AND ";
        }
        if (after != null)
        {
            stmt = stmt + operator + " LAST_PROCESS_END_DATE >= ?";
            LOG.debug("last_check after " + after);
            operator = " AND ";
        }
        if (with_result != null)
        {
            stmt = stmt + operator + " RESULT = ?";
            LOG.debug("last_check result = " + with_result);
            operator = " AND ";
        }
        for (int i = 0; i < without_result.length; i++)
        {
            stmt = stmt + operator + " RESULT != ?";
            LOG.debug("last_check result != " + without_result[i]);
            operator = " AND ";
        }
        if (root != null)
        {
            String bitstream_ids = "";
            LOG.debug("bitstream in  = " + root);
            switch (root.getType())
            {
                case Constants.ITEM:
                    bitstream_ids = BITSTREAMS_IN_ITEM;
                    break;
                case Constants.COLLECTION:
                    bitstream_ids = BITSTREAMS_IN_COLLECTION;
                    break;
                case Constants.COMMUNITY:
                    bitstream_ids = BITSTREAMS_IN_COMMUNITY;
                    break;
                case Constants.BITSTREAM:
                    bitstream_ids = THIS_BITSTREAM;
                    break;
                default:
                    throw new RuntimeException(root.toString() + " does not contains Bitstreams");
            }
            stmt = stmt + operator + bitstream_ids;
            operator = " AND ";
        }
        stmt = stmt + " ORDER BY LAST_PROCESS_END_DATE";
        LOG.debug(stmt);

        PreparedStatement sqlStmt = prepareStatement(stmt);
        int pos = 1;
        if (before != null)
        {
            sqlStmt.setTimestamp(pos, new Timestamp(before.getTime()));
            pos++;
        }
        if (after != null)
        {
            sqlStmt.setTimestamp(pos, new Timestamp(after.getTime()));
            pos++;
        }
        if (with_result != null)
        {
            sqlStmt.setString(pos, with_result);
            pos++;
        }
        for (int i = 0; i < without_result.length; i++)
        {
            sqlStmt.setString(pos, without_result[i].trim());
            pos++;
        }
        if (root != null)
        {
            sqlStmt.setInt(pos, root.getID());
            pos++;
        }
        return sqlStmt.executeQuery();
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

//# TODO add -a option for all bitstreams

        options.addOption("i", "include_result", true, "Check only bitstreams whose last result is <RESULT>");
        options.addOption("x", "exclude_result", true, "Check only bitstreams whose last result is not one of the given results (use a comma separated list)");
        options.addOption("h", "help", false, "Help");
        options.addOption("b", "before", true, "Check only bitstreams last checked before given date");
        options.addOption("a", "after", true, "Check only bitstreams last checked after given date");
        options.addOption("c", "count", true, "Check at most given number of bitstreams");
        options.addOption("r", "root", true, "Community, Collection, Item, or Bitstream (given as handle or TYPE.ID)");
        options.addOption("v", "verbose", false, "Be verbose");

        try
        {
            line = parser.parse(options, args);
            if (line.hasOption('h'))
            {
                printHelp(options);
                return;
            }

            String with_result = null;
            String exclude_results = line.hasOption('x') ? line.getOptionValue('x') : null;
            if (line.hasOption('i'))
            {
                with_result = line.getOptionValue('i');
                if (line.hasOption('x'))
                {
                    throw new RuntimeException("may not  give both -i and -x options");
                }
            } else {
                if (! line.hasOption('x'))
                     exclude_results = DEFAULT_EXCLUDES;
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
            CheckBitstreamIterator iter = null;
            iter = CheckBitstreamIterator.select(with_result, exclude_results, context, before, after, root);
            if (iter == null)
            {
                throw new RuntimeException("not enough data to create iterator");
            }


            while (iter.next())
            {
                System.out.println("" + count + ": " + iter.bitstream_id() +  "\t" + iter.result() +  "\t" + iter.last_process_end_date());
                count--;
                if (count == 0) break;
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

        myhelp.printHelp("ChecksumBitstreamIterator\n", options);

        System.err.println("Give dates in the american style: mm/dd/yyyy eg 2/20/2013 for Feb 20th 2013");

    }

}
