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

/**
 * This is the RestWrapper object for the RelationshipRestResource class. This will contain all the data that is
 * used in that resource and more specifically, the label, dsoid and list of RelationshipRest objects
 * The other methods are generic getters and setters
 */
public class RelationshipRestWrapper implements RestAddressableModel {

    @JsonIgnore
    private List<RelationshipRest> relationshipRestList;

    private String label;
    private String dsoId;



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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDsoId() {
        return dsoId;
    }

    public void setDsoId(String dsoId) {
        this.dsoId = dsoId;
    }
}
