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

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This test suite includes end to end test to verify the integration with ORCID as an External Source.
 * The test run only if the orcid.clientid property is configured.
 * 
 * @author Mykhaylo Boychuk (4Science.it)
 *
 */
public class OrcidExternalSourcesIT extends AbstractControllerIntegrationTest {

    @Autowired
    ConfigurationService configurationService;

    @Before
    public void onlyRunIfConfigExists() {
        if (StringUtils.isBlank(configurationService.getProperty("orcid.clientid"))) {
            Assume.assumeNoException(new IllegalStateException("Missing ORCID credentials"));
        }
    }

    @Test
    public void findOneExternalSourcesExistingSources() throws Exception {
        getClient().perform(get("/api/integration/externalsources/orcid"))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.allOf(
                           hasJsonPath("$.id", is("orcid")),
                           hasJsonPath("$.name", is("orcid")),
                           hasJsonPath("$.hierarchical", is(false)),
                           hasJsonPath("$.type", is("externalsource"))
                   )));
    }

    @Test
    public void findOneExternalSourcesExistingSourcesWithentryValueTest() throws Exception {
        String entry = "0000-0002-9029-1854";
        getClient().perform(get("/api/integration/externalsources/orcid/entryValues/" + entry))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.allOf(
                           hasJsonPath("$.id", is(entry)),
                           hasJsonPath("$.display", is("Bollini, Andrea")),
                           hasJsonPath("$.value", is("Bollini, Andrea")),
                           hasJsonPath("$.externalSource", is("orcid")),
                           hasJsonPath("$.type", is("externalSourceEntry"))
                   )))
                   .andExpect(jsonPath("$.metadata['dc.identifier.uri'][0].value",is("https://orcid.org/" + entry)))
                   .andExpect(jsonPath("$.metadata['person.familyName'][0].value",is("Bollini")))
                   .andExpect(jsonPath("$.metadata['person.givenName'][0].value",is("Andrea")))
                   .andExpect(jsonPath("$.metadata['person.identifier.orcid'][0].value",is(entry)));
    }

    @Test
    public void findOneExternalSourceEntriesApplicableQuery() throws Exception {
        String q = "orcid:0000-0002-9029-1854";
        getClient().perform(get("/api/integration/externalsources/orcid/entries")
                   .param("query", q))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.externalSourceEntries[0]", Matchers.allOf(
                           hasJsonPath("$.id", is("0000-0002-9029-1854")),
                           hasJsonPath("$.display", is("Bollini, Andrea")),
                           hasJsonPath("$.value", is("Bollini, Andrea")),
                           hasJsonPath("$.externalSource", is("orcid")),
                           hasJsonPath("$.type", is("externalSourceEntry"))
                   )))
                  .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.identifier.uri'][0].value",
                                   is("https://orcid.org/0000-0002-9029-1854")))
                  .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['person.familyName'][0].value",
                                   is("Bollini")))
                  .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['person.givenName'][0].value",
                                   is("Andrea")))
               .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['person.identifier.orcid'][0].value",
                                   is("0000-0002-9029-1854")));
    }

    @Test
    public void findOneExternalSourceEntriesApplicableQueryFamilyNameAndGivenNamesTest() throws Exception {
        String q = "family-name:bollini AND given-names:andrea";
        getClient().perform(get("/api/integration/externalsources/orcid/entries")
                   .param("query", q))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.externalSourceEntries[0]", Matchers.allOf(
                           hasJsonPath("$.id", is("0000-0002-9029-1854")),
                           hasJsonPath("$.display", is("Bollini, Andrea")),
                           hasJsonPath("$.value", is("Bollini, Andrea")),
                           hasJsonPath("$.externalSource", is("orcid")),
                           hasJsonPath("$.type", is("externalSourceEntry"))
                   )))
                  .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.identifier.uri'][0].value",
                                   is("https://orcid.org/0000-0002-9029-1854")))
                  .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['person.familyName'][0].value",
                                   is("Bollini")))
                  .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['person.givenName'][0].value",
                                   is("Andrea")))
               .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['person.identifier.orcid'][0].value",
                                   is("0000-0002-9029-1854")));
    }
}
