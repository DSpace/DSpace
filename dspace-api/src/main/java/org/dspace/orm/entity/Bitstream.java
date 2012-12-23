/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm.entity;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.dspace.core.Constants;
import org.springframework.beans.factory.annotation.Configurable;

@Entity
@Table(name = "bitstream")
@Configurable
public class Bitstream implements IDSpaceObject {
    private int id;
    private BitstreamFormat format;
    private String name;
    private Long size;
    private String checksum;
    private String checksumAlgorithm;
    private String description;
    private String userFormatDescription;
    private String source;
    private String internalId;
    private boolean deleted;
    private Integer storeNumber;
    private Integer sequenceId;
    private List<Bundle> bundles;

    @Id
    @Column(name = "bitstream_id")
    @GeneratedValue
    public int getID() {
        return id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bitstream_format_id", nullable = true)
    public BitstreamFormat getFormat() {
        return format;
    }

    @Column(name = "name", nullable = true)
    public String getName() {
        return name;
    }

    @Column(name = "size_bytes")
    public Long getSize() {
        return size;
    }

    @Column(name = "checksum", nullable = true)
    public String getChecksum() {
        return checksum;
    }

    @Column(name = "checksum_algorithm", nullable = true)
    public String getChecksumAlgorithm() {
        return checksumAlgorithm;
    }

    @Column(name = "description", nullable = true)
    public String getDescription() {
        return description;
    }

    @Column(name = "user_format_description", nullable = true)
    public String getUserFormatDescription() {
        return userFormatDescription;
    }

    @Column(name = "source")
    public String getSource() {
        return source;
    }

    @Column(name = "internal_id", nullable = true)
    public String getInternalId() {
        return internalId;
    }

    @Column(name = "deleted")
    public boolean isDeleted() {
        return deleted;
    }

    @Column(name = "store_number")
    public Integer getStoreNumber() {
        return storeNumber;
    }

    @Column(name = "sequence_id")
    public Integer getSequenceId() {
        return sequenceId;
    }

    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.ALL })
    @JoinTable(name = "bundle2bitstream", joinColumns = { @JoinColumn(name = "bitstream_id") }, inverseJoinColumns = { @JoinColumn(name = "bundle_id") })
    public List<Bundle> getBundles() {
        return bundles;
    }

    public void setBundles(List<Bundle> bundles) {
        this.bundles = bundles;
    }

    public void setID(int id) {
        this.id = id;
    }

    public void setFormat(BitstreamFormat format) {
        this.format = format;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public void setChecksumAlgorithm(String checksumAlgorithm) {
        this.checksumAlgorithm = checksumAlgorithm;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUserFormatDescription(String userFormatDescription) {
        this.userFormatDescription = userFormatDescription;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setInternalId(String internalId) {
        this.internalId = internalId;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void setStoreNumber(Integer storeNumber) {
        this.storeNumber = storeNumber;
    }

    public void setSequenceId(Integer sequenceId) {
        this.sequenceId = sequenceId;
    }

	@Override
	@Transient
	public int getType() {
		return Constants.BITSTREAM;
	}

}
