/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Object extends Base {

    @JsonProperty("ietf:cite-as")
    private String ietfCiteAs;

    @JsonProperty("sorg:name")
    private String title;

    @JsonProperty("url")
    private Url url;

    public Object() {
        super();
    }

    public String getIetfCiteAs() {
        return ietfCiteAs;
    }

    public void setIetfCiteAs(String ietfCiteAs) {
        this.ietfCiteAs = ietfCiteAs;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Url getUrl() {
        return url;
    }

    public void setUrl(Url url) {
        this.url = url;
    }

}
