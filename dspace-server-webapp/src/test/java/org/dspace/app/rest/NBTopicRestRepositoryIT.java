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

import java.util.Date;

import org.dspace.app.rest.matcher.NBTopicMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.NBEventBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.NBEvent;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;

public class NBTopicRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    @Ignore
    public void findAllTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        //create collection
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        //create items
        Item publicItem1 = ItemBuilder.createItem(context, col1).withTitle("Public item 1").withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John").withSubject("ExtraEntry").build();
        Item publicItem2 = ItemBuilder.createItem(context, col1).withTitle("Public item 2").withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withSubject("TestingForMore").build();
        Item publicItem3 = ItemBuilder.createItem(context, col1).withTitle("Public item 3").withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withSubject("AnotherTest")
                .withSubject("TestingForMore").build();
        Item publicItem4 = ItemBuilder.createItem(context, col1).withTitle("Public item 3").withIssueDate("2016-02-14")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withSubject("AnotherTest")
                .withSubject("Item4").build();
        Item publicItem5 = ItemBuilder.createItem(context, col1).withTitle("Public item 3").withIssueDate("2016-02-15")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withSubject("AnotherTest")
                .withSubject("Item5Subject").build();
        Item publicItem6 = ItemBuilder.createItem(context, col1).withTitle("Public item 3").withIssueDate("2016-02-16")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withSubject("AnotherTest")
                .withSubject("Item6Subject").build();

        //create event
        NBEvent event1 = new NBEvent("oai:www.openstarts.units.it:" + publicItem1.getHandle(),
                publicItem1.getID().toString(), "Science and Freedom", "ENRICH/MISSING/DOI", 0.375,
                "{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}", new Date());
        NBEvent event2 = new NBEvent("oai:www.openstarts.units.it:" + publicItem2.getHandle(),
                publicItem2.getID().toString(), "Science and Freedom", "ENRICH/MISSING/PID", 0.375,
                "{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}", new Date());
        NBEvent event3 = new NBEvent("oai:www.openstarts.units.it:" + publicItem3.getHandle(),
                publicItem3.getID().toString(), "Science and Freedom", "ENRICH/MISSING/DOI", 0.375,
                "{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}", new Date());
        NBEvent event4 = new NBEvent("oai:www.openstarts.units.it:" + publicItem4.getHandle(),
                publicItem4.getID().toString(), "Science and Freedom", "ENRICH/MISSING/ABSTRACT", 0.375,
                "{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}", new Date());
        NBEvent event5 = new NBEvent("oai:www.openstarts.units.it:" + publicItem5.getHandle(),
                publicItem5.getID().toString(), "Science and Freedom", "ENRICH/MISSING/ABSTRACT", 0.375,
                "{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}", new Date());
        NBEvent event6 = new NBEvent("oai:www.openstarts.units.it:" + publicItem5.getHandle(),
                publicItem6.getID().toString(), "Science and Freedom", "ENRICH/MISSING/ABSTRACT", 0.375,
                "{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}", new Date());

        //save item
        NBEventBuilder builder1 = NBEventBuilder.createTarget(context, col1, event1.getTitle())
                .withTopic(event1.getTopic()).withLastUpdate(new Date()).withMessage(event1.getMessage())
                .withOriginalId(event1.getOriginalId()).withTitle(event1.getTitle()).withTrust(event1.getTrust());
        event1 = builder1.build();
        NBEventBuilder builder2 = NBEventBuilder.createTarget(context, col1, event2.getTitle())
                .withTopic(event2.getTopic()).withLastUpdate(new Date()).withMessage(event2.getMessage())
                .withOriginalId(event2.getOriginalId()).withTitle(event2.getTitle()).withTrust(event2.getTrust());
        event2 = builder2.build();
        NBEventBuilder builder3 = NBEventBuilder.createTarget(context, col1, event3.getTitle())
                .withTopic(event3.getTopic()).withLastUpdate(new Date()).withMessage(event3.getMessage())
                .withOriginalId(event3.getOriginalId()).withTitle(event3.getTitle()).withTrust(event3.getTrust());
        event3 = builder3.build();
        NBEventBuilder builder4 = NBEventBuilder.createTarget(context, col1, event4.getTitle())
                .withTopic(event4.getTopic()).withLastUpdate(new Date()).withMessage(event4.getMessage())
                .withOriginalId(event4.getOriginalId()).withTitle(event4.getTitle()).withTrust(event4.getTrust());
        event4 = builder4.build();
        NBEventBuilder builder5 = NBEventBuilder.createTarget(context, col1, event5.getTitle())
                .withTopic(event5.getTopic()).withLastUpdate(new Date()).withMessage(event5.getMessage())
                .withOriginalId(event5.getOriginalId()).withTitle(event5.getTitle()).withTrust(event5.getTrust());
        event5 = builder5.build();
        NBEventBuilder builder6 = NBEventBuilder.createTarget(context, col1, event6.getTitle())
                .withTopic(event6.getTopic()).withLastUpdate(new Date()).withMessage(event6.getMessage())
                .withOriginalId(event6.getOriginalId()).withTitle(event6.getTitle()).withTrust(event6.getTrust());
        event6 = builder6.build();

        //test result
        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/integration/nbtopics")).andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.nbtopics",
                        Matchers.containsInAnyOrder(NBTopicMatcher.matchNBTopicEntry("ENRICH/MISSING/PID", 1),
                                NBTopicMatcher.matchNBTopicEntry("ENRICH/MISSING/ABSTRACT", 3),
                                NBTopicMatcher.matchNBTopicEntry("ENRICH/MISSING/DOI", 2))))
                .andExpect(jsonPath("$.page.size", is(20))).andExpect(jsonPath("$.page.totalElements", is(3)));

    }

    @Test
    @Ignore
    public void findAllUnauthorizedTest() throws Exception {
        getClient().perform(get("/api/integration/nbtopics")).andExpect(status().isUnauthorized());
    }

    @Test
    @Ignore
    public void findAllForbiddenTest() throws Exception {
        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(get("/api/integration/nbtopics")).andExpect(status().isForbidden());
    }

    @Test
    @Ignore
    public void findAllPaginationTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        //create collection
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        //create items
        Item publicItem1 = ItemBuilder.createItem(context, col1).withTitle("Public item 1").withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John").withSubject("ExtraEntry").build();
        Item publicItem2 = ItemBuilder.createItem(context, col1).withTitle("Public item 2").withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withSubject("TestingForMore").build();
        Item publicItem3 = ItemBuilder.createItem(context, col1).withTitle("Public item 3").withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withSubject("AnotherTest")
                .withSubject("TestingForMore").build();
        Item publicItem4 = ItemBuilder.createItem(context, col1).withTitle("Public item 3").withIssueDate("2016-02-14")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withSubject("AnotherTest")
                .withSubject("Item4").build();
        Item publicItem5 = ItemBuilder.createItem(context, col1).withTitle("Public item 3").withIssueDate("2016-02-15")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withSubject("AnotherTest")
                .withSubject("Item5Subject").build();
        Item publicItem6 = ItemBuilder.createItem(context, col1).withTitle("Public item 3").withIssueDate("2016-02-16")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withSubject("AnotherTest")
                .withSubject("Item6Subject").build();

        //create event
        NBEvent event1 = new NBEvent("oai:www.openstarts.units.it:" + publicItem1.getHandle(),
                publicItem1.getID().toString(), "Science and Freedom", "ENRICH/MISSING/DOI", 0.375,
                "{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}", new Date());
        NBEvent event2 = new NBEvent("oai:www.openstarts.units.it:" + publicItem2.getHandle(),
                publicItem2.getID().toString(), "Science and Freedom", "ENRICH/MISSING/PID", 0.375,
                "{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}", new Date());
        NBEvent event3 = new NBEvent("oai:www.openstarts.units.it:" + publicItem3.getHandle(),
                publicItem3.getID().toString(), "Science and Freedom", "ENRICH/MISSING/DOI", 0.375,
                "{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}", new Date());
        NBEvent event4 = new NBEvent("oai:www.openstarts.units.it:" + publicItem4.getHandle(),
                publicItem4.getID().toString(), "Science and Freedom", "ENRICH/MISSING/ABSTRACT", 0.375,
                "{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}", new Date());
        NBEvent event5 = new NBEvent("oai:www.openstarts.units.it:" + publicItem5.getHandle(),
                publicItem5.getID().toString(), "Science and Freedom", "ENRICH/MISSING/ABSTRACT", 0.375,
                "{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}", new Date());
        NBEvent event6 = new NBEvent("oai:www.openstarts.units.it:" + publicItem5.getHandle(),
                publicItem6.getID().toString(), "Science and Freedom", "ENRICH/MISSING/ABSTRACT", 0.375,
                "{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}", new Date());

        //save item
        NBEventBuilder builder1 = NBEventBuilder.createTarget(context, col1, event1.getTitle())
                .withTopic(event1.getTopic()).withLastUpdate(new Date()).withMessage(event1.getMessage())
                .withOriginalId(event1.getOriginalId()).withTitle(event1.getTitle()).withTrust(event1.getTrust());
        event1 = builder1.build();
        NBEventBuilder builder2 = NBEventBuilder.createTarget(context, col1, event2.getTitle())
                .withTopic(event2.getTopic()).withLastUpdate(new Date()).withMessage(event2.getMessage())
                .withOriginalId(event2.getOriginalId()).withTitle(event2.getTitle()).withTrust(event2.getTrust());
        event2 = builder2.build();
        NBEventBuilder builder3 = NBEventBuilder.createTarget(context, col1, event3.getTitle())
                .withTopic(event3.getTopic()).withLastUpdate(new Date()).withMessage(event3.getMessage())
                .withOriginalId(event3.getOriginalId()).withTitle(event3.getTitle()).withTrust(event3.getTrust());
        event3 = builder3.build();
        NBEventBuilder builder4 = NBEventBuilder.createTarget(context, col1, event4.getTitle())
                .withTopic(event4.getTopic()).withLastUpdate(new Date()).withMessage(event4.getMessage())
                .withOriginalId(event4.getOriginalId()).withTitle(event4.getTitle()).withTrust(event4.getTrust());
        event4 = builder4.build();
        NBEventBuilder builder5 = NBEventBuilder.createTarget(context, col1, event5.getTitle())
                .withTopic(event5.getTopic()).withLastUpdate(new Date()).withMessage(event5.getMessage())
                .withOriginalId(event5.getOriginalId()).withTitle(event5.getTitle()).withTrust(event5.getTrust());
        event5 = builder5.build();
        NBEventBuilder builder6 = NBEventBuilder.createTarget(context, col1, event6.getTitle())
                .withTopic(event6.getTopic()).withLastUpdate(new Date()).withMessage(event6.getMessage())
                .withOriginalId(event6.getOriginalId()).withTitle(event6.getTitle()).withTrust(event6.getTrust());
        event6 = builder6.build();

        //test result
        context.restoreAuthSystemState();
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/integration/nbtopics").param("size", "5").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.page.size", is(5))).andExpect(jsonPath("$.page.totalElements", is(5)));
        getClient(authToken).perform(get("/api/integration/nbtopics").param("size", "5").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.page.size", is(5))).andExpect(jsonPath("$.page.totalElements", is(1)));

    }

    @Test
    public void findOneTest() throws Exception {
        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();

        //create collection
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        //create items
        Item publicItem1 = ItemBuilder.createItem(context, col1).withTitle("Public item 1").withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald").withAuthor("Doe, John").withSubject("ExtraEntry").build();
        Item publicItem2 = ItemBuilder.createItem(context, col1).withTitle("Public item 2").withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withSubject("TestingForMore").build();
        Item publicItem3 = ItemBuilder.createItem(context, col1).withTitle("Public item 3").withIssueDate("2016-02-13")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withSubject("AnotherTest")
                .withSubject("TestingForMore").build();
        Item publicItem4 = ItemBuilder.createItem(context, col1).withTitle("Public item 3").withIssueDate("2016-02-14")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withSubject("AnotherTest")
                .withSubject("Item4").build();
        Item publicItem5 = ItemBuilder.createItem(context, col1).withTitle("Public item 3").withIssueDate("2016-02-15")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withSubject("AnotherTest")
                .withSubject("Item5Subject").build();
        Item publicItem6 = ItemBuilder.createItem(context, col1).withTitle("Public item 3").withIssueDate("2016-02-16")
                .withAuthor("Smith, Maria").withAuthor("Doe, Jane").withSubject("AnotherTest")
                .withSubject("Item6Subject").build();

        //create event
        NBEvent event1 = new NBEvent("oai:www.openstarts.units.it:" + publicItem1.getHandle(),
                publicItem1.getID().toString(), "Science and Freedom", "ENRICH/MISSING/DOI", 0.375,
                "{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}", new Date());
        NBEvent event2 = new NBEvent("oai:www.openstarts.units.it:" + publicItem2.getHandle(),
                publicItem2.getID().toString(), "Science and Freedom", "ENRICH/MISSING/PID", 0.375,
                "{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}", new Date());
        NBEvent event3 = new NBEvent("oai:www.openstarts.units.it:" + publicItem3.getHandle(),
                publicItem3.getID().toString(), "Science and Freedom", "ENRICH/MISSING/DOI", 0.375,
                "{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}", new Date());
        NBEvent event4 = new NBEvent("oai:www.openstarts.units.it:" + publicItem4.getHandle(),
                publicItem4.getID().toString(), "Science and Freedom", "ENRICH/MISSING/ABSTRACT", 0.375,
                "{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}", new Date());
        NBEvent event5 = new NBEvent("oai:www.openstarts.units.it:" + publicItem5.getHandle(),
                publicItem5.getID().toString(), "Science and Freedom", "ENRICH/MISSING/ABSTRACT", 0.375,
                "{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}", new Date());
        NBEvent event6 = new NBEvent("oai:www.openstarts.units.it:" + publicItem5.getHandle(),
                publicItem6.getID().toString(), "Science and Freedom", "ENRICH/MISSING/ABSTRACT", 0.375,
                "{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}", new Date());

        //save item
        NBEventBuilder builder1 = NBEventBuilder.createTarget(context, col1, event1.getTitle())
                .withTopic(event1.getTopic()).withLastUpdate(new Date()).withMessage(event1.getMessage())
                .withOriginalId(event1.getOriginalId()).withTitle(event1.getTitle()).withTrust(event1.getTrust());
        event1 = builder1.build();
        NBEventBuilder builder2 = NBEventBuilder.createTarget(context, col1, event2.getTitle())
                .withTopic(event2.getTopic()).withLastUpdate(new Date()).withMessage(event2.getMessage())
                .withOriginalId(event2.getOriginalId()).withTitle(event2.getTitle()).withTrust(event2.getTrust());
        event2 = builder2.build();
        NBEventBuilder builder3 = NBEventBuilder.createTarget(context, col1, event3.getTitle())
                .withTopic(event3.getTopic()).withLastUpdate(new Date()).withMessage(event3.getMessage())
                .withOriginalId(event3.getOriginalId()).withTitle(event3.getTitle()).withTrust(event3.getTrust());
        event3 = builder3.build();
        NBEventBuilder builder4 = NBEventBuilder.createTarget(context, col1, event4.getTitle())
                .withTopic(event4.getTopic()).withLastUpdate(new Date()).withMessage(event4.getMessage())
                .withOriginalId(event4.getOriginalId()).withTitle(event4.getTitle()).withTrust(event4.getTrust());
        event4 = builder4.build();
        NBEventBuilder builder5 = NBEventBuilder.createTarget(context, col1, event5.getTitle())
                .withTopic(event5.getTopic()).withLastUpdate(new Date()).withMessage(event5.getMessage())
                .withOriginalId(event5.getOriginalId()).withTitle(event5.getTitle()).withTrust(event5.getTrust());
        event5 = builder5.build();
        NBEventBuilder builder6 = NBEventBuilder.createTarget(context, col1, event6.getTitle())
                .withTopic(event6.getTopic()).withLastUpdate(new Date()).withMessage(event6.getMessage())
                .withOriginalId(event6.getOriginalId()).withTitle(event6.getTitle()).withTrust(event6.getTrust());
        event6 = builder6.build();

        //test result
        context.restoreAuthSystemState();
        try {
            String authToken = getAuthToken(admin.getEmail(), password);
            getClient(authToken).perform(get("/api/integration/nbtopics/ENRICH!MISSING!DOI"))
                .andExpect(jsonPath("$._embedded.nbtopics",
                    Matchers.containsInAnyOrder(NBTopicMatcher.matchNBTopicEntry("ENRICH/MISSING/PID", 1))));
            getClient(authToken).perform(get("/api/integration/nbtopics/ENRICH!MISSING!ABSTRACT"))
            .andExpect(jsonPath("$._embedded.nbtopics",
                Matchers.containsInAnyOrder(NBTopicMatcher.matchNBTopicEntry("ENRICH!MISSING!ABSTRACT", 3))));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @Ignore
    public void findOneUnauthorizedTest() throws Exception {
        getClient().perform(get("/api/integration/nbtopics/ENRICH!MISSING!DOI")).andExpect(status().isUnauthorized());
    }

    @Test
    @Ignore
    public void findOneForbiddenTest() throws Exception {
        String authToken = getAuthToken(eperson.getEmail(), password);
        getClient(authToken).perform(get("/api/integration/nbtopics/ENRICH!MISSING!DOI"))
            .andExpect(status().isForbidden());
    }

}
