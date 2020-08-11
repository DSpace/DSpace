/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.matcher.ExternalSourceEntryMatcher;
import org.dspace.app.rest.matcher.ExternalSourceMatcher;
import org.dspace.app.rest.matcher.PageMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.Test;

public class ExternalSourcesRestControllerIT extends AbstractControllerIntegrationTest {

    @Test
    public void findAllExternalSources() throws Exception {
        getClient().perform(get("/api/integration/externalsources"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$._embedded.externalsources", Matchers.hasItem(
                                ExternalSourceMatcher.matchExternalSource("mock", "mock", false)
                            )))
                            .andExpect(jsonPath("$.page.totalElements", Matchers.is(1)));
    }

    @Test
    public void findOneExternalSourcesExistingSources() throws Exception {
        getClient().perform(get("/api/integration/externalsources/mock"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.is(
                       ExternalSourceMatcher.matchExternalSource("mock", "mock", false)
                   )));
    }
    @Test
    public void findOneExternalSourcesNotExistingSources() throws Exception {
        getClient().perform(get("/api/integration/externalsources/mock2"))
                   .andExpect(status().isNotFound());
    }

    @Test
    public void findOneExternalSourceEntryValue() throws Exception {
        getClient().perform(get("/api/integration/externalsources/mock/entryValues/one"))
                   .andExpect(status().isOk())
                    .andExpect(jsonPath("$", Matchers.is(
                        ExternalSourceEntryMatcher.matchExternalSourceEntry("one", "one", "one", "mock")
                    )));
    }

    @Test
    public void findOneExternalSourceEntryValueInvalidEntryId() throws Exception {
        getClient().perform(get("/api/integration/externalsources/mock/entryValues/entryIdInvalid"))
                   .andExpect(status().isNotFound());
    }

    @Test
    public void findOneExternalSourceEntryValueInvalidSource() throws Exception {
        getClient().perform(get("/api/integration/externalsources/mocktwo/entryValues/one"))
                   .andExpect(status().isNotFound());
    }

    @Test
    public void findOneExternalSourceEntriesInvalidSource() throws Exception {
        getClient().perform(get("/api/integration/externalsources/mocktwo/entries")
                                .param("query", "test"))
                   .andExpect(status().isNotFound());
    }

    @Test
    public void findOneExternalSourceEntriesApplicableQuery() throws Exception {
        getClient().perform(get("/api/integration/externalsources/mock/entries")
                                .param("query", "one"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.externalSourceEntries", Matchers.containsInAnyOrder(
                       ExternalSourceEntryMatcher.matchExternalSourceEntry("one", "one", "one", "mock"),
                       ExternalSourceEntryMatcher.matchExternalSourceEntry("onetwo", "onetwo", "onetwo", "mock")
                   )))
                    .andExpect(jsonPath("$.page", PageMatcher.pageEntryWithTotalPagesAndElements(0, 20, 1, 2)));
    }

    @Test
    public void findOneExternalSourceEntriesApplicableQueryPagination() throws Exception {
        getClient().perform(get("/api/integration/externalsources/mock/entries")
                                .param("query", "one").param("size", "1"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.externalSourceEntries", Matchers.hasItem(
                       ExternalSourceEntryMatcher.matchExternalSourceEntry("onetwo", "onetwo", "onetwo", "mock")
                   )))
                   .andExpect(jsonPath("$.page", PageMatcher.pageEntryWithTotalPagesAndElements(0, 1, 2, 2)));

        getClient().perform(get("/api/integration/externalsources/mock/entries")
                                .param("query", "one").param("size", "1").param("page", "1"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.externalSourceEntries", Matchers.hasItem(
                       ExternalSourceEntryMatcher.matchExternalSourceEntry("one", "one", "one", "mock")
                   )))
                   .andExpect(jsonPath("$.page", PageMatcher.pageEntryWithTotalPagesAndElements(1, 1, 2, 2)));
    }

    @Test
    public void findOneExternalSourceEntriesNoReturnQuery() throws Exception {
        getClient().perform(get("/api/integration/externalsources/mock/entries")
                                .param("query", "randomqueryfornoresults"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded").doesNotExist());
    }

    @Test
    public void findOneExternalSourceEntriesNoQuery() throws Exception {
        getClient().perform(get("/api/integration/externalsources/mock/entries"))
                   .andExpect(status().isBadRequest());
    }
}
