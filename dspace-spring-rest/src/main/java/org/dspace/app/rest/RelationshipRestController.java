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

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.converter.RelationshipConverter;
import org.dspace.app.rest.link.HalLinkService;
import org.dspace.app.rest.model.RelationshipRest;
import org.dspace.app.rest.model.RelationshipRestWrapper;
import org.dspace.app.rest.model.hateoas.RelationshipResourceWrapper;
import org.dspace.app.rest.repository.RelationshipRestRepository;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * This will be the entry point for the api/core/relationships endpoint with additional paths to it
 */
@RestController
@RequestMapping("/api/core/relationships")
public class RelationshipRestController {

    /**
     * Regular expression in the request mapping to accept a string as identifier but not the other kind of
     * identifier (digits or uuid)
     */
    private static final String REGEX_REQUESTMAPPING_LABEL = "/{label:^(?!^\\d+$)" +
        "(?!^[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}$)[\\w+\\-]+$+}";

    /**
     * Regular expression in the request mapping to accept number as identifier
     */
    private static final String REGEX_REQUESTMAPPING_IDENTIFIER_AS_DIGIT = "/{id:\\d+}";

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private RelationshipRestRepository relationshipRestRepository;

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

    /**
     * This method will retrieve all the Relationships that have a RelationshipType which has a left or right label
     * equal to the one passed along in the pathvariable.
     * This is further filtered by an optional dso parameter to filter on only the relationships for the given dso
     * if this is applicable
     *
     * @param response  The response object
     * @param request   The request object
     * @param label     The label on which the Relationship's RelationshipType will be matched
     * @param dsoId     The ID of the dso on which we'll search for relationships if applicable
     * @param pageable  The page object
     * @return          A Resource containing all the relationships that meet the criteria
     * @throws Exception    If something goes wrong
     */
    @RequestMapping(method = RequestMethod.GET, value = REGEX_REQUESTMAPPING_LABEL)
    public RelationshipResourceWrapper retrieveByLabel(HttpServletResponse response,
                                                       HttpServletRequest request, @PathVariable String label,
                                                       @RequestParam(name = "dso", required = false) String dsoId,
                                                       Pageable pageable)
        throws Exception {

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
                relationships.addAll(relationshipService.findByItemAndRelationshipType(context,
                                                                                       item, relationshipType));
            }
        } else {
            for (RelationshipType relationshipType : relationshipTypeList) {
                relationships.addAll(relationshipService.findByRelationshipType(context, relationshipType));
            }
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
            relationshipRestWrapper, utils, relationshipRests.size(), pageable);

        halLinkService.addLinks(relationshipResourceWrapper, pageable);
        return relationshipResourceWrapper;
    }

    /**
     * Method to change the left item of a relationship with a given item in the body
     * @return The modified relationship
     */
    @RequestMapping(method = RequestMethod.PUT, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_DIGIT + "/leftItem",
            consumes = {"text/uri-list"})
    public RelationshipRest updateRelationshipLeft(@PathVariable Integer id, HttpServletResponse response,
                                                              HttpServletRequest request) throws SQLException {
        Context context = ContextUtil.obtainContext(request);
        return relationshipRestRepository.put(context,"/api/core/relationships/", id,
                utils.getStringListFromRequest(request), false);
    }

    /**
     * Method to change the right item of a relationship with a given item in the body
     * @return The modified relationship
     */
    @RequestMapping(method = RequestMethod.PUT, value = REGEX_REQUESTMAPPING_IDENTIFIER_AS_DIGIT + "/rightItem",
            consumes = {"text/uri-list"})
    public RelationshipRest updateRelationshipRight(@PathVariable Integer id, HttpServletResponse response,
                                                               HttpServletRequest request) throws SQLException {
        Context context = ContextUtil.obtainContext(request);
        return relationshipRestRepository.put(context,"/api/core/relationships/", id,
                utils.getStringListFromRequest(request), true);
    }
}