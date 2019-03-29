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
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import org.dspace.checker.service.MostRecentChecksumService;
import org.dspace.checker.service.SimpleReporterService;
import org.dspace.content.Bitstream;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Simple Reporter implementation.
 * 
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 * 
 * TODO estimate string buffer sizes.
 */
public class SimpleReporterServiceImpl implements SimpleReporterService
{
    @Autowired(required = true)
    protected MostRecentChecksumService mostRecentChecksumService = null;
    @Autowired(required = true)
    protected BitstreamService bitstreamService = null;

    private String msg(String key)
    {
        return I18nUtil.getMessage("org.dspace.checker.SimpleReporterImpl." + key);
    }

    /**
     * Main Constructor.
     */
    protected SimpleReporterServiceImpl()
    {
    }

    /**
     * Sends the Deleted bitstream report to an administrator. for the
     * specified date range.
     * 
     * @param context context
     * @param startDate
     *            the start date for the range
     * @param endDate
     *            the end date for the range
     * @param osw
     *            the output stream writer to write to.
     * 
     * @return number of bitstreams found
     * 
     * @throws IOException if IO error
     *             if io error occurs
     * @throws SQLException if database error
     */
    @Override
    public int getDeletedBitstreamReport(Context context, Date startDate, Date endDate,
            OutputStreamWriter osw) throws IOException, SQLException {
        // get all the bitstreams marked deleted for today
        List<MostRecentChecksum> recentChecksums = mostRecentChecksumService.findBitstreamResultTypeReport(context, startDate,
                endDate, ChecksumResultCode.BITSTREAM_MARKED_DELETED);

        osw.write("\n");
        osw.write(msg("deleted-bitstream-intro"));
        osw.write(applyDateFormatShort(startDate));
        osw.write(" ");
        osw.write(msg("date-range-to"));
        osw.write(" ");
        osw.write(applyDateFormatShort(endDate));
        osw.write("\n\n\n");

        if (recentChecksums.size() == 0)
        {
            osw.write("\n\n");
            osw.write(msg("no-bitstreams-to-delete"));
            osw.write("\n");
        }
        else
        {
            printHistoryRecords(recentChecksums, osw);
        }

        return recentChecksums.size();
    }

    /**
     * Send the checksum changed report for the specified date range.
     * 
     * @param context context
     * @param startDate
     *            the start date for the range
     * @param endDate
     *            the end date for the range
     * @param osw
     *            the output stream writer to write to.
     * 
     * @return number of bitstreams found
     * 
     * @throws IOException if IO error
     *             if io error occurs
     * @throws SQLException if database error
     */
    @Override
    public int getChangedChecksumReport(Context context, Date startDate, Date endDate,
            OutputStreamWriter osw) throws IOException, SQLException {
        // get all the bitstreams marked deleted for today
        List<MostRecentChecksum> history = mostRecentChecksumService.findBitstreamResultTypeReport(context, startDate,
                endDate, ChecksumResultCode.CHECKSUM_NO_MATCH);

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
     * @param context context
     * @param startDate
     *            the start date for the range.
     * @param endDate
     *            the end date for the range.
     * @param osw
     *            the output stream writer to write to.
     * 
     * @return number of bitstreams found
     * 
     * @throws IOException if IO error
     *             if io error occurs
     * @throws SQLException if database error
     */
    @Override
    public int getBitstreamNotFoundReport(Context context, Date startDate, Date endDate,
            OutputStreamWriter osw) throws IOException, SQLException {
        // get all the bitstreams marked deleted for today
        List<MostRecentChecksum> history = mostRecentChecksumService.findBitstreamResultTypeReport(context, startDate,
                endDate, ChecksumResultCode.BITSTREAM_NOT_FOUND);

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
     * @param context context
     * @param startDate
     *            the start date for the range
     * @param endDate
     *            the end date for the range
     * @param osw
     *            the output stream writer to write to.
     * 
     * @return number of bitstreams found
     * 
     * @throws IOException if IO error
     *             if io error occurs
     * @throws SQLException if database error
     */
    @Override
    public int getNotToBeProcessedReport(Context context, Date startDate, Date endDate,
            OutputStreamWriter osw) throws IOException, SQLException {
        // get all the bitstreams marked deleted for today
        List<MostRecentChecksum> mostRecentChecksums = mostRecentChecksumService.findNotProcessedBitstreamsReport(context, startDate,
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

        if (mostRecentChecksums.size() == 0)
        {
            osw.write("\n\n");
            osw.write(msg("no-bitstreams-to-no-longer-be-processed"));
            osw.write("\n");
        }
        else
        {
            printHistoryRecords(mostRecentChecksums, osw);
        }

        return mostRecentChecksums.size();
    }

    /**
     * Get any bitstreams that are not checked by the checksum checker.
     * 
     * @param context context
     * @param osw
     *            the OutputStreamWriter to write to
     * 
     * @return the number of unchecked bitstreams
     * 
     * @throws IOException if IO error
     *             if io error occurs
     * @throws SQLException if database error
     */
    @Override
    public int getUncheckedBitstreamsReport(Context context, OutputStreamWriter osw)
            throws IOException, SQLException {
        // get all the bitstreams marked deleted for today
        List<Bitstream> bitstreams = bitstreamService.findBitstreamsWithNoRecentChecksum(context);

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
            this.printDSpaceInfoRecords(context, bitstreams, osw);
        }

        return bitstreams.size();
    }

    /**
     * Create a list of the found history records.
     * 
     * @param mostRecentChecksums
     *            the list of history records to be iterated over.
     * @param osw
     *            the output stream writer to write to.
     * 
     * @throws IOException if IO error
     *             if io error occurs
     */
    protected void printHistoryRecords(List<MostRecentChecksum> mostRecentChecksums, OutputStreamWriter osw)
            throws IOException
    {
        for (MostRecentChecksum mostRecentChecksum : mostRecentChecksums) {
            StringBuffer buf = new StringBuffer(1000);
            buf.append("------------------------------------------------ \n");
            buf.append(msg("bitstream-id")).append(" = ").append(
                    mostRecentChecksum.getBitstream().getID()).append("\n");
            buf.append(msg("process-start-date")).append(" = ").append(
                    applyDateFormatLong(mostRecentChecksum.getProcessStartDate()))
                    .append("\n");
            buf.append(msg("process-end-date")).append(" = ").append(
                    applyDateFormatLong(mostRecentChecksum.getProcessEndDate()))
                    .append("\n");
            buf.append(msg("checksum-expected")).append(" = ").append(
                    mostRecentChecksum.getExpectedChecksum()).append("\n");
            buf.append(msg("checksum-calculated")).append(" = ").append(
                    mostRecentChecksum.getCurrentChecksum()).append("\n");
            buf.append(msg("result")).append(" = ").append(
                    mostRecentChecksum.getChecksumResult().getResultCode()).append("\n");
            buf.append("----------------------------------------------- \n\n");
            osw.write(buf.toString());
        }
    }

    /**
     * Create a list of the found history records.
     * 
     * @param context context
     * @param bitstreams
     *            the list of history records to be iterated over.
     * @param osw
     *            the output stream to write to.
     * 
     * @throws IOException if IO error
     *             if io error occurs
     * @throws SQLException if database error
     */
    protected void printDSpaceInfoRecords(Context context, List<Bitstream> bitstreams, OutputStreamWriter osw)
            throws IOException, SQLException {

        for (Bitstream info : bitstreams)
        {
            StringBuffer buf = new StringBuffer(1000);
            buf.append("------------------------------------------------ \n");
            buf.append(msg("format-id")).append(" =  ").append(
                    info.getFormat(context).getID()).append("\n");
            buf.append(msg("deleted")).append(" = ").append(info.isDeleted())
                    .append("\n");
            buf.append(msg("bitstream-id")).append(" = ").append(
                    info.getID()).append("\n");
            buf.append(msg("checksum-algorithm")).append(" = ").append(
                    info.getChecksumAlgorithm()).append("\n");
            buf.append(msg("internal-id")).append(" = ").append(
                    info.getInternalId()).append("\n");
            buf.append(msg("name")).append(" = ").append(info.getName())
                    .append("\n");
            buf.append(msg("size")).append(" = ").append(info.getSizeBytes())
                    .append("\n");
            buf.append(msg("source")).append(" = ").append(info.getSource())
                    .append("\n");
            buf.append(msg("checksum")).append(" = ").append(
                    info.getChecksum()).append("\n");
            buf.append(msg("store-number")).append(" = ").append(
                    info.getStoreNumber()).append("\n");
            buf.append(msg("description")).append(" = ").append(
                    info.getUserFormatDescription()).append("\n");
            buf.append("----------------------------------------------- \n\n");
            osw.write(buf.toString());
        }
    }

    protected String applyDateFormatLong(Date thisDate)
    {
        return DateFormat.getDateInstance(DateFormat.MEDIUM).format(thisDate);
    }

    protected String applyDateFormatShort(Date thisDate)
    {
        return DateFormat.getDateInstance(DateFormat.SHORT).format(thisDate); 
    }
}
