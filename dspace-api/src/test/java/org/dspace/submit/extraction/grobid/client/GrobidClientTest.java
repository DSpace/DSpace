/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.extraction.grobid.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dspace.service.impl.HttpConnectionPoolService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.w3c.dom.Document;

/**
 * Unit tests for {@link GrobidClientImpl}.
 * Mocks HTTP responses to verify behaviour on success, no-content, and errors.
 *
 * @author Kim Shepherd
 */
@RunWith(MockitoJUnitRunner.class)
public class GrobidClientTest {

    @InjectMocks
    private GrobidClientImpl grobidClient = new GrobidClientImpl("http://localhost:8070");

    @Mock
    private HttpConnectionPoolService httpConnectionPoolService;

    @Mock
    private CloseableHttpClient httpClient;

    @Mock
    private CloseableHttpResponse httpResponse;

    @Mock
    private StatusLine statusLine;

    @Mock
    private HttpEntity httpEntity;

    @Before
    public void setUp() throws Exception {
        when(httpConnectionPoolService.getClient()).thenReturn(httpClient);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
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

        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent()).thenReturn(
            new ByteArrayInputStream(teiXml.getBytes(StandardCharsets.UTF_8)));

        InputStream pdfStream = new ByteArrayInputStream("mock pdf content".getBytes(StandardCharsets.UTF_8));
        Optional<Document> result = grobidClient.retrieveHeaderDocument(pdfStream);

        assertTrue("Valid TEI XML response should result in valid parsed Document," +
                "without explicit consolidate header set", result.isPresent());
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

        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent()).thenReturn(
                new ByteArrayInputStream(teiXml.getBytes(StandardCharsets.UTF_8)));

        InputStream pdfStream = new ByteArrayInputStream("mock pdf content".getBytes(StandardCharsets.UTF_8));
        Optional<Document> result = grobidClient.retrieveHeaderDocument(
                pdfStream, ConsolidateHeaderEnum.CONSOLIDATE_AND_INJECT_METADATA);

        assertTrue("Valid TEI XML response should result in valid parsed Document," +
                "with explicit CONSOLIDATE_AND_INJECT_METADATA header set", result.isPresent());
        Document doc = result.get();
        assertEquals("TEI", doc.getDocumentElement().getNodeName());
    }

    @Test(expected = GrobidClientException.class)
    public void testInvalidXmlResponseThrowsException() throws Exception {
        String invalidXml = "invalid XML :)";

        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent()).thenReturn(
            new ByteArrayInputStream(invalidXml.getBytes(StandardCharsets.UTF_8)));

        InputStream pdfStream = new ByteArrayInputStream("mock pdf content".getBytes(StandardCharsets.UTF_8));
        grobidClient.retrieveHeaderDocument(pdfStream);
    }

    @Test
    public void testNoContentReturnsEmpty() throws Exception {
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_NO_CONTENT);

        InputStream pdfStream = new ByteArrayInputStream("mock pdf content".getBytes(StandardCharsets.UTF_8));
        Optional<Document> result = grobidClient.retrieveHeaderDocument(pdfStream);

        assertNotNull(result);
        assertFalse("204 response should result in empty Optional return val", result.isPresent());
    }

    @Test(expected = GrobidClientException.class)
    public void testServerErrorThrowsException() throws Exception {
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent()).thenReturn(
                new ByteArrayInputStream("Internal Server Error".getBytes(StandardCharsets.UTF_8)));

        InputStream pdfStream = new ByteArrayInputStream("mock pdf content".getBytes(StandardCharsets.UTF_8));
        grobidClient.retrieveHeaderDocument(pdfStream);
    }

    @Test(expected = GrobidClientException.class)
    public void testServiceUnavailableThrowsException() throws Exception {
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_SERVICE_UNAVAILABLE);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent()).thenReturn(
            new ByteArrayInputStream("Service Unavailable".getBytes(StandardCharsets.UTF_8)));

        InputStream pdfStream = new ByteArrayInputStream("mock pdf content".getBytes(StandardCharsets.UTF_8));
        grobidClient.retrieveHeaderDocument(pdfStream);
    }

}
