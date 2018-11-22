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
