/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.SubscriptionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.service.SubscribeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for "eperson" of subscription
 */
@Component(SubscriptionRest.CATEGORY + "." + SubscriptionRest.NAME + "." + SubscriptionRest.EPERSON)
public class SubscriptionEPersonLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {

    @Autowired
    private SubscribeService subscribeService;

    @PreAuthorize("hasPermission(#subscriptionId, 'subscription', 'READ')")
    public EPersonRest getEPerson(@Nullable HttpServletRequest request, Integer subscriptionId,
                                  @Nullable Pageable optionalPageable, Projection projection) {
        try {
            Subscription subscription = subscribeService.findById(obtainContext(), subscriptionId);
            if (Objects.isNull(subscription)) {
                throw new ResourceNotFoundException("No such subscription: " + subscriptionId);
            }
            return converter.toRest(subscription.getEPerson(),  projection);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}