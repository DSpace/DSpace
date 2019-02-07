/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.converter.MetadataConverter;

/**
 * An embeddable representation of the Metadata to use in with DSpace REST
 * Resource
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class MetadataValueRest {

    String value;

    String language;

    String authority;

    int confidence;

    /**
     * The order of this metadata value with respect to others in the same DSO with the same key.
     *
     * In the REST representation, all values of the same key are given as a json array that expresses
     * their relative order, so there is no need to expose the exact numeric value publicly. The numeric
     * value is only used at this level to ensure the intended order is respected when converting to/from json.
     *
     * @see MetadataConverter#convert(List)
     * @see MetadataRest#put(String, MetadataValueRest...)
     */
    @JsonIgnore
    int place = -1;

    public MetadataValueRest() {
    }

    public MetadataValueRest(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
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

    public int getPlace() {
        return place;
    }

    public void setPlace(int place) {
        this.place = place;
    }
}
