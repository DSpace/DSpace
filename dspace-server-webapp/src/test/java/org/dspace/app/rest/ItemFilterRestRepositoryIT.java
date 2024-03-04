/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.repository.ItemFilterRestRepository;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.junit.Test;

/**
 * Integration test for {@link ItemFilterRestRepository}
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class ItemFilterRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Test
    public void findOneUnauthorizedTest() throws Exception {
        getClient().perform(get("/api/config/itemfilters/test"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneForbiddenTest() throws Exception {
        getClient(getAuthToken(eperson.getEmail(), password))
            .perform(get("/api/config/itemfilters/test"))
            .andExpect(status().isForbidden());
    }

    @Test
    public void findOneNotFoundTest() throws Exception {
        getClient(getAuthToken(admin.getEmail(), password))
            .perform(get("/api/config/itemfilters/test"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void findOneTest() throws Exception {
        getClient(getAuthToken(admin.getEmail(), password))
            .perform(get("/api/config/itemfilters/always_true_filter"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is("always_true_filter")))
            .andExpect(jsonPath("$._links.self.href",
                containsString("/api/config/itemfilters/always_true_filter")));
    }

    @Test
    public void findAllUnauthorizedTest() throws Exception {
        getClient().perform(get("/api/config/itemfilters"))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void findAllForbiddenTest() throws Exception {
        getClient(getAuthToken(eperson.getEmail(), password))
            .perform(get("/api/config/itemfilters"))
            .andExpect(status().isForbidden());
    }

    @Test
    public void findAllPaginatedSortedTest() throws Exception {
        getClient(getAuthToken(admin.getEmail(), password))
            .perform(get("/api/config/itemfilters")
                .param("size", "30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(5)))
            .andExpect(jsonPath("$.page.totalPages", is(1)))
            .andExpect(jsonPath("$.page.size", is(30)))
            .andExpect(jsonPath("$._embedded.itemfilters", contains(
                hasJsonPath("$.id", is("always_true_filter")),
                hasJsonPath("$.id", is("demo_filter")),
                hasJsonPath("$.id", is("doi-filter")),
                hasJsonPath("$.id", is("in-outfit-collection_condition")),
                hasJsonPath("$.id", is("type_filter")))));
    }
}