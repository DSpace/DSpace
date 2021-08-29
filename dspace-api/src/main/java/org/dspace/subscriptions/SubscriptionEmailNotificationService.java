/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.subscriptions;


import org.apache.commons.collections.ListUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.metrics.service.CrisMetricsService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.service.SubscribeService;
import org.dspace.metrics.UpdateCrisMetricsInSolrDocService;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.dspace.subscriptions.service.DSpaceObjectUpdates;
import org.dspace.subscriptions.service.SubscriptionGenerator;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * Implementation of {@link DSpaceRunnable} to find subscribed objects and send notification mails about them
 *
 * @author alba aliu
 */
public class SubscriptionEmailNotificationService {
    private CrisMetricsService crisMetricsService;
    private static final Logger log = LogManager.getLogger(SubscriptionEmailNotification.class);
    private UpdateCrisMetricsInSolrDocService updateCrisMetricsInSolrDocService;
    @Autowired
    private SubscribeService subscribeService;
    @Resource(name = "contentUpdates")
    private final Map<String, DSpaceObjectUpdates> contentUpdates = new HashMap<>();
    @Resource(name = "generators")
    private final Map<String, SubscriptionGenerator> generators = new HashMap<>();
    private List<IndexableObject> communitiesUpdates = new ArrayList<>();
    private List<IndexableObject> collectionUpdates = new ArrayList<>();
    private List<IndexableObject> itemUpdates = new ArrayList<>();

    // TODO REFACTOR THIS
    public void perform(Context context, DSpaceRunnableHandler handler, String type, String frequency) {
        try {
            context.turnOffAuthorisationSystem();
            List<Subscription> subscriptionList = findAllSubscriptionsByTypeAndFrequency(context, type, frequency);
            List<EPerson> ePersonList = new ArrayList<>();
            // if content subscription
            if (type.equals("content")) {
                // the list of the person who has subscribed
                int iterator = 0;
                for (Subscription subscription : subscriptionList) {
                    HibernateProxy hibernateProxy = (HibernateProxy) subscription.getdSpaceObject();
                    LazyInitializer initializer = hibernateProxy.getHibernateLazyInitializer();
                    if (initializer.getImplementation() instanceof Community) {
                        communitiesUpdates.addAll(contentUpdates.get("community").findUpdates(context, subscription.getdSpaceObject(), frequency));
                    } else if (initializer.getImplementation() instanceof Collection) {
                        collectionUpdates.addAll(contentUpdates.get("collection").findUpdates(context, subscription.getdSpaceObject(), frequency));
                    } else if (initializer.getImplementation() instanceof Item) {
                        itemUpdates.addAll(contentUpdates.get("item").findUpdates(context, subscription.getdSpaceObject(), frequency));
                    }
                    if (iterator == 0) {
                        ePersonList.add(subscription.getePerson());
                    } else {
                        if (!ePersonList.get(ePersonList.size() - 1).equals(subscription.getePerson())) {
                            ePersonList.add(subscription.getePerson());
                        }
                    }
                    iterator++;
                }
                List listmerged = ListUtils.union(communitiesUpdates, collectionUpdates);
                List listmergedFinal = ListUtils.union(listmerged, itemUpdates);
                for (EPerson ePerson : ePersonList) {
                    // generate mail for type content
                    generators.get(type).notifyForSubscriptions(context, ePerson, listmergedFinal);
                }
            } else {
                int iterator = 0;
                List<CrisMetrics> crisMetricsList = new ArrayList<>();
                for (Subscription subscription : subscriptionList) {
                    try {
                        crisMetricsList.addAll(crisMetricsService.findAllByItem(context, (Item) subscription.getdSpaceObject()));
                    } catch (Exception e) {
                        log.error(e.getMessage());
                    }
                    if (iterator == 0) {
                        ePersonList.add(subscription.getePerson());
                    } else {
                        if (!ePersonList.get(ePersonList.size() - 1).equals(subscription.getePerson())) {
                            ePersonList.add(subscription.getePerson());
                        }
                    }
                    iterator++;
                }
                for (EPerson ePerson : ePersonList) {
                    generators.get(type).notifyForSubscriptions(context, ePerson, crisMetricsList);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            handler.handleException(e);
            context.abort();
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private List<Subscription> findAllSubscriptionsByTypeAndFrequency(Context context, String type, String frequency) {
        try {
            return this.subscribeService.findAllSubscriptionsByTypeAndFrequency(context, type, frequency);
        } catch (SQLException sqlException) {
            log.error(sqlException.getMessage());
        }
        return null;
    }

    //    public SubscriptionEmailNotificationService(LinkedHashMap<String, ContentGenerator> generators) {
//        System.out.println(generators);
//    }
}
