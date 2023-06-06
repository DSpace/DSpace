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
import java.util.Objects;

import org.dspace.app.bulkaccesscontrol.model.BulkAccessConditionConfiguration;
import org.dspace.app.bulkaccesscontrol.service.BulkAccessConditionConfigurationService;
import org.dspace.app.rest.exception.RESTAuthorizationException;
import org.dspace.app.rest.model.BulkAccessConditionRest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to manage Bulk Access Condition options
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
@Component(BulkAccessConditionRest.CATEGORY + "." + BulkAccessConditionRest.NAME)
public class BulkAccessConditionRestRepository extends DSpaceRestRepository<BulkAccessConditionRest, String> {

    @Autowired
    private BulkAccessConditionConfigurationService bulkAccessConditionConfigurationService;

    @Autowired
    private AuthorizeService authorizeService;

    @Override
    @PreAuthorize("permitAll()")
    public BulkAccessConditionRest findOne(Context context, String id) {

        if (!isAuthorized(context)) {
            throw new RESTAuthorizationException("Only admin users of community or collection or item " +
                "are allowed to bulk access condition");
        }

        BulkAccessConditionConfiguration bulkConfiguration =
            bulkAccessConditionConfigurationService.getBulkAccessConditionConfiguration(id);

        return Objects.nonNull(bulkConfiguration) ?
            converter.toRest(bulkConfiguration, utils.obtainProjection()) : null;
    }

    @Override
    @PreAuthorize("permitAll()")
    public Page<BulkAccessConditionRest> findAll(Context context, Pageable pageable) {

        if (!isAuthorized(context)) {
            throw new RESTAuthorizationException("Only admin users of community or collection or item " +
                "are allowed to bulk access condition");
        }

        List<BulkAccessConditionConfiguration> configurations =
            bulkAccessConditionConfigurationService.getBulkAccessConditionConfigurations();

        return converter.toRestPage(configurations, pageable, configurations.size(), utils.obtainProjection());
    }

    @Override
    public Class<BulkAccessConditionRest> getDomainClass() {
        return BulkAccessConditionRest.class;
    }

    private boolean isAuthorized(Context context) {
        try {
            return context.getCurrentUser() != null &&
                (authorizeService.isAdmin(context) || authorizeService.isComColAdmin(context) ||
                    authorizeService.isItemAdmin(context));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}