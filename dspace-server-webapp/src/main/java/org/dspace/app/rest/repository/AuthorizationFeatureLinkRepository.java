/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureService;
import org.dspace.app.rest.authorization.AuthorizationRestUtil;
import org.dspace.app.rest.model.AuthorizationFeatureRest;
import org.dspace.app.rest.model.AuthorizationRest;
import org.dspace.app.rest.projection.Projection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "feature" subresource of an individual authorization.
 */
@Component(AuthorizationRest.CATEGORY + "." + AuthorizationRest.PLURAL_NAME + "." + AuthorizationRest.FEATURE)
public class AuthorizationFeatureLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    @Autowired
    private AuthorizationRestUtil authorizationRestUtil;

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    @PreAuthorize("hasPermission(#authzId, 'AUTHORIZATION', 'READ')")
    public AuthorizationFeatureRest getFeature(@Nullable HttpServletRequest request,
                                 String authzId,
                                 @Nullable Pageable optionalPageable,
                                 Projection projection) {
        String featureName = authorizationRestUtil.getFeatureName(authzId);
        AuthorizationFeature feature = authorizationFeatureService.find(featureName);
        if (feature == null) {
            return null;
        }
        return converter.toRest(feature, projection);
    }
}
