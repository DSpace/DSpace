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
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.EntityTypeRest;
import org.dspace.app.rest.model.RelationshipTypeRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.EntityType;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Link repository for "relationships" subresource of an individual EntityType
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component(EntityTypeRest.CATEGORY + "." + EntityTypeRest.NAME + "." + EntityTypeRest.RELATION_SHIP_TYPES)
public class EntityTypeRelationshipLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    @Autowired
    private EntityTypeService entityTypeService;
    @Autowired
    private RelationshipTypeService relationshipTypeService;

    /**
     * This method will retrieve all the RelationshipTypes that conform
     * to the given EntityType by the given ID and it will return this in a wrapped resource.
     * 
     * @param request                The request object
     * @param id                     The ID of the EntityType objects that we'll use to retrieve the RelationshipTypes
     * @param optionalPageable       The pagination object
     * @param projection             The current Projection
     * @return                       List of RelationshipType objects as defined above
     */
    public Page<RelationshipTypeRest> getEntityTypeRelationship(@Nullable HttpServletRequest request,
                                                                          Integer id,
                                                                @Nullable Pageable optionalPageable,
                                                                          Projection projection) {
        try {
            Context context = obtainContext();
            Pageable pageable = utils.getPageable(optionalPageable);
            EntityType entityType = entityTypeService.find(context, id);
            if (Objects.isNull(entityType)) {
                throw new ResourceNotFoundException("No such EntityType: " + id);
            }
            int total = relationshipTypeService.countByEntityType(context, entityType);
            List<RelationshipType> list = relationshipTypeService.findByEntityType(context, entityType,
                                          pageable.getPageSize(), Math.toIntExact(pageable.getOffset()));
            return converter.toRestPage(list, pageable, total, projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}