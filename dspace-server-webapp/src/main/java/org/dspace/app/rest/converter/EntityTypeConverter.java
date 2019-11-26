/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.EntityTypeRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.EntityType;
import org.springframework.stereotype.Component;

/**
 * This converter is responsible for transforming the model representation of an EntityType to the REST
 * representation of an EntityType and vice versa
 */
@Component
public class EntityTypeConverter implements DSpaceConverter<org.dspace.content.EntityType, EntityTypeRest> {

    @Override
    public EntityTypeRest convert(EntityType obj, Projection projection) {
        EntityTypeRest entityTypeRest = new EntityTypeRest();
        entityTypeRest.setProjection(projection);
        entityTypeRest.setId(obj.getID());
        entityTypeRest.setLabel(obj.getLabel());
        return entityTypeRest;
    }

    @Override
    public Class<EntityType> getModelClass() {
        return EntityType.class;
    }
}
