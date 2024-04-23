/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.authorization.AuthorizationRestUtil;
import org.dspace.app.rest.model.AuthorizationRest;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "object" subresource of an individual authorization.
 */
@Component(AuthorizationRest.CATEGORY + "." + AuthorizationRest.PLURAL_NAME + "." + AuthorizationRest.OBJECT)
public class AuthorizationObjectLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    @Autowired
    private AuthorizationRestUtil authorizationRestUtil;

    @PreAuthorize("hasPermission(#authzId, 'AUTHORIZATION', 'READ')")
    public BaseObjectRest getObject(@Nullable HttpServletRequest request,
                                 String authzId,
                                 @Nullable Pageable optionalPageable,
                                 Projection projection) {
        Context context = obtainContext();
        BaseObjectRest object;
        try {
            object = authorizationRestUtil.getObject(context, authzId);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return object;
    }
}
