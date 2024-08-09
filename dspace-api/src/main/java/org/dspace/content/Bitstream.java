/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.HibernateProxyHelper;

/**
 * Class representing bitstreams stored in the DSpace system.
 * <P>
 * When modifying the bitstream metadata, changes are not reflected in the
 * database until <code>update</code> is called. Note that you cannot alter
 * the contents of a bitstream; you need to create a new bitstream.
 *
 * @author Robert Tansley
 */
@Entity
@Table(name = "bitstream")
public class Bitstream extends DSpaceObject implements DSpaceObjectLegacySupport {
    @Transient
    BitstreamService bitreamService;

    @Column(name = "bitstream_id", insertable = false, updatable = false)
    private Integer legacyId;

    @Column(name = "sequence_id")
    private Integer sequenceId = -1;

    @Column(name = "checksum", length = 64)
    private String checksum;

    @Column(name = "checksum_algorithm", length = 32)
    private String checksumAlgorithm;

    @Column(name = "size_bytes")
    private long sizeBytes;

    @Column(name = "deleted")
    private boolean deleted = false;

    @Column(name = "internal_id", length = 256)
    private String internalId;

    @Column(name = "store_number")
    private int storeNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "bitstream_format_id")
    private BitstreamFormat bitstreamFormat;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "bitstreams")
    private List<Bundle> bundles = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "logo")
    private Community community;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "logo")
    private Collection collection;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.content.service.BitstreamService#create(Context, Bundle, InputStream)}
     * or
     * {@link org.dspace.content.service.BitstreamService#create(Context, InputStream)}
     */
    protected Bitstream() {
    }

    /**
     * Get the sequence ID of this bitstream. The sequence ID is a unique (within an Item) integer that references
     * this bitstream. It acts as a "persistent" identifier within the Item for this Bitstream (as Bitstream names
     * are not persistent). Because it is unique within an Item, sequence IDs are assigned by the ItemService.update()
     * method.
     *
     * @see org.dspace.content.ItemServiceImpl#update(Context, Item)
     * @return the sequence ID
     */
    public int getSequenceID() {
        if (sequenceId == null) {
            return -1;
        } else {
            return sequenceId;
        }
    }

    /**
     * Set the sequence ID of this bitstream. The sequence ID is a unique (within an Item) integer that references
     * this bitstream. While this method is public, it should only be used by ItemService.update() or other methods
     * which validate the uniqueness of the ID within the associated Item. This method itself does not validate
     * uniqueness of the ID, nor does the underlying database table.
     *
     * @see org.dspace.content.ItemServiceImpl#update(Context, Item)
     * @param sid the ID
     */
    public void setSequenceID(int sid) {
        sequenceId = sid;
        setMetadataModified();
        addDetails("SequenceID");
    }

    /**
     * Get the name of this bitstream - typically the filename, without any path
     * information
     *
     * @return the name of the bitstream
     */
    @Override
    public String getName() {
        return bitreamService.getName(this);
    }

    /**
     * Get the checksum of the content of the bitstream, for integrity checking
     *
     * @return the checksum
     */
    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    /**
     * Get the algorithm used to calculate the checksum
     *
     * @return the algorithm, e.g. "MD5"
     */
    public String getChecksumAlgorithm() {
        return checksumAlgorithm;
    }

    public void setChecksumAlgorithm(String checksumAlgorithm) {
        this.checksumAlgorithm = checksumAlgorithm;
    }

    /**
     * Get the size of the bitstream
     *
     * @return the size in bytes
     */
    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    protected BitstreamFormat getBitstreamFormat() {
        return bitstreamFormat;
    }

    void setFormat(BitstreamFormat bitstreamFormat) {
        this.bitstreamFormat = bitstreamFormat;
        setModified();
    }

    /**
     * Bitstreams are only logically deleted (via a flag in the database).
     * This method allows us to verify is the bitstream is still valid
     *
     * @return true if the bitstream has been deleted
     * @throws SQLException if database error
     */
    public boolean isDeleted() throws SQLException {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    /**
     * Get the bundles this bitstream appears in
     *
     * @return array of <code>Bundle</code> s this bitstream appears in
     * @throws SQLException if database error
     */
    public List<Bundle> getBundles() throws SQLException {
        return bundles;
    }

    void setBundles(List<Bundle> bundles) {
        this.bundles = bundles;
    }


    /**
     * return type found in Constants
     *
     * @return int Constants.BITSTREAM
     */
    @Override
    public int getType() {
        return Constants.BITSTREAM;
    }

    public Collection getCollection() {
        return collection;
    }

    public void setCollection(Collection collection) {
        this.collection = collection;
    }

    public Community getCommunity() {
        return community;
    }

    public void setCommunity(Community community) {
        this.community = community;
    }

    /**
     * Get the asset store number where this bitstream is stored
     *
     * @return the asset store number of the bitstream
     */
    public int getStoreNumber() {
        return storeNumber;
    }

    /**
     * Set the asset store number where this bitstream is stored
     *
     * @param storeNumber asset store number of the bitstream
     */
    public void setStoreNumber(int storeNumber) {
        this.storeNumber = storeNumber;
    }

    public String getInternalId() {
        return internalId;
    }

    public void setInternalId(String internalId) {
        this.internalId = internalId;
    }

    @Override
    public Integer getLegacyId() {
        return legacyId;
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same Bitstream
     * as this object, <code>false</code> otherwise
     *
     * @param other object to compare to
     * @return <code>true</code> if object passed in represents the same
     * collection as this object
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Bitstream)) {
            return false;
        }
        Class<?> objClass = HibernateProxyHelper.getClassWithoutInitializingProxy(other);
        if (this.getClass() != objClass) {
            return false;
        }
        final Bitstream otherBitstream = (Bitstream) other;
        return this.getID().equals(otherBitstream.getID());
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash += 73 * hash + getType();
        hash += 73 * hash + getID().hashCode();
        return hash;
    }

}
