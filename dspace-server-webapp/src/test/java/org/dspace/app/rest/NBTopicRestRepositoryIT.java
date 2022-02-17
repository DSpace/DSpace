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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.matcher.NBTopicMatcher;
import org.dspace.app.rest.repository.NBTopicRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.NBEventBuilder;
import org.dspace.content.Collection;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link NBTopicRestRepository}.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class NBTopicRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Test
    public void findAllTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        NBEventBuilder.createTarget(context, col1, "Science and Freedom")
                .withTopic("ENRICH/MISSING/PID")
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}").build();
        NBEventBuilder.createTarget(context, col1, "Science and Freedom 2")
                .withTopic("ENRICH/MISSING/PID")
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144301\"}").build();
        NBEventBuilder.createTarget(context, col1, "Science and Freedom 3")
                .withTopic("ENRICH/MORE/PID")
                .withMessage("{\"pids[0].type\":\"pmid\",\"pids[0].value\":\"10.2307/2144302\"}").build();
        NBEventBuilder.createTarget(context, col1, "Science and Freedom 4")
                .withTopic("ENRICH/MISSING/ABSTRACT")
                .withMessage(
                        "{\"abstracts[0]\": \"Descrizione delle caratteristiche...\"}")
                .build();
        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/integration/nbtopics")).andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.nbtopics",
                        Matchers.containsInAnyOrder(NBTopicMatcher.matchNBTopicEntry("ENRICH/MISSING/PID", 2),
                                NBTopicMatcher.matchNBTopicEntry("ENRICH/MISSING/ABSTRACT", 1),
                                NBTopicMatcher.matchNBTopicEntry("ENRICH/MORE/PID", 1))))
                .andExpect(jsonPath("$.page.size", is(20))).andExpect(jsonPath("$.page.totalElements", is(3)));

    }

    @Test
    public void findAllUnauthorizedTest() throws Exception {
        getClient().perform(get("/api/integration/nbtopics")).andExpect(status().isUnauthorized());
    }

    @Test
    public void findAllForbiddenTest() throws Exception {
        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(get("/api/integration/nbtopics")).andExpect(status().isForbidden());
    }

    @Test
    public void findAllPaginationTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        //create collection
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        NBEventBuilder.createTarget(context, col1, "Science and Freedom")
                .withTopic("ENRICH/MISSING/PID")
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}").build();
        NBEventBuilder.createTarget(context, col1, "Science and Freedom 2")
                .withTopic("ENRICH/MISSING/PID")
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144301\"}").build();
        NBEventBuilder.createTarget(context, col1, "Science and Freedom 3")
                .withTopic("ENRICH/MORE/PID")
                .withMessage("{\"pids[0].type\":\"pmid\",\"pids[0].value\":\"10.2307/2144302\"}").build();
        NBEventBuilder.createTarget(context, col1, "Science and Freedom 4")
                .withTopic("ENRICH/MISSING/ABSTRACT")
                .withMessage(
                        "{\"abstracts[0]\": \"Descrizione delle caratteristiche...\"}")
                .build();
        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/integration/nbtopics").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.nbtopics", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.page.size", is(2))).andExpect(jsonPath("$.page.totalElements", is(3)));
        getClient(authToken).perform(get("/api/integration/nbtopics").param("size", "2").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.nbtopics", Matchers.hasSize(1)))
                .andExpect(jsonPath("$.page.size", is(2))).andExpect(jsonPath("$.page.totalElements", is(3)));
    }

    @Test
    public void findOneTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        NBEventBuilder.createTarget(context, col1, "Science and Freedom")
                .withTopic("ENRICH/MISSING/PID")
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}").build();
        NBEventBuilder.createTarget(context, col1, "Science and Freedom 2")
                .withTopic("ENRICH/MISSING/PID")
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144301\"}").build();
        NBEventBuilder.createTarget(context, col1, "Science and Freedom 3")
                .withTopic("ENRICH/MORE/PID")
                .withMessage("{\"pids[0].type\":\"pmid\",\"pids[0].value\":\"10.2307/2144302\"}").build();
        NBEventBuilder.createTarget(context, col1, "Science and Freedom 4")
                .withTopic("ENRICH/MISSING/ABSTRACT")
                .withMessage(
                        "{\"abstracts[0]\": \"Descrizione delle caratteristiche...\"}")
                .build();
        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/integration/nbtopics/ENRICH!MISSING!PID"))
                .andExpect(jsonPath("$", NBTopicMatcher.matchNBTopicEntry("ENRICH/MISSING/PID", 2)));
        getClient(authToken).perform(get("/api/integration/nbtopics/ENRICH!MISSING!ABSTRACT"))
                .andExpect(jsonPath("$", NBTopicMatcher.matchNBTopicEntry("ENRICH/MISSING/ABSTRACT", 1)));
    }

    @Test
    public void findOneUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        NBEventBuilder.createTarget(context, col1, "Science and Freedom")
                .withTopic("ENRICH/MISSING/PID").build();
        context.restoreAuthSystemState();
        getClient().perform(get("/api/integration/nbtopics/ENRICH!MISSING!PID")).andExpect(status().isUnauthorized());
        getClient().perform(get("/api/integration/nbtopics/ENRICH!MISSING!ABSTRACT"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        NBEventBuilder.createTarget(context, col1, "Science and Freedom")
                .withTopic("ENRICH/MISSING/PID").build();
        context.restoreAuthSystemState();
        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(get("/api/integration/nbtopics/ENRICH!MISSING!PID"))
            .andExpect(status().isForbidden());
        getClient(authToken).perform(get("/api/integration/nbtopics/ENRICH!MISSING!ABSTRACT"))
            .andExpect(status().isForbidden());
    }

    @Test
    public void findBySourceTest() throws Exception {
        context.turnOffAuthorisationSystem();
        configurationService.setProperty("nbevent.sources",
            new String[] { "openaire", "test-source", "test-source-2" });
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        NBEventBuilder.createTarget(context, col1, "Science and Freedom")
            .withTopic("ENRICH/MISSING/PID")
            .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}").build();
        NBEventBuilder.createTarget(context, col1, "Science and Freedom 2")
            .withTopic("ENRICH/MISSING/PID")
            .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144301\"}").build();
        NBEventBuilder.createTarget(context, col1, "Science and Freedom 3")
            .withTopic("ENRICH/MORE/PID")
            .withMessage("{\"pids[0].type\":\"pmid\",\"pids[0].value\":\"10.2307/2144302\"}").build();
        NBEventBuilder.createTarget(context, col1, "Science and Freedom 4")
            .withTopic("ENRICH/MISSING/ABSTRACT")
            .withMessage(
                "{\"abstracts[0]\": \"Descrizione delle caratteristiche...\"}")
            .build();
        NBEventBuilder.createTarget(context, col1, "Science and Freedom 5")
            .withTopic("TEST/TOPIC")
            .withSource("test-source")
            .build();
        NBEventBuilder.createTarget(context, col1, "Science and Freedom 6")
            .withTopic("TEST/TOPIC")
            .withSource("test-source")
            .build();
        NBEventBuilder.createTarget(context, col1, "Science and Freedom 7")
            .withTopic("TEST/TOPIC/2")
            .withSource("test-source")
            .build();
        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/integration/nbtopics/search/bySource")
            .param("source", "openaire"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.nbtopics",
                Matchers.containsInAnyOrder(NBTopicMatcher.matchNBTopicEntry("ENRICH/MISSING/PID", 2),
                    NBTopicMatcher.matchNBTopicEntry("ENRICH/MISSING/ABSTRACT", 1),
                    NBTopicMatcher.matchNBTopicEntry("ENRICH/MORE/PID", 1))))
            .andExpect(jsonPath("$.page.size", is(20))).andExpect(jsonPath("$.page.totalElements", is(3)));
        getClient(authToken).perform(get("/api/integration/nbtopics/search/bySource")
            .param("source", "test-source"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.nbtopics",
                Matchers.containsInAnyOrder(NBTopicMatcher.matchNBTopicEntry("TEST/TOPIC/2", 1),
                    NBTopicMatcher.matchNBTopicEntry("TEST/TOPIC", 2))))
            .andExpect(jsonPath("$.page.size", is(20))).andExpect(jsonPath("$.page.totalElements", is(2)));
        getClient(authToken).perform(get("/api/integration/nbtopics/search/bySource")
            .param("source", "test-source-2"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.nbtopics").doesNotExist())
            .andExpect(jsonPath("$.page.size", is(20))).andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void findBySourceUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        NBEventBuilder.createTarget(context, col1, "Science and Freedom")
            .withTopic("ENRICH/MISSING/PID").build();
        context.restoreAuthSystemState();
        getClient().perform(get("/api/integration/nbtopics/search/bySource")
            .param("source", "openaire"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void findBySourceForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        NBEventBuilder.createTarget(context, col1, "Science and Freedom")
            .withTopic("ENRICH/MISSING/PID").build();
        context.restoreAuthSystemState();
        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(get("/api/integration/nbtopics/search/bySource")
            .param("source", "openaire"))
            .andExpect(status().isForbidden());
    }

}
