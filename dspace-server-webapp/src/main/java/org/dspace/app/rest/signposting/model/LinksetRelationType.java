/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * An enumeration that holds track of linkset relation types.
 */
public enum LinksetRelationType {

    ITEM("item"),
    CITE_AS("cite-as"),
    AUTHOR("author"),
    TYPE("type"),
    LICENSE("license"),
    COLLECTION("collection"),
    LINKSET("linkset"),
    DESCRIBES("describes"),
    DESCRIBED_BY("describedby");

    private final String name;

    LinksetRelationType(String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }
}
