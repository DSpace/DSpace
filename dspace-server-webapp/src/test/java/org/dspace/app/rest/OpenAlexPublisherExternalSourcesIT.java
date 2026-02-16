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
public class OpenAlexPublisherExternalSourcesIT extends AbstractControllerIntegrationTest {


    @MockitoBean
    private LiveImportClient liveImportClient;

    @Autowired
    @Qualifier("openalexImportPublisherService")
    private OpenAlexImportMetadataSourceServiceImpl openAlexImportMetadataSourceService;


    @Before
    public void setUp() {
        ReflectionTestUtils.setField(openAlexImportMetadataSourceService, "liveImportClient", liveImportClient);
    }

    @Test
    public void findOneOpenalexImportPublisherExternalExternalSourceTest() throws Exception {
        getClient().perform(get("/api/integration/externalsources?size=25")).andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.externalsources", Matchers.hasItem(
                       ExternalSourceMatcher.matchExternalSource("openalexPublisher",
                                                                 "openalexPublisher", false))));
    }

    @Test
    public void findOpenalexPublisherExternalSourceEntriesEmptyWithQueryTest() throws Exception {

        try (InputStream file = getClass().getResourceAsStream("openalex-empty.json")) {
            String jsonResponse = IOUtils.toString(file, Charset.defaultCharset());
            when(liveImportClient.executeHttpGetRequest(anyInt(), anyString(), anyMap()))
                .thenReturn(jsonResponse);

            getClient().perform(get("/api/integration/externalsources/openalexPublisher/entries")
                                    .param("query", "empty"))
                       .andExpect(status().isOk()).andExpect(jsonPath("$.page.number", is(0)));
            verify(liveImportClient, times(2)).executeHttpGetRequest(anyInt(), anyString(), anyMap());
        }
    }

    @Test
    public void findOpenalexPublisherExternalSourceEntriesTest() throws Exception {
        try (InputStream file = getClass().getResourceAsStream("openalex-publisher-single.json")) {
            String jsonResponse = IOUtils.toString(file, Charset.defaultCharset());

            when(liveImportClient.executeHttpGetRequest(anyInt(), anyString(), anyMap()))
                .thenReturn(jsonResponse);

            getClient().perform(get("/api/integration/externalsources/openalexPublisher/entries")
                                    .param("query", "Elsevier"))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].id").value("P4310320990"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].display").value("Elsevier BV"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].externalSource").value("openalexPublisher"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.identifier.openalex'][0].value")
                               .value("P4310320990"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.title'][0].value")
                                      .value("Elsevier BV"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['organization.legalName'][0].value")
                               .value("Elsevier BV"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.description'][0].value").value(
                               "Elsevier"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.description'][1].value").value(
                               "elsevier.com"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.description'][2].value").value(
                               "Elsevier Science"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.description'][3].value").value(
                               "Uitg. Elsevier"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.description'][4].value").value(
                               "السفیر"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.description'][5].value").value(
                               "السویر"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.description'][6].value").value(
                               "انتشارات الزویر"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.description'][7].value").value(
                               "لودویک السفیر"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.description'][8].value").value(
                               "爱思唯尔"));

            verify(liveImportClient, times(2)).executeHttpGetRequest(anyInt(), anyString(), anyMap());
        }
    }


    @Test
    public void findAllOpenalexPublisherExternalSourceEntriesWithQueryTest() throws Exception {

        try (InputStream file = getClass().getResourceAsStream("openalex-publisher-multiple.json")) {
            String jsonResponse = IOUtils.toString(file, Charset.defaultCharset());

            when(liveImportClient.executeHttpGetRequest(anyInt(), anyString(), anyMap()))
                .thenReturn(jsonResponse);

            getClient().perform(get("/api/integration/externalsources/openalexPublisher/entries")
                                    .param("query", "Elsevier"))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$._embedded.externalSourceEntries", hasSize(2)))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries",
                                           Matchers.containsInAnyOrder(
                                               ExternalSourceEntryMatcher.matchExternalSourceEntry(
                                                   "P4310320990",
                                                   "Elsevier BV",
                                                   "Elsevier BV",
                                                   "openalexPublisher"
                                               ),
                                               ExternalSourceEntryMatcher.matchExternalSourceEntry(
                                                   "P4310320175",
                                                   "Masson",
                                                   "Masson",
                                                   "openalexPublisher"
                                               )
                                           )
                       ));

            verify(liveImportClient, times(2)).executeHttpGetRequest(anyInt(), anyString(), anyMap());
        }
    }

}
