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
public class Service extends Base {

    @JsonProperty("inbox")
    private String inbox;

    /**
     * 
     */
    public Service() {
        super();
    }

    /**
     * @return String
     */
    public String getInbox() {
        return inbox;
    }

    /**
     * @param inbox
     */
    public void setInbox(String inbox) {
        this.inbox = inbox;
    }

}