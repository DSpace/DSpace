/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.content.QAEvent.OPENAIRE_SOURCE;
import static org.dspace.qaevent.service.impl.QAEventServiceImpl.QAEVENTS_SOURCES;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.dspace.app.rest.matcher.QATopicMatcher;
import org.dspace.app.rest.repository.QATopicRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.QAEventBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.QAEvent;
import org.dspace.qaevent.QANotifyPatterns;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link QATopicRestRepository}.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class QATopicRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Test
    public void findAllNotImplementedTest() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/integration/qualityassurancetopics"))
                             .andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void findOneTest() throws Exception {
        context.turnOffAuthorisationSystem();
        configurationService.setProperty(QAEVENTS_SOURCES, new String[] { OPENAIRE_SOURCE, "test-source" });
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom")
                      .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                      .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}")
                      .build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom 2")
                      .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                      .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144301\"}")
                      .build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom 3")
                      .withTopic(QANotifyPatterns.TOPIC_ENRICH_MORE_PID)
                      .withMessage("{\"pids[0].type\":\"pmid\",\"pids[0].value\":\"10.2307/2144302\"}")
                      .build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom 4")
                      .withTopic(org.dspace.qaevent.QANotifyPatterns.TOPIC_ENRICH_MISSING_ABSTRACT)
                      .withMessage("{\"test\": \"Test...\"}")
                      .build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom 4")
                      .withSource("test-source")
                      .withTopic("TOPIC/TEST")
                      .withMessage("{\"abstracts[0]\": \"Descrizione delle caratteristiche...\"}")
                      .build();
        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(
                              get("/api/integration/qualityassurancetopics/" + OPENAIRE_SOURCE + ":ENRICH!MISSING!PID"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", QATopicMatcher.matchQATopicEntry(
                                                      QANotifyPatterns.TOPIC_ENRICH_MISSING_PID, 2)));

        getClient(adminToken).perform(get("/api/integration/qualityassurancetopics/"
                                          + OPENAIRE_SOURCE + ":ENRICH!MISSING!ABSTRACT"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$", QATopicMatcher.matchQATopicEntry(
                                                      QANotifyPatterns.TOPIC_ENRICH_MISSING_ABSTRACT, 1)));

        getClient(adminToken).perform(get("/api/integration/qualityassurancetopics/test-source:TOPIC!TEST"))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$",QATopicMatcher.matchQATopicEntry("test-source", "TOPIC/TEST", 1)));
    }

    @Test
    public void findOneNotFoundTest() throws Exception {
        context.turnOffAuthorisationSystem();
        configurationService.setProperty("qaevent.sources",
                new String[] { QAEvent.OPENAIRE_SOURCE });
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom")
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}").build();
        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);
        // using a wrong id
        getClient(authToken).perform(get("/api/integration/qualityassurancetopics/ENRICH!MISSING!PID"))
                .andExpect(status().isNotFound());
        // using a plausible id related to an unknown source
        getClient(authToken)
                .perform(get("/api/integration/qualityassurancetopics/unknown-source:ENRICH!MISSING!ABSTRACT"))
                .andExpect(status().isNotFound());
        // using a not existing topic
        getClient(authToken).perform(get("/api/integration/qualityassurancetopics/openaire:not-existing-topic"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void findOneUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom")
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID).build();
        context.restoreAuthSystemState();
        getClient().perform(get("/api/integration/qualityassurancetopics/openaire:ENRICH!MISSING!PID"))
                .andExpect(status().isUnauthorized());
        getClient().perform(get("/api/integration/qualityassurancetopics/openaire:ENRICH!MISSING!ABSTRACT"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom")
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID).build();
        context.restoreAuthSystemState();
        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(get("/api/integration/qualityassurancetopics/openaire:ENRICH!MISSING!PID"))
            .andExpect(status().isForbidden());
        getClient(authToken).perform(get("/api/integration/qualityassurancetopics/openaire:ENRICH!MISSING!ABSTRACT"))
            .andExpect(status().isForbidden());
    }

    @Test
    public void findBySourceTest() throws Exception {
        context.turnOffAuthorisationSystem();
        configurationService.setProperty(QAEVENTS_SOURCES, new String[] {
                                         OPENAIRE_SOURCE, "test-source", "test-source-2" });

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .build();

        QAEventBuilder.createTarget(context, col1, "Science and Freedom")
                      .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                      .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}")
                      .build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom 2")
                      .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                      .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144301\"}")
                      .build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom 3")
                      .withTopic(QANotifyPatterns.TOPIC_ENRICH_MORE_PID)
                      .withMessage("{\"pids[0].type\":\"pmid\",\"pids[0].value\":\"10.2307/2144302\"}")
                      .build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom 4")
                      .withTopic(org.dspace.qaevent.QANotifyPatterns.TOPIC_ENRICH_MISSING_ABSTRACT)
                      .withMessage("{\"abstracts[0]\": \"Descrizione delle caratteristiche...\"}")
                      .build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom 5")
                      .withTopic("TEST/TOPIC")
                      .withSource("test-source")
                      .build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom 6")
                      .withTopic("TEST/TOPIC")
                      .withSource("test-source")
                      .build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom 7")
                      .withTopic("TEST/TOPIC/2")
                      .withSource("test-source")
                      .build();
        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/integration/qualityassurancetopics/search/bySource")
                            .param("source", OPENAIRE_SOURCE))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.qualityassurancetopics",
                Matchers.containsInAnyOrder(
                    QATopicMatcher.matchQATopicEntry(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID, 2),
                                QATopicMatcher.matchQATopicEntry(QANotifyPatterns.TOPIC_ENRICH_MISSING_ABSTRACT, 1),
                                QATopicMatcher.matchQATopicEntry(QANotifyPatterns.TOPIC_ENRICH_MORE_PID, 1)
                                )))
                            .andExpect(jsonPath("$.page.size", is(20)))
                            .andExpect(jsonPath("$.page.totalElements", is(3)));
        getClient(authToken).perform(get("/api/integration/qualityassurancetopics/search/bySource")
                            .param("source", "test-source"))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(contentType))
                            .andExpect(jsonPath("$._embedded.qualityassurancetopics", Matchers.containsInAnyOrder(
                                       QATopicMatcher.matchQATopicEntry("test-source", "TEST/TOPIC/2", 1),
                                       QATopicMatcher.matchQATopicEntry("test-source", "TEST/TOPIC", 2)
                                       )))
                            .andExpect(jsonPath("$.page.size", is(20)))
                            .andExpect(jsonPath("$.page.totalElements", is(2)));
        getClient(authToken).perform(get("/api/integration/qualityassurancetopics/search/bySource")
                            .param("source", "test-source-2"))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(contentType))
                            .andExpect(jsonPath("$._embedded.qualityassurancetopics").doesNotExist())
                            .andExpect(jsonPath("$.page.size", is(20)))
                            .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void findBySourcePaginationTest() throws Exception {
        context.turnOffAuthorisationSystem();
        configurationService.setProperty("qaevent.sources",
                new String[] { QAEvent.OPENAIRE_SOURCE, "test-source", "test-source-2" });
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        //create collection
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom")
            .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
            .withSource(QAEvent.OPENAIRE_SOURCE)
            .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}").build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom 2")
            .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
            .withSource(QAEvent.OPENAIRE_SOURCE)
            .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144301\"}").build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom 3")
            .withTopic(QANotifyPatterns.TOPIC_ENRICH_MORE_PID)
            .withSource(QAEvent.OPENAIRE_SOURCE)
            .withMessage("{\"pids[0].type\":\"pmid\",\"pids[0].value\":\"10.2307/2144302\"}").build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom 4")
            .withTopic(org.dspace.qaevent.QANotifyPatterns.TOPIC_ENRICH_MISSING_ABSTRACT)
            .withSource(QAEvent.OPENAIRE_SOURCE)
            .withMessage(
                "{\"abstracts[0]\": \"Descrizione delle caratteristiche...\"}")
            .build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom 5")
            .withTopic("TEST/TOPIC")
            .withSource("test-source")
            .build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom 6")
            .withTopic("TEST/TOPIC")
            .withSource("test-source")
            .build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom 7")
            .withTopic("TEST/TOPIC/2")
            .withSource("test-source")
            .build();
        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/integration/qualityassurancetopics/search/bySource")
                    .param("source", QAEvent.OPENAIRE_SOURCE)
                    .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.qualityassurancetopics", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.page.size", is(2))).andExpect(jsonPath("$.page.totalElements", is(3)));
        getClient(authToken).perform(get("/api/integration/qualityassurancetopics/search/bySource")
                    .param("source", QAEvent.OPENAIRE_SOURCE)
                    .param("size", "2")
                    .param("page", "1"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.qualityassurancetopics", Matchers.hasSize(1)))
            .andExpect(jsonPath("$.page.size", is(2))).andExpect(jsonPath("$.page.totalElements", is(3)));
       //test unsupported
        getClient(authToken).perform(get("/api/integration/qualityassurancetopics/search/bySource")
                .param("source", "test-source")
                .param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded").doesNotExist())
            .andExpect(jsonPath("$.page.size", is(2))).andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void findBySourceUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                                         .withName("Parent Community")
                                         .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom")
                      .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                      .build();
        context.restoreAuthSystemState();

        getClient().perform(get("/api/integration/qualityassurancetopics/search/bySource")
                   .param("source", OPENAIRE_SOURCE))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findBySourceForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom")
            .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID).build();
        context.restoreAuthSystemState();
        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(get("/api/integration/qualityassurancetopics/search/bySource")
            .param("source", QAEvent.OPENAIRE_SOURCE))
            .andExpect(status().isForbidden());
    }

    @Test
    public void findByTargetTest() throws Exception {
        context.turnOffAuthorisationSystem();
        configurationService.setProperty("qaevent.sources",
            new String[] { QAEvent.OPENAIRE_SOURCE, "test-source", "test-source-2" });
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        Item item1 = ItemBuilder.createItem(context, col1).withTitle("Science and Freedom").build();
        Item item2 = ItemBuilder.createItem(context, col1).withTitle("Science and Freedom 2").build();
        QAEventBuilder.createTarget(context, item1)
            .withSource(QAEvent.OPENAIRE_SOURCE)
            .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
            .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}").build();
        QAEventBuilder.createTarget(context, item1)
            .withSource(QAEvent.OPENAIRE_SOURCE)
            .withTopic(org.dspace.qaevent.QANotifyPatterns.TOPIC_ENRICH_MISSING_ABSTRACT)
            .withMessage(
                "{\"abstracts[0]\": \"Descrizione delle caratteristiche...\"}")
            .build();
        QAEventBuilder.createTarget(context, item1)
            .withTopic("TEST/TOPIC")
            .withSource("test-source")
            .build();
        QAEventBuilder.createTarget(context, item1)
            .withTopic("TEST/TOPIC/2")
            .withSource("test-source")
            .build();
        QAEventBuilder.createTarget(context, item2)
            .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
            .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144301\"}").build();
        QAEventBuilder.createTarget(context, item2)
            .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
            .withMessage("{\"pids[0].type\":\"pmid\",\"pids[0].value\":\"2144301\"}").build();
        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/integration/qualityassurancetopics/search/byTarget")
                .param("target", item1.getID().toString())
                .param("source", QAEvent.OPENAIRE_SOURCE))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.qualityassurancetopics",
                        Matchers.containsInAnyOrder(
                                QATopicMatcher.matchQATopicEntry(QAEvent.OPENAIRE_SOURCE,
                                    QANotifyPatterns.TOPIC_ENRICH_MISSING_PID,
                                        item1.getID().toString(), 1),
                                QATopicMatcher.matchQATopicEntry(QAEvent.OPENAIRE_SOURCE,
                                    QANotifyPatterns.TOPIC_ENRICH_MISSING_ABSTRACT,
                                    item1.getID().toString(), 1))))
            .andExpect(jsonPath("$.page.size", is(20))).andExpect(jsonPath("$.page.totalElements", is(2)));
        getClient(authToken).perform(get("/api/integration/qualityassurancetopics/search/byTarget")
                .param("target", item2.getID().toString())
                .param("source", QAEvent.OPENAIRE_SOURCE))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.qualityassurancetopics",
                    Matchers.containsInAnyOrder(
                            QATopicMatcher.matchQATopicEntry(QAEvent.OPENAIRE_SOURCE,
                                QANotifyPatterns.TOPIC_ENRICH_MISSING_PID,
                                    item2.getID().toString(), 2))))
            .andExpect(jsonPath("$.page.size", is(20))).andExpect(jsonPath("$.page.totalElements", is(1)));
        getClient(authToken).perform(get("/api/integration/qualityassurancetopics/search/byTarget")
            .param("target", UUID.randomUUID().toString())
            .param("source", QAEvent.OPENAIRE_SOURCE))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.qualityassurancetopics").doesNotExist())
            .andExpect(jsonPath("$.page.size", is(20))).andExpect(jsonPath("$.page.totalElements", is(0)));
        getClient(authToken).perform(get("/api/integration/qualityassurancetopics/search/byTarget")
                .param("target", item2.getID().toString())
                .param("source", "test-source"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.qualityassurancetopics").doesNotExist())
                .andExpect(jsonPath("$.page.size", is(20))).andExpect(jsonPath("$.page.totalElements", is(0)));

    }

    @Test
    public void findByTargetZeroEventsOpenaireTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        Item item1 = ItemBuilder.createItem(context, col1).withTitle("Science and Freedom").build();
        QAEventBuilder.createTarget(context, item1)
            .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID).build();
        context.restoreAuthSystemState();
        getClient().perform(get("/api/integration/qualityassurancetopics/search/byTarget")
                .param("source", QAEvent.OPENAIRE_SOURCE)
                .param("target", item1.getID().toString()))
                .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void findByTargetZeroEventsAnotherSourceTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        Item item1 = ItemBuilder.createItem(context, col1).withTitle("Science and Freedom").build();
        QAEventBuilder.createTarget(context, item1)
            .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID).build();
        context.restoreAuthSystemState();
        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(get("/api/integration/qualityassurancetopics/search/byTarget")
            .param("target", item1.getID().toString())
            .param("source", "test-source"))
            .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void findByTargetBadRequest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        Item item1 = ItemBuilder.createItem(context, col1).withTitle("Science and Freedom").build();
        QAEventBuilder.createTarget(context, item1)
            .withSource("test-source")
            .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID).build();
        context.restoreAuthSystemState();
        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(get("/api/integration/qualityassurancetopics/search/byTarget")
            .param("source", "test-source"))
            .andExpect(status().isBadRequest());
        getClient(authToken).perform(get("/api/integration/qualityassurancetopics/search/byTarget")
                .param("target", item1.getID().toString()))
                .andExpect(status().isBadRequest());
    }

}
