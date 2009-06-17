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

import java.util.Date;

/**
 * <p>
 * Represents a history record for the bitstream.
 * </p>
 * 
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 * 
 */
public class ChecksumHistory
{

    /** Unique bitstream id. */
    private int bitstreamId;

    /** Date the process started. */
    private Date processStartDate;

    /** Date the process ended. */
    private Date processEndDate;

    /** The expected checksum. */
    private String checksumExpected;

    /** The checksum calculated. */
    private String checksumCalculated;

    /** The string result. */
    private String result;

    public ChecksumHistory()
    {
        ;
    }

    /**
     * Minimal Constructor.
     * 
     * @param bitstreamId
     *            bitstream id in the database
     */
    public ChecksumHistory(int bitstreamId)
    {
        this.bitstreamId = bitstreamId;
    }

    /**
     * * Full history info Constructor.
     * 
     * @param bitstrmId
     *            bitstream Id.
     * @param startDate
     *            process start date
     * @param endDate
     *            process end date
     * @param checksumExpted
     *            expected checksum
     * @param checksumCalc
     *            calculated checksum
     * @param inResult
     *            result information
     */
    public ChecksumHistory(int bitstrmId, Date startDate, Date endDate,
            String checksumExpted, String checksumCalc, String inResult)
    {
        this.bitstreamId = bitstrmId;
        this.processStartDate = startDate;
        this.processEndDate = endDate;
        this.checksumExpected = checksumExpted;
        this.checksumCalculated = checksumCalc;
        this.result = inResult;
    }

    /**
     * @return Returns the bitstreamId.
     */
    public int getBitstreamId()
    {
        return bitstreamId;
    }

    /**
     * @return Returns the checksumCalculated.
     */
    public String getChecksumCalculated()
    {
        return checksumCalculated;
    }

    /**
     * Set the checksum calculated.
     * 
     * @param checksumCalculated
     *            The checksumCalculated to set.
     */
    public void setChecksumCalculated(String checksumCalculated)
    {
        this.checksumCalculated = checksumCalculated;
    }

    /**
     * Get the extpected checksum.
     * 
     * @return Returns the checksumExpected.
     */
    public String getChecksumExpected()
    {
        return checksumExpected;
    }

    /**
     * Set the expected checksum.
     * 
     * @param checksumExpected
     *            The checksumExpected to set.
     */
    public void setChecksumExpected(String checksumExpected)
    {
        this.checksumExpected = checksumExpected;
    }

    /**
     * Get the process end date. This is the date and time the processing ended.
     * 
     * @return Returns the processEndDate.
     */
    public Date getProcessEndDate()
    {
        return processEndDate;
    }

    /**
     * Set the process end date. This is the date and time the processing ended.
     * 
     * @param processEndDate
     *            The processEndDate to set.
     */
    public void setProcessEndDate(Date processEndDate)
    {
        this.processEndDate = processEndDate;
    }

    /**
     * Get the process start date. This is the date and time the processing
     * started.
     * 
     * @return Returns the processStartDate.
     */
    public Date getProcessStartDate()
    {
        return processStartDate;
    }

    /**
     * Set the process start date. This is the date and time the processing
     * started.
     * 
     * @param processStartDate
     *            The processStartDate to set.
     */
    public void setProcessStartDate(Date processStartDate)
    {
        this.processStartDate = processStartDate;
    }

    /**
     * Return the processing result.
     * 
     * @return
     */
    public String getResult()
    {
        return result;
    }

    /**
     * Set the checksum processing result.
     * 
     * @param result
     *            The result to set.
     */
    public void setResult(String result)
    {
        this.result = result;
    }
}
