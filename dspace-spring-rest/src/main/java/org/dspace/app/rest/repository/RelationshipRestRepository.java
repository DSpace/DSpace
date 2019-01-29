/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.dspace.app.rest.converter.RelationshipConverter;
import org.dspace.app.rest.converter.RelationshipTypeConverter;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.RelationshipRest;
import org.dspace.app.rest.model.hateoas.DSpaceResource;
import org.dspace.app.rest.model.hateoas.RelationshipResource;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * This is the repository that is responsible to manage Relationship Rest objects
 */
@Component(RelationshipRest.CATEGORY + "." + RelationshipRest.NAME)
public class RelationshipRestRepository extends DSpaceRestRepository<RelationshipRest, Integer> {

    private static final Logger log = Logger.getLogger(RelationshipRestRepository.class);


    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private RelationshipConverter relationshipConverter;

    @Autowired
    private RelationshipTypeConverter relationshipTypeConverter;

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private ItemService itemService;

    public RelationshipRest findOne(Context context, Integer integer) {
        try {
            return relationshipConverter.fromModel(relationshipService.find(context, integer));
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public Page<RelationshipRest> findAll(Context context, Pageable pageable) {
        List<Relationship> relationships = null;
        try {
            relationships = relationshipService.findAll(context);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        Page<RelationshipRest> page = utils.getPage(relationships, pageable).map(relationshipConverter);
        return page;
    }

    public Class<RelationshipRest> getDomainClass() {
        return RelationshipRest.class;
    }

    public DSpaceResource<RelationshipRest> wrapResource(RelationshipRest model, String... rels) {
        return new RelationshipResource(model, utils, rels);
    }

    protected RelationshipRest createAndReturn(Context context)
        throws AuthorizeException, SQLException, RepositoryMethodNotImplementedException {

        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        RelationshipRest relationshipRest = null;
        try {
            relationshipRest = mapper.readValue(req.getInputStream(), RelationshipRest.class);
        } catch (IOException e1) {
            throw new UnprocessableEntityException("error parsing the body");
        }

        Relationship relationship = new Relationship();
        Item leftItem = itemService.find(context, UUIDUtils.fromString(req.getParameter("leftItem")));
        Item rightItem = itemService.find(context, UUIDUtils.fromString(req.getParameter("rightItem")));
        RelationshipType relationshipType = relationshipTypeService
            .find(context, Integer.parseInt(req.getParameter("relationshipType")));

        EPerson ePerson = context.getCurrentUser();
        if (authorizeService.authorizeActionBoolean(context, leftItem, Constants.WRITE) ||
            authorizeService.authorizeActionBoolean(context, rightItem, Constants.WRITE)) {
            relationship.setLeftItem(leftItem);
            relationship.setRightItem(rightItem);
            relationship.setRelationshipType(relationshipType);
            relationship = relationshipService.create(context, relationship);
            context.turnOffAuthorisationSystem();
            relationshipService.updateItem(context, relationship.getLeftItem());
            relationshipService.updateItem(context, relationship.getRightItem());
            context.restoreAuthSystemState();
            return relationshipConverter.fromModel(relationship);
        } else {
            throw new AccessDeniedException("You do not have write rights on this relationship's items");
        }

    }

    @Override
    protected RelationshipRest put(Context context, HttpServletRequest request, String apiCategory, String model,
                                   Integer id,
                                   JsonNode jsonNode)
        throws RepositoryMethodNotImplementedException, SQLException, AuthorizeException {

        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        RelationshipRest relationshipRest = null;
        try {
            relationshipRest = mapper.readValue(jsonNode.toString(), RelationshipRest.class);
        } catch (IOException e) {
            throw new UnprocessableEntityException("error parsing the body");
        }
        Relationship relationship = relationshipService.find(context, id);
        if (relationship == null) {
            throw new ResourceNotFoundException(apiCategory + "." + model + " with id: " + id + " not found");
        }
        Item leftItem = itemService.find(context, UUIDUtils.fromString(req.getParameter("leftItem")));
        Item rightItem = itemService.find(context, UUIDUtils.fromString(req.getParameter("rightItem")));
        RelationshipType relationshipType = relationshipTypeService
            .find(context, Integer.parseInt(req.getParameter("relationshipType")));

        if (authorizeService.authorizeActionBoolean(context, leftItem, Constants.WRITE) ||
            authorizeService.authorizeActionBoolean(context, rightItem, Constants.WRITE)) {
            relationship.setId(relationshipRest.getId());
            relationship.setLeftItem(leftItem);
            relationship.setRightItem(rightItem);

            relationship.setRelationshipType(relationshipType);
            if (relationshipRest.getLeftPlace() != -1) {
                relationship.setLeftPlace(relationshipRest.getLeftPlace());
            }
            if (relationshipRest.getRightPlace() != -1) {
                relationship.setRightPlace(relationshipRest.getRightPlace());
            }

            relationshipService.updatePlaceInRelationship(context, relationship, false);
            relationshipService.update(context, relationship);

            return relationshipConverter.fromModel(relationship);
        } else {
            throw new AccessDeniedException("You do not have write rights on this relationship's items");
        }
    }

    @Override
    protected void delete(Context context, Integer id) throws AuthorizeException {
        Relationship relationship = null;
        try {
            relationship = relationshipService.find(context, id);
            if (relationship != null) {
                if (authorizeService.authorizeActionBoolean(context, relationship.getLeftItem(), Constants.WRITE) ||
                    authorizeService.authorizeActionBoolean(context, relationship.getRightItem(), Constants.WRITE)) {
                    relationshipService.delete(context, relationship);
                } else {
                    throw new AccessDeniedException("You do not have write rights on this relationship's items");
                }
            }
        } catch (SQLException e) {
            log.error("Error deleting Relationship specified by ID:" + id, e);
        }
    }
}
