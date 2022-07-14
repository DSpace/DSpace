/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;

/**
 * The MetadataValueWrapper REST Resource
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class MetadataValueWrapperRest extends BaseObjectRest<Integer> {

    public static final String NAME = "metadatavalue";
    public static final String CATEGORY = RestAddressableModel.CORE;

    @JsonIgnore
    private MetadataFieldRest field;

    private String value;

    private String language;

    private String authority;

    private int confidence;

    private int place = -1;

    public MetadataFieldRest getField() {
        return field;
    }

    public void setField(MetadataFieldRest field) {
        this.field = field;
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

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }
}
