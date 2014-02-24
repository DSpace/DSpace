/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
    private static final Logger log = Logger.getLogger(ReportWriter.class);

    private static  SimpleDateFormat detailedDateFormat =  new SimpleDateFormat("yyyy-mm-dd HH:mm:ss.SSS");

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

    public static String applyDateFormatDetailed(Date thisDate)
    {
        return detailedDateFormat.format(thisDate);
    }

    /**
     * header given in writeHeader; the default implementations repeats the header in writeFooter
     */
    protected String header;

    /**
     * destination for report output
     */
    protected OutputStreamWriter outputStreamWriter;

    /**
    * verbosity level
    */
    protected int verbosityLevel;

    /**
     * Context to be used for data rertieval
     */
    protected Context context;

    /**
     * string formatted bistreamId used as  prefix when writing to outputStreamWriter
     */
    private String bitstreamId;

    /**
     *  outputStreamWriter.write(bitstreamId + " : " + info + "\n")
     */
    private void writeBitstreamInfo(String info) throws IOException {
        outputStreamWriter.write(bitstreamId + " : " );
        outputStreamWriter.write(info + "\n");
    }

    /**
     * Main Constructor.
     * @param osw
     *      destination for report output; should not be null
     */
    public ReportWriter(OutputStreamWriter osw, int vLevel, Context ctxt)
    {
        assert( osw != null);
        outputStreamWriter = osw;
        verbosityLevel = vLevel;
        context = ctxt;
        if (log.isDebugEnabled())
            log.info(String.format("Create %s verbosityLevel=%d", this.getClass().getName(), verbosityLevel));
    }

    /**
     * prints report header to outputStreamWriter
     *
     * @param hdr    report header
     *
     * @throws IOException
     */
    public void writeHeaderChecksumHistory(String hdr) throws IOException {
        header = hdr;
        outputStreamWriter.write("---------------------------\n");
        outputStreamWriter.write(msg("start") + " "  + hdr + "\n");
        outputStreamWriter.write("\tdspace.name: " + ConfigurationManager.getProperty("dspace.name") + "\n");
        outputStreamWriter.write("\tassetstore.dir: " + ConfigurationManager.getProperty("assetstore.dir") + "\n");
        outputStreamWriter.write("\tdb.url: " + ConfigurationManager.getProperty("db.url") + "\n");
        outputStreamWriter.write("---------------------------\n");
    }

    /**
     * prints report header to outputStreamWriter
     *
     * @param hdr    report header
     *
     * @throws IOException
     */
    public void writeHeaderBitstreamInfo(String hdr) throws IOException {
        writeHeaderChecksumHistory(hdr);
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
        outputStreamWriter.flush();
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
                bitstreamId = String.valueOf(historyInfo.getBitstreamId());
                outputStreamWriter.write("------------------------------------------------ \n");
                writeBitstreamInfo(
                        String.format("%s = %s",
                                msg("bitstream-id"), bitstreamId));
                writeBitstreamInfo(
                        String.format("%s = %s",
                                msg("process-start-date"),
                                applyDateFormatLong(historyInfo.getProcessStartDate())));
                writeBitstreamInfo(
                        String.format("%s = %s",
                                msg("process-end-date"),
                                applyDateFormatLong(historyInfo.getProcessEndDate())));
                writeBitstreamInfo(
                        String.format("%s = %s",
                                msg("checksum-expected"),
                                historyInfo.getChecksumExpected()));
                writeBitstreamInfo(
                        String.format("%s = %s",
                                msg("checksum-calculated"),
                                historyInfo.getChecksumCalculated()));
                writeBitstreamInfo(
                        String.format("%s = %s", msg("result"),
                                historyInfo.getResultLong()));
                if (verbosityLevel > 0) {
                    writeBitstreamInfo(
                            String.format("%s = %s",
                                    msg("internal-id"), CheckerInfo.getInternalId(historyInfo.getBitstream(context))));
                    writeBitstreamInfo(
                            String.format("item-handle = %s", CheckerInfo.getHandle(historyInfo.getItem(context))));
                    writeBitstreamInfo(
                            String.format("collection-handle = %s", CheckerInfo.getHandle(historyInfo.getCollection(context))));
                    writeBitstreamInfo(
                            String.format("community-handle = %s", CheckerInfo.getHandle(historyInfo.getCommunity(context))));
                    writeBitstreamInfo(String.format("%s = %s",
                            msg("source"), CheckerInfo.getSource(historyInfo.getBitstream(context))));

                }
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
     *
     */
    public int writeBodyBitstreamInfo(List<DSpaceBitstreamInfo> bitstreams) throws IOException {
        int n = bitstreams.size();
        if (n == 0) {
            outputStreamWriter.write(msg("no-unchecked-bitstreams") + "\n");
        } else {
            outputStreamWriter.write(msg("howto-add-unchecked-bitstreams" + "\n"));
            outputStreamWriter.write("\n\n");

            for (DSpaceBitstreamInfo bitstreamInfo : bitstreams) {
                outputStreamWriter.write("----------------------------------------------- \n");
                writeBitstreamInfo(String.format("%s = %s",
                        msg("format-id"), bitstreamInfo.getBitstreamFormatId()));
                writeBitstreamInfo(String.format("%s = %s",
                        msg("deleted"), String.valueOf(bitstreamInfo.getDeleted())));
                writeBitstreamInfo(String.format("%s = %s", ("bitstream-id"), bitstreamId));
                writeBitstreamInfo(String.format("%s = %s",
                        msg("format-id"), bitstreamInfo.getBitstreamFormatId()));
                writeBitstreamInfo(String.format("%s = %s",
                        msg("format-id"), bitstreamInfo.getBitstreamFormatId()));
                writeBitstreamInfo(String.format("%s = %s",
                        msg("format-id"), bitstreamInfo.getBitstreamFormatId()));
                writeBitstreamInfo(String.format("%s = %s",
                        msg("checksum-algorithm"), bitstreamInfo.getChecksumAlgorithm()));
                writeBitstreamInfo(String.format("%s = %s",
                        msg("internal-id"), bitstreamInfo.getInternalId()));
                writeBitstreamInfo(String.format("%s = %s",
                        msg("name"), bitstreamInfo.getName()));
                writeBitstreamInfo(String.format("%s = %s",
                        msg("size"), bitstreamInfo.getSize()));
                writeBitstreamInfo(String.format("%s = %s",
                        msg("source"), CheckerInfo.getSource(bitstreamInfo.getBitstream(context))));
                writeBitstreamInfo(String.format("%s = %s",
                        msg("checksum"), bitstreamInfo.getStoredChecksum()));
                writeBitstreamInfo(String.format("%s = %s",
                        msg("store - number"), bitstreamInfo.getStoreNumber()));
                writeBitstreamInfo(String.format("%s = %s",
                        msg("description"), bitstreamInfo.getUserFormatDescription()));
                outputStreamWriter.write("----------------------------------------------- \n\n");
            }
        }
        return n;
    }

}
