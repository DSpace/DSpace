package org.dspace.checker;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.List;

/**
 * @author Monika Mevenkamp
 */
public class TSVReportWriter extends ReportWriter {

    public TSVReportWriter(OutputStreamWriter writer, int vLevel) {
        super(writer, vLevel);
    }

    /**
     * prints report header to outputStreamWriter
     *
     * @param hdr report header
     * @throws IOException
     */
    @Override
    public void writeHeaderChecksumHistory(String hdr) throws IOException {
        header = hdr;
        outputStreamWriter.write("# " + hdr + "\n");
        outputStreamWriter.write("bitstream-id\t");
        outputStreamWriter.write("process-start-date\t");
        outputStreamWriter.write("process-end-date\t");
        outputStreamWriter.write("checksum-expected\t");
        outputStreamWriter.write("checksum-calculated\t");
        outputStreamWriter.write("result");
        outputStreamWriter.write("\n");
    }


    /**
     * prints report footer to outputStreamWriter
     *
     * @throws IOException
     */
    @Override
    public void writeHeaderBitstreamInfo(String hdr) throws IOException {
        header = hdr;
        outputStreamWriter.write("# " + hdr + "\n");
        outputStreamWriter.write("bitstream-id\t");
        outputStreamWriter.write("format-id\t");
        outputStreamWriter.write("deleted\t");
        outputStreamWriter.write("internal-id\t");
        outputStreamWriter.write("size\t");
        outputStreamWriter.write("checksum-algorithm\t");
        outputStreamWriter.write("checksum\t");
        outputStreamWriter.write("store-number\t");
        outputStreamWriter.write("source");
        outputStreamWriter.write("\n");
    }

    @Override
    public void writeFooter() throws IOException {

    }

    /**
     * prints the elements in history one by one to outputStreamWriter
     *
     * @param history ChecksumHistory objects to be written
     * @return history.size()
     * @throws IOException
     */
    @Override
    public int writeBodyChecksumHistory(List<ChecksumHistory> history) throws IOException {
        int n = history.size();
        if (n > 0) {
            Iterator<ChecksumHistory> iter;
            iter = history.iterator();
            while (iter.hasNext()) {
                ChecksumHistory historyInfo = iter.next();
                outputStreamWriter.write(String.valueOf(historyInfo.getBitstreamId()));
                outputStreamWriter.write("\t");
                outputStreamWriter.write(applyDateFormatDetailed(historyInfo.getProcessStartDate()));
                outputStreamWriter.write("\t");
                outputStreamWriter.write(applyDateFormatDetailed(historyInfo.getProcessEndDate()));
                outputStreamWriter.write("\t");
                outputStreamWriter.write(historyInfo.getChecksumExpected());
                outputStreamWriter.write("\t");
                outputStreamWriter.write(historyInfo.getChecksumCalculated());
                outputStreamWriter.write("\t");
                outputStreamWriter.write(historyInfo.getResultCode());
                outputStreamWriter.write("\n");
            }
        }
        return n;
    }

    /**
     * prints the elements in bitstreams one by one to outputStreamWriter
     *
     * @param bitstreams DSpaceBitstreamInfo objects to be written
     * @return bitstreams.size()
     * @throws IOException
     * @TODO include description with proper quoting
     */
    @Override
    public int writeBodyBitstreamInfo(List<DSpaceBitstreamInfo> bitstreams) throws IOException {
        int n = bitstreams.size();

        for (DSpaceBitstreamInfo bitstreamInfo : bitstreams) {
            outputStreamWriter.write(String.valueOf(bitstreamInfo.getBitstreamId()));
            outputStreamWriter.write("\t");
            outputStreamWriter.write(bitstreamInfo.getBitstreamFormatId());
            outputStreamWriter.write("\t");
            outputStreamWriter.write(String.valueOf(bitstreamInfo.getDeleted()));
            outputStreamWriter.write("\t");
            outputStreamWriter.write(bitstreamInfo.getInternalId());
            outputStreamWriter.write("\t");
            outputStreamWriter.write(String.valueOf(bitstreamInfo.getSize()));
            outputStreamWriter.write("\t");
            outputStreamWriter.write(bitstreamInfo.getChecksumAlgorithm());
            outputStreamWriter.write("\t");
            outputStreamWriter.write(bitstreamInfo.getStoredChecksum());
            outputStreamWriter.write("\t");
            outputStreamWriter.write(String.valueOf(bitstreamInfo.getStoreNumber()));
            outputStreamWriter.write("\t");
            outputStreamWriter.write(bitstreamInfo.getSource());
            outputStreamWriter.write("\t");
            outputStreamWriter.write("\n");
        }

        return n;
    }
}
