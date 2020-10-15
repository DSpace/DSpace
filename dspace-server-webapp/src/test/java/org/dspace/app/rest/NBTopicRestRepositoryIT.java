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

import org.dspace.app.nbevent.service.NBEventService;
import org.dspace.app.rest.matcher.EPersonMatcher;
import org.dspace.app.rest.matcher.HalMatcher;
import org.dspace.app.rest.matcher.NBTopicMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.EPersonBuilder;
import org.dspace.content.NBEvent;
import org.dspace.eperson.EPerson;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class NBTopicRestRepositoryIT extends AbstractControllerIntegrationTest {
    @Autowired
    private NBEventService nbEventService;

    @Test
    public void findAllTest() throws Exception {
        context.turnOffAuthorisationSystem();
        NBEvent event1 = new NBEvent("oai:www.openstarts.units.it:10077/22730", "ITEM-UUUID", "Science and Freedom",
                "ENRICH/MISSING/DOI", 0.375, "{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}",
                new Date());
        nbEventService.store(context, event1);

        NBEvent event2 = new NBEvent("oai:www.openstarts.units.it:10077/22730", "ITEM-UUUID", "Science and Freedom",
                "ENRICH/MISSING/PID", 0.375, "{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}",
                new Date());
        NBEvent event3 = new NBEvent("oai:www.openstarts.units.it:10077/22730", "ITEM-UUUID", "Science and Freedom",
                "ENRICH/MISSING/DOI", 0.375, "{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}",
                new Date());
        NBEvent event4 = new NBEvent("oai:www.openstarts.units.it:10077/22730", "ITEM-UUUID", "Science and Freedom",
                "ENRICH/MISSING/ABSTRACT", 0.375, "{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}",
                new Date());
        NBEvent event5 = new NBEvent("oai:www.openstarts.units.it:10077/22730", "ITEM-UUUID", "Science and Freedom",
                "ENRICH/MISSING/ABSTRACT", 0.375, "{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}",
                new Date());
        NBEvent event6 = new NBEvent("oai:www.openstarts.units.it:10077/22730", "ITEM-UUUID", "Science and Freedom",
                "ENRICH/MISSING/ABSTRACT", 0.375, "{\"pids[0].type\":\"doi\",\"pids[0].value\":\"10.2307/2144300\"}",
                new Date());
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
    public void findAllUnauthorizedTest() throws Exception {
        // Access endpoint without being authenticated
        getClient().perform(get("/api/integration/nbtopics")).andExpect(status().isUnauthorized());
    }

    @Test
    public void findAllForbiddenTest() throws Exception {
        String authToken = getAuthToken(eperson.getEmail(), password);
        // Access endpoint logged in as an unprivileged user
        getClient(authToken).perform(get("/api/integration/nbtopics")).andExpect(status().isForbidden());
    }

    @Test
    @Ignore
    public void findAllPaginationTest() throws Exception {
        context.turnOffAuthorisationSystem();

        context.restoreAuthSystemState();

//        String authToken = getAuthToken(admin.getEmail(), password);
        // NOTE: /eperson/epersons endpoint returns users sorted by email
        // using size = 2 the first page will contain our new test user and default
        // 'admin' ONLY
//        getClient(authToken).perform(get("/api/eperson/epersons").param("size", "2")).andExpect(status().isOk())
//                .andExpect(content().contentType(contentType))
//                .andExpect(jsonPath("$._embedded.epersons",
//                        Matchers.containsInAnyOrder(EPersonMatcher.matchEPersonEntry(testEPerson),
//                                EPersonMatcher.matchEPersonOnEmail(admin.getEmail()))))
//                .andExpect(jsonPath("$._embedded.epersons",
//                        Matchers.not(Matchers.contains(EPersonMatcher.matchEPersonOnEmail(eperson.getEmail())))))
//                .andExpect(jsonPath("$.page.size", is(2))).andExpect(jsonPath("$.page.totalElements", is(3)));
//
//        // using size = 2 the *second* page will contains our default 'eperson' ONLY
//        getClient(authToken).perform(get("/api/eperson/epersons").param("size", "2").param("page", "1"))
//                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
//                .andExpect(jsonPath("$._embedded.epersons",
//                        Matchers.contains(EPersonMatcher.matchEPersonOnEmail(eperson.getEmail()))))
//                .andExpect(jsonPath("$._embedded.epersons", Matchers.hasSize(1)))
//                .andExpect(jsonPath("$.page.size", is(2))).andExpect(jsonPath("$.page.totalElements", is(3)));
//
//        getClient().perform(get("/api/eperson/epersons")).andExpect(status().isUnauthorized());
    }

    @Test
    @Ignore
    public void findOneTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson ePerson = EPersonBuilder.createEPerson(context).withNameInMetadata("John", "Doe")
                .withEmail("Johndoe@example.com").build();

        EPerson ePerson2 = EPersonBuilder.createEPerson(context).withNameInMetadata("Jane", "Smith")
                .withEmail("janesmith@example.com").build();

        context.restoreAuthSystemState();

        // When full projection is requested, response should include expected
        // properties, links, and embeds.
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/eperson/epersons/" + ePerson2.getID()).param("projection", "full"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", EPersonMatcher.matchFullEmbeds()))
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", is(EPersonMatcher.matchEPersonEntry(ePerson2))))
                .andExpect(jsonPath("$", Matchers.not(is(EPersonMatcher.matchEPersonEntry(ePerson)))));

        // When no projection is requested, response should include expected properties,
        // links, and no embeds.
        getClient(authToken).perform(get("/api/eperson/epersons/" + ePerson2.getID())).andExpect(status().isOk())
                .andExpect(jsonPath("$", HalMatcher.matchNoEmbeds()));

    }

    @Test
    @Ignore
    public void findOneForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson ePerson1 = EPersonBuilder.createEPerson(context).withNameInMetadata("Mik", "Reck")
                .withEmail("MikReck@email.com").withPassword("qwerty01").build();

        EPerson ePerson2 = EPersonBuilder.createEPerson(context).withNameInMetadata("Bob", "Smith")
                .withEmail("bobsmith@example.com").build();

        context.restoreAuthSystemState();

        String tokenEperson1 = getAuthToken(ePerson1.getEmail(), "qwerty01");
        getClient(tokenEperson1).perform(get("/api/eperson/epersons/" + ePerson2.getID()))
                .andExpect(status().isForbidden());
    }
}
