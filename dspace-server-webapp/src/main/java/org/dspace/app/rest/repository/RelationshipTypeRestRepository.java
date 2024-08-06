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
import java.util.Objects;

import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.RelationshipTypeRest;
import org.dspace.content.EntityType;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository that is responsible to manage RelationshipType Rest objects
 */
@Component(RelationshipTypeRest.CATEGORY + "." + RelationshipTypeRest.PLURAL_NAME)
public class RelationshipTypeRestRepository extends DSpaceRestRepository<RelationshipTypeRest, Integer> {

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private EntityTypeService entityTypeService;

    @Override
    @PreAuthorize("permitAll()")
    public RelationshipTypeRest findOne(Context context, Integer integer) {
        try {
            return converter.toRest(relationshipTypeService.find(context, integer), utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Page<RelationshipTypeRest> findAll(Context context, Pageable pageable) {
        try {
            List<RelationshipType> relationshipTypes = relationshipTypeService.findAll(context);
            return converter.toRestPage(relationshipTypes, pageable, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Returns a list of relationship types that matches provided entity type on any side of relationship
     * 
     * @param type             The entity type label
     * @param pageable         The page information
     * @return
     * @throws SQLException    If database error
     */
    @SearchRestMethod(name = "byEntityType")
    public Page<RelationshipTypeRest> findByEntityType(@Parameter(value = "type", required = true) String type,
                                                        Pageable pageable) throws SQLException {
        Context context = obtainContext();
        EntityType entityType = entityTypeService.findByEntityType(context, type);
        if (Objects.isNull(entityType)) {
            throw new DSpaceBadRequestException("EntityType with name: " + type + " not found");
        }
        List<RelationshipType> relationshipTypes = relationshipTypeService.findByEntityType(context, entityType,
                                Math.toIntExact(pageable.getPageSize()), Math.toIntExact(pageable.getOffset()));
        int total = relationshipTypeService.countByEntityType(context, entityType);
        return converter.toRestPage(relationshipTypes, pageable, total, utils.obtainProjection());
    }

    @Override
    public Class<RelationshipTypeRest> getDomainClass() {
        return RelationshipTypeRest.class;
    }
}
