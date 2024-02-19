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
 * used to map @see org.dspace.app.ldn.model.Citation
 */
public class Url extends Base {

    @JsonProperty("mediaType")
    private String mediaType;

    /**
     * 
     */
    public Url() {
        super();
    }

    /**
     * @return String
     */
    public String getMediaType() {
        return mediaType;
    }

    /**
     * @param mediaType
     */
    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

}