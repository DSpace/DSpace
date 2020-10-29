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
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.model.EntityTypeRest;
import org.dspace.content.Collection;
import org.dspace.content.EntityType;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.EntityTypeService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.external.service.ExternalDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository that is responsible to manage EntityType Rest objects
 */
@Component(EntityTypeRest.CATEGORY + "." + EntityTypeRest.NAME)
public class EntityTypeRestRepository extends DSpaceRestRepository<EntityTypeRest, Integer> {

    @Autowired
    private EntityTypeService entityTypeService;
    @Autowired
    private CollectionService collectionService;
    @Autowired
    private ExternalDataService externalDataService;

    @Override
    @PreAuthorize("permitAll()")
    public EntityTypeRest findOne(Context context, Integer integer) {
        try {
            EntityType entityType = entityTypeService.find(context, integer);
            if (entityType == null) {
                throw new ResourceNotFoundException("The entityType for ID: " + integer + " could not be found");
            }
            return converter.toRest(entityType, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Page<EntityTypeRest> findAll(Context context, Pageable pageable) {
        try {
            List<EntityType> entityTypes = entityTypeService.findAll(context);
            return converter.toRestPage(entityTypes, pageable, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Class<EntityTypeRest> getDomainClass() {
        return EntityTypeRest.class;
    }

    @SearchRestMethod(name = "findAllByAuthorizedCollection")
    public Page<EntityTypeRest> findAllByAuthorizedCollection(Pageable pageable) {
        try {
            Context context = obtainContext();
            List<Collection> collections = collectionService.findAuthorized(context, null, Constants.ADD);

            List<EntityType> entityTypes =
                collections.stream().map(type -> type.getRelationshipType()).distinct()
                    .map(type -> {
                        if (StringUtils.isBlank(type)) {
                            return null;
                        }
                        try {
                            return entityTypeService.findByEntityType(context, type);
                        } catch (SQLException e) {
                            throw new RuntimeException(e.getMessage(), e);
                        }
                    }).filter(x -> x != null).collect(Collectors.toList());
            return converter.toRestPage(entityTypes, pageable, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @SearchRestMethod(name = "findAllByAuthorizedExternalSource")
    public Page<EntityTypeRest> findAllByAuthorizedExternalSource(Pageable pageable) {
        try {
            Context context = obtainContext();
            List<Collection> collections = collectionService.findAuthorized(context, null, Constants.ADD);
            List<EntityType> entityTypes = collections.stream().map(type -> type.getRelationshipType()).distinct()
                    .map(type -> {
                        if (StringUtils.isBlank(type)) {
                            return null;
                        }
                        try {
                            return entityTypeService.findByEntityType(context, type);
                        } catch (SQLException e) {
                            throw new RuntimeException(e.getMessage(), e);
                        }
                    }).filter(x -> x != null)
                    .filter(x -> externalDataService.getExternalDataProvidersForEntityType(x.getLabel()).size() > 0)
                    .collect(Collectors.toList());
            return converter.toRestPage(entityTypes, pageable, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
