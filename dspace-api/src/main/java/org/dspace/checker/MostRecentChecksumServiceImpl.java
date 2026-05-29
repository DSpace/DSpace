/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.checker.dao.MostRecentChecksumDAO;
import org.dspace.checker.service.ChecksumResultService;
import org.dspace.checker.service.MostRecentChecksumService;
import org.dspace.content.Bitstream;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the MostRecentChecksum object.
 * This class is responsible for all business logic calls for the MostRecentChecksum object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class MostRecentChecksumServiceImpl implements MostRecentChecksumService {
    private static final Logger log =
            org.apache.logging.log4j.LogManager.getLogger(MostRecentChecksumServiceImpl.class);

    @Autowired(required = true)
    protected MostRecentChecksumDAO mostRecentChecksumDAO;

    @Autowired(required = true)
    protected ChecksumResultService checksumResultService;

    @Autowired(required = true)
    protected BitstreamService bitstreamService;

    protected MostRecentChecksumServiceImpl() {

    }

    @Override
    public MostRecentChecksum getNonPersistedObject() {
        return new MostRecentChecksum();
    }

    @Override
    public MostRecentChecksum findByBitstream(Context context, Bitstream bitstream) throws SQLException {
        return mostRecentChecksumDAO.findByBitstream(context, bitstream);
    }

    /**
     * Find all bitstreams that were set to not be processed for the specified
     * date range.
     *
     * @param context   Context
     * @param startDate the start of the date range
     * @param endDate   the end of the date range
     * @return a list of BitstreamHistoryInfo objects
     * @throws SQLException if database error
     */
    @Override
    public List<MostRecentChecksum> findNotProcessedBitstreamsReport(Context context, Instant startDate,
                                                                     Instant endDate)
        throws SQLException {
        return mostRecentChecksumDAO.findByNotProcessedInDateRange(context, startDate, endDate);
    }

    /**
     * Select the most recent bitstream for a given date range with the
     * specified status.
     *
     * @param context    Context
     * @param startDate  the start date range
     * @param endDate    the end date range.
     * @param resultCode the result code
     * @return a list of BitstreamHistoryInfo objects
     * @throws SQLException if database error
     */
    @Override
    public List<MostRecentChecksum> findBitstreamResultTypeReport(Context context, Instant startDate,
                                                                  Instant endDate,
                                                                  ChecksumResultCode resultCode) throws SQLException {
        return mostRecentChecksumDAO.findByResultTypeInDateRange(context, startDate, endDate, resultCode);
    }

    /**
     * Queries the bitstream table for bitstream IDs that are not yet in the
     * most_recent_checksum table, and inserts them into the
     * most_recent_checksum table.
     * @param context Context
     * @throws SQLException if database error
     */
    @Override
    public void updateMissingBitstreams(Context context) throws SQLException {
        log.info("Retrieving missing bitsreams (bitstream IDs that are not yet in most_recent_checksum table)...");
        int updated = mostRecentChecksumDAO.updateMissingBitstreams(context);
        log.info("Updated most_recent_checksum for " + updated + " bitstreams.");
        log.info("Missing bitsreams processing done.");
    }

    @Override
    public void deleteByBitstream(Context context, Bitstream bitstream) throws SQLException {
        mostRecentChecksumDAO.deleteByBitstream(context, bitstream);
    }

    /**
     * Get the oldest most recent checksum record. If more than
     * one found the first one in the result set is returned.
     *
     * @param context COntext
     * @return the oldest MostRecentChecksum or NULL if the table is empty
     * @throws SQLException if database error
     */
    @Override
    public MostRecentChecksum findOldestRecord(Context context) throws SQLException {
        return mostRecentChecksumDAO.getOldestRecord(context);
    }

    /**
     * Returns the oldest bitstream that in the set of bitstreams that are less
     * than the specified date. If no bitstreams are found -1 is returned.
     *
     * @param context      context
     * @param lessThanDate date
     * @return id of olded bitstream or -1 if not bitstreams are found
     * @throws SQLException if database error
     */
    @Override
    public MostRecentChecksum findOldestRecord(Context context, Instant lessThanDate) throws SQLException {
        return mostRecentChecksumDAO.getOldestRecord(context, lessThanDate);
    }

    @Override
    public List<MostRecentChecksum> findNotInHistory(Context context) throws SQLException {
        return mostRecentChecksumDAO.findNotInHistory(context);
    }

    @Override
    public void update(Context context, MostRecentChecksum mostRecentChecksum) throws SQLException {
        mostRecentChecksumDAO.save(context, mostRecentChecksum);
    }
}
