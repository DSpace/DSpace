/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import org.dspace.core.I18nUtil;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * ReportWriter implements default reporting output
 * This implementation is based on the previous SimpleReporterImpl.java/0d30c67d9ea472cc2c466ae7dc54c5f6834c5e81
 * @author Monika Mevenkamp
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 *
 */
public class ReportWriter
{

    public static String msg(String key)
    {
        return I18nUtil.getMessage("org.dspace.checker.ReportWriter." + key);
    }

    public static String dateRange(Date startDate, Date endDate) {
        if (startDate == null) {
            assert(endDate == null);
            return String.format("%s %s", msg("date-range-to"),
                    applyDateFormatShort(new Date()));
        }
        return String.format("%s %s %s", applyDateFormatShort(startDate),
                msg("date-range-to"),
                applyDateFormatShort(endDate));
    }

    public static String applyDateFormatLong(Date thisDate)
    {
        return DateFormat.getDateInstance(DateFormat.MEDIUM).format(thisDate);
    }

    public static  String applyDateFormatShort(Date thisDate)
    {
        return DateFormat.getDateInstance(DateFormat.SHORT).format(thisDate);
    }


    /**
     * header given in writeHeader; the default implementations repeats the header in writeFooter
     */
    private String header;

    /**
     * destination for report output
     */
    protected OutputStreamWriter outputStreamWriter;


    /**
     * Main Constructor.
     * @param osw
     *      destination for report output; should not be null
     */
    public ReportWriter(OutputStreamWriter osw)
    {
        assert( osw != null);
        outputStreamWriter = osw;
    }

    /**
     * prints report header to outputStreamWriter
     *
     * @param hdr    report header
     *
     * @throws IOException
     */
    public void writeHeader(String hdr) throws IOException {
        header = hdr;
        outputStreamWriter.write("---------------------------\n");
        outputStreamWriter.write(msg("start") + " "  + hdr + "\n");
        outputStreamWriter.write("---------------------------\n");
    }

    /**
     * prints report footer to outputStreamWriter
     * @throws IOException
     */
    public void writeFooter() throws IOException
    {
        outputStreamWriter.write("---------------------------\n");
        outputStreamWriter.write(msg("end") + " "  + header + "\n");
        outputStreamWriter.write("---------------------------\n");
    }

    /**
     * prints the elements in history one by one to outputStreamWriter
     * @param history ChecksumHistory objects to be written
     * @return  history.size()
     * @throws IOException
     */
    public int writeBodyChecksumHistory(List<ChecksumHistory> history) throws IOException {
        int n = history.size();
        if (n > 0) {
            Iterator<ChecksumHistory> iter;
            iter = history.iterator();
            while (iter.hasNext()) {
                ChecksumHistory historyInfo = iter.next();
                outputStreamWriter.write("------------------------------------------------ \n");
                outputStreamWriter.write(
                        String.format("%s = %s\n",
                                msg("bitstream-id"), historyInfo.getBitstreamId()));
                outputStreamWriter.write(
                        String.format("%s = %s\n",
                                msg("process-start-date"),
                                applyDateFormatLong(historyInfo.getProcessStartDate())));
                outputStreamWriter.write(
                        String.format("%s = %s\n",
                                msg("process-end-date"),
                                applyDateFormatLong(historyInfo.getProcessEndDate())));
                outputStreamWriter.write(
                        String.format("%s = %s\n",
                                msg("checksum-expected"),
                                historyInfo.getChecksumExpected()));
                outputStreamWriter.write(
                        String.format("%s = %s\n",
                                msg("checksum-calculated"),
                                historyInfo.getChecksumCalculated()));
                outputStreamWriter.write(
                        String.format("%s = %s\n", msg("result"),
                                historyInfo.getResult()));
                outputStreamWriter.write("------------------------------------------------ \n");
            }
        }
        return history.size();
    }

    /**
     * prints the elements in bitstreams one by one to outputStreamWriter
     * @param bitstreams DSpaceBitstreamInfo objects to be written
     * @return  bitstreams.size()
     * @throws IOException
     */
    public int writeBodyBitstreamInfo(List<DSpaceBitstreamInfo> bitstreams) throws IOException {
        int n = bitstreams.size();
        if (n == 0) {
            outputStreamWriter.write(msg("no-unchecked-bitstreams") + "\n");
        } else {
            outputStreamWriter.write(msg("howto-add-unchecked-bitstreams" + "\n"));
            outputStreamWriter.write("\n\n");

            for (DSpaceBitstreamInfo bitstreamInfo : bitstreams) {
                StringBuilder buf = new StringBuilder(1000);
                buf.append("------------------------------------------------ \n");
                buf.append(msg("format-id")).append(" =  ").append(
                        bitstreamInfo.getBitstreamFormatId()).append("\n");
                buf.append(msg("deleted")).append(" = ").append(bitstreamInfo.getDeleted())
                        .append("\n");
                buf.append(msg("bitstream-id")).append(" = ").append(
                        bitstreamInfo.getBitstreamId()).append("\n");
                buf.append(msg("checksum-algorithm")).append(" = ").append(
                        bitstreamInfo.getChecksumAlgorithm()).append("\n");
                buf.append(msg("internal-id")).append(" = ").append(
                        bitstreamInfo.getInternalId()).append("\n");
                buf.append(msg("name")).append(" = ").append(bitstreamInfo.getName())
                        .append("\n");
                buf.append(msg("size")).append(" = ").append(bitstreamInfo.getSize())
                        .append("\n");
                buf.append(msg("source")).append(" = ").append(bitstreamInfo.getSource())
                        .append("\n");
                buf.append(msg("checksum")).append(" = ").append(
                        bitstreamInfo.getStoredChecksum()).append("\n");
                buf.append(msg("store-number")).append(" = ").append(
                        bitstreamInfo.getStoreNumber()).append("\n");
                buf.append(msg("description")).append(" = ").append(
                        bitstreamInfo.getUserFormatDescription()).append("\n");
                buf.append("----------------------------------------------- \n\n");
                outputStreamWriter.write(buf.toString());
            }
        }
        return n;
    }

}
