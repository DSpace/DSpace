/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.RelationshipTypeRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.RelationshipType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * This converter is responsible for transforming the model representation of an RelationshipType to the REST
 * representation of an RelationshipType and vice versa
 */
@Component
public class RelationshipTypeConverter implements DSpaceConverter<RelationshipType, RelationshipTypeRest>  {

    // Must be loaded @Lazy, as ConverterService autowires all DSpaceConverter components
    @Lazy
    @Autowired
    private ConverterService converter;

    @Override
    public RelationshipTypeRest convert(RelationshipType obj, Projection projection) {
        RelationshipTypeRest relationshipTypeRest = new RelationshipTypeRest();
        relationshipTypeRest.setProjection(projection);

        relationshipTypeRest.setId(obj.getID());
        relationshipTypeRest.setLeftwardType(obj.getLeftwardType());
        relationshipTypeRest.setRightwardType(obj.getRightwardType());
        relationshipTypeRest.setCopyToLeft(obj.isCopyToLeft());
        relationshipTypeRest.setCopyToRight(obj.isCopyToRight());
        relationshipTypeRest.setLeftMinCardinality(obj.getLeftMinCardinality());
        relationshipTypeRest.setLeftMaxCardinality(obj.getLeftMaxCardinality());
        relationshipTypeRest.setRightMinCardinality(obj.getRightMinCardinality());
        relationshipTypeRest.setRightMaxCardinality(obj.getRightMaxCardinality());
        relationshipTypeRest.setLeftType(converter.toRest(obj.getLeftType(), projection));
        relationshipTypeRest.setRightType(converter.toRest(obj.getRightType(), projection));

        return relationshipTypeRest;
    }

    @Override
    public Class<RelationshipType> getModelClass() {
        return RelationshipType.class;
    }
}
