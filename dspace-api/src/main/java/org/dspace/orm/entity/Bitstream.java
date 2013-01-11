/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orm.entity;

import java.io.InputStream;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.dspace.orm.entity.content.DSpaceObjectType;
import org.dspace.services.AuthorizationService;
import org.dspace.services.StorageService;
import org.dspace.services.auth.Action;
import org.dspace.services.auth.AuthorizationException;
import org.dspace.services.exceptions.StorageException;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Autowired;

@Entity
@Table(name = "bitstream")
@SequenceGenerator(name="bitstream_gen", sequenceName="bitstream_seq")
@Configurable
public class Bitstream extends DSpaceObject {

	/**
	 * This prefix string marks registered bitstreams in internal_id
	 */
	public static final String REGISTERED_FLAG = "-R";
	
	@Autowired StorageService storage;
	@Autowired AuthorizationService authorization;
	
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
    private List<Bundle> primaryBundles;
    private List<Community> communities;

	private List<Collection> collections;

    @Id
    @Column(name = "bitstream_id")
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="bitstream_gen")
    public int getID() {
        return id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bitstream_format_id", nullable = true)
    public BitstreamFormat getFormat() {
        return format;
    }
    
    @OneToMany(mappedBy = "primary")
    public List<Bundle> getPrimaryBundles () {
    	return primaryBundles;
    }
    
    public void setPrimaryBundles (List<Bundle> bundles) {
    	this.primaryBundles = bundles;
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

    @OneToMany(fetch=FetchType.LAZY, mappedBy="logo")
    public List<Community> getCommunities() {
		return communities;
	}

	public void setCommunities(List<Community> communities) {
		this.communities = communities;
	}

	@OneToMany(fetch=FetchType.LAZY, mappedBy="logo")
    public List<Collection> getCollections() {
		return collections;
	}

	public void setCollections(List<Collection> collections) {
		this.collections = collections;
	}

	@Override
	@Transient
	public DSpaceObjectType getType() {
		return DSpaceObjectType.BITSTREAM;
	}
	
	@Transient
	public InputStream retrieve () throws StorageException, AuthorizationException {
		authorization.authorized(this, Action.READ);
		return storage.retrieve(this);
	}

	/**
	 * The bitstream is a registered file
	 *
	 * @return true if the bitstream is a registered file
	 */
	@Transient
	public boolean isRegistered () {
		if (this.getInternalId().substring(0, REGISTERED_FLAG.length())
	            .equals(REGISTERED_FLAG)) 
	    {
	        return true;
	    }
	    return false;
	}

	@Override
	@Transient
	public IDSpaceObject getParentObject() {
		if (this.getBundles() == null || !this.getBundles().isEmpty()) return this.getBundles().get(0);
		else {
			if (this.getCommunities() == null || !this.getCommunities().isEmpty())
				return this.getCommunities().get(0);
			if (this.getCollections() == null || !this.getCollections().isEmpty())
				return this.getCollections().get(0);
		}
		return null;
	}
}
