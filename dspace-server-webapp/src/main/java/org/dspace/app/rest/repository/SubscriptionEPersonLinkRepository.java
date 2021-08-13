/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.SubscriptionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.core.Context;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.service.SubscribeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;



/**
 * Link repository for "eperson" of subscription
 */
@Component(SubscriptionRest.CATEGORY + "." + SubscriptionRest.NAME + "." + SubscriptionRest.EPERSON)
public class SubscriptionEPersonLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {

    @Autowired
    SubscribeService subscribeService;

    public EPersonRest getEPerson(@Nullable HttpServletRequest request,
                                  Integer subscriptionId,
                                  @Nullable Pageable optionalPageable,
                                  Projection projection) {
        try {
            Context context = obtainContext();
            Subscription subscription = subscribeService.findById(context, subscriptionId);
            if (subscription == null) {
                throw new ResourceNotFoundException("No such subscription: " + subscriptionId);
            }

            return converter.toRest(subscription.getePerson(),  projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
