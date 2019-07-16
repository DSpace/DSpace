/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.submit;

public class SelectableRelationship {
    private String relationshipType;
    private String filter;
    private String searchConfiguration;

    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getFilter() {
        return filter;
    }

    public void setSearchConfiguration(String searchConfiguration) {
        this.searchConfiguration = searchConfiguration;
    }

    public String getSearchConfiguration() {
        return searchConfiguration;
    }
}
