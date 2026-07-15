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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.external.provider.impl.OrcidV3AuthorDataProvider;
import org.dspace.orcid.client.OrcidClient;
import org.dspace.orcid.client.OrcidConfiguration;
import org.hamcrest.Matchers;
import org.junit.Assume;
import org.junit.Test;
import org.mockito.Mockito;
import org.orcid.jaxb.model.v3.release.record.Record;
import org.orcid.jaxb.model.v3.release.search.expanded.ExpandedResult;
import org.orcid.jaxb.model.v3.release.search.expanded.ExpandedSearch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

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
    private OrcidV3AuthorDataProvider orcidV3AuthorDataProvider;

    public void onlyRunIfConfigExists() {
        OrcidConfiguration config = (OrcidConfiguration) ReflectionTestUtils
            .getField(orcidV3AuthorDataProvider, "orcidConfiguration");
        if (config == null || !config.isApiConfigured()) {
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
                   .andExpect(jsonPath("$.metadata['dc.identifier.uri'][0].value",
                       is("https://sandbox.orcid.org/" + entry)))
                   .andExpect(jsonPath("$.metadata['person.familyName'][0].value", is("Bollini")))
                   .andExpect(jsonPath("$.metadata['person.givenName'][0].value", is("Andrea")))
                   .andExpect(jsonPath("$.metadata['person.identifier.orcid'][0].value", is(entry)));
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

    /**
     * This test uses mock data in the orcid-person-record.xml file to simulate the
     * response from ORCID and verify that it is properly consumed and exposed by
     * the REST API.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void findOneExternalSourcesMockitoTest() throws Exception {
        OrcidClient mockOrcidClient = Mockito.mock(OrcidClient.class);
        OrcidClient realClient = (OrcidClient) ReflectionTestUtils
            .getField(orcidV3AuthorDataProvider, "orcidClient");
        OrcidConfiguration realConfig = (OrcidConfiguration) ReflectionTestUtils
            .getField(orcidV3AuthorDataProvider, "orcidConfiguration");
        OrcidConfiguration mockConfig = Mockito.mock(OrcidConfiguration.class);
        when(mockConfig.isApiConfigured()).thenReturn(false);

        ReflectionTestUtils.setField(orcidV3AuthorDataProvider, "orcidClient", mockOrcidClient);
        ReflectionTestUtils.setField(orcidV3AuthorDataProvider, "orcidConfiguration", mockConfig);

        String entry = "0000-0002-9029-1854";
        Record mockRecord = loadRecord("orcid-person-record.xml");
        when(mockOrcidClient.getRecord(eq(entry))).thenReturn(mockRecord);

        try {
            getClient().perform(get("/api/integration/externalsources/orcid/entryValues/" + entry))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$", Matchers.allOf(
                               hasJsonPath("$.id", is(entry)),
                               hasJsonPath("$.display", is("Bollini, Andrea")),
                               hasJsonPath("$.value", is("Bollini, Andrea")),
                               hasJsonPath("$.externalSource", is("orcid")),
                               hasJsonPath("$.type", is("externalSourceEntry"))
                               )));
        } finally {
            ReflectionTestUtils.setField(orcidV3AuthorDataProvider, "orcidClient", realClient);
            ReflectionTestUtils.setField(orcidV3AuthorDataProvider, "orcidConfiguration", realConfig);
        }
    }

    /**
     * This test uses mock data in the orcid-person-record.xml file to simulate the
     * response from ORCID and verify that it is properly consumed and exposed by
     * the REST API. The search uses the expanded-search endpoint, and for each result
     * the full record is fetched.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void findOneExternalSourceEntriesApplicableQueryMockitoTest() throws Exception {
        OrcidClient mockOrcidClient = Mockito.mock(OrcidClient.class);
        OrcidClient realClient = (OrcidClient) ReflectionTestUtils
            .getField(orcidV3AuthorDataProvider, "orcidClient");
        OrcidConfiguration realConfig = (OrcidConfiguration) ReflectionTestUtils
            .getField(orcidV3AuthorDataProvider, "orcidConfiguration");
        OrcidConfiguration mockConfig = Mockito.mock(OrcidConfiguration.class);
        when(mockConfig.isApiConfigured()).thenReturn(false);

        ReflectionTestUtils.setField(orcidV3AuthorDataProvider, "orcidClient", mockOrcidClient);
        ReflectionTestUtils.setField(orcidV3AuthorDataProvider, "orcidConfiguration", mockConfig);
        try {
            ExpandedSearch searchResult = buildSearchResult("0000-0002-9029-1854");
            when(mockOrcidClient.expandedSearch(anyString(), anyInt(), anyInt())).thenReturn(searchResult);

            Record mockRecord = loadRecord("orcid-person-record.xml");
            when(mockOrcidClient.getRecord(eq("0000-0002-9029-1854"))).thenReturn(mockRecord);

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
            ReflectionTestUtils.setField(orcidV3AuthorDataProvider, "orcidClient", realClient);
            ReflectionTestUtils.setField(orcidV3AuthorDataProvider, "orcidConfiguration", realConfig);
        }
    }

    /**
     * This test uses mock data in the orcid-person-record.xml file to simulate the
     * response from ORCID and verify that it is properly consumed and exposed by
     * the REST API. The search uses the expanded-search endpoint, and for each result
     * the full record is fetched.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void findOneExternalSourceEntriesApplicableQueryFamilyNameAndGivenNamesMockitoTest() throws Exception {
        OrcidClient mockOrcidClient = Mockito.mock(OrcidClient.class);
        OrcidClient realClient = (OrcidClient) ReflectionTestUtils
            .getField(orcidV3AuthorDataProvider, "orcidClient");
        OrcidConfiguration realConfig = (OrcidConfiguration) ReflectionTestUtils
            .getField(orcidV3AuthorDataProvider, "orcidConfiguration");
        OrcidConfiguration mockConfig = Mockito.mock(OrcidConfiguration.class);
        when(mockConfig.isApiConfigured()).thenReturn(false);

        ReflectionTestUtils.setField(orcidV3AuthorDataProvider, "orcidClient", mockOrcidClient);
        ReflectionTestUtils.setField(orcidV3AuthorDataProvider, "orcidConfiguration", mockConfig);
        try {
            ExpandedSearch searchResult = buildSearchResult("0000-0002-9029-1854");
            when(mockOrcidClient.expandedSearch(anyString(), anyInt(), anyInt())).thenReturn(searchResult);

            Record mockRecord = loadRecord("orcid-person-record.xml");
            when(mockOrcidClient.getRecord(eq("0000-0002-9029-1854"))).thenReturn(mockRecord);

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
            ReflectionTestUtils.setField(orcidV3AuthorDataProvider, "orcidClient", realClient);
            ReflectionTestUtils.setField(orcidV3AuthorDataProvider, "orcidConfiguration", realConfig);
        }
    }

    /**
     * Load an ORCID Record from an XML resource on the classpath.
     */
    private Record loadRecord(String resourceName) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(resourceName)) {
            JAXBContext jaxbContext = JAXBContext.newInstance(Record.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return (Record) unmarshaller.unmarshal(is);
        }
    }

    /**
     * Build an ExpandedSearch result containing a single ORCID ID.
     */
    private ExpandedSearch buildSearchResult(String orcidId) {
        ExpandedSearch searchResult = new ExpandedSearch();
        searchResult.setNumFound(1L);
        ExpandedResult result = new ExpandedResult();
        result.setOrcidId(orcidId);
        searchResult.getResults().add(result);
        return searchResult;
    }
}
