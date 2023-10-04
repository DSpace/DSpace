/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.dspace.content.MetadataField;
import org.dspace.core.ReloadableEntity;
import org.hibernate.annotations.Type;

/**
 * Metadata related to a registration data {@link RegistrationData}
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
@Entity
@Table(name = "registrationdata_metadata")
public class RegistrationDataMetadata implements ReloadableEntity<Integer>, Comparable<RegistrationDataMetadata> {

    @Id
    @Column(name = "registrationdata_metadata_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "registrationdata_metadatavalue_seq")
    @SequenceGenerator(
        name = "registrationdata_metadatavalue_seq",
        sequenceName = "registrationdata_metadatavalue_seq",
        allocationSize = 1
    )
    private final Integer id;

    /**
     * {@link RegistrationData} linked to this metadata value
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrationdata_id")
    private RegistrationData registrationData = null;

    /**
     * The linked {@link MetadataField} instance
     */
    @ManyToOne
    @JoinColumn(name = "metadata_field_id")
    private MetadataField metadataField = null;

    /**
     * Value represented by this {@link RegistrationDataMetadata} instance
     * related to the metadataField {@link MetadataField}
     */
    @Lob
    @Type(type = "org.dspace.storage.rdbms.hibernate.DatabaseAwareLobType")
    @Column(name = "text_value")
    private String value = null;

    /**
     * Protected constructor
     */
    protected RegistrationDataMetadata() {
        id = 0;
    }


    @Override
    public Integer getID() {
        return id;
    }

    public MetadataField getMetadataField() {
        return metadataField;
    }

    void setMetadataField(MetadataField metadataField) {
        this.metadataField = metadataField;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public int compareTo(RegistrationDataMetadata o) {
        return Integer.compare(this.id, o.id);
    }

    void setRegistrationData(RegistrationData registrationData) {
        this.registrationData = registrationData;
    }

    public RegistrationData getRegistrationData() {
        return registrationData;
    }
}
