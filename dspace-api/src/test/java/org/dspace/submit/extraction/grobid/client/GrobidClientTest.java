/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.extraction.grobid.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.dspace.service.impl.HttpConnectionPoolService;
import org.dspace.services.ConfigurationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;

/**
 * Unit tests for {@link GrobidClientImpl}.
 * Mocks HTTP responses to verify behaviour on success, no-content, and errors.
 *
 * @author Kim Shepherd
 */
@ExtendWith(MockitoExtension.class)
public class GrobidClientTest {

    @InjectMocks
    private GrobidClientImpl grobidClient = new GrobidClientImpl();

    @Mock
    private HttpConnectionPoolService httpConnectionPoolService;

    @Mock
    private ConfigurationService configurationService;

    @Mock
    private CloseableHttpClient httpClient;

    @Mock
    private CloseableHttpResponse httpResponse;

    @Mock
    private HttpEntity httpEntity;

    @BeforeEach
    public void setUp() throws Exception {
        // Catch config property gets and return a valid non-null URL to 'enable' the client
        when(configurationService.getProperty("grobid.service.url", null))
                .thenReturn("http://localhost:8070");
        when(configurationService.getIntProperty("grobid.client.maxRetries", 3))
                .thenReturn(3);
        when(configurationService.getIntProperty("grobid.client.retryInterval", 2000))
                .thenReturn(2000);

        grobidClient.init();
        // Lenient: not every test reaches the HTTP call (e.g. the disabled client test)
        lenient().when(httpConnectionPoolService.getClient(any())).thenReturn(httpClient);
        lenient().when(httpClient.execute(any(ClassicHttpRequest.class))).thenReturn(httpResponse);
    }

    @Test
    public void testValidResponseReturnsDocument() throws Exception {
        String teiXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<TEI xmlns=\"http://www.tei-c.org/ns/1.0\">"
            + "<teiHeader>"
            + "<fileDesc>"
            + "<titleStmt><title>Test Article</title></titleStmt>"
            + "</fileDesc>"
            + "</teiHeader>"
            + "</TEI>";

        when(httpResponse.getCode()).thenReturn(HttpStatus.SC_OK);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent()).thenReturn(
            new ByteArrayInputStream(teiXml.getBytes(StandardCharsets.UTF_8)));

        InputStream pdfStream = new ByteArrayInputStream("mock pdf content".getBytes(StandardCharsets.UTF_8));
        Optional<Document> result = grobidClient.retrieveHeaderDocument(pdfStream);

        assertTrue(result.isPresent(), "Valid TEI XML response should result in valid parsed Document," +
                "without explicit consolidate header set");
        Document doc = result.get();
        assertEquals("TEI", doc.getDocumentElement().getNodeName());
    }

    @Test
    public void testValidResponseWithConsolidateHeader() throws Exception {
        String teiXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<TEI xmlns=\"http://www.tei-c.org/ns/1.0\">"
                + "<teiHeader>"
                + "<fileDesc>"
                + "<titleStmt><title>Test Consolidated Article</title></titleStmt>"
                + "</fileDesc>"
                + "</teiHeader>"
                + "</TEI>";

        when(httpResponse.getCode()).thenReturn(HttpStatus.SC_OK);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent()).thenReturn(
                new ByteArrayInputStream(teiXml.getBytes(StandardCharsets.UTF_8)));

        InputStream pdfStream = new ByteArrayInputStream("mock pdf content".getBytes(StandardCharsets.UTF_8));
        Optional<Document> result = grobidClient.retrieveHeaderDocument(
                pdfStream, ConsolidateHeaderEnum.CONSOLIDATE_AND_INJECT_METADATA);

        assertTrue(result.isPresent(), "Valid TEI XML response should result in valid parsed Document," +
                "with explicit CONSOLIDATE_AND_INJECT_METADATA header set");
        Document doc = result.get();
        assertEquals("TEI", doc.getDocumentElement().getNodeName());
    }

    @Test
    public void testInvalidXmlResponseThrowsException() throws Exception {
        String invalidXml = "invalid XML :)";

        when(httpResponse.getCode()).thenReturn(HttpStatus.SC_OK);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent()).thenReturn(
            new ByteArrayInputStream(invalidXml.getBytes(StandardCharsets.UTF_8)));

        InputStream pdfStream = new ByteArrayInputStream("mock pdf content".getBytes(StandardCharsets.UTF_8));
        assertThrows(GrobidClientException.class, () -> grobidClient.retrieveHeaderDocument(pdfStream));
    }

    @Test
    public void testNoContentReturnsEmpty() throws Exception {
        when(httpResponse.getCode()).thenReturn(HttpStatus.SC_NO_CONTENT);

        InputStream pdfStream = new ByteArrayInputStream("mock pdf content".getBytes(StandardCharsets.UTF_8));
        Optional<Document> result = grobidClient.retrieveHeaderDocument(pdfStream);

        assertNotNull(result);
        assertFalse(result.isPresent(), "204 response should result in empty Optional return val");
    }

    @Test
    public void testServerErrorThrowsException() throws Exception {
        when(httpResponse.getCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent()).thenReturn(
                new ByteArrayInputStream("Internal Server Error".getBytes(StandardCharsets.UTF_8)));

        InputStream pdfStream = new ByteArrayInputStream("mock pdf content".getBytes(StandardCharsets.UTF_8));
        assertThrows(GrobidClientException.class, () -> grobidClient.retrieveHeaderDocument(pdfStream));
    }

    @Test
    public void testServiceUnavailableThrowsException() throws Exception {
        when(httpResponse.getCode()).thenReturn(HttpStatus.SC_SERVICE_UNAVAILABLE);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent()).thenReturn(
            new ByteArrayInputStream("Service Unavailable".getBytes(StandardCharsets.UTF_8)));

        InputStream pdfStream = new ByteArrayInputStream("mock pdf content".getBytes(StandardCharsets.UTF_8));
        assertThrows(GrobidClientException.class, () -> grobidClient.retrieveHeaderDocument(pdfStream));
    }

    @Test
    public void testDisabledClientThrowsException() throws Exception {
        // Override the earlier mock so we definitely return null now (disable)
        when(configurationService.getProperty("grobid.service.url", null))
                .thenReturn(null);
        grobidClient.init();

        InputStream pdfStream = new ByteArrayInputStream("mock pdf content".getBytes(StandardCharsets.UTF_8));
        assertThrows(GrobidClientException.class, () -> grobidClient.retrieveHeaderDocument(pdfStream));
    }

}
