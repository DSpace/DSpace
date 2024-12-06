/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.matcher.QASourceMatcher.matchQASourceEntry;
import static org.dspace.qaevent.service.impl.QAEventServiceImpl.QAEVENTS_SOURCES;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.repository.QASourceRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.QAEventBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.QAEvent;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link QASourceRestRepository}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class QASourceRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    private Item target;

    @Before
    public void setup() {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withTitle("Community")
                                          .build();

        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
                                                 .withName("Collection")
                                                 .build();

        target = ItemBuilder.createItem(context, collection)
                            .withTitle("Item")
                            .build();

        context.restoreAuthSystemState();

        configurationService.setProperty(QAEVENTS_SOURCES,
                new String[] { QAEvent.OPENAIRE_SOURCE,"coar-notify", "test-source","test-source-2" });
    }

    @Test
    public void testFindAll() throws Exception {
        context.turnOffAuthorisationSystem();
        createEvent(QAEvent.OPENAIRE_SOURCE, "TOPIC/OPENAIRE/1", "Title 1");
        createEvent(QAEvent.OPENAIRE_SOURCE, "TOPIC/OPENAIRE/2", "Title 2");
        context.setCurrentUser(eperson);
        createEvent(QAEvent.OPENAIRE_SOURCE, "TOPIC/OPENAIRE/2", "Title 3");
        createEvent(QAEvent.OPENAIRE_SOURCE, "TOPIC/OPENAIRE/2", "Title 4");

        createEvent("test-source", "TOPIC/TEST/1", "Title 5");
        createEvent("test-source", "TOPIC/TEST/1", "Title 6");
        createEvent("coar-notify", "TOPIC", "Title 7");
        context.setCurrentUser(eperson);
        createEvent("coar-notify", "TOPIC", "Title 8");
        createEvent("coar-notify", "TOPIC", "Title 9");
        context.setCurrentUser(null);
        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/integration/qualityassurancesources"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.qualityassurancesources", contains(
                matchQASourceEntry(QAEvent.OPENAIRE_SOURCE, 4),
                matchQASourceEntry("coar-notify", 3),
                matchQASourceEntry("test-source", 2),
                matchQASourceEntry("test-source-2", 0))))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(4)));

        // check with our eperson submitter
        authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(get("/api/integration/qualityassurancesources"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.qualityassurancesources", contains(
                matchQASourceEntry("coar-notify", 3))))
            .andExpect(jsonPath("$.page.size", is(20)))
            .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void testFindAllUnauthorized() throws Exception {
        context.turnOffAuthorisationSystem();

        createEvent(QAEvent.OPENAIRE_SOURCE, "TOPIC/OPENAIRE/1", "Title 1");
        createEvent("test-source", "TOPIC/TEST/1", "Title 4");

        context.restoreAuthSystemState();

        getClient().perform(get("/api/integration/qualityassurancesources"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void testFindOne() throws Exception {

        context.turnOffAuthorisationSystem();
        Community com = CommunityBuilder.createCommunity(context).withName("Test community").build();
        Collection col = CollectionBuilder.createCollection(context, com).withName("Test collection").build();

        createEvent(QAEvent.OPENAIRE_SOURCE, "TOPIC/OPENAIRE/1", "Title 1");
        createEvent(QAEvent.OPENAIRE_SOURCE, "TOPIC/OPENAIRE/2", "Title 2");
        createEvent(QAEvent.OPENAIRE_SOURCE, "TOPIC/OPENAIRE/2", "Title 3");

        createEvent("test-source", "TOPIC/TEST/1", "Title 4");
        createEvent("test-source", "TOPIC/TEST/1", "Title 5");
        context.setCurrentUser(admin);
        Item target1 = ItemBuilder.createItem(context, col).withTitle("Title 7").build();
        createEvent(QAEvent.COAR_NOTIFY_SOURCE, "TOPIC", target1);
        context.setCurrentUser(eperson);
        createEvent(QAEvent.COAR_NOTIFY_SOURCE, "TOPIC", "Title 8");
        createEvent(QAEvent.COAR_NOTIFY_SOURCE, "TOPIC", "Title 9");
        context.setCurrentUser(null);
        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/integration/qualityassurancesources/openaire"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$", matchQASourceEntry(QAEvent.OPENAIRE_SOURCE, 3)));

        getClient(authToken).perform(get("/api/integration/qualityassurancesources/coar-notify"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$", matchQASourceEntry("coar-notify", 3)));


        getClient(authToken).perform(get("/api/integration/qualityassurancesources/test-source"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$", matchQASourceEntry("test-source", 2)));

        getClient(authToken).perform(get("/api/integration/qualityassurancesources/test-source-2"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$", matchQASourceEntry("test-source-2", 0)));

        getClient(authToken).perform(get("/api/integration/qualityassurancesources/unknown-test-source"))
            .andExpect(status().isNotFound());

        authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(get("/api/integration/qualityassurancesources/openaire"))
            .andExpect(status().isForbidden());
        getClient(authToken).perform(get("/api/integration/qualityassurancesources/unknown-test-source"))
            .andExpect(status().isForbidden());
        // the eperson will see only 2 events in coar-notify as 1 is related to an item was submitted by other
        getClient(authToken).perform(get("/api/integration/qualityassurancesources/coar-notify"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$", matchQASourceEntry("coar-notify", 2)));


    }

    @Test
    public void testFindOneForbidden() throws Exception {

        context.turnOffAuthorisationSystem();

        createEvent(QAEvent.OPENAIRE_SOURCE, "TOPIC/OPENAIRE/1", "Title 1");
        createEvent("test-source", "TOPIC/TEST/1", "Title 4");

        context.restoreAuthSystemState();

        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/integration/qualityassurancesources/openaire"))
            .andExpect(status().isForbidden());

    }

    @Test
    public void testFindOneUnauthorized() throws Exception {

        context.turnOffAuthorisationSystem();

        createEvent(QAEvent.OPENAIRE_SOURCE, "TOPIC/OPENAIRE/1", "Title 1");
        createEvent("test-source", "TOPIC/TEST/1", "Title 4");

        context.restoreAuthSystemState();

        getClient().perform(get("/api/integration/qualityassurancesources/openaire"))
            .andExpect(status().isUnauthorized());

    }

    @Test
    public void testFindAllByTarget() throws Exception {

        context.turnOffAuthorisationSystem();
        Community com = CommunityBuilder.createCommunity(context).withName("Test community").build();
        Collection col = CollectionBuilder.createCollection(context, com).withName("Test collection").build();
        Item target1 = ItemBuilder.createItem(context, col).withTitle("Test item1").build();
        Item target2 = ItemBuilder.createItem(context, col).withTitle("Test item2").build();
        createEvent(QAEvent.OPENAIRE_SOURCE, "TOPIC/OPENAIRE/1", target1);
        createEvent(QAEvent.OPENAIRE_SOURCE, "TOPIC/OPENAIRE/2", target1);
        createEvent("test-source", "TOPIC/TEST/1", target1);
        createEvent("test-source", "TOPIC/TEST/1", target2);

        context.setCurrentUser(eperson);
        Item target3 = ItemBuilder.createItem(context, col).withTitle("Test item3").build();
        context.setCurrentUser(null);
        createEvent(QAEvent.COAR_NOTIFY_SOURCE, "TOPIC", target3);
        createEvent(QAEvent.COAR_NOTIFY_SOURCE, "TOPIC2", target3);
        createEvent(QAEvent.COAR_NOTIFY_SOURCE, "TOPIC", target2);
        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
                .perform(get("/api/integration/qualityassurancesources/search/byTarget").param("target",
                        target1.getID().toString()))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.qualityassurancesources",
                        contains(matchQASourceEntry(QAEvent.OPENAIRE_SOURCE + ":" + target1.getID().toString(), 2),
                                matchQASourceEntry("test-source:" + target1.getID().toString(), 1))))
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", is(2)));

        getClient(authToken)
                .perform(get("/api/integration/qualityassurancesources/search/byTarget").param("target",
                        target2.getID().toString()))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.qualityassurancesources",
                        contains(
                                matchQASourceEntry(QAEvent.COAR_NOTIFY_SOURCE + ":" + target2.getID().toString(), 1),
                                matchQASourceEntry("test-source:" + target2.getID().toString(), 1))))
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", is(2)));
        getClient(authToken)
                .perform(get("/api/integration/qualityassurancesources/search/byTarget").param("target",
                        target3.getID().toString()))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.qualityassurancesources",
                    contains(matchQASourceEntry("coar-notify:" + target3.getID().toString(), 2))))
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", is(1)));

        // check with our eperson submitter
        authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken)
                .perform(get("/api/integration/qualityassurancesources/search/byTarget").param("target",
                        target1.getID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", is(0)));
        getClient(authToken)
                .perform(get("/api/integration/qualityassurancesources/search/byTarget").param("target",
                        target2.getID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", is(1)));
        getClient(authToken)
                .perform(get("/api/integration/qualityassurancesources/search/byTarget").param("target",
                        target3.getID().toString()))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.qualityassurancesources",
                        contains(matchQASourceEntry("coar-notify:" + target3.getID().toString(), 2))))
                .andExpect(jsonPath("$.page.size", is(20)))
                .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void testFindByTargetBadRequest() throws Exception {

        context.turnOffAuthorisationSystem();
        Community com = CommunityBuilder.createCommunity(context).withName("Test community").build();
        Collection col = CollectionBuilder.createCollection(context, com).withName("Test collection").build();
        Item target1 = ItemBuilder.createItem(context, col).withTitle("Test item1").build();
        Item target2 = ItemBuilder.createItem(context, col).withTitle("Test item2").build();
        createEvent(QAEvent.OPENAIRE_SOURCE, "TOPIC/OPENAIRE/1", target1);
        createEvent(QAEvent.OPENAIRE_SOURCE, "TOPIC/OPENAIRE/2", target1);
        createEvent("test-source", "TOPIC/TEST/1", target1);
        createEvent("test-source", "TOPIC/TEST/1", target2);

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken)
                .perform(get("/api/integration/qualityassurancesources/search/byTarget"))
                .andExpect(status().isBadRequest());
    }


    @Test
    public void testFindByTargetUnauthorized() throws Exception {

        context.turnOffAuthorisationSystem();
        Community com = CommunityBuilder.createCommunity(context).withName("Test community").build();
        Collection col = CollectionBuilder.createCollection(context, com).withName("Test collection").build();
        Item target1 = ItemBuilder.createItem(context, col).withTitle("Test item1").build();
        Item target2 = ItemBuilder.createItem(context, col).withTitle("Test item2").build();
        createEvent(QAEvent.OPENAIRE_SOURCE, "TOPIC/OPENAIRE/1", target1);
        createEvent(QAEvent.OPENAIRE_SOURCE, "TOPIC/OPENAIRE/2", target1);
        createEvent("test-source", "TOPIC/TEST/1", target1);
        createEvent("test-source", "TOPIC/TEST/1", target2);

        context.restoreAuthSystemState();

        getClient()
                .perform(get("/api/integration/qualityassurancesources/search/byTarget").param("target",
                        target1.getID().toString()))
                .andExpect(status().isUnauthorized());
    }

    private QAEvent createEvent(String source, String topic, String title) {
        return QAEventBuilder.createTarget(context, target)
            .withSource(source)
            .withTopic(topic)
            .withTitle(title)
            .build();
    }

    private QAEvent createEvent(String source, String topic, Item item) {
        return QAEventBuilder.createTarget(context, item)
            .withSource(source)
            .withTopic(topic)
            .withTitle(item.getName())
            .build();
    }

}
