/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
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
import org.dspace.builder.SuggestionTargetBuilder;
import org.dspace.content.Collection;
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
                .withRelationshipType("Person").build();
        context.restoreAuthSystemState();
    }

    @Test
    public void findAllTest() throws Exception {
        buildSuggestionTargetsList();

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/integration/suggestiontargets")).andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.suggestiontargets", Matchers.hasItems(
                        allOf(hasJsonPath("$.display", is("Bollini, Andrea")), hasJsonPath("$.totals.reciter", is(31)),
                                hasJsonPath("$.totals.scopus", is(3)), hasJsonPath("$.type", is("suggestiontarget"))),
                        allOf(hasJsonPath("$.display", is("Digilio, Giuseppe")),
                                hasJsonPath("$.totals.reciter", is(11)), hasJsonPath("$.totals.scopus", is(0)),
                                hasJsonPath("$.type", is("suggestiontarget"))))))
                .andExpect(
                        jsonPath("$._links.self.href", Matchers.containsString("/api/integration/suggestiontargets")))
                .andExpect(jsonPath("$.page.size", is(20))).andExpect(jsonPath("$.page.totalElements", is(11)));
    }

    private void buildSuggestionTargetsList() {
        // We turn off the authorization system in order to create the structure as
        // defined below
        context.turnOffAuthorisationSystem();
        SuggestionTarget target = SuggestionTargetBuilder.createTarget(context, colPeople)
                .withPreferredName("Bollini, Andrea").withSuggestionCount("reciter", 31)
                .withSuggestionCount("scopus", 3).build();

        SuggestionTarget targetEPerson = SuggestionTargetBuilder.createTarget(context, colPeople)
                .withPreferredName("Digilio, Giuseppe").withOwner(eperson).withSuggestionCount("reciter", 11)
                .withSuggestionCount("scopus", 0).build();

        for (int idx = 0; idx < 8; idx++) {
            SuggestionTargetBuilder.createTarget(context, colPeople).withPreferredName("Test " + idx)
                    .withSuggestionCount("reciter", idx + 3).withSuggestionCount("scopus", idx + 7).build();
        }

        SuggestionTarget targetLast = SuggestionTargetBuilder.createTarget(context, colPeople)
                .withPreferredName("Lombardi, Corrado").withSuggestionCount("reciter", 0)
                .withSuggestionCount("scopus", 3).build();
        context.restoreAuthSystemState();
    }

    @Test
    public void findAllPaginationTest() throws Exception {
        buildSuggestionTargetsList();
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/integration/suggestiontargets").param("size", "1"))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.suggestiontargets",
                        Matchers.contains(allOf(hasJsonPath("$.display", is("Bollini, Andrea")),
                                hasJsonPath("$.totals.reciter", is(31)), hasJsonPath("$.totals.scopus", is(3)),
                                hasJsonPath("$.type", is("suggestiontarget"))))))
                .andExpect(
                        jsonPath("$._links.self.href", Matchers.containsString("/api/integration/suggestiontargets")))
                .andExpect(jsonPath("$._links.next.href",
                        Matchers.containsString("/api/integration/suggestiontargets?page=1")))
                .andExpect(jsonPath("$._links.last.href",
                        Matchers.containsString("/api/integration/suggestiontargets?page=10")))
                .andExpect(jsonPath("$._links.first.href",
                        Matchers.containsString("/api/integration/suggestiontargets?page=0")))
                .andExpect(jsonPath("$._links.previous.href").doesNotExist())
                .andExpect(jsonPath("$.page.size", is(1))).andExpect(jsonPath("$.page.totalElements", is(11)));

        getClient(adminToken).perform(get("/api/integration/suggestiontargets").param("size", "1").param("page", "1"))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.suggestiontargets",
                        Matchers.contains(allOf(hasJsonPath("$.display", is("Digilio, Giuseppe")),
                                hasJsonPath("$.totals.reciter", is(11)), hasJsonPath("$.totals.scopus", is(0)),
                                hasJsonPath("$.type", is("suggestiontarget"))))))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.containsString("/api/integration/suggestiontargets?page=1")))
                .andExpect(jsonPath("$._links.next.href",
                        Matchers.containsString("/api/integration/suggestiontargets?page=2")))
                .andExpect(jsonPath("$._links.last.href",
                        Matchers.containsString("/api/integration/suggestiontargets?page=10")))
                .andExpect(jsonPath("$._links.first.href",
                        Matchers.containsString("/api/integration/suggestiontargets?page=0")))
                .andExpect(jsonPath("$._links.previous.href",
                        Matchers.containsString("/api/integration/suggestiontargets?page=0")))
                .andExpect(jsonPath("$.page.size", is(1))).andExpect(jsonPath("$.page.totalElements", is(11)));
        getClient(adminToken).perform(get("/api/integration/suggestiontargets").param("size", "1").param("page", "10"))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.suggestiontargets",
                        Matchers.contains(allOf(hasJsonPath("$.display", is("Lombardi, Corrado")),
                                hasJsonPath("$.totals.reciter", is(0)), hasJsonPath("$.totals.scopus", is(3)),
                                hasJsonPath("$.type", is("suggestiontarget"))))))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.containsString("/api/integration/suggestiontargets?page=10")))
                .andExpect(jsonPath("$._links.next.href").doesNotExist())
                .andExpect(jsonPath("$._links.last.href",
                        Matchers.containsString("/api/integration/suggestiontargets?page=10")))
                .andExpect(jsonPath("$._links.first.href",
                        Matchers.containsString("/api/integration/suggestiontargets?page=0")))
                .andExpect(jsonPath("$._links.previous.href",
                        Matchers.containsString("/api/integration/suggestiontargets?page=9")))
                .andExpect(jsonPath("$.page.size", is(1))).andExpect(jsonPath("$.page.totalElements", is(11)));
    }

    @Test
    public void findAllUnAuthenticatedTest() throws Exception {
        buildSuggestionTargetsList();
        // anonymous cannot access the suggestions endpoint
        getClient().perform(get("/api/integration/suggestiontargets")).andExpect(status().isUnauthorized());
    }

    @Test
    public void findAllForbiddenTest() throws Exception {
        buildSuggestionTargetsList();
        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/integration/suggestiontargets")).andExpect(status().isForbidden());
    }

    @Test
    public void findOneTest() throws Exception {
        context.turnOffAuthorisationSystem();
        SuggestionTarget target = SuggestionTargetBuilder.createTarget(context, colPeople)
                .withPreferredName("Bollini, Andrea").withSuggestionCount("reciter", 31)
                .withSuggestionCount("scopus", 3).build();

        SuggestionTarget targetEPerson = SuggestionTargetBuilder.createTarget(context, colPeople)
                .withPreferredName("Digilio, Giuseppe").withOwner(eperson).withSuggestionCount("reciter", 11)
                .withSuggestionCount("scopus", 0).build();
        context.restoreAuthSystemState();

        String uuidStr = target.getID().toString();
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/integration/suggestiontargets/" + uuidStr)).andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$",
                        Matchers.allOf(hasJsonPath("$.display", is("Bollini, Andrea")),
                                hasJsonPath("$.totals.reciter", is(31)), hasJsonPath("$.totals.scopus", is(3)),
                                hasJsonPath("$.type", is("suggestiontarget")))))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.endsWith("/api/integration/suggestiontargets/" + uuidStr)));

        // build a person profile linked to our eperson
        String uuidStrEpersonProfile = targetEPerson.getID().toString();

        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/integration/suggestiontargets/" + uuidStrEpersonProfile))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$",
                        Matchers.allOf(hasJsonPath("$.display", is("Digilio, Giuseppe")),
                                hasJsonPath("$.totals.reciter", is(11)), hasJsonPath("$.totals.scopus", is(0)),
                                hasJsonPath("$.type", is("suggestiontarget")))))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.endsWith("/api/integration/suggestiontargets/" + uuidStrEpersonProfile)));
    }

    @Test
    public void findOneFullProjectionTest() throws Exception {
        context.turnOffAuthorisationSystem();
        SuggestionTarget target = SuggestionTargetBuilder.createTarget(context, colPeople)
                .withPreferredName("Bollini, Andrea").withSuggestionCount("reciter", 31)
                .withSuggestionCount("scopus", 3).build();

        SuggestionTarget targetEPerson = SuggestionTargetBuilder.createTarget(context, colPeople)
                .withPreferredName("Digilio, Giuseppe").withOwner(eperson).withSuggestionCount("reciter", 11)
                .withSuggestionCount("scopus", 0).build();
        context.restoreAuthSystemState();

        String uuidStr = target.getID().toString();
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/integration/suggestiontargets/" + uuidStr).param("projection", "full"))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$",
                        Matchers.allOf(hasJsonPath("$.display", is("Bollini, Andrea")),
                                hasJsonPath("$.totals.reciter", is(31)), hasJsonPath("$.totals.scopus", is(3)),
                                hasJsonPath("$.type", is("suggestiontarget")))))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.endsWith("/api/integration/suggestiontargets/" + uuidStr)))
                .andExpect(jsonPath("$._embedded.target.id", Matchers.is(uuidStr)));

        String uuidStrEpersonProfile = targetEPerson.getID().toString();
        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken)
                .perform(get("/api/integration/suggestiontargets/" + uuidStrEpersonProfile).param("projection", "full"))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$",
                        Matchers.allOf(hasJsonPath("$.display", is("Digilio, Giuseppe")),
                                hasJsonPath("$.totals.reciter", is(11)), hasJsonPath("$.totals.scopus", is(0)),
                                hasJsonPath("$.type", is("suggestiontarget")))))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.endsWith("/api/integration/suggestiontargets/" + uuidStrEpersonProfile)))
                .andExpect(jsonPath("$._embedded.target.id", Matchers.is(uuidStrEpersonProfile)));
    }

    @Test
    public void findOneUnAuthenticatedTest() throws Exception {
            context.turnOffAuthorisationSystem();
        SuggestionTarget target = SuggestionTargetBuilder.createTarget(context, colPeople)
                .withPreferredName("Bollini, Andrea").withSuggestionCount("reciter", 31)
                .withSuggestionCount("scopus", 3).build();
        context.restoreAuthSystemState();
        String uuidStr = target.getID().toString();
        getClient().perform(get("/api/integration/suggestiontargets/" + uuidStr)).andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneForbiddenTest() throws Exception {
        // build a generic person profile
        context.turnOffAuthorisationSystem();
        SuggestionTarget target = SuggestionTargetBuilder.createTarget(context, colPeople)
                .withPreferredName("Bollini, Andrea").withSuggestionCount("reciter", 31)
                .withSuggestionCount("scopus", 3).build();
        context.restoreAuthSystemState();
        String uuidStr = target.getID().toString();
        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/integration/suggestiontargets/" + uuidStr))
                .andExpect(status().isForbidden());
    }

    @Test
    public void findOneTestWrongUUID() throws Exception {
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/integration/suggestiontargets/not-an-uuid"))
                .andExpect(status().isNotFound());
        getClient(adminToken).perform(get("/api/integration/suggestiontargets/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());

        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(get("/api/integration/suggestiontargets/not-an-uuid"))
                .andExpect(status().isForbidden());
        getClient(epersonToken).perform(get("/api/integration/suggestiontargets/" + UUID.randomUUID()))
                .andExpect(status().isForbidden());

        getClient().perform(get("/api/integration/suggestiontargets/not-an-uuid"))
                .andExpect(status().isUnauthorized());
        getClient().perform(get("/api/integration/suggestiontargets/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}
