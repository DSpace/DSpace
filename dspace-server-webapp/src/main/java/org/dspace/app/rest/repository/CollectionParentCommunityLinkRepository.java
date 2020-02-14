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
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

@Component(CollectionRest.CATEGORY + "." + CollectionRest.NAME + "." + CollectionRest.PARENT_COMMUNITY)
public class CollectionParentCommunityLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {

    @Autowired
    private CollectionService collectionService;

    public CommunityRest getParentCommunity(@Nullable HttpServletRequest httpServletRequest,
                                            UUID collectionId,
                                            @Nullable Pageable optionalPageable,
                                            Projection projection) {
        try {
            Context context = obtainContext();
            Collection collection = collectionService.find(context, collectionId);
            Community parentCommunity = (Community) collectionService.getParentObject(context, collection);
            if (collection == null) {
                throw new ResourceNotFoundException("No such collection: " + collectionId);
            }
            return converter.toRest(parentCommunity, projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
