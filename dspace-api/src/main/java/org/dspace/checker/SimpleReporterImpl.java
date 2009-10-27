/*
 * Copyright (c) 2004-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.checker;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
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
    /** log4j logger. */
    private static Logger LOG = Logger.getLogger(SimpleReporterImpl.class);

    /** Utility date format which includes hours minutes and seconds. */
    private static final DateFormat DATE_FORMAT_MAX = DateFormat
            .getDateInstance(DateFormat.MEDIUM);

    /** Utility date format which only includes Month/day/year. */
    private static final DateFormat DATE_FORMAT_MIN = DateFormat
            .getDateInstance(DateFormat.SHORT);

    /** The reporter access object to be used. */
    private ReporterDAO reporter = null;

    private String msg(String key)
    {
        return I18nUtil.getMessage("org.dspace.checker.SimpleReporterImpl." + key);
    }

    /**
     * Main Constructor.
     * 
     * @param reporter
     *            reporter to select the information
     */
    public SimpleReporterImpl()
    {
        this.reporter = new ReporterDAO();
    }

    /**
     * Sends the Deleteted bitstream report to an administrator. for the
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
        List history = reporter.getBitstreamResultTypeReport(startDate,
                endDate, ChecksumCheckResults.BITSTREAM_MARKED_DELETED);

        osw.write("\n");
        osw.write(msg("deleted-bitstream-intro"));
        osw.write(DATE_FORMAT_MIN.format(startDate));
        osw.write(" ");
        osw.write(msg("date-range-to"));
        osw.write(" ");
        osw.write(DATE_FORMAT_MIN.format(endDate));
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
        List history = reporter.getBitstreamResultTypeReport(startDate,
                endDate, ChecksumCheckResults.CHECKSUM_NO_MATCH);

        osw.write("\n");
        osw.write(msg("checksum-did-not-match"));
        osw.write(" ");
        osw.write("\n");
        osw.write(DATE_FORMAT_MIN.format(startDate));
        osw.write(" ");
        osw.write(msg("date-range-to"));
        osw.write(" ");
        osw.write(DATE_FORMAT_MIN.format(endDate));
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
        List history = reporter.getBitstreamResultTypeReport(startDate,
                endDate, ChecksumCheckResults.BITSTREAM_NOT_FOUND);

        osw.write("\n");
        osw.write(msg("bitstream-not-found-report"));
        osw.write(DATE_FORMAT_MIN.format(startDate));
        osw.write(" ");
        osw.write(msg("date-range-to"));
        osw.write(" ");
        osw.write(DATE_FORMAT_MIN.format(endDate));
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
        List history = reporter.getNotProcessedBitstreamsReport(startDate,
                endDate);

        osw.write("\n");
        osw.write(msg("bitstream-will-no-longer-be-processed"));
        osw.write(" ");
        osw.write(DATE_FORMAT_MIN.format(startDate));
        osw.write(" ");
        osw.write(msg("date-range-to"));
        osw.write(" ");
        osw.write(DATE_FORMAT_MIN.format(endDate));
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
    public int getUncheckedBitstreamsReport(OutputStreamWriter osw)
            throws IOException
    {
        // get all the bitstreams marked deleted for today
        List bitstreams = reporter.getUnknownBitstreams();

        osw.write("\n");
        osw.write(msg("unchecked-bitstream-report"));
        osw.write(DATE_FORMAT_MIN.format(new Date()));
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
    private void printHistoryRecords(List history, OutputStreamWriter osw)
            throws IOException
    {
        Iterator iter = history.iterator();
        while (iter.hasNext())
        {
            ChecksumHistory historyInfo = (ChecksumHistory) iter.next();
            StringBuffer buf = new StringBuffer(1000);
            buf.append("------------------------------------------------ \n");
            buf.append(msg("bitstream-id")).append(" = ").append(
                    historyInfo.getBitstreamId()).append("\n");
            buf.append(msg("process-start-date")).append(" = ").append(
                    DATE_FORMAT_MAX.format(historyInfo.getProcessStartDate()))
                    .append("\n");
            buf.append(msg("process-end-date")).append(" = ").append(
                    DATE_FORMAT_MAX.format(historyInfo.getProcessEndDate()))
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
    private void printDSpaceInfoRecords(List bitstreams, OutputStreamWriter osw)
            throws IOException
    {
        Iterator iter = bitstreams.iterator();

        while (iter.hasNext())
        {
            DSpaceBitstreamInfo info = (DSpaceBitstreamInfo) iter.next();
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
}
