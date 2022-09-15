/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.RelationshipRest;
import org.dspace.app.rest.model.RelationshipTypeRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "relationshipType" subresource of an individual Relationship.
 */
@Component(RelationshipRest.CATEGORY + "." + RelationshipRest.NAME + "." + RelationshipRest.RELATIONSHIP_TYPE)
public class RelationshipTypeRelationshipLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    @Autowired
    RelationshipService relationshipService;

    @PreAuthorize("permitAll()")
    public RelationshipTypeRest getRelationshipType(@Nullable HttpServletRequest request,
                                                            Integer relationshipId,
                                                            @Nullable Pageable optionalPageable,
                                                            Projection projection) {
        try {
            Context context = obtainContext();
            Relationship relationship = relationshipService.find(context, relationshipId);
            if (relationship == null) {
                throw new ResourceNotFoundException("No such relationship: " + relationshipId);
            }
            int total = relationshipService.countByRelationshipType(context, relationship.getRelationshipType());
            Pageable pageable = utils.getPageable(optionalPageable);
            RelationshipType relationshipType = relationship.getRelationshipType();
            return converter.toRest(relationshipType, projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
