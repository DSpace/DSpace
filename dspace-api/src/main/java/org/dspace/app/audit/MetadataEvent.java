/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.audit;

import java.util.Objects;

import org.dspace.content.MetadataValue;

/**
 * Represents an event related to the modification of a metadata value,
 * storing details for audit purposes in the Solr core.
 * This class is used to track changes such as addition, modification, or removal
 * of metadata values, including field, value, authority, confidence, and place.
 *
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 */
public class MetadataEvent {

    /**
     * Action constant representing the addition of a metadata value.
     */
    public static final String ADD = "ADD";
    /**
     * Action constant representing the modification of a metadata value.
     */
    public static final String MODIFY = "MODIFY";
    /**
     * Action constant representing the removal of a metadata value.
     */
    public static final String REMOVE = "REMOVE";

    private String metadataField;
    private String value;
    private String authority;
    private Integer confidence;
    private int place;
    private String action;

    public MetadataEvent() {
    }

    public MetadataEvent(MetadataValue metadataValue, String action) {
        this.metadataField = metadataValue.getMetadataField().toString('_');
        this.value = metadataValue.getValue();
        this.authority = metadataValue.getAuthority();
        this.confidence = metadataValue.getConfidence();
        this.place = metadataValue.getPlace();
        this.action = action;
    }

    public String getMetadataField() {
        return metadataField;
    }

    public void setMetadataField(String metadataField) {
        this.metadataField = metadataField;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public Integer getConfidence() {
        return confidence;
    }

    public void setConfidence(Integer confidence) {
        this.confidence = confidence;
    }

    public int getPlace() {
        return place;
    }

    public void setPlace(int place) {
        this.place = place;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MetadataEvent that = (MetadataEvent) o;
        return place == that.place && Objects.equals(metadataField, that.metadataField) &&
            Objects.equals(value, that.value) && Objects.equals(authority, that.authority) &&
            Objects.equals(confidence, that.confidence) && Objects.equals(action, that.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metadataField, value, authority, confidence, place, action);
    }
}
