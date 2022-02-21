/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Context extends Base {

    @JsonProperty("ietf:cite-as")
    private String ietfCiteAs;

    @JsonProperty("relationship")
    private String relationship;

    @JsonProperty("url")
    private Url url;

    public Context() {
        super();
    }

    public String getIetfCiteAs() {
        return ietfCiteAs;
    }

    public void setIetfCiteAs(String ietfCiteAs) {
        this.ietfCiteAs = ietfCiteAs;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public Url getUrl() {
        return url;
    }

    public void setUrl(Url url) {
        this.url = url;
    }

}
