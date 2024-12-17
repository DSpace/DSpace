/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dto;

import java.util.Comparator;
import java.util.Objects;

import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.Choices;

/**
 * This class acts as Data transfer object in which we can store data like in a regular MetadataValue object, but this
 * one isn't saved in the DB. This can freely be used to represent Metadata without it being saved in the database,
 * this will typically be used when transferring data
 *
 * @author kevinvandevelde at atmire.com
 */
public class MetadataValueDTO {

    private String schema;
    private String element;
    private String qualifier;
    private String language;
    private String value;
    private String authority;
    private int confidence = Choices.CF_UNSET;

    public MetadataValueDTO(MetadataValue metadataValue) {
        MetadataField metadataField = metadataValue.getMetadataField();
        MetadataSchema metadataSchema = metadataField.getMetadataSchema();
        schema = metadataSchema.getName();
        element = metadataField.getElement();
        qualifier = metadataField.getQualifier();
        language = metadataValue.getLanguage();
        value = metadataValue.getValue();
        authority = metadataValue.getAuthority();
        confidence = metadataValue.getConfidence();
    }

    public MetadataValueDTO() {
    }

    /**
     * Constructor for the MetadataValueDTO class
     * @param schema        The schema to be assigned to this MetadataValueDTO object
     * @param element       The element to be assigned to this MetadataValueDTO object
     * @param qualifier     The qualifier to be assigned to this MetadataValueDTO object
     * @param language      The language to be assigend to this MetadataValueDTO object
     * @param value         The value to be assigned to this MetadataValueDTO object
     * @param authority     The authority to be assigned to this MetadataValueDTO object
     * @param confidence    The confidence to be assigned to this MetadataValueDTO object
     */
    public MetadataValueDTO(String schema, String element, String qualifier, String language, String value,
                            String authority, int confidence) {
        this.schema = schema;
        this.element = element;
        this.qualifier = qualifier;
        this.language = language;
        this.value = value;
        this.authority = authority;
        this.confidence = confidence;
    }

    /**
     * Constructor for the MetadataValueDTO class
     * @param schema        The schema to be assigned to this MetadataValueDTO object
     * @param element       The element to be assigned to this MetadataValueDTO object
     * @param qualifier     The qualifier to be assigned to this MetadataValueDTO object
     * @param language      The language to be assigend to this MetadataValueDTO object
     * @param value         The value to be assigned to this MetadataValueDTO object
     */
    public MetadataValueDTO(String schema, String element, String qualifier, String language, String value) {
        this.schema = schema;
        this.element = element;
        this.qualifier = qualifier;
        this.language = language;
        this.value = value;
    }

    public String getSchema() {
        return schema;
    }

    public String getElement() {
        return element;
    }

    public String getQualifier() {
        return qualifier;
    }

    public String getValue() {
        return value;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public void setElement(String element) {
        this.element = element;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public int getConfidence() {
        return confidence;
    }

    public void setConfidence(int confidence) {
        this.confidence = confidence;
    }

    @Override
    public String toString() {
        return "MetadataValueDTO{" +
                "schema='" + schema + '\'' +
                ", element='" + element + '\'' +
                ", qualifier='" + qualifier + '\'' +
                ", language='" + language + '\'' +
                ", value='" + value + '\'' +
                ", authority='" + authority + '\'' +
                ", confidence=" + confidence +
                "}\n";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MetadataValueDTO that = (MetadataValueDTO) o;
        return confidence == that.confidence &&
                Objects.equals(schema, that.schema) && Objects.equals(element, that.element) &&
                Objects.equals(qualifier, that.qualifier) && Objects.equals(language, that.language) &&
                Objects.equals(value, that.value) && Objects.equals(authority, that.authority);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schema, element, qualifier, language, value, authority, confidence);
    }

    /**
     * Build a comparator to support proper sorting of MetadataValueDTO objects.
     * Order of sorting is based on how these things are normally sorted in human-readable formats, with
     * field name -> value -> lang/auth/etc being the usual order we use. In all these individual tests, nulls are
     * sorted first (eg. dc.title before dc.title.alternative)
     * @see org.dspace.external.model.ExternalDataObject#equals(Object)
     * 1. Qualifier
     * 2. Element
     * 3. Schema
     * 4. Value
     * 5. Language
     * 6. Authority
     * @return comparator
     */
    public static Comparator<MetadataValueDTO> comparator() {
        return Comparator.comparing(MetadataValueDTO::getQualifier, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(MetadataValueDTO::getElement, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(MetadataValueDTO::getSchema, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(MetadataValueDTO::getValue, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(MetadataValueDTO::getLanguage, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(MetadataValueDTO::getAuthority, Comparator.nullsFirst(Comparator.naturalOrder()));
    }


}
