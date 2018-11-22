package org.dspace.app.rest;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.converter.RelationshipConverter;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core/items/" +
    "{uuid:[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}}/relationships")
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

    @RequestMapping(method = RequestMethod.GET, value = "/{label}")
    public RelationshipResourceWrapper retrieveByLabel(@PathVariable UUID uuid, HttpServletResponse response,
                                                       HttpServletRequest request, @PathVariable String label)
        throws SQLException {

        Context context = ContextUtil.obtainContext(request);

        List<RelationshipType> relationshipTypeList = relationshipTypeService.findByLeftOrRightLabel(context, label);
        Item item = itemService.find(context, uuid);

        List<Relationship> relationships = new LinkedList<>();

        for (RelationshipType relationshipType : relationshipTypeList) {
            relationships.addAll(relationshipService.findByItemAndRelationshipType(context, item, relationshipType));
        }

        List<RelationshipRest> relationshipRests = new LinkedList<>();
        for (Relationship relationship : relationships) {
            relationshipRests.add(relationshipConverter.fromModel(relationship));
        }

        RelationshipRestWrapper relationshipRestWrapper = new RelationshipRestWrapper();
        relationshipRestWrapper.setRelationshipRestList(relationshipRests);

        RelationshipResourceWrapper relationshipResourceWrapper = new RelationshipResourceWrapper(
            relationshipRestWrapper, utils);

        return relationshipResourceWrapper;
    }

}
