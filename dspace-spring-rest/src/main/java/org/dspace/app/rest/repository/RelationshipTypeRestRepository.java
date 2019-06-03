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

import org.dspace.app.rest.converter.RelationshipTypeConverter;
import org.dspace.app.rest.model.RelationshipTypeRest;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.model.hateoas.RelationshipTypeResource;
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

    @Autowired
    private RelationshipTypeConverter relationshipTypeConverter;

    public RelationshipTypeRest findOne(Context context, Integer integer) {
        try {
            return relationshipTypeConverter.fromModel(relationshipTypeService.find(context, integer));
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public Page<RelationshipTypeRest> findAll(Context context, Pageable pageable) {
        List<RelationshipType> relationshipTypeList = null;
        try {
            relationshipTypeList = relationshipTypeService.findAll(context);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        Page<RelationshipTypeRest> page = utils.getPage(relationshipTypeList, pageable).map(relationshipTypeConverter);
        return page;
    }

    public Class<RelationshipTypeRest> getDomainClass() {
        return RelationshipTypeRest.class;
    }

    public DSpaceResource<RelationshipTypeRest> wrapResource(RelationshipTypeRest model, String... rels) {
        return new RelationshipTypeResource(model, utils, rels);
    }
}
