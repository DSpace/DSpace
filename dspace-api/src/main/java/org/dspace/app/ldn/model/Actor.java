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
public class Actor extends Base {

    @JsonProperty("name")
    private String name;

    /**
     * 
     */
    public Actor() {
        super();
    }

    /**
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

}