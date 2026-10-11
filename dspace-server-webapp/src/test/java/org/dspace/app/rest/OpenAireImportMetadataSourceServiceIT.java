/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.liveimportclient.service.LiveImportClientImpl;
import org.dspace.importer.external.openaire.service.OpenAireImportMetadataSourceServiceImpl;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link OpenAireImportMetadataSourceServiceImpl}, with the OpenAIRE API mocked.
 */
public class OpenAireImportMetadataSourceServiceIT extends AbstractLiveImportIntegrationTest {

    private static final String TITLE = "A test record for the OpenAIRE importer";

    @Autowired
    private LiveImportClientImpl liveImportClientImpl;

    private OpenAireImportMetadataSourceServiceImpl openAireService;

    private CloseableHttpClient originalHttpClient;

    private CloseableHttpClient httpClient;

    @BeforeEach
    public void setup() throws Exception {
        openAireService = DSpaceServicesFactory.getInstance().getServiceManager()
            .getServiceByName("openaireImportServiceByTitle", OpenAireImportMetadataSourceServiceImpl.class);
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
        mockOpenAireResponse("openaire-publication-test.xml");

        Collection<ImportRecord> records = openAireService.getRecords("repository", 0, 10);

        assertEquals(1, records.size());
        assertEquals(TITLE, records.iterator().next().getSingleValue("dc", "title", null).get());
        assertEquals(Map.of("title", "repository", "page", "1", "size", "10"), sentQueryParameters());
    }

    @Test
    public void getRecordsCountTest() throws Exception {
        mockOpenAireResponse("openaire-publication-test.xml");

        assertEquals(1, openAireService.getRecordsCount("repository"));
        assertEquals(Map.of("title", "repository"), sentQueryParameters());
    }

    @Test
    public void getRecordByIdTest() throws Exception {
        mockOpenAireResponse("openaire-publication-test.xml");

        ImportRecord record = openAireService.getRecord("test________::0000");

        assertEquals(TITLE, record.getSingleValue("dc", "title", null).get());
        assertEquals(Map.of("openairePublicationID", "test________::0000"), sentQueryParameters());
    }

    @Test
    public void getRecordsCountWithServerErrorTest() throws Exception {
        CloseableHttpResponse response = mockResponse("", 500, "Internal Server Error");
        when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);

        assertEquals(0, openAireService.getRecordsCount("repository"));
    }

    private void mockOpenAireResponse(String resource) throws Exception {
        try (InputStream body = getClass().getResourceAsStream(resource)) {
            CloseableHttpResponse response = mockResponse(IOUtils.toString(body, StandardCharsets.UTF_8), 200, "OK");
            when(httpClient.execute(ArgumentMatchers.any())).thenReturn(response);
        }
    }

    /**
     * @return the decoded query parameters of the single request sent to the OpenAIRE API
     */
    private Map<String, String> sentQueryParameters() throws Exception {
        ArgumentCaptor<ClassicHttpRequest> request = ArgumentCaptor.forClass(ClassicHttpRequest.class);
        verify(httpClient).execute(request.capture());
        List<NameValuePair> parameters = new URIBuilder(request.getValue().getUri()).getQueryParams();
        return parameters.stream().collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
    }
}
