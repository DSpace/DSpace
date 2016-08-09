/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import org.dspace.checker.dao.ChecksumHistoryDAO;
import org.dspace.checker.service.ChecksumHistoryService;
import org.dspace.checker.service.ChecksumResultService;
import org.dspace.checker.service.MostRecentChecksumService;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Service implementation for the ChecksumHistory object.
 * This class is responsible for all business logic calls for the ChecksumHistory object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class ChecksumHistoryServiceImpl implements ChecksumHistoryService {

    @Autowired(required = true)
    protected ChecksumHistoryDAO checksumHistoryDAO;

    @Autowired(required = true)
    protected MostRecentChecksumService mostRecentChecksumService;
    @Autowired(required = true)
    protected ChecksumResultService checksumResultService;

    protected ChecksumHistoryServiceImpl()
    {

    }

    @Override
    public void updateMissingBitstreams(Context context) throws SQLException {
//                "insert into checksum_history ( "
//               + "bitstream_id, process_start_date, "
//               + "process_end_date, checksum_expected, "
//               + "checksum_calculated, result ) "
//               + "select most_recent_checksum.bitstream_id, "
//               + "most_recent_checksum.last_process_start_date, "
//               + "most_recent_checksum.last_process_end_date, "
//               + "most_recent_checksum.expected_checksum, most_recent_checksum.expected_checksum, "
//               + "CASE WHEN bitstream.deleted = true THEN 'BITSTREAM_MARKED_DELETED' else 'CHECKSUM_MATCH' END "
//               + "from most_recent_checksum, bitstream where "
//               + "not exists( select 'x' from checksum_history where "
//               + "most_recent_checksum.bitstream_id = checksum_history.bitstream_id ) "
//               + "and most_recent_checksum.bitstream_id = bitstream.bitstream_id";
        List<MostRecentChecksum> mostRecentChecksums = mostRecentChecksumService.findNotInHistory(context);
        for (MostRecentChecksum mostRecentChecksum : mostRecentChecksums) {
            addHistory(context, mostRecentChecksum);
        }
    }

    @Override
    public void addHistory(Context context, MostRecentChecksum mostRecentChecksum) throws SQLException {
        ChecksumHistory checksumHistory = new ChecksumHistory();
        checksumHistory.setBitstream(mostRecentChecksum.getBitstream());
        checksumHistory.setProcessStartDate(mostRecentChecksum.getProcessStartDate());
        checksumHistory.setProcessEndDate(mostRecentChecksum.getProcessEndDate());
        checksumHistory.setChecksumExpected(mostRecentChecksum.getExpectedChecksum());
        checksumHistory.setChecksumCalculated(mostRecentChecksum.getCurrentChecksum());
        ChecksumResult checksumResult;
        if(mostRecentChecksum.getBitstream().isDeleted())
        {
            checksumResult = checksumResultService.findByCode(context, ChecksumResultCode.BITSTREAM_MARKED_DELETED);
        } else {
            checksumResult = checksumResultService.findByCode(context, ChecksumResultCode.CHECKSUM_MATCH);
        }

        checksumHistory.setResult(checksumResult);

        checksumHistoryDAO.create(context, checksumHistory);
        checksumHistoryDAO.save(context, checksumHistory);
    }

    /**
     * Delete the history records from the database.
     *
     * @param context Context
     * @param retentionDate
     *            any records older than this data are deleted.
     * @param checksumResultCode
     *            result code records must have for them to be deleted.
     * @return number of records deleted.
     * @throws SQLException if database error occurs.
     */
    @Override
    public int deleteByDateAndCode(Context context, Date retentionDate, ChecksumResultCode checksumResultCode) throws SQLException
    {
        return checksumHistoryDAO.deleteByDateAndCode(context, retentionDate, checksumResultCode);
    }

    @Override
    public void deleteByBitstream(Context context, Bitstream bitstream) throws SQLException {
        //Delete the most recent
        mostRecentChecksumService.deleteByBitstream(context, bitstream);
        //Delete the history as well
        checksumHistoryDAO.deleteByBitstream(context, bitstream);
    }

    @Override
    public int prune(Context context, Map<ChecksumResultCode, Long> interests) throws SQLException {
        long now = System.currentTimeMillis();
        int count = 0;
        for (Map.Entry<ChecksumResultCode, Long> interest : interests.entrySet())
        {
            count += deleteByDateAndCode(context, new Date(now - interest.getValue().longValue()),
                    interest.getKey());
        }
        return count;

    }

}
