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
        ;
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
    public List getBitstreamResultTypeReport(Date startDate, Date endDate,
            String resultCode)
    {
        List bitstreamHistory = new LinkedList();

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
    public List getNotProcessedBitstreamsReport(Date startDate, Date endDate)
    {
        List bitstreamHistory = new LinkedList();

        Connection conn = null;
        PreparedStatement prepStmt = null;
        ResultSet rs = null;

        try
        {
            // create the connection and execute the statement
            conn = DatabaseManager.getConnection();

            if ("oracle".equals(ConfigurationManager.getProperty("db.name")))
            	prepStmt = conn.prepareStatement(DATE_RANGE_NOT_PROCESSED_BITSTREAMS_ORACLE);
           	else
            	prepStmt = conn.prepareStatement(DATE_RANGE_NOT_PROCESSED_BITSTREAMS);

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
    public List getUnknownBitstreams()
    {
        List unknownBitstreams = new LinkedList();

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
            cleanup(prepStmt, conn, rs);
        }

        return unknownBitstreams;
    }
}
