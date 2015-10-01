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
import java.util.List;

/**
 * Service interface class for the MostRecentChecksum object.
 * The implementation of this class is responsible for all business logic calls for the MostRecentChecksum object and is autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface MostRecentChecksumService {

    public MostRecentChecksum getNonPersistedObject();

    public MostRecentChecksum findByBitstream(Context context, Bitstream bitstream) throws SQLException;

    public List<MostRecentChecksum> findNotProcessedBitstreamsReport(Context context, Date startDate, Date endDate) throws SQLException;

    public List<MostRecentChecksum> findBitstreamResultTypeReport(Context context, Date startDate, Date endDate, ChecksumResultCode resultCode) throws SQLException;

    public void updateMissingBitstreams(Context context) throws SQLException;

    public void deleteByBitstream(Context context, Bitstream bitstream) throws SQLException;

    public MostRecentChecksum findOldestRecord(Context context) throws SQLException;

    public MostRecentChecksum findOldestRecord(Context context, Date lessThanDate) throws SQLException;

    public List<MostRecentChecksum> findNotInHistory(Context context) throws SQLException;

    public void update(Context context, MostRecentChecksum mostRecentChecksum) throws SQLException;
}
