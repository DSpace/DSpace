/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.RelationshipRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Relationship;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * This converter is responsible for transforming the model representation of an Relationship to the REST
 * representation of an Relationship and vice versa
 */
@Component
public class RelationshipConverter implements DSpaceConverter<Relationship, RelationshipRest> {

    // Must be loaded @Lazy, as ConverterService autowires all DSpaceConverter components
    @Lazy
    @Autowired
    private ConverterService converter;

    @Override
    public RelationshipRest convert(Relationship obj, Projection projection) {
        RelationshipRest relationshipRest = new RelationshipRest();
        relationshipRest.setProjection(projection);
        relationshipRest.setId(obj.getID());
        relationshipRest.setLeftId(obj.getLeftItem().getID());
        relationshipRest.setRelationshipType(converter.toRest(obj.getRelationshipType(), projection));
        relationshipRest.setRightId(obj.getRightItem().getID());
        relationshipRest.setLeftPlace(obj.getLeftPlace());
        relationshipRest.setRightPlace(obj.getRightPlace());
        relationshipRest.setLeftwardValue(obj.getLeftwardValue());
        relationshipRest.setRightwardValue(obj.getRightwardValue());
        return relationshipRest;
    }

    @Override
    public Class<Relationship> getModelClass() {
        return Relationship.class;
    }
}
