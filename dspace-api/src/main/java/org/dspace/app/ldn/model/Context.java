/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * used to map @see org.dspace.app.ldn.model.Notification
 */
public class Context extends Citation {

    @JsonProperty("IsSupplementedBy")
    private List<Context> isSupplementedBy;

    @JsonProperty("IsSupplementTo")
    private List<Context> isSupplementTo;

    /**
     * 
     */
    public Context() {
        super();
    }

    /**
     * @return List<Context>
     */
    public List<Context> getIsSupplementedBy() {
        return isSupplementedBy;
    }

    /**
     * @param isSupplementedBy
     */
    public void setIsSupplementedBy(List<Context> isSupplementedBy) {
        this.isSupplementedBy = isSupplementedBy;
    }

    /**
     * @return List<Context>
     */
    public List<Context> getIsSupplementTo() {
        return isSupplementTo;
    }

    /**
     * @param isSupplementTo
     */
    public void setIsSupplementTo(List<Context> isSupplementTo) {
        this.isSupplementTo = isSupplementTo;
    }

}
