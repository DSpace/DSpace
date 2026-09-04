/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker.service;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.dspace.checker.ChecksumHistory;
import org.dspace.checker.ChecksumResultCode;
import org.dspace.checker.MostRecentChecksum;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;

/**
 * Service interface class for the ChecksumHistory object.
 * The implementation of this class is responsible for all business logic calls for the ChecksumHistory object and is
 * autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface ChecksumHistoryService {

    public void updateMissingBitstreams(Context context) throws SQLException;

    public void addHistory(Context context, MostRecentChecksum mostRecentChecksum) throws SQLException;

    public int deleteByDateAndCode(Context context, Instant retentionDate, ChecksumResultCode result)
        throws SQLException;

    public void deleteByBitstream(Context context, Bitstream bitstream) throws SQLException;

    /**
     * Prune the history records from the database.
     *
     * @param context   context
     * @param interests set of results and the duration of time before they are
     *                  removed from the database
     * @return number of bitstreams deleted
     * @throws SQLException if database error
     */
    public int prune(Context context, Map<ChecksumResultCode, Long> interests) throws SQLException;

    /**
     * Find a single ChecksumHistory by its ID.
     * <p>
     *  Note: A custom method is required here because {@link org.dspace.core.GenericDAO}
     *  does not provide a findByID method that accepts a Long primary key.
     * </p>
     *
     * @param context DSpace context
     * @param id      Check ID
     * @return ChecksumHistory or null if not found
     * @throws SQLException if database error
     */
    ChecksumHistory find(Context context, Long id) throws SQLException;

    /**
     * Find all ChecksumHistory records with pagination.
     *
     * @param context  DSpace context
     * @param pageSize the maximum number of results to return
     * @param offset   the starting index for the results
     * @return list of ChecksumHistory
     * @throws SQLException if database error
     */
    List<ChecksumHistory> findAll(Context context, int pageSize, int offset) throws SQLException;

    /**
     * Count all ChecksumHistory records in the database.
     *
     * @param context DSpace context
     * @return total number of ChecksumHistory records
     * @throws SQLException if database error
     */
    int countTotal(Context context) throws SQLException;

    /**
     * Find a paginated list of ChecksumHistory records for the given Bitstream.
     *
     * @param context   The relevant DSpace Context.
     * @param bitstream the bitstream whose checksum history to retrieve.
     * @param pageSize  the maximum number of results to return.
     * @param offset    the starting index for the results.
     * @return a paginated list of ChecksumHistory matching the criteria.
     * @throws SQLException if database error
     */
    List<ChecksumHistory> findByBitstream(Context context, Bitstream bitstream, int pageSize, int offset)
            throws SQLException;

    /**
     * Count the total number of ChecksumHistory records for the given Bitstream.
     *
     * @param context   The relevant DSpace Context.
     * @param bitstream the bitstream whose checksum history count to retrieve.
     * @return the total number of ChecksumHistory rows for the bitstream.
     * @throws SQLException if database error
     */
    int countByBitstream(Context context, Bitstream bitstream) throws SQLException;

    /**
     * Find a paginated list of ChecksumHistory records for the specified result code.
     *
     * @param context            The relevant DSpace Context.
     * @param checksumResultCode the result code to filter by.
     * @param pageSize           the maximum number of results to return.
     * @param offset             the starting index for the results.
     * @return a paginated list of ChecksumHistory matching the criteria.
     * @throws SQLException if database error
     */
    List<ChecksumHistory> findByResultCode(Context context, ChecksumResultCode checksumResultCode,
                                           int pageSize, int offset) throws SQLException;

    /**
     * Count the total number of ChecksumHistory records for the specified result code.
     *
     * @param context            The relevant DSpace Context.
     * @param checksumResultCode the result code to count by.
     * @return the total number of ChecksumHistory rows with the specified result code.
     * @throws SQLException if database error
     */
    int countByResultCode(Context context, ChecksumResultCode checksumResultCode) throws SQLException;
}
