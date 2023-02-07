/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dspace.app.rest.matcher.EPersonMatcher;
import org.dspace.app.rest.matcher.SubscriptionMatcher;
import org.dspace.app.rest.model.SubscriptionParameterRest;
import org.dspace.app.rest.model.SubscriptionRest;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.SubscribeBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Subscription;
import org.dspace.eperson.SubscriptionParameter;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * Integration test to test the /api/config/subscriptions endpoint
 * (Class has to start or end with IT to be picked up by the failsafe plugin)
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class SubscriptionRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ResourcePolicyService resourcePolicyService;

    private Community subCommunity;
    private Collection collection;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        subCommunity = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                       .withName("Test Sub Community")
                                       .build();
        collection = CollectionBuilder.createCollection(context, parentCommunity)
                                      .withName("Collection 1")
                                      .withSubmitterGroup(eperson)
                                      .build();

        context.restoreAuthSystemState();
    }

    @Test
    public void findAll() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("D");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription1 = SubscribeBuilder.subscribeBuilder(context,
                                     "content", collection, eperson, subscriptionParameterList).build();
        List<SubscriptionParameter> subscriptionParameterList2 = new ArrayList<>();
        SubscriptionParameter subscriptionParameter2 = new SubscriptionParameter();
        subscriptionParameter2.setName("frequency");
        subscriptionParameter2.setValue("W");
        subscriptionParameterList2.add(subscriptionParameter2);
        Subscription subscription2 = SubscribeBuilder.subscribeBuilder(context,
                                     "content", collection, admin, subscriptionParameterList2).build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/subscriptions"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$._embedded.subscriptions", Matchers.containsInAnyOrder(
                                 SubscriptionMatcher.matchSubscription(subscription1),
                                 SubscriptionMatcher.matchSubscription(subscription2)
                                 )))
                             .andExpect(jsonPath("$.page.size", is(20)))
                             .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(2)))
                             .andExpect(jsonPath("$.page.totalPages", greaterThanOrEqualTo(1)))
                             .andExpect(jsonPath("$.page.number", is(0)));
    }

    @Test
    public void findAllAnonymous() throws Exception {
        getClient().perform(get("/api/core/subscriptions"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findAllAsUser() throws Exception {
        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEPerson).perform(get("/api/core/subscriptions"))
                               .andExpect(status().isForbidden());
    }

    @Test
    public void findOneWithOwnerTest() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("M");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context,
                                    "content", collection, eperson, subscriptionParameterList).build();
        context.restoreAuthSystemState();

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEPerson).perform(get("/api/core/subscriptions/" + subscription.getID()))
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                //By default we expect at least 1 submission forms so this to be reflected in the page object
                .andExpect(jsonPath("$.subscriptionType", is("content")))
                .andExpect(jsonPath("$.subscriptionParameterList[0].name", is("frequency")))
                .andExpect(jsonPath("$.subscriptionParameterList[0].value", is("M")))
                .andExpect(jsonPath("$._links.eperson.href", Matchers.endsWith("/eperson")))
                .andExpect(jsonPath("$._links.resource.href", Matchers.endsWith("/resource")))
                .andExpect(jsonPath("$._links.self.href",
                           Matchers.startsWith(REST_SERVER_URL + "core/subscriptions/" + subscription.getID())))
                .andExpect(jsonPath("$._links.resource.href",
                           Matchers.startsWith(REST_SERVER_URL + "core/subscriptions")))
                .andExpect(jsonPath("$._links.eperson.href",
                           Matchers.startsWith(REST_SERVER_URL + "core/subscriptions")));
    }

    @Test
    public void findOneAdminTest() throws Exception {
        context.turnOffAuthorisationSystem();

        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("W");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context,
                                    "content", collection, admin, subscriptionParameterList).build();
        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/subscriptions/" + subscription.getID()))
                .andExpect(status().isOk())
                //We expect the content type to be "application/hal+json;charset=UTF-8"
                .andExpect(content().contentType(contentType))
                //By default we expect at least 1 submission forms so this to be reflected in the page object
                .andExpect(jsonPath("$.subscriptionType", is("content")))
                .andExpect(jsonPath("$.subscriptionParameterList[0].name", is("frequency")))
                .andExpect(jsonPath("$.subscriptionParameterList[0].value", is("W")))
                .andExpect(jsonPath("$._links.self.href",
                           Matchers.startsWith(REST_SERVER_URL + "core/subscriptions/" + subscription.getID())))
                .andExpect(jsonPath("$._links.resource.href",
                           Matchers.startsWith(REST_SERVER_URL + "core/subscriptions")))
                .andExpect(jsonPath("$._links.resource.href", Matchers.endsWith("/resource")))
                .andExpect(jsonPath("$._links.eperson.href",
                           Matchers.startsWith(REST_SERVER_URL + "core/subscriptions")))
                .andExpect(jsonPath("$._links.eperson.href", Matchers.endsWith("/eperson")));
    }

    @Test
    public void findOneAnonymousTest() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("D");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context,
                                    "content", collection, admin, subscriptionParameterList).build();
        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/subscriptions/" + subscription.getID()))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("W");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context,
                                    "content", collection, admin, subscriptionParameterList).build();
        context.restoreAuthSystemState();

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEPerson).perform(get("/api/core/subscriptions/" + subscription.getID()))
                               .andExpect(status().isForbidden());
    }

    @Test
    public void findOneNotFoundTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/subscriptions/" + Integer.MAX_VALUE))
                             .andExpect(status().isNotFound());
    }

    @Test
    public void findSubscriptionsByEPersonAdminTest() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("D");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription1 = SubscribeBuilder.subscribeBuilder(context,
                                     "content", collection, eperson, subscriptionParameterList).build();
        List<SubscriptionParameter> subscriptionParameterList2 = new ArrayList<>();
        SubscriptionParameter subscriptionParameter2 = new SubscriptionParameter();
        subscriptionParameter2.setName("frequency");
        subscriptionParameter2.setValue("W");
        subscriptionParameterList2.add(subscriptionParameter2);
        Subscription subscription2 = SubscribeBuilder.subscribeBuilder(context,
                                     "content", collection, eperson, subscriptionParameterList2).build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/subscriptions/search/findByEPerson")
                             .param("uuid", eperson.getID().toString()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$._embedded.subscriptions", Matchers.containsInAnyOrder(
                                            SubscriptionMatcher.matchSubscription(subscription1),
                                            SubscriptionMatcher.matchSubscription(subscription2)
                                            )))
                             .andExpect(jsonPath("$.page.size", is(20)))
                             .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(2)))
                             .andExpect(jsonPath("$.page.totalPages", greaterThanOrEqualTo(1)))
                             .andExpect(jsonPath("$.page.number", is(0)));
    }

    @Test
    public void findSubscriptionsByEPersonOwnerTest() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("M");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription1 = SubscribeBuilder.subscribeBuilder(context,
                                     "content", collection, eperson, subscriptionParameterList).build();
        List<SubscriptionParameter> subscriptionParameterList2 = new ArrayList<>();
        SubscriptionParameter subscriptionParameter2 = new SubscriptionParameter();
        subscriptionParameter2.setName("frequency");
        subscriptionParameter2.setValue("D");
        subscriptionParameterList2.add(subscriptionParameter2);
        Subscription subscription2 = SubscribeBuilder.subscribeBuilder(context,
                                     "content", collection, eperson, subscriptionParameterList2).build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(eperson.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/subscriptions/search/findByEPerson")
                             .param("uuid", eperson.getID().toString()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$._embedded.subscriptions", Matchers.containsInAnyOrder(
                                            SubscriptionMatcher.matchSubscription(subscription1),
                                            SubscriptionMatcher.matchSubscription(subscription2)
                                            )))
                             .andExpect(jsonPath("$.page.size", is(20)))
                             .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(2)))
                             .andExpect(jsonPath("$.page.totalPages", greaterThanOrEqualTo(1)))
                             .andExpect(jsonPath("$.page.number", is(0)));
    }

    @Test
    public void findSubscriptionsByEPersonUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("D");
        subscriptionParameterList.add(subscriptionParameter);
        SubscribeBuilder.subscribeBuilder(context, "content", collection, eperson, subscriptionParameterList).build();
        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/subscriptions/search/findByEPerson")
                   .param("uuid", eperson.getID().toString()))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findSubscriptionsByEPersonForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson user = EPersonBuilder.createEPerson(context)
                                     .withEmail("user1@mail.com")
                                     .withPassword(password)
                                     .build();

        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("D");
        subscriptionParameterList.add(subscriptionParameter);
        SubscribeBuilder.subscribeBuilder(context, "content", collection, user, subscriptionParameterList).build();
        context.restoreAuthSystemState();

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEPerson).perform(get("/api/core/subscriptions/search/findByEPerson")
                               .param("uuid", user.getID().toString()))
                               .andExpect(status().isForbidden());
    }

    @Test
    public void findByEPersonAndDsoAdminTest() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("D");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription1 = SubscribeBuilder.subscribeBuilder(context,
                                      "content", collection, eperson, subscriptionParameterList).build();

        List<SubscriptionParameter> subscriptionParameterList2 = new ArrayList<>();
        SubscriptionParameter subscriptionParameter2 = new SubscriptionParameter();
        subscriptionParameter2.setName("frequency");
        subscriptionParameter2.setValue("W");
        subscriptionParameterList2.add(subscriptionParameter2);
        Subscription subscription2 = SubscribeBuilder.subscribeBuilder(context,
                                     "content", collection, eperson, subscriptionParameterList2).build();

        List<SubscriptionParameter> subscriptionParameterList3 = new ArrayList<>();
        SubscriptionParameter subscriptionParameter3 = new SubscriptionParameter();
        subscriptionParameter3.setName("frequency");
        subscriptionParameter3.setValue("M");
        subscriptionParameterList3.add(subscriptionParameter3);
        Subscription subscription3 = SubscribeBuilder.subscribeBuilder(context,
                                     "content", subCommunity, eperson, subscriptionParameterList3).build();

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/subscriptions/search/findByEPersonAndDso")
                             .param("eperson_id", eperson.getID().toString())
                             .param("resource", collection.getID().toString()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$._embedded.subscriptions", Matchers.containsInAnyOrder(
                                            SubscriptionMatcher.matchSubscription(subscription1),
                                            SubscriptionMatcher.matchSubscription(subscription2)
                                            )))
                             .andExpect(jsonPath("$.page.size", is(20)))
                             .andExpect(jsonPath("$.page.totalElements", is(2)))
                             .andExpect(jsonPath("$.page.totalPages", is(1)))
                             .andExpect(jsonPath("$.page.number", is(0)));

        getClient(tokenAdmin).perform(get("/api/core/subscriptions/search/findByEPersonAndDso")
                             .param("eperson_id", eperson.getID().toString())
                             .param("resource", subCommunity.getID().toString()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$._embedded.subscriptions", Matchers.contains(
                                           SubscriptionMatcher.matchSubscription(subscription3)
                                           )))
                             .andExpect(jsonPath("$.page.size", is(20)))
                             .andExpect(jsonPath("$.page.totalElements", is(1)))
                             .andExpect(jsonPath("$.page.totalPages", is(1)))
                             .andExpect(jsonPath("$.page.number", is(0)));
    }

    @Test
    public void findByEPersonAndDsoOwnerTest() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("D");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription1 = SubscribeBuilder.subscribeBuilder(context,
                                      "content", subCommunity, eperson, subscriptionParameterList).build();

        context.restoreAuthSystemState();

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEPerson).perform(get("/api/core/subscriptions/search/findByEPersonAndDso")
                               .param("eperson_id", eperson.getID().toString())
                               .param("resource", subCommunity.getID().toString()))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$._embedded.subscriptions", Matchers.contains(
                                          SubscriptionMatcher.matchSubscription(subscription1)
                                          )))
                               .andExpect(jsonPath("$.page.size", is(20)))
                               .andExpect(jsonPath("$.page.totalElements", is(1)))
                               .andExpect(jsonPath("$.page.totalPages", is(1)))
                               .andExpect(jsonPath("$.page.number", is(0)));
    }

    @Test
    public void findByEPersonAndDsoUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("D");
        subscriptionParameterList.add(subscriptionParameter);
        SubscribeBuilder.subscribeBuilder(context, "content", subCommunity, eperson, subscriptionParameterList).build();
        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/subscriptions/search/findByEPersonAndDso")
                   .param("eperson_id", eperson.getID().toString())
                   .param("resource", subCommunity.getID().toString()))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findByEPersonAndDsoForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("D");
        subscriptionParameterList.add(subscriptionParameter);
        SubscribeBuilder.subscribeBuilder(context, "content", subCommunity, admin, subscriptionParameterList).build();
        context.restoreAuthSystemState();

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEPerson).perform(get("/api/core/subscriptions/search/findByEPersonAndDso")
                               .param("eperson_id", admin.getID().toString())
                               .param("resource", subCommunity.getID().toString()))
                               .andExpect(status().isForbidden());
    }

    @Test
    public void createSubscriptionUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        SubscriptionParameterRest subscriptionParameterRest = new SubscriptionParameterRest();
        subscriptionParameterRest.setValue("frequency");
        subscriptionParameterRest.setName("D");
        List<SubscriptionParameterRest> subscriptionParameterRestList = new ArrayList<>();
        subscriptionParameterRestList.add(subscriptionParameterRest);

        SubscriptionRest subscriptionRest = new SubscriptionRest();
        subscriptionRest.setSubscriptionType("content");

        context.restoreAuthSystemState();

        ObjectMapper objectMapper = new ObjectMapper();

        getClient().perform(post("/api/core/subscriptions")
                   .param("resource", collection.getID().toString())
                   .param("eperson_id", eperson.getID().toString())
                   .content(objectMapper.writeValueAsString(subscriptionRest))
                   .contentType(contentType))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void createSubscriptionAdminForOtherPersonTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Map<String, Object> map = new HashMap<>();
        map.put("subscriptionType", "content");
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> sub_list = new HashMap<>();
        sub_list.put("name", "frequency");
        sub_list.put("value", "D");
        list.add(sub_list);
        map.put("subscriptionParameterList", list);

        context.restoreAuthSystemState();

        AtomicReference<Integer> idRef = new AtomicReference<Integer>();

        try {
            String tokenAdmin = getAuthToken(admin.getEmail(), password);
            getClient(tokenAdmin).perform(post("/api/core/subscriptions")
                                 .param("resource", collection.getID().toString())
                                 .param("eperson_id", eperson.getID().toString())
                                 .content(new ObjectMapper().writeValueAsString(map))
                                 .contentType(MediaType.APPLICATION_JSON_VALUE))
                       .andExpect(status().isCreated())
                       .andExpect(jsonPath("$.subscriptionType", is("content")))
                       .andExpect(jsonPath("$.subscriptionParameterList[0].name", is("frequency")))
                       .andExpect(jsonPath("$.subscriptionParameterList[0].value", is("D")))
                       .andExpect(jsonPath("$._links.eperson.href", Matchers.endsWith("eperson")))
                       .andExpect(jsonPath("$._links.resource.href", Matchers.endsWith("resource")))
                       .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));
        } finally {
            SubscribeBuilder.deleteSubscription(idRef.get());
        }
    }

    @Test
    public void createSubscriptionByEPersonTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Map<String, Object> map = new HashMap<>();
        map.put("subscriptionType", "content");
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> sub_list = new HashMap<>();
        sub_list.put("name", "frequency");
        sub_list.put("value", "W");
        list.add(sub_list);
        map.put("subscriptionParameterList", list);

        context.restoreAuthSystemState();

        AtomicReference<Integer> idRef = new AtomicReference<Integer>();

        try {
            String tokenEPerson = getAuthToken(eperson.getEmail(), password);
            getClient(tokenEPerson).perform(post("/api/core/subscriptions")
                                   .param("resource", collection.getID().toString())
                                   .param("eperson_id", eperson.getID().toString())
                                   .content(new ObjectMapper().writeValueAsString(map))
                                   .contentType(MediaType.APPLICATION_JSON_VALUE))
                       .andExpect(status().isCreated())
                       .andExpect(jsonPath("$.subscriptionType", is("content")))
                       .andExpect(jsonPath("$.subscriptionParameterList[0].name", is("frequency")))
                       .andExpect(jsonPath("$.subscriptionParameterList[0].value", is("W")))
                       .andExpect(jsonPath("$._links.eperson.href", Matchers.endsWith("eperson")))
                       .andExpect(jsonPath("$._links.resource.href", Matchers.endsWith("resource")))
                       .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));
        } finally {
            SubscribeBuilder.deleteSubscription(idRef.get());
        }
    }

    @Test
    public void createSubscriptionForItemByEPersonTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Item item1 = ItemBuilder.createItem(context, collection)
                                .withTitle("Public item")
                                .build();

        Map<String, Object> map = new HashMap<>();
        map.put("subscriptionType", "content");
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> sub_list = new HashMap<>();
        sub_list.put("name", "frequency");
        sub_list.put("value", "W");
        list.add(sub_list);
        map.put("subscriptionParameterList", list);

        context.restoreAuthSystemState();

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEPerson).perform(post("/api/core/subscriptions")
                               .param("resource", item1.getID().toString())
                               .param("eperson_id", eperson.getID().toString())
                               .content(new ObjectMapper().writeValueAsString(map))
                               .contentType(MediaType.APPLICATION_JSON_VALUE))
                               .andExpect(status().isBadRequest());
    }

    @Test
    public void createSubscriptionForItemByAdminTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Item item1 = ItemBuilder.createItem(context, collection)
                                .withTitle("Public item")
                                .build();

        Map<String, Object> map = new HashMap<>();
        map.put("subscriptionType", "content");
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> sub_list = new HashMap<>();
        sub_list.put("name", "frequency");
        sub_list.put("value", "W");
        list.add(sub_list);
        map.put("subscriptionParameterList", list);

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(post("/api/core/subscriptions")
                             .param("resource", item1.getID().toString())
                             .param("eperson_id", eperson.getID().toString())
                             .content(new ObjectMapper().writeValueAsString(map))
                             .contentType(MediaType.APPLICATION_JSON_VALUE))
                             .andExpect(status().isBadRequest());
    }

    @Test
    public void createSubscriptionWrongResourceUUIDTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Map<String, Object> map = new HashMap<>();
        map.put("subscriptionType", "content");
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> sub_list = new HashMap<>();
        sub_list.put("name", "frequency");
        sub_list.put("value", "W");
        list.add(sub_list);
        map.put("subscriptionParameterList", list);

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(post("/api/core/subscriptions")
                             .param("resource", UUID.randomUUID().toString())
                             .param("eperson_id", eperson.getID().toString())
                             .content(new ObjectMapper().writeValueAsString(map))
                             .contentType(MediaType.APPLICATION_JSON_VALUE))
                             .andExpect(status().isBadRequest());
    }

    @Test
    public void createSubscriptionMissingResourceUUIDTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Map<String, Object> map = new HashMap<>();
        map.put("subscriptionType", "content");
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> sub_list = new HashMap<>();
        sub_list.put("name", "frequency");
        sub_list.put("value", "W");
        list.add(sub_list);
        map.put("subscriptionParameterList", list);

        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(post("/api/core/subscriptions")
                             .param("eperson_id", eperson.getID().toString())
                             .content(new ObjectMapper().writeValueAsString(map))
                             .contentType(MediaType.APPLICATION_JSON_VALUE))
                             .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void createSubscriptionWithWrongSubscriptionParameterNameTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Map<String, Object> map = new HashMap<>();
        map.put("subscriptionType", "content");
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> sub_list = new HashMap<>();
        sub_list.put("name", "TestName");
        sub_list.put("value", "X");
        list.add(sub_list);
        map.put("subscriptionParameterList", list);

        context.restoreAuthSystemState();

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEPerson).perform(post("/api/core/subscriptions")
                               .param("resource", collection.getID().toString())
                               .param("eperson_id", eperson.getID().toString())
                               .content(new ObjectMapper().writeValueAsString(map))
                               .contentType(MediaType.APPLICATION_JSON_VALUE))
                               .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void createSubscriptionWithInvalidSubscriptionParameterValueTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Map<String, Object> map = new HashMap<>();
        map.put("subscriptionType", "content");
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> sub_list = new HashMap<>();
        sub_list.put("name", "frequency");
        sub_list.put("value", "X");
        list.add(sub_list);
        map.put("subscriptionParameterList", list);

        context.restoreAuthSystemState();

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEPerson).perform(post("/api/core/subscriptions")
                               .param("resource", collection.getID().toString())
                               .param("eperson_id", eperson.getID().toString())
                               .content(new ObjectMapper().writeValueAsString(map))
                               .contentType(MediaType.APPLICATION_JSON_VALUE))
                               .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void createSubscriptionWithInvalidSubscriptionTypeValueTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Map<String, Object> map = new HashMap<>();
        map.put("subscriptionType", "InvalidValue");
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> sub_list = new HashMap<>();
        sub_list.put("name", "frequency");
        sub_list.put("value", "W");
        list.add(sub_list);
        map.put("subscriptionParameterList", list);

        context.restoreAuthSystemState();

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEPerson).perform(post("/api/core/subscriptions")
                               .param("resource", collection.getID().toString())
                               .param("eperson_id", eperson.getID().toString())
                               .content(new ObjectMapper().writeValueAsString(map))
                               .contentType(MediaType.APPLICATION_JSON_VALUE))
                               .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void createSubscriptionInvalidJsonTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Map<String, Object> map = new HashMap<>();
        map.put("type", "content");
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> sub_list = new HashMap<>();
        sub_list.put("name", "frequency");
        sub_list.put("value", "daily");
        list.add(sub_list);
        map.put("subscriptionParameterList", list);

        context.restoreAuthSystemState();

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEPerson).perform(post("/api/core/subscriptions")
                               .param("resource", collection.getID().toString())
                               .param("eperson_id", eperson.getID().toString())
                               .content(new ObjectMapper().writeValueAsString(map))
                               .contentType(MediaType.APPLICATION_JSON_VALUE))
                               .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void createSubscriptionPersonForAnotherPersonTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson user = EPersonBuilder.createEPerson(context)
                                     .withEmail("user1@mail.com")
                                     .withPassword(password)
                                     .build();

        Map<String, Object> map = new HashMap<>();
        map.put("subscriptionType", "content");
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> sub_list = new HashMap<>();
        sub_list.put("name", "frequency");
        sub_list.put("value", "D");
        list.add(sub_list);
        map.put("subscriptionParameterList", list);

        context.restoreAuthSystemState();

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEPerson).perform(post("/api/core/subscriptions")
                               .param("resource", collection.getID().toString())
                               .param("eperson_id", user.getID().toString())
                               .content(new ObjectMapper().writeValueAsString(map))
                               .contentType(MediaType.APPLICATION_JSON_VALUE))
                               .andExpect(status().isForbidden());
    }

    @Test
    public void deleteSubscriptionUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("D");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context,
                                    "content", collection, eperson, subscriptionParameterList).build();
        context.restoreAuthSystemState();

        getClient().perform(delete("/api/core/subscriptions/" + subscription.getID()))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void deleteSubscriptionAdminTest() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("D");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription =  SubscribeBuilder.subscribeBuilder(context,
                                     "content", collection, eperson, subscriptionParameterList).build();
        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(delete("/api/core/subscriptions/" + subscription.getID()))
                             .andExpect(status().isNoContent());

        getClient(tokenAdmin).perform(get("/api/core/subscriptions/" + subscription.getID()))
                             .andExpect(status().isNotFound());
    }

    @Test
    public void deleteSubscriptionForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("D");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription =  SubscribeBuilder.subscribeBuilder(context,
                                     "content", collection, admin, subscriptionParameterList).build();
        context.restoreAuthSystemState();

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEPerson).perform(delete("/api/core/subscriptions/" + subscription.getID()))
                               .andExpect(status().isForbidden());
    }

    @Test
    public void deleteSubscriptionNotFoundTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(delete("/api/core/subscriptions/" + Integer.MAX_VALUE))
                             .andExpect(status().isNotFound());
    }

    @Test
    public void putSubscriptionUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("Parameter1");
        subscriptionParameter.setValue("ValueParameter1");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context,
                "content", collection, admin, subscriptionParameterList).build();
        context.restoreAuthSystemState();

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> newSubscription = new HashMap<>();
        newSubscription.put("subscriptionType", "content");
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> sub_list = new HashMap<>();
        sub_list.put("name", "frequency");
        sub_list.put("value", "daily");
        list.add(sub_list);
        newSubscription.put("subscriptionParameterList", list);

        getClient().perform(put("/api/core/subscriptions/" + subscription.getID())
                   .content(objectMapper.writeValueAsString(newSubscription))
                   .contentType(MediaType.APPLICATION_JSON_VALUE))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void putSubscriptionForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("D");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context,
                                    "content", collection, admin, subscriptionParameterList).build();
        context.restoreAuthSystemState();

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> newSubscription = new HashMap<>();
        newSubscription.put("subscriptionType", "content");
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> sub_list = new HashMap<>();
        sub_list.put("name", "frequency");
        sub_list.put("value", "W");
        list.add(sub_list);
        newSubscription.put("subscriptionParameterList", list);

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(put("/api/core/subscriptions/" + subscription.getID())
                        .content(objectMapper.writeValueAsString(newSubscription))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                        .andExpect(status().isForbidden());
    }

    @Test
    public void putSubscriptionTest() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("D");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context,
                                    "content", collection, eperson, subscriptionParameterList).build();
        context.restoreAuthSystemState();

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> newSubscription = new HashMap<>();
        newSubscription.put("subscriptionType", "content");
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> sub_list = new HashMap<>();
        sub_list.put("name", "frequency");
        sub_list.put("value", "W");
        list.add(sub_list);
        newSubscription.put("subscriptionParameterList", list);

        String tokenSubscriber = getAuthToken(eperson.getEmail(), password);
        getClient(tokenSubscriber).perform(put("/api/core/subscriptions/" + subscription.getID())
                                  .content(objectMapper.writeValueAsString(newSubscription))
                                  .contentType(MediaType.APPLICATION_JSON_VALUE))
                                  .andExpect(status().isOk())
                                  .andExpect(content().contentType(contentType))
                                  .andExpect(jsonPath("$.subscriptionType", is("content")))
                                  .andExpect(jsonPath("$.subscriptionParameterList[0].name", is("frequency")))
                                  .andExpect(jsonPath("$.subscriptionParameterList[0].value", is("W")))
                                  .andExpect(jsonPath("$._links.eperson.href", Matchers.endsWith("/eperson")))
                                  .andExpect(jsonPath("$._links.resource.href",Matchers.endsWith("/resource")));
    }

    @Test
    public void putSubscriptionInvalidSubscriptionParameterNameTest() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("D");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context,
                                    "content", collection, eperson, subscriptionParameterList).build();
        context.restoreAuthSystemState();

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> newSubscription = new HashMap<>();
        newSubscription.put("subscriptionType", "content");
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> sub_list = new HashMap<>();
        sub_list.put("name", "InvalidName");
        sub_list.put("value", "W");
        list.add(sub_list);
        newSubscription.put("subscriptionParameterList", list);

        String tokenSubscriber = getAuthToken(eperson.getEmail(), password);
        getClient(tokenSubscriber).perform(put("/api/core/subscriptions/" + subscription.getID())
                                  .content(objectMapper.writeValueAsString(newSubscription))
                                  .contentType(MediaType.APPLICATION_JSON_VALUE))
                                  .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void putSubscriptionInvalidSubscriptionParameterValueTest() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("D");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context,
                                    "content", collection, eperson, subscriptionParameterList).build();
        context.restoreAuthSystemState();

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> newSubscription = new HashMap<>();
        newSubscription.put("subscriptionType", "content");
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> sub_list = new HashMap<>();
        sub_list.put("name", "frequency");
        sub_list.put("value", "Y");
        list.add(sub_list);
        newSubscription.put("subscriptionParameterList", list);

        String tokenSubscriber = getAuthToken(eperson.getEmail(), password);
        getClient(tokenSubscriber).perform(put("/api/core/subscriptions/" + subscription.getID())
                                  .content(objectMapper.writeValueAsString(newSubscription))
                                  .contentType(MediaType.APPLICATION_JSON_VALUE))
                                  .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void putSubscriptionInvalidSubscriptionTypeValueTest() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("D");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context,
                                    "content", collection, eperson, subscriptionParameterList).build();
        context.restoreAuthSystemState();

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> newSubscription = new HashMap<>();
        newSubscription.put("subscriptionType", "InvalidType");
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> sub_list = new HashMap<>();
        sub_list.put("name", "frequency");
        sub_list.put("value", "D");
        list.add(sub_list);
        newSubscription.put("subscriptionParameterList", list);

        String tokenSubscriber = getAuthToken(eperson.getEmail(), password);
        getClient(tokenSubscriber).perform(put("/api/core/subscriptions/" + subscription.getID())
                                  .content(objectMapper.writeValueAsString(newSubscription))
                                  .contentType(MediaType.APPLICATION_JSON_VALUE))
                                  .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void putSubscriptionAdminTest() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("D");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context,
                                    "content", collection, eperson, subscriptionParameterList).build();
        context.restoreAuthSystemState();

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> newSubscription = new HashMap<>();
        newSubscription.put("subscriptionType", "content");
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> sub_list = new HashMap<>();
        sub_list.put("name", "frequency");
        sub_list.put("value", "W");
        list.add(sub_list);
        newSubscription.put("subscriptionParameterList", list);

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(put("/api/core/subscriptions/" + subscription.getID())
                             .content(objectMapper.writeValueAsString(newSubscription))
                             .contentType(MediaType.APPLICATION_JSON_VALUE))
                             .andExpect(status().isOk())
                             .andExpect(content().contentType(contentType))
                             .andExpect(jsonPath("$.subscriptionType", is("content")))
                             .andExpect(jsonPath("$.subscriptionParameterList[0].name", is("frequency")))
                             .andExpect(jsonPath("$.subscriptionParameterList[0].value", is("W")))
                             .andExpect(jsonPath("$._links.eperson.href", Matchers.endsWith("/eperson")))
                             .andExpect(jsonPath("$._links.resource.href", Matchers.endsWith("/resource")));
    }

    @Test
    public void linkedEpersonOfSubscriptionAdminTest() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("M");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context,
                                    "content", collection, eperson, subscriptionParameterList).build();
        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/subscriptions/" + subscription.getID() + "/eperson"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", is(EPersonMatcher.matchEPersonEntry(eperson))));
    }

    @Test
    public void linkedEpersonOfSubscriptionTest() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("M");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context,
                                    "content", collection, eperson, subscriptionParameterList).build();
        context.restoreAuthSystemState();

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEPerson).perform(get("/api/core/subscriptions/" + subscription.getID() + "/eperson"))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$", is(EPersonMatcher.matchEPersonEntry(eperson))));
    }

    @Test
    public void linkedEpersonOfSubscriptionUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("M");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context,
                                    "content", collection, eperson, subscriptionParameterList).build();
        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/subscriptions/" + subscription.getID() + "/eperson"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void linkedEpersonOfSubscriptionForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("W");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context,
                                    "content", collection, admin, subscriptionParameterList).build();
        context.restoreAuthSystemState();

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEPerson).perform(get("/api/core/subscriptions/" + subscription.getID() + "/eperson"))
                               .andExpect(status().isForbidden());
    }

    @Test
    public void linkedEpersonOfSubscriptionNotFoundTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/subscriptions/" + Integer.MAX_VALUE + "/eperson"))
                             .andExpect(status().isNotFound());
    }

    @Test
    public void linkedDSpaceObjectOfSubscriptionAdminTest() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("W");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context,
                                    "content", collection, eperson, subscriptionParameterList).build();
        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/subscriptions/" + subscription.getID() + "/resource"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.uuid", Matchers.is(collection.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(collection.getName())))
                             .andExpect(jsonPath("$.type", Matchers.is("collection")));
    }

    @Test
    public void linkedDSpaceObjectOfSubscriptionTest() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("W");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context,
                                    "content", collection, eperson, subscriptionParameterList).build();
        context.restoreAuthSystemState();

        String tokenAdmin = getAuthToken(eperson.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/subscriptions/" + subscription.getID() + "/resource"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.uuid", Matchers.is(collection.getID().toString())))
                             .andExpect(jsonPath("$.name", Matchers.is(collection.getName())))
                             .andExpect(jsonPath("$.type", Matchers.is("collection")));
    }

    @Test
    public void linkedDSpaceObjectOfSubscriptionUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("W");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context,
                                    "content", collection, eperson, subscriptionParameterList).build();
        context.restoreAuthSystemState();

        getClient().perform(get("/api/core/subscriptions/" + subscription.getID() + "/resource"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void linkedDSpaceObjectOfSubscriptionForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("D");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context,
                                    "content", collection, admin, subscriptionParameterList).build();
        context.restoreAuthSystemState();

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEPerson).perform(get("/api/core/subscriptions/" + subscription.getID() + "/resource"))
                               .andExpect(status().isForbidden());
    }

    @Test
    public void linkedDSpaceObjectAndRestrictedAccessAfterYouHaveSubscriptionToItTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1 Test")
                                           .withSubmitterGroup(eperson)
                                           .build();

        List<SubscriptionParameter> subscriptionParameterList = new ArrayList<>();
        SubscriptionParameter subscriptionParameter = new SubscriptionParameter();
        subscriptionParameter.setName("frequency");
        subscriptionParameter.setValue("D");
        subscriptionParameterList.add(subscriptionParameter);
        Subscription subscription = SubscribeBuilder.subscribeBuilder(context,
                                    "content", col1, eperson, subscriptionParameterList).build();
        context.restoreAuthSystemState();

        String tokenEPerson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEPerson).perform(get("/api/core/subscriptions/" + subscription.getID() + "/resource"))
                               .andExpect(status().isOk())
                               .andExpect(jsonPath("$.uuid", Matchers.is(col1.getID().toString())))
                               .andExpect(jsonPath("$.name", Matchers.is(col1.getName())))
                               .andExpect(jsonPath("$.type", Matchers.is("collection")));

        context.turnOffAuthorisationSystem();
        // remove all policies for col1
        resourcePolicyService.removeAllPolicies(context, col1);
        context.restoreAuthSystemState();

        // prove that col1 become not accessible
        getClient(tokenEPerson).perform(get("/api/core/subscriptions/" + subscription.getID() + "/resource"))
                               .andExpect(status().isNoContent());
    }

    @Test
    public void linkedDSpaceObjectOfSubscriptionNotFoundTest() throws Exception {
        String tokenAdmin = getAuthToken(admin.getEmail(), password);
        getClient(tokenAdmin).perform(get("/api/core/subscriptions/" + Integer.MAX_VALUE + "/resource"))
                             .andExpect(status().isNotFound());
    }

}