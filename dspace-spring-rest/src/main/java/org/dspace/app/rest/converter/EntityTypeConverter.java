/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.EntityTypeRest;
import org.dspace.content.ItemRelationshipsType;
import org.springframework.stereotype.Component;

/**
 * This converter is responsible for transforming the model representation of an EntityType to the REST
 * representation of an EntityType and vice versa
 */
@Component
public class EntityTypeConverter extends DSpaceConverter<ItemRelationshipsType, EntityTypeRest> {

    /**
     * This method converts the EntityType model object that is passed along in the params to the
     * REST representation of this object
     * @param obj   The EntityType model object to be converted
     * @return      The EntityType REST object that is made from the model object
     */
    public EntityTypeRest fromModel(ItemRelationshipsType obj) {
        EntityTypeRest entityTypeRest = new EntityTypeRest();
        entityTypeRest.setId(obj.getID());
        entityTypeRest.setLabel(obj.getLabel());
        return entityTypeRest;
    }

    /**
     * This method converts the EntityType REST object that is passed along in the params to the model
     * representation of this object
     * @param obj   The EntityType REST object to be converted
     * @return      The EntityType model object that is made from the REST object
     */
    public ItemRelationshipsType toModel(EntityTypeRest obj) {
        ItemRelationshipsType itemRelationshipsType = new ItemRelationshipsType();
        itemRelationshipsType.setId(obj.getId());
        itemRelationshipsType.setLabel(obj.getLabel());
        return itemRelationshipsType;
    }
}
