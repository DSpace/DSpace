/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.matcher.SuggestionTargetMatcher.matchSuggestionTarget;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.suggestion.SuggestionTarget;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.SuggestionTargetBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration Tests against the /api/integration/suggestiontargets endpoint
 */
public class SuggestionTargetRestRepositoryIT extends AbstractControllerIntegrationTest {
    private Collection colPeople;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        // We turn off the authorization system in order to create the structure as
        // defined below
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        colPeople = CollectionBuilder.createCollection(context, parentCommunity).withName("People")
                .withEntityType("Person").build();
        context.restoreAuthSystemState();
    }

    /**
     * Build a list of suggestion target, Bollini, Andrea has suggestion from both
     * sources, Digilio, Giuseppe only from reciter Test 0, 3, 6 from both sources,
     * Test 1, 2, 4, 5 only from ReCiter and finally Lombardi, Corrado only from
     * scopus
     */
    private void buildSuggestionTargetsList() {
        // We turn off the authorization system in order to create the structure as
        // defined below
        context.turnOffAuthorisationSystem();
        Item itemFirst = ItemBuilder.createItem(context, colPeople).withTitle("Bollini, Andrea").build();
        SuggestionTarget targetFirstReciter = SuggestionTargetBuilder.createTarget(context, itemFirst)
                .withSuggestionCount("reciter", 31).build();
        SuggestionTarget targetFirstScopus = SuggestionTargetBuilder.createTarget(context, itemFirst)
                .withSuggestionCount("scopus", 3).build();
        SuggestionTarget targetSecond = SuggestionTargetBuilder
                .createTarget(context, colPeople, "Digilio, Giuseppe", eperson).withSuggestionCount("reciter", 11)
                .build();
        for (int idx = 0; idx < 8; idx++) {
            Item item = ItemBuilder.createItem(context, colPeople).withTitle("Test " + idx).build();
            SuggestionTargetBuilder.createTarget(context, item).withSuggestionCount("reciter", idx + 3).build();
            if (idx % 3 == 0) {
                SuggestionTargetBuilder.createTarget(context, item).withSuggestionCount("scopus", idx + 7).build();
            }
        }
        Item itemLast = ItemBuilder.createItem(context, colPeople).withTitle("Lombardi, Corrado").build();
        SuggestionTarget targetLast = SuggestionTargetBuilder.createTarget(context, itemLast)
                .withSuggestionCount("scopus", 3).build();
        context.restoreAuthSystemState();
    }

    @Test
    public void findAllTest() throws Exception {
        buildSuggestionTargetsList();
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/integration/suggestiontargets"))
                .andExpect(status().isMethodNotAllowed());
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/integration/suggestiontargets")).andExpect(status().isMethodNotAllowed());
        getClient().perform(get("/api/integration/suggestiontargets")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void findBySourceTest() throws Exception {
        buildSuggestionTargetsList();
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken)
                .perform(get("/api/integration/suggestiontargets/search/findBySource").param("source", "reciter"))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.suggestiontargets", Matchers.contains(
                        matchSuggestionTarget("Bollini, Andrea", "reciter", 31),
                        matchSuggestionTarget("Digilio, Giuseppe", "reciter", 11),
                        matchSuggestionTarget("Test 7", "reciter", 10), matchSuggestionTarget("Test 6", "reciter", 9),
                        matchSuggestionTarget("Test 5", "reciter", 8), matchSuggestionTarget("Test 4", "reciter", 7),
                        matchSuggestionTarget("Test 3", "reciter", 6), matchSuggestionTarget("Test 2", "reciter", 5),
                        matchSuggestionTarget("Test 1", "reciter", 4), matchSuggestionTarget("Test 0", "reciter", 3)
                        )))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.containsString(
                                "/api/integration/suggestiontargets/search/findBySource?source=reciter")))
                .andExpect(jsonPath("$.page.size", is(20))).andExpect(jsonPath("$.page.totalElements", is(10)));
        getClient(adminToken)
                .perform(get("/api/integration/suggestiontargets/search/findBySource").param("source", "scopus"))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.suggestiontargets",
                        Matchers.containsInAnyOrder(
                                matchSuggestionTarget("Test 6", "scopus", 13),
                                matchSuggestionTarget("Test 3", "scopus", 10),
                                matchSuggestionTarget("Test 0", "scopus", 7),
                                matchSuggestionTarget("Bollini, Andrea", "scopus", 3),
                                matchSuggestionTarget("Lombardi, Corrado", "scopus", 3))))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.containsString(
                                "/api/integration/suggestiontargets/search/findBySource?source=scopus")))
                .andExpect(jsonPath("$.page.size", is(20))).andExpect(jsonPath("$.page.totalElements", is(5)));
    }

    @Test
    public void findBySourcePaginationTest() throws Exception {
        buildSuggestionTargetsList();
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken)
                .perform(get("/api/integration/suggestiontargets/search/findBySource")
                        .param("source", "reciter").param("size", "1"))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.suggestiontargets",
                        Matchers.contains(matchSuggestionTarget("Bollini, Andrea", "reciter", 31))))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/suggestiontargets/search/findBySource?"),
                                Matchers.containsString("source=reciter"), Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$._links.next.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/suggestiontargets/search/findBySource?"),
                                Matchers.containsString("source=reciter"), Matchers.containsString("page=1"),
                                Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$._links.last.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/suggestiontargets/search/findBySource?"),
                                Matchers.containsString("source=reciter"), Matchers.containsString("page=9"),
                                Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$._links.first.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/suggestiontargets/search/findBySource?"),
                                Matchers.containsString("source=reciter"), Matchers.containsString("page=0"),
                                Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$._links.prev.href").doesNotExist()).andExpect(jsonPath("$.page.size", is(1)))
                .andExpect(jsonPath("$.page.totalElements", is(10)));
        getClient(adminToken)
                .perform(get("/api/integration/suggestiontargets/search/findBySource").param("source", "reciter")
                        .param("size", "1").param("page", "1"))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.suggestiontargets",
                        Matchers.contains(matchSuggestionTarget("Digilio, Giuseppe", "reciter", 11))))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/suggestiontargets/search/findBySource?"),
                                Matchers.containsString("source=reciter"), Matchers.containsString("page=1"),
                                Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$._links.next.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/suggestiontargets/search/findBySource?"),
                                Matchers.containsString("source=reciter"), Matchers.containsString("page=2"),
                                Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$._links.last.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/suggestiontargets/search/findBySource?"),
                                Matchers.containsString("source=reciter"), Matchers.containsString("page=9"),
                                Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$._links.first.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/suggestiontargets/search/findBySource?"),
                                Matchers.containsString("source=reciter"), Matchers.containsString("page=0"),
                                Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$._links.prev.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/suggestiontargets/search/findBySource?"),
                                Matchers.containsString("source=reciter"), Matchers.containsString("page=0"),
                                Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$.page.size", is(1))).andExpect(jsonPath("$.page.totalElements", is(10)));
        getClient(adminToken)
                .perform(get("/api/integration/suggestiontargets/search/findBySource").param("source", "reciter")
                        .param("size", "1").param("page", "9"))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.suggestiontargets",
                        Matchers.contains(matchSuggestionTarget("Test 0", "reciter", 3))))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/suggestiontargets/search/findBySource?"),
                                Matchers.containsString("source=reciter"), Matchers.containsString("page=9"),
                                Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$._links.next.href").doesNotExist())
                .andExpect(jsonPath("$._links.last.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/suggestiontargets/search/findBySource?"),
                                Matchers.containsString("source=reciter"), Matchers.containsString("page=9"),
                                Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$._links.first.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/suggestiontargets/search/findBySource?"),
                                Matchers.containsString("source=reciter"), Matchers.containsString("page=0"),
                                Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$._links.prev.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/suggestiontargets/search/findBySource?"),
                                Matchers.containsString("source=reciter"), Matchers.containsString("page=8"),
                                Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$.page.size", is(1))).andExpect(jsonPath("$.page.totalElements", is(10)));
        getClient(adminToken)
                .perform(get("/api/integration/suggestiontargets/search/findBySource").param("source", "scopus")
                        .param("size", "3").param("page", "0"))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.suggestiontargets",
                        Matchers.contains(
                                matchSuggestionTarget("Test 6", "scopus", 13),
                                matchSuggestionTarget("Test 3", "scopus", 10),
                                matchSuggestionTarget("Test 0", "scopus", 7))))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/suggestiontargets/search/findBySource?"),
                                Matchers.containsString("source=scopus"), Matchers.containsString("page=0"),
                                Matchers.containsString("size=3"))))
                .andExpect(jsonPath("$._links.next.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/suggestiontargets/search/findBySource?"),
                                Matchers.containsString("source=scopus"), Matchers.containsString("page=1"),
                                Matchers.containsString("size=3"))))
                .andExpect(jsonPath("$._links.last.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/suggestiontargets/search/findBySource?"),
                                Matchers.containsString("source=scopus"), Matchers.containsString("page=1"),
                                Matchers.containsString("size=3"))))
                .andExpect(jsonPath("$._links.first.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/suggestiontargets/search/findBySource?"),
                                Matchers.containsString("source=scopus"), Matchers.containsString("page=0"),
                                Matchers.containsString("size=3"))))
                .andExpect(jsonPath("$._links.prev.href").doesNotExist()).andExpect(jsonPath("$.page.size", is(3)))
                .andExpect(jsonPath("$.page.totalElements", is(5)));

        getClient(adminToken)
                .perform(get("/api/integration/suggestiontargets/search/findBySource").param("source", "scopus")
                        .param("size", "3").param("page", "1"))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.suggestiontargets", Matchers.iterableWithSize(2)))
                .andExpect(jsonPath("$._embedded.suggestiontargets",
                        Matchers.containsInAnyOrder(matchSuggestionTarget("Bollini, Andrea", "scopus", 3),
                                matchSuggestionTarget("Lombardi, Corrado", "scopus", 3))))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/suggestiontargets/search/findBySource?"),
                                Matchers.containsString("source=scopus"), Matchers.containsString("page=1"),
                                Matchers.containsString("size=3"))))
                .andExpect(jsonPath("$._links.next.href").doesNotExist())
                .andExpect(jsonPath("$._links.last.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/suggestiontargets/search/findBySource?"),
                                Matchers.containsString("source=scopus"), Matchers.containsString("page=1"),
                                Matchers.containsString("size=3"))))
                .andExpect(jsonPath("$._links.first.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/suggestiontargets/search/findBySource?"),
                                Matchers.containsString("source=scopus"), Matchers.containsString("page=0"),
                                Matchers.containsString("size=3"))))
                .andExpect(jsonPath("$._links.prev.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/suggestiontargets/search/findBySource?"),
                                Matchers.containsString("source=scopus"), Matchers.containsString("page=0"),
                                Matchers.containsString("size=3"))))
                .andExpect(jsonPath("$.page.size", is(3))).andExpect(jsonPath("$.page.totalElements", is(5)));
    }

    @Test
    public void findBySourceUnAuthenticatedTest() throws Exception {
        buildSuggestionTargetsList();
        // anonymous cannot access the suggestions endpoint
        getClient().perform(get("/api/integration/suggestiontargets/search/findBySource").param("source", "reciter"))
                .andExpect(status().isUnauthorized());
        getClient().perform(get("/api/integration/suggestiontargets/search/findBySource").param("source", "not-exist"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void findBySourceForbiddenTest() throws Exception {
        buildSuggestionTargetsList();
        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson)
                .perform(get("/api/integration/suggestiontargets/search/findBySource").param("source", "reciter"))
                .andExpect(status().isForbidden());
        getClient(tokenEperson)
                .perform(get("/api/integration/suggestiontargets/search/findBySource").param("source", "not-exist"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void findBySourceBadRequestTest() throws Exception {
        String tokenEperson = getAuthToken(admin.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/integration/suggestiontargets/search/findBySource"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void findOneTest() throws Exception {
        context.turnOffAuthorisationSystem();
        SuggestionTarget target = SuggestionTargetBuilder.createTarget(context, colPeople, "Bollini, Andrea")
                .withSuggestionCount("scopus", 3).build();
        SuggestionTarget targetEPerson = SuggestionTargetBuilder
                .createTarget(context, colPeople, "Digilio, Giuseppe", eperson).withSuggestionCount("reciter", 11)
                .build();
        context.restoreAuthSystemState();
        String uuidStr = target.getID().toString();
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/integration/suggestiontargets/" + uuidStr)).andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", matchSuggestionTarget("Bollini, Andrea", "scopus", 3))).andExpect(jsonPath(
                        "$._links.self.href", Matchers.endsWith("/api/integration/suggestiontargets/" + uuidStr)));
        // build a person profile linked to our eperson
        String uuidStrEpersonProfile = targetEPerson.getID().toString();
        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/integration/suggestiontargets/" + uuidStrEpersonProfile))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", matchSuggestionTarget("Digilio, Giuseppe", "reciter", 11)))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.endsWith("/api/integration/suggestiontargets/" + uuidStrEpersonProfile)));
    }

    @Test
    public void findOneFullProjectionTest() throws Exception {
        context.turnOffAuthorisationSystem();
        SuggestionTarget target = SuggestionTargetBuilder.createTarget(context, colPeople, "Bollini, Andrea")
                .withSuggestionCount("scopus", 3).build();
        SuggestionTarget targetEPerson = SuggestionTargetBuilder
                .createTarget(context, colPeople, "Digilio, Giuseppe", eperson).withSuggestionCount("reciter", 11)
                .build();
        context.restoreAuthSystemState();
        String uuidStrTarget = target.getID().toString();
        String uuidStrProfile = target.getTarget().getID().toString();
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken)
                .perform(get("/api/integration/suggestiontargets/" + uuidStrTarget).param("projection", "full"))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", matchSuggestionTarget("Bollini, Andrea", "scopus", 3)))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.endsWith("/api/integration/suggestiontargets/" + uuidStrTarget)))
                .andExpect(jsonPath("$._embedded.target.id", Matchers.is(uuidStrProfile)));
        String uuidStrEpersonTarget = targetEPerson.getID().toString();
        String uuidStrEpersonProfile = targetEPerson.getTarget().getID().toString();
        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken)
                .perform(get("/api/integration/suggestiontargets/" + uuidStrEpersonTarget).param("projection", "full"))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", matchSuggestionTarget("Digilio, Giuseppe", "reciter", 11)))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.endsWith("/api/integration/suggestiontargets/" + uuidStrEpersonTarget)))
                .andExpect(jsonPath("$._embedded.target.id", Matchers.is(uuidStrEpersonProfile)));
    }

    @Test
    public void findOneUnAuthenticatedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        SuggestionTarget target = SuggestionTargetBuilder.createTarget(context, colPeople, "Bollini, Andrea")
                .withSuggestionCount("reciter", 31).build();
        context.restoreAuthSystemState();
        String uuidStr = target.getID().toString();
        getClient().perform(get("/api/integration/suggestiontargets/" + uuidStr)).andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneForbiddenTest() throws Exception {
        // build a generic person profile
        context.turnOffAuthorisationSystem();
        SuggestionTarget target = SuggestionTargetBuilder.createTarget(context, colPeople, "Bollini, Andrea")
                .withSuggestionCount("reciter", 31).build();
        context.restoreAuthSystemState();
        String uuidStr = target.getID().toString();
        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/integration/suggestiontargets/" + uuidStr))
                .andExpect(status().isForbidden());
    }

    @Test
    public void findOneTestWrongID() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/integration/suggestiontargets/not-an-uuid"))
                .andExpect(status().isNotFound());
        getClient(adminToken).perform(get("/api/integration/suggestiontargets/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
        getClient(adminToken).perform(get("/api/integration/suggestiontargets/scopus:" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
        getClient(adminToken).perform(get("/api/integration/suggestiontargets/invalid:" + UUID.randomUUID()))
                .andExpect(status().isNotFound());

        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(get("/api/integration/suggestiontargets/not-an-uuid"))
                .andExpect(status().isForbidden());
        getClient(epersonToken).perform(get("/api/integration/suggestiontargets/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
        getClient(epersonToken).perform(get("/api/integration/suggestiontargets/scopus:" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
        getClient(epersonToken).perform(get("/api/integration/suggestiontargets/invalid:" + UUID.randomUUID()))
                .andExpect(status().isForbidden());

        getClient().perform(get("/api/integration/suggestiontargets/not-an-uuid")).andExpect(status().isUnauthorized());
        getClient().perform(get("/api/integration/suggestiontargets/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        getClient().perform(get("/api/integration/suggestiontargets/scopus:" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        getClient().perform(get("/api/integration/suggestiontargets/invalid:" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void findByTargetTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item itemFirst = ItemBuilder.createItem(context, colPeople).withTitle("Bollini, Andrea").build();
        SuggestionTarget targetFirstReciter = SuggestionTargetBuilder.createTarget(context, itemFirst)
                .withSuggestionCount("reciter", 31).build();
        SuggestionTarget targetFirstScopus = SuggestionTargetBuilder.createTarget(context, itemFirst)
                .withSuggestionCount("scopus", 3).build();
        Item itemLast = ItemBuilder.createItem(context, colPeople).withTitle("Lombardi, Corrado")
                .withDSpaceObjectOwner(eperson.getFullName(), eperson.getID().toString()).build();
        SuggestionTarget targetLast = SuggestionTargetBuilder.createTarget(context, itemLast)
                .withSuggestionCount("scopus", 2).build();
        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken)
                .perform(get("/api/integration/suggestiontargets/search/findByTarget").param("target",
                        itemFirst.getID().toString()))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.suggestiontargets",
                        Matchers.contains(matchSuggestionTarget("Bollini, Andrea", "reciter", 31),
                                matchSuggestionTarget("Bollini, Andrea", "scopus", 3))))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.containsString("/api/integration/suggestiontargets/search/findByTarget?target="
                                + itemFirst.getID().toString())))
                .andExpect(jsonPath("$.page.size", is(20))).andExpect(jsonPath("$.page.totalElements", is(2)));
        getClient(adminToken)
                .perform(get("/api/integration/suggestiontargets/search/findByTarget").param("target",
                        itemLast.getID().toString()))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.suggestiontargets",
                        Matchers.contains(matchSuggestionTarget("Lombardi, Corrado", "scopus", 2))))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.containsString("/api/integration/suggestiontargets/search/findByTarget?target="
                                + itemLast.getID().toString())))
                .andExpect(jsonPath("$.page.size", is(20))).andExpect(jsonPath("$.page.totalElements", is(1)));
        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken)
                .perform(get("/api/integration/suggestiontargets/search/findByTarget").param("target",
                        itemLast.getID().toString()))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.suggestiontargets",
                        Matchers.contains(matchSuggestionTarget("Lombardi, Corrado", "scopus", 2))))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.containsString("/api/integration/suggestiontargets/search/findByTarget?target="
                                + itemLast.getID().toString())))
                .andExpect(jsonPath("$.page.size", is(20))).andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void findByTargetPaginationTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item itemFirst = ItemBuilder.createItem(context, colPeople).withTitle("Bollini, Andrea").build();
        SuggestionTarget targetFirstReciter = SuggestionTargetBuilder.createTarget(context, itemFirst)
                .withSuggestionCount("reciter", 31).build();
        SuggestionTarget targetFirstScopus = SuggestionTargetBuilder.createTarget(context, itemFirst)
                .withSuggestionCount("scopus", 3).build();
        context.restoreAuthSystemState();

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken)
                .perform(get("/api/integration/suggestiontargets/search/findByTarget").param("size", "1")
                        .param("target", itemFirst.getID().toString()))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.suggestiontargets",
                        Matchers.contains(matchSuggestionTarget("Bollini, Andrea", "reciter", 31))))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/suggestiontargets/search/findByTarget?"),
                                Matchers.containsString("target=" + itemFirst.getID().toString()),
                                Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$._links.next.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/suggestiontargets/search/findByTarget?"),
                                Matchers.containsString("target=" + itemFirst.getID().toString()),
                                Matchers.containsString("page=1"), Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$._links.last.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/suggestiontargets/search/findByTarget?"),
                                Matchers.containsString("target=" + itemFirst.getID().toString()),
                                Matchers.containsString("page=1"), Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$._links.first.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/suggestiontargets/search/findByTarget?"),
                                Matchers.containsString("target=" + itemFirst.getID().toString()),
                                Matchers.containsString("page=0"), Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$._links.prev.href").doesNotExist()).andExpect(jsonPath("$.page.size", is(1)))
                .andExpect(jsonPath("$.page.size", is(1))).andExpect(jsonPath("$.page.totalElements", is(2)));
        getClient(adminToken)
                .perform(get("/api/integration/suggestiontargets/search/findByTarget").param("size", "1")
                        .param("page", "1").param("target", itemFirst.getID().toString()))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.suggestiontargets",
                        Matchers.contains(matchSuggestionTarget("Bollini, Andrea", "scopus", 3))))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/suggestiontargets/search/findByTarget?"),
                                Matchers.containsString("target=" + itemFirst.getID().toString()),
                                Matchers.containsString("size=1"), Matchers.containsString("page=1"))))
                .andExpect(jsonPath("$._links.prev.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/suggestiontargets/search/findByTarget?"),
                                Matchers.containsString("target=" + itemFirst.getID().toString()),
                                Matchers.containsString("page=0"), Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$._links.last.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/suggestiontargets/search/findByTarget?"),
                                Matchers.containsString("target=" + itemFirst.getID().toString()),
                                Matchers.containsString("page=1"), Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$._links.first.href",
                        Matchers.allOf(
                                Matchers.containsString("/api/integration/suggestiontargets/search/findByTarget?"),
                                Matchers.containsString("target=" + itemFirst.getID().toString()),
                                Matchers.containsString("page=0"), Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$._links.next.href").doesNotExist()).andExpect(jsonPath("$.page.size", is(1)))
                .andExpect(jsonPath("$.page.size", is(1))).andExpect(jsonPath("$.page.totalElements", is(2)));
    }

    @Test
    public void findByTargetUnAuthenticatedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item itemFirst = ItemBuilder.createItem(context, colPeople).withTitle("Bollini, Andrea").build();
        SuggestionTarget targetFirstReciter = SuggestionTargetBuilder.createTarget(context, itemFirst)
                .withSuggestionCount("reciter", 31).build();
        SuggestionTarget targetFirstScopus = SuggestionTargetBuilder.createTarget(context, itemFirst)
                .withSuggestionCount("scopus", 3).build();
        Item itemLast = ItemBuilder.createItem(context, colPeople).withTitle("Lombardi, Corrado")
                .withDSpaceObjectOwner(eperson.getFullName(), eperson.getID().toString()).build();
        SuggestionTarget targetLast = SuggestionTargetBuilder.createTarget(context, itemLast)
                .withSuggestionCount("scopus", 2).build();
        context.restoreAuthSystemState();

        // anonymous cannot access the suggestions endpoint
        getClient().perform(get("/api/integration/suggestiontargets/search/findByTarget").param("target",
                itemFirst.getID().toString())).andExpect(status().isUnauthorized());
        getClient().perform(get("/api/integration/suggestiontargets/search/findByTarget").param("target",
                itemLast.getID().toString())).andExpect(status().isUnauthorized());
    }

    @Test
    public void findByTargetForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item itemFirst = ItemBuilder.createItem(context, colPeople).withTitle("Bollini, Andrea").build();
        SuggestionTarget targetFirstReciter = SuggestionTargetBuilder.createTarget(context, itemFirst)
                .withSuggestionCount("reciter", 31).build();
        SuggestionTarget targetFirstScopus = SuggestionTargetBuilder.createTarget(context, itemFirst)
                .withSuggestionCount("scopus", 3).build();
        Item itemLast = ItemBuilder.createItem(context, colPeople).withTitle("Lombardi, Corrado")
                .withDSpaceObjectOwner(eperson.getFullName(), eperson.getID().toString()).build();
        SuggestionTarget targetLast = SuggestionTargetBuilder.createTarget(context, itemLast)
                .withSuggestionCount("scopus", 2).build();
        EPerson anotherEPerson = EPersonBuilder.createEPerson(context).withEmail("another@example.com")
                .withPassword(password).withNameInMetadata("Test", "Test").build();
        context.restoreAuthSystemState();

        String tokenAnother = getAuthToken(anotherEPerson.getEmail(), password);
        getClient(tokenAnother).perform(get("/api/integration/suggestiontargets/search/findByTarget").param("target",
                itemFirst.getID().toString())).andExpect(status().isForbidden());
        getClient(tokenAnother).perform(get("/api/integration/suggestiontargets/search/findByTarget").param("target",
                itemLast.getID().toString())).andExpect(status().isForbidden());
    }

    @Test
    public void findByTargetBadRequestTest() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken)
                .perform(get("/api/integration/suggestiontargets/search/findByTarget").param("target", "not-exist"))
                .andExpect(status().isBadRequest());
        getClient(adminToken).perform(get("/api/integration/suggestiontargets/search/findByTarget"))
                .andExpect(status().isBadRequest());
        getClient().perform(get("/api/integration/suggestiontargets/search/findByTarget").param("target", "not-exist"))
                .andExpect(status().isBadRequest());
    }

}