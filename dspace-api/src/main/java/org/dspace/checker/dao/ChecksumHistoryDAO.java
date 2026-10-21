/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker.dao;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import org.dspace.checker.ChecksumHistory;
import org.dspace.checker.ChecksumResultCode;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

/**
 * Database Access Object interface class for the ChecksumHistory object.
 * The implementation of this class is responsible for all database calls for the ChecksumHistory object and is
 * autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface ChecksumHistoryDAO extends GenericDAO<ChecksumHistory> {

    /**
     * Delete all ChecksumHistory rows with retention date before the given and
     * the specified result code.
     *
     * @param context            The relevant DSpace Context.
     * @param retentionDate      row must be older than this to be deleted.
     * @param checksumResultCode row must have this result to be deleted.
     * @return number of rows deleted.
     * @throws SQLException if database error
     */
    public int deleteByDateAndCode(Context context, Instant retentionDate, ChecksumResultCode checksumResultCode)
        throws SQLException;

    /**
     * Delete all ChecksumHistory rows for the given Bitstream.
     *
     * @param context   The relevant DSpace Context.
     * @param bitstream which bitstream's checksums to delete
     * @throws SQLException if database error
     */
    public void deleteByBitstream(Context context, Bitstream bitstream) throws SQLException;

    /**
     * Find a paginated list of ChecksumHistory rows for the given Bitstream.
     *
     * @param context   The relevant DSpace Context.
     * @param bitstream which bitstream's checksum history to retrieve.
     * @param pageSize  the maximum number of results to return.
     * @param offset    the starting index for the results.
     * @return list of ChecksumHistory matching the criteria.
     * @throws SQLException if database error
     */
    List<ChecksumHistory> findByBitstream(Context context, Bitstream bitstream, int pageSize, int offset)
            throws SQLException;

    /**
     * Count the total number of ChecksumHistory rows for the given Bitstream.
     *
     * @param context   The relevant DSpace Context.
     * @param bitstream which bitstream's checksum history count to retrieve.
     * @return total number of ChecksumHistory rows for the bitstream.
     * @throws SQLException if database error
     */
    int countByBitstream(Context context, Bitstream bitstream) throws SQLException;

    /**
     * Find a paginated list of ChecksumHistory rows for the specified result code.
     *
     * @param context            The relevant DSpace Context.
     * @param checksumResultCode row must have this result to be retrieved.
     * @param pageSize           the maximum number of results to return.
     * @param offset             the starting index for the results.
     * @return list of ChecksumHistory matching the criteria.
     * @throws SQLException if database error
     */
    List<ChecksumHistory> findByResultCode(Context context, ChecksumResultCode checksumResultCode,
                                           int pageSize, int offset) throws SQLException;

    /**
     * Count the total number of ChecksumHistory rows for the specified result code.
     *
     * @param context            The relevant DSpace Context.
     * @param checksumResultCode row must have this result to be counted.
     * @return total number of ChecksumHistory rows with the specified result code.
     * @throws SQLException if database error
     */
    int countByResultCode(Context context, ChecksumResultCode checksumResultCode) throws SQLException;

    /**
     * Find a single ChecksumHistory by its Long ID.
     *
     * @param context DSpace context
     * @param id      Check ID
     * @return ChecksumHistory or null if not found
     * @throws SQLException if database error
     */
    ChecksumHistory findByID(Context context, Long id) throws SQLException;

    /**
     * Count all ChecksumHistory records in the database.
     *
     * @param context DSpace context
     * @return total number of ChecksumHistory records
     * @throws SQLException if database error
     */
    int countTotal(Context context) throws SQLException;
}
