/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.dspace.core.Context;
import org.dspace.core.I18nUtil;

/**
 * 
 * Simple Reporter implementation.
 * 
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 * 
 * @todo estimate string buffer sizes.
 */
public class SimpleReporterImpl implements SimpleReporter
{
    /** The reporter access object to be used. */
    private ReporterDAO reporter = null;

    private String msg(String key)
    {
        return I18nUtil.getMessage("org.dspace.checker.SimpleReporterImpl." + key);
    }

    /**
     * Main Constructor.
     */
    public SimpleReporterImpl()
    {
        this.reporter = new ReporterDAO();
    }

    /**
     * Sends the Deleted bitstream report to an administrator. for the
     * specified date range.
     * 
     * @param startDate
     *            the start date for the range
     * @param endDate
     *            the end date for the range
     * @param osw
     *            the output stream writer to write to.
     * 
     * @return number of bitstreams found
     * 
     * @throws IOException
     *             if io error occurs
     */
    public int getDeletedBitstreamReport(Date startDate, Date endDate,
            OutputStreamWriter osw) throws IOException
    {
        // get all the bitstreams marked deleted for today
        List<ChecksumHistory> history = reporter.getBitstreamResultTypeReport(startDate,
                endDate, ChecksumCheckResults.BITSTREAM_MARKED_DELETED);

        osw.write("\n");
        osw.write(msg("deleted-bitstream-intro"));
        osw.write(applyDateFormatShort(startDate));
        osw.write(" ");
        osw.write(msg("date-range-to"));
        osw.write(" ");
        osw.write(applyDateFormatShort(endDate));
        osw.write("\n\n\n");

        if (history.size() == 0)
        {
            osw.write("\n\n");
            osw.write(msg("no-bitstreams-to-delete"));
            osw.write("\n");
        }
        else
        {
            printHistoryRecords(history, osw);
        }

        return history.size();
    }

    /**
     * Send the checksum changed report for the specified date range.
     * 
     * @param startDate
     *            the start date for the range
     * @param endDate
     *            the end date for the range
     * @param osw
     *            the output stream writer to write to.
     * 
     * @return number of bitstreams found
     * 
     * @throws IOException
     *             if io error occurs
     */
    public int getChangedChecksumReport(Date startDate, Date endDate,
            OutputStreamWriter osw) throws IOException
    {
        // get all the bitstreams marked deleted for today
        List<ChecksumHistory> history = reporter.getBitstreamResultTypeReport(startDate,
                endDate, ChecksumCheckResults.CHECKSUM_NO_MATCH);

        osw.write("\n");
        osw.write(msg("checksum-did-not-match"));
        osw.write(" ");
        osw.write("\n");
        osw.write(applyDateFormatShort(startDate));
        osw.write(" ");
        osw.write(msg("date-range-to"));
        osw.write(" ");
        osw.write(applyDateFormatShort(endDate));
        osw.write("\n\n\n");

        if (history.size() == 0)
        {
            osw.write("\n\n");
            osw.write(msg("no-changed-bitstreams"));
            osw.write("\n");
        }
        else
        {
            printHistoryRecords(history, osw);
        }

        return history.size();
    }

    /**
     * Send the bitstream not found report for the specified date range.
     * 
     * @param startDate
     *            the start date for the range.
     * @param endDate
     *            the end date for the range.
     * @param osw
     *            the output stream writer to write to.
     * 
     * @return number of bitstreams found
     * 
     * @throws IOException
     *             if io error occurs
     */
    public int getBitstreamNotFoundReport(Date startDate, Date endDate,
            OutputStreamWriter osw) throws IOException
    {
        // get all the bitstreams marked deleted for today
        List<ChecksumHistory> history = reporter.getBitstreamResultTypeReport(startDate,
                endDate, ChecksumCheckResults.BITSTREAM_NOT_FOUND);

        osw.write("\n");
        osw.write(msg("bitstream-not-found-report"));
        osw.write(applyDateFormatShort(startDate));
        osw.write(" ");
        osw.write(msg("date-range-to"));
        osw.write(" ");
        osw.write(applyDateFormatShort(endDate));
        osw.write("\n\n\n");

        if (history.size() == 0)
        {
            osw.write("\n\n");
            osw.write(msg("no-bitstreams-changed"));
            osw.write("\n");
        }
        else
        {
            printHistoryRecords(history, osw);
        }

        return history.size();
    }

    /**
     * Send the bitstreams that were set to not be processed report for the
     * specified date range.
     * 
     * @param startDate
     *            the start date for the range
     * @param endDate
     *            the end date for the range
     * @param osw
     *            the output stream writer to write to.
     * 
     * @return number of bitstreams found
     * 
     * @throws IOException
     *             if io error occurs
     */
    public int getNotToBeProcessedReport(Date startDate, Date endDate,
            OutputStreamWriter osw) throws IOException
    {
        // get all the bitstreams marked deleted for today
        List<ChecksumHistory> history = reporter.getNotProcessedBitstreamsReport(startDate,
                endDate);

        osw.write("\n");
        osw.write(msg("bitstream-will-no-longer-be-processed"));
        osw.write(" ");
        osw.write(applyDateFormatShort(startDate));
        osw.write(" ");
        osw.write(msg("date-range-to"));
        osw.write(" ");
        osw.write(applyDateFormatShort(endDate));
        osw.write("\n\n\n");

        if (history.size() == 0)
        {
            osw.write("\n\n");
            osw.write(msg("no-bitstreams-to-no-longer-be-processed"));
            osw.write("\n");
        }
        else
        {
            printHistoryRecords(history, osw);
        }

        return history.size();
    }

    /**
     * Get any bitstreams that are not checked by the checksum checker.
     * 
     * @param osw
     *            the OutputStreamWriter to write to
     * 
     * @return the number of unchecked bitstreams
     * 
     * @throws IOException
     *             if io error occurs
     */
    public int getUncheckedBitstreamsReport(Context context, OutputStreamWriter osw)
            throws IOException
    {
        // get all the bitstreams marked deleted for today
        List<DSpaceBitstreamInfo> bitstreams = reporter.getUnknownBitstreams(context);

        osw.write("\n");
        osw.write(msg("unchecked-bitstream-report"));
        osw.write(applyDateFormatShort(new Date()));
        osw.write("\n\n\n");

        if (bitstreams.size() == 0)
        {
            osw.write("\n\n");
            osw.write(msg("no-unchecked-bitstreams"));
            osw.write("\n");
        }
        else
        {
            osw.write(msg("howto-add-unchecked-bitstreams"));
            osw.write("\n\n\n");
            this.printDSpaceInfoRecords(bitstreams, osw);
        }

        return bitstreams.size();
    }

    /**
     * Create a list of the found history records.
     * 
     * @param history
     *            the list of history records to be iterated over.
     * @param osw
     *            the output stream writer to write to.
     * 
     * @throws IOException
     *             if io error occurs
     */
    private void printHistoryRecords(List<ChecksumHistory> history, OutputStreamWriter osw)
            throws IOException
    {
        Iterator<ChecksumHistory> iter = history.iterator();
        while (iter.hasNext())
        {
            ChecksumHistory historyInfo = iter.next();
            StringBuffer buf = new StringBuffer(1000);
            buf.append("------------------------------------------------ \n");
            buf.append(msg("bitstream-id")).append(" = ").append(
                    historyInfo.getBitstreamId()).append("\n");
            buf.append(msg("process-start-date")).append(" = ").append(
                    applyDateFormatLong(historyInfo.getProcessStartDate()))
                    .append("\n");
            buf.append(msg("process-end-date")).append(" = ").append(
                    applyDateFormatLong(historyInfo.getProcessEndDate()))
                    .append("\n");
            buf.append(msg("checksum-expected")).append(" = ").append(
                    historyInfo.getChecksumExpected()).append("\n");
            buf.append(msg("checksum-calculated")).append(" = ").append(
                    historyInfo.getChecksumCalculated()).append("\n");
            buf.append(msg("result")).append(" = ").append(
                    historyInfo.getResult()).append("\n");
            buf.append("----------------------------------------------- \n\n");
            osw.write(buf.toString());
        }
    }

    /**
     * Create a list of the found history records.
     * 
     * @param bitstreams
     *            the list of history records to be iterated over.
     * @param osw
     *            the output stream to write to.
     * 
     * @throws IOException
     *             if io error occurs
     */
    private void printDSpaceInfoRecords(List<DSpaceBitstreamInfo> bitstreams, OutputStreamWriter osw)
            throws IOException
    {
        Iterator<DSpaceBitstreamInfo> iter = bitstreams.iterator();

        while (iter.hasNext())
        {
            DSpaceBitstreamInfo info = iter.next();
            StringBuffer buf = new StringBuffer(1000);
            buf.append("------------------------------------------------ \n");
            buf.append(msg("format-id")).append(" =  ").append(
                    info.getBitstreamFormatId()).append("\n");
            buf.append(msg("deleted")).append(" = ").append(info.getDeleted())
                    .append("\n");
            buf.append(msg("bitstream-id")).append(" = ").append(
                    info.getBitstreamId()).append("\n");
            buf.append(msg("checksum-algorithm")).append(" = ").append(
                    info.getChecksumAlgorithm()).append("\n");
            buf.append(msg("internal-id")).append(" = ").append(
                    info.getInternalId()).append("\n");
            buf.append(msg("name")).append(" = ").append(info.getName())
                    .append("\n");
            buf.append(msg("size")).append(" = ").append(info.getSize())
                    .append("\n");
            buf.append(msg("source")).append(" = ").append(info.getSource())
                    .append("\n");
            buf.append(msg("checksum")).append(" = ").append(
                    info.getStoredChecksum()).append("\n");
            buf.append(msg("store-number")).append(" = ").append(
                    info.getStoreNumber()).append("\n");
            buf.append(msg("description")).append(" = ").append(
                    info.getUserFormatDescription()).append("\n");
            buf.append("----------------------------------------------- \n\n");
            osw.write(buf.toString());
        }
    }

    private String applyDateFormatLong(Date thisDate)
    {
        return DateFormat.getDateInstance(DateFormat.MEDIUM).format(thisDate);
    }

    private String applyDateFormatShort(Date thisDate)
    {
        return DateFormat.getDateInstance(DateFormat.SHORT).format(thisDate); 
    }
}
