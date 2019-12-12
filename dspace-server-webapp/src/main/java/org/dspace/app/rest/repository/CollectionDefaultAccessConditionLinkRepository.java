/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.ResourcePolicyRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.model.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "defaultAccessConditions" subresource of an individual collection.
 */
@Component(CollectionRest.CATEGORY + "." + CollectionRest.NAME + "." + CollectionRest.DEFAULT_ACCESS_CONDITIONS)
public class CollectionDefaultAccessConditionLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    @Autowired
    AuthorizeService authorizeService;

    @Autowired
    CollectionService collectionService;

    @PreAuthorize("hasPermission(#collectionId, 'COLLECTION', 'READ')")
    public Page<BitstreamRest> getDefaultAccessConditions(@Nullable HttpServletRequest request,
                                                          UUID collectionId,
                                                          @Nullable Pageable optionalPageable,
                                                          Projection projection) {
        try {
            Context context = obtainContext();
            Collection collection = collectionService.find(context, collectionId);
            if (collection == null) {
                return null;
            }
            List<ResourcePolicy> policies = authorizeService.getPoliciesActionFilter(context, collection,
                    Constants.DEFAULT_BITSTREAM_READ);
            Pageable pageable = optionalPageable != null ? optionalPageable : new PageRequest(0, 20);
            return converter.toRestPage(utils.getPage(policies, pageable), projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
