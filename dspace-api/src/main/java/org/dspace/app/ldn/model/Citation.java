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
public class Citation extends Base {

    @JsonProperty("ietf:cite-as")
    private String ietfCiteAs;

    @JsonProperty("ietf:item")
    private Url url;

    /**
     * 
     */
    public Citation() {
        super();
    }

    /**
     * @return String
     */
    public String getIetfCiteAs() {
        return ietfCiteAs;
    }

    /**
     * @param ietfCiteAs
     */
    public void setIetfCiteAs(String ietfCiteAs) {
        this.ietfCiteAs = ietfCiteAs;
    }

    /**
     * @return Url
     */
    public Url getUrl() {
        return url;
    }

    /**
     * @param url
     */
    public void setUrl(Url url) {
        this.url = url;
    }

}