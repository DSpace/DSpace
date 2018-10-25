/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.app.rest.model.RelationshipRest;
import org.dspace.content.Relationship;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This converter is responsible for transforming the model representation of an Relationship to the REST
 * representation of an Relationship and vice versa
 */
@Component
public class RelationshipConverter extends DSpaceConverter<Relationship, RelationshipRest> {

    private static final Logger log = Logger.getLogger(RelationshipConverter.class);

    @Autowired
    private ItemService itemService;

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
        relationshipRest.setId(obj.getId());
        relationshipRest.setLeftId(obj.getLeftItem().getID());
        relationshipRest.setRelationshipType(relationshipTypeConverter.fromModel(obj.getRelationshipType()));
        relationshipRest.setRightId(obj.getRightItem().getID());
        relationshipRest.setLeftPlace(obj.getLeftPlace());
        relationshipRest.setRightPlace(obj.getRightPlace());
        return relationshipRest;
    }

    /**
     * This method converts the Relationship REST object that is passed along in the params to the model
     * representation of this object
     * @param obj   The Relationship REST object to be converted
     * @return      The Relationship model object that is made from the REST object
     */
    public Relationship toModel(RelationshipRest obj) {
        Relationship relationship = new Relationship();
        try {
            Context context = new Context();
            relationship.setLeftItem(itemService.find(context, obj.getLeftId()));
            relationship.setRightItem(itemService.find(context, obj.getRightId()));
        } catch (SQLException e) {
            log.error(e,e);
        }
        relationship.setRelationshipType(relationshipTypeConverter.toModel(obj.getRelationshipType()));
        relationship.setLeftPlace(obj.getLeftPlace());
        relationship.setRightPlace(obj.getRightPlace());
        relationship.setId(obj.getId());
        return relationship;
    }
}
