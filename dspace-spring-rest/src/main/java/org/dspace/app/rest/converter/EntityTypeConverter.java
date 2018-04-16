/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.EntityTypeRest;
import org.dspace.content.EntityType;
import org.springframework.stereotype.Component;

@Component
public class EntityTypeConverter extends DSpaceConverter<org.dspace.content.EntityType, EntityTypeRest> {

    public EntityTypeRest fromModel(EntityType obj) {
        EntityTypeRest entityTypeRest = new EntityTypeRest();
        entityTypeRest.setId(obj.getId());
        entityTypeRest.setLabel(obj.getLabel());
        return entityTypeRest;
    }

    public EntityType toModel(EntityTypeRest obj) {
        EntityType entityType = new EntityType();
        entityType.setId(obj.getId());
        entityType.setLabel(obj.getLabel());
        return entityType;
    }
}
