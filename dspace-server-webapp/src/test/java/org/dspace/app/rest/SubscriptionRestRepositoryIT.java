/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.model.SubscriptionParameterRest;
import org.dspace.app.rest.model.SubscriptionRest;
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
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
    private Collection collection;
    private Item publicItem;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community community = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        collection = CollectionBuilder.createCollection(context, community).withName("Collection 1").build();
        // creation of the item which will be the DSO related with a subscription
        publicItem = ItemBuilder.createItem(context, collection)
                .withTitle("Test")
                .withIssueDate("2010-10-17")
                .withAuthor("Smith, Donald")
                .withSubject("ExtraEntry")
                .build();
        context.restoreAuthSystemState();
    }

    // FIND ALL
    @Test
    public void findAll() throws Exception {
        context.turnOffAuthorisationSystem();
        String token = getAuthToken(admin.getEmail(), password);
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("Frequency");
        subscriptionParameter.setValue("Daily");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context, "TypeTest", publicItem, admin, subscriptionParameterList).build();
        subscriptionParameter.setSubscription(subscription);
        //When we call the root endpoint
        context.restoreAuthSystemState();
        getClient(token).perform(get("/api/core/subscriptions"))
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                //By default we expect at least 1 submission forms so this to be reflected in the page object
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.page.totalPages", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.page.number", is(0)))
                .andExpect(jsonPath("$._embedded.subscriptions[0].subscriptionType", is("TypeTest")))
                .andExpect(jsonPath("$._embedded.subscriptions[0]._links.dSpaceObject.href", Matchers.startsWith(REST_SERVER_URL + "core/subscriptions")))
                .andExpect(jsonPath("$._embedded.subscriptions[0]._links.dSpaceObject.href", Matchers.endsWith("dSpaceObject")))
                .andExpect(jsonPath("$._embedded.subscriptions[0]._links.ePerson.href", Matchers.startsWith(REST_SERVER_URL + "core/subscriptions")))
                .andExpect(jsonPath("$._embedded.subscriptions[0]._links.ePerson.href", Matchers.endsWith("ePerson")))
                .andExpect(jsonPath("$._embedded.subscriptions[0].subscriptionParameterList[0].name", is("Frequency")))
                .andExpect(jsonPath("$._embedded.subscriptions[0].subscriptionParameterList[0].value", is("Daily")))
                .andExpect(jsonPath("$._links.self.href", Matchers.is(REST_SERVER_URL + "core/subscriptions")));
    }

    @Test
    public void findAllAnonymous() throws Exception {
        //When we call the root endpoint as anonymous user
        getClient().perform(get("/api/core/subscriptions"))
                //The status has to be 401 Not Authorized
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void findAllAsUser() throws Exception {
        context.turnOffAuthorisationSystem();
        String token = getAuthToken(eperson.getEmail(), password);
        context.restoreAuthSystemState();
        //When we call the root endpoint as simple user
        getClient(token).perform(get("/api/core/subscriptions"))
                //The status has to be 403 Forbidden
                .andExpect(status().isForbidden());
    }

    @Test
    public void findAllWithResourceType() throws Exception {
        context.turnOffAuthorisationSystem();
        //When we call the root endpoint as anonymous user
        getClient().perform(get("/api/core/subscriptions"))
                //The status has to be 401 Not Authorized
                .andExpect(status().isUnauthorized());
        String token = getAuthToken(admin.getEmail(), password);
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("Frequency");
        subscriptionParameter.setValue("Daily");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context, "TypeTest", publicItem, admin, subscriptionParameterList).build();
        subscriptionParameter.setSubscription(subscription);
        //When we call the root endpoint
        context.restoreAuthSystemState();
        getClient(token).perform(get("/api/core/subscriptions?resourceType=Item"))
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                //By default we expect at least 1 submission forms so this to be reflected in the page object
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.page.totalPages", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.page.number", is(0)))
                .andExpect(jsonPath("$._embedded.subscriptions[0].subscriptionType", is("TypeTest")))
                .andExpect(jsonPath("$._embedded.subscriptions[0]._links.dSpaceObject.href", Matchers.startsWith(REST_SERVER_URL + "core/subscriptions")))
                .andExpect(jsonPath("$._embedded.subscriptions[0]._links.dSpaceObject.href", Matchers.endsWith("dSpaceObject")))
                .andExpect(jsonPath("$._embedded.subscriptions[0]._links.ePerson.href", Matchers.startsWith(REST_SERVER_URL + "core/subscriptions")))
                .andExpect(jsonPath("$._embedded.subscriptions[0]._links.ePerson.href", Matchers.endsWith("ePerson")))
                .andExpect(jsonPath("$._embedded.subscriptions[0].subscriptionParameterList[0].name", is("Frequency")))
                .andExpect(jsonPath("$._embedded.subscriptions[0].subscriptionParameterList[0].value", is("Daily")))
                .andExpect(jsonPath("$._links.self.href", Matchers.is(REST_SERVER_URL + "core/subscriptions?resourceType=Item")));
        // search for subscriptions related with collections
        getClient(token).perform(get("/api/core/subscriptions?resourceType=Collection"))
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                //By default we expect at least 1 submission forms so this to be reflected in the page object
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.page.totalPages", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.page.number", is(0)))
                .andExpect(jsonPath("$._links.self.href", Matchers.is(REST_SERVER_URL + "core/subscriptions?resourceType=Collection")));
    }

    // FIND BY ID
    @Test
    public void findByIdAsAdministrator() throws Exception {
        context.turnOffAuthorisationSystem();
        String token = getAuthToken(admin.getEmail(), password);
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("Parameter");
        subscriptionParameter.setValue("ValueParameter");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context, "TestType", publicItem, admin, subscriptionParameterList).build();
        context.restoreAuthSystemState();
        //When we call the root endpoint
        getClient(token).perform(get("/api/core/subscriptions/" + subscription.getID()))
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                //By default we expect at least 1 submission forms so this to be reflected in the page object
                .andExpect(jsonPath("$.subscriptionType", is("TestType")))
                .andExpect(jsonPath("$.subscriptionParameterList[0].name", is("Parameter")))
                .andExpect(jsonPath("$.subscriptionParameterList[0].value", is("ValueParameter")))
                .andExpect(jsonPath("$._links.self.href", Matchers.startsWith(REST_SERVER_URL + "core/subscriptions/" + subscription.getID())))
                .andExpect(jsonPath("$._links.dSpaceObject.href", Matchers.startsWith(REST_SERVER_URL + "core/subscriptions")))
                .andExpect(jsonPath("$._links.dSpaceObject.href", Matchers.endsWith("/dSpaceObject")))
                .andExpect(jsonPath("$._links.ePerson.href", Matchers.startsWith(REST_SERVER_URL + "core/subscriptions")))
                .andExpect(jsonPath("$._links.ePerson.href", Matchers.endsWith("/ePerson")));


    }

    @Test
    public void findByIdAsAnonymous() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("Parameter");
        subscriptionParameter.setValue("ValueParameter");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context, "TestType", publicItem, admin, subscriptionParameterList).build();
        context.restoreAuthSystemState();
        //When we call the root endpoint
        getClient().perform(get("/api/core/subscriptions/" + subscription.getID()))
                //The status has to be 401
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void findByIdNotAsSubscriberNotAsAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        String token = getAuthToken(eperson.getEmail(), password);
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("Parameter");
        subscriptionParameter.setValue("ValueParameter");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context, "TestType", publicItem, admin, subscriptionParameterList).build();
        context.restoreAuthSystemState();
        //When we call the root endpoint
        getClient(token).perform(get("/api/core/subscriptions/" + subscription.getID()))
                //The status has to be 500
                .andExpect(status().isInternalServerError());
    }

    // FIND ALL BY EPERSON/DSO
    @Test
    public void findAllSubscriptionsByEPerson() throws Exception {
        //When we call the root endpoint as anonymous user
//        getClient().perform(get("/api/core/subscriptions"))
//                //The status has to be 403 Not Authorized
//                .andExpect(status().isUnauthorized());
        String token = getAuthToken(eperson.getEmail(), password);
        context.turnOffAuthorisationSystem();
        EPerson user = EPersonBuilder.createEPerson(context)
                .withEmail("user@test.it")
                .withPassword(password)
                .build();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("Parameter1");
        subscriptionParameter.setValue("ValueParameter1");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context, "TestType", publicItem, user, subscriptionParameterList).build();
        context.restoreAuthSystemState();
        //When we call the root endpoint
        getClient(token).perform(get("/api/core/subscriptions/search/findByEPerson?id=" + eperson.getID()))
                //The status has to be 200 OK
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.page.totalPages", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.page.number", is(0)))
                .andExpect(jsonPath("$._embedded.subscriptions[0].subscriptionType", is("TestType")))
                .andExpect(jsonPath("$._embedded.subscriptions[0]._links.dSpaceObject.href", Matchers.startsWith(REST_SERVER_URL + "core/subscriptions")))
                .andExpect(jsonPath("$._embedded.subscriptions[0]._links.dSpaceObject.href", Matchers.endsWith("dSpaceObject")))
                .andExpect(jsonPath("$._embedded.subscriptions[0]._links.ePerson.href", Matchers.startsWith(REST_SERVER_URL + "core/subscriptions")))
                .andExpect(jsonPath("$._embedded.subscriptions[0]._links.ePerson.href", Matchers.endsWith("ePerson")))
                .andExpect(jsonPath("$._embedded.subscriptions[0].subscriptionParameterList[0].name", is("Parameter1")))
                .andExpect(jsonPath("$._embedded.subscriptions[0].subscriptionParameterList[0].value", is("ValueParameter1")));


        EPerson epersonIT = EPersonBuilder.createEPerson(context)
                .withEmail("epersonIT@example.com")
                .withPassword(password)
                .withLanguage("al")
                .build();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("Parameter1");
        subscriptionParameter.setValue("ValueParameter1");
        subscriptionParameterList.add(subscriptionParameter);
        List<SubscriptionParameter> subscriptionParameterList1 = new ArrayList<>();
        SubscriptionParameter subscriptionParameter1 = new SubscriptionParameter();
        subscriptionParameter1.setName("Parameter1");
        subscriptionParameter1.setValue("ValueParameter1");
        subscriptionParameterList1.add(subscriptionParameter1);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context, "TestType", collection, user, subscriptionParameterList).build();
        Subscription subscription1 = SubscribeBuilder.subscribeBuilder(context, "Test", collection, user, subscriptionParameterList1).build();
        context.restoreAuthSystemState();
        //When we call the root endpoint
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/core/subscriptions/search/findByEPersonAndDso?dspace_object_id=" + collection.getID() + "&eperson_id=" + user.getID()))
                //The status has to be 200 OK
                .andExpect(status().isUnauthorized());
    }

    // ADD
    @Test
    public void addSubscriptionNotLoggedIn() throws Exception {
        SubscriptionParameterRest subscriptionParameterRest = new SubscriptionParameterRest();
        subscriptionParameterRest.setValue("nameTest");
        subscriptionParameterRest.setName("valueTest");
        List<SubscriptionParameterRest> subscriptionParameterRestList = new ArrayList<>();
        subscriptionParameterRestList.add(subscriptionParameterRest);
        SubscriptionRest subscriptionRest = new SubscriptionRest();
        subscriptionRest.setType("testType");
        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("dspace_object_id", publicItem.getID().toString());
        params.add("eperson_id", eperson.getID().toString());
        ObjectMapper objectMapper = new ObjectMapper();
        getClient().perform(post("/api/core/subscriptions?dspace_object_id=" + publicItem.getID() + "&eperson_id=" + eperson.getID())
                        .content(objectMapper.writeValueAsString(subscriptionRest))
                        .contentType(contentType))
                //The status has to be 401 Not Authorized
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void addSubscriptionAsAdmin() throws Exception {
        //When we call the root endpoint as anonymous user
        SubscriptionParameterRest subscriptionParameterRest = new SubscriptionParameterRest();
        subscriptionParameterRest.setValue("nameTest");
        subscriptionParameterRest.setName("valueTest");
        List<SubscriptionParameterRest> subscriptionParameterRestList = new ArrayList<>();
        subscriptionParameterRestList.add(subscriptionParameterRest);
        SubscriptionRest subscriptionRest = new SubscriptionRest();
        subscriptionRest.setType("testType");
//        subscriptionRest.setSubscriptionParameterList(subscriptionParameterRestList);
        ObjectMapper objectMapper = new ObjectMapper();
        String token = getAuthToken(admin.getEmail(), password);
        Map<String, Object> map = new HashMap<>();
        map.put("type", "test");
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> sub_list = new HashMap<>();
        sub_list.put("name", "frequency");
        sub_list.put("value", "daily");
        list.add(sub_list);
        map.put("subscriptionParameterList", list);
        getClient(token).perform(post("/api/core/subscriptions?dspace_object_id=" + publicItem.getID() + "&eperson_id=" + admin.getID())
                        .content(objectMapper.writeValueAsString(map))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                //The status has to be 200 OK
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                //By default we expect at least 1 submission forms so this to be reflected in the page object
                .andExpect(jsonPath("$.subscriptionType", is("testType")))
                .andExpect(jsonPath("$.id", Matchers.endsWith(REST_SERVER_URL + "core/dSpaceObject")))
                .andExpect(jsonPath("$.subscriptionParameterList[0].name", is("nameTest")))
                .andExpect(jsonPath("$.subscriptionParameterList[0].value", is("valueTest")))
                .andExpect(jsonPath("$._links.self.href", Matchers.startsWith(REST_SERVER_URL + "core/subscriptions")))
                .andExpect(jsonPath("$._links.dSpaceObject.href", Matchers.startsWith(REST_SERVER_URL + "core/subscriptions")))
                .andExpect(jsonPath("$._links.dSpaceObject.href", Matchers.endsWith("dSpaceObject")))
                .andExpect(jsonPath("$._links.ePerson.href", Matchers.startsWith(REST_SERVER_URL + "core/subscriptions")))
                .andExpect(jsonPath("$._links.ePerson.href", Matchers.endsWith("ePerson")));
    }

    // PUT
    @Test
    public void editSubscriptionAnonymous() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("Parameter1");
        subscriptionParameter.setValue("ValueParameter1");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context, "TestType", publicItem, admin, subscriptionParameterList).build();
        ObjectMapper objectMapper = new ObjectMapper();
        String token = getAuthToken(admin.getEmail(), password);
        Map<String, Object> newSubscription = new HashMap<>();
        newSubscription.put("type", "test");
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> sub_list = new HashMap<>();
        sub_list.put("name", "frequency");
        sub_list.put("value", "daily");
        list.add(sub_list);
        newSubscription.put("subscriptionParameterList", list);
        context.restoreAuthSystemState();
        //When we call the root endpoint as anonymous user
        getClient().perform(put("/api/core/subscriptions/" + subscription.getID() + "?dspace_object_id=" + publicItem.getID() + "&eperson_id=" + admin.getID())
                        .content(objectMapper.writeValueAsString(newSubscription))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
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
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("Parameter1");
        subscriptionParameter.setValue("ValueParameter1");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context, "TestType", publicItem, eperson, subscriptionParameterList).build();
        context.restoreAuthSystemState();
        ObjectMapper objectMapper = new ObjectMapper();
        String token = getAuthToken(epersonIT.getEmail(), password);
        Map<String, Object> newSubscription = new HashMap<>();
        newSubscription.put("type", "test");
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> sub_list = new HashMap<>();
        sub_list.put("name", "frequency");
        sub_list.put("value", "daily");
        list.add(sub_list);
        newSubscription.put("subscriptionParameterList", list);
        //When we call the root endpoint as anonymous user
        getClient(token).perform(put("/api/core/subscriptions/" + subscription.getID() + "?dspace_object_id=" + publicItem.getID() + "&eperson_id=" + admin.getID())
                        .content(objectMapper.writeValueAsString(newSubscription))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                //The status has to be 500 Error
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void editSubscriptionAsAdministratorOrSubscriber() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        String tokenSubscriber = getAuthToken(eperson.getEmail(), password);
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("Frequency");
        subscriptionParameter.setValue("Daily");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context, "TestType", publicItem, eperson, subscriptionParameterList).build();
        context.restoreAuthSystemState();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> newSubscription = new HashMap<>();
        newSubscription.put("type", "test");
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> sub_list = new HashMap<>();
        sub_list.put("name", "frequency");
        sub_list.put("value", "daily");
        list.add(sub_list);
        newSubscription.put("subscriptionParameterList", list);
        String tokenSubscriber = getAuthToken(eperson.getEmail(), password);
        getClient(tokenSubscriber).perform(put("/api/core/subscriptions/" + subscription.getID() + "?dspace_object_id=" + publicItem.getID() + "&eperson_id=" + eperson.getID())
                        //The status has to be 403 Not Authorized
                        .content(objectMapper.writeValueAsString(newSubscription))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                //By default we expect at least 1 submission forms so this to be reflected in the page object
                .andExpect(jsonPath("$.subscriptionType", is("test")))
                .andExpect(jsonPath("$.subscriptionParameterList[0].name", is("frequency")))
                .andExpect(jsonPath("$.subscriptionParameterList[0].value", is("daily")))
                .andExpect(jsonPath("$._links.self.href", Matchers.startsWith(REST_SERVER_URL + "core/subscriptions")))
                .andExpect(jsonPath("$._links.dSpaceObject.href", Matchers.startsWith(REST_SERVER_URL + "core/subscriptions")))
                .andExpect(jsonPath("$._links.dSpaceObject.href", Matchers.endsWith("/dSpaceObject")))
                .andExpect(jsonPath("$._links.ePerson.href", Matchers.startsWith(REST_SERVER_URL + "core/subscriptions")))
                .andExpect(jsonPath("$._links.ePerson.href", Matchers.endsWith("/ePerson")));
    }

    // DELETE
    @Test
    public void deleteSubscriptionNotAsSubscriberNotAsAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson epersonIT = EPersonBuilder.createEPerson(context)
                .withEmail("epersonIT@example.com")
                .withPassword(password)
                .withLanguage("al")
                .build();
        String epersonITtoken = getAuthToken(epersonIT.getEmail(), password);
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("Frequency");
        subscriptionParameter.setValue("Daily");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context, "Test", publicItem, eperson, subscriptionParameterList).build();
        context.restoreAuthSystemState();
        getClient(epersonITtoken).perform(put("/api/core/subscriptions"))
                //The status has to be 403 Not Authorized
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void deleteSubscriptionAsAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("Frequency");
        subscriptionParameter.setValue("Daily");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context, "Test", publicItem, eperson, subscriptionParameterList).build();
        String token = getAuthToken(admin.getEmail(), password);
        context.restoreAuthSystemState();
        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(put("/api/core/subscriptions"))
                //The status has to be 403 Not Authorized
                .andExpect(status().isOk());
    }

    // PATCH
    @Test
    public void patchReplaceSubscriptionParameterAsAdmin() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("TestName");
        subscriptionParameter.setValue("TestValue");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context, "Test", publicItem, eperson, subscriptionParameterList).build();
        String token = getAuthToken(admin.getEmail(), password);
        List<Operation> ops = new ArrayList<Operation>();
        Map<String, String> value = new HashMap<>();
        value.put("name", "frequency");
        value.put("value", "monthly");
        ReplaceOperation replaceOperation = new ReplaceOperation("/subscriptionsParameter/" + subscription.getSubscriptionParameterList().get(0).getId(), value);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);
        getClient(token).perform(patch("/api/core/subscriptions/" + subscription.getID())
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
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("TestName");
        subscriptionParameter.setValue("TestValue");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context, "Test", publicItem, eperson, subscriptionParameterList).build();
        List<Operation> ops = new ArrayList<Operation>();
        Map<String, String> value = new HashMap<>();
        value.put("name", "frequency");
        value.put("value", "monthly");
        ReplaceOperation replaceOperation = new ReplaceOperation("/subscriptionsParameter/" + subscription.getSubscriptionParameterList().get(0).getId(), value);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);
        EPerson epersonIT = EPersonBuilder.createEPerson(context)
                .withEmail("epersonIT@example.com")
                .withPassword(password)
                .withLanguage("al")
                .build();
        String epersonITtoken = getAuthToken(epersonIT.getEmail(), password);
        getClient(epersonITtoken).perform(patch("/api/core/subscriptions/" + subscription.getID())
                        .content(patchBody)
                )
                //The status has to be 403 Forbidden
                .andExpect(status().isForbidden());
    }

    @Test
    public void patchAddSubscriptionParameter() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("TestName");
        subscriptionParameter.setValue("TestValue");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context, "Test", publicItem, eperson, subscriptionParameterList).build();
        String token = getAuthToken(admin.getEmail(), password);
        List<Operation> ops = new ArrayList<Operation>();
        Map<String, String> value = new HashMap<>();
        value.put("name", "frequency");
        value.put("value", "monthly");
        AddOperation addOperation = new AddOperation("/subscriptionsParameter/" + subscription.getSubscriptionParameterList().get(0).getId(), value);
        ops.add(addOperation);
        String patchBody = getPatchContent(ops);
        getClient(token).perform(patch("/api/core/subscriptions/" + subscription.getID())
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
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("TestName");
        subscriptionParameter.setValue("TestValue");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context, "Test", publicItem, eperson, subscriptionParameterList).build();
        String token = getAuthToken(admin.getEmail(), password);
        List<Operation> ops = new ArrayList<Operation>();
        Map<String, String> value = new HashMap<>();
        value.put("name", "frequency");
        value.put("value", "monthly");
        RemoveOperation removeOperation = new RemoveOperation("/subscriptionsParameter/" + subscription.getSubscriptionParameterList().get(0).getId());
        ops.add(removeOperation);
        String patchBody = getPatchContent(ops);
        context.restoreAuthSystemState();
        getClient(token).perform(patch("/api/core/subscriptions/" + subscription.getID())
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