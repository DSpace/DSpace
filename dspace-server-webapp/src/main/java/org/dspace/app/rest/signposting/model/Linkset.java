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
    private List<LinksetRelation> author;
    @JsonProperty("cite-as")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<LinksetRelation> citeAs;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<LinksetRelation> item;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<LinksetRelation> collection;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<LinksetRelation> type;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<LinksetRelation> license;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<LinksetRelation> linkset;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<LinksetRelation> describes;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<LinksetRelation> describedby;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String anchor;

    public List<LinksetRelation> getAuthor() {
        if (this.author == null) {
            this.author = new ArrayList<>();
        }
        return author;
    }
    public void setAuthor(List<LinksetRelation> author) {
        this.author = author;
    }

    public List<LinksetRelation> getCiteAs() {
        if (this.citeAs == null) {
            this.citeAs = new ArrayList<>();
        }
        return citeAs;
    }
    public void setCiteAs(List<LinksetRelation> citeAs) {
        this.citeAs = citeAs;
    }

    public List<LinksetRelation> getItem() {
        if (this.item == null) {
            this.item = new ArrayList<>();
        }
        return item;
    }
    public void setItem(List<LinksetRelation> item) {
        this.item = item;
    }

    public List<LinksetRelation> getCollection() {
        if (this.collection == null) {
            this.collection = new ArrayList<>();
        }
        return collection;
    }
    public void setCollection(List<LinksetRelation> collection) {
        this.collection = collection;
    }

    public List<LinksetRelation> getType() {
        if (type == null) {
            type = new ArrayList<>();
        }
        return type;
    }
    public void setType(List<LinksetRelation> type) {
        this.type = type;
    }

    public List<LinksetRelation> getLicense() {
        if (license == null) {
            license = new ArrayList<>();
        }
        return license;
    }
    public void setLicense(List<LinksetRelation> license) {
        this.license = license;
    }

    public List<LinksetRelation> getLinkset() {
        if (linkset == null) {
            linkset = new ArrayList<>();
        }
        return linkset;
    }
    public void setLinkset(List<LinksetRelation> linkset) {
        this.linkset = linkset;
    }

    public List<LinksetRelation> getDescribes() {
        if (describes == null) {
            describes = new ArrayList<>();
        }
        return describes;
    }
    public void setDescribes(List<LinksetRelation> describes) {
        this.describes = describes;
    }

    public List<LinksetRelation> getDescribedby() {
        if (describedby == null) {
            describes = new ArrayList<>();
        }
        return describedby;
    }
    public void setDescribedby(List<LinksetRelation> describedby) {
        this.describedby = describedby;
    }

    public String getAnchor() {
        return anchor;
    }
    public void setAnchor(String anchor) {
        this.anchor = anchor;
    }
}
