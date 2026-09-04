/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority.zdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dspace.app.client.DSpaceHttpClientFactory;
import org.dspace.authority.AuthorityValue;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link ZDBService}.
 *
 * <p>These tests exercise the defensive guards added around {@code response.getStatusLine()},
 * {@code response.getEntity()} and the {@code records} element look-up, together with the happy
 * paths (search and detail endpoints) and the input-validation branches, so that all code branches
 * of the service are covered.</p>
 *
 * @author DSpace
 */
public class ZDBServiceTest {

    private static final String SEARCH_URL = "https://example.org/sru?operation=searchRetrieve";
    private static final String DETAIL_URL_TEMPLATE = "https://example.org/detail?id={0}";

    /**
     * A well-formed SRU search response containing a single record. The prefixed element names
     * (e.g. {@code rdf:Description}, {@code dc:title}) mirror the real ZDB SRU payload and match
     * the namespace-unaware look-ups performed by {@link org.dspace.app.util.XMLUtils}.
     */
    private static final String SEARCH_XML_ONE_RECORD =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<searchRetrieveResponse xmlns=\"http://www.loc.gov/zing/srw/\""
        + " xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\""
        + " xmlns:dc=\"http://purl.org/dc/elements/1.1/\""
        + " xmlns:bibo=\"http://purl.org/ontology/bibo/\""
        + " xmlns:dcterms=\"http://purl.org/dc/terms/\">"
        + "<numberOfRecords>1</numberOfRecords>"
        + "<records>"
        + "<record>"
        + "<recordData>"
        + "<rdf:RDF>"
        + "<rdf:Description rdf:about=\"https://zdb.org/12345\">"
        + "<dc:title>Acta Mathematica</dc:title>"
        + "<dc:title>Acta Math. Journal</dc:title>"
        + "<dc:publisher>Springer</dc:publisher>"
        + "<bibo:issn>1234-5678</bibo:issn>"
        + "<dcterms:alternative>Acta Math</dcterms:alternative>"
        + "</rdf:Description>"
        + "</rdf:RDF>"
        + "</recordData>"
        + "</record>"
        + "</records>"
        + "</searchRetrieveResponse>";

    /**
     * A well-formed detail response whose document root is {@code rdf:RDF}, as returned by the
     * ZDB detail endpoint.
     */
    private static final String DETAIL_XML =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\""
        + " xmlns:dc=\"http://purl.org/dc/elements/1.1/\""
        + " xmlns:bibo=\"http://purl.org/ontology/bibo/\""
        + " xmlns:dcterms=\"http://purl.org/dc/terms/\">"
        + "<rdf:Description rdf:about=\"https://zdb.org/98765\">"
        + "<dc:title>Journal of Testing</dc:title>"
        + "<dc:publisher>Elsevier</dc:publisher>"
        + "<bibo:issn>9876-5432</bibo:issn>"
        + "</rdf:Description>"
        + "</rdf:RDF>";

    private ZDBService zdbService;

    private CloseableHttpClient httpClient;

    @Mock
    private ConfigurationService configurationService;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        zdbService = new ZDBService();
        ReflectionTestUtils.setField(zdbService, "configurationService", configurationService);
        when(configurationService.getProperty("cris.zdb.search.url")).thenReturn(SEARCH_URL);
    }

    @Test
    public void testListReturnsEmptyWhenStatusLineIsNull() throws Exception {
        // A null status line is guarded against (no NPE): the error is logged and an
        // empty result list is returned instead of dereferencing the missing status line.
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(null);

        try (MockedStatic<DSpaceHttpClientFactory> ignored = mockClient(response)) {
            List<ZDBAuthorityValue> results = zdbService.listRecords("Acta", 0, 10);
            assertTrue("Expected an empty result list when the response has no status line",
                results.isEmpty());
        }
    }

    @Test
    public void testListReturnsEmptyWhenStatusCodeIsNotOk() throws Exception {
        // A non-OK status code raises an IOException that is caught, logged and turned into an
        // empty result list.
        CloseableHttpResponse response = mockResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, null);

        try (MockedStatic<DSpaceHttpClientFactory> ignored = mockClient(response)) {
            List<ZDBAuthorityValue> results = zdbService.listRecords("Acta", 0, 10);
            assertTrue("Expected an empty result list when the response status is not 200",
                results.isEmpty());
        }
    }

    @Test
    public void testListReturnsEmptyOnHttp200WithoutBody() throws Exception {
        // A HTTP 200 is theoretically possible without a body (null entity). The guard prevents an
        // NPE on getContent(): the error is logged and an empty result list is returned.
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(response.getStatusLine()).thenReturn(statusLine);
        when(response.getEntity()).thenReturn(null);

        try (MockedStatic<DSpaceHttpClientFactory> ignored = mockClient(response)) {
            List<ZDBAuthorityValue> results = zdbService.listRecords("Acta", 0, 10);
            assertTrue("Expected an empty result list for a 200 response without a body",
                results.isEmpty());
        }
    }

    @Test
    public void testListReturnsEmptyWhenRecordsElementMissing() throws Exception {
        // A well-formed SRU response that does not contain any "records" element.
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<searchRetrieveResponse xmlns=\"http://www.loc.gov/zing/srw/\">"
            + "<numberOfRecords>0</numberOfRecords>"
            + "</searchRetrieveResponse>";

        CloseableHttpResponse response = mockResponse(HttpStatus.SC_OK, xml);

        try (MockedStatic<DSpaceHttpClientFactory> ignored = mockClient(response)) {
            List<ZDBAuthorityValue> results = zdbService.listRecords("Acta", 0, 10);
            assertTrue("Expected an empty result list when no 'records' element is present",
                results.isEmpty());
        }
    }

    @Test
    public void testListReturnsEmptyOnMalformedXml() throws Exception {
        // A 200 response with a malformed body triggers a SAXException that is caught, logged and
        // turned into an empty result list.
        CloseableHttpResponse response = mockResponse(HttpStatus.SC_OK, "<not-well-formed>");

        try (MockedStatic<DSpaceHttpClientFactory> ignored = mockClient(response)) {
            List<ZDBAuthorityValue> results = zdbService.listRecords("Acta", 0, 10);
            assertTrue("Expected an empty result list when the response body is malformed XML",
                results.isEmpty());
        }
    }

    @Test
    public void testListParsesSearchRecords() throws Exception {
        // A well-formed search response with one record is parsed into a single authority value,
        // exercising the search branch (non rdf:RDF root with a populated "records" element).
        CloseableHttpResponse response = mockResponse(HttpStatus.SC_OK, SEARCH_XML_ONE_RECORD);

        try (MockedStatic<DSpaceHttpClientFactory> ignored = mockClient(response)) {
            List<ZDBAuthorityValue> results = zdbService.listRecords("Acta", 0, 10);

            assertEquals("Expected a single parsed record", 1, results.size());
            ZDBAuthorityValue value = results.get(0);
            assertEquals("Unexpected service id", "12345", value.getServiceId());
            assertEquals("Unexpected primary title", "Acta Mathematica", value.getValue());
            assertEquals("Unexpected ISSN metadata", List.of("1234-5678"),
                value.getOtherMetadata().get("journalIssn"));
            assertEquals("Unexpected publisher metadata", List.of("Springer"),
                value.getOtherMetadata().get("journalPublisher"));
            // The first title becomes the value; any further titles are stored as other metadata
            assertEquals("Unexpected secondary title metadata", List.of("Acta Math. Journal"),
                value.getOtherMetadata().get("journalTitle"));
            assertEquals("Unexpected alternative title metadata", List.of("Acta Math"),
                value.getOtherMetadata().get("journalAlternativeTitle"));
        }
    }

    @Test
    public void testListTotalComesFromNumberOfRecords() throws Exception {
        // The grand total must come from the SRU "numberOfRecords" element (42 here), not from the
        // number of records actually returned on this page (1).
        String xml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<searchRetrieveResponse xmlns=\"http://www.loc.gov/zing/srw/\""
            + " xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\""
            + " xmlns:dc=\"http://purl.org/dc/elements/1.1/\">"
            + "<numberOfRecords>42</numberOfRecords>"
            + "<records>"
            + "<record><recordData><rdf:RDF>"
            + "<rdf:Description rdf:about=\"https://zdb.org/12345\">"
            + "<dc:title>Acta Mathematica</dc:title>"
            + "</rdf:Description>"
            + "</rdf:RDF></recordData></record>"
            + "</records>"
            + "</searchRetrieveResponse>";

        CloseableHttpResponse response = mockResponse(HttpStatus.SC_OK, xml);

        try (MockedStatic<DSpaceHttpClientFactory> ignored = mockClient(response)) {
            ZDBSearchResult result = zdbService.list("Acta", 0, 10);
            assertEquals("Total must be read from numberOfRecords", 42, result.getTotal());
            assertEquals("Expected a single parsed record on this page", 1, result.getRecords().size());
        }
    }

    @Test
    public void testListTotalZeroWhenNumberOfRecordsMissing() throws Exception {
        // When the SRU response has no "numberOfRecords" element the total defaults to 0.
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<searchRetrieveResponse xmlns=\"http://www.loc.gov/zing/srw/\">"
            + "</searchRetrieveResponse>";

        CloseableHttpResponse response = mockResponse(HttpStatus.SC_OK, xml);

        try (MockedStatic<DSpaceHttpClientFactory> ignored = mockClient(response)) {
            ZDBSearchResult result = zdbService.list("Acta", 0, 10);
            assertEquals("Total should default to 0 when numberOfRecords is absent",
                0, result.getTotal());
            assertTrue("Expected no records when none are present", result.getRecords().isEmpty());
        }
    }

    @Test
    public void testDetailsReturnsRecordForRdfResponse() throws Exception {
        // The detail endpoint returns a document whose root is rdf:RDF, exercising that branch.
        when(configurationService.getProperty("cris.zdb.detail.url")).thenReturn(DETAIL_URL_TEMPLATE);

        CloseableHttpResponse response = mockResponse(HttpStatus.SC_OK, DETAIL_XML);

        try (MockedStatic<DSpaceHttpClientFactory> ignored = mockClient(response)) {
            AuthorityValue value = zdbService.details("98765-2");

            assertNotNull("Expected a non-null authority value for a valid detail response", value);
            assertEquals("Unexpected service id", "98765", value.getServiceId());
            assertEquals("Unexpected primary title", "Journal of Testing", value.getValue());
        }
    }

    @Test
    public void testDetailsReturnsNullWhenNoResults() throws Exception {
        // When the detail search yields no records, details() returns null.
        when(configurationService.getProperty("cris.zdb.detail.url")).thenReturn(DETAIL_URL_TEMPLATE);

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<searchRetrieveResponse xmlns=\"http://www.loc.gov/zing/srw/\">"
            + "<numberOfRecords>0</numberOfRecords>"
            + "</searchRetrieveResponse>";
        CloseableHttpResponse response = mockResponse(HttpStatus.SC_OK, xml);

        try (MockedStatic<DSpaceHttpClientFactory> ignored = mockClient(response)) {
            AuthorityValue value = zdbService.details("98765-2");
            assertNull("Expected null when the detail search returns no records", value);
        }
    }

    @Test
    public void testListSendsPaginationParameters() throws Exception {
        // A positive start/rows must translate to SRU startRecord (1-based) and maximumRecords params.
        CloseableHttpResponse response = mockResponse(HttpStatus.SC_OK, SEARCH_XML_ONE_RECORD);

        try (MockedStatic<DSpaceHttpClientFactory> ignored = mockClient(response)) {
            zdbService.list("Acta", 20, 10);

            HttpGet executed = capturedRequest();
            String uri = executed.getURI().toASCIIString();
            // 0-based offset 20 becomes 1-based startRecord 21
            assertTrue("Expected startRecord=21 in the SRU request URL, was: " + uri,
                uri.contains("startRecord=21"));
            assertTrue("Expected maximumRecords=10 in the SRU request URL, was: " + uri,
                uri.contains("maximumRecords=10"));
        }
    }

    @Test
    public void testListOmitsMaximumRecordsWhenRowsNotPositive() throws Exception {
        // When rows is not positive the maximumRecords param is omitted so the SRU service applies
        // its own default page size, but startRecord is still sent for the (0-based) offset.
        CloseableHttpResponse response = mockResponse(HttpStatus.SC_OK, SEARCH_XML_ONE_RECORD);

        try (MockedStatic<DSpaceHttpClientFactory> ignored = mockClient(response)) {
            zdbService.list("Acta", 0, 0);

            HttpGet executed = capturedRequest();
            String uri = executed.getURI().toASCIIString();
            assertTrue("Expected startRecord=1 in the SRU request URL, was: " + uri,
                uri.contains("startRecord=1"));
            assertFalse("maximumRecords should be omitted when rows is not positive, was: " + uri,
                uri.contains("maximumRecords"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testListThrowsWhenQueryIsEmpty() throws Exception {
        zdbService.list("", 0, 10);
    }

    @Test(expected = IllegalStateException.class)
    public void testListThrowsWhenSearchUrlMissing() throws Exception {
        when(configurationService.getProperty("cris.zdb.search.url")).thenReturn(null);
        zdbService.list("Acta", 0, 10);
    }

    @Test(expected = IllegalStateException.class)
    public void testDetailsThrowsWhenDetailUrlMissing() throws Exception {
        when(configurationService.getProperty("cris.zdb.detail.url")).thenReturn("");
        zdbService.details("12345-6");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildDetailsUrlRejectsInvalidId() {
        zdbService.buildDetailsURL("not-a-valid-id");
    }

    @Test
    public void testBuildDetailsUrlBuildsUrlForValidId() {
        when(configurationService.getProperty("cris.zdb.detail.url")).thenReturn(DETAIL_URL_TEMPLATE);
        assertEquals("Unexpected detail URL", "https://example.org/detail?id=12345-6",
            zdbService.buildDetailsURL("12345-6"));
    }

    /**
     * Build a mocked {@link CloseableHttpResponse} with the given status code and (optionally) an
     * XML body served through its entity.
     *
     * @param statusCode the HTTP status code the response should report
     * @param xmlBody    the response body to serve, or {@code null} for no entity
     * @return the mocked response
     * @throws IOException never thrown by the mock setup
     */
    private CloseableHttpResponse mockResponse(int statusCode, String xmlBody) throws IOException {
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(response.getStatusLine()).thenReturn(statusLine);
        if (xmlBody != null) {
            HttpEntity entity = mock(HttpEntity.class);
            when(entity.getContent())
                .thenReturn(new ByteArrayInputStream(xmlBody.getBytes(StandardCharsets.UTF_8)));
            when(response.getEntity()).thenReturn(entity);
        }
        return response;
    }

    /**
     * Build a {@link MockedStatic} for {@link DSpaceHttpClientFactory} whose client returns
     * the supplied response for any executed request.
     *
     * @param response the response the mocked client should return
     * @return the static mock (to be used in a try-with-resources block)
     * @throws IOException never thrown by the mock setup
     */
    private MockedStatic<DSpaceHttpClientFactory> mockClient(CloseableHttpResponse response) throws IOException {
        httpClient = mock(CloseableHttpClient.class);
        when(httpClient.execute(any())).thenReturn(response);

        DSpaceHttpClientFactory factory = mock(DSpaceHttpClientFactory.class);
        when(factory.build()).thenReturn(httpClient);

        MockedStatic<DSpaceHttpClientFactory> mockedStatic = Mockito.mockStatic(DSpaceHttpClientFactory.class);
        mockedStatic.when(DSpaceHttpClientFactory::getInstance).thenReturn(factory);
        return mockedStatic;
    }

    /**
     * Capture the request executed against the mocked HTTP client, so that the constructed SRU URL
     * (including pagination parameters) can be asserted.
     *
     * @return the executed {@link HttpGet} request
     * @throws IOException never thrown by the mock verification
     */
    private HttpGet capturedRequest() throws IOException {
        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(httpClient).execute(captor.capture());
        return (HttpGet) captor.getValue();
    }
}
