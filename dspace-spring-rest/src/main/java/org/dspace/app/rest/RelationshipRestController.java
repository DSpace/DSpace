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
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.rest.converter.RelationshipConverter;
import org.dspace.app.rest.link.HalLinkService;
import org.dspace.app.rest.model.RelationshipRest;
import org.dspace.app.rest.model.RelationshipRestWrapper;
import org.dspace.app.rest.model.hateoas.RelationshipResourceWrapper;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core/relationships")
public class RelationshipRestController {

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private RelationshipConverter relationshipConverter;

    @Autowired
    Utils utils;

    @Autowired
    private HalLinkService halLinkService;

    @RequestMapping(method = RequestMethod.GET, value = "/{label}")
    public RelationshipResourceWrapper retrieveByLabel(HttpServletResponse response,
                                                       HttpServletRequest request, @PathVariable String label,
                                                       @RequestParam(name = "dso", required = false) String dsoId)
        throws SQLException {

        Context context = ContextUtil.obtainContext(request);

        List<RelationshipType> relationshipTypeList = relationshipTypeService.findByLeftOrRightLabel(context, label);
        List<Relationship> relationships = new LinkedList<>();
        if (StringUtils.isNotBlank(dsoId)) {

            UUID uuid = UUIDUtils.fromString(dsoId);
            Item item = itemService.find(context, uuid);

            if (item == null) {
                throw new ResourceNotFoundException("The request DSO with id: " + dsoId + " was not found");
            }
            for (RelationshipType relationshipType : relationshipTypeList) {
                relationships.addAll(relationshipService.findByItemAndRelationshipType(context, item, relationshipType));
            }
        } else {
            for (RelationshipType relationshipType : relationshipTypeList) {
                relationships.addAll(relationshipService.findByRelationshipType(context, relationshipType));
            }
//            relationships = relationshipService.findAll(context);
        }

        List<RelationshipRest> relationshipRests = new LinkedList<>();
        for (Relationship relationship : relationships) {
            relationshipRests.add(relationshipConverter.fromModel(relationship));
        }

        RelationshipRestWrapper relationshipRestWrapper = new RelationshipRestWrapper();
        relationshipRestWrapper.setLabel(label);
        relationshipRestWrapper.setDsoId(dsoId);
        relationshipRestWrapper.setRelationshipRestList(relationshipRests);

        RelationshipResourceWrapper relationshipResourceWrapper = new RelationshipResourceWrapper(
            relationshipRestWrapper, utils);

        halLinkService.addLinks(relationshipResourceWrapper);
        return relationshipResourceWrapper;
    }

}
