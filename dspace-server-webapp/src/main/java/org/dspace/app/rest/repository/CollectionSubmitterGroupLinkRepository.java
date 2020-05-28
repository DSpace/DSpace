/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "submittergroup" subresource of an individual collection.
 *
 */
@Component(CollectionRest.CATEGORY + "." + CollectionRest.NAME + "." + CollectionRest.SUBMITTERS_GROUP)
public class CollectionSubmitterGroupLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {

    @Autowired
    private CollectionService collectionService;

    /**
     * This method is responsible for retrieving the Submittergroup of a Collection
     * @param request           The current request
     * @param collectionId       The id of the collection that we'll retrieve the submitterGroup for
     * @param optionalPageable  The pageable if applicable
     * @param projection        The current Projection
     * @return                  The submitterGroup of the given collection
     */
    @PreAuthorize("hasPermission(#collectionId, 'COLLECTION', 'READ')")
    public GroupRest getSubmittersGroup(@Nullable HttpServletRequest request,
                                   UUID collectionId,
                                   @Nullable Pageable optionalPageable,
                                   Projection projection) {
        try {
            Context context = obtainContext();
            Collection collection = collectionService.find(context, collectionId);
            if (collection == null) {
                throw new ResourceNotFoundException("No such collection: " + collectionId);
            }
            try {
                AuthorizeUtil.authorizeManageSubmittersGroup(context, collection);
            } catch (AuthorizeException e) {
                throw new AccessDeniedException("The current user was not allowed to retrieve the submitterGroup for" +
                                                    " collection: " + collectionId);
            }
            Group submitters = collection.getSubmitters();
            if (submitters == null) {
                return null;
            }
            return converter.toRest(submitters, projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
