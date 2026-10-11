/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.dspace.importer.external.arxiv.service.ArXivImportMetadataSourceServiceImpl;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.liveimportclient.service.LiveImportClientImpl;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link ArXivImportMetadataSourceServiceImpl}, with the arXiv API mocked.
 */
public class ArXivImportMetadataSourceServiceIT extends AbstractLiveImportIntegrationTest {

    @Autowired
    private LiveImportClientImpl liveImportClientImpl;

    private ArXivImportMetadataSourceServiceImpl arXivService;

    private CloseableHttpClient originalHttpClient;

    private CloseableHttpClient httpClient;

    @BeforeEach
    public void setup() throws Exception {
        arXivService = DSpaceServicesFactory.getInstance().getServiceManager()
            .getServiceByName("ArXivImportService", ArXivImportMetadataSourceServiceImpl.class);
        originalHttpClient = liveImportClientImpl.getHttpClient();
        httpClient = Mockito.mock(CloseableHttpClient.class);
        liveImportClientImpl.setHttpClient(httpClient);
    }

    @AfterEach
    public void restoreHttpClient() {
        liveImportClientImpl.setHttpClient(originalHttpClient);
    }

    @Test
    public void getRecordsTest() throws Exception {
        mockArXivResponse("arxiv-test.xml", 200, "OK");

        Collection<ImportRecord> records = arXivService.getRecords("all:repository", 0, 1);

        assertEquals(1, records.size());
        ImportRecord record = records.iterator().next();
        assertEquals("A test record for the arXiv importer", record.getSingleValue("dc", "title", null).get());
        assertEquals(Map.of("search_query", "all:repository", "max_results", "1", "start", "0"),
                     sentQueryParameters());
    }

    @Test
    public void getRecordsCountTest() throws Exception {
        mockArXivResponse("arxiv-test.xml", 200, "OK");

        assertEquals(1, arXivService.getRecordsCount("all:repository"));
        assertEquals(Map.of("search_query", "all:repository"), sentQueryParameters());
    }

    @Test
    public void getRecordByIdTest() throws Exception {
        mockArXivResponse("arxiv-test.xml", 200, "OK");

        ImportRecord record = arXivService.getRecord("arXiv:0000.00000");

        assertEquals("A test record for the arXiv importer", record.getSingleValue("dc", "title", null).get());
        assertEquals(Map.of("id_list", "0000.00000"), sentQueryParameters());
    }

    @Test
    public void getRecordsWithServerErrorTest() throws Exception {
        CloseableHttpResponse response = mockResponse("", 500, "Internal Server Error");
        when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

        assertNull(arXivService.getRecord("arXiv:0000.00000"));
    }

    private void mockArXivResponse(String resource, int status, String reason) throws Exception {
        try (InputStream body = getClass().getResourceAsStream(resource)) {
            CloseableHttpResponse response =
                mockResponse(IOUtils.toString(body, StandardCharsets.UTF_8), status, reason);
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);
        }
    }

    /**
     * @return the decoded query parameters of the single request sent to the arXiv API
     */
    private Map<String, String> sentQueryParameters() throws Exception {
        ArgumentCaptor<ClassicHttpRequest> request = ArgumentCaptor.forClass(ClassicHttpRequest.class);
        verify(httpClient).execute(request.capture());
        List<NameValuePair> parameters = new URIBuilder(request.getValue().getUri()).getQueryParams();
        return parameters.stream().collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
    }
}
