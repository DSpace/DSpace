/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker.service;

import org.dspace.checker.ChecksumResultCode;
import org.dspace.checker.MostRecentChecksum;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

/**
 * Service interface class for the ChecksumHistory object.
 * The implementation of this class is responsible for all business logic calls for the ChecksumHistory object and is autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface ChecksumHistoryService {

    public void updateMissingBitstreams(Context context) throws SQLException;

    public void addHistory(Context context, MostRecentChecksum mostRecentChecksum) throws SQLException;

    public int deleteByDateAndCode(Context context, Date retentionDate, ChecksumResultCode result) throws SQLException;

    public void deleteByBitstream(Context context, Bitstream bitstream) throws SQLException;

    /**
     * Prune the history records from the database.
     *
     * @param context context
     * @param interests
     *            set of results and the duration of time before they are
     *            removed from the database
     *
     * @return number of bitstreams deleted
     * @throws SQLException if database error
     */
    public int prune(Context context, Map<ChecksumResultCode, Long> interests) throws SQLException;
}
