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
    public int getUncheckedBitstreamsReport(OutputStreamWriter osw)
            throws IOException;
}
