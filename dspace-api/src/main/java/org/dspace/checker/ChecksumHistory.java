/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
        this.processStartDate = (startDate == null ? null : new Date(startDate.getTime()));
        this.processEndDate = (endDate == null ? null : new Date(endDate.getTime()));
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
        return processEndDate == null ? null : new Date(processEndDate.getTime());
    }

    /**
     * Set the process end date. This is the date and time the processing ended.
     * 
     * @param processEndDate
     *            The processEndDate to set.
     */
    public void setProcessEndDate(Date processEndDate)
    {
        this.processEndDate = (processEndDate == null ? null : new Date(processEndDate.getTime()));
    }

    /**
     * Get the process start date. This is the date and time the processing
     * started.
     * 
     * @return Returns the processStartDate.
     */
    public Date getProcessStartDate()
    {
        return processStartDate == null ? null : new Date(processStartDate.getTime());
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
        this.processStartDate = (processStartDate == null ? null : new Date(processStartDate.getTime()));
    }

    /**
     * Return the processing result.
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
