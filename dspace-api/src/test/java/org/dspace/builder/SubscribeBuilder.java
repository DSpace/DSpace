/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.builder;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.SubscriptionParameter;
import org.dspace.eperson.service.SubscribeService;

public class SubscribeBuilder extends AbstractBuilder<Subscription, SubscribeService> {

    /* Log4j logger*/
    private static final Logger log = LogManager.getLogger();

    private Subscription subscription;

    protected SubscribeBuilder(Context context) {
        super(context);
    }

    @Override
    protected SubscribeService getService() {
        return subscribeService;
    }

    @Override
    public void cleanup() throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            // Ensure object and any related objects are reloaded before checking to see what needs cleanup
            subscription = c.reloadEntity(subscription);
            if (subscription != null) {
                delete(c, subscription);
            }
            c.complete();
            indexingService.commit();
        }
    }

    public static void deleteSubscription(int id) throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            Subscription subscription = subscribeService.findById(c, id);
            if (Objects.nonNull(subscription)) {
                try {
                    subscribeService.deleteSubscription(c, subscription);
                } catch (SQLException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }
            c.complete();
        }
        indexingService.commit();
    }

    @Override
    public Subscription build() {
        try {

            context.dispatchEvents();

            indexingService.commit();
        } catch (SearchServiceException  e) {
            log.error(e);
        }
        return subscription;
    }

    public static SubscribeBuilder subscribeBuilder(final Context context, String type, DSpaceObject dSpaceObject,
            EPerson ePerson, List<SubscriptionParameter> subscriptionParameterList) {
        SubscribeBuilder builder = new SubscribeBuilder(context);
        return builder.create(context, type, dSpaceObject, ePerson, subscriptionParameterList);
    }

    private SubscribeBuilder create(Context context, String type, DSpaceObject dSpaceObject, EPerson ePerson,
            List<SubscriptionParameter> subscriptionParameterList) {
        try {

            this.context = context;
            this.subscription = subscribeService.subscribe(context, ePerson, dSpaceObject,
                    subscriptionParameterList, type);

        } catch (SQLException | AuthorizeException e) {
            log.warn("Failed to create the Subscription", e);
        }
        return this;
    }

    @Override
    public void delete(Context c, Subscription dso) throws Exception {
        if (Objects.nonNull(dso)) {
            getService().deleteSubscription(c, dso);
        }
    }

}