/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker.service;

import org.dspace.core.Context;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
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
public interface SimpleReporterService
{
    /**
     * Returns the bitstreams set found to be deleted for the specified date
     * range.
     * 
     * @param context context
     * @param startDate
     *            the start date range
     * @param endDate
     *            the end date range
     * @param osw
     *            the output stream writer to write to
     * 
     * @return number of bitstreams found
     * 
     * @throws IOException if IO error
     *             if io error occurs
     * @throws SQLException if database error
     */
    public int getDeletedBitstreamReport(Context context, Date startDate, Date endDate,
            OutputStreamWriter osw) throws IOException, SQLException;

    /**
     * The a report of bitstreams found where the checksum has been changed
     * since the last check for the specified date range.
     * 
     * @param context context
     * @param startDate
     *            the start date range.
     * @param endDate
     *            then end date range.
     * @param osw
     *            the output stream writer to write to
     * 
     * @return number of bitstreams found
     * 
     * @throws IOException if IO error
     *             if io error occurs
     * @throws SQLException if database error
     */
    public int getChangedChecksumReport(Context context, Date startDate, Date endDate,
            OutputStreamWriter osw) throws IOException, SQLException;

    /**
     * The report of bitstreams for the specified date range where it was
     * determined the bitstreams can no longer be found.
     * 
     * @param context context
     * @param startDate
     *            the start date range.
     * @param endDate
     *            the end date range.
     * @param osw
     *            the output stream writer to write to
     * 
     * @return number of bitstreams found
     * 
     * @throws IOException if IO error
     *             if io error occurs
     * @throws SQLException if database error
     */
    public int getBitstreamNotFoundReport(Context context, Date startDate, Date endDate,
            OutputStreamWriter osw) throws IOException, SQLException;

    /**
     * The bitstreams that were set to not be processed report for the specified
     * date range.
     * 
     * @param context context
     * @param startDate
     *            the start date range.
     * @param endDate
     *            the end date range.
     * @param osw
     *            the output stream writer to write to
     * @return number of bitstreams found
     * 
     * @throws IOException if IO error
     *             if io error occurs
     * @throws SQLException if database error
     * 
     */
    public int getNotToBeProcessedReport(Context context, Date startDate, Date endDate,
            OutputStreamWriter osw) throws IOException, SQLException;

    /**
     * The bitstreams that are not known to the checksum checker. This means
     * they are in the bitstream table but not in the most recent checksum table
     * 
     * @param context context
     * @param osw
     *            the output stream writer to write to
     * 
     * @return number of bitstreams found
     * 
     * @throws IOException if IO error
     *             if io error occurs
     * @throws SQLException if database error
     * 
     */
    public int getUncheckedBitstreamsReport(Context context, OutputStreamWriter osw)
            throws IOException, SQLException;
}
