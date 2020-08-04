/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.submit;

/**
 * The SelectableRelationship REST Resource. It is not addressable directly, only
 * used as inline object in the InputForm resource.
 *
 * SelectableRelationship encapsulates the configuration specific to the
 * submission of an entity relationship.
 * It contains a mandatory relationshipType, an optional filter for the lookup,
 * and a mandatory searchConfiguration for the lookup
 *
 * @author Raf Ponsaerts (raf.ponsaerts at atmire.com)
 * @author Ben Bosman (ben.bosman at atmire.com)
 */
public class SelectableRelationship {
    private String relationshipType;
    private String filter;
    private String searchConfiguration;
    private String nameVariants;

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

    public void setNameVariants(String nameVariants) {
        this.nameVariants = nameVariants;
    }

    public String getNameVariants() {
        return nameVariants;
    }
}
