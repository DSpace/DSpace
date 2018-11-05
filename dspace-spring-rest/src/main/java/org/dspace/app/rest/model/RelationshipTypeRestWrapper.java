package org.dspace.app.rest.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.RelationshipTypeRestController;

public class RelationshipTypeRestWrapper implements RestAddressableModel {

    @JsonIgnore
    private List<RelationshipTypeRest> relationshipTypeRestList;

    public List<RelationshipTypeRest> getRelationshipTypeRestList() {
        return relationshipTypeRestList;
    }

    public void setRelationshipTypeRestList(
        List<RelationshipTypeRest> relationshipTypeRestList) {
        this.relationshipTypeRestList = relationshipTypeRestList;
    }

    public String getCategory() {
        return "core";
    }

    public Class getController() {
        return RelationshipTypeRestController.class;
    }

    public String getType() {
        return "relationshiptype";
    }
}
