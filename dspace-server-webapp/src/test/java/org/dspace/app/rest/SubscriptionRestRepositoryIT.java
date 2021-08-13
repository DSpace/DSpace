/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * <p>
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.RemoveOperation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.SiteService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.SubscriptionParameter;
import org.dspace.eperson.service.SubscribeService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test to test the /api/config/subscriptions endpoint
 * (Class has to start or end with IT to be picked up by the failsafe plugin)
 */
public class SubscriptionRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    SubscribeService subscribeService;
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        // We turn off the authorization system in order to create the structure as
        // defined below
//        context.turnOffAuthorisationSystem();
//        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
//        colPeople = CollectionBuilder.createCollection(context, parentCommunity).withName("People")
//                .withEntityType("Person").build();
//        context.restoreAuthSystemState();
    }

    @Test
    public void findAll() throws Exception {
        context.turnOffAuthorisationSystem();
        //When we call the root endpoint as anonymous user
        getClient().perform(get("/api/core/subscriptions"))
                //The status has to be 401 Not Authorized
                .andExpect(status().isUnauthorized());

        String token = getAuthToken(admin.getEmail(), password);
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        // creation of the item which will be the DSO related with a subscription
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Test")
                .withIssueDate("2010-10-17")
                .withAuthor("Smith, Donald")
                .withSubject("ExtraEntry")
                .build();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("Frequency");
        subscriptionParameter.setValue("Daily");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = subscribeService.subscribe(context, admin, publicItem1, subscriptionParameterList, "TypeTest");
        subscriptionParameter.setSubscription(subscription);
        //When we call the root endpoint
        context.restoreAuthSystemState();
        getClient(token).perform(get("/api/core/subscriptions"))
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                //By default we expect at least 1 submission forms so this to be reflected in the page object
                .andExpect(jsonPath("$.page.size", is(20)));
    }
    @Test
    public void findByIdAsAdministrator() throws Exception {
        //When we call the root endpoint as anonymous user
        getClient().perform(get("/api/core/subscriptions"))
                //The status has to be 403 Not Authorized
                .andExpect(status().isUnauthorized());

        String token = getAuthToken(admin.getEmail(), password);
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        // creation of the item which will be the DSO related with a subscription
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Item")
                .withIssueDate("2020-10-17")
                .withAuthor("John, Doe")
                .withSubject("Test")
                .build();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("Parameter");
        subscriptionParameter.setValue("ValueParameter");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = subscribeService.subscribe(context, admin, publicItem1, subscriptionParameterList, "TestType");
        context.restoreAuthSystemState();
        //When we call the root endpoint
        getClient(token).perform(get("/api/core/subscriptions/" + subscription.getID()))
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                //By default we expect at least 1 submission forms so this to be reflected in the page object
                .andExpect(jsonPath("$.type", is("TestType")))
                .andExpect(jsonPath("$.id", Matchers.endsWith(REST_SERVER_URL + "/api/core/dSpaceObject")))
                .andExpect(jsonPath("$.subscriptionParameterList[0].name", is("Parameter")))
                .andExpect(jsonPath("$.subscriptionParameterList[0].value", is("ValueParameter")))
                .andExpect(jsonPath("$._links.self.href", Matchers.startsWith(REST_SERVER_URL + "/api/core/subscriptions")))
                .andExpect(jsonPath("$._links.dSpaceObject.href", Matchers.startsWith(REST_SERVER_URL + "/api/core/subscriptions")))
                .andExpect(jsonPath("$._links.dSpaceObject.href", Matchers.endsWith(REST_SERVER_URL + "/api/core/dSpaceObject")))
                .andExpect(jsonPath("$._links.ePerson.href", Matchers.startsWith(REST_SERVER_URL + "/api/core/subscriptions")))
                .andExpect(jsonPath("$._links.ePerson.href", Matchers.endsWith(REST_SERVER_URL + "/api/core/ePerson")));


    }
    @Test
    public void findByIdAsRandomUser() throws Exception {
        //When we call the root endpoint as anonymous user
        getClient().perform(get("/api/core/subscriptions"))
                //The status has to be 403 Not Authorized
                .andExpect(status().isUnauthorized());

        String token = getAuthToken(eperson.getEmail(), password);
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        // creation of the item which will be the DSO related with a subscription
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Item")
                .withIssueDate("2020-10-17")
                .withAuthor("John, Doe")
                .withSubject("Test")
                .build();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("Parameter");
        subscriptionParameter.setValue("ValueParameter");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = subscribeService.subscribe(context, admin, publicItem1, subscriptionParameterList, "TestType");
        context.restoreAuthSystemState();
        //When we call the root endpoint
        getClient(token).perform(get("/api/core/subscriptions/" + subscription.getID()))
                //The status has to be 200 OK
                .andExpect(status().isUnauthorized());
    }
    @Test
    public void findAllSubscriptionsByEPerson() throws Exception {
        //When we call the root endpoint as anonymous user
        getClient().perform(get("/api/core/subscriptions"))
                //The status has to be 403 Not Authorized
                .andExpect(status().isUnauthorized());
        String token = getAuthToken(eperson.getEmail(), password);
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        // creation of the item which will be the DSO related with a subscription
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Item")
                .withIssueDate("2020-10-17")
                .withAuthor("John, Doe")
                .withSubject("Test")
                .build();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("Parameter1");
        subscriptionParameter.setValue("ValueParameter1");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = subscribeService.subscribe(context, eperson, publicItem1, subscriptionParameterList, "TestType");
        context.restoreAuthSystemState();
        //When we call the root endpoint
        getClient(token).perform(get("/api/core/subscriptions/findByEPerson?id=" + eperson.getID()))
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                //By default we expect at least 1 submission forms so this to be reflected in the page object
                .andExpect(jsonPath("$.type", is("TestType")))
                .andExpect(jsonPath("$.id", Matchers.endsWith(REST_SERVER_URL + "/api/core/dSpaceObject")))
                .andExpect(jsonPath("$.subscriptionParameterList[0].name", is("Parameter1")))
                .andExpect(jsonPath("$.subscriptionParameterList[0].value", is("ValueParameter1")))
                .andExpect(jsonPath("$._links.self.href", Matchers.startsWith(REST_SERVER_URL + "/api/core/subscriptions")))
                .andExpect(jsonPath("$._links.dSpaceObject.href", Matchers.startsWith(REST_SERVER_URL + "/api/core/subscriptions")))
                .andExpect(jsonPath("$._links.dSpaceObject.href", Matchers.endsWith(REST_SERVER_URL + "/api/core/dSpaceObject")))
                .andExpect(jsonPath("$._links.ePerson.href", Matchers.startsWith(REST_SERVER_URL + "/api/core/subscriptions")))
                .andExpect(jsonPath("$._links.ePerson.href", Matchers.endsWith(REST_SERVER_URL + "/api/core/ePerson")));
        EPerson epersonIT = EPersonBuilder.createEPerson(context)
                .withEmail("epersonIT@example.com")
                .withPassword(password)
                .withLanguage("al")
                .build();
        String epersonITtoken = getAuthToken(epersonIT.getEmail(), password);
        getClient(epersonITtoken).perform(get("/api/core/subscriptions/" + subscription.getID()))
                //The status has to be 200 OK
                .andExpect(status().isUnauthorized());
    }
    @Test
    public void addSubscriptionNotLoggedIn() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        // creation of the item which will be the DSO related with a subscription
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Item")
                .withIssueDate("2020-10-17")
                .withAuthor("John, Doe")
                .withSubject("Test")
                .build();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("Parameter1");
        subscriptionParameter.setValue("ValueParameter1");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = subscribeService.subscribe(context, eperson, publicItem1, subscriptionParameterList, "TestType");
        context.restoreAuthSystemState();
        //When we call the root endpoint as anonymous user
        getClient().perform(post("/api/core/subscriptions"))
                //The status has to be 403 Not Authorized
                .andExpect(status().isUnauthorized());
    }
    @Test
    public void addSubscriptionAsLoggedIn() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        // creation of the item which will be the DSO related with a subscription
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Item")
                .withIssueDate("2020-10-17")
                .withAuthor("John, Doe")
                .withSubject("Test")
                .build();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("Test");
        subscriptionParameter.setValue("Test");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = subscribeService.subscribe(context, eperson, publicItem1, subscriptionParameterList, "Type");
        context.restoreAuthSystemState();
        //When we call the root endpoint as anonymous user
        getClient().perform(post("/api/core/subscriptions"))
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                //By default we expect at least 1 submission forms so this to be reflected in the page object
                .andExpect(jsonPath("$.type", is("Type")))
                .andExpect(jsonPath("$.id", Matchers.endsWith(REST_SERVER_URL + "/api/core/dSpaceObject")))
                .andExpect(jsonPath("$.subscriptionParameterList[0].name", is("Test")))
                .andExpect(jsonPath("$.subscriptionParameterList[0].value", is("Test")))
                .andExpect(jsonPath("$._links.self.href", Matchers.startsWith(REST_SERVER_URL + "/api/core/subscriptions")))
                .andExpect(jsonPath("$._links.dSpaceObject.href", Matchers.startsWith(REST_SERVER_URL + "/api/core/subscriptions")))
                .andExpect(jsonPath("$._links.dSpaceObject.href", Matchers.endsWith(REST_SERVER_URL + "/api/core/dSpaceObject")))
                .andExpect(jsonPath("$._links.ePerson.href", Matchers.startsWith(REST_SERVER_URL + "/api/core/subscriptions")))
                .andExpect(jsonPath("$._links.ePerson.href", Matchers.endsWith(REST_SERVER_URL + "/api/core/ePerson")));
    }
    @Test
    public void editSubscriptionAnonymous() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        // creation of the item which will be the DSO related with a subscription
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Item")
                .withIssueDate("2020-10-17")
                .withAuthor("John, Doe")
                .withSubject("Test")
                .build();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("Parameter1");
        subscriptionParameter.setValue("ValueParameter1");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = subscribeService.subscribe(context, eperson, publicItem1, subscriptionParameterList, "TestType");
        context.restoreAuthSystemState();
        //When we call the root endpoint as anonymous user
        getClient().perform(put("/api/core/subscriptions"))
                //The status has to be 403 Not Authorized
                .andExpect(status().isUnauthorized());
    }
    @Test
    public void editSubscriptionNotAsSubscriberNotAsAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson epersonIT = EPersonBuilder.createEPerson(context)
                .withEmail("epersonIT@example.com")
                .withPassword(password)
                .withLanguage("al")
                .build();
        String epersonITtoken = getAuthToken(epersonIT.getEmail(), password);
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        // creation of the item which will be the DSO related with a subscription
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Item")
                .withIssueDate("2020-10-17")
                .withAuthor("John, Doe")
                .withSubject("Test")
                .build();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("Parameter1");
        subscriptionParameter.setValue("ValueParameter1");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = subscribeService.subscribe(context, eperson, publicItem1, subscriptionParameterList, "TestType");
        context.restoreAuthSystemState();
        //When we call the root endpoint as anonymous user
        getClient(epersonITtoken).perform(put("/api/core/subscriptions"))
                //The status has to be 403 Not Authorized
                .andExpect(status().isUnauthorized());
    }
    @Test
    public void editSubscriptionAsAdministratorOrSubscriber() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenSubscriber = getAuthToken(eperson.getEmail(), password);
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        // creation of the item which will be the DSO related with a subscription
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Item")
                .withIssueDate("2020-10-17")
                .withAuthor("John, Doe")
                .withSubject("Test")
                .build();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("Frequency");
        subscriptionParameter.setValue("Daily");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = subscribeService.subscribe(context, eperson, publicItem1, subscriptionParameterList, "Test");
        context.restoreAuthSystemState();
        getClient(tokenSubscriber).perform(put("/api/core/subscriptions"))
                //The status has to be 403 Not Authorized
                .andExpect(status().isOk());
        //When we call the root endpoint as anonymous user
        getClient(tokenAdmin).perform(put("/api/core/subscriptions"))
                //The status has to be 403 Not Authorized
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                //By default we expect at least 1 submission forms so this to be reflected in the page object
                .andExpect(jsonPath("$.type", is("Test")))
                .andExpect(jsonPath("$.id", Matchers.endsWith(REST_SERVER_URL + "/api/core/dSpaceObject")))
                .andExpect(jsonPath("$.subscriptionParameterList[0].name", is("Frequency")))
                .andExpect(jsonPath("$.subscriptionParameterList[0].value", is("Daily")))
                .andExpect(jsonPath("$._links.self.href", Matchers.startsWith(REST_SERVER_URL + "/api/core/subscriptions")))
                .andExpect(jsonPath("$._links.dSpaceObject.href", Matchers.startsWith(REST_SERVER_URL + "/api/core/subscriptions")))
                .andExpect(jsonPath("$._links.dSpaceObject.href", Matchers.endsWith(REST_SERVER_URL + "/api/core/dSpaceObject")))
                .andExpect(jsonPath("$._links.ePerson.href", Matchers.startsWith(REST_SERVER_URL + "/api/core/subscriptions")))
                .andExpect(jsonPath("$._links.ePerson.href", Matchers.endsWith(REST_SERVER_URL + "/api/core/ePerson")));
    }
    @Test
    public void deleteSubscriptionNotAsSubscriberNotAsAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson epersonIT = EPersonBuilder.createEPerson(context)
                .withEmail("epersonIT@example.com")
                .withPassword(password)
                .withLanguage("al")
                .build();
        String epersonITtoken = getAuthToken(epersonIT.getEmail(), password);
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        // creation of the item which will be the DSO related with a subscription
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Item")
                .withIssueDate("2020-10-17")
                .withAuthor("John, Doe")
                .withSubject("Test")
                .build();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("Frequency");
        subscriptionParameter.setValue("Daily");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = subscribeService.subscribe(context, eperson, publicItem1, subscriptionParameterList, "Test");
        context.restoreAuthSystemState();
        getClient(epersonITtoken).perform(put("/api/core/subscriptions"))
                //The status has to be 403 Not Authorized
                .andExpect(status().isUnauthorized());
    }
    @Test
    public void deleteSubscriptionAsAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        // creation of the item which will be the DSO related with a subscription
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Item")
                .withIssueDate("2020-10-17")
                .withAuthor("John, Doe")
                .withSubject("Test")
                .build();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("Frequency");
        subscriptionParameter.setValue("Daily");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = subscribeService.subscribe(context, eperson, publicItem1, subscriptionParameterList, "Test");
        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(put("/api/core/subscriptions"))
                //The status has to be 403 Not Authorized
                .andExpect(status().isOk());
    }
    @Test
    public void patchReplaceSubscriptionParameterAsAdmin() throws Exception {
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        // creation of the item which will be the DSO related with a subscription
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Item")
                .withIssueDate("2020-10-17")
                .withAuthor("John, Doe")
                .withSubject("Test")
                .build();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("TestName");
        subscriptionParameter.setValue("TestValue");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = subscribeService.subscribe(context, eperson, publicItem1, subscriptionParameterList, "Test");
        String token = getAuthToken(admin.getEmail(), password);
        List<Operation> ops = new ArrayList<Operation>();
        Map<String, String> value = new HashMap<>();
        value.put("name", "frequency");
        value.put("value", "monthly");
        ReplaceOperation replaceOperation = new ReplaceOperation("/subscriptionsParameter/"+subscription.getSubscriptionParameterList().get(0).getId(), value);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);
        getClient(token).perform(patch("/api/core/subscriptions/"+subscription.getID())
                        .content(patchBody)
                )
                //The status has to be 403 Not Authorized
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                //By default we expect at least 1 submission forms so this to be reflected in the page object
                .andExpect(jsonPath("$.type", is("Test")))
                .andExpect(jsonPath("$.id", Matchers.endsWith(REST_SERVER_URL + "/api/core/dSpaceObject")))
                .andExpect(jsonPath("$.subscriptionParameterList[0].name", is("frequency")))
                .andExpect(jsonPath("$.subscriptionParameterList[0].value", is("monthly")))
                .andExpect(jsonPath("$._links.self.href", Matchers.startsWith(REST_SERVER_URL + "/api/core/subscriptions")))
                .andExpect(jsonPath("$._links.dSpaceObject.href", Matchers.startsWith(REST_SERVER_URL + "/api/core/subscriptions")))
                .andExpect(jsonPath("$._links.dSpaceObject.href", Matchers.endsWith(REST_SERVER_URL + "/api/core/dSpaceObject")))
                .andExpect(jsonPath("$._links.ePerson.href", Matchers.startsWith(REST_SERVER_URL + "/api/core/subscriptions")))
                .andExpect(jsonPath("$._links.ePerson.href", Matchers.endsWith(REST_SERVER_URL + "/api/core/ePerson")));
    }
    @Test
    public void patchSubscriptionParameterNotAsAdminNotAsSubscriber() throws Exception {
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        // creation of the item which will be the DSO related with a subscription
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Item")
                .withIssueDate("2020-10-17")
                .withAuthor("John, Doe")
                .withSubject("Test")
                .build();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("TestName");
        subscriptionParameter.setValue("TestValue");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = subscribeService.subscribe(context, eperson, publicItem1, subscriptionParameterList, "Test");
        List<Operation> ops = new ArrayList<Operation>();
        Map<String, String> value = new HashMap<>();
        value.put("name", "frequency");
        value.put("value", "monthly");
        ReplaceOperation replaceOperation = new ReplaceOperation("/subscriptionsParameter/"+subscription.getSubscriptionParameterList().get(0).getId(), value);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);
        EPerson epersonIT = EPersonBuilder.createEPerson(context)
                .withEmail("epersonIT@example.com")
                .withPassword(password)
                .withLanguage("al")
                .build();
        String epersonITtoken = getAuthToken(epersonIT.getEmail(), password);
        getClient(epersonITtoken).perform(patch("/api/core/subscriptions/"+subscription.getID())
                        .content(patchBody)
                )
                //The status has to be 200 OK
                .andExpect(status().isUnauthorized());
    }
    @Test
    public void patchAddSubscriptionParameter() throws Exception {
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        // creation of the item which will be the DSO related with a subscription
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Item")
                .withIssueDate("2020-10-17")
                .withAuthor("John, Doe")
                .withSubject("Test")
                .build();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("TestName");
        subscriptionParameter.setValue("TestValue");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = subscribeService.subscribe(context, eperson, publicItem1, subscriptionParameterList, "Test");
        String token = getAuthToken(admin.getEmail(), password);
        List<Operation> ops = new ArrayList<Operation>();
        Map<String, String> value = new HashMap<>();
        value.put("name", "frequency");
        value.put("value", "monthly");
        AddOperation addOperation = new AddOperation("/subscriptionsParameter/"+subscription.getSubscriptionParameterList().get(0).getId(), value);
        ops.add(addOperation);
        String patchBody = getPatchContent(ops);
        getClient(token).perform(patch("/api/core/subscriptions/"+subscription.getID())
                        .content(patchBody)
                )
                //The status has to be 403 Not Authorized
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                //By default we expect at least 1 submission forms so this to be reflected in the page object
                .andExpect(jsonPath("$.type", is("Test")))
                .andExpect(jsonPath("$.id", Matchers.endsWith(REST_SERVER_URL + "/api/core/dSpaceObject")))
                .andExpect(jsonPath("$.subscriptionParameterList[0].name", is("TestName")))
                .andExpect(jsonPath("$.subscriptionParameterList[0].value", is("TestValue")))
                .andExpect(jsonPath("$.subscriptionParameterList[1].name", is("frequency")))
                .andExpect(jsonPath("$.subscriptionParameterList[1].value", is("monthly")))
                .andExpect(jsonPath("$._links.self.href", Matchers.startsWith(REST_SERVER_URL + "/api/core/subscriptions")))
                .andExpect(jsonPath("$._links.dSpaceObject.href", Matchers.startsWith(REST_SERVER_URL + "/api/core/subscriptions")))
                .andExpect(jsonPath("$._links.dSpaceObject.href", Matchers.endsWith(REST_SERVER_URL + "/api/core/dSpaceObject")))
                .andExpect(jsonPath("$._links.ePerson.href", Matchers.startsWith(REST_SERVER_URL + "/api/core/subscriptions")))
                .andExpect(jsonPath("$._links.ePerson.href", Matchers.endsWith(REST_SERVER_URL + "/api/core/ePerson")));
    }
    @Test
    public void patchRemoveSubscriptionParameter() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        // creation of the item which will be the DSO related with a subscription
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                .withTitle("Item")
                .withIssueDate("2020-10-17")
                .withAuthor("John, Doe")
                .withSubject("Test")
                .build();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("TestName");
        subscriptionParameter.setValue("TestValue");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = subscribeService.subscribe(context, eperson, publicItem1, subscriptionParameterList, "Test");
        String token = getAuthToken(admin.getEmail(), password);
        List<Operation> ops = new ArrayList<Operation>();
        Map<String, String> value = new HashMap<>();
        value.put("name", "frequency");
        value.put("value", "monthly");
        RemoveOperation removeOperation = new RemoveOperation("/subscriptionsParameter/"+subscription.getSubscriptionParameterList().get(0).getId());
        ops.add(removeOperation);
        String patchBody = getPatchContent(ops);
        context.restoreAuthSystemState();
        getClient(token).perform(patch("/api/core/subscriptions/"+subscription.getID())
                        .content(patchBody)
                )
                //The status has to be 403 Not Authorized
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                //By default we expect at least 1 submission forms so this to be reflected in the page object
                .andExpect(jsonPath("$.type", is("Test")))
                .andExpect(jsonPath("$.id", Matchers.endsWith(REST_SERVER_URL + "/api/core/dSpaceObject")))
                .andExpect(jsonPath("$.subscriptionParameterList", Matchers.arrayWithSize(0)))
                .andExpect(jsonPath("$._links.self.href", Matchers.startsWith(REST_SERVER_URL + "/api/core/subscriptions")));
    }
}