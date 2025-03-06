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
    public void findOpenalexPublicationExternalSourceEntriesEmptyWithQueryTest() throws Exception {

        try (InputStream file = getClass().getResourceAsStream("openalex-empty.json")) {
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
    public void findOneOpenalexPublicationExternalSourceEntriesWithQueryTest() throws Exception {

        try (InputStream file = getClass().getResourceAsStream("openalex-publication-single.json")) {
            String jsonResponse = IOUtils.toString(file, Charset.defaultCharset());

            when(liveImportClient.executeHttpGetRequest(anyInt(), anyString(), anyMap()))
                .thenReturn(jsonResponse);

            getClient().perform(get("/api/integration/externalsources/openalexPublication/entries")
                                    .param("query", "protein"))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$._embedded.externalSourceEntries", Matchers.hasSize(1)))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].id").value("W1775749144"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].display")
                                      .value("PROTEIN MEASUREMENT WITH THE FOLIN PHENOL REAGENT"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].value")
                                      .value("PROTEIN MEASUREMENT WITH THE FOLIN PHENOL REAGENT"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].externalSource")
                                      .value("openalexPublication"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.contributor.author']",
                                           Matchers.hasSize(4)))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.contributor.author'][0].value")
                               .value("OliverH. Lowry"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.contributor.author'][1].value")
                               .value("NiraJ. Rosebrough"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.contributor.author'][2].value")
                               .value("A. Farr"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.contributor.author'][3].value")
                               .value("RoseJ. Randall"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.identifier.other']",
                                           Matchers.hasSize(1)))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.identifier.other'][0].value")
                               .value("https://pubmed.ncbi.nlm.nih.gov/14907713"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.identifier.uri']",
                                           Matchers.hasSize(7)))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.identifier.uri'][0]" +
                                               ".value")
                                      .value("https://doi.org/10.1016/s0021-9258(19)52451-6"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.identifier.uri'][1]" +
                                               ".value")
                                      .value("https://doi.org/10.1016/s0021-9258(19)52451-6/pdf"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.identifier.uri'][2]" +
                                               ".value")
                                      .value("https://doi.org/10.1016/s0021-9258(19)52451-6"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.identifier.uri'][3]" +
                                               ".value")
                                      .value("https://www.jbc.org/article/S0021-9258(19)52451-6/pdf"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.identifier.uri'][4]" +
                                               ".value")
                                      .value("https://www.jbc.org/article/S0021-9258(19)52451-6/pdf"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.identifier.uri'][5]" +
                                               ".value")
                                      .value("https://doi.org/10.1016/s0021-9258(19)52451-6"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.identifier.uri'][6]" +
                                               ".value")
                                      .value("https://doi.org/10.1016/s0021-9258(19)52451-6"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.date.issued'][0].value")
                                      .value("1951-11-01"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.identifier" +
                                               ".openalex'][0].value").value("W1775749144"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['oaire.citation.volume'][0]" +
                                               ".value")
                                      .value("193"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['oaire.citation.issue'][0].value")
                               .value("1"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['oaire.citation" +
                                               ".startPage'][0].value")
                                      .value("265"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['oaire.citation" +
                                               ".endPage'][0].value")
                                      .value("275"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['oaire.version'][0].value")
                               .value("http://purl.org/coar/version/c_970fb48d4fbd8a85"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['oaire.citation.title'][0].value")
                               .value("Journal of Biological Chemistry"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.identifier.issn']",
                                           Matchers.hasSize(3)))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.identifier.doi'][0].value")
                               .value("10.1016/s0021-9258(19)52451-6"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.language.iso'][0].value")
                                      .value("en"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.title'][0].value")
                                      .value("PROTEIN MEASUREMENT WITH THE FOLIN PHENOL REAGENT"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.subject']",
                                           Matchers.hasSize(13)))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.subject'][9].value")
                                      .value("Peer Review, Research standards"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.subject'][12].value")
                                      .value(
                                          "Life Sciences Biochemistry, Genetics and Molecular Biology Molecular " +
                                              "Biology Cancer and biochemical research"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.rights.license'][0].value")
                               .value("cc-by"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.type'][0].value")
                               .value("Article"))
                       .andExpect(jsonPath("$.page.totalElements").value(1))
                       .andExpect(jsonPath("$.page.totalPages").value(1))
                       .andExpect(jsonPath("$.page.size").value(20))
                       .andExpect(jsonPath("$.page.number").value(0));

            verify(liveImportClient, times(2)).executeHttpGetRequest(anyInt(), anyString(), anyMap());
        }
    }

    @Test
    public void findAllOpenalexPublicationExternalSourceEntriesWithQueryTest() throws Exception {

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
