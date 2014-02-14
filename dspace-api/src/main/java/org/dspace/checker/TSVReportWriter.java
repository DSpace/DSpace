package org.dspace.checker;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.List;

/**
 * @author Monika Mevenkamp
 */
public class TSVReportWriter extends ReportWriter {

    public TSVReportWriter(OutputStreamWriter writer, int vLevel, Context ctxt) {
        super(writer, vLevel, ctxt);
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
        writeInstanceInfo();
        outputStreamWriter.write("bitstream-id\t");
        if (verbosityLevel > 0) {
            outputStreamWriter.write("process-start-date\t");
        }
        outputStreamWriter.write("process-end-date\t");
        outputStreamWriter.write("checksum-expected\t");
        outputStreamWriter.write("checksum-calculated\t");
        outputStreamWriter.write("result");
        if (verbosityLevel > 0) {
            outputStreamWriter.write("\t");
            outputStreamWriter.write("internal-id\t");
            outputStreamWriter.write("item\t");
            outputStreamWriter.write("collection\t");
            outputStreamWriter.write("community\t");
            outputStreamWriter.write("source");
        }
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
        writeInstanceInfo();
        outputStreamWriter.write("bitstream-id\t");
        outputStreamWriter.write("format-id\t");
        outputStreamWriter.write("deleted\t");
        outputStreamWriter.write("internal-id\t");
        outputStreamWriter.write("size\t");
        outputStreamWriter.write("checksum-algorithm\t");
        outputStreamWriter.write("checksum\t");
        outputStreamWriter.write("store-number\t");
        if (verbosityLevel > 0) {
            outputStreamWriter.write("item\t");
            outputStreamWriter.write("collection\t");
            outputStreamWriter.write("community\t");
        }
        outputStreamWriter.write("source");

        outputStreamWriter.write("\n");
    }

    private void writeInstanceInfo() throws IOException {
        outputStreamWriter.write("# dspace.name: " + ConfigurationManager.getProperty("dspace.name") + "\n");
        outputStreamWriter.write("# assetstore.dir: " + ConfigurationManager.getProperty("assetstore.dir") + "\n");
        outputStreamWriter.write("# db.url: " + ConfigurationManager.getProperty("db.url") + "\n");
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
            for (ChecksumHistory info : history) {
                outputStreamWriter.write(String.valueOf(info.getBitstreamId()));
                if (verbosityLevel > 0) {
                    outputStreamWriter.write("\t");
                    outputStreamWriter.write(applyDateFormatDetailed(info.getProcessStartDate()));
                }
                outputStreamWriter.write("\t");
                outputStreamWriter.write(applyDateFormatDetailed(info.getProcessEndDate()));
                outputStreamWriter.write("\t");
                outputStreamWriter.write(info.getChecksumExpected());
                outputStreamWriter.write("\t");
                outputStreamWriter.write(info.getChecksumCalculated());
                outputStreamWriter.write("\t");
                outputStreamWriter.write(info.getResultCode());
                if (verbosityLevel > 0) {
                    outputStreamWriter.write("\t");
                    outputStreamWriter.write(CheckerInfo.getInternalId(info.getBitstream(context)));
                    outputStreamWriter.write("\t");
                    outputStreamWriter.write(CheckerInfo.getHandle(info.getItem(context)));
                    outputStreamWriter.write("\t");
                    outputStreamWriter.write(CheckerInfo.getHandle(info.getCollection(context)));
                    outputStreamWriter.write("\t");
                    outputStreamWriter.write(CheckerInfo.getHandle(info.getCommunity(context)));
                    outputStreamWriter.write("\t");
                    outputStreamWriter.write(CheckerInfo.getSource(info.getBitstream(context)));
                }
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

        for (DSpaceBitstreamInfo info : bitstreams) {
            outputStreamWriter.write(String.valueOf(info.getBitstreamId()));
            outputStreamWriter.write("\t");
            outputStreamWriter.write(info.getBitstreamFormatId());
            outputStreamWriter.write("\t");
            outputStreamWriter.write(String.valueOf(info.getDeleted()));
            outputStreamWriter.write("\t");
            outputStreamWriter.write(info.getInternalId());
            outputStreamWriter.write("\t");
            outputStreamWriter.write(String.valueOf(info.getSize()));
            outputStreamWriter.write("\t");
            outputStreamWriter.write(info.getChecksumAlgorithm());
            outputStreamWriter.write("\t");
            outputStreamWriter.write(info.getStoredChecksum());
            outputStreamWriter.write("\t");
            outputStreamWriter.write(String.valueOf(info.getStoreNumber()));
            outputStreamWriter.write("\t");
            if (verbosityLevel > 0) {
                outputStreamWriter.write(CheckerInfo.getHandle(info.getItem(context)));
                outputStreamWriter.write("\t");
                outputStreamWriter.write(CheckerInfo.getHandle(info.getCollection(context)));
                outputStreamWriter.write("\t");
                outputStreamWriter.write(CheckerInfo.getHandle(info.getCommunity(context)));
                outputStreamWriter.write("\t");
            }
            outputStreamWriter.write(CheckerInfo.getSource(info.getBitstream(context)));
            outputStreamWriter.write("\n");
        }

        return n;
    }
}
