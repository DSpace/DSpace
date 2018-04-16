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

public class RelationshipTypeRest extends BaseObjectRest<Integer> {

    public static final String NAME = "relationshiptype";
    public static final String CATEGORY = "core";

    private String leftLabel;
    private String rightLabel;
    private int leftMinCardinality;
    private int leftMaxCardinality;
    private int rightMinCardinality;
    private int rightMaxCardinality;
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

    public int getLeftMinCardinality() {
        return leftMinCardinality;
    }

    public void setLeftMinCardinality(int leftMinCardinality) {
        this.leftMinCardinality = leftMinCardinality;
    }

    public int getLeftMaxCardinality() {
        return leftMaxCardinality;
    }

    public void setLeftMaxCardinality(int leftMaxCardinality) {
        this.leftMaxCardinality = leftMaxCardinality;
    }

    public int getRightMinCardinality() {
        return rightMinCardinality;
    }

    public void setRightMinCardinality(int rightMinCardinality) {
        this.rightMinCardinality = rightMinCardinality;
    }

    public int getRightMaxCardinality() {
        return rightMaxCardinality;
    }

    public void setRightMaxCardinality(int rightMaxCardinality) {
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
