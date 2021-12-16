/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.matcher.ExternalSourceEntryMatcher;
import org.dspace.app.rest.matcher.ExternalSourceMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.Test;

public class OpenAIREFundingExternalSourcesIT extends AbstractControllerIntegrationTest {

    /**
     * Test openaire funding external source
     * 
     * @throws Exception
     */
    @Test
    public void findOneOpenAIREFundingExternalSourceTest() throws Exception {
        getClient().perform(get("/api/integration/externalsources")).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.externalsources", Matchers.hasItem(
                        ExternalSourceMatcher.matchExternalSource("openAIREFunding", "openAIREFunding", false))));
    }

    /**
     * Test openaire funding entries for a query returning no results
     * 
     * @throws Exception
     */
    @Test
    public void findOneOpenAIREFundingExternalSourceEntriesEmptyWithQueryTest() throws Exception {

        getClient().perform(get("/api/integration/externalsources/openAIREFunding/entries").param("query", "empty"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.number", is(0)));
    }

    /**
     * Test openaire funding entries with multiple keywords for a query returning no
     * results
     * 
     * @throws Exception
     */
    @Test
    public void findOneOpenAIREFundingExternalSourceEntriesWithQueryMultipleKeywordsTest() throws Exception {

        getClient()
                .perform(
                        get("/api/integration/externalsources/openAIREFunding/entries").param("query", "empty+results"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.number", is(0)));
    }

    /**
     * Test openaire funding entries for a query using ?query=mock
     * 
     * @throws Exception
     */
    @Test
    public void findOneOpenAIREFundingExternalSourceEntriesWithQueryTest() throws Exception {
        getClient().perform(get("/api/integration/externalsources/openAIREFunding/entries").param("query", "mushroom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.externalSourceEntries",
                        Matchers.hasItem(ExternalSourceEntryMatcher.matchExternalSourceEntry(
                                "aW5mbzpldS1yZXBvL2dyYW50QWdyZWVtZW50L05XTy8rLzIzMDAxNDc3MjgvTkw=",
                                "Master switches of initiation of mushroom formation",
                                "Master switches of initiation of mushroom formation", "openAIREFunding"))));

    }

    /**
     * Test openaire funding entry value
     * 
     * @throws Exception
     */
    @Test
    public void findOneOpenAIREFundingExternalSourceEntryValueTest() throws Exception {

        // "info:eu-repo/grantAgreement/mock/mock/mock/mock" base64 encoded
        String projectID = "aW5mbzpldS1yZXBvL2dyYW50QWdyZWVtZW50L0ZDVC81ODc2LVBQQ0RUSS8xMTAwNjIvUFQ=";
        String projectName = "Portuguese Wild Mushrooms: Chemical characterization and functional study"
                + " of antiproliferative and proapoptotic properties in cancer cell lines";

        getClient().perform(get("/api/integration/externalsources/openAIREFunding/entryValues/" + projectID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$",
                        Matchers.allOf(hasJsonPath("$.id", is(projectID)), hasJsonPath("$.display", is(projectName)),
                                hasJsonPath("$.value", is(projectName)),
                                hasJsonPath("$.externalSource", is("openAIREFunding")),
                                hasJsonPath("$.type", is("externalSourceEntry")))));

    }
}
