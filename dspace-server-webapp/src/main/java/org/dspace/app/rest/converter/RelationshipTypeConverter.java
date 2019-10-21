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

/**
 * This converter is responsible for transforming the model representation of an RelationshipType to the REST
 * representation of an RelationshipType and vice versa
 */
@Component
public class RelationshipTypeConverter implements DSpaceConverter<RelationshipType, RelationshipTypeRest>  {

    @Autowired
    private ConverterService converter;

    /**
     * This method converts the RelationshipType model object that is passed along in the params to the
     * REST representation of this object
     * @param obj   The RelationshipType model object to be converted
     * @return      The RelationshipType REST object that is made from the model object
     */
    public RelationshipTypeRest convert(RelationshipType obj) {
        RelationshipTypeRest relationshipTypeRest = new RelationshipTypeRest();

        relationshipTypeRest.setId(obj.getID());
        relationshipTypeRest.setLeftwardType(obj.getLeftwardType());
        relationshipTypeRest.setRightwardType(obj.getRightwardType());
        relationshipTypeRest.setLeftMinCardinality(obj.getLeftMinCardinality());
        relationshipTypeRest.setLeftMaxCardinality(obj.getLeftMaxCardinality());
        relationshipTypeRest.setRightMinCardinality(obj.getRightMinCardinality());
        relationshipTypeRest.setRightMaxCardinality(obj.getRightMaxCardinality());
        relationshipTypeRest.setLeftType(converter.toRest(obj.getLeftType()));
        relationshipTypeRest.setRightType(converter.toRest(obj.getRightType()));

        return relationshipTypeRest;
    }

    @Override
    public Class<RelationshipType> getModelClass() {
        return RelationshipType.class;
    }
}
