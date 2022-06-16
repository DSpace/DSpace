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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.external.OrcidRestConnector;
import org.dspace.external.provider.impl.OrcidV3AuthorDataProvider;
import org.dspace.services.ConfigurationService;
import org.hamcrest.Matchers;
import org.junit.Assume;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This test suite includes static test with mock data and end to end test to
 * verify the integration with ORCID as an External Source. The end to end test
 * run only if the orcid.application-client-id property is configured but of course also
 * orcid.application-client-secret is needed to successful run the tests. This can be enabled
 * setting the orcid credentials via env variables, see the comments in the
 * override section of the config-definition.xml
 * 
 * @author Mykhaylo Boychuk (4Science.it)
 *
 */
public class OrcidExternalSourcesIT extends AbstractControllerIntegrationTest {

    @Autowired
    ConfigurationService configurationService;

    @Autowired
    private OrcidV3AuthorDataProvider orcidV3AuthorDataProvider;

    public void onlyRunIfConfigExists() {
        if (StringUtils.isBlank(configurationService.getProperty("orcid.application-client-id"))) {
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
        // this test will query the real ORCID API if configured in the CI otherwise will be skipped
        onlyRunIfConfigExists();
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
                   .andExpect(jsonPath("$.metadata['dc.identifier.uri'][0].value",is("https://sandbox.orcid.org/" + entry)))
                   .andExpect(jsonPath("$.metadata['person.familyName'][0].value",is("Bollini")))
                   .andExpect(jsonPath("$.metadata['person.givenName'][0].value",is("Andrea")))
                   .andExpect(jsonPath("$.metadata['person.identifier.orcid'][0].value",is(entry)));
    }

    @Test
    public void findOneExternalSourceEntriesApplicableQueryTest() throws Exception {
        // this test will query the real ORCID API if configured in the CI otherwise will be skipped
        onlyRunIfConfigExists();
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
                                   is("https://sandbox.orcid.org/0000-0002-9029-1854")))
                  .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['person.familyName'][0].value",
                                   is("Bollini")))
                  .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['person.givenName'][0].value",
                                   is("Andrea")))
               .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['person.identifier.orcid'][0].value",
                                   is("0000-0002-9029-1854")));
    }

    @Test
    public void findOneExternalSourceEntriesApplicableQueryFamilyNameAndGivenNamesTest() throws Exception {
        // this test will query the real ORCID API if configured in the CI otherwise will be skipped
        onlyRunIfConfigExists();
        String q = "family-name:bollini AND given-names:andrea";
        getClient().perform(get("/api/integration/externalsources/orcid/entries")
                   .param("query", q))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.externalSourceEntries", Matchers.hasItem(
                           Matchers.allOf(
                               hasJsonPath("$.id", is("0000-0002-9029-1854")),
                               hasJsonPath("$.display", is("Bollini, Andrea")),
                               hasJsonPath("$.value", is("Bollini, Andrea")),
                               hasJsonPath("$.externalSource", is("orcid")),
                               hasJsonPath("$.type", is("externalSourceEntry")))
                   )))
                  .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.identifier.uri'][0].value",
                                   is("https://sandbox.orcid.org/0000-0002-9029-1854")))
                  .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['person.familyName'][0].value",
                                   is("Bollini")))
                  .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['person.givenName'][0].value",
                                   is("Andrea")))
               .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['person.identifier.orcid'][0].value",
                                   is("0000-0002-9029-1854")));
    }

    @Test
    /**
     * This test uses mock data in the orcid-person-record.xml file to simulate the
     * response from ORCID and verify that it is properly consumed and exposed by
     * the REST API
     *
     * @throws Exception
     */
    public void findOneExternalSourcesMockitoTest() throws Exception {
        OrcidRestConnector orcidConnector = Mockito.mock(OrcidRestConnector.class);
        OrcidRestConnector realConnector = orcidV3AuthorDataProvider.getOrcidRestConnector();
        orcidV3AuthorDataProvider.setOrcidRestConnector(orcidConnector);
        when(orcidConnector.get(ArgumentMatchers.endsWith("/person"), ArgumentMatchers.any()))
                .thenAnswer(new Answer<InputStream>() {
                    public InputStream answer(InvocationOnMock invocation) {
                        return getClass().getResourceAsStream("orcid-person-record.xml");
                    }
                });

        String entry = "0000-0002-9029-1854";
        getClient().perform(get("/api/integration/externalsources/orcid/entryValues/" + entry))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$", Matchers.allOf(
                           hasJsonPath("$.id", is(entry)),
                           hasJsonPath("$.display", is("Bollini, Andrea")),
                           hasJsonPath("$.value", is("Bollini, Andrea")),
                           hasJsonPath("$.externalSource", is("orcid")),
                           hasJsonPath("$.type", is("externalSourceEntry"))
                           )));

        orcidV3AuthorDataProvider.setOrcidRestConnector(realConnector);
    }

    @Test
    /**
     * This test uses mock data in the orcid-search.xml and orcid-person-record.xml
     * file to simulate the response from ORCID and verify that it is properly
     * consumed and exposed by the REST API. The orcid-search.xml file indeed
     * contains the ORCID matching the user query, for each of them our integration
     * need to grab details making a second call to the ORCID profile (this is due
     * to the ORCID API structure and cannot be avoid)
     *
     * @throws Exception
     */
    public void findOneExternalSourceEntriesApplicableQueryMockitoTest() throws Exception {
        OrcidRestConnector orcidConnector = Mockito.mock(OrcidRestConnector.class);
        OrcidRestConnector realConnector = orcidV3AuthorDataProvider.getOrcidRestConnector();
        orcidV3AuthorDataProvider.setOrcidRestConnector(orcidConnector);
        try {
            when(orcidConnector.get(ArgumentMatchers.startsWith("search?"), ArgumentMatchers.any()))
                    .thenAnswer(new Answer<InputStream>() {
                        public InputStream answer(InvocationOnMock invocation) {
                            return getClass().getResourceAsStream("orcid-search.xml");
                        }
                    });
            when(orcidConnector.get(ArgumentMatchers.endsWith("/person"), ArgumentMatchers.any()))
                    .thenAnswer(new Answer<InputStream>() {
                        public InputStream answer(InvocationOnMock invocation) {
                            return getClass().getResourceAsStream("orcid-person-record.xml");
                        }
                    });
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
                                       is("https://sandbox.orcid.org/0000-0002-9029-1854")))
                      .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['person.familyName'][0].value",
                                       is("Bollini")))
                      .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['person.givenName'][0].value",
                                       is("Andrea")))
                      .andExpect(jsonPath(
                            "$._embedded.externalSourceEntries[0].metadata['person.identifier.orcid'][0].value",
                            is("0000-0002-9029-1854")));
        } finally {
            orcidV3AuthorDataProvider.setOrcidRestConnector(realConnector);
        }
    }

    @Test
    /**
     * This test uses mock data in the orcid-search.xml and orcid-person-record.xml
     * file to simulate the response from ORCID and verify that it is properly
     * consumed and exposed by the REST API. The orcid-search.xml file indeed
     * contains the ORCID matching the user query, for each of them our integration
     * need to grab details making a second call to the ORCID profile (this is due
     * to the ORCID API structure and cannot be avoid)
     *
     * @throws Exception
     */
    public void findOneExternalSourceEntriesApplicableQueryFamilyNameAndGivenNamesMockitoTest() throws Exception {
        OrcidRestConnector orcidConnector = Mockito.mock(OrcidRestConnector.class);
        OrcidRestConnector realConnector = orcidV3AuthorDataProvider.getOrcidRestConnector();
        orcidV3AuthorDataProvider.setOrcidRestConnector(orcidConnector);
        try {
            when(orcidConnector.get(ArgumentMatchers.startsWith("search?"), ArgumentMatchers.any()))
                    .thenAnswer(new Answer<InputStream>() {
                        public InputStream answer(InvocationOnMock invocation) {
                            return getClass().getResourceAsStream("orcid-search.xml");
                        }
                    });
            when(orcidConnector.get(ArgumentMatchers.endsWith("/person"), ArgumentMatchers.any()))
                    .thenAnswer(new Answer<InputStream>() {
                        public InputStream answer(InvocationOnMock invocation) {
                            return getClass().getResourceAsStream("orcid-person-record.xml");
                        }
                    });
            String q = "family-name:bollini AND given-names:andrea";
            getClient().perform(get("/api/integration/externalsources/orcid/entries")
                       .param("query", q))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$._embedded.externalSourceEntries", Matchers.hasItem(
                               Matchers.allOf(
                                   hasJsonPath("$.id", is("0000-0002-9029-1854")),
                                   hasJsonPath("$.display", is("Bollini, Andrea")),
                                   hasJsonPath("$.value", is("Bollini, Andrea")),
                                   hasJsonPath("$.externalSource", is("orcid")),
                                   hasJsonPath("$.type", is("externalSourceEntry")))
                       )))
                      .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.identifier.uri'][0].value",
                                       is("https://sandbox.orcid.org/0000-0002-9029-1854")))
                      .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['person.familyName'][0].value",
                                       is("Bollini")))
                      .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['person.givenName'][0].value",
                                       is("Andrea")))
                      .andExpect(jsonPath(
                                "$._embedded.externalSourceEntries[0].metadata['person.identifier.orcid'][0].value",
                                           is("0000-0002-9029-1854")));
        } finally {
            orcidV3AuthorDataProvider.setOrcidRestConnector(realConnector);
        }
    }

}
