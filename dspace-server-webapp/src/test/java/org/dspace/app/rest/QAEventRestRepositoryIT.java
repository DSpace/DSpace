/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static org.dspace.content.QAEvent.COAR_NOTIFY_SOURCE;
import static org.dspace.content.QAEvent.DSPACE_USERS_SOURCE;
import static org.dspace.content.QAEvent.OPENAIRE_SOURCE;
import static org.dspace.correctiontype.WithdrawnCorrectionType.WITHDRAWAL_REINSTATE_GROUP;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.MediaType;
import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.rest.matcher.ItemMatcher;
import org.dspace.app.rest.matcher.MetadataMatcher;
import org.dspace.app.rest.matcher.QAEventMatcher;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.repository.QAEventRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.NotifyServiceBuilder;
import org.dspace.builder.QAEventBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.content.Collection;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.QAEvent;
import org.dspace.content.QAEventProcessed;
import org.dspace.content.service.ItemService;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.qaevent.QANotifyPatterns;
import org.dspace.qaevent.action.ASimpleMetadataAction;
import org.dspace.qaevent.dao.QAEventsDAO;
import org.dspace.qaevent.service.dto.CorrectionTypeMessageDTO;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link QAEventRestRepository}.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class QAEventRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private QAEventsDAO qaEventsDao;
    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private ASimpleMetadataAction AddReviewMetadataAction;

    @Autowired
    private ASimpleMetadataAction AddEndorsedMetadataAction;

    @Test
    public void findAllNotImplementedTest() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/integration/qualityassuranceevents"))
                             .andExpect(status()
                             .isMethodNotAllowed());

        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(get("/api/integration/qualityassuranceevents"))
                               .andExpect(status()
                               .isMethodNotAllowed());

        getClient().perform(get("/api/integration/qualityassuranceevents"))
                   .andExpect(status()
                   .isMethodNotAllowed());
    }

    @Test
    public void findOneTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        QAEvent event1 = QAEventBuilder.createTarget(context, col1, "Science and Freedom")
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}").build();
        QAEvent event2 = QAEventBuilder.createTarget(context, col1, "Science and Freedom 4")
                .withTopic(org.dspace.qaevent.QANotifyPatterns.TOPIC_ENRICH_MISSING_ABSTRACT)
                .withMessage("{\"abstracts[0]\": \"Descrizione delle caratteristiche...\"}").build();
        EPerson anotherSubmitter = EPersonBuilder.createEPerson(context).withEmail("another-submitter@example.com")
                .withPassword(password).build();
        context.setCurrentUser(anotherSubmitter);
        QAEvent event3 = QAEventBuilder.createTarget(context, col1, "Science and Freedom")
                .withSource(COAR_NOTIFY_SOURCE)
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MORE_REVIEW)
                .withMessage("{\"abstracts[0]\": \"Descrizione delle caratteristiche...\"}").build();
        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/integration/qualityassuranceevents/" + event1.getEventId()))
            .andExpect(status().isOk())
                .andExpect(jsonPath("$", QAEventMatcher.matchQAEventEntry(event1)));
        getClient(authToken).perform(get("/api/integration/qualityassuranceevents/" + event2.getEventId()))
            .andExpect(status().isOk())
                .andExpect(jsonPath("$", QAEventMatcher.matchQAEventEntry(event2)));
        getClient(authToken).perform(get("/api/integration/qualityassuranceevents/" + event3.getEventId()))
            .andExpect(status().isOk())
                .andExpect(jsonPath("$", QAEventMatcher.matchQAEventEntry(event3)));
        authToken = getAuthToken(anotherSubmitter.getEmail(), password);
        // eperson should be see the coar-notify event related to the item that it has submitted
        getClient(authToken).perform(get("/api/integration/qualityassuranceevents/" + event3.getEventId()))
            .andExpect(status().isOk())
                .andExpect(jsonPath("$", QAEventMatcher.matchQAEventEntry(event3)));
    }

    @Test
    public void findOneWithProjectionTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .build();

        QAEvent event1 = QAEventBuilder.createTarget(context, col1, "Science and Freedom")
                                       .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                                       .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}")
                                       .build();
        QAEvent event5 = QAEventBuilder.createTarget(context, col1, "Science and Freedom 5")
                        .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PROJECT)
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
        getClient(authToken).perform(get("/api/integration/qualityassuranceevents/" + event1.getEventId())
                            .param("projection", "full"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$", QAEventMatcher.matchQAEventFullEntry(event1)));

        getClient(authToken).perform(get("/api/integration/qualityassuranceevents/" + event5.getEventId())
                            .param("projection", "full"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$", QAEventMatcher.matchQAEventFullEntry(event5)));
    }

    @Test
    public void findOneUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .build();
        QAEvent event1 = QAEventBuilder.createTarget(context, col1, "Science and Freedom")
                                       .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                                       .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}")
                                       .build();
        context.restoreAuthSystemState();

        getClient().perform(get("/api/integration/qualityassuranceevents/" + event1.getEventId()))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        QAEvent event1 = QAEventBuilder.createTarget(context, col1, "Science and Freedom")
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}").build();
        EPerson anotherSubmitter = EPersonBuilder.createEPerson(context).withEmail("another_submitter@example.com")
                .build();
        context.setCurrentUser(anotherSubmitter);
        QAEvent event2 = QAEventBuilder.createTarget(context, col1, "Science and Freedom")
                .withSource(COAR_NOTIFY_SOURCE)
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MORE_REVIEW)
                .withMessage("{\"href\":\"https://doi.org/10.2307/2144300\"}").build();
        context.restoreAuthSystemState();
        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(get("/api/integration/qualityassuranceevents/" + event1.getEventId()))
                .andExpect(status().isForbidden());
        getClient(authToken).perform(get("/api/integration/qualityassuranceevents/" + event2.getEventId()))
                .andExpect(status().isForbidden());
    }

    @Test
    public void findByTopicAndTargetTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        String uuid = UUID.randomUUID().toString();
        Item item = ItemBuilder.createItem(context, col1).withTitle("Tracking Papyrus and Parchment Paths")
                .build();
        QAEvent event1 = QAEventBuilder.createTarget(context, item)
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}")
                .build();
        QAEvent event2 = QAEventBuilder.createTarget(context, item)
                .withSource(COAR_NOTIFY_SOURCE)
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MORE_REVIEW)
                .withMessage("{\"href\":\"https://doi.org/10.2307/2144301\"}").build();
        EPerson anotherSubmitter = EPersonBuilder.createEPerson(context).withEmail("another-submitter@example.com")
                .withPassword(password).build();
        context.setCurrentUser(anotherSubmitter);
        // this event is related to a new item not submitted by eperson
        QAEvent event3 = QAEventBuilder.createTarget(context, col1, "Science and Freedom")
                .withSource(COAR_NOTIFY_SOURCE)
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MORE_REVIEW)
                .withMessage("{\"href\":\"https://doi.org/10.2307/2144300\"}").build();
        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(
                get("/api/integration/qualityassuranceevents/search/findByTopic")
                    .param("topic", QAEvent.OPENAIRE_SOURCE + ":ENRICH!MISSING!PID:" + uuid.toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(0)));

        uuid = item.getID().toString();
        // check for an existing item but a different topic
        getClient(authToken)
            .perform(
                get("/api/integration/qualityassuranceevents/search/findByTopic")
                    .param("topic", QAEvent.OPENAIRE_SOURCE + ":not-existing:" + uuid.toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(0)));
        // check for an existing topic but a different source
        getClient(authToken)
            .perform(
                get("/api/integration/qualityassuranceevents/search/findByTopic")
                    .param("topic", QAEvent.COAR_NOTIFY_SOURCE + ":ENRICH!MISSING!PID"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(0)));

        // check for an existing item and topic
        getClient(authToken)
            .perform(
                get("/api/integration/qualityassuranceevents/search/findByTopic")
                    .param("topic", QAEvent.OPENAIRE_SOURCE + ":ENRICH!MISSING!PID:" + uuid.toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.qualityassuranceevents", Matchers.hasSize(1)))
            .andExpect(jsonPath("$._embedded.qualityassuranceevents",
                        Matchers.contains(QAEventMatcher.matchQAEventEntry(event1))))
            .andExpect(jsonPath("$.page.size", is(20))).andExpect(jsonPath("$.page.totalElements", is(1)));
        // use the coar-notify source that has a custom security
        getClient(authToken)
            .perform(
                get("/api/integration/qualityassuranceevents/search/findByTopic")
                    .param("topic", QAEvent.COAR_NOTIFY_SOURCE + ":ENRICH!MORE!REVIEW:" + uuid.toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.qualityassuranceevents", Matchers.hasSize(1)))
            .andExpect(jsonPath("$._embedded.qualityassuranceevents",
                        Matchers.contains(QAEventMatcher.matchQAEventEntry(event2))))
            .andExpect(jsonPath("$.page.size", is(20))).andExpect(jsonPath("$.page.totalElements", is(1)));
        // check for an existing topic
        getClient(authToken)
            .perform(
                get("/api/integration/qualityassuranceevents/search/findByTopic")
                    .param("topic", QAEvent.OPENAIRE_SOURCE + ":ENRICH!MISSING!PID"))
            .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.qualityassuranceevents", Matchers.hasSize(1)))
            .andExpect(jsonPath("$._embedded.qualityassuranceevents",
                        Matchers.contains(QAEventMatcher.matchQAEventEntry(event1))))
            .andExpect(jsonPath("$.page.size", is(20))).andExpect(jsonPath("$.page.totalElements", is(1)));
        // use the coar-notify source that has a custom security
        getClient(authToken)
            .perform(
                get("/api/integration/qualityassuranceevents/search/findByTopic")
                    .param("topic", QAEvent.COAR_NOTIFY_SOURCE + ":ENRICH!MORE!REVIEW"))
            .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.qualityassuranceevents", Matchers.hasSize(2)))
            .andExpect(jsonPath("$._embedded.qualityassuranceevents",
                    Matchers.containsInAnyOrder(
                            QAEventMatcher.matchQAEventEntry(event2),
                            QAEventMatcher.matchQAEventEntry(event3))))
            .andExpect(jsonPath("$.page.size", is(20))).andExpect(jsonPath("$.page.totalElements", is(2)));
        // check results for eperson
        authToken = getAuthToken(eperson.getEmail(), password);
        // check for an item that was submitted by eperson but in a qasource restricted to admins
        getClient(authToken)
            .perform(
                get("/api/integration/qualityassuranceevents/search/findByTopic")
                    .param("topic", QAEvent.OPENAIRE_SOURCE + ":ENRICH!MISSING!PID:" + uuid.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(0)));
        // use the coar-notify source that has a custom security, only 1 event is related to the item submitted by
        // eperson
        getClient(authToken)
            .perform(
                get("/api/integration/qualityassuranceevents/search/findByTopic")
                    .param("topic", QAEvent.COAR_NOTIFY_SOURCE + ":ENRICH!MORE!REVIEW:" + uuid.toString()))
            .andExpect(status().isOk()).andExpect(jsonPath("$._embedded.qualityassuranceevents", Matchers.hasSize(1)))
            .andExpect(jsonPath("$._embedded.qualityassuranceevents",
                        Matchers.contains(QAEventMatcher.matchQAEventEntry(event2))))
            .andExpect(jsonPath("$.page.size", is(20))).andExpect(jsonPath("$.page.totalElements", is(1)));
        // check for an existing topic
        getClient(authToken)
            .perform(
                get("/api/integration/qualityassuranceevents/search/findByTopic")
                    .param("topic", QAEvent.OPENAIRE_SOURCE + ":ENRICH!MISSING!PID"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(0)));

    }

    @Test
    public void findByTopicTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        QAEvent event1 = QAEventBuilder.createTarget(context, col1, "Science and Freedom")
                .withTopic("ENRICH/MISSING/PID")
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}").build();
        QAEvent event2 = QAEventBuilder.createTarget(context, col1, "Science and Freedom 2")
                .withTopic("ENRICH/MISSING/PID")
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144301\"}").build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom 3")
                .withTopic("ENRICH/MORE/PID")
                .withMessage("{\"pids[0].type\":\"pmid\",\"pids[0].value\":\"10.2307/2144302\"}").build();
        QAEvent event4 = QAEventBuilder.createTarget(context, col1, "Science and Freedom 4")
                .withTopic("ENRICH/MISSING/ABSTRACT")
                .withMessage("{\"abstracts[0]\": \"Descrizione delle caratteristiche...\"}").build();
        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/integration/qualityassuranceevents/search/findByTopic")
                        .param("topic", OPENAIRE_SOURCE + ":" + "ENRICH!MISSING!PID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.qualityassuranceevents", Matchers.hasSize(2)))
                .andExpect(jsonPath("$._embedded.qualityassuranceevents",Matchers.containsInAnyOrder(
                        QAEventMatcher.matchQAEventEntry(event1),
                        QAEventMatcher.matchQAEventEntry(event2)
                )))
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", is(2)));

        getClient(authToken).perform(get("/api/integration/qualityassuranceevents/search/findByTopic")
                        .param("topic", OPENAIRE_SOURCE + ":" + "ENRICH!MISSING!ABSTRACT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.qualityassuranceevents", Matchers.hasSize(1)))
                .andExpect(jsonPath("$._embedded.qualityassuranceevents", Matchers.containsInAnyOrder(
                        QAEventMatcher.matchQAEventEntry(event4))))
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", is(1)));

        getClient(authToken).perform(get("/api/integration/qualityassuranceevents/search/findByTopic")
                        .param("topic", "not-existing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void findByTopicPaginatedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        QAEvent event1 = QAEventBuilder.createTarget(context, col1, "Science and Freedom")
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}").build();
        QAEvent event2 = QAEventBuilder.createTarget(context, col1, "Science and Freedom 2")
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144301\"}").build();
        QAEvent event3 = QAEventBuilder.createTarget(context, col1, "Science and Freedom 3")
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144302\"}").build();
        QAEvent event4 = QAEventBuilder.createTarget(context, col1, "Science and Freedom 4")
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                .withMessage("{\"pids[0].type\":\"pmc\",\"pids[0].value\":\"2144303\"}").build();
        QAEvent event5 = QAEventBuilder.createTarget(context, col1, "Science and Freedom 5")
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                .withMessage("{\"pids[0].type\":\"pmid\",\"pids[0].value\":\"2144304\"}").build();
        QAEvent event6 = QAEventBuilder.createTarget(context, col1, "Science and Freedom")
                .withSource(OPENAIRE_SOURCE)
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}").build();
        QAEvent event7 = QAEventBuilder.createTarget(context, col1, "Science and Freedom 2")
                .withSource(OPENAIRE_SOURCE)
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144301\"}").build();
        QAEvent event8 = QAEventBuilder.createTarget(context, col1, "Science and Freedom 3")
                .withSource(OPENAIRE_SOURCE)
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144302\"}").build();
        QAEvent event9 = QAEventBuilder.createTarget(context, col1, "Science and Freedom 4")
                .withSource(OPENAIRE_SOURCE)
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                .withMessage("{\"pids[0].type\":\"pmc\",\"pids[0].value\":\"2144303\"}").build();
        QAEvent event10 = QAEventBuilder.createTarget(context, col1, "Science and Freedom 5")
                .withSource(OPENAIRE_SOURCE)
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                .withMessage("{\"pids[0].type\":\"pmid\",\"pids[0].value\":\"2144304\"}").build();
        context.setCurrentUser(admin);
        // this event will be related to an item submitted by the admin
        QAEvent event11 = QAEventBuilder.createTarget(context, col1, "Science and Freedom 5")
                .withSource(OPENAIRE_SOURCE)
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                .withMessage("{\"pids[0].type\":\"pmid\",\"pids[0].value\":\"2144304\"}").build();
        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
                .perform(
                        get("/api/integration/qualityassuranceevents/search/findByTopic")
                                .param("topic", QAEvent.OPENAIRE_SOURCE + ":ENRICH!MISSING!PID")
                                .param("size", "2"))
                .andExpect(status().isOk()).andExpect(
                        jsonPath("$._embedded.qualityassuranceevents", Matchers.hasSize(2)))
                .andExpect(jsonPath("$._embedded.qualityassuranceevents",
                        Matchers.containsInAnyOrder(
                                QAEventMatcher.matchQAEventEntry(event1),
                                QAEventMatcher.matchQAEventEntry(event2))))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/qualityassuranceevents/search/findByTopic?"),
                                Matchers.containsString("topic=" + QAEvent.OPENAIRE_SOURCE + ":ENRICH!MISSING!PID"),
                                Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.next.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/qualityassuranceevents/search/findByTopic?"),
                                Matchers.containsString("topic=" + QAEvent.OPENAIRE_SOURCE + ":ENRICH!MISSING!PID"),
                                Matchers.containsString("page=1"),
                                Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.last.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/qualityassuranceevents/search/findByTopic?"),
                                Matchers.containsString("topic=" + QAEvent.OPENAIRE_SOURCE + ":ENRICH!MISSING!PID"),
                                Matchers.containsString("page=5"),
                                Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.first.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/qualityassuranceevents/search/findByTopic?"),
                                Matchers.containsString("topic=" + QAEvent.OPENAIRE_SOURCE + ":ENRICH!MISSING!PID"),
                                Matchers.containsString("page=0"),
                                Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.prev.href").doesNotExist())
                .andExpect(jsonPath("$.page.size", is(2)))
                .andExpect(jsonPath("$.page.totalPages", is(6)))
                .andExpect(jsonPath("$.page.totalElements", is(11)));

        getClient(authToken)
                .perform(
                        get("/api/integration/qualityassuranceevents/search/findByTopic")
                                .param("topic", QAEvent.OPENAIRE_SOURCE + ":ENRICH!MISSING!PID")
                                .param("size", "2").param("page", "1"))
                .andExpect(status().isOk()).andExpect(
                        jsonPath("$._embedded.qualityassuranceevents", Matchers.hasSize(2)))
                .andExpect(jsonPath("$._embedded.qualityassuranceevents",
                        Matchers.containsInAnyOrder(
                                QAEventMatcher.matchQAEventEntry(event3),
                                QAEventMatcher.matchQAEventEntry(event4))))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/qualityassuranceevents/search/findByTopic?"),
                                Matchers.containsString("topic=" + QAEvent.OPENAIRE_SOURCE + ":ENRICH!MISSING!PID"),
                                Matchers.containsString("page=1"),
                                Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.next.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/qualityassuranceevents/search/findByTopic?"),
                                Matchers.containsString("topic=" + QAEvent.OPENAIRE_SOURCE + ":ENRICH!MISSING!PID"),
                                Matchers.containsString("page=2"),
                                Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.last.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/qualityassuranceevents/search/findByTopic?"),
                                Matchers.containsString("topic=" + QAEvent.OPENAIRE_SOURCE + ":ENRICH!MISSING!PID"),
                                Matchers.containsString("page=5"),
                                Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.first.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/qualityassuranceevents/search/findByTopic?"),
                                Matchers.containsString("topic=" + QAEvent.OPENAIRE_SOURCE + ":ENRICH!MISSING!PID"),
                                Matchers.containsString("page=0"),
                                Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.prev.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/qualityassuranceevents/search/findByTopic?"),
                                Matchers.containsString("topic=" + QAEvent.OPENAIRE_SOURCE + ":ENRICH!MISSING!PID"),
                                Matchers.containsString("page=0"),
                                Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$.page.size", is(2)))
                .andExpect(jsonPath("$.page.totalPages", is(6)))
                .andExpect(jsonPath("$.page.totalElements", is(11)));

        getClient(authToken)
                .perform(
                        get("/api/integration/qualityassuranceevents/search/findByTopic")
                                .param("topic", QAEvent.OPENAIRE_SOURCE + ":ENRICH!MISSING!PID")
                                .param("size", "2").param("page", "2"))
                .andExpect(status().isOk()).andExpect(
                        jsonPath("$._embedded.qualityassuranceevents", Matchers.hasSize(2)))
                .andExpect(jsonPath("$._embedded.qualityassuranceevents",
                        Matchers.hasItem(
                                QAEventMatcher.matchQAEventEntry(event5))))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/qualityassuranceevents/search/findByTopic?"),
                                Matchers.containsString("topic=" + QAEvent.OPENAIRE_SOURCE + ":ENRICH!MISSING!PID"),
                                Matchers.containsString("page=2"),
                                Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.next.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/qualityassuranceevents/search/findByTopic?"),
                                Matchers.containsString("topic=" + QAEvent.OPENAIRE_SOURCE + ":ENRICH!MISSING!PID"),
                                Matchers.containsString("page=3"),
                                Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.last.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/qualityassuranceevents/search/findByTopic?"),
                                Matchers.containsString("topic=" + QAEvent.OPENAIRE_SOURCE + ":ENRICH!MISSING!PID"),
                                Matchers.containsString("page=5"),
                                Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.first.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/qualityassuranceevents/search/findByTopic?"),
                                Matchers.containsString("topic=" + QAEvent.OPENAIRE_SOURCE + ":ENRICH!MISSING!PID"),
                                Matchers.containsString("page=0"),
                                Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$._links.prev.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/qualityassuranceevents/search/findByTopic?"),
                                Matchers.containsString("topic=" + QAEvent.OPENAIRE_SOURCE + ":ENRICH!MISSING!PID"),
                                Matchers.containsString("page=1"),
                                Matchers.containsString("size=2"))))
                .andExpect(jsonPath("$.page.size", is(2)))
                .andExpect(jsonPath("$.page.totalPages", is(6)))
                .andExpect(jsonPath("$.page.totalElements", is(11)));

        // check if the pagination is working properly also when a security filter is in place
        authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken)
                .perform(
                        get("/api/integration/qualityassuranceevents/search/findByTopic")
                                .param("topic", QAEvent.OPENAIRE_SOURCE + ":ENRICH!MISSING!PID")
                                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasNoJsonPath("$._embedded.qualityassuranceevents")))
                .andExpect(jsonPath("$.page.size", is(2)))
                .andExpect(jsonPath("$.page.totalPages", is(0)))
                .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void findByTopicUnauthorizedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom")
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}").build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom 2")
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144301\"}").build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom 3")
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MORE_PID)
                .withMessage("{\"pids[0].type\":\"pmid\",\"pids[0].value\":\"10.2307/2144302\"}").build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom 4")
                .withTopic(org.dspace.qaevent.QANotifyPatterns.TOPIC_ENRICH_MISSING_ABSTRACT)
                .withMessage("{\"abstracts[0]\": \"Descrizione delle caratteristiche...\"}").build();
        context.restoreAuthSystemState();
        getClient().perform(get("/api/integration/qualityassuranceevents/search/findByTopic")
                   .param("topic", OPENAIRE_SOURCE + ":" + "ENRICH!MISSING!PID"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findByTopicBadRequestTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom")
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}").build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom 2")
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144301\"}").build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom 3")
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MORE_PID)
                .withMessage("{\"pids[0].type\":\"pmid\",\"pids[0].value\":\"10.2307/2144302\"}").build();
        QAEventBuilder.createTarget(context, col1, "Science and Freedom 4")
                .withTopic(org.dspace.qaevent.QANotifyPatterns.TOPIC_ENRICH_MISSING_ABSTRACT)
                .withMessage("{\"abstracts[0]\": \"Descrizione delle caratteristiche...\"}").build();
        context.restoreAuthSystemState();
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/integration/qualityassuranceevents/search/findByTopic"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void recordDecisionTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType publication = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType project = EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();

        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publication, project,
                                "isProjectOfPublication", "isPublicationOfProject", 0, null, 0, null)
                               .withCopyToRight(true)
                               .build();

        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withEntityType("Publication")
                                           .withName("Collection 1")
                                           .build();
        Collection colFunding = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Collection Fundings")
                                                 .withEntityType("Project")
                                                 .build();

        Item funding = ItemBuilder.createItem(context, colFunding)
                                  .withTitle("Tracking Papyrus and Parchment Paths")
                                  .build();

        QAEvent eventProjectBound = QAEventBuilder.createTarget(context, col1, "Science and Freedom with project")
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PROJECT)
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
        QAEvent eventProjectNoBound = QAEventBuilder
                .createTarget(context, col1, "Science and Freedom with unrelated project")
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PROJECT)
                .withMessage(
                        "{\"projects[0].acronym\":\"NEW\","
                        + "\"projects[0].code\":\"123456\","
                        + "\"projects[0].funder\":\"EC\","
                        + "\"projects[0].fundingProgram\":\"H2020\","
                        + "\"projects[0].jurisdiction\":\"EU\","
                        + "\"projects[0].openaireId\":\"newProjectID\","
                        + "\"projects[0].title\":\"A new project\"}")
                .build();
        QAEvent eventMissingPID1 = QAEventBuilder.createTarget(context, col1, "Science and Freedom")
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}")
                .build();
        QAEvent eventMissingPID2 = QAEventBuilder.createTarget(context, col1, "Science and Freedom 2")
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144301\"}")
                .build();
        QAEvent eventMissingUnknownPID = QAEventBuilder.createTarget(context, col1, "Science and Freedom URN PID")
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                .withMessage(
                        "{\"pids[0].type\":\"urn\",\"pids[0].value\":\"http://thesis2.sba.units.it/store/handle/item/12937\"}")
                .build();
        QAEvent eventMorePID = QAEventBuilder.createTarget(context, col1, "Science and Freedom 3")
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MORE_PID)
                .withMessage("{\"pids[0].type\":\"pmid\",\"pids[0].value\":\"2144302\"}")
                .build();
        QAEvent eventAbstract = QAEventBuilder.createTarget(context, col1, "Science and Freedom 4")
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_ABSTRACT)
                .withMessage("{\"abstracts[0]\": \"An abstract to add...\"}")
                .build();
        QAEvent eventAbstractToDiscard = QAEventBuilder.createTarget(context, col1, "Science and Freedom 7")
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_ABSTRACT)
                .withMessage("{\"abstracts[0]\": \"Abstract to discard...\"}")
                .build();
        context.restoreAuthSystemState();

        // prepare the different patches for our decisions
        List<Operation> acceptOp = new ArrayList<Operation>();
        acceptOp.add(new ReplaceOperation("/status", QAEvent.ACCEPTED));

        List<Operation> acceptOpUppercase = new ArrayList<Operation>();
        acceptOpUppercase.add(new ReplaceOperation("/status", QAEvent.ACCEPTED));

        List<Operation> discardOp = new ArrayList<Operation>();
        discardOp.add(new ReplaceOperation("/status", QAEvent.DISCARDED));

        List<Operation> rejectOp = new ArrayList<Operation>();
        rejectOp.add(new ReplaceOperation("/status", QAEvent.REJECTED));

        String patchAccept = getPatchContent(acceptOp);
        String patchAcceptUppercase = getPatchContent(acceptOpUppercase);
        String patchDiscard = getPatchContent(discardOp);
        String patchReject = getPatchContent(rejectOp);

        String authToken = getAuthToken(admin.getEmail(), password);
        // accept pid1, unknownPID, morePID, the two projects and abstract
        eventMissingPID1.setStatus(QAEvent.ACCEPTED);
        eventMorePID.setStatus(QAEvent.ACCEPTED);
        eventMissingUnknownPID.setStatus(QAEvent.ACCEPTED);
        eventMissingUnknownPID.setStatus(QAEvent.ACCEPTED);
        eventProjectBound.setStatus(QAEvent.ACCEPTED);
        eventProjectNoBound.setStatus(QAEvent.ACCEPTED);
        eventAbstract.setStatus(QAEvent.ACCEPTED);

        getClient(authToken).perform(patch("/api/integration/qualityassuranceevents/" + eventMissingPID1.getEventId())
                            .content(patchAccept)
                            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$", QAEventMatcher.matchQAEventEntry(eventMissingPID1)));

        getClient(authToken).perform(patch("/api/integration/qualityassuranceevents/" + eventMorePID.getEventId())
                            .content(patchAcceptUppercase)
                            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$", QAEventMatcher.matchQAEventEntry(eventMorePID)));

        getClient(authToken)
            .perform(patch("/api/integration/qualityassuranceevents/" + eventMissingUnknownPID.getEventId())
                .content(patchAccept)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", QAEventMatcher.matchQAEventEntry(eventMissingUnknownPID)));

        getClient(authToken).perform(patch("/api/integration/qualityassuranceevents/" + eventProjectBound.getEventId())
                            .content(patchAccept)
                            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$", QAEventMatcher.matchQAEventEntry(eventProjectBound)));

        getClient(authToken)
            .perform(patch("/api/integration/qualityassuranceevents/" + eventProjectNoBound.getEventId())
                .content(patchAccept)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", QAEventMatcher.matchQAEventEntry(eventProjectNoBound)));

        getClient(authToken).perform(patch("/api/integration/qualityassuranceevents/" + eventAbstract.getEventId())
                            .content(patchAccept)
                            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$", QAEventMatcher.matchQAEventEntry(eventAbstract)));

        // check if the item has been updated
        getClient(authToken).perform(get("/api/core/items/" + eventMissingPID1.getTarget())
                            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$",hasJsonPath("$.metadata['dc.identifier.other'][0].value",
                                                    is("10.2307/2144300"))));

        getClient(authToken).perform(get("/api/core/items/" + eventMorePID.getTarget())
                            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$", hasJsonPath("$.metadata['dc.identifier.other'][0].value",
                                                     is("2144302"))));

        getClient(authToken).perform(get("/api/core/items/" + eventMissingUnknownPID.getTarget())
                            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$", hasJsonPath("$.metadata['dc.identifier.other'][0].value",
                                                     is("http://thesis2.sba.units.it/store/handle/item/12937"))));

        getClient(authToken).perform(get("/api/core/items/" + eventProjectBound.getTarget())
                            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$",
                                    hasJsonPath("$.metadata['relation.isProjectOfPublication'][0].value",
                                             is(funding.getID().toString()))));

        getClient(authToken).perform(get("/api/core/items/" + eventProjectNoBound.getTarget())
                            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$",
                                    hasJsonPath("$.metadata['relation.isProjectOfPublication'][0].value",
                                             is(not(empty())))));

        getClient(authToken).perform(get("/api/core/items/" + eventAbstract.getTarget())
                            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$", hasJsonPath("$.metadata['dc.description.abstract'][0].value",
                                                              is("An abstract to add..."))));

        // reject pid2
        eventMissingPID2.setStatus(QAEvent.REJECTED);
        getClient(authToken).perform(patch("/api/integration/qualityassuranceevents/" + eventMissingPID2.getEventId())
                            .content(patchReject)
                            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$", QAEventMatcher.matchQAEventEntry(eventMissingPID2)));

        getClient(authToken).perform(get("/api/core/items/" + eventMissingPID2.getTarget())
                            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$", hasNoJsonPath("$.metadata['dc.identifier.other']")));

        // discard abstractToDiscard
        eventAbstractToDiscard.setStatus(QAEvent.DISCARDED);
        getClient(authToken)
            .perform(patch("/api/integration/qualityassuranceevents/" + eventAbstractToDiscard.getEventId())
                .content(patchDiscard)
                .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", QAEventMatcher.matchQAEventEntry(eventAbstractToDiscard)));

        getClient(authToken).perform(get("/api/core/items/" + eventMissingPID2.getTarget())
                            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$", hasNoJsonPath("$.metadata['dc.description.abstract']")));

        // no pending qa events should be longer available
        getClient(authToken).perform(get("/api/integration/qualityassurancesources/" + QAEvent.OPENAIRE_SOURCE))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType(contentType))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.totalEvents", is(0)));
    }

    @Test
    public void recordDecisionNotifyTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EntityType publication = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType project = EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();
        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publication, project, "isProjectOfPublication",
                "isPublicationOfProject", 0, null, 0,
                null).withCopyToRight(true).build();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Publication")
            .withName("Collection 1").build();
        Collection colFunding = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection Fundings")
            .withEntityType("Project").build();
        Item item = ItemBuilder.createItem(context, colFunding).withTitle("Tracking Papyrus and Parchment Paths")
                .build();
        NotifyServiceEntity notifyServiceEntity =
            NotifyServiceBuilder.createNotifyServiceBuilder(context, "service name")
                                .withDescription("service description")
                                .withUrl("https://review-service.com/inbox/about/")
                                .withLdnUrl("https://review-service.com/inbox/")
                                .withScore(BigDecimal.valueOf(0.6d))
                                .build();
        String href = "EC";
        QAEvent eventMoreReview = QAEventBuilder.createTarget(context, col1, "Science and Freedom with project")
                .withSource(COAR_NOTIFY_SOURCE)
                .withTopic("ENRICH/MORE/REVIEW")
                .withMessage(
                        "{"
                        + "\"serviceName\":\"" + notifyServiceEntity.getName() + "\","
                        + "\"serviceId\":\"" + notifyServiceEntity.getID() + "\","
                        + "\"href\":\"" + href + "\","
                        + "\"relationship\":\"H2020\""
                        + "}")
                .withRelatedItem(item.getID().toString())
                .build();
        QAEvent eventMoreEndorsement = QAEventBuilder.createTarget(context, col1, "Science and Freedom with project")
            .withSource(COAR_NOTIFY_SOURCE)
            .withTopic("ENRICH/MORE/ENDORSEMENT")
            .withMessage(
                    "{"
                    + "\"serviceName\":\"" + notifyServiceEntity.getName() + "\","
                    + "\"serviceId\":\"" + notifyServiceEntity.getID() + "\","
                    + "\"href\":\"" + href + "\","
                    + "\"relationship\":\"H2020\""
                    + "}")
            .withRelatedItem(item.getID().toString())
            .build();
        context.restoreAuthSystemState();
        List<Operation> acceptOp = new ArrayList<Operation>();
        acceptOp.add(new ReplaceOperation("/status", QAEvent.ACCEPTED));
        String patchAccept = getPatchContent(acceptOp);
        String authToken = getAuthToken(admin.getEmail(), password);
        eventMoreEndorsement.setStatus(QAEvent.ACCEPTED);
        eventMoreReview.setStatus(QAEvent.ACCEPTED);
        // MORE REVIEW
        getClient(authToken).perform(patch("/api/integration/qualityassuranceevents/" + eventMoreReview.getEventId())
            .content(patchAccept)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", QAEventMatcher.matchQAEventNotifyEntry(eventMoreReview)));
        getClient(authToken).perform(get("/api/core/items/" + eventMoreReview.getTarget())
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$",
                hasJsonPath("$.metadata",
                        MetadataMatcher.matchMetadata(AddReviewMetadataAction.getMetadata(), href))));
        // MORE ENDORSEMENT
        getClient(authToken).perform(patch("/api/integration/qualityassuranceevents/"
            + eventMoreEndorsement.getEventId())
            .content(patchAccept)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", QAEventMatcher.matchQAEventNotifyEntry(eventMoreEndorsement)));

        getClient(authToken).perform(get("/api/core/items/" + eventMoreEndorsement.getTarget())
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$",
                hasJsonPath("$.metadata",
                        MetadataMatcher.matchMetadata(AddEndorsedMetadataAction.getMetadata(), href))));

    }

    @Test
    public void setRelatedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        Collection colFunding = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection Fundings").build();
        QAEvent event = QAEventBuilder.createTarget(context, col1, "Science and Freedom 5")
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PROJECT)
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
            .perform(get("/api/integration/qualityassuranceevents/" + event.getEventId()).param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", QAEventMatcher.matchQAEventFullEntry(event)));

        getClient(authToken)
            .perform(post("/api/integration/qualityassuranceevents/" + event.getEventId() + "/related").param("item",
                        funding.getID().toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", ItemMatcher.matchItemProperties(funding)));
        // update our local event copy to reflect the association with the related item
        event.setRelated(funding.getID().toString());
        getClient(authToken)
            .perform(get("/api/integration/qualityassuranceevents/" + event.getEventId()).param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", QAEventMatcher.matchQAEventFullEntry(event)));
        getClient(authToken)
            .perform(get("/api/integration/qualityassuranceevents/" + event.getEventId() + "/related"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", ItemMatcher.matchItemProperties(funding)));
    }

    @Test
    public void unsetRelatedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        Collection colFunding = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection Fundings").build();
        Item funding = ItemBuilder.createItem(context, colFunding).withTitle("Tracking Papyrus and Parchment Paths")
                .build();
        QAEvent event = QAEventBuilder.createTarget(context, col1, "Science and Freedom 5")
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PROJECT)
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
            .perform(get("/api/integration/qualityassuranceevents/" + event.getEventId()).param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", QAEventMatcher.matchQAEventFullEntry(event)));
        getClient(authToken)
            .perform(delete("/api/integration/qualityassuranceevents/" + event.getEventId() + "/related"))
                .andExpect(status().isNoContent());

        // update our local event copy to reflect the association with the related item
        event.setRelated(null);
        getClient(authToken)
            .perform(get("/api/integration/qualityassuranceevents/" + event.getEventId()).param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", QAEventMatcher.matchQAEventFullEntry(event)));
        getClient(authToken)
            .perform(get("/api/integration/qualityassuranceevents/" + event.getEventId() + "/related"))
            .andExpect(status().isNoContent());
    }

    @Test
    public void setInvalidRelatedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        Collection colFunding = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection Fundings").build();
        QAEvent event = QAEventBuilder.createTarget(context, col1, "Science and Freedom")
                .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}").build();
        Item funding = ItemBuilder.createItem(context, colFunding).withTitle("Tracking Papyrus and Parchment Paths")
                .build();
        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
            .perform(get("/api/integration/qualityassuranceevents/" + event.getEventId()).param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", QAEventMatcher.matchQAEventFullEntry(event)));

        getClient(authToken)
            .perform(post("/api/integration/qualityassuranceevents/" + event.getEventId() + "/related").param("item",
                        funding.getID().toString()))
                .andExpect(status().isUnprocessableEntity());
        // check that no related item has been added to our event
        getClient(authToken)
            .perform(get("/api/integration/qualityassuranceevents/" + event.getEventId()).param("projection", "full"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", QAEventMatcher.matchQAEventFullEntry(event)));
    }

    @Test
    public void deleteItemWithEventTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
                                           .withName("Collection 1")
                                           .build();
        QAEvent event1 = QAEventBuilder.createTarget(context, col1, "Science and Freedom")
                                       .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                                       .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}")
                                       .build();
        QAEvent event2 = QAEventBuilder.createTarget(context, col1, "Science and Freedom 2")
                                       .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
                                       .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144301\"}")
                                       .build();
        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/integration/qualityassuranceevents/search/findByTopic")
                            .param("topic", OPENAIRE_SOURCE + ":" + "ENRICH!MISSING!PID"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$._embedded.qualityassuranceevents", Matchers.hasSize(2)))
                            .andExpect(jsonPath("$._embedded.qualityassuranceevents", Matchers.containsInAnyOrder(
                                       QAEventMatcher.matchQAEventEntry(event1),
                                       QAEventMatcher.matchQAEventEntry(event2)
                                       )))
                            .andExpect(jsonPath("$.page.size", is(20)))
                            .andExpect(jsonPath("$.page.totalElements", is(2)));

        getClient(authToken).perform(delete("/api/core/items/" + event1.getTarget()))
                            .andExpect(status().is(204));

        getClient(authToken).perform(get("/api/core/items/" + event1.getTarget()))
                            .andExpect(status().is(404));

        getClient(authToken).perform(get("/api/integration/qualityassuranceevents/search/findByTopic")
                            .param("topic", OPENAIRE_SOURCE + ":" + "ENRICH!MISSING!PID"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$._embedded.qualityassuranceevents", Matchers.hasSize(1)))
                            .andExpect(jsonPath("$._embedded.qualityassuranceevents", Matchers.containsInAnyOrder(
                                       QAEventMatcher.matchQAEventEntry(event2)
                                       )))
                            .andExpect(jsonPath("$.page.size", is(20)))
                            .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void testEventDeletion() throws Exception {

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection 1")
            .build();

        QAEvent event = QAEventBuilder.createTarget(context, col1, "Science and Freedom")
            .withTopic(QANotifyPatterns.TOPIC_ENRICH_MISSING_PID)
            .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}")
            .build();

        QAEventBuilder.createTarget(context, col1, "Science and Freedom")
            .withTopic("ENRICH/MISSING/PID")
            .withMessage("{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}")
            .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(get("/api/integration/qualityassuranceevents/" + event.getEventId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", QAEventMatcher.matchQAEventEntry(event)));

        List<QAEventProcessed> processedEvents = qaEventsDao.findAll(context);
        assertThat(processedEvents, empty());

        getClient(authToken).perform(delete("/api/integration/qualityassuranceevents/" + event.getEventId()))
            .andExpect(status().isNoContent());

        getClient(authToken).perform(get("/api/integration/qualityassuranceevents/" + event.getEventId()))
            .andExpect(status().isNotFound());

        processedEvents = qaEventsDao.findAll(context);
        assertThat(processedEvents, hasSize(1));

        QAEventProcessed processedEvent = processedEvents.get(0);
        assertThat(processedEvent.getEventId(), is(event.getEventId()));
        assertThat(processedEvent.getItem(), notNullValue());
        assertThat(processedEvent.getItem().getID().toString(), is(event.getTarget()));
        assertThat(processedEvent.getEventTimestamp(), notNullValue());
        assertThat(processedEvent.getEperson().getID(), is(admin.getID()));
    }

    @Test
    public void createQAEventByCorrectionTypeUnAuthorizedTest() throws Exception {
        getClient().perform(post("/api/integration/qualityassuranceevents")
                   .param("correctionType", "request-withdrawn")
                   .param("target", UUID.randomUUID().toString())
                   .contentType(contentType))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void createQAEventByCorrectionTypeWithMissingTargetTest() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(post("/api/integration/qualityassuranceevents")
                             .param("correctionType", "request-withdrawn")
                             .contentType(contentType))
                             .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void createQAEventsAndIgnoreAutomaticallyByScoreAndFilterTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        Item item = ItemBuilder.createItem(context, col1).withTitle("demo").build();

        QAEvent event =
            QAEventBuilder.createTarget(context, item)
                          .withSource(COAR_NOTIFY_SOURCE)
                          .withTrust(0.4)
                          .withTopic(QANotifyPatterns.TOPIC_ENRICH_MORE_REVIEW)
                          .withMessage("{\"abstracts[0]\": \"https://doi.org/10.3214/987654\"}")
                          .build();

        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(get("/api/core/items/" + item.getID()))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.metadata['datacite.relation.isReviewedBy']").doesNotExist());

        getClient(authToken).perform(get("/api/integration/qualityassuranceevents/" + event.getEventId()))
                            .andExpect(status().isNotFound());
    }

    @Test
    public void createQAEventsAndRejectAutomaticallyByScoreAndFilterTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        Item item = ItemBuilder.createItem(context, col1).withTitle("demo").build();

        QAEvent event =
            QAEventBuilder.createTarget(context, item)
                          .withSource(COAR_NOTIFY_SOURCE)
                          .withTrust(0.3)
                          .withTopic(QANotifyPatterns.TOPIC_ENRICH_MORE_REVIEW)
                          .withMessage("{\"abstracts[0]\": \"https://doi.org/10.3214/987654\"}")
                          .build();

        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(get("/api/core/items/" + item.getID()))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.metadata['datacite.relation.isReviewedBy']").doesNotExist());

        getClient(authToken).perform(get("/api/integration/qualityassuranceevents/" + event.getEventId()))
                            .andExpect(status().isNotFound());
    }

    @Test
    public void createQAEventsAndDoNothingScoreNotInRangTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        Item item = ItemBuilder.createItem(context, col1).withTitle("demo").build();

        QAEvent event =
            QAEventBuilder.createTarget(context, item)
                          .withSource(COAR_NOTIFY_SOURCE)
                          .withTrust(0.7)
                          .withTopic(QANotifyPatterns.TOPIC_ENRICH_MORE_REVIEW)
                          .withMessage("{\"abstracts[0]\": \"https://doi.org/10.3214/987654\"}")
                          .build();

        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(get("/api/core/items/" + item.getID()))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.metadata['datacite.relation.isReviewedBy']").doesNotExist());

        getClient(authToken).perform(get("/api/integration/qualityassuranceevents/" + event.getEventId())
                                .param("projection", "full"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$", QAEventMatcher.matchQAEventFullEntry(event)));
    }

    @Test
    public void createQAEventsAndDoNothingFilterNotCompatibleWithItemTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();
        Item item = ItemBuilder.createItem(context, col1).withTitle("item title").build();

        QAEvent event =
            QAEventBuilder.createTarget(context, item)
                          .withSource(COAR_NOTIFY_SOURCE)
                          .withTrust(0.8)
                          .withTopic(QANotifyPatterns.TOPIC_ENRICH_MORE_REVIEW)
                          .withMessage("{\"abstracts[0]\": \"https://doi.org/10.3214/987654\"}")
                          .build();

        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);

        getClient(authToken).perform(get("/api/core/items/" + item.getID()))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.metadata['datacite.relation.isReviewedBy']").doesNotExist());

        getClient(authToken).perform(get("/api/integration/qualityassuranceevents/" + event.getEventId())
                                .param("projection", "full"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$", QAEventMatcher.matchQAEventFullEntry(event)));
    }


    @Test
    public void createQAEventByCorrectionTypeWithdrawnRequestTest() throws Exception {
        context.turnOffAuthorisationSystem();
        configurationService.setProperty(WITHDRAWAL_REINSTATE_GROUP, "Anonymous");
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withName("Collection for Publications")
                                          .withEntityType("Publication")
                                          .build();

        Item publication = ItemBuilder.createItem(context, col)
                                      .withTitle("Publication archived item")
                                      .build();
        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/core/items/" + publication.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.inArchive", is(true)))
                             .andExpect(jsonPath("$.withdrawn", is(false)));

        AtomicReference<String> idRef = new AtomicReference<String>();

        CorrectionTypeMessageDTO message = new CorrectionTypeMessageDTO("reasone");

        String ePersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(ePersonToken).perform(post("/api/integration/qualityassuranceevents")
                               .param("correctionType", "request-withdrawn")
                               .param("target", publication.getID().toString())
                               .content(new ObjectMapper().writeValueAsBytes(message))
                               .contentType(contentType))
                               .andExpect(status().isCreated())
                               .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        getClient(adminToken).perform(get("/api/integration/qualityassuranceevents/" + idRef.get()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", is(idRef.get())))
                             .andExpect(jsonPath("$.source", is(DSPACE_USERS_SOURCE)))
                             .andExpect(jsonPath("$.title", is(publication.getName())))
                             .andExpect(jsonPath("$.topic", is("REQUEST/WITHDRAWN")))
                             .andExpect(jsonPath("$.trust", is("1.000")))
                             .andExpect(jsonPath("$.status", is("PENDING")));

        getClient(adminToken).perform(get("/api/core/items/" + publication.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.inArchive", is(true)))
                             .andExpect(jsonPath("$.withdrawn", is(false)));

        List<Operation> acceptOp = new ArrayList<Operation>();
        acceptOp.add(new ReplaceOperation("/status", QAEvent.ACCEPTED));

        getClient(adminToken).perform(patch("/api/integration/qualityassuranceevents/" + idRef.get())
                             .content(getPatchContent(acceptOp))
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isOk());

        getClient(adminToken).perform(get("/api/core/items/" + publication.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.inArchive", is(false)))
                             .andExpect(jsonPath("$.withdrawn", is(true)));
    }

    @Test
    public void createQAEventByCorrectionTypeReinstateRequestTest() throws Exception {
        context.turnOffAuthorisationSystem();
        configurationService.setProperty(WITHDRAWAL_REINSTATE_GROUP, "Anonymous");
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withName("Collection for Publications")
                                          .withEntityType("Publication")
                                          .build();

        Item publication = ItemBuilder.createItem(context, col)
                                      .withTitle("Publication archived item")
                                      .withdrawn()
                                      .build();
        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);
        String ePersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(adminToken).perform(get("/api/core/items/" + publication.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.inArchive", is(false)))
                             .andExpect(jsonPath("$.withdrawn", is(true)));

        AtomicReference<String> idRef = new AtomicReference<String>();

        ObjectMapper mapper = new ObjectMapper();
        CorrectionTypeMessageDTO dto = new CorrectionTypeMessageDTO("provided reason!");

        getClient(ePersonToken).perform(post("/api/integration/qualityassuranceevents")
                               .param("correctionType", "request-reinstate")
                               .param("target", publication.getID().toString())
                               .contentType(contentType)
                               .content(mapper.writeValueAsBytes(dto)))
                               .andExpect(status().isCreated())
                               .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        getClient(adminToken).perform(get("/api/integration/qualityassuranceevents/" + idRef.get()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", is(idRef.get())))
                             .andExpect(jsonPath("$.source", is(DSPACE_USERS_SOURCE)))
                             .andExpect(jsonPath("$.title", is(publication.getName())))
                             .andExpect(jsonPath("$.topic", is("REQUEST/REINSTATE")))
                             .andExpect(jsonPath("$.trust", is("1.000")))
                             .andExpect(jsonPath("$.status", is("PENDING")));

        getClient(adminToken).perform(get("/api/core/items/" + publication.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.inArchive", is(false)))
                             .andExpect(jsonPath("$.withdrawn", is(true)));

        List<Operation> acceptOp = new ArrayList<Operation>();
        acceptOp.add(new ReplaceOperation("/status", QAEvent.ACCEPTED));

        getClient(adminToken).perform(patch("/api/integration/qualityassuranceevents/" + idRef.get())
                             .content(getPatchContent(acceptOp))
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isOk());

        getClient(adminToken).perform(get("/api/core/items/" + publication.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.inArchive", is(true)))
                             .andExpect(jsonPath("$.withdrawn", is(false)));
    }

    @Test
    public void createQAEventOnlyUserPresentInWithdrawalReinstateGroupTest() throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson user1 = EPersonBuilder.createEPerson(context)
                                      .withEmail("eperson-test@mail.com")
                                      .withPassword(password)
                                      .build();

        Group withdrawalGroup = GroupBuilder.createGroup(context)
                                            .withName("WithdrawGroup")
                                            .addMember(user1)
                                            .build();

        configurationService.setProperty(WITHDRAWAL_REINSTATE_GROUP, withdrawalGroup.getName());

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        Collection col = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withName("Collection for Publications")
                                          .withEntityType("Publication")
                                          .build();

        Item publication = ItemBuilder.createItem(context, col)
                                      .withTitle("Publication archived item")
                                      .build();
        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);
        AtomicReference<String> idRef = new AtomicReference<String>();

        CorrectionTypeMessageDTO message = new CorrectionTypeMessageDTO("reasone");

        String ePersonToken = getAuthToken(eperson.getEmail(), password);
        // eperson is not present into the withdraw-reinstate group
        // and so cannot make the request
        getClient(ePersonToken).perform(post("/api/integration/qualityassuranceevents")
                               .param("correctionType", "request-withdrawn")
                               .param("target", publication.getID().toString())
                               .content(new ObjectMapper().writeValueAsBytes(message))
                               .contentType(contentType))
                               .andExpect(status().isUnprocessableEntity());

        String user1Token = getAuthToken(user1.getEmail(), password);
        // instead user1 is present into the withdraw-reinstate group
        getClient(user1Token).perform(post("/api/integration/qualityassuranceevents")
                             .param("correctionType", "request-withdrawn")
                             .param("target", publication.getID().toString())
                             .content(new ObjectMapper().writeValueAsBytes(message))
                             .contentType(contentType))
                             .andExpect(status().isCreated())
                             .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

        getClient(adminToken).perform(get("/api/integration/qualityassuranceevents/" + idRef.get()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.id", is(idRef.get())))
                             .andExpect(jsonPath("$.source", is(DSPACE_USERS_SOURCE)))
                             .andExpect(jsonPath("$.title", is(publication.getName())))
                             .andExpect(jsonPath("$.topic", is("REQUEST/WITHDRAWN")))
                             .andExpect(jsonPath("$.trust", is("1.000")))
                             .andExpect(jsonPath("$.status", is("PENDING")));

        getClient(adminToken).perform(get("/api/core/items/" + publication.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.inArchive", is(true)))
                             .andExpect(jsonPath("$.withdrawn", is(false)));

        List<Operation> acceptOp = new ArrayList<Operation>();
        acceptOp.add(new ReplaceOperation("/status", QAEvent.ACCEPTED));

        getClient(adminToken).perform(patch("/api/integration/qualityassuranceevents/" + idRef.get())
                             .content(getPatchContent(acceptOp))
                             .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                             .andExpect(status().isOk());

        getClient(adminToken).perform(get("/api/core/items/" + publication.getID()))
                             .andExpect(status().isOk())
                             .andExpect(jsonPath("$.inArchive", is(false)))
                             .andExpect(jsonPath("$.withdrawn", is(true)));
    }

}
