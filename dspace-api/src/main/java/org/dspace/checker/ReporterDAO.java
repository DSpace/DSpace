/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;

/**
 * This class will report information on the checksum checker process.
 * 
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 * 
 * 
 */
public class ReporterDAO extends DAOSupport
{
    /**
     * Select the most recent bitstream information for a given date range with
     * the specified status. This select is from the checksum history and
     * checksum results tables.
     */
    public static final String DATE_RANGE_BITSTREAMS = "select bitstream_id, last_process_start_date, last_process_end_date, "
            + "expected_checksum, current_checksum, result_description "
            + "from most_recent_checksum, checksum_results "
            + "where most_recent_checksum.result = checksum_results.result_code "
            + "and most_recent_checksum.result= ? "
            + "and most_recent_checksum.last_process_start_date >= ? "
            + "and most_recent_checksum.last_process_start_date < ? "
            + "order by bitstream_id";

    /**
     * 
     * Find all bitstreams that were set to not be processed for the specified
     * date range.
     * 
     */
    public static final String DATE_RANGE_NOT_PROCESSED_BITSTREAMS = "select most_recent_checksum.bitstream_id, "
            + "most_recent_checksum.last_process_start_date, most_recent_checksum.last_process_end_date, "
            + "most_recent_checksum.expected_checksum, most_recent_checksum.current_checksum, "
            + "result_description "
            + "from checksum_results, most_recent_checksum "
            + "where most_recent_checksum.to_be_processed = false "
            + "and most_recent_checksum.result = checksum_results.result_code "
            + "and most_recent_checksum.last_process_start_date >= ? "
            + "and most_recent_checksum.last_process_start_date < ? "
            + "order by most_recent_checksum.bitstream_id";

    public static final String DATE_RANGE_NOT_PROCESSED_BITSTREAMS_ORACLE = "select most_recent_checksum.bitstream_id, "
        + "most_recent_checksum.last_process_start_date, most_recent_checksum.last_process_end_date, "
        + "most_recent_checksum.expected_checksum, most_recent_checksum.current_checksum, "
        + "result_description "
        + "from checksum_results, most_recent_checksum "
        + "where most_recent_checksum.to_be_processed = 0 "
        + "and most_recent_checksum.result = checksum_results.result_code "
        + "and most_recent_checksum.last_process_start_date >= ? "
        + "and most_recent_checksum.last_process_start_date < ? "
        + "order by most_recent_checksum.bitstream_id";
    
    /**
     * Find all bitstreams that the checksum checker is unaware of
     */
    public static final String FIND_UNKNOWN_BITSTREAMS = "select bitstream.deleted, bitstream.store_number, bitstream.size_bytes, "
            + "bitstreamformatregistry.short_description, bitstream.bitstream_id,  "
            + "bitstream.internal_id, "
            + "bitstream.checksum_algorithm, bitstream.checksum "
            + "from bitstream left outer join bitstreamformatregistry on "
            + "bitstream.bitstream_format_id = bitstreamformatregistry.bitstream_format_id "
            + "where not exists( select 'x' from most_recent_checksum "
            + "where most_recent_checksum.bitstream_id = bitstream.bitstream_id )";

    /**
     * Usual Log4J Logger.
     */
    private static final Logger LOG = Logger.getLogger(ReporterDAO.class);

    /**
     * Default constructor
     */
    public ReporterDAO()
    {
    }

    /**
     * Select the most recent bitstream for a given date range with the
     * specified status.
     * 
     * @param startDate
     *            the start date range
     * @param endDate
     *            the end date range.
     * @param resultCode
     *            the result code
     * 
     * @return a list of BitstreamHistoryInfo objects
     */
    public List<ChecksumHistory> getBitstreamResultTypeReport(Date startDate, Date endDate,
            String resultCode)
    {
        List<ChecksumHistory> bitstreamHistory = new LinkedList<ChecksumHistory>();

        Connection conn = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        try
        {
            // create the connection and execute the statement
            conn = DatabaseManager.getConnection();

            prepStmt = conn.prepareStatement(DATE_RANGE_BITSTREAMS);

            prepStmt.setString(1, resultCode);
            prepStmt.setDate(2, new java.sql.Date(startDate.getTime()));
            prepStmt.setDate(3, new java.sql.Date(endDate.getTime()));

            rs = prepStmt.executeQuery();

            // add the bitstream history objects
            while (rs.next())
            {
                bitstreamHistory.add(new ChecksumHistory(rs
                        .getInt("bitstream_id"), rs
                        .getTimestamp("last_process_start_date"), rs
                        .getTimestamp("last_process_end_date"), rs
                        .getString("expected_checksum"), rs
                        .getString("current_checksum"), rs
                        .getString("result_description")));
            }
        }
        catch (SQLException e)
        {
            LOG.warn("Bitstream history could not be found for specified type "
                    + e.getMessage(), e);
        }
        finally
        {
            cleanup(prepStmt, conn, rs);
        }

        return bitstreamHistory;
    }

    /**
     * Find all bitstreams that were set to not be processed for the specified
     * date range.
     * 
     * @param startDate
     *            the start of the date range
     * @param endDate
     *            the end of the date range
     * @return a list of BitstreamHistoryInfo objects
     */
    public List<ChecksumHistory> getNotProcessedBitstreamsReport(Date startDate, Date endDate)
    {
        List<ChecksumHistory> bitstreamHistory = new LinkedList<ChecksumHistory>();

        Connection conn = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        try
        {
            // create the connection and execute the statement
            conn = DatabaseManager.getConnection();

            if (DatabaseManager.isOracle())
            {
                prepStmt = conn.prepareStatement(DATE_RANGE_NOT_PROCESSED_BITSTREAMS_ORACLE);
            }
           	else
            {
                prepStmt = conn.prepareStatement(DATE_RANGE_NOT_PROCESSED_BITSTREAMS);
            }

            prepStmt.setDate(1, new java.sql.Date(startDate.getTime()));
            prepStmt.setDate(2, new java.sql.Date(endDate.getTime()));

            rs = prepStmt.executeQuery();

            // add the bitstream history objects
            while (rs.next())
            {
                bitstreamHistory.add(new ChecksumHistory(rs
                        .getInt("bitstream_id"), rs
                        .getTimestamp("last_process_start_date"), rs
                        .getTimestamp("last_process_end_date"), rs
                        .getString("expected_checksum"), rs
                        .getString("current_checksum"), rs
                        .getString("result_description")));
            }
        }
        catch (SQLException e)
        {
            LOG.warn("Bitstream history could not be found for specified type "
                    + e.getMessage(), e);
        }
        finally
        {
            cleanup(prepStmt, conn, rs);
        }

        return bitstreamHistory;
    }

    /**
     * Find all bitstreams that the checksum checker is currently not aware of
     * 
     * @return a List of DSpaceBitstreamInfo objects
     */
    public List<DSpaceBitstreamInfo> getUnknownBitstreams(Context context)
    {
        List<DSpaceBitstreamInfo> unknownBitstreams = new LinkedList<DSpaceBitstreamInfo>();

        Connection conn = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        try
        {
            // create the connection and execute the statement
            conn = DatabaseManager.getConnection();

            prepStmt = conn.prepareStatement(FIND_UNKNOWN_BITSTREAMS);

            rs = prepStmt.executeQuery();

            // add the bitstream history objects
            while (rs.next())
            {
                Bitstream bitstream = Bitstream.find(context, rs.getInt("bitstream_id"));
                unknownBitstreams.add(new DSpaceBitstreamInfo(rs
                        .getBoolean("deleted"), rs.getInt("store_number"), rs
                        .getInt("size_bytes"), rs
                        .getString("short_description"), rs
                        .getInt("bitstream_id"), bitstream.getFormatDescription(), rs
                        .getString("internal_id"), bitstream.getSource(), rs
                        .getString("checksum_algorithm"), rs
                        .getString("checksum"), bitstream.getName(), bitstream.getDescription()));
            }
        }
        catch (SQLException e)
        {
            LOG.warn("Bitstream history could not be found for specified type "
                    + e.getMessage(), e);
        }
        finally
        {
            cleanup(prepStmt, conn, rs);
        }

        return unknownBitstreams;
    }
}
