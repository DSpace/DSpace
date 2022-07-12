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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.RelationshipRest;
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
import org.dspace.services.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository that is responsible to manage Relationship Rest objects
 */
@Component(RelationshipRest.CATEGORY + "." + RelationshipRest.NAME)
public class RelationshipRestRepository extends DSpaceRestRepository<RelationshipRest, Integer> {

    private static final Logger log = LogManager.getLogger();

    private static final String ALL = "all";
    private static final String LEFT = "left";
    private static final String RIGHT = "right";
    private static final String CONFIGURED = "configured";

    @Autowired
    private ItemService itemService;

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private RequestService requestService;

    @Override
    @PreAuthorize("permitAll()")
    public RelationshipRest findOne(Context context, Integer integer) {
        try {
            Relationship relationship = relationshipService.find(context, integer);

            if (relationship == null) {
                return null;
            }
            return converter.toRest(relationship, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Page<RelationshipRest> findAll(Context context, Pageable pageable) {
        try {
            long total = relationshipService.countTotal(context);
            List<Relationship> relationships = relationshipService.findAll(context,
                    pageable.getPageSize(), Math.toIntExact(pageable.getOffset()));
            return converter.toRestPage(relationships, pageable, total, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Class<RelationshipRest> getDomainClass() {
        return RelationshipRest.class;
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

            String leftwardValue = req.getParameter("leftwardValue");
            String rightwardValue = req.getParameter("rightwardValue");

            EPerson ePerson = context.getCurrentUser();
            if (authorizeService.authorizeActionBoolean(context, leftItem, Constants.WRITE) ||
                authorizeService.authorizeActionBoolean(context, rightItem, Constants.WRITE)) {
                Relationship relationship = relationshipService.create(context, leftItem, rightItem,
                                                                       relationshipType, -1, -1,
                                                                       leftwardValue, rightwardValue);
                return converter.toRest(relationship, utils.obtainProjection());
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
            Item newLeftItem;
            Item newRightItem;

            if (itemToReplaceIsRight) {
                newLeftItem = null;
                newRightItem = replacementItemInRelationship;
            } else {
                newLeftItem = replacementItemInRelationship;
                newRightItem = null;
            }

            if (isAllowedToModifyRelationship(context, relationship, newLeftItem, newRightItem)) {
                try {
                    relationshipService.move(context, relationship, newLeftItem, newRightItem);
                    context.commit();
                    context.reloadEntity(relationship);
                } catch (AuthorizeException e) {
                    throw new AccessDeniedException("You do not have write rights on this relationship's items");
                }

                return converter.toRest(relationship, utils.obtainProjection());
            } else {
                throw new AccessDeniedException("You do not have write rights on this relationship's items");
            }
        } else {
            throw new UnprocessableEntityException("The given items in the request were not valid");
        }

    }

    /**
     * Method to replace the metadata of a relationship (the left/right places and the leftward/rightward labels)
     * @param context     the dspace context
     * @param request
     * @param apiCategory the API category e.g. "api"
     * @param model       the DSpace model e.g. "metadatafield"
     * @param id          the ID of the target REST object
     * @param jsonNode    the part of the request body representing the updated rest object
     * @return
     * @throws RepositoryMethodNotImplementedException
     * @throws SQLException
     * @throws AuthorizeException
     */
    @Override
    protected RelationshipRest put(Context context, HttpServletRequest request, String apiCategory, String model,
                                   Integer id, JsonNode jsonNode)
        throws RepositoryMethodNotImplementedException, SQLException, AuthorizeException {

        Relationship relationship;
        try {
            relationship = relationshipService.find(context, id);
        } catch (SQLException e) {
            throw new ResourceNotFoundException("Relationship" + " with id: " + id + " not found");
        }

        if (relationship == null) {
            throw new ResourceNotFoundException("Relationship" + " with id: " + id + " not found");
        }

        try {

            RelationshipRest relationshipRest;

            try {
                relationshipRest = new ObjectMapper().readValue(jsonNode.toString(), RelationshipRest.class);
            } catch (IOException e) {
                throw new UnprocessableEntityException("Error parsing request body: " + e.toString());
            }

            relationship.setLeftwardValue(relationshipRest.getLeftwardValue());
            relationship.setRightwardValue(relationshipRest.getRightwardValue());

            Integer newRightPlace = null;
            Integer newLeftPlace = null;
            if (jsonNode.hasNonNull("rightPlace")) {
                newRightPlace = relationshipRest.getRightPlace();
            }

            if (jsonNode.hasNonNull("leftPlace")) {
                newLeftPlace = relationshipRest.getLeftPlace();
            }

            relationshipService.move(context, relationship, newLeftPlace, newRightPlace);
            context.commit();
            context.reloadEntity(relationship);

            return converter.toRest(relationship, utils.obtainProjection());
        } catch (AuthorizeException e) {
            throw new AccessDeniedException("You do not have write rights on this relationship's metadata");
        }
    }

    /**
     * This method will check with the current user has write rights on both one of the original items and one of the
     * new items for the relationship.
     * @param context       The relevant DSpace context
     * @param relationship  The relationship to be checked on
     * @param leftItem      The new left Item
     * @param rightItem     The new right Item
     * @return A boolean indicating whether the user is allowed or not
     * @throws SQLException If something goes wrong
     */
    private boolean isAllowedToModifyRelationship(Context context, Relationship relationship, Item leftItem,
                                                  Item rightItem) throws SQLException {
        return (
            // Authorized to write new Items (if specified)
            (leftItem == null || authorizeService.authorizeActionBoolean(context, leftItem, Constants.WRITE))
                || (rightItem == null || authorizeService.authorizeActionBoolean(context, rightItem, Constants.WRITE))
            // Authorized to write old Items
            && (authorizeService.authorizeActionBoolean(context, relationship.getLeftItem(), Constants.WRITE)
                || authorizeService.authorizeActionBoolean(context, relationship.getRightItem(), Constants.WRITE))
            );
    }

    @Override
    protected void delete(Context context, Integer id) throws AuthorizeException {
        String copyVirtual =
            requestService.getCurrentRequest().getServletRequest().getParameter("copyVirtualMetadata");
        if (copyVirtual == null) {
            copyVirtual = "none";
        }

        Relationship relationship = null;
        try {
            relationship = relationshipService.find(context, id);
            if (relationship != null) {
                try {
                    switch (copyVirtual) {
                        case ALL:
                            relationshipService.delete(context, relationship, true, true);
                            break;
                        case LEFT:
                            relationshipService.delete(context, relationship, true, false);
                            break;
                        case RIGHT:
                            relationshipService.delete(context, relationship, false, true);
                            break;
                        case CONFIGURED:
                            relationshipService.delete(context, relationship);
                            break;
                        default:
                            relationshipService.delete(context, relationship, false, false);
                            break;
                    }
                } catch (AuthorizeException e) {
                    throw new AccessDeniedException("You do not have write rights on this relationship's items");
                }
            }
        } catch (SQLException e) {
            log.error("Error deleting Relationship specified by ID:" + id, e);
        }
    }


    /**
     * This method will find all the Relationship objects that a RelationshipType that corresponds to the given Label
     * It's also possible to pass a DSO along to this method with a parameter which will only return Relationship
     * objects that have this DSO as leftItem or rightItem.
     * This endpoint is paginated
     *
     * @param label     The label of a RelationshipType which the Relationships must have if they're to be returned
     * @param dsoId     The dsoId of the object that has to be a leftItem or rightItem if this parameter is present
     * @param pageable  The page object
     * @return          A page with all the RelationshipRest objects that correspond to the constraints
     * @throws SQLException If something goes wrong
     */
    @SearchRestMethod(name = "byLabel")
    public Page<RelationshipRest> findByLabel(@Parameter(value = "label", required = true) String label,
                                              @Parameter(value = "dso", required = false) UUID dsoId,
                                              Pageable pageable) throws SQLException {
        Context context = obtainContext();

        List<RelationshipType> relationshipTypeList =
            relationshipTypeService.findByLeftwardOrRightwardTypeName(context, label);
        List<Relationship> relationships = new LinkedList<>();
        int total = 0;
        if (dsoId != null) {

            Item item = itemService.find(context, dsoId);

            if (item == null) {
                throw new ResourceNotFoundException("The request DSO with id: " + dsoId + " was not found");
            }
            for (RelationshipType relationshipType : relationshipTypeList) {
                boolean isLeft = false;
                if (relationshipType.getLeftwardType().equalsIgnoreCase(label)) {
                    isLeft = true;
                }
                total += relationshipService.countByItemAndRelationshipType(context, item, relationshipType, isLeft);
                relationships.addAll(relationshipService.findByItemAndRelationshipType(context, item, relationshipType,
                        isLeft, pageable.getPageSize(), Math.toIntExact(pageable.getOffset())));
            }
        } else {
            for (RelationshipType relationshipType : relationshipTypeList) {
                total += relationshipService.countByRelationshipType(context, relationshipType);
                relationships.addAll(relationshipService.findByRelationshipType(context, relationshipType,
                        pageable.getPageSize(), Math.toIntExact(pageable.getOffset())));
            }
        }

        return converter.toRestPage(relationships, pageable, total, utils.obtainProjection());
    }

    /**
     * This method is intended to be used when giving an item (focus) and a list
     * of potentially related items we need to know which of these other items
     * are already in a specific relationship with the focus item and,
     * by exclusion which ones are not yet related.
     * 
     * @param typeId          The relationship type id to apply as a filter to the returned relationships
     * @param label           The name of the relation as defined from the side of the 'focusItem'
     * @param focusUUID       The uuid of the item to be checked on the side defined by 'relationshipLabel'
     * @param items           The uuid of the items to be found on the other side of returned relationships
     * @param pageable        The page information
     * @return
     * @throws SQLException   If database error
     */
    @SearchRestMethod(name = "byItemsAndType")
    public Page<RelationshipRest> findByItemsAndType(
                                            @Parameter(value = "typeId", required = true) Integer typeId,
                                            @Parameter(value = "relationshipLabel", required = true) String label,
                                            @Parameter(value = "focusItem", required = true) UUID focusUUID,
                                            @Parameter(value = "relatedItem", required = true) Set<UUID> items,
                                             Pageable pageable) throws SQLException {
        Context context = obtainContext();
        int total = 0;
        List<Relationship> relationships = new LinkedList<>();
        RelationshipType relationshipType = relationshipTypeService.find(context, typeId);
        if (Objects.nonNull(relationshipType)) {
            if (!relationshipType.getLeftwardType().equals(label) &&
                !relationshipType.getRightwardType().equals(label)) {
                throw new UnprocessableEntityException("The provided label: " + label +
                                                       " , does not match any relation!");
            }
            relationships = relationshipService.findByItemRelationshipTypeAndRelatedList(context, focusUUID,
                       relationshipType, new ArrayList<UUID>(items), relationshipType.getLeftwardType().equals(label),
                       Math.toIntExact(pageable.getOffset()),
                       Math.toIntExact(pageable.getPageSize()));

            total = relationshipService.countByItemRelationshipTypeAndRelatedList(context, focusUUID,
                       relationshipType, new ArrayList<UUID>(items), relationshipType.getLeftwardType().equals(label));
        }
        return converter.toRestPage(relationships, pageable, total, utils.obtainProjection());
    }

}
