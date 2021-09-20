/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang3.time.DateUtils;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.metrics.service.CrisMetricsService;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.CrisMetricsBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.SubscribeBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.configuration.DiscoveryConfigurationService;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.SubscriptionParameter;
import org.dspace.eperson.service.SubscribeService;
import org.dspace.services.ConfigurationService;
import org.dspace.subscriptions.ContentGenerator;
import org.dspace.subscriptions.StatisticsGenerator;
import org.dspace.subscriptions.SubscriptionEmailNotification;
import org.dspace.subscriptions.SubscriptionEmailNotificationService;
import org.dspace.subscriptions.dSpaceObjectsUpdates.CollectionsUpdates;
import org.dspace.subscriptions.dSpaceObjectsUpdates.CommunityUpdates;
import org.dspace.subscriptions.dSpaceObjectsUpdates.ItemsUpdates;
import org.dspace.subscriptions.service.DSpaceObjectUpdates;
import org.dspace.subscriptions.service.SubscriptionGenerator;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test for script of sending subscriptions for items/collections/communities
 *  @author Alba Aliu (alba.aliu at atis.al)
 */

public class SubscriptionsSendEmailNotificationIT extends AbstractControllerIntegrationTest {
    private SubscriptionEmailNotificationService subscriptionEmailNotificationService;
    @Autowired
    ConfigurationService configurationService;
    @Autowired
    SubscribeService subscribeService;
    @Autowired
    CrisMetricsService crisMetricsService;
    @Autowired
    CollectionService collectionService;
    @Autowired
    CommunityService communityService;
    @Autowired
    ItemService itemService;
    @Autowired
    SearchService searchService;
    @Autowired
    DiscoveryConfigurationService discoveryConfigurationService;
    StatisticsGenerator statisticsGenerator = mock(StatisticsGenerator.class);
    ContentGenerator contentGenerator = mock(ContentGenerator.class);
    ItemsUpdates itemsUpdates;
    CollectionsUpdates collectionsUpdates;
    CommunityUpdates communityUpdates;
    SubscriptionEmailNotification subscriptionEmailNotification;

    @Captor
    private ArgumentCaptor<EPerson> personArgumentCaptor;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        itemsUpdates = new ItemsUpdates(collectionService, communityService,
            itemService, discoveryConfigurationService, searchService);
        collectionsUpdates = new CollectionsUpdates(searchService);
        communityUpdates = new CommunityUpdates(searchService);
        itemsUpdates = new ItemsUpdates(collectionService, communityService, itemService,
            discoveryConfigurationService, searchService);
        Map<String, SubscriptionGenerator> generatorMap = new HashMap<>();
        generatorMap.put("content", contentGenerator);
        generatorMap.put("statistics", statisticsGenerator);
        Map<String, DSpaceObjectUpdates> contentUpdateMap = new HashMap<>();
        contentUpdateMap.put("community", communityUpdates);
        contentUpdateMap.put("collection", collectionsUpdates);
        contentUpdateMap.put("item", itemsUpdates);
        // Explicitly use solr commit in SolrLoggerServiceImpl#postView
        configurationService.setProperty("solr-statistics.autoCommit", false);
        this.subscriptionEmailNotificationService = new SubscriptionEmailNotificationService(
            crisMetricsService, subscribeService, generatorMap, contentUpdateMap);
        subscriptionEmailNotification = new SubscriptionEmailNotification();
    }


    //verify that method that invokes mail send is called correctly for type content and frequence weekly
    @Test
    public void sendSubscriptionMailTypeContentWeekly() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        parentCommunity = CommunityBuilder.createSubCommunity(context, community).build();
        Collection col1 = CollectionBuilder.createCollection(context, community).build();
        Collection col2 = CollectionBuilder.createCollection(context, community).build();
        Item orgUnit = ItemBuilder.createItem(context, col1)
                .withEntityType("OrgUnit").withFullName("4Science")
                .withTitle("4Science").buildWithLastModifiedDate( generateTimeOnBasedFrequency("W"));
        //person item for relation inverse
        //it has as affiliation 4Science
        Item person = ItemBuilder.createItem(context, col2)
                .withEntityType("Person").withFullName("testPerson")
                .withTitle("testPerson")
                .withAffiliation(orgUnit.getName(),
                    orgUnit.getID().toString())
            .buildWithLastModifiedDate(generateTimeOnBasedFrequency("W"));
        // subscription with dso of type item
        Subscription subscription = SubscribeBuilder
            .subscribeBuilder(context, "content", orgUnit, eperson,
                generateSubscriptionParameterListFrequency("W")).build();
        Subscription subscriptionComm = SubscribeBuilder
            .subscribeBuilder(context, "content", community,
                eperson, generateSubscriptionParameterListFrequency("D")).build();
        Subscription subscriptionColl = SubscribeBuilder
            .subscribeBuilder(context, "content", col1, eperson,
                generateSubscriptionParameterListFrequency("W")).build();
        context.restoreAuthSystemState();
        String[] args = new String[]{"subscription-send", "-t", "content", "-f", "W"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        subscriptionEmailNotification.initialize(args, handler, eperson);
        // attach service class
        subscriptionEmailNotification.setSubscriptionEmailNotificationService(subscriptionEmailNotificationService);
        //run script
        subscriptionEmailNotification.run();
        List<IndexableObject> items = new ArrayList<>();
        IndexableItem indexableObject = new IndexableItem(person);
        items.add(indexableObject);

        List<IndexableObject> collections = new ArrayList<>();
        IndexableItem indexableObject1 = new IndexableItem(orgUnit);
        collections.add(indexableObject1);
        // verify that method in invoked correctly
        verify(contentGenerator).notifyForSubscriptions(
            subscriptionEmailNotification.getContext(), eperson, new ArrayList<>(),
            collections, items);
        verifyNoMoreInteractions(contentGenerator);
    }


    //verify that method that invokes mail send is called correctly for type content and frequence weekly
    @Test
    public void sendSubscriptionMailTypeContentNoSubscription() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        parentCommunity = CommunityBuilder.createSubCommunity(context, community).build();
        Collection col1 = CollectionBuilder.createCollection(context, community).build();
        Collection col2 = CollectionBuilder.createCollection(context, community).build();
        Item orgUnit = ItemBuilder.createItem(context, col1)
                .withEntityType("OrgUnit").withFullName("4Science")
                .withTitle("4Science").buildWithLastModifiedDate(generateTimeOnBasedFrequency("W"));
        //person item for relation inverse
        //it has as affiliation 4Science
        Item person = ItemBuilder.createItem(context, col2)
                .withEntityType("Person").withFullName("testPerson")
                .withTitle("testPerson")
                .withAffiliation(orgUnit.getName(),
                    orgUnit.getID().toString())
            .buildWithLastModifiedDate(generateTimeOnBasedFrequency("W"));
        // subscription with dso of type item
        Subscription subscription = SubscribeBuilder
            .subscribeBuilder(context, "statistics",
                orgUnit, eperson, generateSubscriptionParameterListFrequency("W")).build();
        Subscription subscriptionComm = SubscribeBuilder
            .subscribeBuilder(context, "statistics", community,
                eperson, generateSubscriptionParameterListFrequency("D")).build();
        Subscription subscriptionColl = SubscribeBuilder
            .subscribeBuilder(context, "statistics", col1, eperson,
                generateSubscriptionParameterListFrequency("W")).build();
        context.restoreAuthSystemState();
        String[] args = new String[]{"subscription-send", "-t", "content", "-f", "W"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        subscriptionEmailNotification.initialize(args, handler, eperson);
        // attach service class
        subscriptionEmailNotification.setSubscriptionEmailNotificationService(subscriptionEmailNotificationService);
        //run script
        subscriptionEmailNotification.run();
        verifyZeroInteractions(contentGenerator);
        verifyZeroInteractions(statisticsGenerator);
    }

    //verify that method that invokes mail send is called correctly for type
    // content and frequence weekly for two different users
    @Test
    public void sendSubscriptionMailTypeContentWeeklyForTwoPersons() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        parentCommunity = CommunityBuilder.createSubCommunity(context, community).build();
        Collection col1 = CollectionBuilder.createCollection(context, community).build();
        Collection col2 = CollectionBuilder.createCollection(context, community).build();
        Item orgUnit = ItemBuilder.createItem(context, col1)
                .withEntityType("OrgUnit").withFullName("4Science")
                .withTitle("4Science").buildWithLastModifiedDate(generateTimeOnBasedFrequency("W"));
        //person item for relation inverse
        //it has as affiliation 4Science
        Item person = ItemBuilder.createItem(context, col2)
                .withEntityType("Person").withFullName("testPerson")
                .withTitle("testPerson")
                .withAffiliation(orgUnit.getName(),
                    orgUnit.getID().toString()).buildWithLastModifiedDate(
                        generateTimeOnBasedFrequency("W"));
        // subscription with dso of type item
        Subscription subscription = SubscribeBuilder
            .subscribeBuilder(context, "content", orgUnit,
                admin, generateSubscriptionParameterListFrequency("W")).build();
        Subscription subscriptionComm = SubscribeBuilder
            .subscribeBuilder(context, "content", community, eperson,
                generateSubscriptionParameterListFrequency("D")).build();
        Subscription subscriptionColl = SubscribeBuilder
            .subscribeBuilder(context, "content", orgUnit, eperson,
                generateSubscriptionParameterListFrequency("W")).build();
        context.restoreAuthSystemState();
        String[] args = new String[]{"subscription-send", "-t", "content", "-f", "W"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        subscriptionEmailNotification.initialize(args, handler, eperson);
        // attach service class
        subscriptionEmailNotification.setSubscriptionEmailNotificationService(subscriptionEmailNotificationService);
        //run script
        subscriptionEmailNotification.run();
        List<IndexableObject> items = new ArrayList<>();
        IndexableItem indexableObject = new IndexableItem(person);
        items.add(indexableObject);
        // verify that method is invoked twice for different users
        verify(contentGenerator,
            times(2))
            .notifyForSubscriptions(
                eq(subscriptionEmailNotification.getContext()),
                personArgumentCaptor.capture(), eq(new ArrayList<>()),
                eq(new ArrayList<>()), eq(items));
        List<EPerson> allValues = personArgumentCaptor.getAllValues();
        List<EPerson> personList = new ArrayList<>();
        personList.add(admin);
        personList.add(eperson);
        Matchers.containsInAnyOrder(allValues, personList);
    }


    //verify that method that invokes mail send is called correctly for type content and frequence weekly
    @Test
    public void sendSubscriptionMailTypeContentMonthly() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        parentCommunity = CommunityBuilder.createSubCommunity(context, community).build();
        Collection col1 = CollectionBuilder.createCollection(context, community).build();
        Collection col2 = CollectionBuilder.createCollection(context, community).build();
        Collection col3 = CollectionBuilder.createCollection(context, community).build();
        Item orgUnit = ItemBuilder.createItem(context, col1)
                .withEntityType("OrgUnit").withFullName("orgUnit")
                .withTitle("orgUnit").buildWithLastModifiedDate(generateTimeOnBasedFrequency("M"));
        //person item for relation inverse
        //it has as affiliation 4Science
        Item person = ItemBuilder.createItem(context, col3)
                .withEntityType("Person").withFullName("personTest")
                .withTitle("personTest")
                .withAffiliation(orgUnit.getName(),
                    orgUnit.getID().toString())
            .buildWithLastModifiedDate(generateTimeOnBasedFrequency("M"));

        Item itemOfComm = ItemBuilder.createItem(context, col2)
                .withEntityType("Equipment").withFullName("testEquipment")
                .withTitle("testEquipment")
                .buildWithLastModifiedDate(generateTimeOnBasedFrequency("M"));
        // subscription with dso of type item
        Subscription subscription = SubscribeBuilder
            .subscribeBuilder(context, "content", orgUnit, eperson,
                generateSubscriptionParameterListFrequency("M")).build();
        Subscription subscriptionComm = SubscribeBuilder
            .subscribeBuilder(context, "content",
                community, eperson, generateSubscriptionParameterListFrequency("D")).build();
        Subscription subscriptionColl = SubscribeBuilder
            .subscribeBuilder(context, "content", col1, eperson,
                generateSubscriptionParameterListFrequency("M")).build();
        context.restoreAuthSystemState();
        String[] args = new String[]{"subscription-send", "-t", "content", "-f", "M"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        subscriptionEmailNotification.initialize(args, handler, eperson);
        // attach service class
        subscriptionEmailNotification.setSubscriptionEmailNotificationService(subscriptionEmailNotificationService);
        //run script
        subscriptionEmailNotification.run();
        List<IndexableObject> items = new ArrayList<>();
        IndexableItem indexableObject = new IndexableItem(person);
        items.add(indexableObject);

        List<IndexableObject> collections = new ArrayList<>();
        IndexableItem indexableObject1 = new IndexableItem(orgUnit);
        collections.add(indexableObject1);

        List<IndexableObject> communities = new ArrayList<>();

        // verify that method in invoked correctly
        verify(contentGenerator)
            .notifyForSubscriptions(subscriptionEmailNotification.getContext(),
                eperson, communities, collections, items);
        verifyNoMoreInteractions(contentGenerator);
    }


    //verify that method that invokes mail send is called correctly for type content and frequence weekly
    @Test
    public void sendSubscriptionMailTypeContentDaily() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        parentCommunity = CommunityBuilder.createSubCommunity(context, community).build();
        Collection col1 = CollectionBuilder.createCollection(context, community).build();
        Collection col2 = CollectionBuilder.createCollection(context, community).build();
        Item orgUnit = ItemBuilder.createItem(context, col1)
                .withEntityType("OrgUnit").withFullName("orgUnit")
                .withTitle("orgUnit").buildWithLastModifiedDate(generateTimeOnBasedFrequency("D"));
        //person item for relation inverse
        //it has as affiliation 4Science
        Item person = ItemBuilder.createItem(context, col2)
                .withEntityType("Person").withFullName("person")
                .withTitle("person")
                .withAffiliation(orgUnit.getName(),
                    orgUnit.getID().toString())
            .buildWithLastModifiedDate(generateTimeOnBasedFrequency("D"));
        // subscription with dso of type item
        Subscription subscription = SubscribeBuilder
            .subscribeBuilder(context, "content", orgUnit,
                eperson, generateSubscriptionParameterListFrequency("D")).build();
        Subscription subscriptionComm = SubscribeBuilder
            .subscribeBuilder(context, "content", community,
                eperson, generateSubscriptionParameterListFrequency("W")).build();
        Subscription subscriptionColl = SubscribeBuilder
            .subscribeBuilder(context, "content", col1, eperson,
                generateSubscriptionParameterListFrequency("D")).build();
        context.restoreAuthSystemState();
        String[] args = new String[]{"subscription-send", "-t", "content", "-f", "D"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        subscriptionEmailNotification.initialize(args, handler, eperson);
        // attach service class
        subscriptionEmailNotification.setSubscriptionEmailNotificationService(subscriptionEmailNotificationService);
        //run script
        subscriptionEmailNotification.run();
        List<IndexableObject> items = new ArrayList<>();
        IndexableItem indexableObject = new IndexableItem(person);
        items.add(indexableObject);
        List<IndexableObject> collections = new ArrayList<>();
        IndexableItem indexableObject1 = new IndexableItem(orgUnit);
        collections.add(indexableObject1);
        // verify that method in invoked correctly
        verify(contentGenerator)
            .notifyForSubscriptions(subscriptionEmailNotification.getContext(),
                eperson, new ArrayList<>(), collections, items);
        verifyNoMoreInteractions(contentGenerator);

    }

    //verify that method that invokes mail send is called correctly for type statistics and frequence monthly
    @Test
    public void sendSubscriptionMailTypeStatisticsWeekly() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        parentCommunity = CommunityBuilder.createSubCommunity(context, community).build();
        Collection col1 = CollectionBuilder.createCollection(context, community).build();
        Item orgUnit = ItemBuilder.createItem(context, col1)
                .withEntityType("OrgUnit").withFullName("4Science")
                .withTitle("4Science").buildWithLastModifiedDate(generateTimeOnBasedFrequency("D"));
        //person item for relation inverse
        //it has as affiliation 4Science
        Item person = ItemBuilder.createItem(context, col1)
                .withEntityType("Person").withFullName("testPerson")
                .withTitle("testPerson")
                .withAffiliation(orgUnit.getName(),
                    orgUnit.getID().toString())
            .buildWithLastModifiedDate(generateTimeOnBasedFrequency("D"));
        // subscription with dso of type item
        Subscription subscription = SubscribeBuilder
            .subscribeBuilder(context, "statistics",
                orgUnit, eperson, generateSubscriptionParameterListFrequency("D")).build();
        Subscription subscriptionComm = SubscribeBuilder
            .subscribeBuilder(context, "statistics", community, eperson,
                generateSubscriptionParameterListFrequency("D")).build();
        Subscription subscriptionColl = SubscribeBuilder
            .subscribeBuilder(context, "statistics", col1, eperson,
                generateSubscriptionParameterListFrequency("D")).build();
        //create cris metrics related with dso
        CrisMetrics crisMetricsComm = CrisMetricsBuilder.createCrisMetrics(context, community)
                .withMetricType("view")
                .withMetricCount(2)
                .withAcquisitionDate(
                        DateUtils.addDays(new Date(), -7))
                .isLast(true).build();
        CrisMetrics crisMetricsColl = CrisMetricsBuilder.createCrisMetrics(context, col1)
                .withMetricType("view")
                .withMetricCount(2)
                .withAcquisitionDate(
                        DateUtils.addDays(new Date(), -7))
                .isLast(true).build();
        CrisMetrics crisMetricsItem = CrisMetricsBuilder.createCrisMetrics(context, orgUnit)
                .withMetricType("view")
                .withMetricCount(2)
                .withAcquisitionDate(
                        DateUtils.addDays(new Date(), -7))
                .isLast(true).build();
        // THIS MUST NOT BE SHOWN IN EMAIL
        CrisMetrics crisMetricsTest = CrisMetricsBuilder.createCrisMetrics(context, person)
                .withMetricType("view")
                .withMetricCount(2)
                .withAcquisitionDate(
                        DateUtils.addDays(new Date(), -7))
                .isLast(true).build();
        context.restoreAuthSystemState();
        String[] args = new String[]{"subscription-send", "-t", "statistics", "-f", "D"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        subscriptionEmailNotification.initialize(args, handler, eperson);
        // attach service class
        subscriptionEmailNotification.setSubscriptionEmailNotificationService(subscriptionEmailNotificationService);
        //run script
        subscriptionEmailNotification.run();
        List<CrisMetrics> crisMetrics = new ArrayList<>();
        crisMetrics.add(crisMetricsItem);
        crisMetrics.add(crisMetricsComm);
        crisMetrics.add(crisMetricsColl);
        // verify that method in invoked correctly
        verify(statisticsGenerator)
            .notifyForSubscriptions(subscriptionEmailNotification.getContext(),
                eperson, crisMetrics, null, null);
    }

    //verify that method that invokes mail send is called correctly for type statistics and frequence monthly
    @Test
    public void sendSubscriptionMailTypeStatisticsMonthly() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        parentCommunity = CommunityBuilder.createSubCommunity(context, community).build();
        Collection col1 = CollectionBuilder.createCollection(context, community).build();
        Item orgUnit = ItemBuilder.createItem(context, col1)
                .withEntityType("OrgUnit").withFullName("4Science")
                .withTitle("4Science").buildWithLastModifiedDate(generateTimeOnBasedFrequency("M"));
        //person item for relation inverse
        //it has as affiliation 4Science
        Item person = ItemBuilder.createItem(context, col1)
                .withEntityType("Person").withFullName("testPerson")
                .withTitle("testPerson")
                .withAffiliation(orgUnit.getName(),
                    orgUnit.getID().toString()).buildWithLastModifiedDate(generateTimeOnBasedFrequency("M"));
        // subscription with dso of type item
        Subscription subscription = SubscribeBuilder
            .subscribeBuilder(context, "statistics",
                orgUnit, eperson, generateSubscriptionParameterListFrequency("M")).build();
        Subscription subscriptionComm = SubscribeBuilder
            .subscribeBuilder(context, "statistics", community,
                eperson, generateSubscriptionParameterListFrequency("D")).build();
        Subscription subscriptionColl = SubscribeBuilder
            .subscribeBuilder(context, "statistics", col1, eperson,
                generateSubscriptionParameterListFrequency("D")).build();
        //create cris metrics related with dso
        CrisMetrics crisMetricsComm = CrisMetricsBuilder.createCrisMetrics(context, community)
                .withMetricType("view")
                .withMetricCount(2)
                .withAcquisitionDate(
                        DateUtils.addDays(new Date(), -7))
                .isLast(true).build();
        CrisMetrics crisMetricsColl = CrisMetricsBuilder.createCrisMetrics(context, col1)
                .withMetricType("view")
                .withMetricCount(2)
                .withAcquisitionDate(
                        DateUtils.addDays(new Date(), -7))
                .isLast(true).build();
        CrisMetrics crisMetricsItem = CrisMetricsBuilder.createCrisMetrics(context, orgUnit)
                .withMetricType("view")
                .withMetricCount(2)
                .withAcquisitionDate(
                        DateUtils.addDays(new Date(), -7))
                .isLast(true).build();
        // THIS MUST NOT BE SHOWN IN EMAIL
        CrisMetrics crisMetricsTest = CrisMetricsBuilder.createCrisMetrics(context, person)
                .withMetricType("view")
                .withMetricCount(2)
                .withAcquisitionDate(
                        DateUtils.addDays(new Date(), -7))
                .isLast(true).build();
        context.restoreAuthSystemState();
        String[] args = new String[]{"subscription-send", "-t", "statistics", "-f", "M"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        subscriptionEmailNotification.initialize(args, handler, eperson);
        // attach service class
        subscriptionEmailNotification.setSubscriptionEmailNotificationService(subscriptionEmailNotificationService);
        //run script
        subscriptionEmailNotification.run();
        List<CrisMetrics> crisMetrics = new ArrayList<>();
        crisMetrics.add(crisMetricsItem);
        // verify that method in invoked correctly
        verify(statisticsGenerator)
            .notifyForSubscriptions(subscriptionEmailNotification.getContext(), eperson, crisMetrics, null, null);
    }

    //verify that method that invokes mail send is called correctly for type statistics and frequence monthly
    @Test
    public void sendSubscriptionMailTypeStatisticsDaily() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        parentCommunity = CommunityBuilder.createSubCommunity(context, community).build();
        Collection col1 = CollectionBuilder.createCollection(context, community).build();
        Item orgUnit = ItemBuilder.createItem(context, col1)
                .withEntityType("OrgUnit").withFullName("4Science")
                .withTitle("4Science").buildWithLastModifiedDate(generateTimeOnBasedFrequency("D"));
        //person item for relation inverse
        //it has as affiliation 4Science
        Item person = ItemBuilder.createItem(context, col1)
                .withEntityType("Person").withFullName("testPerson")
                .withTitle("testPerson")
                .withAffiliation(orgUnit.getName(),
                    orgUnit.getID().toString()).buildWithLastModifiedDate(generateTimeOnBasedFrequency("D"));
        // subscription with dso of type item
        Subscription subscription = SubscribeBuilder
            .subscribeBuilder(context, "statistics", orgUnit,
                eperson, generateSubscriptionParameterListFrequency("D")).build();
        Subscription subscriptionComm = SubscribeBuilder
            .subscribeBuilder(context, "statistics", community,
                eperson, generateSubscriptionParameterListFrequency("D")).build();
        Subscription subscriptionColl = SubscribeBuilder
            .subscribeBuilder(context, "statistics", col1, eperson,
                generateSubscriptionParameterListFrequency("D")).build();
        //create cris metrics related with dso
        CrisMetrics crisMetricsComm = CrisMetricsBuilder.createCrisMetrics(context, community)
                .withMetricType("view")
                .withMetricCount(2)
                .withAcquisitionDate(
                        DateUtils.addDays(new Date(), -7))
                .isLast(true).build();
        CrisMetrics crisMetricsColl = CrisMetricsBuilder.createCrisMetrics(context, col1)
                .withMetricType("view")
                .withMetricCount(2)
                .withAcquisitionDate(
                        DateUtils.addDays(new Date(), -7))
                .isLast(true).build();
        CrisMetrics crisMetricsItem = CrisMetricsBuilder.createCrisMetrics(context, orgUnit)
                .withMetricType("view")
                .withMetricCount(2)
                .withAcquisitionDate(
                        DateUtils.addDays(new Date(), -7))
                .isLast(true).build();
        // THIS MUST NOT BE SHOWN IN EMAIL
        CrisMetrics crisMetricsTest = CrisMetricsBuilder.createCrisMetrics(context, person)
                .withMetricType("view")
                .withMetricCount(2)
                .withAcquisitionDate(
                        DateUtils.addDays(new Date(), -7))
                .isLast(true).build();
        context.restoreAuthSystemState();
        String[] args = new String[]{"subscription-send", "-t", "statistics", "-f", "D"};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        subscriptionEmailNotification.initialize(args, handler, eperson);
        // attach service class
        subscriptionEmailNotification.setSubscriptionEmailNotificationService(subscriptionEmailNotificationService);
        //run script
        subscriptionEmailNotification.run();
        List<CrisMetrics> crisMetrics = new ArrayList<>();
        crisMetrics.add(crisMetricsItem);
        crisMetrics.add(crisMetricsComm);
        crisMetrics.add(crisMetricsColl);
        // verify that method in invoked correctly
        verify(statisticsGenerator)
            .notifyForSubscriptions(subscriptionEmailNotification.getContext(), eperson, crisMetrics, null, null);
    }

    private List<SubscriptionParameter> generateSubscriptionParameterListFrequency(String frequencyValue) {
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue(frequencyValue);
        subscriptionParameterList.add(subscriptionParameter);
        return subscriptionParameterList;
    }

    private Date generateTimeOnBasedFrequency(String frequency) {
        GregorianCalendar localCalendar = new GregorianCalendar();
        localCalendar.setTime(new Date());
        TimeZone utcZone = TimeZone.getTimeZone("UTC");
        localCalendar.setTimeZone(utcZone);
        // Now set the UTC equivalent.
        switch (frequency) {
            case "D":
                localCalendar.add(GregorianCalendar.DAY_OF_YEAR, -1);
                break;
            case "M":
                localCalendar.add(GregorianCalendar.MONTH, -1);
                break;
            case "W":
                localCalendar.add(GregorianCalendar.WEEK_OF_MONTH, -1);
                break;
            default:
                return null;
        }
        return localCalendar.getTime();
    }

}

