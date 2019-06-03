/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.List;

import org.dspace.app.rest.converter.EntityTypeConverter;
import org.dspace.app.rest.model.EntityTypeRest;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.model.hateoas.EntityTypeResource;
import org.dspace.content.EntityType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

/**
 * This is the repository that is responsible to manage EntityType Rest objects
 */
@Component(EntityTypeRest.CATEGORY + "." + EntityTypeRest.NAME)
public class EntityTypeRestRepository extends DSpaceRestRepository<EntityTypeRest, Integer> {

    @Autowired
    private EntityTypeService entityTypeService;

    @Autowired
    private EntityTypeConverter entityTypeConverter;

    public EntityTypeRest findOne(Context context, Integer integer) {
        try {
            EntityType entityType = entityTypeService.find(context, integer);
            if (entityType == null) {
                throw new ResourceNotFoundException("The entityType for ID: " + integer + " could not be found");
            }
            return entityTypeConverter.fromModel(entityType);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public Page<EntityTypeRest> findAll(Context context, Pageable pageable) {
        List<EntityType> entityTypeList = null;
        try {
            entityTypeList = entityTypeService.findAll(context);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        Page<EntityTypeRest> page = utils.getPage(entityTypeList, pageable).map(entityTypeConverter);
        return page;
    }

    public Class<EntityTypeRest> getDomainClass() {
        return EntityTypeRest.class;
    }

    public DSpaceResource<EntityTypeRest> wrapResource(EntityTypeRest model, String... rels) {
        return new EntityTypeResource(model, utils, rels);
    }
}
