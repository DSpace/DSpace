/*
 * Copyright (c) 2004-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.checker;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.storage.rdbms.DatabaseManager;

/**
 * <p>
 * Database Access Object for bitstream information (metadata). Also performs
 * the needed insert/update/delete commands on the database for the checksum
 * checker.
 * </p>
 * 
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 * 
 */
public final class BitstreamInfoDAO extends DAOSupport
{
    /**
     * This value should be returned by <code>next()</code> to indicate that
     * there are no more values.
     */
    public static int SENTINEL = -1;

    /** Query that gets bitstream information for a specified ID. */
    private static final String FIND_BY_BITSTREAM_ID = "select bitstream.deleted, bitstream.store_number, bitstream.size_bytes, "
            + "bitstreamformatregistry.short_description, bitstream.bitstream_id,  "
            + "bitstream.user_format_description, bitstream.internal_id, "
            + "bitstream.source, bitstream.checksum_algorithm, bitstream.checksum, "
            + "bitstream.name, most_recent_checksum.last_process_end_date,"
            + "most_recent_checksum.to_be_processed "
            + "from bitstream left outer join bitstreamformatregistry on "
            + "bitstream.bitstream_format_id = bitstreamformatregistry.bitstream_format_id, "
            + "most_recent_checksum "
            + "where bitstream.bitstream_id = ? "
            + "and bitstream.bitstream_id = most_recent_checksum.bitstream_id";

    /**
     * Query that selects bitstream IDs from bitstream table that are not yet in
     * the most_recent_checksum table, and inserts them into
     * most_recent_checksum.
     */
    private static final String INSERT_MISSING_CHECKSUM_BITSTREAMS = "insert into most_recent_checksum ( "
            + "bitstream_id, to_be_processed, expected_checksum, current_checksum, "
            + "last_process_start_date, last_process_end_date, "
            + "checksum_algorithm, matched_prev_checksum, result ) "
            + "select bitstream.bitstream_id, "
            + "CASE WHEN bitstream.deleted = false THEN true ELSE false END, "
            + "CASE WHEN bitstream.checksum IS NULL THEN '' ELSE bitstream.checksum END, "
            + "CASE WHEN bitstream.checksum IS NULL THEN '' ELSE bitstream.checksum END, "
            + "?, ?, CASE WHEN bitstream.checksum_algorithm IS NULL "
            + "THEN 'MD5' ELSE bitstream.checksum_algorithm END, true, "
            + "CASE WHEN bitstream.deleted = true THEN 'BITSTREAM_MARKED_DELETED' else 'CHECKSUM_MATCH' END "
            + "from bitstream where not exists( "
            + "select 'x' from most_recent_checksum "
            + "where most_recent_checksum.bitstream_id = bitstream.bitstream_id )";

    private static final String INSERT_MISSING_CHECKSUM_BITSTREAMS_ORACLE = "insert into most_recent_checksum ( "
        + "bitstream_id, to_be_processed, expected_checksum, current_checksum, "
        + "last_process_start_date, last_process_end_date, "
        + "checksum_algorithm, matched_prev_checksum, result ) "
        + "select bitstream.bitstream_id, "
        + "CASE WHEN bitstream.deleted = 0 THEN 1 ELSE 0 END, "
        + "CASE WHEN bitstream.checksum IS NULL THEN '' ELSE bitstream.checksum END, "
        + "CASE WHEN bitstream.checksum IS NULL THEN '' ELSE bitstream.checksum END, "
        + "? AS last_process_start_date, ? AS last_process_end_date, CASE WHEN bitstream.checksum_algorithm IS NULL "
        + "THEN 'MD5' ELSE bitstream.checksum_algorithm END, 1, "
        + "CASE WHEN bitstream.deleted = 1 THEN 'BITSTREAM_MARKED_DELETED' else 'CHECKSUM_MATCH' END "
        + "from bitstream where not exists( "
        + "select 'x' from most_recent_checksum "
        + "where most_recent_checksum.bitstream_id = bitstream.bitstream_id )";
    
    /**
     * Query that updates most_recent_checksum table with checksum result for
     * specified bitstream ID.
     */
    private static final String UPDATE_CHECKSUM = "UPDATE  most_recent_checksum "
            + "SET current_checksum = ?, expected_checksum = ?, matched_prev_checksum = ?, to_be_processed= ?, "
            + "last_process_start_date=?, last_process_end_date=?, result=? WHERE bitstream_id = ? ";

    /**
     * Deletes from the most_recent_checksum where the bitstream id is found
     */
    private static final String DELETE_BITSTREAM_INFO = "Delete from most_recent_checksum "
            + "where bitstream_id = ?";

    /**
     * This selects the next bitstream in order of last processing end date. The
     * timestamp is truncated to milliseconds this is because the Date for java
     * does not support nanoseconds and milliseconds were considered accurate
     * enough
     */
    public static final String GET_OLDEST_BITSTREAM = "select bitstream_id  "
            + "from most_recent_checksum " + "where to_be_processed = true "
            + "order by date_trunc('milliseconds', last_process_end_date), "
            + "bitstream_id " + "ASC LIMIT 1";

    public static final String GET_OLDEST_BITSTREAM_ORACLE = "SELECT bitstream_id FROM (select bitstream_id  "
        + "from most_recent_checksum " + "where to_be_processed = 1 "
        + "order by trunc(last_process_end_date, 'mi'), "
        + "bitstream_id " + "ASC) WHERE rownum=1";
    
    /**
     * Selects the next bitstream in order of last processing end date, ensuring
     * that no bitstream is checked more than once since the date parameter
     * used.
     */
    public static final String GET_OLDEST_BITSTREAM_DATE = "select bitstream_id  "
            + "from most_recent_checksum "
            + "where to_be_processed = true "
            + "and last_process_start_date < ? "
            + "order by date_trunc('milliseconds', last_process_end_date), "
            + "bitstream_id " + "ASC LIMIT 1";

    public static final String GET_OLDEST_BITSTREAM_DATE_ORACLE = "SELECT bitstream_id FROM (select bitstream_id  "
        + "from most_recent_checksum "
        + "where to_be_processed = 1 "
        + "and last_process_start_date < ? "
        + "order by trunc(last_process_end_date, 'mi'), "
        + "bitstream_id " + "ASC) WHERE rownum=1";
    
    /** SQL query to retrieve bitstreams for a given item. */
    private static final String ITEM_BITSTREAMS = "SELECT b2b.bitstream_id "
            + "FROM bundle2bitstream b2b, item2bundle i2b WHERE "
            + "b2b.bundle_id=i2b.bundle_id AND i2b.item_id=?";

    /** SQL query to retrieve bitstreams for a given collection. */
    private static final String COLLECTION_BITSTREAMS = "SELECT b2b.bitstream_id "
            + "FROM bundle2bitstream b2b, item2bundle i2b, collection2item c2i WHERE "
            + "b2b.bundle_id=i2b.bundle_id AND c2i.item_id=i2b.item_id AND c2i.collection_id=?";

    /** SQL query to retrieve bitstreams for a given community. */
    private static final String COMMUNITY_BITSTREAMS = "SELECT b2b.bitstream_id FROM bundle2bitstream b2b, item2bundle i2b, collection2item c2i, community2collection c2c WHERE b2b.bundle_id=i2b.bundle_id AND c2i.item_id=i2b.item_id AND c2c.collection_id=c2i.collection_id AND c2c.community_id=?";

    /** Standard Log4J logger. */
    private static final Logger LOG = Logger.getLogger(BitstreamInfoDAO.class);

    /**
     * History data access object for checksum_history table
     */
    private ChecksumHistoryDAO checksumHistoryDAO;

    /**
     * Default constructor
     */
    public BitstreamInfoDAO()
    {
        checksumHistoryDAO = new ChecksumHistoryDAO();
    }

    /**
     * Updates most_recent_checksum with latest checksum and result of
     * comparison with previous checksum.
     * 
     * @param info
     *            The BitstreamInfo to update.
     * 
     * @throws IllegalArgumentException
     *             if the BitstreamInfo given is null.
     */
    public void update(BitstreamInfo info)
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
            stmt = conn.prepareStatement(UPDATE_CHECKSUM);
            stmt.setString(1, (info.getCalculatedChecksum() != null) ? info
                    .getCalculatedChecksum() : "");
            stmt.setString(2, info.getStoredChecksum());
            stmt.setBoolean(3, ChecksumCheckResults.CHECKSUM_MATCH.equals(info
                    .getChecksumCheckResult()));
            stmt.setBoolean(4, info.getToBeProcessed());
            stmt.setTimestamp(5, new Timestamp(info.getProcessStartDate()
                    .getTime()));
            stmt.setTimestamp(6, new Timestamp(info.getProcessEndDate()
                    .getTime()));
            stmt.setString(7, info.getChecksumCheckResult());
            stmt.setInt(8, info.getBitstreamId());
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
     * Find a bitstream by its id.
     * 
     * @param id
     *            the bitstream id
     * 
     * @return the bitstream information needed for checksum validation. Returns
     *         null if bitstream info isn't found.
     */
    public BitstreamInfo findByBitstreamId(int id)
    {
        Connection conn = null;
        BitstreamInfo info = null;
        PreparedStatement prepStmt = null;

        try
        {
            // create the connection and execute the statement
            conn = DatabaseManager.getConnection();

            prepStmt = conn.prepareStatement(FIND_BY_BITSTREAM_ID);

            prepStmt.setInt(1, id);

            ResultSet rs = prepStmt.executeQuery();

            // if the bitstream is found return it
            if (rs.next())
            {
                info = new BitstreamInfo(rs.getBoolean("deleted"), rs
                        .getInt("store_number"), rs.getInt("size_bytes"), rs
                        .getString("short_description"), rs
                        .getInt("bitstream_id"), rs
                        .getString("user_format_description"), rs
                        .getString("internal_id"), rs.getString("source"), rs
                        .getString("checksum_algorithm"), rs
                        .getString("checksum"), rs.getString("name"), rs
                        .getTimestamp("last_process_end_date"), rs
                        .getBoolean("to_be_processed"), new Date());
            }
        }
        catch (SQLException e)
        {
            LOG.warn("Bitstream metadata could not be retrieved. "
                    + e.getMessage(), e);
        }
        finally
        {
            cleanup(prepStmt, conn);
        }

        return info;
    }

    /**
     * Queries the bitstream table for bitstream IDs that are not yet in the
     * most_recent_checksum table, and inserts them into the
     * most_recent_checksum and checksum_history tables.
     */
    public void updateMissingBitstreams()
    {
        Connection conn = null;
        PreparedStatement stmt = null;

        try
        {
            LOG.debug("updating missing bitstreams");
            conn = DatabaseManager.getConnection();
            if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
                stmt = conn.prepareStatement(INSERT_MISSING_CHECKSUM_BITSTREAMS_ORACLE);
            else
            	stmt = conn.prepareStatement(INSERT_MISSING_CHECKSUM_BITSTREAMS);
            stmt.setTimestamp(1, new java.sql.Timestamp(new Date().getTime()));
            stmt.setTimestamp(2, new java.sql.Timestamp(new Date().getTime()));
            stmt.executeUpdate();

            checksumHistoryDAO.updateMissingBitstreams(conn);
            conn.commit();
        }
        catch (SQLException e)
        {
            LOG.error(
                    "Problem inserting missing bitstreams. " + e.getMessage(),
                    e);
            throw new RuntimeException("Problem inserting missing bitstreams. "
                    + e.getMessage(), e);
        }
        finally
        {
            cleanup(stmt, conn);
        }
    }

    /**
     * Deletes the bitstream from the most_recent_checksum table if it exist.
     * 
     * @param id
     *            the bitstream id.
     * 
     * @return number of records deleted
     */
    protected int deleteBitstreamInfo(int id, Connection conn)
    {
        PreparedStatement stmt = null;

        int numDeleted = 0;

        try
        {
            stmt = conn.prepareStatement(DELETE_BITSTREAM_INFO);
            stmt.setInt(1, id);

            numDeleted = stmt.executeUpdate();

            if (numDeleted > 1)
            {
                conn.rollback();
                throw new IllegalStateException(
                        "Too many rows deleted! Number of rows deleted: "
                                + numDeleted
                                + " only one row should be deleted for bitstream id "
                                + id);
            }
        }
        catch (SQLException e)
        {
            LOG.error("Problem deleting bitstream. " + e.getMessage(), e);
            throw new RuntimeException("Problem deleting bitstream. "
                    + e.getMessage(), e);
        }
        finally
        {
            cleanup(stmt);
        }

        return numDeleted;
    }

    public int deleteBitstreamInfoWithHistory(int id)
    {
        Connection conn = null;
        int numDeleted = 0;

        try
        {
            conn = DatabaseManager.getConnection();
            deleteBitstreamInfo(id, conn);
            checksumHistoryDAO.deleteHistoryForBitstreamInfo(id, conn);
            conn.commit();
        }
        catch (SQLException e)
        {
            LOG.error("Problem deleting bitstream. " + e.getMessage(), e);
            throw new RuntimeException("Problem deleting bitstream. "
                    + e.getMessage(), e);
        }
        finally
        {
            cleanup(conn);
        }

        return numDeleted;

    }

    /**
     * Get the oldest bitstream in the most recent checksum table. If more than
     * one found the first one in the result set is returned.
     * 
     * @return the bitstream id or -1 if the no bitstreams are found
     * 
     */
    public int getOldestBitstream()
    {
        Connection conn = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        try
        {

            conn = DatabaseManager.getConnection();
            if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
            	prepStmt = conn.prepareStatement(GET_OLDEST_BITSTREAM_ORACLE);
            else
            	prepStmt = conn.prepareStatement(GET_OLDEST_BITSTREAM);
            rs = prepStmt.executeQuery();
            if (rs.next())
            {
                return rs.getInt(1);
            }
            else
            {
                return SENTINEL;
            }
        }
        catch (SQLException e)
        {
            LOG.error("Problem with get oldest bitstream " + e.getMessage(), e);
            throw new RuntimeException("Oldest bitstream error. "
                    + e.getMessage(), e);

        }
        finally
        {
            cleanup(prepStmt, conn);

        }
    }

    /**
     * Returns the oldest bistream that in the set of bitstreams that are less
     * than the specified date. If no bitstreams are found -1 is returned.
     * 
     * @param lessThanDate
     * @return id of olded bitstream or -1 if not bistreams are found
     */
    public int getOldestBitstream(Timestamp lessThanDate)
    {
        Connection conn = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        try
        {
            conn = DatabaseManager.getConnection();
            if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
            	prepStmt = conn.prepareStatement(GET_OLDEST_BITSTREAM_DATE_ORACLE);
            else
            	prepStmt = conn.prepareStatement(GET_OLDEST_BITSTREAM_DATE);
            prepStmt.setTimestamp(1, lessThanDate);
            rs = prepStmt.executeQuery();
            if (rs.next())
            {
                return rs.getInt(1);
            }
            else
            {
                return SENTINEL;
            }
        }
        catch (SQLException e)
        {
            LOG.error("get oldest bitstream less than date " + e.getMessage(),
                    e);
            throw new RuntimeException("get oldest bitstream less than date. "
                    + e.getMessage(), e);

        }
        finally
        {
            cleanup(prepStmt, conn);

        }
    }

    /**
     * Get the bitstream ids for a given Item
     * 
     * @param itemId
     * @return the list of bitstream ids for this item
     */
    public List getItemBitstreams(int itemId)
    {
        List ids = new ArrayList();

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try
        {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement(ITEM_BITSTREAMS);
            ps.setInt(1, itemId);

            rs = ps.executeQuery();

            while (rs.next())
            {
                ids.add(new Integer(rs.getInt(1)));
            }

        }
        catch (SQLException e)
        {
            LOG.error("get item bitstreams " + e.getMessage(), e);
            throw new RuntimeException(
                    "get item bitstreams. " + e.getMessage(), e);

        }
        finally
        {
            cleanup(ps, conn, rs);
        }

        return ids;
    }

    /**
     * Get the bitstream ids for a given collection
     * 
     * @param itemId
     * @return the list of bitstream ids for this item
     */
    public List getCollectionBitstreams(int collectionId)
    {
        List ids = new ArrayList();

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try
        {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement(COLLECTION_BITSTREAMS);
            ps.setInt(1, collectionId);

            rs = ps.executeQuery();

            while (rs.next())
            {
                ids.add(new Integer(rs.getInt(1)));
            }

        }
        catch (SQLException e)
        {
            LOG.error("get item bitstreams " + e.getMessage(), e);
            throw new RuntimeException(
                    "get item bitstreams. " + e.getMessage(), e);

        }
        finally
        {
            cleanup(ps, conn, rs);
        }

        return ids;
    }

    /**
     * Get the bitstream ids for a given community
     * 
     * @param itemId
     * @return the list of bitstream ids for this item
     */
    public List getCommunityBitstreams(int communityId)
    {
        List ids = new ArrayList();

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try
        {
            conn = DatabaseManager.getConnection();
            ps = conn.prepareStatement(COMMUNITY_BITSTREAMS);
            ps.setInt(1, communityId);

            rs = ps.executeQuery();

            while (rs.next())
            {
                ids.add(new Integer(rs.getInt(1)));
            }

        }
        catch (SQLException e)
        {
            LOG.error("get item bitstreams " + e.getMessage(), e);
            throw new RuntimeException(
                    "get item bitstreams. " + e.getMessage(), e);

        }
        finally
        {
            cleanup(ps, conn, rs);
        }

        return ids;
    }

}
