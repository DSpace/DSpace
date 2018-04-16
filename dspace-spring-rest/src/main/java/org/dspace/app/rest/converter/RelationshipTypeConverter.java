/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.RelationshipTypeRest;
import org.dspace.content.RelationshipType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RelationshipTypeConverter extends DSpaceConverter<RelationshipType, RelationshipTypeRest>  {

    @Autowired
    private EntityTypeConverter entityTypeConverter;

    public RelationshipTypeRest fromModel(RelationshipType obj) {
        RelationshipTypeRest relationshipTypeRest = new RelationshipTypeRest();

        relationshipTypeRest.setId(obj.getId());
        relationshipTypeRest.setLeftLabel(obj.getLeftLabel());
        relationshipTypeRest.setRightLabel(obj.getRightLabel());
        relationshipTypeRest.setLeftMinCardinality(obj.getLeftMinCardinality());
        relationshipTypeRest.setLeftMaxCardinality(obj.getLeftMaxCardinality());
        relationshipTypeRest.setRightMinCardinality(obj.getRightMinCardinality());
        relationshipTypeRest.setRightMaxCardinality(obj.getRightMaxCardinality());
        relationshipTypeRest.setLeftType(entityTypeConverter.fromModel(obj.getLeftType()));
        relationshipTypeRest.setRightType(entityTypeConverter.fromModel(obj.getRightType()));

        return relationshipTypeRest;
    }

    public RelationshipType toModel(RelationshipTypeRest obj) {
        RelationshipType relationshipType = new RelationshipType();

        relationshipType.setId(obj.getId());
        relationshipType.setLeftLabel(obj.getLeftLabel());
        relationshipType.setRightLabel(obj.getRightLabel());
        relationshipType.setLeftMinCardinality(obj.getLeftMinCardinality());
        relationshipType.setLeftMaxCardinality(obj.getLeftMaxCardinality());
        relationshipType.setRightMinCardinality(obj.getRightMinCardinality());
        relationshipType.setRightMaxCardinality(obj.getRightMaxCardinality());
        relationshipType.setLeftType(entityTypeConverter.toModel(obj.getLeftType()));
        relationshipType.setRightType(entityTypeConverter.toModel(obj.getRightType()));

        return relationshipType;
    }
}
