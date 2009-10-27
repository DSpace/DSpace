package org.dspace.checker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.storage.rdbms.DatabaseManager;

/**
 * <p>
 * This is the data access for the checksum history information. All
 * update,insert and delete database operations should go through this class for
 * checksum history operations.
 * </p>
 * 
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 * 
 * 
 */
public class ChecksumHistoryDAO extends DAOSupport
{
    /**
     * Query that selects bitstream IDs from most_recent_checksum table that are
     * not yet in the checksum_history table, and inserts them into
     * checksum_history.
     */
    private static final String INSERT_MISSING_HISTORY_BITSTREAMS = "insert into checksum_history ( "
            + "bitstream_id, process_start_date, "
            + "process_end_date, checksum_expected, "
            + "checksum_calculated, result ) "
            + "select most_recent_checksum.bitstream_id, "
            + "most_recent_checksum.last_process_start_date, "
            + "most_recent_checksum.last_process_end_date, "
            + "most_recent_checksum.expected_checksum, most_recent_checksum.expected_checksum, "
            + "CASE WHEN bitstream.deleted = true THEN 'BITSTREAM_MARKED_DELETED' else 'CHECKSUM_MATCH' END "
            + "from most_recent_checksum, bitstream where "
            + "not exists( select 'x' from checksum_history where "
            + "most_recent_checksum.bitstream_id = checksum_history.bitstream_id ) "
            + "and most_recent_checksum.bitstream_id = bitstream.bitstream_id";

    private static final String INSERT_MISSING_HISTORY_BITSTREAMS_ORACLE = "insert into checksum_history ( "
        + "check_id, bitstream_id, process_start_date, "
        + "process_end_date, checksum_expected, "
        + "checksum_calculated, result ) "
        + "select checksum_history_seq.nextval, most_recent_checksum.bitstream_id, "
        + "most_recent_checksum.last_process_start_date, "
        + "most_recent_checksum.last_process_end_date, "
        + "most_recent_checksum.expected_checksum, most_recent_checksum.expected_checksum, "
        + "CASE WHEN bitstream.deleted = 1 THEN 'BITSTREAM_MARKED_DELETED' else 'CHECKSUM_MATCH' END "
        + "from most_recent_checksum, bitstream where "
        + "not exists( select 'x' from checksum_history where "
        + "most_recent_checksum.bitstream_id = checksum_history.bitstream_id ) "
        + "and most_recent_checksum.bitstream_id = bitstream.bitstream_id";
   
    /** Query that inserts results of recent check into the history table. */
    private static final String INSERT_HISTORY = "insert into checksum_history (  bitstream_id, process_start_date, "
            + " process_end_date, checksum_expected, checksum_calculated, result ) "
            + " values ( ?, ?, ?, ?, ?, ?)";

    private static final String INSERT_HISTORY_ORACLE = "insert into checksum_history (  check_id, bitstream_id, process_start_date, "
            + " process_end_date, checksum_expected, checksum_calculated, result ) "
            + " values ( checksum_history_seq.nextval, ?, ?, ?, ?, ?, ?)";

    /**
     * Deletes from the most_recent_checksum where the bitstream id is found
     */
    private static final String DELETE_BITSTREAM_HISTORY = "Delete from checksum_history "
            + "where bitstream_id = ?";

    /**
     * Logger for the checksum history DAO.
     */
    private static final Logger LOG = Logger
            .getLogger(ChecksumHistoryDAO.class);

    /**
     * Inserts results of checksum check into checksum_history table for a given
     * bitstream.
     * 
     * @param info
     *            the BitstreamInfo representing a checksum check.
     * 
     * @throws IllegalArgumentException
     *             if the <code>BitstreamInfo</code> is null.
     */
    public void insertHistory(BitstreamInfo info)
    {
        if (info == null)
        {
            throw new IllegalArgumentException(
                    "BitstreamInfo parameter may not be null");
        }

        Connection conn = null;
        PreparedStatement stmt = null;

        try
        {
            conn = DatabaseManager.getConnection();
            if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
	            stmt = conn.prepareStatement(INSERT_HISTORY_ORACLE);
            else
            	stmt = conn.prepareStatement(INSERT_HISTORY);
            stmt.setInt(1, info.getBitstreamId());
            stmt.setTimestamp(2, new java.sql.Timestamp(info
                    .getProcessStartDate().getTime()));
            stmt.setTimestamp(3, new java.sql.Timestamp(info
                    .getProcessEndDate().getTime()));
            stmt.setString(4, info.getStoredChecksum());
            stmt.setString(5, info.getCalculatedChecksum());
            stmt.setString(6, info.getChecksumCheckResult());
            stmt.executeUpdate();
            conn.commit();
        }
        catch (SQLException e)
        {
            LOG.error("Problem updating checksum row. " + e.getMessage(), e);
            throw new RuntimeException("Problem updating checksum row. "
                    + e.getMessage(), e);
        }
        finally
        {
            cleanup(stmt, conn);
        }
    }

    /**
     * Deletes the bitstream from the bitstream_history table if it exist.
     * 
     * @param id
     *            the bitstream id.
     * 
     * @return number of records deleted
     */
    protected int deleteHistoryForBitstreamInfo(int id, Connection conn)
    {
        PreparedStatement stmt = null;

        int numDeleted = 0;

        try
        {
            conn = DatabaseManager.getConnection();
            stmt = conn.prepareStatement(DELETE_BITSTREAM_HISTORY);
            stmt.setInt(1, id);

            numDeleted = stmt.executeUpdate();
            conn.commit();
        }
        catch (SQLException e)
        {
            LOG.error("Problem with inserting missing bitstream. "
                    + e.getMessage(), e);
            throw new RuntimeException("Problem inserting missing bitstream. "
                    + e.getMessage(), e);
        }
        finally
        {
            cleanup(stmt, conn);
        }

        return numDeleted;
    }

    /**
     * @param conn
     */
    protected void updateMissingBitstreams(Connection conn) throws SQLException
    {
        PreparedStatement stmt = null;
        try
        {
            if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
	            stmt = conn.prepareStatement(INSERT_MISSING_HISTORY_BITSTREAMS_ORACLE);
            else
            	stmt = conn.prepareStatement(INSERT_MISSING_HISTORY_BITSTREAMS);
            stmt.executeUpdate();
        }
        catch (SQLException e)
        {
            LOG.error("Problem updating missing history. " + e.getMessage(), e);
            throw new RuntimeException("Problem updating missing history. "
                    + e.getMessage(), e);
        }
        finally
        {
            cleanup(stmt);
        }
    }

    /**
     * Delete the history records from the database.
     * 
     * @param retentionDate
     *            any records older than this data are deleted.
     * @param result
     *            result code records must have for them to be deleted.
     * @param conn
     *            database connection.
     * @return number of records deleted.
     * @throws SQLException
     *             if database error occurs.
     */
    protected int deleteHistoryByDateAndCode(Date retentionDate, String result,
            Connection conn) throws SQLException
    {
        PreparedStatement update = null;

        try
        {
            update = conn
                    .prepareStatement("DELETE FROM checksum_history WHERE process_end_date<? AND result=?");
            update.setTimestamp(1, new Timestamp(retentionDate.getTime()));
            update.setString(2, result);
            return update.executeUpdate();
        }
        finally
        {
            cleanup(update);
        }

    }

    /**
     * Prune the history records from the database.
     * 
     * @param interests
     *            set of results and the duration of time before they are
     *            removed from the database
     * 
     * @return number of bitstreams deleted
     */
    public int prune(Map interests)
    {
        Connection conn = null;
        try
        {
            conn = DatabaseManager.getConnection();
            long now = System.currentTimeMillis();
            int count = 0;
            for (Iterator iter = interests.keySet().iterator(); iter.hasNext();)
            {
                String result = (String) iter.next();
                Long dur = (Long) interests.get(result);
                count += deleteHistoryByDateAndCode(new Date(now
                        - dur.longValue()), result, conn);
                conn.commit();
            }
            return count;
        }
        catch (SQLException e)
        {
            LOG.error("Problem pruning results: " + e.getMessage(), e);
            throw new RuntimeException("Problem pruning results: "
                    + e.getMessage(), e);
        }
        finally
        {
            DatabaseManager.freeConnection(conn);
        }
    }
}
