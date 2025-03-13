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
import static org.junit.Assert.assertEquals;
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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.matcher.ExternalSourceEntryMatcher;
import org.dspace.app.rest.matcher.ExternalSourceMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.importer.external.liveimportclient.service.LiveImportClient;
import org.dspace.importer.external.openalex.service.OpenAlexImportMetadataSourceServiceImpl;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 **/
public class OpenAlexJournalExternalSourcesIT extends AbstractControllerIntegrationTest {


    @MockitoBean
    private LiveImportClient liveImportClient;

    @Autowired
    @Qualifier("openalexImportJournalService")
    private OpenAlexImportMetadataSourceServiceImpl openAlexImportMetadataSourceService;


    @Before
    public void setUp() {
        ReflectionTestUtils.setField(openAlexImportMetadataSourceService, "liveImportClient", liveImportClient);
    }

    @Test
    public void findOneOpenalexImportJournalServiceExternalSourceTest() throws Exception {
        getClient().perform(get("/api/integration/externalsources?size=25")).andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.externalsources", Matchers.hasItem(
                       ExternalSourceMatcher.matchExternalSource("openalexJournal",
                                                                 "openalexJournal", false))));
    }

    @Test
    public void findOpenalexJournalExternalSourceEntriesEmptyWithQueryTest() throws Exception {

        try (InputStream file = getClass().getResourceAsStream("openalex-empty.json")) {
            String jsonResponse = IOUtils.toString(file, Charset.defaultCharset());
            when(liveImportClient.executeHttpGetRequest(anyInt(), anyString(), anyMap()))
                .thenReturn(jsonResponse);


            getClient().perform(get("/api/integration/externalsources/openalexJournal/entries")
                                    .param("query", "empty"))
                       .andExpect(status().isOk()).andExpect(jsonPath("$.page.number", is(0)));
            verify(liveImportClient, times(2)).executeHttpGetRequest(anyInt(), anyString(), anyMap());
        }
    }

    @Test
    public void findOpenalexJournalExternalSourceEntriesTest() throws Exception {
        try (InputStream file = getClass().getResourceAsStream("openalex-journal-single.json")) {
            String jsonResponse = IOUtils.toString(file, Charset.defaultCharset());

            when(liveImportClient.executeHttpGetRequest(anyInt(), anyString(), anyMap()))
                .thenReturn(jsonResponse);

            getClient().perform(get("/api/integration/externalsources/openalexJournal/entries")
                                    .param("query", "Chem"))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].id").value("S41354064"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].display").value("ChemInform"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].externalSource").value("openalexJournal"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.identifier.openalex'][0].value")
                               .value("S41354064"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.title'][0].value")
                                      .value("ChemInform"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['creativework.publisher'][0].value")
                               .value("Wiley"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['creativeworkseries.issn'].length()")
                               .value(3))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.description'].length()")
                                      .value(8));

            // Verify executeHttpGetRequest is called twice with the expected parameters
            ArgumentCaptor<Map<String, Map<String, String>>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
            ArgumentCaptor<Integer> timeoutCaptor = ArgumentCaptor.forClass(Integer.class);
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);

            verify(liveImportClient, times(2)).executeHttpGetRequest(
                timeoutCaptor.capture(),
                urlCaptor.capture(),
                paramsCaptor.capture()
            );

            // Expected parameters
            Map<String, Map<String, String>> expectedParams = new HashMap<>();
            Map<String, String> uriParams = new HashMap<>();
            expectedParams.put("uriParameters", uriParams);
            uriParams.put("filter", "type:journal,default.search:Chem");

            assertEquals(expectedParams, paramsCaptor.getValue());
        }
    }

    @Test
    public void findAllOpenalexJournalExternalSourceEntriesWithQueryTest() throws Exception {

        try (InputStream file = getClass().getResourceAsStream("openalex-journal-multiple.json")) {
            String jsonResponse = IOUtils.toString(file, Charset.defaultCharset());

            when(liveImportClient.executeHttpGetRequest(anyInt(), anyString(), anyMap()))
                .thenReturn(jsonResponse);

            getClient().perform(get("/api/integration/externalsources/openalexJournal/entries")
                                    .param("query", "Chem"))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$._embedded.externalSourceEntries", hasSize(2)))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries",
                                           Matchers.containsInAnyOrder(
                                               ExternalSourceEntryMatcher.matchExternalSourceEntry(
                                                   "S41354064",
                                                   "ChemInform",
                                                   "ChemInform",
                                                   "openalexJournal"
                                               ),
                                               ExternalSourceEntryMatcher.matchExternalSourceEntry(
                                                   "S2764455111",
                                                   "Chem",
                                                   "Chem",
                                                   "openalexJournal"
                                               )
                                           )
                       ));

            verify(liveImportClient, times(2)).executeHttpGetRequest(anyInt(), anyString(), anyMap());
        }
    }

}
