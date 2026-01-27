/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.IOUtils;
import org.dspace.app.rest.matcher.ExternalSourceMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.importer.external.liveimportclient.service.LiveImportClient;
import org.dspace.importer.external.openalex.service.OpenAlexImportMetadataSourceServiceImpl;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 **/
public class OpenAlexPublicationByDOIExternalSourcesIT extends AbstractControllerIntegrationTest {


    @MockitoBean
    private LiveImportClient liveImportClient;

    @Autowired
    @Qualifier("openalexImportPublicationByDOIService")
    private OpenAlexImportMetadataSourceServiceImpl openAlexImportMetadataSourceService;


    @Before
    public void setUp() throws Exception {
        super.setUp();
        ReflectionTestUtils.setField(openAlexImportMetadataSourceService, "liveImportClient", liveImportClient);
    }

    @Test
    public void findOneOpenalexImportPublicationByDOIExternalSourceTest() throws Exception {
        getClient().perform(get("/api/integration/externalsources?size=25")).andExpect(status().isOk())
                   .andExpect(jsonPath("$._embedded.externalsources", Matchers.hasItem(
                       ExternalSourceMatcher.matchExternalSource("openalexPublicationByDOI",
                                                                 "openalexPublicationByDOI", false))));
    }

    @Test
    public void findOpenalexPublicationByDOIExternalSourceEntriesEmptyWithQueryTest() throws Exception {

        try (InputStream file = getClass().getResourceAsStream("openalex-empty.json")) {
            String jsonResponse = IOUtils.toString(file, Charset.defaultCharset());
            when(liveImportClient.executeHttpGetRequest(anyInt(), anyString(), anyMap()))
                .thenReturn(jsonResponse);


            getClient().perform(get("/api/integration/externalsources/openalexPublicationByDOI/entries")
                                    .param("query", "W1775749144"))
                       .andExpect(status().isOk()).andExpect(jsonPath("$.page.number", is(0)));
            verify(liveImportClient, times(2)).executeHttpGetRequest(anyInt(), anyString(), anyMap());
        }
    }

    @Test
    public void findOneOpenalexPublicationByDOIExternalSourceEntriesWithQueryTest() throws Exception {

        try (InputStream file = getClass().getResourceAsStream("openalex-publication-single.json")) {
            String jsonResponse = IOUtils.toString(file, Charset.defaultCharset());

            when(liveImportClient.executeHttpGetRequest(anyInt(), anyString(), anyMap()))
                .thenReturn(jsonResponse);

            getClient().perform(get("/api/integration/externalsources/openalexPublicationByDOI/entries")
                                    .param("query", "10.1016/s0021-9258(19)52451-6"))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$._embedded.externalSourceEntries", Matchers.hasSize(1)))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].id").value("W1775749144"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].display")
                                      .value("PROTEIN MEASUREMENT WITH THE FOLIN PHENOL REAGENT"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].value")
                                      .value("PROTEIN MEASUREMENT WITH THE FOLIN PHENOL REAGENT"))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].externalSource")
                                      .value("openalexPublicationByDOI"))
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
                                           Matchers.hasSize(3)))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.identifier.other'][0].value")
                               .value("https://pubmed.ncbi.nlm.nih.gov/14907713"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.identifier.other'][1].value")
                               .value("https://doi.org/10.1016/s0021-9258(19)52451-6/pdf"))
                       .andExpect(
                           jsonPath("$._embedded.externalSourceEntries[0].metadata['dc.identifier.other'][2].value")
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

            // Capture arguments
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Map<String, Map<String, String>>> paramsCaptor = ArgumentCaptor.forClass(Map.class);

            verify(liveImportClient, times(2)).executeHttpGetRequest(anyInt(), urlCaptor.capture(),
                                                                     paramsCaptor.capture());

            // Assert the URL is correct
            assertEquals(2, urlCaptor.getAllValues().size());
            assertEquals("https://api.openalex.org/works", urlCaptor.getAllValues().get(0));
            assertEquals("https://api.openalex.org/works", urlCaptor.getAllValues().get(1));

            // Assert the parameters contain "filter" => "authorships.author.id:"
            assertEquals(2, paramsCaptor.getAllValues().size());
            Map<String, Map<String, String>> capturedParams = paramsCaptor.getAllValues().get(0);
            assertTrue(capturedParams.containsKey("uriParameters"));
            assertEquals("doi:10.1016/s0021-9258(19)52451-6", capturedParams.get("uriParameters").get("filter"));
            assertEquals("20", capturedParams.get("uriParameters").get("per_page"));
            assertEquals("1", capturedParams.get("uriParameters").get("page"));
            Map<String, Map<String, String>> capturedParams1 = paramsCaptor.getAllValues().get(1);
            assertTrue(capturedParams1.containsKey("uriParameters"));
            assertEquals("doi:10.1016/s0021-9258(19)52451-6", capturedParams1.get("uriParameters").get("filter"));
        }
    }

    @Test
    public void findOneOpenalexPublicationByDOIExternalSourceEntriesWithUrlDOIQueryTest() throws Exception {

        try (InputStream file = getClass().getResourceAsStream("openalex-publication-single.json")) {
            String jsonResponse = IOUtils.toString(file, Charset.defaultCharset());

            when(liveImportClient.executeHttpGetRequest(anyInt(), anyString(), anyMap()))
                .thenReturn(jsonResponse);

            getClient().perform(get("/api/integration/externalsources/openalexPublicationByDOI/entries")
                                    .param("query", "https://doi.org/10.1016/s0021-9258(19)52451-6"))
                       .andExpect(status().isOk())
                       .andExpect(jsonPath("$._embedded.externalSourceEntries", Matchers.hasSize(1)))
                       .andExpect(jsonPath("$._embedded.externalSourceEntries[0].id").value("W1775749144"));

            // Capture arguments
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Map<String, Map<String, String>>> paramsCaptor = ArgumentCaptor.forClass(Map.class);

            verify(liveImportClient, times(2)).executeHttpGetRequest(anyInt(), urlCaptor.capture(),
                                                                     paramsCaptor.capture());

            // Assert the URL is correct
            assertEquals(2, urlCaptor.getAllValues().size());
            assertEquals("https://api.openalex.org/works", urlCaptor.getAllValues().get(0));
            assertEquals("https://api.openalex.org/works", urlCaptor.getAllValues().get(1));

            // Assert the parameters contain "filter" => "authorships.author.id:"
            assertEquals(2, paramsCaptor.getAllValues().size());
            Map<String, Map<String, String>> capturedParams = paramsCaptor.getAllValues().get(0);
            assertTrue(capturedParams.containsKey("uriParameters"));
            assertEquals("doi:10.1016/s0021-9258(19)52451-6", capturedParams.get("uriParameters").get("filter"));
            assertEquals("20", capturedParams.get("uriParameters").get("per_page"));
            assertEquals("1", capturedParams.get("uriParameters").get("page"));
            Map<String, Map<String, String>> capturedParams1 = paramsCaptor.getAllValues().get(1);
            assertTrue(capturedParams1.containsKey("uriParameters"));
            assertEquals("doi:10.1016/s0021-9258(19)52451-6", capturedParams1.get("uriParameters").get("filter"));
        }
    }

    @Test
    public void createWorkspaceItemFromOpenAlexPubIdTest() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1)
                                           .withName("Collection 1")
                                           .withSubmitterGroup(eperson)
                                           .build();
        context.restoreAuthSystemState();

        AtomicReference<Integer> idRef1 = new AtomicReference<>();
        try (InputStream file = getClass().getResourceAsStream("openalex-publication-by-author-id.json")) {
            String jsonResponse = IOUtils.toString(file, Charset.defaultCharset());

            when(liveImportClient.executeHttpGetRequest(anyInt(), anyString(), anyMap()))
                .thenReturn(jsonResponse);

            String authToken = getAuthToken(eperson.getEmail(), password);

            // create a workspaceitem explicitly in the col1
            getClient(authToken).perform(post("/api/submission/workspaceitems")
                                             .param("owningCollection", col1.getID().toString())
                                             .param("embed", "item,sections,collection")
                                             .contentType(MediaType.parseMediaType("text/uri-list"))
                                             .content(
                                                 "http://localhost:8080/server/api/integration/externalsources" +
                                                     "/openalexPublicationByDOI/entryValues/W1775749144"))
                                .andExpect(status().isCreated())
                                .andDo(result -> idRef1.set(read(result.getResponse().getContentAsString(), "$.id")))
                                .andExpect(jsonPath("$._embedded.item.name")
                                               .value("PROTEIN MEASUREMENT WITH THE FOLIN PHENOL REAGENT"))
                                .andExpect(jsonPath("$._embedded.item.metadata['dc.contributor.author']",
                                                    Matchers.hasSize(4)))
                                .andExpect(
                                    jsonPath("$._embedded.item.metadata['dc.contributor.author'][0].value")
                                        .value("OliverH. Lowry"))
                                .andExpect(
                                    jsonPath("$._embedded.item.metadata['dc.contributor.author'][1].value")
                                        .value("NiraJ. Rosebrough"))
                                .andExpect(
                                    jsonPath("$._embedded.item.metadata['dc.contributor.author'][2].value")
                                        .value("A. Farr"))
                                .andExpect(
                                    jsonPath("$._embedded.item.metadata['dc.contributor.author'][3].value")
                                        .value("RoseJ. Randall"))
                                .andExpect(jsonPath("$._embedded.item.metadata['dc.identifier.other']",
                                                    Matchers.hasSize(2)))
                                .andExpect(
                                    jsonPath("$._embedded.item.metadata['dc.identifier.other'][0].value")
                                        .value("https://pubmed.ncbi.nlm.nih.gov/14907713"))
                                .andExpect(
                                    jsonPath("$._embedded.item.metadata['dc.identifier.other'][1].value")
                                        .value("/best_oa_location/pdf_url"))
                                .andExpect(jsonPath("$._embedded.item.metadata['dc.date.issued'][0].value")
                                               .value("1951-11-01"))
                                .andExpect(jsonPath("$._embedded.item.metadata['dc.identifier" +
                                                        ".openalex'][0].value").value("W1775749144"))
                                .andExpect(jsonPath("$._embedded.item.metadata['oaire.citation.volume'][0]" +
                                                        ".value")
                                               .value("193"))
                                .andExpect(
                                    jsonPath("$._embedded.item.metadata['oaire.citation.issue'][0].value")
                                        .value("1"))
                                .andExpect(jsonPath("$._embedded.item.metadata['oaire.citation" +
                                                        ".startPage'][0].value")
                                               .value("265"))
                                .andExpect(jsonPath("$._embedded.item.metadata['oaire.citation" +
                                                        ".endPage'][0].value")
                                               .value("275"))
                                .andExpect(
                                    jsonPath("$._embedded.item.metadata['oaire.version'][0].value")
                                        .value("http://purl.org/coar/version/c_970fb48d4fbd8a85"))
                                .andExpect(
                                    jsonPath("$._embedded.item.metadata['oaire.citation.title'][0].value")
                                        .value("Journal of Biological Chemistry"))
                                .andExpect(jsonPath("$._embedded.item.metadata['dc.identifier.issn']",
                                                    Matchers.hasSize(3)))
                                .andExpect(
                                    jsonPath("$._embedded.item.metadata['dc.identifier.doi'][0].value")
                                        .value("10.1016/s0021-9258(19)52451-6"))
                                .andExpect(jsonPath("$._embedded.item.metadata['dc.language.iso'][0].value")
                                               .value("en"))
                                .andExpect(jsonPath("$._embedded.item.metadata['dc.title'][0].value")
                                               .value("PROTEIN MEASUREMENT WITH THE FOLIN PHENOL REAGENT"))
                                .andExpect(jsonPath("$._embedded.item.metadata['dc.subject']",
                                                    Matchers.hasSize(13)))
                                .andExpect(jsonPath("$._embedded.item.metadata['dc.subject'][9].value")
                                               .value("Peer Review, Research standards"))
                                .andExpect(jsonPath("$._embedded.item.metadata['dc.subject'][12].value")
                                               .value(
                                                   "Life Sciences Biochemistry, Genetics and Molecular Biology " +
                                                       "Molecular " +
                                                       "Biology Cancer and biochemical research"))
                                .andExpect(
                                    jsonPath("$._embedded.item.metadata['dc.rights.license'][0].value")
                                        .value("cc-by"))
                                .andExpect(
                                    jsonPath("$._embedded.item.metadata['dc.type'][0].value")
                                        .value("Article"))
                                .andExpect(jsonPath("$._embedded.collection.id", is(col1.getID().toString())));

            // Capture arguments
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Map<String, Map<String, String>>> paramsCaptor = ArgumentCaptor.forClass(Map.class);

            verify(liveImportClient, times(1)).executeHttpGetRequest(anyInt(), urlCaptor.capture(),
                                                                     paramsCaptor.capture());

            // Assert the URL is correct
            assertEquals(1, urlCaptor.getAllValues().size());
            assertEquals("https://api.openalex.org/works/W1775749144", urlCaptor.getValue());
            assertTrue(paramsCaptor.getValue().isEmpty());
        } finally {
            WorkspaceItemBuilder.deleteWorkspaceItem(idRef1.get());
        }
    }
}
