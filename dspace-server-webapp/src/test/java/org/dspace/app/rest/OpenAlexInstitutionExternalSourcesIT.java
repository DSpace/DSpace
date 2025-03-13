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
public class OpenAlexInstitutionExternalSourcesIT extends AbstractControllerIntegrationTest {


    @MockitoBean
    private LiveImportClient liveImportClient;

    @Autowired
    @Qualifier("openalexImportInstitutionService")
    private OpenAlexImportMetadataSourceServiceImpl openAlexImportMetadataSourceService;


    @Before
    public void setUp() {
        ReflectionTestUtils.setField(openAlexImportMetadataSourceService, "liveImportClient", liveImportClient);
    }

    @Test
    public void findOneOpenalexImportInstitutionExternalExternalSourceTest() throws Exception {
        getClient().perform(get("/api/integration/externalsources?size=25")).andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.externalsources", Matchers.hasItem(
                       ExternalSourceMatcher.matchExternalSource("openalexInstitution",
                                                                 "openalexInstitution", false))));
    }

    @Test
    public void findOpenalexInstitutionExternalSourceEntriesEmptyWithQueryTest() throws Exception {

        try (InputStream file = getClass().getResourceAsStream("openalex-empty.json")) {
            String jsonResponse = IOUtils.toString(file, Charset.defaultCharset());
            when(liveImportClient.executeHttpGetRequest(anyInt(), anyString(), anyMap()))
                .thenReturn(jsonResponse);

            getClient().perform(get("/api/integration/externalsources/openalexInstitution/entries")
                                    .param("query", "empty"))
                       .andExpect(status().isOk()).andExpect(jsonPath("$.page.number", is(0)));
            verify(liveImportClient, times(2)).executeHttpGetRequest(anyInt(), anyString(), anyMap());
        }
    }

    @Test
    public void findOpenalexInstitutionExternalSourceEntriesTest() throws Exception {
        try (InputStream file = getClass().getResourceAsStream("openalex-institution-single.json")) {
            String jsonResponse = IOUtils.toString(file, Charset.defaultCharset());

            when(liveImportClient.executeHttpGetRequest(anyInt(), anyString(), anyMap()))
                .thenReturn(jsonResponse);

            getClient().perform(get("/api/integration/externalsources/openalexInstitution/entries")
                                    .param("query", "Centre"))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].id").value("I1294671590"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].display")
                                      .value("Centre National de la Recherche Scientifique"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].externalSource")
                                      .value("openalexInstitution"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.identifier.openalex'][0].value")
                               .value("I1294671590"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.identifier.isni'][0].value")
                               .value("https://www.isni.org/wiki/1234"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.identifier.rid'][0].value")
                               .value("https://www.rid.org/wiki/1234"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.title'][0].value")
                                      .value("Centre National de la Recherche Scientifique"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['organization.legalName'][0].value")
                               .value("Centre National de la Recherche Scientifique"))
                       .andExpect(jsonPath(
                           "$._embedded.externalSourceEntries[0].metadata['organization.address.addressCountry'][0]" +
                               ".value")
                                      .value("FR"))
                       .andExpect(jsonPath(
                           "$._embedded.externalSourceEntries[0].metadata['organization.address.addressLocality'][0]" +
                               ".value")
                                      .value("Paris Test Region France"))
                       .andExpect(jsonPath(
                           "$._embedded.externalSourceEntries[0].metadata['organization.identifier.ror'][0].value")
                                      .value("https://ror.org/02feahw73"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.description'][0].value")
                                      .value("CNRS"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.description'][1].value")
                                      .value("French National Centre for Scientific Research"));

            verify(liveImportClient, times(2)).executeHttpGetRequest(anyInt(), anyString(), anyMap());
        }
    }


    @Test
    public void findAllOpenalexInstitutionExternalSourceEntriesWithQueryTest() throws Exception {

        try (InputStream file = getClass().getResourceAsStream("openalex-institution-multiple.json")) {
            String jsonResponse = IOUtils.toString(file, Charset.defaultCharset());

            when(liveImportClient.executeHttpGetRequest(anyInt(), anyString(), anyMap()))
                .thenReturn(jsonResponse);

            getClient().perform(get("/api/integration/externalsources/openalexInstitution/entries")
                                    .param("query", "Centre"))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$._embedded.externalSourceEntries", hasSize(2)))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries",
                                           Matchers.containsInAnyOrder(
                                               ExternalSourceEntryMatcher.matchExternalSourceEntry(
                                                   "I1294671590",
                                                   "Centre National de la Recherche Scientifique",
                                                   "Centre National de la Recherche Scientifique",
                                                   "openalexInstitution"
                                               ),
                                               ExternalSourceEntryMatcher.matchExternalSourceEntry(
                                                   "I4210118689",
                                                   "Joint Research Centre",
                                                   "Joint Research Centre",
                                                   "openalexInstitution"
                                               )
                                           )
                       ));

            verify(liveImportClient, times(2)).executeHttpGetRequest(anyInt(), anyString(), anyMap());
        }
    }

}
