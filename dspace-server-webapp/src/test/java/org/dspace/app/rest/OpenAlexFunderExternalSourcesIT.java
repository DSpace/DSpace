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
public class OpenAlexFunderExternalSourcesIT extends AbstractControllerIntegrationTest {


    @MockitoBean
    private LiveImportClient liveImportClient;

    @Autowired
    @Qualifier("openalexImportFunderService")
    private OpenAlexImportMetadataSourceServiceImpl openAlexImportMetadataSourceService;


    @Before
    public void setUp() {
        ReflectionTestUtils.setField(openAlexImportMetadataSourceService, "liveImportClient", liveImportClient);
    }

    @Test
    public void findOneOpenalexImportFunderExternalExternalSourceTest() throws Exception {
        getClient().perform(get("/api/integration/externalsources?size=25")).andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.externalsources", Matchers.hasItem(
                       ExternalSourceMatcher.matchExternalSource("openalexFunder",
                                                                 "openalexFunder", false))));
    }

    @Test
    public void findOpenalexFunderExternalSourceEntriesEmptyWithQueryTest() throws Exception {

        try (InputStream file = getClass().getResourceAsStream("openalex-empty.json")) {
            String jsonResponse = IOUtils.toString(file, Charset.defaultCharset());
            when(liveImportClient.executeHttpGetRequest(anyInt(), anyString(), anyMap()))
                .thenReturn(jsonResponse);

            getClient().perform(get("/api/integration/externalsources/openalexFunder/entries")
                                    .param("query", "empty"))
                       .andExpect(status().isOk()).andExpect(jsonPath("$.page.number", is(0)));
            verify(liveImportClient, times(2)).executeHttpGetRequest(anyInt(), anyString(), anyMap());
        }
    }

    @Test
    public void findOpenalexFunderExternalSourceEntriesTest() throws Exception {
        try (InputStream file = getClass().getResourceAsStream("openalex-funder-single.json")) {
            String jsonResponse = IOUtils.toString(file, Charset.defaultCharset());

            when(liveImportClient.executeHttpGetRequest(anyInt(), anyString(), anyMap()))
                .thenReturn(jsonResponse);

            getClient().perform(get("/api/integration/externalsources/openalexFunder/entries")
                                    .param("query", "National"))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].id").value("F4320321001"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].display")
                                      .value("National Natural Science Foundation of China"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].externalSource").value("openalexFunder"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.identifier.openalex'][0].value")
                               .value("F4320321001"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.title'][0].value")
                                      .value("National Natural Science Foundation of China"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['organization.legalName'][0].value")
                               .value("National Natural Science Foundation of China"))
                       .andExpect(jsonPath(
                           "$._embedded.externalSourceEntries[0].metadata['organization.address.addressCountry'][0]" +
                               ".value")
                                      .value("CN"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.description'][0].value").value(
                               "NNSF of China"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.description'][1].value").value(
                               "Guójiā Zìrán Kēxué Jījīn Wěiyuánhuì"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.description'][2].value").value(
                               "NSF of China"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.description'][3].value").value(
                               "The National Natural Science Foundation of China"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.description'][4].value").value(
                               "国家自然科学基金委员会"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.description'][5].value").value(
                               "National Nature Science Foundation of China"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.description'][6].value").value(
                               "NSFC"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.description'][7].value").value(
                               "National Science Foundation of China"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.description'][8].value").value(
                               "Natural Science Foundation of China"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.description'][9].value").value(
                               "NNSFC"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.description'][10].value").value(
                               "Chinese National Science Foundation"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.description'][11].value").value(
                               "NNSF"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.description'][12].value").value(
                               "Chinese government body"))
                       .andExpect(jsonPath("$._links.self.href")
                                      .value(
                                          "http://localhost/api/integration/externalsources/openalexFunder/entries" +
                                              "?page=0&size=20"))
                       .andExpect(jsonPath("$.page.size").value(20))
                       .andExpect(jsonPath("$.page.totalElements").value(1819))
                       .andExpect(jsonPath("$.page.totalPages").value(91))
                       .andExpect(jsonPath("$.page.number").value(0));

            verify(liveImportClient, times(2)).executeHttpGetRequest(anyInt(), anyString(), anyMap());
        }
    }


    @Test
    public void findAllOpenalexFunderExternalSourceEntriesWithQueryTest() throws Exception {

        try (InputStream file = getClass().getResourceAsStream("openalex-funder-multiple.json")) {
            String jsonResponse = IOUtils.toString(file, Charset.defaultCharset());

            when(liveImportClient.executeHttpGetRequest(anyInt(), anyString(), anyMap()))
                .thenReturn(jsonResponse);

            getClient().perform(get("/api/integration/externalsources/openalexFunder/entries")
                                    .param("query", "National"))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$._embedded.externalSourceEntries", hasSize(2)))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries",
                                           Matchers.containsInAnyOrder(
                                               ExternalSourceEntryMatcher.matchExternalSourceEntry(
                                                   "F4320321001",
                                                   "National Natural Science Foundation of China",
                                                   "National Natural Science Foundation of China",
                                                   "openalexFunder"
                                               ),
                                               ExternalSourceEntryMatcher.matchExternalSourceEntry(
                                                   "F4320306076",
                                                   "National Science Foundation",
                                                   "National Science Foundation",
                                                   "openalexFunder"
                                               )
                                           )
                       ));

            verify(liveImportClient, times(2)).executeHttpGetRequest(anyInt(), anyString(), anyMap());
        }
    }

}
