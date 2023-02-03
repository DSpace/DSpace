/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.subscriptions;

import static org.dspace.core.Constants.COLLECTION;
import static org.dspace.core.Constants.COMMUNITY;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.service.SubscribeService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.dspace.subscriptions.service.DSpaceObjectUpdates;
import org.dspace.subscriptions.service.SubscriptionGenerator;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link DSpaceRunnable} to find subscribed objects and send notification mails about them
 *
 * @author alba aliu
 */
public class SubscriptionEmailNotificationServiceImpl implements SubscriptionEmailNotificationService {

    private static final Logger log = LogManager.getLogger(SubscriptionEmailNotificationServiceImpl.class);

    private Map<String, DSpaceObjectUpdates> contentUpdates = new HashMap<>();
    @SuppressWarnings("rawtypes")
    private Map<String, SubscriptionGenerator> subscriptionType2generators = new HashMap<>();

    @Autowired
    private SubscribeService subscribeService;

    @SuppressWarnings("rawtypes")
    public SubscriptionEmailNotificationServiceImpl(Map<String, DSpaceObjectUpdates> contentUpdates,
                                                    Map<String, SubscriptionGenerator> subscriptionType2generators) {
        this.contentUpdates = contentUpdates;
        this.subscriptionType2generators = subscriptionType2generators;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void perform(Context context, DSpaceRunnableHandler handler, String subscriptionType, String frequency) {
        List<IndexableObject> communities = new ArrayList<>();
        List<IndexableObject> collections = new ArrayList<>();
        try {
            List<Subscription> subscriptions =
                               findAllSubscriptionsBySubscriptionTypeAndFrequency(context, subscriptionType, frequency);
            // Here is verified if SubscriptionType is "content" Or "statistics" as them are configured
            if (subscriptionType2generators.keySet().contains(subscriptionType)) {
                // the list of the person who has subscribed
                int iterator = 0;
                for (Subscription subscription : subscriptions) {
                    DSpaceObject dSpaceObject = subscription.getDSpaceObject();

                    if (dSpaceObject.getType() == COMMUNITY) {
                        communities.addAll(contentUpdates.get(Community.class.getSimpleName().toLowerCase())
                                                         .findUpdates(context, dSpaceObject, frequency));
                    } else if (dSpaceObject.getType() == COLLECTION) {
                        collections.addAll(contentUpdates.get(Collection.class.getSimpleName().toLowerCase())
                                                         .findUpdates(context, dSpaceObject, frequency));
                    } else {
                        log.warn("found an invalid DSpace Object type ({}) among subscriptions to send",
                                 dSpaceObject.getType());
                        continue;
                    }

                    var ePerson = subscription.getEPerson();
                    if (iterator < subscriptions.size() - 1) {
                        // as the subscriptions are ordered by eperson id, so we send them by ePerson
                        if (ePerson.equals(subscriptions.get(iterator + 1).getEPerson())) {
                            iterator++;
                            continue;
                        } else {
                            subscriptionType2generators.get(subscriptionType)
                                      .notifyForSubscriptions(context, ePerson, communities, collections);
                            communities.clear();
                            collections.clear();
                        }
                    } else {
                        //in the end of the iteration
                        subscriptionType2generators.get(subscriptionType)
                                        .notifyForSubscriptions(context, ePerson, communities, collections);
                    }
                    iterator++;
                }
            } else {
                throw new IllegalArgumentException("Currently this SubscriptionType:" + subscriptionType +
                                                   " is not supported!");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            handler.handleException(e);
            context.abort();
        }
    }

    /**
     * Return all Subscriptions by subscriptionType and frequency ordered by ePerson ID
     * if there are none it returns an empty list
     * 
     * @param context            DSpace context
     * @param subscriptionType   Could be "content" or "statistics". NOTE: in DSpace we have only "content"
     * @param frequency          Could be "D" stand for Day, "W" stand for Week, and "M" stand for Month
     * @return
     */
    private List<Subscription> findAllSubscriptionsBySubscriptionTypeAndFrequency(Context context,
             String subscriptionType, String frequency) {
        try {
            return subscribeService.findAllSubscriptionsBySubscriptionTypeAndFrequency(context, subscriptionType,
                                    frequency)
                                   .stream()
                                   .sorted(Comparator.comparing(s -> s.getEPerson().getID()))
                                   .collect(Collectors.toList());
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return new ArrayList<Subscription>();
    }

    @Override
    public Set<String> getSupportedSubscriptionTypes() {
        return subscriptionType2generators.keySet();
    }

}
