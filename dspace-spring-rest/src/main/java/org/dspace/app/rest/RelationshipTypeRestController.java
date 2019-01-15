/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.converter.RelationshipTypeConverter;
import org.dspace.app.rest.link.HalLinkService;
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

/**
 * This controller will handle all the incoming calls on the api/core/entitytypes/{id}/relationshiptypes endpoint
 * where the id parameter can be filled in to match a specific entityType and then get all the relationshipTypes
 * for the given EntityType
 */
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

    @Autowired
    private HalLinkService halLinkService;

    /**
     * This method will retrieve all the RelationshipTypes that conform to the given EntityType by the given ID and
     * it will return this in a wrapped resource.
     *
     * @param id        The ID of the EntityType objects that we'll use to retrieve the RelationshipTypes
     * @param response  The response object
     * @param request   The request object
     * @return          The wrapped resource containing the list of RelationshipType objects as defined above
     * @throws SQLException If something goes wrong
     */
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
        relationshipTypeRestWrapper.setEntityTypeId(id);
        relationshipTypeRestWrapper.setEntityTypeLabel(entityType.getLabel());
        relationshipTypeRestWrapper.setRelationshipTypeRestList(relationshipTypeRests);

        RelationshipTypeResourceWrapper relationshipTypeResourceWrapper = new RelationshipTypeResourceWrapper(
            relationshipTypeRestWrapper, utils);
        halLinkService.addLinks(relationshipTypeResourceWrapper);
        return relationshipTypeResourceWrapper;
    }
}
