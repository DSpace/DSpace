/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;
import org.dspace.checker.factory.CheckerServiceFactory;
import org.dspace.checker.service.ChecksumHistoryService;
import org.dspace.checker.service.ChecksumResultService;
import org.dspace.checker.service.MostRecentChecksumService;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;
import org.dspace.storage.bitstore.factory.StorageServiceFactory;
import org.dspace.storage.bitstore.service.BitstreamStorageService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

/**
 * <p>
 * Main class for the checksum checker tool, which calculates checksums for each
 * bitstream whose ID is in the most_recent_checksum table, and compares it
 * against the last calculated checksum for that bitstream.
 * </p>
 * 
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 * 
 * 
 * TODO the accessor methods are currently unused - are they useful?
 * TODO check for any existing resource problems
 */
public final class CheckerCommand
{
    /** Usual Log4J logger. */
    private static final Logger LOG = Logger.getLogger(CheckerCommand.class);

    private Context context;

    /** BitstreamInfoDAO dependency. */
    private MostRecentChecksumService checksumService = null;

    /**
     * Checksum history Data access object
     */
    private ChecksumHistoryService checksumHistoryService = null;
    private BitstreamStorageService bitstreamStorageService = null;
    private ChecksumResultService checksumResultService = null;

    /** start time for current process. */
    private Date processStartDate = null;

    /**
     * Dispatcher to be used for processing run.
     */
    private BitstreamDispatcher dispatcher = null;

    /**
     * Container/logger with details about each bitstream and checksum results.
     */
    private ChecksumResultsCollector collector = null;

    /** Report all processing */
    private boolean reportVerbose = false;

    /**
     * Default constructor uses DSpace plugin manager to construct dependencies.
     * @param context Context
     */
    public CheckerCommand(Context context)
    {
        checksumService = CheckerServiceFactory.getInstance().getMostRecentChecksumService();
        checksumHistoryService = CheckerServiceFactory.getInstance().getChecksumHistoryService();
        bitstreamStorageService = StorageServiceFactory.getInstance().getBitstreamStorageService();
        checksumResultService = CheckerServiceFactory.getInstance().getChecksumResultService();
        this.context = context;
    }

    /**
     * <p>
     * Uses the options set up on this checker to determine a mode of execution,
     * and then accepts bitstream ids from the dispatcher and checks their
     * bitstreams against the db records.
     * </p>
     * 
     * <p>
     * N.B. a valid BitstreamDispatcher must be provided using
     * setBitstreamDispatcher before calling this method
     * </p>
     * @throws SQLException if database error
     */
    public void process() throws SQLException {
        LOG.debug("Begin Checker Processing");

        if (dispatcher == null)
        {
            throw new IllegalStateException("No BitstreamDispatcher provided");
        }

        if (collector == null)
        {
            collector = new ResultsLogger(processStartDate);
        }

        // update missing bitstreams that were entered into the
        // bitstream table - this always done.
        checksumService.updateMissingBitstreams(context);

        Bitstream bitstream = dispatcher.next();

        while (bitstream != null)
        {
            LOG.debug("Processing bitstream id = " + bitstream.getID());
            MostRecentChecksum info = checkBitstream(bitstream);

            if (reportVerbose
                    || !ChecksumResultCode.CHECKSUM_MATCH.equals(info.getChecksumResult().getResultCode()))
            {
                collector.collect(context, info);
            }

            context.uncacheEntity(bitstream);
            bitstream = dispatcher.next();
        }
    }

    /**
     * Check a specified bitstream.
     * 
     * @param bitstream
     *            the bitstream
     * 
     * @return the information about the bitstream and its checksum data
     * @throws SQLException if database error
     */
    protected MostRecentChecksum checkBitstream(final Bitstream bitstream) throws SQLException {
        // get bitstream info from bitstream table
        MostRecentChecksum info = checksumService.findByBitstream(context, bitstream);

        // requested id was not found in bitstream
        // or most_recent_checksum table
        if (info == null)
        {
            // Note: this case should only occur if id is requested at
            // command line, since ref integrity checks should
            // prevent id from appearing in most_recent_checksum
            // but not bitstream table, or vice versa
            info = checksumService.getNonPersistedObject();
            processNullInfoBitstream(info);
        }
        else if (!info.isToBeProcessed())
        {
            // most_recent_checksum.to_be_processed is marked
            // 'false' for this bitstream id.
            // Do not do any db updates
            info.setChecksumResult(getChecksumResultByCode(ChecksumResultCode.BITSTREAM_NOT_PROCESSED));
        }
        else if (info.getBitstream().isDeleted())
        {
            // bitstream id is marked 'deleted' in bitstream table.
            processDeletedBitstream(info);
        }
        else
        {
            processBitstream(info);
        }

        return info;
    }

    /**
     * Compares two checksums.
     * 
     * @param checksumA
     *            the first checksum
     * @param checksumB
     *            the second checksum
     * 
     * @return a result code (constants defined in Util)
     * @throws SQLException if database error
     */
    protected ChecksumResult compareChecksums(String checksumA, String checksumB) throws SQLException {
        ChecksumResult result = getChecksumResultByCode(ChecksumResultCode.CHECKSUM_NO_MATCH);

        if ((checksumA == null) || (checksumB == null))
        {
            result = getChecksumResultByCode(ChecksumResultCode.CHECKSUM_PREV_NOT_FOUND);
        }
        else if (checksumA.equals(checksumB))
        {
            result = getChecksumResultByCode(ChecksumResultCode.CHECKSUM_MATCH);
        }

        return result;
    }

    /**
     * Process bitstream that was marked 'deleted' in bitstream table. A deleted
     * bitstream should only be checked once afterwards it should be marked
     * 'to_be_processed=false'. Note that to_be_processed must be manually
     * updated in db to allow for future processing.
     * 
     * @param info
     *            a deleted bitstream.
     * @throws SQLException if database error
     */
    protected void processDeletedBitstream(MostRecentChecksum info) throws SQLException {
        info.setProcessStartDate(new Date());
        info.setChecksumResult(getChecksumResultByCode(ChecksumResultCode.BITSTREAM_MARKED_DELETED));
        info.setProcessEndDate(new Date());
        info.setToBeProcessed(false);
        checksumService.update(context, info);
        checksumHistoryService.addHistory(context, info);
    }

    /**
     * Process bitstream whose ID was not found in most_recent_checksum or
     * bitstream table. No updates can be done. The missing bitstream is output
     * to the log file.
     * 
     * @param info
     *            A not found BitStreamInfo
     * TODO is this method required?
     * @throws SQLException if database error
     */
    protected void processNullInfoBitstream(MostRecentChecksum info) throws SQLException {
        info.setInfoFound(false);
        info.setProcessStartDate(new Date());
        info.setProcessEndDate(new Date());
        info.setChecksumResult(getChecksumResultByCode(ChecksumResultCode.BITSTREAM_INFO_NOT_FOUND));
    }

    /**
     * <p>
     * Process general case bitstream.
     * </p>
     * 
     * <p>
     * Note: bitstream will have timestamp indicating it was "checked", even if
     * actual checksumming never took place.
     * </p>
     * 
     * TODO Why does bitstream have a timestamp indicating it's checked if
     *       checksumming doesn't occur?
     * 
     * @param info
     *            BitstreamInfo to handle
     * @throws SQLException if database error
     */
    protected void processBitstream(MostRecentChecksum info) throws SQLException {
        info.setProcessStartDate(new Date());

        try
        {
            Map checksumMap = bitstreamStorageService.computeChecksum(context, info.getBitstream());
            if(MapUtils.isNotEmpty(checksumMap)) {
                info.setBitstreamFound(true);
                if(checksumMap.containsKey("checksum")) {
                    info.setCurrentChecksum(checksumMap.get("checksum").toString());
                }

                if(checksumMap.containsKey("checksum_algorithm")) {
                    info.setChecksumAlgorithm(checksumMap.get("checksum_algorithm").toString());
                }
            }

            // compare new checksum to previous checksum
            info.setChecksumResult(compareChecksums(info.getExpectedChecksum(), info.getCurrentChecksum()));
        }
        catch (IOException e)
        {
            // bitstream located, but file missing from asset store
            info.setChecksumResult(getChecksumResultByCode(ChecksumResultCode.BITSTREAM_NOT_FOUND));
            info.setToBeProcessed(false);
            LOG.error("Error retrieving bitstream ID " + info.getBitstream().getID()
                    + " from " + "asset store.", e);
        }
        catch (SQLException e)
        {
            // ??this code only executes if an SQL
            // exception occurs in *DSpace* code, probably
            // indicating a general db problem?
            info.setChecksumResult(getChecksumResultByCode(ChecksumResultCode.BITSTREAM_INFO_NOT_FOUND));
            LOG.error("Error retrieving metadata for bitstream ID "
                    + info.getBitstream().getID(), e);
        } finally
        {
            info.setProcessEndDate(new Date());

            // record new checksum and comparison result in db
            checksumService.update(context, info);
            checksumHistoryService.addHistory(context, info);
        }
    }

    protected ChecksumResult getChecksumResultByCode(ChecksumResultCode checksumResultCode) throws SQLException {
        return checksumResultService.findByCode(context, checksumResultCode);
    }

    /**
     * Get dispatcher being used by this run of the checker.
     * 
     * @return the dispatcher being used by this run.
     */
    public BitstreamDispatcher getDispatcher()
    {
        return dispatcher;
    }

    /**
     * Set the dispatcher to be used by this run of the checker.
     * 
     * @param dispatcher
     *            Dispatcher to use.
     */
    public void setDispatcher(BitstreamDispatcher dispatcher)
    {
        this.dispatcher = dispatcher;
    }

    /**
     * Get the collector that holds/logs the results for this process run.
     * 
     * @return The ChecksumResultsCollector being used.
     */
    public ChecksumResultsCollector getCollector()
    {
        return collector;
    }

    /**
     * Set the collector that holds/logs the results for this process run.
     * 
     * @param collector
     *            the collector to be used for this run
     */
    public void setCollector(ChecksumResultsCollector collector)
    {
        this.collector = collector;
    }

    /**
     * Get time at which checker process began.
     * 
     * @return start time
     */
    public Date getProcessStartDate()
    {
        return processStartDate == null ? null : new Date(processStartDate.getTime());
    }

    /**
     * Set time at which checker process began.
     * 
     * @param startDate
     *            start time
     */
    public void setProcessStartDate(Date startDate)
    {
        processStartDate = startDate == null ? null : new Date(startDate.getTime());
    }

    /**
     * Determine if any errors are reported
     * 
     * @return true if only errors reported
     */
    public boolean isReportVerbose()
    {
        return reportVerbose;
    }

    /**
     * Set report errors only
     * 
     * @param reportVerbose
     *            true to report only errors in the logs.
     */
    public void setReportVerbose(boolean reportVerbose)
    {
        this.reportVerbose = reportVerbose;
    }
}
