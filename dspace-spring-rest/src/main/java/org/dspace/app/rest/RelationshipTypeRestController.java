package org.dspace.app.rest;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.converter.RelationshipTypeConverter;
import org.dspace.app.rest.model.RelationshipTypeRest;
import org.dspace.app.rest.model.RelationshipTypeRestWrapper;
import org.dspace.app.rest.model.hateoas.RelationshipTypeResourceWrapper;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.content.EntityType;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core/entitytypes/{id}/relationshiptypes")
public class RelationshipTypeRestController {


    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private EntityTypeService entityTypeService;

    @Autowired
    private RelationshipTypeConverter relationshipTypeConverter;

    @Autowired
    private Utils utils;

    @RequestMapping(method = RequestMethod.GET)
    public RelationshipTypeResourceWrapper retrieve(@PathVariable Integer id, HttpServletResponse response,
                                                    HttpServletRequest request) throws SQLException {
        Context context = ContextUtil.obtainContext(request);
        EntityType entityType = entityTypeService.find(context, id);
        List<RelationshipType> list = relationshipTypeService.findByEntityType(context, entityType);

        List<RelationshipTypeRest> relationshipTypeRests = new LinkedList<>();

        for (RelationshipType relationshipType : list) {
            relationshipTypeRests.add(relationshipTypeConverter.fromModel(relationshipType));
        }


        RelationshipTypeRestWrapper relationshipTypeRestWrapper = new RelationshipTypeRestWrapper();
        relationshipTypeRestWrapper.setRelationshipTypeRestList(relationshipTypeRests);
        return new RelationshipTypeResourceWrapper(relationshipTypeRestWrapper, utils);
    }
}
