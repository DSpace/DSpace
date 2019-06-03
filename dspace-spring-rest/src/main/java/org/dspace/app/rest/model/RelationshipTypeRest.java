/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.RestResourceController;

/**
 * This class is the REST representation of the RelationshipType model class.
 * This class acts as a data holder for the RelationshipTypeResource class
 * Refer to {@link org.dspace.content.RelationshipType} for an explanation of the properties
 */
public class RelationshipTypeRest extends BaseObjectRest<Integer> {

    public static final String NAME = "relationshiptype";
    public static final String CATEGORY = "core";

    private String leftLabel;
    private String rightLabel;
    private Integer leftMinCardinality;
    private Integer leftMaxCardinality;
    private Integer rightMinCardinality;
    private Integer rightMaxCardinality;
    private EntityTypeRest leftType;
    private EntityTypeRest rightType;

    public String getType() {
        return NAME;
    }

    public String getCategory() {
        return CATEGORY;
    }

    public Class getController() {
        return RestResourceController.class;
    }

    public String getLeftLabel() {
        return leftLabel;
    }

    public void setLeftLabel(String leftLabel) {
        this.leftLabel = leftLabel;
    }

    public String getRightLabel() {
        return rightLabel;
    }

    public void setRightLabel(String rightLabel) {
        this.rightLabel = rightLabel;
    }

    public Integer getLeftMinCardinality() {
        return leftMinCardinality;
    }

    public void setLeftMinCardinality(Integer leftMinCardinality) {
        this.leftMinCardinality = leftMinCardinality;
    }

    public Integer getLeftMaxCardinality() {
        return leftMaxCardinality;
    }

    public void setLeftMaxCardinality(Integer leftMaxCardinality) {
        this.leftMaxCardinality = leftMaxCardinality;
    }

    public Integer getRightMinCardinality() {
        return rightMinCardinality;
    }

    public void setRightMinCardinality(Integer rightMinCardinality) {
        this.rightMinCardinality = rightMinCardinality;
    }

    public Integer getRightMaxCardinality() {
        return rightMaxCardinality;
    }

    public void setRightMaxCardinality(Integer rightMaxCardinality) {
        this.rightMaxCardinality = rightMaxCardinality;
    }

    @LinkRest(linkClass = EntityTypeRest.class)
    @JsonIgnore
    public EntityTypeRest getLeftType() {
        return leftType;
    }

    public void setLeftType(EntityTypeRest leftType) {
        this.leftType = leftType;
    }

    @LinkRest(linkClass = EntityTypeRest.class)
    @JsonIgnore
    public EntityTypeRest getRightType() {
        return rightType;
    }

    public void setRightType(EntityTypeRest rightType) {
        this.rightType = rightType;
    }
}
