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

import org.dspace.app.rest.model.DSpaceObjectRest;
import org.dspace.app.rest.model.SubscriptionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.service.SubscribeService;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Link repository for "dataSpaceObject" of subscription
 */
@Component(SubscriptionRest.CATEGORY + "." + SubscriptionRest.NAME + "." + SubscriptionRest.DSPACE_OBJECT)
public class SubscriptionDSpaceObjectLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {

    @Autowired
    private SubscribeService subscribeService;

    public DSpaceObjectRest getDSpaceObject(@Nullable HttpServletRequest request,
                                            @Nullable Pageable optionalPageable,
                                            Integer subscriptionId, Projection projection) {
        try {
            Subscription subscription = subscribeService.findById(obtainContext(), subscriptionId);
            if (Objects.isNull(subscription)) {
                throw new ResourceNotFoundException("No such subscription: " + subscriptionId);
            }
            if (subscription.getdSpaceObject() instanceof Item ||
                subscription.getdSpaceObject() instanceof Community ||
                subscription.getdSpaceObject() instanceof Collection) {
                return converter.toRest(subscription.getdSpaceObject(), projection);
            } else {
                HibernateProxy hibernateProxy = (HibernateProxy) subscription.getdSpaceObject();
                LazyInitializer initializer = hibernateProxy.getHibernateLazyInitializer();
                return converter.toRest(initializer.getImplementation(), projection);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}