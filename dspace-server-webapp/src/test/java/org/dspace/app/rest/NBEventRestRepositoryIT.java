/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.matcher.NBEventMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.NBEventBuilder;
import org.dspace.content.Collection;
import org.dspace.content.NBEvent;
import org.hamcrest.Matchers;
import org.junit.Test;

public class NBEventRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    public void findAllNotImplementedTest() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/integration/nbevents")).andExpect(status().isMethodNotAllowed());
        String epersonToken = getAuthToken(admin.getEmail(), password);
        getClient(epersonToken).perform(get("/api/integration/nbevents")).andExpect(status().isMethodNotAllowed());
        getClient().perform(get("/api/integration/nbevents")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void findOneTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        NBEvent event1 = NBEventBuilder.createTarget(context, col1, "Science and Freedom")
                .withTopic("ENRICH/MISSING/PID")
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}").build();
        NBEvent event4 = NBEventBuilder.createTarget(context, col1, "Science and Freedom 4")
                .withTopic("ENRICH/MISSING/ABSTRACT")
                .withMessage("{\"abstracts[0]\": \"Descrizione delle caratteristiche...\"}").build();
        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/integration/nbevents/" + event1.getEventId())).andExpect(status().isOk())
                .andExpect(jsonPath("$", NBEventMatcher.matchNBEventEntry(event1)));
        getClient(authToken).perform(get("/api/integration/nbevents/" + event4.getEventId())).andExpect(status().isOk())
                .andExpect(jsonPath("$", NBEventMatcher.matchNBEventEntry(event4)));
    }

    @Test
    public void findOneUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        NBEvent event1 = NBEventBuilder.createTarget(context, col1, "Science and Freedom")
                .withTopic("ENRICH/MISSING/PID")
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}").build();
        context.restoreAuthSystemState();
        getClient().perform(get("/api/integration/nbevents/" + event1.getEventId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        NBEvent event1 = NBEventBuilder.createTarget(context, col1, "Science and Freedom")
                .withTopic("ENRICH/MISSING/PID")
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}").build();
        context.restoreAuthSystemState();
        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(get("/api/integration/nbevents/" + event1.getEventId()))
                .andExpect(status().isForbidden());
    }

    @Test
    public void findByTopicTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        NBEvent event1 = NBEventBuilder.createTarget(context, col1, "Science and Freedom")
                .withTopic("ENRICH/MISSING/PID")
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}").build();
        NBEvent event2 = NBEventBuilder.createTarget(context, col1, "Science and Freedom 2")
                .withTopic("ENRICH/MISSING/PID")
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144301\"}").build();
        NBEvent event3 = NBEventBuilder.createTarget(context, col1, "Science and Freedom 3")
                .withTopic("ENRICH/MORE/PID")
                .withMessage("{\"pids[0].type\":\"pmid\",\"pids[0].value\":\"10.2307/2144302\"}").build();
        NBEvent event4 = NBEventBuilder.createTarget(context, col1, "Science and Freedom 4")
                .withTopic("ENRICH/MISSING/ABSTRACT")
                .withMessage("{\"abstracts[0]\": \"Descrizione delle caratteristiche...\"}").build();
        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
                .perform(get("/api/integration/nbevents/search/findByTopic").param("topic", "ENRICH!MISSING!PID"))
                .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.nbevents", Matchers.hasSize(2)))
                .andExpect(jsonPath("$._embedded.nbevents",
                        Matchers.containsInAnyOrder(NBEventMatcher.matchNBEventEntry(event1),
                                NBEventMatcher.matchNBEventEntry(event2))))
                .andExpect(jsonPath("$.page.size", is(20))).andExpect(jsonPath("$.page.totalElements", is(2)));
        getClient(authToken)
                .perform(get("/api/integration/nbevents/search/findByTopic").param("topic", "ENRICH!MISSING!ABSTRACT"))
                .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.nbevents", Matchers.hasSize(1)))
                .andExpect(jsonPath("$._embedded.nbevents",
                        Matchers.containsInAnyOrder(NBEventMatcher.matchNBEventEntry(event4))))
                .andExpect(jsonPath("$.page.size", is(20))).andExpect(jsonPath("$.page.totalElements", is(1)));
        getClient(authToken).perform(get("/api/integration/nbevents/search/findByTopic").param("topic", "not-existing"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void findByTopicUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        NBEvent event1 = NBEventBuilder.createTarget(context, col1, "Science and Freedom")
                .withTopic("ENRICH/MISSING/PID")
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}").build();
        NBEvent event2 = NBEventBuilder.createTarget(context, col1, "Science and Freedom 2")
                .withTopic("ENRICH/MISSING/PID")
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144301\"}").build();
        NBEvent event3 = NBEventBuilder.createTarget(context, col1, "Science and Freedom 3")
                .withTopic("ENRICH/MORE/PID")
                .withMessage("{\"pids[0].type\":\"pmid\",\"pids[0].value\":\"10.2307/2144302\"}").build();
        NBEvent event4 = NBEventBuilder.createTarget(context, col1, "Science and Freedom 4")
                .withTopic("ENRICH/MISSING/ABSTRACT")
                .withMessage("{\"abstracts[0]\": \"Descrizione delle caratteristiche...\"}").build();
        context.restoreAuthSystemState();
        getClient().perform(get("/api/integration/nbevents/search/findByTopic").param("topic", "ENRICH!MISSING!PID"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void findByTopicForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        NBEvent event1 = NBEventBuilder.createTarget(context, col1, "Science and Freedom")
                .withTopic("ENRICH/MISSING/PID")
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}").build();
        NBEvent event2 = NBEventBuilder.createTarget(context, col1, "Science and Freedom 2")
                .withTopic("ENRICH/MISSING/PID")
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144301\"}").build();
        NBEvent event3 = NBEventBuilder.createTarget(context, col1, "Science and Freedom 3")
                .withTopic("ENRICH/MORE/PID")
                .withMessage("{\"pids[0].type\":\"pmid\",\"pids[0].value\":\"10.2307/2144302\"}").build();
        NBEvent event4 = NBEventBuilder.createTarget(context, col1, "Science and Freedom 4")
                .withTopic("ENRICH/MISSING/ABSTRACT")
                .withMessage("{\"abstracts[0]\": \"Descrizione delle caratteristiche...\"}").build();
        context.restoreAuthSystemState();
        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken)
                .perform(get("/api/integration/nbevents/search/findByTopic").param("topic", "ENRICH!MISSING!PID"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void findByTopicBadRequestTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        NBEvent event1 = NBEventBuilder.createTarget(context, col1, "Science and Freedom")
                .withTopic("ENRICH/MISSING/PID")
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}").build();
        NBEvent event2 = NBEventBuilder.createTarget(context, col1, "Science and Freedom 2")
                .withTopic("ENRICH/MISSING/PID")
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144301\"}").build();
        NBEvent event3 = NBEventBuilder.createTarget(context, col1, "Science and Freedom 3")
                .withTopic("ENRICH/MORE/PID")
                .withMessage("{\"pids[0].type\":\"pmid\",\"pids[0].value\":\"10.2307/2144302\"}").build();
        NBEvent event4 = NBEventBuilder.createTarget(context, col1, "Science and Freedom 4")
                .withTopic("ENRICH/MISSING/ABSTRACT")
                .withMessage("{\"abstracts[0]\": \"Descrizione delle caratteristiche...\"}").build();
        context.restoreAuthSystemState();
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/integration/nbevents/search/findByTopic"))
                .andExpect(status().isBadRequest());
    }

}
