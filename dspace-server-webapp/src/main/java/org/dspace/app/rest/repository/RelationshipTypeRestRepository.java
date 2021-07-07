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
import javax.persistence.EntityNotFoundException;

import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.model.MetadataFieldRest;
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
@Component(RelationshipTypeRest.CATEGORY + "." + RelationshipTypeRest.NAME)
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

    @SearchRestMethod(name = "byEntityTypeId")
    public Page<MetadataFieldRest> findByEntityTypeId(@Parameter(value = "id", required = true) int entityTypeId,
        Pageable pageable) {

        try {
            Context context = obtainContext();
            EntityType entityType = entityTypeService.find(context, entityTypeId);
            if (entityType == null) {
                throw new EntityNotFoundException(
                    String.format("There was no entityType found with id %s", entityTypeId));
            }
            return this.findByEntityTypeWithPagination(context, entityType, pageable);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @SearchRestMethod(name = "byEntityType")
    public Page<MetadataFieldRest> findByEntityType(@Parameter(value = "type", required = true) String entityTypeLabel,
        Pageable pageable) {

        try {
            Context context = obtainContext();
            EntityType entityType = entityTypeService.findByEntityType(context, entityTypeLabel);
            if (entityType == null) {
                throw new EntityNotFoundException(
                    String.format("There was no entityType found with label %s", entityTypeLabel));
            }
            return this.findByEntityTypeWithPagination(context, entityType, pageable);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private Page<MetadataFieldRest> findByEntityTypeWithPagination(Context context, EntityType entityType,
        Pageable pageable) throws SQLException {

        List<RelationshipType> pageLimitedMatchingRelationshipTypes =
            relationshipTypeService.findByEntityType(context, entityType);
        return converter.toRestPage(pageLimitedMatchingRelationshipTypes, pageable, utils.obtainProjection());
    }

    @Override
    public Class<RelationshipTypeRest> getDomainClass() {
        return RelationshipTypeRest.class;
    }
}
