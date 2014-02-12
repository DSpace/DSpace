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
import org.dspace.core.ConfigurationManager;
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
    private static Logger log = Logger.getLogger(ReporterDAO.class);

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
     * Select the most recent bitstream information without date restriction with
     * the specified status. This select is from the checksum history and
     * checksum results tables.
     */
    public static final String NO_DATE_RANGE_BITSTREAMS = "select bitstream_id, last_process_start_date, last_process_end_date, "
            + "expected_checksum, current_checksum, result_description "
            + "from most_recent_checksum, checksum_results "
            + "where most_recent_checksum.result = checksum_results.result_code "
            + "and most_recent_checksum.result= ? "
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
     *
     * Find all bitstreams that were set to not be processed
     *
     */
    public static final String NOT_PROCESSED_BITSTREAMS = "select most_recent_checksum.bitstream_id, "
            + "most_recent_checksum.last_process_start_date, most_recent_checksum.last_process_end_date, "
            + "most_recent_checksum.expected_checksum, most_recent_checksum.current_checksum, "
            + "result_description "
            + "from checksum_results, most_recent_checksum "
            + "where most_recent_checksum.to_be_processed = false "
            + "and most_recent_checksum.result = checksum_results.result_code "
            + "order by most_recent_checksum.bitstream_id";

    public static final String NOT_PROCESSED_BITSTREAMS_ORACLE = "select most_recent_checksum.bitstream_id, "
            + "most_recent_checksum.last_process_start_date, most_recent_checksum.last_process_end_date, "
            + "most_recent_checksum.expected_checksum, most_recent_checksum.current_checksum, "
            + "result_description "
            + "from checksum_results, most_recent_checksum "
            + "where most_recent_checksum.to_be_processed = 0 "
            + "and most_recent_checksum.result = checksum_results.result_code "
            + "order by most_recent_checksum.bitstream_id";
    
    /**
     * Find all bitstreams that the checksum checker is unaware of
     */
    public static final String FIND_UNKNOWN_BITSTREAMS = "select bitstream.deleted, bitstream.store_number, bitstream.size_bytes, "
            + "bitstreamformatregistry.short_description, bitstream.bitstream_id,  "
            + "bitstream.user_format_description, bitstream.internal_id, "
            + "bitstream.source, bitstream.checksum_algorithm, bitstream.checksum, "
            + "bitstream.name, bitstream.description "
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
     * Select the most recent checksum for a given date range with the
     * specified status.
     *
     * @param startDate
     *            the start date range
     * @param endDate
     *            the end date range.
     * @param resultCode
     *            the result code
     *
     * @return a list of ChecksumHistory objects
     */
    public List<ChecksumHistory> getChecksumHistoryReportForDateRange(Date startDate, Date endDate,
                                                                      String resultCode) {
        List<ChecksumHistory> history = null;
        Connection conn = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        try {
            // create the connection and execute the statement
            conn = DatabaseManager.getConnection();

            prepStmt = conn.prepareStatement(DATE_RANGE_BITSTREAMS);
            prepStmt.setString(1, resultCode);
            prepStmt.setDate(2, new java.sql.Date(startDate.getTime()));
            prepStmt.setDate(3, new java.sql.Date(endDate.getTime()));

            history = getChecksumHistory(prepStmt);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        } finally {
            cleanup(conn);
        }
        return history;
    }

    /**
     * Select the most recent checksum with the specified status.
     *
     * @param resultCode
     *            the result code
     *
     * @return a list of ChecksumHistory objects
     */
    public List<ChecksumHistory> getChecksumHistoryReport(String resultCode) {
        List<ChecksumHistory> history = null;
        Connection conn = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseManager.getConnection();
            prepStmt = conn.prepareStatement(NO_DATE_RANGE_BITSTREAMS);
            prepStmt.setString(1, resultCode);
            history = getChecksumHistory(prepStmt);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        } finally {
            cleanup(conn);
        }
        return history;
    }

    /**
     * Find all bitstreams that were set to not be processed; if startDate and endDate are given look only for
     * matches within the specified date range.
     *
     * @param startDate the start of the date range, or null
     * @param endDate   the end of the date range, or null
     * @return a list of ChecksumHistory objects or null iff SQLException
     */
    public List<ChecksumHistory> getNotProcessedBitstreamsReport(Date startDate, Date endDate) {
        List<ChecksumHistory> history = null;

        Connection conn = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        assert (startDate == null || endDate != null);
        try {
            // create the connection and execute the statement
            conn = DatabaseManager.getConnection();

            if ("oracle".equals(ConfigurationManager.getProperty("db.name"))) {
                if (startDate == null)
                    prepStmt = conn.prepareStatement(NOT_PROCESSED_BITSTREAMS_ORACLE);
                else
                    prepStmt = conn.prepareStatement(DATE_RANGE_NOT_PROCESSED_BITSTREAMS_ORACLE);
            } else {
                if (startDate == null)
                    prepStmt = conn.prepareStatement(NOT_PROCESSED_BITSTREAMS);
                else
                    prepStmt = conn.prepareStatement(DATE_RANGE_NOT_PROCESSED_BITSTREAMS);
            }
            if (startDate != null) {
                prepStmt.setDate(1, new java.sql.Date(startDate.getTime()));
                prepStmt.setDate(2, new java.sql.Date(endDate.getTime()));
            }
            history = getChecksumHistory(prepStmt);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
        } finally {
            cleanup(conn);
        }

        return history;
    }

    /**
     * Find all bitstreams that the checksum checker is currently not aware of
     * 
     * @return a List of DSpaceBitstreamInfo objects
     */
    public List<DSpaceBitstreamInfo> getUnknownBitstreams()
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
                unknownBitstreams.add(new DSpaceBitstreamInfo(rs
                        .getBoolean("deleted"), rs.getInt("store_number"), rs
                        .getInt("size_bytes"), rs
                        .getString("short_description"), rs
                        .getInt("bitstream_id"), rs
                        .getString("user_format_description"), rs
                        .getString("internal_id"), rs.getString("source"), rs
                        .getString("checksum_algorithm"), rs
                        .getString("checksum"), rs.getString("name"), rs
                        .getString("description")));
            }
        }
        catch (SQLException e)
        {
            LOG.warn("Bitstream history could not be found for specified type "
                    + e.getMessage(), e);
        }
        finally
        {
            cleanup(prepStmt,rs);
            cleanup(conn);
        }

        return unknownBitstreams;
    }


    private List<ChecksumHistory> getChecksumHistory(PreparedStatement prepStmt) throws SQLException {
        assert (prepStmt != null);

        List<ChecksumHistory> history = new LinkedList<ChecksumHistory>();
        ResultSet rs = null;

        try {
            rs = prepStmt.executeQuery();
            while (rs.next()) {
                history.add(new ChecksumHistory(rs
                        .getInt("bitstream_id"), rs
                        .getTimestamp("last_process_start_date"), rs
                        .getTimestamp("last_process_end_date"), rs
                        .getString("expected_checksum"), rs
                        .getString("current_checksum"), rs
                        .getString("result_description")));
            }

        } catch (SQLException e) {
            LOG.warn("Bitstream history could not be found for specified type "
                    + e.getMessage(), e);
        } finally {
            cleanup(prepStmt, rs);
            return history;
        }
    }

}
