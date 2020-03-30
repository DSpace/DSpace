/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.batch;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.CacheConcurrencyStrategy;

/***
 * Contains all the metadata associated to a bitstream that need to be created
 * or updated (optional)
 * 
 * @author fcadili (francesco.cadili at 4science.it)
 *
 */
@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
@Table(name = "imp_bitstream_metadatavalue")
public class ImpBitstreamMetadatavalue {
    @Id
    @Column(name = "imp_bitstream_metadatavalue_id")
    private Integer impBitstreamMetadatavalueId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "imp_bitstream_id", nullable = false)
    private ImpBitstream impBitstream;

    @Column(name = "imp_schema", length = 128, nullable = false)
    private String impSchema;

    @Column(name = "imp_element", length = 128, nullable = false)
    private String impElement;

    @Column(name = "imp_qualifier", length = 128)
    private String impQualifier;

    @Lob
    @Column(name = "imp_value")
    private String impValue;

    @Column(name = "imp_authority", length = 256)
    private String impAuthority;

    @Column(name = "imp_confidence")
    private Integer impConfidence;

    @Column(name = "metadata_order", nullable = false)
    private Integer metadataOrder;

    @Column(name = "text_lang", length = 32)
    private String textLang;

    public Integer getImpBitstreamMetadatavalueId() {
        return impBitstreamMetadatavalueId;
    }

    public void setImpBitstreamMetadatavalueId(Integer impBitstreamMetadatavalueId) {
        this.impBitstreamMetadatavalueId = impBitstreamMetadatavalueId;
    }

    public ImpBitstream getImpBitstream() {
        return impBitstream;
    }

    public void setImpBitstream(ImpBitstream impBitstream) {
        this.impBitstream = impBitstream;
    }

    public String getImpSchema() {
        return impSchema;
    }

    public void setImpSchema(String impSchema) {
        this.impSchema = impSchema;
    }

    public String getImpElement() {
        return impElement;
    }

    public void setImpElement(String impElement) {
        this.impElement = impElement;
    }

    public String getImpQualifier() {
        return impQualifier;
    }

    public void setImpQualifier(String impQualifier) {
        this.impQualifier = impQualifier;
    }

    public String getImpValue() {
        return impValue;
    }

    public void setImpValue(String impValue) {
        this.impValue = impValue;
    }

    public String getImpAuthority() {
        return impAuthority;
    }

    public void setImpAuthority(String impAuthority) {
        this.impAuthority = impAuthority;
    }

    public Integer getImpConfidence() {
        if (impConfidence == null) {
            return -1;
        } else {
            return impConfidence;
        }
    }

    public void setImpConfidence(Integer impConfidence) {
        this.impConfidence = impConfidence;
    }

    public Integer getMetadataOrder() {
        return metadataOrder;
    }

    public void setMetadataOrder(Integer metadata_Oder) {
        this.metadataOrder = metadata_Oder;
    }

    public String getTextLang() {
        return textLang;
    }

    public void setTextLang(String textLang) {
        this.textLang = textLang;
    }
}
