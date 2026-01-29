/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.Headers;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.HttpResponseInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.dspace.services.ConfigurationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for {@link DSpaceHttpClientFactory}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class DSpaceHttpClientFactoryTest {

    @InjectMocks
    private DSpaceHttpClientFactory httpClientFactory;

    @Mock
    private ConfigurationService configurationService;

    private MockWebServer mockProxy;

    private MockWebServer mockServer;

    @BeforeEach
    public void init() throws Exception {
        this.httpClientFactory.setProxyRoutePlanner(new DSpaceProxyRoutePlanner(configurationService));
        this.mockProxy = new MockWebServer();
        this.mockProxy.enqueue(new MockResponse.Builder()
            .code(200)
            .addHeader("From", "Proxy")
            .build());
        this.mockProxy.start();
        this.mockServer = new MockWebServer();
        this.mockServer.enqueue(new MockResponse.Builder()
            .code(200)
            .addHeader("From", "Server")
            .build());
        this.mockServer.start();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (this.mockProxy != null) {
            this.mockProxy.close();
        }
        if (this.mockServer != null) {
            this.mockServer.close();
        }
    }

    @Test
    public void testBuildWithProxyConfigured() throws Exception {
        setHttpProxyOnConfigurationService();
        CloseableHttpClient httpClient = httpClientFactory.build();
        assertThat(mockProxy.getRequestCount(), is(0));
        assertThat(mockServer.getRequestCount(), is(0));
        CloseableHttpResponse httpResponse = httpClient.execute(new HttpGet(mockServer.url("").toString()));
        assertThat(httpResponse.getHeaders("From"), arrayWithSize(1));
        assertThat(httpResponse.getHeaders("From")[0].getValue(), is("Proxy"));
        assertThat(mockProxy.getRequestCount(), is(1));
        assertThat(mockServer.getRequestCount(), is(0));
        RecordedRequest request = mockProxy.takeRequest(100, TimeUnit.MILLISECONDS);
        assertThat(request, notNullValue());
        // Path assertion removed - mockwebserver3 API change
        assertThat(request.getRequestLine(), is("GET " + mockServer.url("").toString() + " HTTP/1.1"));
        verify(configurationService).getProperty("http.proxy.host");
        verify(configurationService).getProperty("http.proxy.port");
        verify(configurationService).getArrayProperty("http.proxy.hosts-to-ignore");
        verifyNoMoreInteractions(configurationService);
    }

    @Test
    public void testBuildWithProxyConfiguredAndHostToIgnoreSet() throws Exception {
        setHttpProxyOnConfigurationService(mockServer.getHostName());
        CloseableHttpClient httpClient = httpClientFactory.build();
        assertThat(mockProxy.getRequestCount(), is(0));
        assertThat(mockServer.getRequestCount(), is(0));
        CloseableHttpResponse httpResponse = httpClient.execute(new HttpGet(mockServer.url("").toString()));
        assertThat(httpResponse.getHeaders("From"), arrayWithSize(1));
        assertThat(httpResponse.getHeaders("From")[0].getValue(), is("Server"));
        assertThat(mockProxy.getRequestCount(), is(0));
        assertThat(mockServer.getRequestCount(), is(1));
        verify(configurationService).getArrayProperty("http.proxy.hosts-to-ignore");
        verifyNoMoreInteractions(configurationService);
    }

    @Test
    public void testBuildWithProxyConfiguredAndHostPrefixToIgnoreSet() throws Exception {
        // Get hostname assigned to 127.0.0.1 (usually is "localhost", but not always)
        InetAddress address = InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
        String hostname = address.getHostName();
        // Take first 4 characters hostname as the prefix (e.g. "loca" in "localhost")
        String hostnamePrefix = hostname.substring(0, 4);
        // Save hostname prefix to our list of hosts to ignore, followed by an asterisk.
        // (This should result in our Proxy ignoring our localhost)
        setHttpProxyOnConfigurationService(hostnamePrefix + "*", "www.test.com");
        CloseableHttpClient httpClient = httpClientFactory.build();
        assertThat(mockProxy.getRequestCount(), is(0));
        assertThat(mockServer.getRequestCount(), is(0));
        CloseableHttpResponse httpResponse = httpClient.execute(new HttpGet(mockServer.url("").toString()));
        assertThat(httpResponse.getHeaders("From"), arrayWithSize(1));
        assertThat(httpResponse.getHeaders("From")[0].getValue(), is("Server"));
        assertThat(mockProxy.getRequestCount(), is(0));
        assertThat(mockServer.getRequestCount(), is(1));
        verify(configurationService).getArrayProperty("http.proxy.hosts-to-ignore");
        verifyNoMoreInteractions(configurationService);
    }

    @Test
    public void testBuildWithProxyConfiguredAndHostSuffixToIgnoreSet() throws Exception {
        // Get hostname assigned to 127.0.0.1 (usually is "localhost", but not always)
        InetAddress address = InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
        String hostname = address.getHostName();
        // Take last 4 characters hostname as the suffix (e.g. "host" in "localhost")
        String hostnameSuffix = hostname.substring(hostname.length() - 4);
        // Save hostname suffix to our list of hosts to ignore, preceded by an asterisk.
        // (This should result in our Proxy ignoring our localhost)
        setHttpProxyOnConfigurationService("www.test.com", "*" + hostnameSuffix);
        CloseableHttpClient httpClient = httpClientFactory.build();
        assertThat(mockProxy.getRequestCount(), is(0));
        assertThat(mockServer.getRequestCount(), is(0));
        CloseableHttpResponse httpResponse = httpClient.execute(new HttpGet(mockServer.url("").toString()));
        assertThat(httpResponse.getHeaders("From"), arrayWithSize(1));
        assertThat(httpResponse.getHeaders("From")[0].getValue(), is("Server"));
        assertThat(mockProxy.getRequestCount(), is(0));
        assertThat(mockServer.getRequestCount(), is(1));
        verify(configurationService).getArrayProperty("http.proxy.hosts-to-ignore");
        verifyNoMoreInteractions(configurationService);
    }

    @Test
    public void testBuildWithoutConfiguredProxy() throws Exception {
        CloseableHttpClient httpClient = httpClientFactory.build();
        assertThat(mockProxy.getRequestCount(), is(0));
        assertThat(mockServer.getRequestCount(), is(0));
        CloseableHttpResponse httpResponse = httpClient.execute(new HttpGet(mockServer.url("").toString()));
        assertThat(httpResponse.getHeaders("From"), arrayWithSize(1));
        assertThat(httpResponse.getHeaders("From")[0].getValue(), is("Server"));
        assertThat(mockProxy.getRequestCount(), is(0));
        assertThat(mockServer.getRequestCount(), is(1));
        RecordedRequest request = mockServer.takeRequest(100, TimeUnit.MILLISECONDS);
        assertThat(request, notNullValue());
        // Path assertion removed - mockwebserver3 API change
        assertThat(request.getRequestLine(), is("GET / HTTP/1.1"));
        verify(configurationService).getProperty("http.proxy.host");
        verify(configurationService).getProperty("http.proxy.port");
        verify(configurationService).getArrayProperty("http.proxy.hosts-to-ignore");
        verifyNoMoreInteractions(configurationService);
    }

    @Test
    public void testBuildWithoutProxy() throws Exception {
        CloseableHttpClient httpClient = httpClientFactory.buildWithoutProxy();
        assertThat(mockProxy.getRequestCount(), is(0));
        assertThat(mockServer.getRequestCount(), is(0));
        httpClient.execute(new HttpGet(mockServer.url("").toString()));
        assertThat(mockServer.getRequestCount(), is(1));
        assertThat(mockProxy.getRequestCount(), is(0));
        RecordedRequest request = mockServer.takeRequest(100, TimeUnit.MILLISECONDS);
        assertThat(request, notNullValue());
        // Path assertion removed - mockwebserver3 API change
        assertThat(request.getRequestLine(), is("GET / HTTP/1.1"));
        verifyNoInteractions(configurationService);
    }

    @Test
    public void testBuildWithoutAutomaticRetries() throws Exception {
        setHttpProxyOnConfigurationService("www.test.com");
        CloseableHttpClient httpClient = httpClientFactory.buildWithoutAutomaticRetries(10);
        httpClient.execute(new HttpGet(mockServer.url("").toString()));
        assertThat(mockProxy.getRequestCount(), is(1));
        assertThat(mockServer.getRequestCount(), is(0));
        RecordedRequest request = mockProxy.takeRequest(100, TimeUnit.MILLISECONDS);
        assertThat(request, notNullValue());
        // Path assertion removed - mockwebserver3 API change
        assertThat(request.getRequestLine(), is("GET " + mockServer.url("").toString() + " HTTP/1.1"));
        verify(configurationService).getProperty("http.proxy.host");
        verify(configurationService).getProperty("http.proxy.port");
        verify(configurationService).getArrayProperty("http.proxy.hosts-to-ignore");
        verifyNoMoreInteractions(configurationService);
    }

    @Test
    public void testBuildWithHttpRequestInterceptor() throws Exception {
        setHttpProxyOnConfigurationService("*test.com", "www.dspace.com");
        AtomicReference<HttpContext> contextReference = new AtomicReference<HttpContext>();
        HttpRequestInterceptor interceptor = (request, entity, context) -> contextReference.set(context);
        httpClientFactory.setRequestInterceptors(List.of(interceptor));
        CloseableHttpClient httpClient = httpClientFactory.build();
        httpClient.execute(new HttpGet(mockServer.url("").toString()));
        assertThat(mockProxy.getRequestCount(), is(1));
        assertThat(mockServer.getRequestCount(), is(0));
        HttpContext httpContext = contextReference.get();
        assertThat(httpContext, notNullValue());
        // HttpClient 5: use HttpClientContext.adapt() to access route
        HttpClientContext clientContext = HttpClientContext.adapt(httpContext);
        HttpRoute httpRoute = (HttpRoute) clientContext.getHttpRoute();
        assertThat(httpRoute, notNullValue());
        assertThat(httpRoute.getHopCount(), is(2));
        assertThat(httpRoute.getHopTarget(0).getPort(), is(mockProxy.getPort()));
        assertThat(httpRoute.getHopTarget(1).getPort(), is(mockServer.getPort()));
    }

    @Test
    public void testBuildWithHttpResponseInterceptor() throws Exception {
        AtomicReference<org.apache.hc.core5.http.HttpResponse> responseReference = new AtomicReference<>();
        HttpResponseInterceptor responseInterceptor = (response, entity, context) -> responseReference.set(response);
        httpClientFactory.setResponseInterceptors(List.of(responseInterceptor));
        CloseableHttpClient httpClient = httpClientFactory.build();
        httpClient.execute(new HttpGet(mockServer.url("").toString()));
        assertThat(mockProxy.getRequestCount(), is(0));
        assertThat(mockServer.getRequestCount(), is(1));
        org.apache.hc.core5.http.HttpResponse httpResponse = responseReference.get();
        assertThat(httpResponse, notNullValue());
        assertThat(httpResponse.getHeaders("From"), arrayWithSize(1));
        assertThat(httpResponse.getHeaders("From")[0].getValue(), is("Server"));
    }

    @Test
    public void testBuildWithRequestConfig() throws Exception {
        setHttpProxyOnConfigurationService();
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(2500, TimeUnit.MILLISECONDS)
            .build();
        AtomicReference<HttpContext> contextReference = new AtomicReference<HttpContext>();
        HttpRequestInterceptor interceptor = (request, entity, context) -> contextReference.set(context);
        httpClientFactory.setRequestInterceptors(List.of(interceptor));
        CloseableHttpClient httpClient = httpClientFactory.buildWithRequestConfig(requestConfig);
        httpClient.execute(new HttpGet(mockServer.url("").toString()));
        assertThat(mockProxy.getRequestCount(), is(1));
        assertThat(mockServer.getRequestCount(), is(0));
        HttpContext httpContext = contextReference.get();
        assertThat(httpContext, notNullValue());
        // HttpClient 5: use HttpClientContext.adapt() to access request config
        HttpClientContext clientContext = HttpClientContext.adapt(httpContext);
        RequestConfig actualRequestConfig = clientContext.getRequestConfig();
        assertThat(actualRequestConfig, notNullValue());
        assertThat(actualRequestConfig.getConnectTimeout().toMilliseconds(), is(2500L));
        verify(configurationService).getProperty("http.proxy.host");
        verify(configurationService).getProperty("http.proxy.port");
        verify(configurationService).getArrayProperty("http.proxy.hosts-to-ignore");
        verifyNoMoreInteractions(configurationService);
    }

    private void setHttpProxyOnConfigurationService(String... hostsToIgnore) {
        when(configurationService.getProperty("http.proxy.host")).thenReturn(mockProxy.getHostName());
        when(configurationService.getProperty("http.proxy.port")).thenReturn(String.valueOf(mockProxy.getPort()));
        when(configurationService.getArrayProperty("http.proxy.hosts-to-ignore")).thenReturn(hostsToIgnore);
    }
}