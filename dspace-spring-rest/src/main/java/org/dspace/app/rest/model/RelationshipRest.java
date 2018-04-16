/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.RestResourceController;

public class RelationshipRest extends BaseObjectRest<Integer> {
    public static final String NAME = "relationship";
    public static final String CATEGORY = "core";

    private UUID leftId;
    private RelationshipTypeRest relationshipType;
    private UUID rightId;
    private int leftPlace;
    private int rightPlace;

    public String getType() {
        return NAME;
    }

    public String getCategory() {
        return CATEGORY;
    }

    public Class getController() {
        return RestResourceController.class;
    }

    public UUID getLeftId() {
        return leftId;
    }

    public void setLeftId(UUID leftId) {
        this.leftId = leftId;
    }

    @LinkRest(linkClass = RelationshipTypeRest.class)
    @JsonIgnore
    public RelationshipTypeRest getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(RelationshipTypeRest relationshipType) {
        this.relationshipType = relationshipType;
    }

    public UUID getRightId() {
        return rightId;
    }

    public void setRightId(UUID rightId) {
        this.rightId = rightId;
    }

    public int getLeftPlace() {
        return leftPlace;
    }

    public void setLeftPlace(int leftPlace) {
        this.leftPlace = leftPlace;
    }

    public int getRightPlace() {
        return rightPlace;
    }

    public void setRightPlace(int rightPlace) {
        this.rightPlace = rightPlace;
    }

}
