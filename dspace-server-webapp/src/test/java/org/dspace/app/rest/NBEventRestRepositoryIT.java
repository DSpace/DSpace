/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.MediaType;

import org.dspace.app.rest.matcher.ItemMatcher;
import org.dspace.app.rest.matcher.NBEventMatcher;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.NBEventBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
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
    public void findOneWithProjectionTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        NBEvent event1 = NBEventBuilder.createTarget(context, col1, "Science and Freedom")
                .withTopic("ENRICH/MISSING/PID")
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}").build();
        NBEvent event5 = NBEventBuilder.createTarget(context, col1, "Science and Freedom 5")
                .withTopic("ENRICH/MISSING/PROJECT")
                .withMessage(
                        "{\"projects[0].acronym\":\"PAThs\","
                        + "\"projects[0].code\":\"687567\","
                        + "\"projects[0].funder\":\"EC\","
                        + "\"projects[0].fundingProgram\":\"H2020\","
                        + "\"projects[0].jurisdiction\":\"EU\","
                        + "\"projects[0].openaireId\":\"40|corda__h2020::6e32f5eb912688f2424c68b851483ea4\","
                        + "\"projects[0].title\":\"Tracking Papyrus and Parchment Paths: "
                        + "An Archaeological Atlas of Coptic Literature."
                        + "\\nLiterary Texts in their Geographical Context: Production, Copying, Usage, "
                        + "Dissemination and Storage\"}")
                .build();
        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
                .perform(get("/api/integration/nbevents/" + event1.getEventId()).param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", NBEventMatcher.matchNBEventFullEntry(event1)));
        getClient(authToken)
                .perform(get("/api/integration/nbevents/" + event5.getEventId()).param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", NBEventMatcher.matchNBEventFullEntry(event5)));
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
    public void findByTopicPaginatedTest() throws Exception {
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
                .withTopic("ENRICH/MISSING/PID")
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144302\"}").build();
        NBEvent event4 = NBEventBuilder.createTarget(context, col1, "Science and Freedom 4")
                .withTopic("ENRICH/MISSING/PID")
                .withMessage("{\"pids[0].type\":\"pmc\",\"pids[0].value\":\"2144303\"}").build();
        NBEvent event5 = NBEventBuilder.createTarget(context, col1, "Science and Freedom 5")
                .withTopic("ENRICH/MISSING/PID")
                .withMessage("{\"pids[0].type\":\"pmid\",\"pids[0].value\":\"2144304\"}").build();
        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
                .perform(get("/api/integration/nbevents/search/findByTopic").param("topic", "ENRICH!MISSING!PID")
                        .param("size", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.nbevents", Matchers.hasSize(2)))
                .andExpect(jsonPath("$._embedded.nbevents",
                        Matchers.containsInAnyOrder(
                                NBEventMatcher.matchNBEventEntry(event1),
                                NBEventMatcher.matchNBEventEntry(event2))))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/nbevents/search/findByTopic?"),
                                Matchers.containsString("topic=ENRICH!MISSING!PID"),
                                Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.next.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/nbevents/search/findByTopic?"),
                                Matchers.containsString("topic=ENRICH!MISSING!PID"), Matchers.containsString("page=1"),
                                Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.last.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/nbevents/search/findByTopic?"),
                                Matchers.containsString("topic=ENRICH!MISSING!PID"), Matchers.containsString("page=2"),
                                Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.first.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/nbevents/search/findByTopic?"),
                                Matchers.containsString("topic=ENRICH!MISSING!PID"), Matchers.containsString("page=0"),
                                Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.prev.href").doesNotExist())
                .andExpect(jsonPath("$.page.size", is(2)))
                .andExpect(jsonPath("$.page.totalPages", is(3)))
                .andExpect(jsonPath("$.page.totalElements", is(5)));

        getClient(authToken)
                .perform(get("/api/integration/nbevents/search/findByTopic").param("topic", "ENRICH!MISSING!PID")
                        .param("size", "2").param("page", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.nbevents", Matchers.hasSize(2)))
                .andExpect(jsonPath("$._embedded.nbevents",
                        Matchers.containsInAnyOrder(
                                NBEventMatcher.matchNBEventEntry(event3),
                                NBEventMatcher.matchNBEventEntry(event4))))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/nbevents/search/findByTopic?"),
                                Matchers.containsString("topic=ENRICH!MISSING!PID"), Matchers.containsString("page=1"),
                                Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.next.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/nbevents/search/findByTopic?"),
                                Matchers.containsString("topic=ENRICH!MISSING!PID"), Matchers.containsString("page=2"),
                                Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.last.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/nbevents/search/findByTopic?"),
                                Matchers.containsString("topic=ENRICH!MISSING!PID"), Matchers.containsString("page=2"),
                                Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.first.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/nbevents/search/findByTopic?"),
                                Matchers.containsString("topic=ENRICH!MISSING!PID"), Matchers.containsString("page=0"),
                                Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.prev.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/nbevents/search/findByTopic?"),
                                Matchers.containsString("topic=ENRICH!MISSING!PID"), Matchers.containsString("page=0"),
                                Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$.page.size", is(2)))
                .andExpect(jsonPath("$.page.totalPages", is(3)))
                .andExpect(jsonPath("$.page.totalElements", is(5)));

        getClient(authToken)
            .perform(get("/api/integration/nbevents/search/findByTopic").param("topic", "ENRICH!MISSING!PID")
                    .param("size", "2").param("page", "2"))
            .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.nbevents", Matchers.hasSize(1)))
            .andExpect(jsonPath("$._embedded.nbevents",
                    Matchers.containsInAnyOrder(
                            NBEventMatcher.matchNBEventEntry(event5))))
            .andExpect(jsonPath("$._links.self.href",
                    Matchers.allOf(
                            Matchers.containsString("/api/integration/nbevents/search/findByTopic?"),
                            Matchers.containsString("topic=ENRICH!MISSING!PID"), Matchers.containsString("page=2"),
                            Matchers.containsString("size=2"))))
            .andExpect(jsonPath("$._links.next.href").doesNotExist())
            .andExpect(jsonPath("$._links.last.href",
                    Matchers.allOf(
                            Matchers.containsString("/api/integration/nbevents/search/findByTopic?"),
                            Matchers.containsString("topic=ENRICH!MISSING!PID"), Matchers.containsString("page=2"),
                            Matchers.containsString("size=2"))))
            .andExpect(jsonPath("$._links.first.href",
                    Matchers.allOf(
                            Matchers.containsString("/api/integration/nbevents/search/findByTopic?"),
                            Matchers.containsString("topic=ENRICH!MISSING!PID"), Matchers.containsString("page=0"),
                            Matchers.containsString("size=2"))))
            .andExpect(jsonPath("$._links.prev.href",
                    Matchers.allOf(
                            Matchers.containsString("/api/integration/nbevents/search/findByTopic?"),
                            Matchers.containsString("topic=ENRICH!MISSING!PID"), Matchers.containsString("page=1"),
                            Matchers.containsString("size=2"))))
            .andExpect(jsonPath("$.page.size", is(2)))
            .andExpect(jsonPath("$.page.totalPages", is(3)))
            .andExpect(jsonPath("$.page.totalElements", is(5)));

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

    @Test
    public void recordDecisionTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        Collection colFunding = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection Fundings").withRelationshipType("Funding").build();
        Item funding = ItemBuilder.createItem(context, colFunding).withTitle("Tracking Papyrus and Parchment Paths")
                .build();
        NBEvent eventProjectBound = NBEventBuilder.createTarget(context, col1, "Science and Freedom with project")
                .withTopic("ENRICH/MISSING/PROJECT")
                .withMessage(
                        "{\"projects[0].acronym\":\"PAThs\","
                        + "\"projects[0].code\":\"687567\","
                        + "\"projects[0].funder\":\"EC\","
                        + "\"projects[0].fundingProgram\":\"H2020\","
                        + "\"projects[0].jurisdiction\":\"EU\","
                        + "\"projects[0].openaireId\":\"40|corda__h2020::6e32f5eb912688f2424c68b851483ea4\","
                        + "\"projects[0].title\":\"Tracking Papyrus and Parchment Paths: "
                        + "An Archaeological Atlas of Coptic Literature."
                        + "\\nLiterary Texts in their Geographical Context: Production, Copying, Usage, "
                        + "Dissemination and Storage\"}")
                .withRelatedItem(funding.getID().toString())
                .build();
        NBEvent eventProjectNoBound = NBEventBuilder
                .createTarget(context, col1, "Science and Freedom with unrelated project")
                .withTopic("ENRICH/MISSING/PROJECT")
                .withMessage(
                        "{\"projects[0].acronym\":\"NEW\","
                        + "\"projects[0].code\":\"123456\","
                        + "\"projects[0].funder\":\"EC\","
                        + "\"projects[0].fundingProgram\":\"H2020\","
                        + "\"projects[0].jurisdiction\":\"EU\","
                        + "\"projects[0].openaireId\":\"newProjectID\","
                        + "\"projects[0].title\":\"A new project\"}")
                .build();
        NBEvent eventMissingPID1 = NBEventBuilder.createTarget(context, col1, "Science and Freedom")
                .withTopic("ENRICH/MISSING/PID")
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}").build();
        NBEvent eventMissingPID2 = NBEventBuilder.createTarget(context, col1, "Science and Freedom 2")
                .withTopic("ENRICH/MISSING/PID")
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144301\"}").build();
        NBEvent eventMissingUnknownPID = NBEventBuilder.createTarget(context, col1, "Science and Freedom URN PID")
                .withTopic("ENRICH/MISSING/PID")
                .withMessage(
                        "{\"pids[0].type\":\"urn\",\"pids[0].value\":\"http://thesis2.sba.units.it/store/handle/item/12937\"}")
                .build();
        NBEvent eventMorePID = NBEventBuilder.createTarget(context, col1, "Science and Freedom 3")
                .withTopic("ENRICH/MORE/PID")
                .withMessage("{\"pids[0].type\":\"pmid\",\"pids[0].value\":\"2144302\"}").build();
        NBEvent eventAbstract = NBEventBuilder.createTarget(context, col1, "Science and Freedom 4")
                .withTopic("ENRICH/MISSING/ABSTRACT")
                .withMessage("{\"abstracts[0]\": \"An abstract to add...\"}").build();
        NBEvent eventAbstractToDiscard = NBEventBuilder.createTarget(context, col1, "Science and Freedom 7")
                .withTopic("ENRICH/MISSING/ABSTRACT")
                .withMessage("{\"abstracts[0]\": \"Abstract to discard...\"}").build();
        context.restoreAuthSystemState();
        // prepare the different patches for our decisions
        List<Operation> acceptOp = new ArrayList<Operation>();
        acceptOp.add(new ReplaceOperation("/status", NBEvent.ACCEPTED));
        List<Operation> acceptOpUppercase = new ArrayList<Operation>();
        acceptOpUppercase.add(new ReplaceOperation("/status", NBEvent.ACCEPTED));
        List<Operation> discardOp = new ArrayList<Operation>();
        discardOp.add(new ReplaceOperation("/status", NBEvent.DISCARDED));
        List<Operation> rejectOp = new ArrayList<Operation>();
        rejectOp.add(new ReplaceOperation("/status", NBEvent.REJECTED));
        String patchAccept = getPatchContent(acceptOp);
        String patchAcceptUppercase = getPatchContent(acceptOpUppercase);
        String patchDiscard = getPatchContent(discardOp);
        String patchReject = getPatchContent(rejectOp);

        String authToken = getAuthToken(admin.getEmail(), password);
        // accept pid1, unknownPID, morePID, the two projects and abstract
        eventMissingPID1.setStatus(NBEvent.ACCEPTED);
        eventMorePID.setStatus(NBEvent.ACCEPTED);
        eventMissingUnknownPID.setStatus(NBEvent.ACCEPTED);
        eventMissingUnknownPID.setStatus(NBEvent.ACCEPTED);
        eventProjectBound.setStatus(NBEvent.ACCEPTED);
        eventProjectNoBound.setStatus(NBEvent.ACCEPTED);
        eventAbstract.setStatus(NBEvent.ACCEPTED);

        getClient(authToken).perform(patch("/api/integration/nbevents/" + eventMissingPID1.getEventId())
                .content(patchAccept)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", NBEventMatcher.matchNBEventEntry(eventMissingPID1)));
        getClient(authToken).perform(patch("/api/integration/nbevents/" + eventMorePID.getEventId())
                .content(patchAcceptUppercase)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", NBEventMatcher.matchNBEventEntry(eventMorePID)));
        getClient(authToken).perform(patch("/api/integration/nbevents/" + eventMissingUnknownPID.getEventId())
                .content(patchAccept)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", NBEventMatcher.matchNBEventEntry(eventMissingUnknownPID)));
        getClient(authToken).perform(patch("/api/integration/nbevents/" + eventProjectBound.getEventId())
                .content(patchAccept)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", NBEventMatcher.matchNBEventEntry(eventProjectBound)));
        getClient(authToken).perform(patch("/api/integration/nbevents/" + eventProjectNoBound.getEventId())
                .content(patchAccept)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", NBEventMatcher.matchNBEventEntry(eventProjectNoBound)));
        getClient(authToken).perform(patch("/api/integration/nbevents/" + eventAbstract.getEventId())
                .content(patchAccept)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", NBEventMatcher.matchNBEventEntry(eventAbstract)));
        // check if the item has been updated
        getClient(authToken).perform(get("/api/core/items/" + eventMissingPID1.getTarget())
                    .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$", hasJsonPath("$.metadata['dc.identifier.doi'][0].value", is("10.2307/2144300"))));
        getClient(authToken).perform(get("/api/core/items/" + eventMorePID.getTarget())
                    .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasJsonPath("$.metadata['dc.identifier.pmid'][0].value", is("2144302"))));
        getClient(authToken).perform(get("/api/core/items/" + eventMissingUnknownPID.getTarget())
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasJsonPath("$.metadata['dc.identifier'][0].value",
                        is("http://thesis2.sba.units.it/store/handle/item/12937"))));
        getClient(authToken).perform(get("/api/core/items/" + eventProjectBound.getTarget())
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",
                        hasJsonPath("$.metadata['dc.relation.funding'][0].value",
                                is("Tracking Papyrus and Parchment Paths"))))
                .andExpect(jsonPath("$",
                        hasJsonPath("$.metadata['dc.relation.funding'][0].authority", is(funding.getID().toString()))));
        getClient(authToken).perform(get("/api/core/items/" + eventProjectNoBound.getTarget())
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$",
                    hasJsonPath("$.metadata['dc.relation.funding'][0].value",
                            is("A new project"))))
            .andExpect(jsonPath("$",
                    hasJsonPath("$.metadata['dc.relation.funding'][0].authority", is(not(empty())))));
        getClient(authToken).perform(get("/api/core/items/" + eventAbstract.getTarget())
                    .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",
                        hasJsonPath("$.metadata['dc.description.abstract'][0].value", is("An abstract to add..."))));
        // reject pid2
        eventMissingPID2.setStatus(NBEvent.REJECTED);
        getClient(authToken).perform(patch("/api/integration/nbevents/" + eventMissingPID2.getEventId())
                .content(patchReject)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", NBEventMatcher.matchNBEventEntry(eventMissingPID2)));
        getClient(authToken).perform(get("/api/core/items/" + eventMissingPID2.getTarget())
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$",
                    hasNoJsonPath("$.metadata['dc.identifier.doi']")));
        // discard abstractToDiscard
        eventAbstractToDiscard.setStatus(NBEvent.DISCARDED);
        getClient(authToken).perform(patch("/api/integration/nbevents/" + eventAbstractToDiscard.getEventId())
                .content(patchDiscard)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", NBEventMatcher.matchNBEventEntry(eventAbstractToDiscard)));
        getClient(authToken).perform(get("/api/core/items/" + eventMissingPID2.getTarget())
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$",
                    hasNoJsonPath("$.metadata['dc.description.abstract']")));
        // no pending nb events should be longer available
        getClient(authToken).perform(get("/api/integration/nbtopics")).andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.size", is(20))).andExpect(jsonPath("$.page.totalElements", is(0)));
        // we should have stored the decision into the database as well
    }

    @Test
    public void setRelatedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        Collection colFunding = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection Fundings").withRelationshipType("Funding").build();
        NBEvent event = NBEventBuilder.createTarget(context, col1, "Science and Freedom 5")
                .withTopic("ENRICH/MISSING/PROJECT")
                .withMessage(
                        "{\"projects[0].acronym\":\"PAThs\","
                        + "\"projects[0].code\":\"687567\","
                        + "\"projects[0].funder\":\"EC\","
                        + "\"projects[0].fundingProgram\":\"H2020\","
                        + "\"projects[0].jurisdiction\":\"EU\","
                        + "\"projects[0].openaireId\":\"40|corda__h2020::6e32f5eb912688f2424c68b851483ea4\","
                        + "\"projects[0].title\":\"Tracking Papyrus and Parchment Paths: "
                        + "An Archaeological Atlas of Coptic Literature."
                        + "\\nLiterary Texts in their Geographical Context: Production, Copying, Usage, "
                        + "Dissemination and Storage\"}")
                .build();
        Item funding = ItemBuilder.createItem(context, colFunding).withTitle("Tracking Papyrus and Parchment Paths")
                .build();
        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
                .perform(get("/api/integration/nbevents/" + event.getEventId()).param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", NBEventMatcher.matchNBEventFullEntry(event)));

        getClient(authToken)
                .perform(post("/api/integration/nbevents/" + event.getEventId() + "/related").param("item",
                        funding.getID().toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", ItemMatcher.matchItemProperties(funding)));
        // update our local event copy to reflect the association with the related item
        event.setRelated(funding.getID().toString());
        getClient(authToken)
                .perform(get("/api/integration/nbevents/" + event.getEventId()).param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", NBEventMatcher.matchNBEventFullEntry(event)));
        getClient(authToken)
                .perform(get("/api/integration/nbevents/" + event.getEventId() + "/related"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", ItemMatcher.matchItemProperties(funding)));
    }

    @Test
    public void unsetRelatedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        Collection colFunding = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection Fundings").withRelationshipType("Funding").build();
        Item funding = ItemBuilder.createItem(context, colFunding).withTitle("Tracking Papyrus and Parchment Paths")
                .build();
        NBEvent event = NBEventBuilder.createTarget(context, col1, "Science and Freedom 5")
                .withTopic("ENRICH/MISSING/PROJECT")
                .withMessage(
                        "{\"projects[0].acronym\":\"PAThs\","
                        + "\"projects[0].code\":\"687567\","
                        + "\"projects[0].funder\":\"EC\","
                        + "\"projects[0].fundingProgram\":\"H2020\","
                        + "\"projects[0].jurisdiction\":\"EU\","
                        + "\"projects[0].openaireId\":\"40|corda__h2020::6e32f5eb912688f2424c68b851483ea4\","
                        + "\"projects[0].title\":\"Tracking Papyrus and Parchment Paths: "
                        + "An Archaeological Atlas of Coptic Literature."
                        + "\\nLiterary Texts in their Geographical Context: Production, Copying, Usage, "
                        + "Dissemination and Storage\"}")
                .withRelatedItem(funding.getID().toString())
                .build();
        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
                .perform(get("/api/integration/nbevents/" + event.getEventId()).param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", NBEventMatcher.matchNBEventFullEntry(event)));
        getClient(authToken)
                .perform(delete("/api/integration/nbevents/" + event.getEventId() + "/related"))
                .andExpect(status().isNoContent());

        // update our local event copy to reflect the association with the related item
        event.setRelated(null);
        getClient(authToken)
                .perform(get("/api/integration/nbevents/" + event.getEventId()).param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", NBEventMatcher.matchNBEventFullEntry(event)));
        getClient(authToken)
            .perform(get("/api/integration/nbevents/" + event.getEventId() + "/related"))
            .andExpect(status().isNoContent());
    }

    @Test
    public void setInvalidRelatedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        Collection colFunding = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection Fundings").withRelationshipType("Funding").build();
        NBEvent event = NBEventBuilder.createTarget(context, col1, "Science and Freedom")
                .withTopic("ENRICH/MISSING/PID")
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}").build();
        Item funding = ItemBuilder.createItem(context, colFunding).withTitle("Tracking Papyrus and Parchment Paths")
                .build();
        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
                .perform(get("/api/integration/nbevents/" + event.getEventId()).param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", NBEventMatcher.matchNBEventFullEntry(event)));

        getClient(authToken)
                .perform(post("/api/integration/nbevents/" + event.getEventId() + "/related").param("item",
                        funding.getID().toString()))
                .andExpect(status().isBadRequest());
        // check that no related item has been added to our event
        getClient(authToken)
                .perform(get("/api/integration/nbevents/" + event.getEventId()).param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", NBEventMatcher.matchNBEventFullEntry(event)));
    }
}
