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

/**
 * Value Object that holds bitstream information that will be used for dspace
 * bitstream.
 * 
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 * 
 */
public final class DSpaceBitstreamInfo
{
    /** database bitstream id. */
    private int bitstreamId;

    /** format */
    private String bitstreamFormat;

    /** name given to the bitstream. */
    private String name;

    /** Stored size of the bitstream. */
    private int size;

    /** the check sum value stored in the database. */
    private String storedChecksum;

    /** checksum algorithm (usually MD5 for now). */
    private String checksumAlgorithm;

    /** Bitstream Format Description */
    private String userFormatDescription;

    /** source name of the file. */
    private String source;

    /** file location in the assetstore. */
    private String internalId;

    /** deleted flag. */
    private boolean deleted;

    /** store number. */
    private int storeNumber;

    /**
     * Blanked off no-op default constructor.
     */
    private DSpaceBitstreamInfo()
    {
        ;
    }

    /**
     * Minimal constructor.
     * 
     * @param bid
     *            Bitstream identifier
     */
    public DSpaceBitstreamInfo(int bid)
    {
        deleted = false;
        storeNumber = -1;
        size = -1;
        bitstreamFormat = null;
        userFormatDescription = null;
        internalId = null;
        source = null;
        checksumAlgorithm = null;
        storedChecksum = null;
        name = null;
        this.bitstreamId = bid;
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
     * @param desc
     *            Bitstream description
     */
    public DSpaceBitstreamInfo(boolean del, int storeNo, int sz,
            String bitstrmFmt, int bitstrmId, String usrFmtDesc,
            String intrnlId, String src, String chksumAlgorthm, String chksum,
            String nm, String desc)
    {
        this.deleted = del;
        this.storeNumber = storeNo;
        this.size = sz;
        this.bitstreamFormat = bitstrmFmt;
        this.bitstreamId = bitstrmId;
        this.userFormatDescription = usrFmtDesc;
        this.internalId = intrnlId;
        this.source = src;
        this.checksumAlgorithm = chksumAlgorthm;
        this.storedChecksum = chksum;
        this.name = nm;
    }

    /**
     * Get the deleted flag.
     * 
     * @return boolean
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
     * Get the store number.
     * 
     * @return int
     */
    public int getStoreNumber()
    {
        return storeNumber;
    }

    /**
     * Set the store number.
     * 
     * @param storeNumber
     *            the store number
     */
    public void setStoreNumber(int storeNumber)
    {
        this.storeNumber = storeNumber;
    }

    /**
     * Get the size.
     * 
     * @return int
     */
    public int getSize()
    {
        return size;
    }

    /**
     * Set the size.
     * 
     * @param size
     *            the bitstream size
     */
    public void setSize(int size)
    {
        this.size = size;
    }

    /**
     * Get the Bitstream Format id.
     * 
     * @return int
     */
    public String getBitstreamFormatId()
    {
        return bitstreamFormat;
    }

    /**
     * Set the Bitstream Format id.
     * 
     * @param bitstrmFmt
     *            id of the bitstream format
     */
    public void setBitstreamFormatId(String bitstrmFmt)
    {
        this.bitstreamFormat = bitstrmFmt;
    }

    /**
     * Get the Bitstream id.
     * 
     * @return int
     */
    public int getBitstreamId()
    {
        return bitstreamId;
    }

    /**
     * Get the user format description.
     * 
     * @return String
     */
    public String getUserFormatDescription()
    {
        return userFormatDescription;
    }

    /**
     * Set the user format description.
     * 
     * @param userFormatDescription
     *            the userFormatDescription
     */
    public void setUserFormatDescription(String userFormatDescription)
    {
        this.userFormatDescription = userFormatDescription;
    }

    /**
     * Get the Internal Id.
     * 
     * @return String
     */
    public String getInternalId()
    {
        return internalId;
    }

    /**
     * Set the Internal Id.
     * 
     * @param internalId
     *            the DSpace internal sequence id for the bitstream.
     */
    public void setInternalId(String internalId)
    {
        this.internalId = internalId;
    }

    /**
     * Get the source.
     * 
     * @return String
     */
    public String getSource()
    {
        return source;
    }

    /**
     * Set the source.
     * 
     * @param source
     *            The bitstream source.
     */
    public void setSource(String source)
    {
        this.source = source;
    }

    /**
     * Get the checksum algorithm.
     * 
     * @return String
     */
    public String getChecksumAlgorithm()
    {
        return checksumAlgorithm;
    }

    /**
     * Set the checksum algorithm.
     * 
     * @param checksumAlgorithm
     *            the algorithm used for checking this bitstream
     */
    public void setChecksumAlgorithm(String checksumAlgorithm)
    {
        this.checksumAlgorithm = checksumAlgorithm;
    }

    /**
     * Get the checksum.
     * 
     * @return String
     */
    public String getStoredChecksum()
    {
        return storedChecksum;
    }

    /**
     * Set the checksum.
     * 
     * @param checksum
     *            The last stored checksum for this bitstream.
     */
    public void setStoredChecksum(String checksum)
    {
        this.storedChecksum = checksum;
    }

    /**
     * Get the name of the bitstream.
     * 
     * @return String
     */
    public String getName()
    {
        return name;
    }

    /**
     * Set the name of the bitstream.
     * 
     * @param nm
     *            The name of this bitstream.
     */
    public void getName(String nm)
    {
        this.name = nm;
    }

    /**
     * The name of the bitstream.
     * 
     * @param name
     *            The name to set.
     */
    public void setName(String name)
    {
        this.name = name;
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

        if (!(o instanceof DSpaceBitstreamInfo))
        {
            return false;
        }

        DSpaceBitstreamInfo other = (DSpaceBitstreamInfo) o;

        return (this.bitstreamId == other.bitstreamId);
    }

    /**
     * HashCode method uses <code>bitstreamId</code> as hashing function.
     * 
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        return bitstreamId;
    }

    /**
     * Describes this BitstreamInfo.
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return new StringBuffer("DSpace Bitstream Information for id ").append(
                bitstreamId).toString();
    }
}
