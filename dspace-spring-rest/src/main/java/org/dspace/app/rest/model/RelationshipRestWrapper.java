/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.RelationshipRestController;

public class RelationshipRestWrapper implements RestAddressableModel {

    @JsonIgnore
    private List<RelationshipRest> relationshipRestList;

    public List<RelationshipRest> getRelationshipRestList() {
        return relationshipRestList;
    }

    public void setRelationshipRestList(List<RelationshipRest> relationshipRestList) {
        this.relationshipRestList = relationshipRestList;
    }

    public String getCategory() {
        return "core";
    }

    public Class getController() {
        return RelationshipRestController.class;
    }

    public String getType() {
        return "relationship";
    }
}
