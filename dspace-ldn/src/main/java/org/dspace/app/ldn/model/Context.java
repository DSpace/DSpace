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

public class Context extends Base {

    @JsonProperty("ietf:cite-as")
    private String ietfCiteAs;

    @JsonProperty("url")
    private Url url;

    @JsonProperty("IsSupplementedBy")
    private List<Context> isSupplementedBy;

    @JsonProperty("IsSupplementTo")
    private List<Context> isSupplementTo;

    public Context() {
        super();
    }

    public String getIetfCiteAs() {
        return ietfCiteAs;
    }

    public void setIetfCiteAs(String ietfCiteAs) {
        this.ietfCiteAs = ietfCiteAs;
    }

    public Url getUrl() {
        return url;
    }

    public void setUrl(Url url) {
        this.url = url;
    }

    public List<Context> getIsSupplementedBy() {
        return isSupplementedBy;
    }

    public void setIsSupplementedBy(List<Context> isSupplementedBy) {
        this.isSupplementedBy = isSupplementedBy;
    }

    public List<Context> getIsSupplementTo() {
        return isSupplementTo;
    }

    public void setIsSupplementTo(List<Context> isSupplementTo) {
        this.isSupplementTo = isSupplementTo;
    }

}
