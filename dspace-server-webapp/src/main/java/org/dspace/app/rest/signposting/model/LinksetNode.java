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
 * DTO object represents a node of a link set.
 */
public class LinksetNode {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String link;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LinksetRelationType relation;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String type;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String anchor;

    public LinksetNode(String link, LinksetRelationType relation, String type, String anchor) {
        this(link, relation, anchor);
        this.type = type;
    }

    public LinksetNode(String link, LinksetRelationType relation, String anchor) {
        this.link = link;
        this.relation = relation;
        this.anchor = anchor;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public LinksetRelationType getRelation() {
        return relation;
    }

    public void setRelation(LinksetRelationType relation) {
        this.relation = relation;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAnchor() {
        return anchor;
    }

    public void setAnchor(String anchor) {
        this.anchor = anchor;
    }
}
