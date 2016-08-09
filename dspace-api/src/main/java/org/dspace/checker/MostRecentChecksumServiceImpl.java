/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import org.apache.log4j.Logger;
import org.dspace.checker.dao.MostRecentChecksumDAO;
import org.dspace.checker.service.ChecksumResultService;
import org.dspace.checker.service.MostRecentChecksumService;
import org.dspace.content.Bitstream;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * Service implementation for the MostRecentChecksum object.
 * This class is responsible for all business logic calls for the MostRecentChecksum object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class MostRecentChecksumServiceImpl implements MostRecentChecksumService
{
    private static final Logger log = Logger.getLogger(MostRecentChecksumServiceImpl.class);

    @Autowired(required = true)
    protected MostRecentChecksumDAO mostRecentChecksumDAO;

    @Autowired(required = true)
    protected ChecksumResultService checksumResultService;

    @Autowired(required = true)
    protected BitstreamService bitstreamService;

    protected MostRecentChecksumServiceImpl()
    {

    }

    @Override
    public MostRecentChecksum getNonPersistedObject()
    {
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
     * @param context Context
     * @param startDate
     *            the start of the date range
     * @param endDate
     *            the end of the date range
     * @return a list of BitstreamHistoryInfo objects
     * @throws SQLException if database error
     */
    @Override
    public List<MostRecentChecksum> findNotProcessedBitstreamsReport(Context context, Date startDate, Date endDate) throws SQLException
    {
        return mostRecentChecksumDAO.findByNotProcessedInDateRange(context, startDate, endDate);
    }

    /**
     * Select the most recent bitstream for a given date range with the
     * specified status.
     *
     * @param context Context
     * @param startDate
     *            the start date range
     * @param endDate
     *            the end date range.
     * @param resultCode
     *            the result code
     *
     * @return a list of BitstreamHistoryInfo objects
     * @throws SQLException if database error
     */
    @Override
    public List<MostRecentChecksum> findBitstreamResultTypeReport(Context context, Date startDate, Date endDate, ChecksumResultCode resultCode) throws SQLException {
        return mostRecentChecksumDAO.findByResultTypeInDateRange(context, startDate, endDate, resultCode);
    }

    /**
     * Queries the bitstream table for bitstream IDs that are not yet in the
     * most_recent_checksum table, and inserts them into the
     * most_recent_checksum and checksum_history tables.
     * @param context Context
     * @throws SQLException if database error
     */
    @Override
    public void updateMissingBitstreams(Context context) throws SQLException {
//                "insert into most_recent_checksum ( "
//                + "bitstream_id, to_be_processed, expected_checksum, current_checksum, "
//                + "last_process_start_date, last_process_end_date, "
//                + "checksum_algorithm, matched_prev_checksum, result ) "
//                + "select bitstream.bitstream_id, "
//                + "CASE WHEN bitstream.deleted = false THEN true ELSE false END, "
//                + "CASE WHEN bitstream.checksum IS NULL THEN '' ELSE bitstream.checksum END, "
//                + "CASE WHEN bitstream.checksum IS NULL THEN '' ELSE bitstream.checksum END, "
//                + "?, ?, CASE WHEN bitstream.checksum_algorithm IS NULL "
//                + "THEN 'MD5' ELSE bitstream.checksum_algorithm END, true, "
//                + "CASE WHEN bitstream.deleted = true THEN 'BITSTREAM_MARKED_DELETED' else 'CHECKSUM_MATCH' END "
//                + "from bitstream where not exists( "
//                + "select 'x' from most_recent_checksum "
//                + "where most_recent_checksum.bitstream_id = bitstream.bitstream_id )";

        List<Bitstream> unknownBitstreams = bitstreamService.findBitstreamsWithNoRecentChecksum(context);
        for (Bitstream bitstream : unknownBitstreams)
        {
            log.info(bitstream + " " + bitstream.getID().toString() + " " + bitstream.getName());

            MostRecentChecksum mostRecentChecksum = new MostRecentChecksum();
            mostRecentChecksum.setBitstream(bitstream);
            //Only process if our bitstream isn't deleted
            mostRecentChecksum.setToBeProcessed(!bitstream.isDeleted());
            if(bitstream.getChecksum() == null)
            {
                mostRecentChecksum.setCurrentChecksum("");
                mostRecentChecksum.setExpectedChecksum("");
            }else{
                mostRecentChecksum.setCurrentChecksum(bitstream.getChecksum());
                mostRecentChecksum.setExpectedChecksum(bitstream.getChecksum());
            }
            mostRecentChecksum.setProcessStartDate(new Date());
            mostRecentChecksum.setProcessEndDate(new Date());
            if(bitstream.getChecksumAlgorithm() == null)
            {
                mostRecentChecksum.setChecksumAlgorithm("MD5");
            }else{
                mostRecentChecksum.setChecksumAlgorithm(bitstream.getChecksumAlgorithm());
            }
            mostRecentChecksum.setMatchedPrevChecksum(true);
            ChecksumResult checksumResult;
            if(bitstream.isDeleted())
            {
                checksumResult = checksumResultService.findByCode(context, ChecksumResultCode.BITSTREAM_MARKED_DELETED);
            } else {
                checksumResult = checksumResultService.findByCode(context, ChecksumResultCode.CHECKSUM_MATCH);
            }
            mostRecentChecksum.setChecksumResult(checksumResult);
            mostRecentChecksumDAO.create(context,  mostRecentChecksum);
            mostRecentChecksumDAO.save(context, mostRecentChecksum);
        }
    }

    @Override
    public void deleteByBitstream(Context context, Bitstream bitstream) throws SQLException
    {
        mostRecentChecksumDAO.deleteByBitstream(context, bitstream);
    }

    /**
     * Get the oldest most recent checksum record. If more than
     * one found the first one in the result set is returned.
     *
     * @param context COntext
     * @return the oldest MostRecentChecksum or NULL if the table is empty
     * @throws SQLException if database error
     *
     */
    @Override
    public MostRecentChecksum findOldestRecord(Context context) throws SQLException
    {
        return mostRecentChecksumDAO.getOldestRecord(context);
    }

    /**
     * Returns the oldest bitstream that in the set of bitstreams that are less
     * than the specified date. If no bitstreams are found -1 is returned.
     *
     * @param context context
     * @param lessThanDate date
     * @return id of olded bitstream or -1 if not bitstreams are found
     * @throws SQLException if database error
     */
    @Override
    public MostRecentChecksum findOldestRecord(Context context, Date lessThanDate) throws SQLException
    {
        return mostRecentChecksumDAO.getOldestRecord(context, lessThanDate);
    }

    @Override
    public List<MostRecentChecksum> findNotInHistory(Context context) throws SQLException
    {
        return mostRecentChecksumDAO.findNotInHistory(context);
    }

    @Override
    public void update(Context context, MostRecentChecksum mostRecentChecksum) throws SQLException {
        mostRecentChecksumDAO.save(context, mostRecentChecksum);
    }
}
