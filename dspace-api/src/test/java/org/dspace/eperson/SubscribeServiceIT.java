/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.eperson;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.dspace.builder.SubscribeBuilder.subscribeBuilder;
import static org.dspace.matcher.SubscribeMatcher.matches;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.SubscribeBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.eperson.service.SubscribeService;
import org.junit.Before;
import org.junit.Test;

public class SubscribeServiceIT extends AbstractIntegrationTestWithDatabase {

    private final SubscribeService subscribeService = ContentServiceFactory.getInstance().getSubscribeService();

    private Collection firstCollection;
    private Collection secondCollection;

    @Before
    public void init() throws Exception {
        context.turnOffAuthorisationSystem();
        Community parentCommunity = CommunityBuilder.createCommunity(context).build();
        firstCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("First Collection").build();
        secondCollection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Second Collection").build();
        context.restoreAuthSystemState();
    }

    @Test
    public void findAllWithoutAndWithLimit() throws Exception {

        String resourceType = "Collection";

        EPerson subscribingUser = context.getCurrentUser();

        createSubscription("content", firstCollection, subscribingUser, weekly());
        createSubscription("content", secondCollection, subscribingUser, daily(), annual());

        // unlimited search returns all subscriptions

        List<Subscription> subscriptions = subscribeService.findAll(context, resourceType, 10, 0);
        assertThat(subscriptions, containsInAnyOrder(
            asList(matches(firstCollection, subscribingUser, "content",
                           singletonList(weekly())),
                   matches(secondCollection, subscribingUser, "content",
                           asList(daily(), annual())))));

        // limited search returns first

        subscriptions = subscribeService.findAll(context, resourceType, 1, 0);

        assertThat(subscriptions, containsInAnyOrder(
            singletonList(matches(firstCollection, subscribingUser, "content",
                                  singletonList(weekly())))));

        // search with offset returns second

        subscriptions = subscribeService.findAll(context, resourceType, 100, 1);

        assertThat(subscriptions, containsInAnyOrder(
            singletonList(matches(secondCollection, subscribingUser, "content",
                                  asList(daily(), annual())))));

        // lookup without resource type
        subscriptions = subscribeService.findAll(context, StringUtils.EMPTY, 100, 0);

        assertThat(subscriptions, containsInAnyOrder(
            asList(matches(firstCollection, subscribingUser, "content",
                           singletonList(weekly())),
                   matches(secondCollection, subscribingUser, "content",
                           asList(daily(), annual())))));

    }

    private static SubscriptionParameter annual() {
        return createSubscriptionParameter("frequency", "A");
    }

    private static SubscriptionParameter daily() {
        return createSubscriptionParameter("frequency", "D");
    }

    @Test(expected = Exception.class)
    public void findAllWithInvalidResource() throws Exception {

        String resourceType = "INVALID";
        Integer limit = 10;
        Integer offset = 0;

        createSubscription("content", firstCollection, context.getCurrentUser(),
                           weekly());

        subscribeService.findAll(context, resourceType, limit, offset);

    }

    @Test
    public void newSubscriptionCreatedByAdmin() throws Exception {

        SubscriptionParameter monthly = createSubscriptionParameter("frequency", "M");

        List<SubscriptionParameter> parameters = Collections.singletonList(
            monthly);

        EPerson currentUser = context.getCurrentUser();
        context.setCurrentUser(admin);
        Subscription subscription = subscribeService.subscribe(context, eperson,
                                                               firstCollection, parameters, "content");

        assertThat(subscription, is(matches(firstCollection, eperson,
                                            "content", singletonList(monthly))));

        SubscribeBuilder.deleteSubscription(subscription.getID());
        context.setCurrentUser(currentUser);

    }

    @Test
    public void newSubscriptionCreatedByCurrentUser() throws Exception {

        EPerson currentUser = context.getCurrentUser();
        Subscription subscription = subscribeService.subscribe(context, currentUser,
                                                               secondCollection,
                                                               asList(daily(), weekly()), "content");

        assertThat(subscription, matches(secondCollection, currentUser, "content",
                                         asList(daily(), weekly())));

        SubscribeBuilder.deleteSubscription(subscription.getID());
    }

    @Test(expected = AuthorizeException.class)
    public void nonAdminDifferentUserTriesToSubscribe() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson notAdmin = EPersonBuilder.createEPerson(context).withEmail("not-admin@example.com").build();
        context.restoreAuthSystemState();
        EPerson currentUser = context.getCurrentUser();
        context.setCurrentUser(notAdmin);
        try {
            subscribeService.subscribe(context, admin, firstCollection,
                                       singletonList(
                                           daily()), "content");
        } finally {
            context.setCurrentUser(currentUser);
        }

    }

    @Test
    public void unsubscribeByAdmin() throws Exception {

        EPerson subscribingUser = context.getCurrentUser();
        createSubscription("content", secondCollection, subscribingUser,
                           weekly());

        List<Subscription> subscriptions =
            subscribeService.findSubscriptionsByEPersonAndDso(context, subscribingUser,
                                                              secondCollection, 100, 0);

        assertEquals(subscriptions.size(), 1);

        context.setCurrentUser(admin);
        subscribeService.unsubscribe(context, subscribingUser, secondCollection);
        context.setCurrentUser(subscribingUser);

        subscriptions =
            subscribeService.findSubscriptionsByEPersonAndDso(context, subscribingUser,
                                                              secondCollection, 100, 0);

        assertEquals(subscriptions.size(), 0);
    }

    @Test
    public void subscribingUserUnsubscribesTheirSubscription() throws Exception {

        EPerson subscribingUser = context.getCurrentUser();
        createSubscription("content", secondCollection, subscribingUser,
                           weekly());

        List<Subscription> subscriptions =
            subscribeService.findSubscriptionsByEPersonAndDso(context, subscribingUser,
                                                              secondCollection, 100, 0);

        assertEquals(subscriptions.size(), 1);


        subscribeService.unsubscribe(context, subscribingUser, secondCollection);

        subscriptions =
            subscribeService.findSubscriptionsByEPersonAndDso(context, subscribingUser,
                                                              secondCollection, 100, 0);

        assertEquals(subscriptions.size(), 0);
    }

    @Test(expected = AuthorizeException.class)
    public void nonAdminDifferentUserTriesToUnSubscribeAnotherUser() throws Exception {
        EPerson subscribingUser = context.getCurrentUser();
        Subscription subscription = createSubscription("content", secondCollection, subscribingUser,
                                                       weekly());

        context.turnOffAuthorisationSystem();
        EPerson nonAdmin = EPersonBuilder.createEPerson(context).build();
        context.restoreAuthSystemState();


        try {
            context.setCurrentUser(nonAdmin);
            subscribeService.unsubscribe(context, subscribingUser, secondCollection);
        } finally {
            context.setCurrentUser(subscribingUser);
            SubscribeBuilder.deleteSubscription(subscription.getID());
        }

    }

    @Test
    public void updateSubscription() throws Exception {

        EPerson currentUser = context.getCurrentUser();
        Subscription subscription = createSubscription("original",
                              firstCollection, currentUser,
                              createSubscriptionParameter("frequency", "M"));

        String updatedType = "updated";
        List<SubscriptionParameter> updatedParameters = Collections.singletonList(
            annual()
        );

        try {
            Subscription updated = subscribeService.updateSubscription(context, subscription.getID(),
                                                                       updatedType, updatedParameters);

            assertThat(updated, is(matches(firstCollection, currentUser, updatedType, updatedParameters)));

            List<Subscription> subscriptions =
                subscribeService.findSubscriptionsByEPersonAndDso(context, currentUser, firstCollection, 10, 0);

            assertThat(subscriptions, contains(
                matches(firstCollection, currentUser, updatedType, updatedParameters)));

        } finally {
            SubscribeBuilder.deleteSubscription(subscription.getID());
        }

    }

    @Test
    public void parametersAdditionAndRemoval() throws Exception {

        SubscriptionParameter firstParameter = createSubscriptionParameter("key1", "value1");
        SubscriptionParameter secondParameter = createSubscriptionParameter("key2", "value2");

        EPerson currentUser = context.getCurrentUser();
        Subscription subscription = createSubscription("type", secondCollection, currentUser,
                                                               firstParameter, secondParameter);
        int subscriptionId = subscription.getID();

        SubscriptionParameter addedParameter = createSubscriptionParameter("added", "add");


        try {
            Subscription updatedSubscription = subscribeService.addSubscriptionParameter(context, subscriptionId,
                                                                                         addedParameter);
            assertThat(updatedSubscription, is(matches(secondCollection, currentUser, "type",
                                                       asList(firstParameter, secondParameter, addedParameter))));
            updatedSubscription = subscribeService.removeSubscriptionParameter(context, subscriptionId,
                                                                               secondParameter);
            assertThat(updatedSubscription, is(matches(secondCollection, currentUser, "type",
                                                       asList(firstParameter, addedParameter))));
        } finally {
            SubscribeBuilder.deleteSubscription(subscriptionId);
        }
    }

    @Test
    public void findersAndDeletionsTest() throws SQLException {
        // method to test all find and delete methods exposed by SubscribeService
        context.turnOffAuthorisationSystem();
        EPerson firstSubscriber = EPersonBuilder.createEPerson(context).withEmail("first-user@example.com").build();
        EPerson secondSubscriber = EPersonBuilder.createEPerson(context).withEmail("second-user@example.com").build();
        EPerson thirdSubscriber = EPersonBuilder.createEPerson(context).withEmail("third-user@example.com").build();
        context.restoreAuthSystemState();

        EPerson currentUser = context.getCurrentUser();
        try {
            context.setCurrentUser(firstSubscriber);
            createSubscription("type1", firstCollection, firstSubscriber, daily(),
                               weekly());
            createSubscription("type1", secondCollection, firstSubscriber,
                               daily(),
                               annual());
            createSubscription("type2", secondCollection, firstSubscriber,
                               daily());

            context.setCurrentUser(secondSubscriber);
            createSubscription("type1", firstCollection, secondSubscriber,
                               daily());
            createSubscription("type1", secondCollection, secondSubscriber,
                               daily(),
                               annual());

            context.setCurrentUser(thirdSubscriber);
            createSubscription("type1", firstCollection, thirdSubscriber, daily());
            createSubscription("type1", secondCollection, thirdSubscriber,
                               daily(),
                               annual());

        } finally {
            context.setCurrentUser(currentUser);
        }

        List<Subscription> firstUserSubscriptions =
            subscribeService.findSubscriptionsByEPerson(context, firstSubscriber, 100, 0);

        assertThat(firstUserSubscriptions, containsInAnyOrder(
            matches(firstCollection, firstSubscriber, "type1", asList(daily(),
                                                                      weekly())),
            matches(secondCollection, firstSubscriber, "type1", asList(daily(),
                                                                       annual())),
            matches(secondCollection, firstSubscriber, "type2", singletonList(
                daily()))
        ));

        List<Subscription> firstUserSubscriptionsLimited =
            subscribeService.findSubscriptionsByEPerson(context, firstSubscriber, 1, 0);

        assertThat(firstUserSubscriptionsLimited.size(), is(1));

        List<Subscription> firstUserSubscriptionsWithOffset =
            subscribeService.findSubscriptionsByEPerson(context, firstSubscriber, 100, 1);

        assertThat(firstUserSubscriptionsWithOffset.size(), is(2));

        subscribeService.deleteByEPerson(context, firstSubscriber);
        assertThat(subscribeService.findSubscriptionsByEPerson(context, firstSubscriber, 100, 0),
                   is(List.of()));

        List<Subscription> secondSubscriberSecondCollectionSubscriptions =
            subscribeService.findSubscriptionsByEPersonAndDso(context, secondSubscriber, firstCollection, 10, 0);

        assertThat(secondSubscriberSecondCollectionSubscriptions, contains(
            matches(firstCollection, secondSubscriber, "type1", singletonList(daily()))
        ));

        List<Subscription> byTypeAndFrequency =
            subscribeService.findAllSubscriptionsBySubscriptionTypeAndFrequency(context, "type1",
                                                                                "D");
        assertThat(byTypeAndFrequency, containsInAnyOrder(
            matches(firstCollection, secondSubscriber, "type1", singletonList(
                daily())),
            matches(secondCollection, secondSubscriber, "type1", asList(daily(),
                                                                        annual())),
            matches(firstCollection, thirdSubscriber, "type1", singletonList(
                daily())),
            matches(secondCollection, thirdSubscriber, "type1", asList(daily(),
                                                                       annual()))
            ));

        assertThat(subscribeService.countAll(context), is(4L));
        assertThat(subscribeService.countByEPersonAndDSO(context, secondSubscriber, secondCollection), is(1L));
        assertThat(subscribeService.countSubscriptionsByEPerson(context, thirdSubscriber), is(2L));


    }

    private static SubscriptionParameter weekly() {
        return createSubscriptionParameter("frequency", "W");
    }

    private Subscription createSubscription(String type, DSpaceObject dso, EPerson ePerson,
                                            SubscriptionParameter... parameters) {
        return subscribeBuilder(context, type,
                                dso, ePerson,
                                Arrays.stream(parameters).collect(Collectors.toList())).build();
    }


    private static SubscriptionParameter createSubscriptionParameter(String name, String value) {
        SubscriptionParameter parameter = new SubscriptionParameter();
        parameter.setName(name);
        parameter.setValue(value);
        return parameter;
    }

}