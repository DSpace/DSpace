/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.batch;

import java.util.UUID;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;

/***
 * Contains all the information related to bitstreams to attach / replace in the
 * item (optional)
 * 
 * @author fcadili (francesco.cadili at 4science.it)
 */
@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
@Table(name = "imp_bitstream")
public class ImpBitstream {
    @Id
    @Column(name = "imp_bitstream_id")
    private Integer impBitstreamId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "imp_id", nullable = false)
    private ImpRecord impRecord;

//    @Column(name = "imp_id")
//    private Integer impId;

    @Transient
    public static final Integer ALL = 0;

    @Transient
    public static final Integer EMBARGO = 1;

    @Transient
    public static final Integer USE_GROUP = 2;

    @Transient
    public static final Integer NOT_VISIBLE = 3;

    @Column(name = "filepath", length = 512, nullable = false)
    private String filepath;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "bundle", length = 512)
    private String bundle;

    @Column(name = "bitstream_order")
    private Integer bitstreamOrder;

    @Column(name = "primary_bitstream")
    private Boolean primaryBitstream;

    @Column(name = "assetstore")
    private Integer assetstore;

    @Column(name = "name", length = 512)
    private String name;

    @Lob
    @Column(name = "imp_blob")
    @Type(type = "org.hibernate.type.BinaryType")
    private byte[] impBlob;

    @Column(name = "embargo_policy")
    private Integer embargoPolicy;

    @Column(name = "embargo_group")
    private UUID embargoGroup;

    @Column(name = "embargo_start_date")
    private String embargoStartDate;

    @Column(name = "md5value", length = 32)
    private String md5value;

    public Integer getImpBitstreamId() {
        return impBitstreamId;
    }

    public void setImpBitstreamId(Integer impBitstreamId) {
        this.impBitstreamId = impBitstreamId;
    }

    public ImpRecord getImpRecord() {
        return impRecord;
    }

    public void setImpRecord(ImpRecord impRecord) {
        this.impRecord = impRecord;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBundle() {
        return bundle;
    }

    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

    public Integer getBitstreamOrder() {
        if (bitstreamOrder == null) {
            return 0;
        } else {
            return bitstreamOrder;
        }
    }

    public void setBitstreamOrder(Integer bitstreamOrder) {
        this.bitstreamOrder = bitstreamOrder;
    }

    public Boolean getPrimaryBitstream() {
        if (primaryBitstream == null) {
            return false;
        } else {
            return primaryBitstream;
        }
    }

    public void setPrimaryBitstream(Boolean primaryBitstream) {
        this.primaryBitstream = primaryBitstream;
    }

    public Integer getAssetstore() {
        if (assetstore == null) {
            return -1;
        } else {
            return assetstore;
        }
    }

    public void setAssetstore(Integer assetstore) {
        this.assetstore = assetstore;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getImpBlob() {
        return impBlob;
    }

    public void setImpBlob(byte[] impBlob) {
        this.impBlob = impBlob;
    }

    public Integer getEmbargoPolicy() {
        if (embargoPolicy == null) {
            return -1;
        } else {
            return embargoPolicy;
        }
    }

    public void setEmbargoPolicy(Integer embargoPolicy) {
        this.embargoPolicy = embargoPolicy;
    }

    public UUID getEmbargoGroup() {
        return embargoGroup;
    }

    public void setEmbargoGroup(UUID embargoGroup) {
        this.embargoGroup = embargoGroup;
    }

    public String getEmbargoStartDate() {
        return embargoStartDate;
    }

    public void setEmbargoStartDate(String embargoStartDate) {
        this.embargoStartDate = embargoStartDate;
    }

    public String getMd5value() {
        return md5value;
    }

    public void setMd5value(String md5value) {
        this.md5value = md5value;
    }
}
