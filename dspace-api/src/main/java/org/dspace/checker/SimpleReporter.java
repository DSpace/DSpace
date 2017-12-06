/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import org.dspace.core.Context;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;

/**
 * 
 * Simple Reporting Class which can return several different reports.
 * 
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 * 
 */
public interface SimpleReporter
{
    /**
     * Returns the bitstreams set found to be deleted for the specified date
     * range.
     * 
     * @param startDate
     *            the start date range
     * @param endDate
     *            the end date range
     * @param osw
     *            the output stream writer to write to
     * 
     * @return number of bitstreams found
     * 
     * @throws IOException
     *             if io error occurs
     */
    public int getDeletedBitstreamReport(Date startDate, Date endDate,
            OutputStreamWriter osw) throws IOException;

    /**
     * The a report of bitstreams found where the checksum has been changed
     * since the last check for the specified date range.
     * 
     * @param startDate
     *            the start date range.
     * @param endDate
     *            then end date range.
     * @param osw
     *            the output stream writer to write to
     * 
     * @return number of bitstreams found
     * 
     * @throws IOException
     *             if io error occurs
     */
    public int getChangedChecksumReport(Date startDate, Date endDate,
            OutputStreamWriter osw) throws IOException;

    /**
     * The report of bitstreams for the specified date range where it was
     * determined the bitstreams can no longer be found.
     * 
     * @param startDate
     *            the start date range.
     * @param endDate
     *            the end date range.
     * @param osw
     *            the output stream writer to write to
     * 
     * @return number of bitstreams found
     * 
     * @throws IOException
     *             if io error occurs
     */
    public int getBitstreamNotFoundReport(Date startDate, Date endDate,
            OutputStreamWriter osw) throws IOException;

    /**
     * The bitstreams that were set to not be processed report for the specified
     * date range.
     * 
     * @param startDate
     *            the start date range.
     * @param endDate
     *            the end date range.
     * @param osw
     *            the output stream writer to write to
     * 
     * @throws IOException
     *             if io error occurs
     * 
     */
    public int getNotToBeProcessedReport(Date startDate, Date endDate,
            OutputStreamWriter osw) throws IOException;

    /**
     * The bitstreams that are not known to the checksum checker. This means
     * they are in the bitstream table but not in the most recent checksum table
     * 
     * @param osw
     *            the output stream writer to write to
     * 
     * @return number of bitstreams found
     * 
     * @throws IOException
     *             if io error occurs
     * 
     */
    public int getUncheckedBitstreamsReport(Context context, OutputStreamWriter osw)
            throws IOException;
}
