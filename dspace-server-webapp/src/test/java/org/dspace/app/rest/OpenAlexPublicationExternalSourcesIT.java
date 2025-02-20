/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

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

public class OpenAlexPublicationExternalSourcesIT extends AbstractControllerIntegrationTest {


    @MockitoBean
    private LiveImportClient liveImportClient;

    @Autowired
    @Qualifier("openalexImportPublicationService")
    private OpenAlexImportMetadataSourceServiceImpl openAlexImportMetadataSourceService;


    @Before
    public void setUp() {
        ReflectionTestUtils.setField(openAlexImportMetadataSourceService, "liveImportClient", liveImportClient);
    }

    @Test
    public void findOneOpenalexImportPublicationExternalSourceTest() throws Exception {
        getClient().perform(get("/api/integration/externalsources?size=25")).andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.externalsources", Matchers.hasItem(
                       ExternalSourceMatcher.matchExternalSource("openalexPublication",
                                                                 "openalexPublication", false))));
    }

    @Test
    public void findOneOpenalexImportPublicationByAuthorIdExternalSourceTest() throws Exception {
        getClient().perform(get("/api/integration/externalsources?size=25")).andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.externalsources", Matchers.hasItem(
                       ExternalSourceMatcher.matchExternalSource("openalexPublicationByAuthorId",
                                                                 "openalexPublicationByAuthorId", false))));
    }

    @Test
    public void findOneOpenalexImportPersonServiceExternalSourceTest() throws Exception {
        getClient().perform(get("/api/integration/externalsources?size=25")).andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.externalsources", Matchers.hasItem(
                       ExternalSourceMatcher.matchExternalSource("openalexPerson",
                                                                 "openalexPerson", false))));
    }


    @Test
    public void findOpenalexPublicationExternalSourceEntriesEmptyWithQueryTest() throws Exception {

        try (InputStream file = getClass().getResourceAsStream("openalex-publication-empty.json")) {
            String jsonResponse = IOUtils.toString(file, Charset.defaultCharset());
            when(liveImportClient.executeHttpGetRequest(anyInt(), anyString(), anyMap()))
                .thenReturn(jsonResponse);


            getClient().perform(get("/api/integration/externalsources/openalexPublication/entries")
                                    .param("query", "empty"))
                       .andExpect(status().isOk()).andExpect(jsonPath("$.page.number", is(0)));
            verify(liveImportClient, times(2)).executeHttpGetRequest(anyInt(), anyString(), anyMap());
        }
    }


    @Test
    public void findOneOpenaireFundingExternalSourceEntriesWithQueryTest() throws Exception {

        try (InputStream file = getClass().getResourceAsStream("openalex-publication-multiple.json")) {
            String jsonResponse = IOUtils.toString(file, Charset.defaultCharset());

            when(liveImportClient.executeHttpGetRequest(anyInt(), anyString(), anyMap()))
                .thenReturn(jsonResponse);

            getClient().perform(get("/api/integration/externalsources/openalexPublication/entries")
                                    .param("query", "covid"))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$._embedded.externalSourceEntries", Matchers.hasSize(2)))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries",
                                           Matchers.containsInAnyOrder(
                                               ExternalSourceEntryMatcher.matchExternalSourceEntry(
                                                   "W3111255098",
                                                   "Safety and Efficacy of the BNT162b2 mRNA Covid-19 Vaccine",
                                                   "Safety and Efficacy of the BNT162b2 mRNA Covid-19 Vaccine",
                                                   "openalexPublication"
                                               ),
                                               ExternalSourceEntryMatcher.matchExternalSourceEntry(
                                                   "W3008028633",
                                                   "Characteristics of and Important Lessons From the Coronavirus " +
                                                       "Disease 2019 (COVID-19) Outbreak in China",
                                                   "Characteristics of and Important Lessons From the Coronavirus " +
                                                       "Disease 2019 (COVID-19) Outbreak in China",
                                                   "openalexPublication"
                                               )
                                           )
                       ));

            verify(liveImportClient, times(2)).executeHttpGetRequest(anyInt(), anyString(), anyMap());
        }
    }


}
