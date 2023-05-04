/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO object represents a set of links.
 */
public class Linkset {

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Relation> author;
    @JsonProperty("cite-as")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Relation> citeAs;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Relation> item;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Relation> collection;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Relation> landingPage;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Relation> type;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Relation> license;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Relation> linkset;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String anchor;

    public List<Relation> getAuthor() {
        if (this.author == null) {
            this.author = new ArrayList<>();
        }
        return author;
    }
    public void setAuthor(List<Relation> author) {
        this.author = author;
    }

    public List<Relation> getCiteAs() {
        if (this.citeAs == null) {
            this.citeAs = new ArrayList<>();
        }
        return citeAs;
    }
    public void setCiteAs(List<Relation> citeAs) {
        this.citeAs = citeAs;
    }

    public List<Relation> getItem() {
        if (this.item == null) {
            this.item = new ArrayList<>();
        }
        return item;
    }
    public void setItem(List<Relation> item) {
        this.item = item;
    }

    public List<Relation> getCollection() {
        if (this.collection == null) {
            this.collection = new ArrayList<>();
        }
        return collection;
    }
    public void setCollection(List<Relation> collection) {
        this.collection = collection;
    }

    public List<Relation> getLandingPage() {
        if (landingPage == null) {
            landingPage = new ArrayList<>();
        }
        return landingPage;
    }
    public void setLandingPage(List<Relation> landingPage) {
        this.landingPage = landingPage;
    }

    public List<Relation> getType() {
        if (type == null) {
            type = new ArrayList<>();
        }
        return type;
    }
    public void setType(List<Relation> type) {
        this.type = type;
    }

    public List<Relation> getLicense() {
        if (license == null) {
            license = new ArrayList<>();
        }
        return license;
    }
    public void setLicense(List<Relation> license) {
        this.license = license;
    }

    public List<Relation> getLinkset() {
        if (linkset == null) {
            linkset = new ArrayList<>();
        }
        return linkset;
    }
    public void setLinkset(List<Relation> linkset) {
        this.linkset = linkset;
    }

    public String getAnchor() {
        return anchor;
    }
    public void setAnchor(String anchor) {
        this.anchor = anchor;
    }
}
