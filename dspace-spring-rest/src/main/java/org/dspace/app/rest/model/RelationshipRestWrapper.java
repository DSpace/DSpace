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
import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RelationshipRestController;

/**
 * This is the RestWrapper object for the RelationshipRestResource class. This will contain all the data that is
 * used in that resource and more specifically, the label, dsoid and list of RelationshipRest objects
 * The other methods are generic getters and setters
 * This will be updated in https://jira.duraspace.org/browse/DS-4084
 */
public class RelationshipRestWrapper implements RestAddressableModel {
    public static final String NAME = "relationship";
    public static final String CATEGORY = RestAddressableModel.CORE;

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

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    public Class getController() {
        return RelationshipRestController.class;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
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
