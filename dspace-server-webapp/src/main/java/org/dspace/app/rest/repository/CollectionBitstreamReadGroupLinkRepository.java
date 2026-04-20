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
import java.util.UUID;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "BitstreamReadGroup" subresource of an individual collection.
 *
 */
@Component(CollectionRest.CATEGORY + "." + CollectionRest.PLURAL_NAME + "." + CollectionRest.BITSTREAM_READ_GROUP)
public class CollectionBitstreamReadGroupLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private AuthorizeService authorizeService;

    /**
     * This method is responsible for retrieving the BitstreamReadGroup of a Collection
     * @param request           The current request
     * @param collectionId       The id of the collection that we'll retrieve the BitstreamReadGroup for
     * @param optionalPageable  The pageable if applicable
     * @param projection        The current Projection
     * @return The BitstreamReadGroup of the given collection
     */
    @PreAuthorize("hasPermission(#collectionId, 'COLLECTION', 'READ')")
    public GroupRest getBitstreamReadGroup(@Nullable HttpServletRequest request,
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
                AuthorizeUtil.authorizeManageDefaultReadGroup(context, collection);
            } catch (AuthorizeException e) {
                throw new AccessDeniedException("The current user was not allowed to retrieve the " +
                                                    "bitstreamReadGroup for collection: " + collectionId);
            }
            List<Group> bitstreamGroups = authorizeService
                .getAuthorizedGroups(context, collection, Constants.DEFAULT_BITSTREAM_READ);
            if (bitstreamGroups == null || bitstreamGroups.isEmpty()) {
                return null;
            }
            Group bitstreamReadGroup = bitstreamGroups.get(0);

            if (bitstreamReadGroup == null) {
                return null;
            }
            return converter.toRest(bitstreamReadGroup, projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
