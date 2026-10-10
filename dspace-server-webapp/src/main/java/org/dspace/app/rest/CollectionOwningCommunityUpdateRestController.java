/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID;
import static org.dspace.core.Constants.COMMUNITY;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller will handle all the incoming calls on the api/core/collections/{uuid}/owningCommunity endpoint
 * where the uuid corresponds to the collection of which you want to edit the owning community.
 */
@RestController
@RequestMapping("/api/core/collections" + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID + "/owningCommunity")
public class CollectionOwningCommunityUpdateRestController {
    @Autowired
    CollectionService collectionService;

    @Autowired
    CommunityService communityService;

    @Autowired
    AuthorizeService authorizeService;

    @Autowired
    ConverterService converter;

    @Autowired
    Utils utils;

    /**
     * This method will update the owning community of the collection that correspond to the provided collection uuid,
     * effectively moving the collection to the new community.
     *
     * @param uuid The UUID of the collection that will be moved
     * @param inheritCommunityPolicies   Boolean flag whether to inherit the target community policies when
     *                                    moving the collection
     * @param response The response object
     * @param request  The request object
     * @return The wrapped resource containing the new owning community or null when the collection was not moved
     * @throws SQLException       If something goes wrong
     * @throws IOException        If something goes wrong
     * @throws AuthorizeException If the user is not authorized to perform the move action
     */
    @RequestMapping(method = RequestMethod.PUT, consumes = {"text/uri-list"})
    @PreAuthorize("hasPermission(#uuid, 'COLLECTION','WRITE')")
    @PostAuthorize("returnObject != null")
    public CommunityRest move(@PathVariable UUID uuid,
                              @RequestParam(name = "inheritPolicies", defaultValue = "false")
                              Boolean inheritCommunityPolicies,
                              HttpServletResponse response,
                              HttpServletRequest request)
            throws SQLException, IOException, AuthorizeException {
        Context context = ContextUtil.obtainContext(request);

        List<DSpaceObject> dsoList = utils.constructDSpaceObjectList(context, utils.getStringListFromRequest(request));

        if (dsoList.size() != 1 || dsoList.get(0).getType() != COMMUNITY) {
            throw new UnprocessableEntityException("The community doesn't exist " +
                    "or the data cannot be resolved to a community.");
        }

        Community targetCommunity = performCollectionMove(context, uuid, (Community) dsoList.get(0),
                inheritCommunityPolicies);

        if (targetCommunity == null) {
            return null;
        }
        return converter.toRest(targetCommunity, utils.obtainProjection());

    }

    /**
     * This method will move the collection from the current community to the target community
     *
     * @param context           The context object
     * @param collection        The collection to be moved
     * @param currentCommunity  The current owning community of the collection
     * @param targetCommunity   The target community of the collection
     * @param inheritPolicies   Boolean flag whether to inherit the target community policies when moving the collection
     * @return The target community
     * @throws SQLException       If something goes wrong
     * @throws IOException        If something goes wrong
     * @throws AuthorizeException If the user is not authorized to perform the move action
     */
    private Community moveCollection(final Context context, final Collection collection,
            final Community currentCommunity, final Community targetCommunity, final boolean inheritPolicies)
            throws SQLException, IOException, AuthorizeException {
        collectionService.move(context, collection, currentCommunity, targetCommunity, inheritPolicies);

        collectionService.updateLastModified(context, collection);

        // Necessary because Controller does not pass through general RestResourceController, and as such does not do
        // its commit in DSpaceRestRepository.createAndReturn() or similar
        context.commit();

        return context.reloadEntity(targetCommunity);
    }

    /**
     * This method will perform the collection move based on the provided collection uuid and the target community
     *
     * @param context          The context Object
     * @param collectionUuid   The uuid of the collection to be moved
     * @param targetCommunity  The target community
     * @param inheritPolicies  Whether to inherit the target community policies when moving the collection
     * @return The new owning community of the collection when authorized or null when not authorized
     * @throws SQLException       If something goes wrong
     * @throws IOException        If something goes wrong
     * @throws AuthorizeException If the user is not authorized to perform the move action
     */
    private Community performCollectionMove(final Context context, final UUID collectionUuid,
            final Community targetCommunity,boolean inheritPolicies)
            throws SQLException, IOException, AuthorizeException {

        Collection collection = collectionService.find(context, collectionUuid);

        if (collection == null) {
            throw new ResourceNotFoundException("Collection with id: " + collectionUuid + " not found");
        }

        DSpaceObject dso = collectionService.getParentObject(context, collection);
        Community currentCommunity = null;
        if (dso instanceof Community) {
            currentCommunity = (Community) dso;
        }

        if (currentCommunity != null) {
            if (targetCommunity.equals(currentCommunity)) {
                throw new DSpaceBadRequestException("The provided community is already the owning community");
            }

            if (authorizeService.authorizeActionBoolean(context, currentCommunity, Constants.ADMIN)) {

                return moveCollection(context, collection, currentCommunity, targetCommunity, inheritPolicies);
            }
        }
        return null;
    }

}
