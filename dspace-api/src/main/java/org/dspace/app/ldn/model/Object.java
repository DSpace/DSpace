/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * used to map @see org.dspace.app.ldn.model.Notification
 */
public class Object extends Citation {

    @JsonProperty("as:object")
    private String asObject;

    @JsonProperty("as:relationship")
    private String asRelationship;

    @JsonProperty("as:subject")
    private String asSubject;

    @JsonProperty("sorg:name")
    private String title;

    /**
     * 
     */
    public Object() {
        super();
    }

    /**
     * @return String
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    public String getAsObject() {
        return asObject;
    }

    public void setAsObject(String asObject) {
        this.asObject = asObject;
    }

    public String getAsRelationship() {
        return asRelationship;
    }

    public void setAsRelationship(String asRelationship) {
        this.asRelationship = asRelationship;
    }

    public String getAsSubject() {
        return asSubject;
    }

    public void setAsSubject(String asSubject) {
        this.asSubject = asSubject;
    }

}