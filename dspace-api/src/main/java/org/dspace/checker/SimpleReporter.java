/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Simple Reporter implementation.
 *
 * @author Monika Mevenkamp
 */
public class SimpleReporter {
    private ReporterDAO reporter;
    private ReportWriter writer;

    /**
     * Main Constructor.
     */
    public SimpleReporter(ReportWriter rw) {
        reporter = new ReporterDAO();
        writer = rw;
    }

    /**
     * Generate deleted bitstream report for specified date range or iff ont dates are given lokk for all matching records
     *
     * @param startDate the start date for the range, or null
     * @param endDate   the end date for the range, or null
     * @throws IOException if io error occurs
     */
    public int deletedBitstreamReport(Date startDate, Date endDate)
            throws IOException {
        List<ChecksumHistory> history = null;
        if (startDate != null) {
            assert (endDate != null);
            history = reporter.getChecksumHistoryReportForDateRange(startDate, endDate, ChecksumCheckResults.BITSTREAM_MARKED_DELETED);
        } else {
            history = reporter.getChecksumHistoryReport(ChecksumCheckResults.BITSTREAM_MARKED_DELETED);
        }

        writer.writeHeader(ReportWriter.msg("deleted-bitstream-intro") + " " + ReportWriter.dateRange(startDate, endDate));
        writer.writeBodyChecksumHistory(history);
        writer.writeFooter();
        return history.size();
    }

    /**
     * Generate changed bitstream report for specified date range or iff ont dates are given lokk for all matching records
     *
     * @param startDate the start date for the range, or null
     * @param endDate   the end date for the range, or null
     * @throws IOException if io error occurs
     */
    public int changedChecksumReport(Date startDate, Date endDate) throws IOException {
        List<ChecksumHistory> history = null;

        if (startDate != null) {
            assert (endDate != null);
            history = reporter.getChecksumHistoryReportForDateRange(startDate,
                    endDate, ChecksumCheckResults.CHECKSUM_NO_MATCH);
        } else {
            history = reporter.getChecksumHistoryReport(ChecksumCheckResults.CHECKSUM_NO_MATCH);
        }

        writer.writeHeader(ReportWriter.msg("checksum-did-not-match") + " " + ReportWriter.dateRange(startDate, endDate));
        writer.writeBodyChecksumHistory(history);
        writer.writeFooter();
        return history.size();
    }

    /**
     * Generate not-found bitstream report for specified date range
     *
     * @param startDate the start date for the range, or null
     * @param endDate   the end date for the range, or null
     * @throws IOException if io error occurs
     */
    public int bitstreamNotFoundReport(Date startDate, Date endDate) throws IOException {
        List<ChecksumHistory> history = null;

        if (startDate != null) {
            assert (endDate != null);
            history = reporter.getChecksumHistoryReportForDateRange(startDate,
                    endDate, ChecksumCheckResults.BITSTREAM_NOT_FOUND);
        } else {
            history = reporter.getChecksumHistoryReport(ChecksumCheckResults.BITSTREAM_NOT_FOUND);
        }
        writer.writeHeader(ReportWriter.msg("bitstream-not-found-report") + " " + ReportWriter.dateRange(startDate, endDate));
        writer.writeBodyChecksumHistory(history);
        writer.writeFooter();
        return history.size();
    }

    /**
     * Send the bitstreams that were set to not be processed report for the
     * specified date range.
     *
     * @param startDate the start date for the range
     * @param endDate   the end date for the range
     * @throws IOException if io error occurs
     */
    public int notToBeProcessedReport(Date startDate, Date endDate)
            throws IOException {

        // get all the bitstreams marked deleted for today
        List<ChecksumHistory> history = reporter.getNotProcessedBitstreamsReport(startDate,
                endDate);
        writer.writeHeader(ReportWriter.msg("bitstream-will-no-longer-be-processed") + " " + ReportWriter.dateRange(startDate, endDate));
        writer.writeBodyChecksumHistory(history);
        writer.writeFooter();
        return history.size();
    }

    /**
     * report any bitstreams that are not checked by the checksum checker.
     *
     * @throws IOException if io error occurs
     */
    public int uncheckedBitstreamsReport()
            throws IOException {
        // get all the bitstreams marked deleted for today
        List<DSpaceBitstreamInfo> bitstreams = reporter.getUnknownBitstreams();
        writer.writeHeader(ReportWriter.msg("unchecked-bitstream-report") + " " +
                ReportWriter.applyDateFormatShort(new Date()));
        writer.writeBodyBitstreamInfo(bitstreams);
        writer.writeFooter();
        return bitstreams.size();
    }

}
