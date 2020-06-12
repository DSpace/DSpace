/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.dspace.content.MetadataField;
import org.dspace.core.ReloadableEntity;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "cris_layout_fieldbitstream2metadata")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
public class CrisLayoutFieldBitstream implements ReloadableEntity<Integer> {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "cris_layout_fieldbitstream2metadata_fieldbitstream_id_seq")
    @SequenceGenerator(
        name = "cris_layout_fieldbitstream2metadata_fieldbitstream_id_seq",
        sequenceName = "cris_layout_fieldbitstream2metadata_fieldbitstream_id_seq",
        allocationSize = 1)
    @Column(name = "fieldbitstream_id", unique = true, nullable = false, insertable = true, updatable = false)
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "layout_field_id")
    private CrisLayoutField layoutField;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metadata_field_id")
    private MetadataField metadataField;
    @Column(name = "bundle")
    private String bundle;
    @Column(name = "metadata_value")
    private String metadataValue;

    @Override
    public Integer getID() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public CrisLayoutField getLayoutField() {
        return layoutField;
    }

    public void setLayoutField(CrisLayoutField layoutField) {
        this.layoutField = layoutField;
    }

    public MetadataField getMetadataField() {
        return metadataField;
    }

    public void setMetadataField(MetadataField metadataField) {
        this.metadataField = metadataField;
    }

    public String getBundle() {
        return bundle;
    }

    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

    public void setMetadataValue(String metadataValue) {
        this.metadataValue = metadataValue;
    }

    public String getMetadataValue() {
        return metadataValue;
    }

}
