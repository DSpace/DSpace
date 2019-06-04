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
import javax.servlet.http.HttpServletRequest;

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
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
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

    @Override
    protected RelationshipRest createAndReturn(Context context, List<String> stringList)
        throws AuthorizeException, SQLException, RepositoryMethodNotImplementedException {

        HttpServletRequest req = getRequestService().getCurrentRequest().getHttpServletRequest();
        List<DSpaceObject> list = utils.constructDSpaceObjectList(context, stringList);
        if (list.size() == 2 && list.get(0).getType() == Constants.ITEM && list.get(1).getType() == Constants.ITEM) {
            Item leftItem = (Item) list.get(0);
            Item rightItem = (Item) list.get(1);
            RelationshipType relationshipType = relationshipTypeService
                .find(context, Integer.parseInt(req.getParameter("relationshipType")));

            EPerson ePerson = context.getCurrentUser();
            if (authorizeService.authorizeActionBoolean(context, leftItem, Constants.WRITE) ||
                authorizeService.authorizeActionBoolean(context, rightItem, Constants.WRITE)) {
                Relationship relationship = relationshipService.create(context, leftItem, rightItem,
                                                                       relationshipType, 0, 0);
                // The above if check deals with the case that a Relationship can be created if the user has write
                // rights on one of the two items. The following updateItem calls can however call the
                // ItemService.update() functions which would fail if the user doesn't have permission on both items.
                // Since we allow this creation to happen under these circumstances, we need to turn off the
                // authorization system here so that this failure doesn't happen when the items need to be update
                context.turnOffAuthorisationSystem();
                relationshipService.updateItem(context, relationship.getLeftItem());
                relationshipService.updateItem(context, relationship.getRightItem());
                context.restoreAuthSystemState();
                return relationshipConverter.fromModel(relationship);
            } else {
                throw new AccessDeniedException("You do not have write rights on this relationship's items");
            }
        } else {
            throw new UnprocessableEntityException("The given items in the request were not valid items");
        }


    }

    /**
     * Method to replace either the right or left item of a relationship with a given new item
     * Called by request mappings in RelationshipRestController
     * - For replace right item (itemToReplaceIsRight = true)
     *      => Newly proposed changed relationship: left = old-left; right = new-item
     * - For replace left item (itemToReplaceIsRight = false)
     *      => Newly proposed changed relationship: left = new-item; right = old-right
     * @param context
     * @param contextPath           What API call was made to get here
     * @param id                    ID of the relationship we wish to modify
     * @param stringList            Item to replace either right or left item of relationship with
     * @param itemToReplaceIsRight  Boolean to decide whether to replace right item (true) or left item (false)
     * @return  The (modified) relationship
     * @throws SQLException
     */
    public RelationshipRest put(Context context, String contextPath, Integer id, List<String> stringList,
                                Boolean itemToReplaceIsRight) throws SQLException {

        Relationship relationship;
        try {
            relationship = relationshipService.find(context, id);
        } catch (SQLException e) {
            throw new ResourceNotFoundException(contextPath + " with id: " + id + " not found");
        }
        if (relationship == null) {
            throw new ResourceNotFoundException(contextPath + " with id: " + id + " not found");
        }
        List<DSpaceObject> dSpaceObjects = utils.constructDSpaceObjectList(context, stringList);
        if (dSpaceObjects.size() == 1 && dSpaceObjects.get(0).getType() == Constants.ITEM) {

            Item replacementItemInRelationship = (Item) dSpaceObjects.get(0);
            Item leftItem;
            Item rightItem;
            if (itemToReplaceIsRight) {
                leftItem = relationship.getLeftItem();
                rightItem = replacementItemInRelationship;
            } else {
                leftItem = replacementItemInRelationship;
                rightItem = relationship.getRightItem();
            }

            if (isAllowedToModifyRelationship(context, relationship, leftItem, rightItem)) {
                relationship.setLeftItem(leftItem);
                relationship.setRightItem(rightItem);

                try {
                    relationshipService.updatePlaceInRelationship(context, relationship, false);
                    relationshipService.update(context, relationship);
                    context.commit();
                    context.reloadEntity(relationship);
                } catch (AuthorizeException e) {
                    throw new AccessDeniedException("You do not have write rights on this relationship's items");
                }

                return relationshipConverter.fromModel(relationship);
            } else {
                throw new AccessDeniedException("You do not have write rights on this relationship's items");
            }
        } else {
            throw new UnprocessableEntityException("The given items in the request were not valid");
        }

    }

    /**
     * This method will check with the current user has write rights on both one of the original items and one of the
     * new items for the relationship.
     * @param context       The relevant DSpace context
     * @param relationship  The relationship to be checked on
     * @param leftItem      The new left Item
     * @param rightItem     The new right Item
     * @return              A boolean indicating whether the user is allowed or not
     * @throws SQLException If something goes wrong
     */
    private boolean isAllowedToModifyRelationship(Context context, Relationship relationship, Item leftItem,
                                                  Item rightItem) throws SQLException {
        return (authorizeService.authorizeActionBoolean(context, leftItem, Constants.WRITE) ||
            authorizeService.authorizeActionBoolean(context, rightItem, Constants.WRITE)) &&
            (authorizeService.authorizeActionBoolean(context, relationship.getLeftItem(), Constants.WRITE) ||
            authorizeService.authorizeActionBoolean(context, relationship.getRightItem(), Constants.WRITE)
            );
    }

    @Override
    protected void delete(Context context, Integer id) throws AuthorizeException {
        Relationship relationship = null;
        try {
            relationship = relationshipService.find(context, id);
            if (relationship != null) {
                try {
                    relationshipService.delete(context, relationship);
                } catch (AuthorizeException e) {
                    throw new AccessDeniedException("You do not have write rights on this relationship's items");
                }
            }
        } catch (SQLException e) {
            log.error("Error deleting Relationship specified by ID:" + id, e);
        }
    }
}