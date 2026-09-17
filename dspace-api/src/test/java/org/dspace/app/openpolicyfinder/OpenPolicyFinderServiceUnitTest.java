/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.openpolicyfinder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.dspace.app.openpolicyfinder.v2.OpenPolicyFinderResponse;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Focused unit tests for {@link OpenPolicyFinderService#performRequest}, exercising the shared
 * retry/error-handling loop in {@code executeRequest}. The existing
 * {@link OpenPolicyFinderServiceTest} uses {@link MockOpenPolicyFinderService} which overrides
 * {@code performRequest} entirely, so it cannot exercise the HTTP retry logic. These tests inject
 * a mocked {@link CloseableHttpClient} via the protected {@link OpenPolicyFinderService#newHttpClient()}
 * hook.
 *
 * <p>Note: this test does not extend {@code AbstractDSpaceTest} so it does not start the DSpace
 * kernel. Configuration is supplied by writing the {@code endpoint} and {@code apiKey} fields
 * directly via {@link ReflectionTestUtils}.</p>
 */
public class OpenPolicyFinderServiceUnitTest {

    private TestableOpenPolicyFinderService service;
    private CloseableHttpClient mockClient;

    @Before
    public void setUp() {
        mockClient = mock(CloseableHttpClient.class);
        service = new TestableOpenPolicyFinderService(mockClient);
        service.setMaxNumberOfTries(3);
        service.setSleepBetweenTimeouts(0L);
        service.setTimeout(1000);
        ReflectionTestUtils.setField(service, "endpoint", "https://api.openpolicyfinder.jisc.ac.uk/retrieve");
        ReflectionTestUtils.setField(service, "apiKey", "test-api-key");
    }

    @Test
    public void missingApiKey_returnsErrorImmediatelyAndDoesNotCallHttp() throws Exception {
        ReflectionTestUtils.setField(service, "apiKey", null);

        OpenPolicyFinderResponse response = service.performRequest(
            "publication", "issn", "equals", "0140-6736", 0, 1);

        assertTrue("Expected isError when API key is missing", response.isError());
        assertEquals("Open Policy Finder configuration invalid or missing", response.getMessage());
        verify(mockClient, times(0)).execute(any(HttpUriRequest.class));
    }

    @Test
    public void non200_returnsErrorAndDoesNotRetry() throws Exception {
        CloseableHttpResponse stub = stubResponse(401, "{\"message\":\"unauthorized\"}");
        when(mockClient.execute(any(HttpUriRequest.class))).thenReturn(stub);

        OpenPolicyFinderResponse response = service.performRequest(
            "publication", "issn", "equals", "0140-6736", 0, 1);

        assertTrue("Expected isError on non-200 status", response.isError());
        assertEquals("Open Policy Finder return not OK status: 401", response.getMessage());
        // Reviewer M2 fix: a non-200 should fail fast, not retry the configured maxNumberOfTries
        verify(mockClient, times(1)).execute(any(HttpUriRequest.class));
    }

    @Test
    public void networkError_returnsErrorWithExceptionMessage() throws Exception {
        when(mockClient.execute(any(HttpUriRequest.class)))
            .thenThrow(new IOException("connection refused"));

        OpenPolicyFinderResponse response = service.performRequest(
            "publication", "issn", "equals", "0140-6736", 0, 1);

        assertTrue("Expected isError on IOException", response.isError());
        assertTrue("Error message should mention connection failure: " + response.getMessage(),
            response.getMessage().contains("connection refused"));
        verify(mockClient, times(1)).execute(any(HttpUriRequest.class));
    }

    @Test
    public void emptyResponseBody_returnsError() throws Exception {
        CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        StatusLine statusLine = stubStatusLine(200, "OK");
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpResponse.getEntity()).thenReturn(null);
        when(mockClient.execute(any(HttpUriRequest.class))).thenReturn(httpResponse);

        OpenPolicyFinderResponse response = service.performRequest(
            "publication", "issn", "equals", "0140-6736", 0, 1);

        assertTrue("Expected isError when response body is null", response.isError());
        assertEquals("Open Policy Finder returned no response", response.getMessage());
    }

    @Test
    public void retryAfterContentReadFailure_returnsSuccessOnSecondAttempt() throws Exception {
        // First attempt: HTTP 200, but reading the response body throws IOException.
        // The shared executeRequest catches that IOException, leaves opfResponse null and the
        // retry loop iterates. Second attempt returns valid JSON.
        CloseableHttpResponse firstAttempt = mock(CloseableHttpResponse.class);
        StatusLine firstStatusLine = stubStatusLine(200, "OK");
        when(firstAttempt.getStatusLine()).thenReturn(firstStatusLine);
        HttpEntity firstEntity = mock(HttpEntity.class);
        when(firstEntity.getContent()).thenThrow(new IOException("simulated content read failure"));
        when(firstAttempt.getEntity()).thenReturn(firstEntity);

        CloseableHttpResponse secondAttempt = stubResponse(200, lancetSuccessJson());
        when(mockClient.execute(any(HttpUriRequest.class)))
            .thenReturn(firstAttempt)
            .thenReturn(secondAttempt);

        OpenPolicyFinderResponse response = service.performRequest(
            "publication", "issn", "equals", "0140-6736", 0, 1);

        assertFalse("Expected non-error response after retry: " + response.getMessage(),
            response.isError());
        verify(mockClient, atLeastOnce()).execute(any(HttpUriRequest.class));
        verify(mockClient, times(2)).execute(any(HttpUriRequest.class));
    }

    private CloseableHttpResponse stubResponse(int statusCode, String body) {
        return stubResponseWithStream(statusCode,
            new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
    }

    private CloseableHttpResponse stubResponseWithStream(int statusCode, InputStream body) {
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        StatusLine statusLine = stubStatusLine(statusCode, reason(statusCode));
        when(response.getStatusLine()).thenReturn(statusLine);
        HttpEntity entity = mock(HttpEntity.class);
        try {
            when(entity.getContent()).thenReturn(body);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        when(response.getEntity()).thenReturn(entity);
        return response;
    }

    private StatusLine stubStatusLine(int code, String reason) {
        StatusLine line = mock(StatusLine.class);
        when(line.getStatusCode()).thenReturn(code);
        when(line.getReasonPhrase()).thenReturn(reason);
        return line;
    }

    private String reason(int code) {
        switch (code) {
            case 200: return "OK";
            case 401: return "Unauthorized";
            case 500: return "Internal Server Error";
            default: return "";
        }
    }

    /**
     * Minimum-viable JSON for {@code OpenPolicyFinderResponse} JSON parsing to produce a
     * non-error response. The parser sets {@code error=true} only when {@code items} is empty
     * or JSON parsing throws, so we just need a single item-shaped object.
     */
    private String lancetSuccessJson() {
        return "{\"items\":[{}]}";
    }

    /**
     * Subclass that injects a fixed {@link CloseableHttpClient} for HTTP execution. Mockito's
     * default no-op {@code close()} on the mock is harmless; reusing the same mock across
     * iterations is intentional so tests can chain {@code thenReturn(...)} stubs across retries.
     */
    private static final class TestableOpenPolicyFinderService extends OpenPolicyFinderService {
        private final CloseableHttpClient httpClient;

        TestableOpenPolicyFinderService(CloseableHttpClient httpClient) {
            this.httpClient = httpClient;
        }

        @Override
        protected CloseableHttpClient newHttpClient() {
            return httpClient;
        }
    }
}
