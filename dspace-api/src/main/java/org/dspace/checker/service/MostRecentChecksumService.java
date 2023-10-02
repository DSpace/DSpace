/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker.service;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.dspace.checker.ChecksumResultCode;
import org.dspace.checker.MostRecentChecksum;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;
import org.hibernate.Session;

/**
 * Service interface class for the MostRecentChecksum object.
 * The implementation of this class is responsible for all business logic calls for the MostRecentChecksum object and
 * is autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface MostRecentChecksumService {

    public MostRecentChecksum getNonPersistedObject();

    public MostRecentChecksum findByBitstream(Session session, Bitstream bitstream) throws SQLException;

    /**
     * Find all bitstreams that were set to not be processed for the specified
     * date range.
     *
     * @param session   current request's database context.
     * @param startDate the start of the date range
     * @param endDate   the end of the date range
     * @return a list of BitstreamHistoryInfo objects
     * @throws SQLException if database error
     */
    public List<MostRecentChecksum> findNotProcessedBitstreamsReport(Session session, Date startDate, Date endDate)
        throws SQLException;

    /**
     * Select the most recent bitstream for a given date range with the
     * specified status.
     *
     * @param session    current request's database context.
     * @param startDate  the start date range
     * @param endDate    the end date range.
     * @param resultCode the result code
     * @return a list of BitstreamHistoryInfo objects
     * @throws SQLException if database error
     */
    public List<MostRecentChecksum> findBitstreamResultTypeReport(Session session, Date startDate, Date endDate,
                                                                  ChecksumResultCode resultCode) throws SQLException;

    public void updateMissingBitstreams(Context context) throws SQLException;

    public void deleteByBitstream(Context context, Bitstream bitstream) throws SQLException;

    /**
     * Get the oldest most recent checksum record. If more than
     * one found the first one in the result set is returned.
     *
     * @param session current request's database context.
     * @return the oldest MostRecentChecksum or NULL if the table is empty
     * @throws SQLException if database error
     */
    public MostRecentChecksum findOldestRecord(Session session) throws SQLException;

    public MostRecentChecksum findOldestRecord(Session session, Date lessThanDate) throws SQLException;

    public List<MostRecentChecksum> findNotInHistory(Session session) throws SQLException;

    public void update(Context context, MostRecentChecksum mostRecentChecksum) throws SQLException;
}
