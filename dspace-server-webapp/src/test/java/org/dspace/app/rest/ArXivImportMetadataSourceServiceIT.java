/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.dspace.importer.external.arxiv.service.ArXivImportMetadataSourceServiceImpl;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.services.ConfigurationService;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link ArXivImportMetadataSourceServiceImpl} exact-id-only behaviour.
 */
public class ArXivImportMetadataSourceServiceIT extends AbstractLiveImportIntegrationTest {

    @Autowired
    private ArXivImportMetadataSourceServiceImpl arXivServiceImpl;

    @Autowired
    private ConfigurationService configurationService;

    /**
     * This method tests that when arxiv.exact-id-only = true and we search for a valid ID that does not exist
     * -> we get no results
     */
    @Test
    public void arxivExactIdOnlyEnabledDoesNotFallbackToQueryTest() throws Exception {
        boolean originalArxivExactId = configurationService.getBooleanProperty("arxiv.exact-id-only", false);

        configurationService.setProperty("arxiv.exact-id-only", true);
        try (InputStream emptyFeed = getClass().getResourceAsStream("arxiv-empty.xml");
             MockedStatic<ClientBuilder> clientBuilder = mockStatic(ClientBuilder.class)) {
            String emptyResponse = IOUtils.toString(emptyFeed, Charset.defaultCharset());
            WebTarget webTarget = mockArxivClient(clientBuilder, emptyResponse);

            Collection<ImportRecord> records = arXivServiceImpl.getRecords("1234.56789", 0, 2);
            assertEquals(0, records.size());
            verify(webTarget).queryParam(eq("id_list"), eq("1234.56789"));
            verify(webTarget, never()).queryParam(eq("search_query"), any());
        } finally {
            configurationService.setProperty("arxiv.exact-id-only", originalArxivExactId);
        }
    }

    /**
     * This method tests that when arxiv.exact-id-only = false, and we don't give an exact ID match
     * -> The fallback query is used to retrieve non-exact values
     */
    @Test
    public void arxivExactIdOnlyDisabledFallsBackToQueryTest() throws Exception {
        boolean originalArxivExactId = configurationService.getBooleanProperty("arxiv.exact-id-only", false);

        configurationService.setProperty("arxiv.exact-id-only", false);
        try (InputStream emptyFeed = getClass().getResourceAsStream("arxiv-empty.xml");
             InputStream queryFeed = getClass().getResourceAsStream("arxiv-query-test.xml");
             MockedStatic<ClientBuilder> clientBuilder = mockStatic(ClientBuilder.class)) {
            String emptyResponse = IOUtils.toString(emptyFeed, Charset.defaultCharset());
            String queryResponse = IOUtils.toString(queryFeed, Charset.defaultCharset());
            WebTarget webTarget = mockArxivClient(clientBuilder, emptyResponse, queryResponse);

            Collection<ImportRecord> records = arXivServiceImpl.getRecords("1234.56789", 0, 2);
            assertEquals(1, records.size());
            verify(webTarget).queryParam(eq("id_list"), eq("1234.56789"));
            verify(webTarget).queryParam(eq("search_query"), eq("1234.56789"));
        } finally {
            configurationService.setProperty("arxiv.exact-id-only", originalArxivExactId);
        }
    }

    /**
     * This method tests that if arxiv.exact-id-only = true and we didn't provide a valid DOI
     * -> an empty list is returned
     */
    @Test
    public void arxivExactIdOnlyEnabledNonIdReturnsEmptyTest() throws Exception {
        boolean originalArxivExactId = configurationService.getBooleanProperty("arxiv.exact-id-only", false);

        configurationService.setProperty("arxiv.exact-id-only", true);
        try (MockedStatic<ClientBuilder> clientBuilder = mockStatic(ClientBuilder.class)) {
            WebTarget webTarget = mockArxivClient(clientBuilder, "");

            Collection<ImportRecord> records = arXivServiceImpl.getRecords("test query", 0, 2);
            assertEquals(0, records.size());
            verify(webTarget, never()).queryParam(anyString(), any());
        } finally {
            configurationService.setProperty("arxiv.exact-id-only", originalArxivExactId);
        }
    }

    private WebTarget mockArxivClient(MockedStatic<ClientBuilder> clientBuilder, String... responses) {
        Client client = mock(Client.class);
        WebTarget webTarget = mock(WebTarget.class);
        WebTarget local = mock(WebTarget.class);
        Invocation.Builder builder = mock(Invocation.Builder.class);
        Response response = mock(Response.class);

        clientBuilder.when(ClientBuilder::newClient).thenReturn(client);
        when(client.target(anyString())).thenReturn(webTarget);
        when(webTarget.queryParam(anyString(), any())).thenReturn(local);
        when(local.queryParam(anyString(), any())).thenReturn(local);
        when(local.request(any(MediaType.class))).thenReturn(builder);
        when(builder.get()).thenReturn(response);
        when(response.getStatus()).thenReturn(200);
        if (responses.length == 1) {
            when(response.readEntity(String.class)).thenReturn(responses[0]);
        } else {
            when(response.readEntity(String.class)).thenReturn(responses[0],
                    Arrays.copyOfRange(responses, 1, responses.length));
        }
        return webTarget;
    }
}
