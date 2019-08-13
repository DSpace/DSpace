/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.RelationshipRest;
import org.dspace.content.Relationship;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * This converter is responsible for transforming the model representation of an Relationship to the REST
 * representation of an Relationship and vice versa
 */
@Component
public class RelationshipConverter implements DSpaceConverter<Relationship, RelationshipRest> {

    @Autowired
    private RelationshipTypeConverter relationshipTypeConverter;


    /**
     * This method converts the Relationship model object that is passed along in the params to the
     * REST representation of this object
     * @param obj   The Relationship model object to be converted
     * @return      The Relationship REST object that is made from the model object
     */
    public RelationshipRest fromModel(Relationship obj) {
        RelationshipRest relationshipRest = new RelationshipRest();
        relationshipRest.setId(obj.getID());
        relationshipRest.setLeftId(obj.getLeftItem().getID());
        relationshipRest.setRelationshipType(relationshipTypeConverter.fromModel(obj.getRelationshipType()));
        relationshipRest.setRightId(obj.getRightItem().getID());
        relationshipRest.setLeftPlace(obj.getLeftPlace());
        relationshipRest.setRightPlace(obj.getRightPlace());
        relationshipRest.setLeftwardValue(obj.getLeftwardValue());
        relationshipRest.setRightwardValue(obj.getRightwardValue());
        return relationshipRest;
    }

    /**
     * This method converts the Relationship REST object that is passed along in the params to the model
     * representation of this object
     * @param obj   The Relationship REST object to be converted
     * @return      The Relationship model object that is made from the REST object
     */
    public Relationship toModel(RelationshipRest obj) {
        throw new NotImplementedException();
    }
}
