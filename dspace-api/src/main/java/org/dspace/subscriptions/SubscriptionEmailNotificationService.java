/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.subscriptions;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.service.SubscribeService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.dspace.subscriptions.service.DSpaceObjectUpdates;
import org.dspace.subscriptions.service.SubscriptionGenerator;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

/**
 * Implementation of {@link DSpaceRunnable} to find subscribed objects and send notification mails about them
 *
 * @author alba aliu
 */
public class SubscriptionEmailNotificationService {

    private static final Logger log = LogManager.getLogger(SubscriptionEmailNotification.class);

    public static final List<String> FREQUENCIES = Arrays.asList("D", "W", "M");

    private Map<String, DSpaceObjectUpdates> contentUpdates = new HashMap<>();
    @SuppressWarnings("rawtypes")
    private Map<String, SubscriptionGenerator> generators = new HashMap<>();
    @SuppressWarnings("rawtypes")
    private List<IndexableObject> communities = new ArrayList<>();
    @SuppressWarnings("rawtypes")
    private List<IndexableObject> collections = new ArrayList<>();
    @SuppressWarnings("rawtypes")
    private List<IndexableObject> items = new ArrayList<>();

    private final SubscribeService subscribeService;

    @SuppressWarnings("unchecked")
    public void perform(Context context, DSpaceRunnableHandler handler, String type, String frequency) {
        try {
            context.turnOffAuthorisationSystem();
            List<Subscription> subscriptions = findAllSubscriptionsBySubscriptionTypeAndFrequency(context, type,
                                               frequency);
            // if content subscription
            // Here is verified if type is "content" Or "statistics" as them are configured
            if (type.equals(generators.keySet().toArray()[0])) {
                // the list of the person who has subscribed
                int iterator = 0;
                for (Subscription subscription : subscriptions) {
                    DSpaceObject dSpaceObject = getdSpaceObject(subscription);
                    if (dSpaceObject instanceof Community) {
                        communities.addAll(contentUpdates.get(Community.class.getSimpleName().toLowerCase(Locale.ROOT))
                                   .findUpdates(context, dSpaceObject, frequency));
                    } else if (dSpaceObject instanceof Collection) {
                        collections.addAll(contentUpdates.get(Collection.class.getSimpleName().toLowerCase(Locale.ROOT))
                                   .findUpdates(context, dSpaceObject, frequency));
                    } else if (dSpaceObject instanceof Item) {
                        items.addAll(contentUpdates.get(Item.class.getSimpleName().toLowerCase(Locale.ROOT))
                             .findUpdates(context, dSpaceObject, frequency));
                    }
                    var ePerson = subscription.getePerson();
                    if (iterator < subscriptions.size() - 1) {
                        if (ePerson.equals(subscriptions.get(iterator + 1).getePerson())) {
                            iterator++;
                            continue;
                        } else {
                            generators.get(type)
                                      .notifyForSubscriptions(context, ePerson, communities, collections, items);
                            communities.clear();
                            collections.clear();
                            items.clear();
                        }
                    } else {
                        //in the end of the iteration
                        generators.get(type).notifyForSubscriptions(context, ePerson, communities, collections, items);
                    }
                    iterator++;
                }
            } else {
                throw new IllegalArgumentException("Currently this type:" + type +
                                                   " of subscription is not supported!");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            handler.handleException(e);
            context.abort();
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private DSpaceObject getdSpaceObject(Subscription subscription) {
        DSpaceObject dSpaceObject = subscription.getdSpaceObject();
        if (subscription.getdSpaceObject() instanceof HibernateProxy) {
            HibernateProxy hibernateProxy = (HibernateProxy) subscription.getdSpaceObject();
            LazyInitializer initializer = hibernateProxy.getHibernateLazyInitializer();
            dSpaceObject = (DSpaceObject) initializer.getImplementation();
        }
        return dSpaceObject;
    }

    private List<Subscription> findAllSubscriptionsBySubscriptionTypeAndFrequency(Context context,
             String subscriptionType, String frequency) {
        try {
            return subscribeService.findAllSubscriptionsBySubscriptionTypeAndFrequency(context, subscriptionType,
                                    frequency)
                                   .stream()
                                   .sorted(Comparator.comparing(s -> s.getePerson().getID()))
                                   .collect(Collectors.toList());
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return new ArrayList<Subscription>();
    }

    @SuppressWarnings("rawtypes")
    public SubscriptionEmailNotificationService(SubscribeService subscribeService,
                                                Map<String, SubscriptionGenerator> generators,
                                                Map<String, DSpaceObjectUpdates> contentUpdates) {
        this.subscribeService = subscribeService;
        this.generators = generators;
        this.contentUpdates = contentUpdates;
    }

}
