/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO object represents a relation to specific resource.
 */
public class LinksetRelation {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String href;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String type;

    public LinksetRelation(String href, String type) {
        this.href = href;
        this.type = type;
    }

    public String getHref() {
        return href;
    }

    public String getType() {
        return type;
    }
}
