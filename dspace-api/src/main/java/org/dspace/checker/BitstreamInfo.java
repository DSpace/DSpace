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
 * Value Object that holds bitstream information that will be used for checksum
 * processing.
 * </p>
 * 
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 * 
 */
public final class BitstreamInfo
{
    /** deleted flag. */
    private boolean deleted;

    /** dspace bitstream information */
    private DSpaceBitstreamInfo dspaceBitstream;

    /**
     * Indicates the bitstream info (metadata) was found in the database.
     * 
     * @todo Is this actually used for anything?
     */
    private boolean infoFound;

    /** indicates the bitstream was found in the database. */
    private boolean bitstreamFound;

    /** the check sum value calculated by the algorithm. */
    private String calculatedChecksum;

    /** should be processed or not? */
    private boolean toBeProcessed;

    /** checksum comparison result code. */
    private String checksumCheckResult;

    /** Date the object was created for processing. */
    private Date processStartDate;

    /** Date the processing was completed. */
    private Date processEndDate;

    /**
     * Blanked off no-op default constructor.
     */
    private BitstreamInfo()
    {
    }

    /**
     * Minimal constructor.
     *
     * @param bid
     *            Bitstream identifier
     */
    public BitstreamInfo(int bid)
    {
        deleted = false;

        dspaceBitstream = new DSpaceBitstreamInfo(bid);

        // set default to true since it's the
        // case for most bitstreams
        infoFound = true;
        bitstreamFound = false;
        calculatedChecksum = null;
        processEndDate = null;
        toBeProcessed = false;
        processStartDate = new Date();
    }

    /**
     * Complete constructor.
     *
     * @param del
     *            Deleted
     * @param storeNo
     *            Bitstream storeNumber
     * @param sz
     *            Bitstream size
     * @param bitstrmFmt
     *            Bitstream format
     * @param bitstrmId
     *            Bitstream id
     * @param usrFmtDesc
     *            Bitstream format description
     * @param intrnlId
     *            Bitstream DSpace internal id
     * @param src
     *            Bitstream source
     * @param chksumAlgorthm
     *            Algorithm used to check bitstream
     * @param chksum
     *            Hash digest obtained
     * @param nm
     *            Name of bitstream
     * @param procEndDate
     *            When the last bitstream check finished.
     * @param toBeProc
     *            Whether the bitstream will be checked or skipped
     * @param procStartDate
     *            When the last bitstream check started.
     */
    public BitstreamInfo(boolean del, int storeNo, long sz, String bitstrmFmt,
            int bitstrmId, String usrFmtDesc, String intrnlId, String src,
            String chksumAlgorthm, String chksum, String nm, Date procEndDate,
            boolean toBeProc, Date procStartDate)
    {
        dspaceBitstream = new DSpaceBitstreamInfo(del, storeNo, sz, bitstrmFmt,
                bitstrmId, usrFmtDesc, intrnlId, src, chksumAlgorthm, chksum,
                nm, "");

        this.deleted = del;
        this.processEndDate = (processEndDate == null ? null : new Date(procEndDate.getTime()));
        this.toBeProcessed = toBeProc;
        this.processStartDate = (processStartDate == null ? null : new Date(procStartDate.getTime()));
        this.infoFound = true;
    }

    /**
     * Get the deleted flag.
     *
     * @return true if the bitstream has been deleted
     */
    public boolean getDeleted()
    {
        return deleted;
    }

    /**
     * Set the deleted flag.
     *
     * @param deleted
     *            deleted flag
     */
    public void setDeleted(boolean deleted)
    {
        this.deleted = deleted;
    }

    /**
     * Get the bitstream store number.
     *
     * @return int
     */
    public int getStoreNumber()
    {
        return dspaceBitstream.getStoreNumber();
    }

    /**
     * Set the store number.
     * 
     * @param storeNumber
     *            the store number
     */
    public void setStoreNumber(int storeNumber)
    {
        dspaceBitstream.setStoreNumber(storeNumber);
    }

    /**
     * Get the size.
     *
     * @return int
     */
    public long getSize()
    {
        return dspaceBitstream.getSize();
    }

    /**
     * Set the size.
     * 
     * @param size
     *            the bitstream size
     */
    public void setSize(int size)
    {
        dspaceBitstream.setSize(size);
    }

    /**
     * Get the Bitstream Format id.
     *
     * @return int
     */
    public String getBitstreamFormatId()
    {
        return dspaceBitstream.getBitstreamFormatId();
    }

    /**
     * Set the Bitstream Format id.
     * 
     * @param bitstrmFmt
     *            id of the bitstream format
     */
    public void setBitstreamFormatId(String bitstrmFmt)
    {
        dspaceBitstream.setBitstreamFormatId(bitstrmFmt);
    }

    /**
     * Get the Bitstream id.
     *
     * @return int
     */
    public int getBitstreamId()
    {
        return dspaceBitstream.getBitstreamId();
    }

    /**
     * Get the user format description.
     *
     * @return String
     */
    public String getUserFormatDescription()
    {
        return dspaceBitstream.getUserFormatDescription();
    }

    /**
     * Set the user format description.
     * 
     * @param userFormatDescription
     *            the userFormatDescription
     */
    public void setUserFormatDescription(String userFormatDescription)
    {
        dspaceBitstream.setUserFormatDescription(userFormatDescription);
    }

    /**
     * Get the Internal Id.
     *
     * @return String
     */
    public String getInternalId()
    {
        return dspaceBitstream.getInternalId();
    }

    /**
     * Set the Internal Id.
     * 
     * @param internalId
     *            the DSpace internal sequence id for the bitstream.
     */
    public void setInternalId(String internalId)
    {
        dspaceBitstream.setInternalId(internalId);
    }

    /**
     * Get the source.
     *
     * @return String
     */
    public String getSource()
    {
        return dspaceBitstream.getSource();
    }

    /**
     * Set the source.
     * 
     * @param source
     *            The bitstream source.
     */
    public void setSource(String source)
    {
        dspaceBitstream.setSource(source);
    }

    /**
     * Get the checksum algorithm.
     *
     * @return String
     */
    public String getChecksumAlgorithm()
    {
        return dspaceBitstream.getChecksumAlgorithm();
    }

    /**
     * Set the checksum algorithm.
     * 
     * @param checksumAlgorithm
     *            the algorithm used for checking this bitstream
     */
    public void setChecksumAlgorithm(String checksumAlgorithm)
    {
        dspaceBitstream.setChecksumAlgorithm(checksumAlgorithm);
    }

    /**
     * Get the checksum.
     *
     * @return String
     */
    public String getStoredChecksum()
    {
        return dspaceBitstream.getStoredChecksum();
    }

    /**
     * Set the checksum.
     * 
     * @param checksum
     *            The last stored checksum for this bitstream.
     */
    public void setStoredChecksum(String checksum)
    {
        dspaceBitstream.setStoredChecksum(checksum);
    }

    /**
     * Get the name of the bitstream.
     *
     * @return String
     */
    public String getName()
    {
        return dspaceBitstream.getName();
    }

    /**
     * Set the name of the bitstream.
     * 
     * @param nm
     *            The name of this bitstream.
     */
    public void setName(String nm)
    {
        dspaceBitstream.setName(nm);
    }

    /**
     * calculatedChecksum accessor.
     *
     * @return Returns the calculatedChecksum.
     */
    public String getCalculatedChecksum()
    {
        return calculatedChecksum;
    }

    /**
     * calculatedChecksum accessor.
     *
     * @param calculatedChecksum
     *            The calculatedChecksum to set.
     */
    public void setCalculatedChecksum(String calculatedChecksum)
    {
        this.calculatedChecksum = calculatedChecksum;
    }

    /**
     * infoFound accessor.
     * 
     * @return Returns infoFound.
     */
    public boolean getInfoFound()
    {
        return this.infoFound;
    }

    /**
     * infoFound accessor.
     * 
     * @param found
     *            sets infoFound.
     */
    public void setInfoFound(boolean found)
    {
        this.infoFound = found;
    }

    /**
     * bitstreamFound accessor.
     * 
     * @return Returns bitstreamFound.
     */
    public boolean getBitstreamFound()
    {
        return this.bitstreamFound;
    }

    /**
     * sets bitstreamFound.
     * 
     * @param found
     *            value of bitstreamFound to set.
     */
    public void setBitstreamFound(boolean found)
    {
        this.bitstreamFound = found;
    }

    /**
     * Identity entirely dependent upon <code>bitstreamId</code>.
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (!(o instanceof BitstreamInfo))
        {
            return false;
        }

        BitstreamInfo other = (BitstreamInfo) o;

        return (this.getBitstreamId() == other.getBitstreamId());
    }

    /**
     * HashCode method uses <code>bitstreamId</code> as hashing function.
     *
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        return getBitstreamId();
    }

    /**
     * Describes this BitstreamInfo.
     *
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return new StringBuffer("ChecksumInformation for id ").append(
                getBitstreamId()).toString();
    }

    /**
     * Sets toBeProcessed.
     *
     * @param toBeProcessed
     *            flag from most_recent_checksum table
     */
    public void setToBeProcessed(boolean toBeProcessed)
    {
        this.toBeProcessed = toBeProcessed;
    }

    /**
     * Gets toBeProcessed.
     *
     * @return value of toBeProcessed flag (from most_recent_checksum table)
     */
    public boolean getToBeProcessed()
    {
        return this.toBeProcessed;
    }

    /**
     * Gets checksumCheckResult.
     *
     * @return result code for comparison of previous and current checksums
     */
    public String getChecksumCheckResult()
    {
        return this.checksumCheckResult;
    }

    /**
     * Sets checksumCheckResult.
     *
     * @param resultCode
     *            for comparison of previous and current checksums
     */
    public void setChecksumCheckResult(String resultCode)
    {
        this.checksumCheckResult = resultCode;
    }

    /**
     * The start date and time this bitstream is being processed.
     *
     * @return date
     */
    public Date getProcessStartDate()
    {
        return this.processStartDate == null ? null : new Date(this.processStartDate.getTime());
    }

    /**
     * The date and time the processing started for this bitstream.
     *
     * @param startDate
     *            date to set.
     */
    public void setProcessStartDate(Date startDate)
    {
        this.processStartDate = startDate == null ? null : new Date(startDate.getTime());
    }

    /**
     * The date and time this bitstream is finished being processed.
     *
     * @return date
     */
    public Date getProcessEndDate()
    {
        return this.processEndDate == null ? null : new Date(this.processEndDate.getTime());
    }

    /**
     * The date and time this bitstream is finished being processed.
     *
     * @param endDate
     *            the date to set.
     */
    public void setProcessEndDate(Date endDate)
    {
        this.processEndDate = endDate == null ? null : new Date(endDate.getTime());
    }

    public String toLongString()
    {
        String me = this.getClass().getName() + "(";
        me = me + "BITSTREAM." + dspaceBitstream.getBitstreamId() + ", ";
        me = me + this.getChecksumCheckResult() + ", ";
        me = me + this.dspaceBitstream.getChecksumAlgorithm() + ", ";
        me = me + "exp=" + this.getStoredChecksum() + ", ";
        me = me + "calc=" + this.getCalculatedChecksum() ;
        me = me + ")";
        return me;
    }

}
