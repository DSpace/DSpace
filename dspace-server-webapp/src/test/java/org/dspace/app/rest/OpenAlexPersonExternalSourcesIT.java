/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.matcher.ExternalSourceEntryMatcher;
import org.dspace.app.rest.matcher.ExternalSourceMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.importer.external.liveimportclient.service.LiveImportClient;
import org.dspace.importer.external.openalex.service.OpenAlexImportMetadataSourceServiceImpl;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 **/
public class OpenAlexPersonExternalSourcesIT extends AbstractControllerIntegrationTest {


    @MockitoBean
    private LiveImportClient liveImportClient;

    @Autowired
    @Qualifier("openalexImportPersonService")
    private OpenAlexImportMetadataSourceServiceImpl openAlexImportMetadataSourceService;


    @Before
    public void setUp() {
        ReflectionTestUtils.setField(openAlexImportMetadataSourceService, "liveImportClient", liveImportClient);
    }

    @Test
    public void findOneOpenalexImportPersonServiceExternalSourceTest() throws Exception {
        getClient().perform(get("/api/integration/externalsources?size=25")).andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.externalsources", Matchers.hasItem(
                       ExternalSourceMatcher.matchExternalSource("openalexPerson",
                                                                 "openalexPerson", false))));
    }

    @Test
    public void findOpenalexPersonExternalSourceEntriesEmptyWithQueryTest() throws Exception {

        try (InputStream file = getClass().getResourceAsStream("openalex-empty.json")) {
            String jsonResponse = IOUtils.toString(file, Charset.defaultCharset());
            when(liveImportClient.executeHttpGetRequest(anyInt(), anyString(), anyMap()))
                .thenReturn(jsonResponse);


            getClient().perform(get("/api/integration/externalsources/openalexPerson/entries")
                                    .param("query", "empty"))
                       .andExpect(status().isOk()).andExpect(jsonPath("$.page.number", is(0)));
            verify(liveImportClient, times(2)).executeHttpGetRequest(anyInt(), anyString(), anyMap());
        }
    }

    @Test
    public void findOpenalexPersonExternalSourceEntriesTest() throws Exception {
        try (InputStream file = getClass().getResourceAsStream("openalex-person-single.json")) {
            String jsonResponse = IOUtils.toString(file, Charset.defaultCharset());
            when(liveImportClient.executeHttpGetRequest(anyInt(), anyString(), anyMap()))
                .thenReturn(jsonResponse);

            getClient().perform(get("/api/integration/externalsources/openalexPerson/entries")
                                    .param("query", "Claudio Cortese"))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$.page.number", is(0)))
                       .andExpect(jsonPath("$.page.totalElements", is(1)))
                       .andExpect(jsonPath("$.page.totalPages", is(1)))
                       .andExpect(jsonPath("$.page.size", is(20)))

                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].id", is("A5016721535")))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].display", is("Claudio Giovanni Cortese")))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].value", is("Claudio Giovanni Cortese")))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].externalSource", is("openalexPerson")))

                       // Verify metadata fields
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.identifier.openalex'][0].value",
                                    is("A5016721535")))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['person.givenName'][0].value",
                                           is("Claudio Giovanni")))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['person.familyName'][0].value",
                                    is("Cortese")))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['person.identifier.orcid'][0].value",
                                    is("https://orcid.org/0000-0002-9429-5000")))

                       // Verify affiliations
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['person.affiliation.name']",
                                           hasSize(10)))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['person.affiliation.name'][0].value",
                                    is("University of Rome Tor Vergata")))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['person.affiliation.name'][1].value",
                                    is("Ospedali Riuniti di Ancona")))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['person.affiliation.name'][2].value",
                                    is("University of Naples Federico II")));

            verify(liveImportClient, times(2)).executeHttpGetRequest(anyInt(), anyString(), anyMap());
        }
    }


    @Test
    public void findAllOpenalexPersonExternalSourceEntriesWithQueryTest() throws Exception {

        try (InputStream file = getClass().getResourceAsStream("openalex-person-multiple.json")) {
            String jsonResponse = IOUtils.toString(file, Charset.defaultCharset());

            when(liveImportClient.executeHttpGetRequest(anyInt(), anyString(), anyMap()))
                .thenReturn(jsonResponse);

            getClient().perform(get("/api/integration/externalsources/openalexPerson/entries")
                                    .param("query", "covid"))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$._embedded.externalSourceEntries", hasSize(2)))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries",
                                           Matchers.containsInAnyOrder(
                                               ExternalSourceEntryMatcher.matchExternalSourceEntry(
                                                   "A5016721535",
                                                   "Claudio Cortese",
                                                   "Claudio Cortese",
                                                   "openalexPerson"
                                               ),
                                               ExternalSourceEntryMatcher.matchExternalSourceEntry(
                                                   "A5008845767",
                                                   "Claudio Giovanni Cortese",
                                                   "Claudio Giovanni Cortese",
                                                   "openalexPerson"
                                               )
                                           )
                       ));

            verify(liveImportClient, times(2)).executeHttpGetRequest(anyInt(), anyString(), anyMap());
        }
    }

}
