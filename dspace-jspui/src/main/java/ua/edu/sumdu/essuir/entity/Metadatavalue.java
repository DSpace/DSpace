package ua.edu.sumdu.essuir.entity;

import javax.persistence.*;

@Entity
@Table(name = "metadatavalue")
public class Metadatavalue {
    @Id
    @Column(name = "metadata_value_id")
    private Integer metadataValueId;

    @Column(name = "resource_id")
    private Integer resourceId;

    @Column(name = "metadata_field_id")
    private Integer metadataFieldId;

    @Column(name = "text_value")
    private String textValue;

    @Column(name = "text_lang")
    private String textLang;

    @Column(name = "place")
    private Integer place;

    @Column(name = "authority")
    private String authority;

    @Column(name = "confidence")
    private Integer confidence;

    @Column(name = "resource_type_id")
    private Integer resourceTypeId;

    public Integer getMetadataValueId() {
        return metadataValueId;
    }

    public Integer getResourceId() {
        return resourceId;
    }

    public Integer getMetadataFieldId() {
        return metadataFieldId;
    }

    public String getTextValue() {
        return textValue;
    }

    public String getTextLang() {
        return textLang;
    }

    public Integer getPlace() {
        return place;
    }

    public String getAuthority() {
        return authority;
    }

    public Integer getConfidence() {
        return confidence;
    }

    public Integer getResourceTypeId() {
        return resourceTypeId;
    }
}
