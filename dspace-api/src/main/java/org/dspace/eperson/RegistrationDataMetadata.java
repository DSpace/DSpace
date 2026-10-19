/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.dspace.content.MetadataField;
import org.dspace.core.ReloadableEntity;
import org.hibernate.Length;

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
    @Column(name = "text_value", length = Length.LONG32)
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
