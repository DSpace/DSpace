/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.matcher.SuggestionSourceMatcher.matchSuggestionSource;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.suggestion.SuggestionTarget;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.SuggestionTargetBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration Tests against the /api/integration/suggestionsources endpoint
 */
public class SuggestionSourceRestRepositoryIT extends AbstractControllerIntegrationTest {
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
        getClient(adminToken).perform(get("/api/integration/suggestionsources")).andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.suggestionsources",
                        Matchers.contains(matchSuggestionSource("reciter", 10), matchSuggestionSource("scopus", 5))))
                .andExpect(jsonPath("$.page.size", is(20))).andExpect(jsonPath("$.page.totalElements", is(2)));
    }

    @Test
    public void findAllPaginationTest() throws Exception {
        buildSuggestionTargetsList();
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/integration/suggestionsources").param("size", "1"))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.suggestionsources",
                        Matchers.contains(matchSuggestionSource("reciter", 10))))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.containsString("/api/integration/suggestionsources")))
                .andExpect(jsonPath("$._links.next.href",
                        Matchers.allOf(Matchers.containsString("/api/integration/suggestionsources?"),
                                Matchers.containsString("page=1"), Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$._links.last.href",
                        Matchers.allOf(Matchers.containsString("/api/integration/suggestionsources?"),
                                Matchers.containsString("page=1"), Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$._links.first.href",
                        Matchers.allOf(Matchers.containsString("/api/integration/suggestionsources?"),
                                Matchers.containsString("page=0"), Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$._links.prev.href").doesNotExist()).andExpect(jsonPath("$.page.size", is(1)))
                .andExpect(jsonPath("$.page.totalElements", is(2)));
        getClient(adminToken).perform(get("/api/integration/suggestionsources").param("size", "1").param("page", "1"))
                .andExpect(status().isOk()).andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.suggestionsources",
                        Matchers.contains(matchSuggestionSource("scopus", 5))))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.containsString("/api/integration/suggestionsources")))
                .andExpect(jsonPath("$._links.next.href").doesNotExist())
                .andExpect(jsonPath("$._links.last.href",
                        Matchers.allOf(Matchers.containsString("/api/integration/suggestionsources?"),
                                Matchers.containsString("page=1"), Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$._links.first.href",
                        Matchers.allOf(Matchers.containsString("/api/integration/suggestionsources?"),
                                Matchers.containsString("page=0"), Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$._links.prev.href",
                        Matchers.allOf(Matchers.containsString("/api/integration/suggestionsources?"),
                                Matchers.containsString("page=0"), Matchers.containsString("size=1"))))
                .andExpect(jsonPath("$.page.size", is(1))).andExpect(jsonPath("$.page.totalElements", is(2)));
    }

    @Test
    public void findAllNotAdminTest() throws Exception {
        buildSuggestionTargetsList();
        // anonymous cannot access the suggestions source endpoint
        getClient().perform(get("/api/integration/suggestionsources")).andExpect(status().isUnauthorized());
        // nor normal user
        String tokenEperson = getAuthToken(eperson.getEmail(), password);
        getClient(tokenEperson).perform(get("/api/integration/suggestionsources")).andExpect(status().isForbidden());

    }

    @Test
    public void findOneTest() throws Exception {
        buildSuggestionTargetsList();
        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(get("/api/integration/suggestionsources/reciter")).andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", matchSuggestionSource("reciter", 10))).andExpect(jsonPath("$._links.self.href",
                        Matchers.endsWith("/api/integration/suggestionsources/reciter")));
        getClient(adminToken).perform(get("/api/integration/suggestionsources/scopus")).andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", matchSuggestionSource("scopus", 5))).andExpect(
                        jsonPath("$._links.self.href", Matchers.endsWith("/api/integration/suggestionsources/scopus")));

    }

    @Test
    public void findOneNotAdminTest() throws Exception {
        buildSuggestionTargetsList();
        String epersonToken = getAuthToken(eperson.getEmail(), password);
        getClient(epersonToken).perform(get("/api/integration/suggestionsources/reciter"))
                .andExpect(status().isForbidden());
        getClient(epersonToken).perform(get("/api/integration/suggestionsources/not-exist"))
                .andExpect(status().isForbidden());
        getClient().perform(get("/api/integration/suggestionsources/reciter")).andExpect(status().isUnauthorized());
        getClient().perform(get("/api/integration/suggestionsources/not-exist")).andExpect(status().isUnauthorized());
    }

}