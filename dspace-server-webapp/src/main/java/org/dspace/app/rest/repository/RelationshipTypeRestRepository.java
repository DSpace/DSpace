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

import org.dspace.app.rest.model.RelationshipTypeRest;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * This is the repository that is responsible to manage RelationshipType Rest objects
 */
@Component(RelationshipTypeRest.CATEGORY + "." + RelationshipTypeRest.NAME)
public class RelationshipTypeRestRepository extends DSpaceRestRepository<RelationshipTypeRest, Integer> {

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Override
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
            return converter.toRestPage(utils.getPage(relationshipTypes, pageable), utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Class<RelationshipTypeRest> getDomainClass() {
        return RelationshipTypeRest.class;
    }
}
