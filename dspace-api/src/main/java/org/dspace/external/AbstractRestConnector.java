/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external;

import java.io.BufferedInputStream;
import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.external.exception.ExternalDataException;
import org.dspace.external.exception.ExternalDataNotFoundException;
import org.dspace.external.exception.ExternalDataRestClientException;

/**
 * Abstract REST connector that can make API requests with Closeable HTTP client.
 * The client can be set by the user, e.g. a Test can mock a client to return a response from disk.
 * For usage examples see {@link LobidGNDRestConnectorTest},
 *                        {@link WikimediaRestConnectorTest},
 *                        {@link GeonamesRestConnectorTest}
 *
 * TODO: Apply changes from DSpace#9821 (Enable proxy for outgoing connections)
 *
 * @author Kim Shepherd
 */
public abstract class AbstractRestConnector {

    /**
     * Injectable http client for test mocking and other custom usage
     */
    private CloseableHttpClient httpClient = null;

    /**
     * REST connector/source name, useful for logging and conditional handling by other services
     */
    protected String name;

    /**
     * Base API url, set in spring configuration
     */
    protected String url;

    /**
     * Logger
     */
    private final Logger log = LogManager.getLogger();

    public AbstractRestConnector() {

    }

    /**
     * Constructor, accepting a URL
     * @param url base URL of API
     */
    public AbstractRestConnector(String url) {
        this.url = url;
    }

    /**
     * Get http client
     * @return http client
     */
    public HttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Set http client
     * @param httpClient http client to use instead of default
     */
    public void setHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Get API base URL
     * @return API base URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Set API base URL
     * @param url API base URL
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Get REST connector name
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Set REST connector name
     * @param name name of connector (e.g. wikimedia, geonames)
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get a response from a remote REST API using closeable HTTP client, and read the body entity
     * into a string for return (not a stream).
     *
     * @param requestUrl the full request URL, including parameters
     * @return parsed response string
     * @throws ExternalDataException if a non-200 code was returned or another error was encountered
     */
    public String get(String requestUrl) throws ExternalDataException {
        log.debug("Using request URL={}, connector={}", requestUrl, name);
        try (CloseableHttpClient closeableHttpClient = createHttpClient()) {
            HttpGet httpGet = new HttpGet(requestUrl);
            CloseableHttpResponse response = closeableHttpClient.execute(httpGet);
            // Check response
            if (200 == response.getStatusLine().getStatusCode()) {
                // Handle successful response
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    log.debug("Null entity for 200 OK response from {} API, status={}",
                            name, response.getStatusLine());
                    throw new ExternalDataRestClientException("External lookup responded with 200 but body was null. "
                            + "connector=" + name + "url=" + requestUrl);
                }
                return readResultEntityToString(entity);
            } else if (404 == response.getStatusLine().getStatusCode()) {
                throw new ExternalDataNotFoundException("External lookup responded with 404 Not Found. connector="
                        + name + "url=" + requestUrl);
            }
            else {
                // Handle unsuccessful response
                log.error("Got unsuccessful response from {} API: code={}, reason={}, url={}",
                        name, response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase(),
                        this.url);
            }
            // If we reached here, something went wrong
            log.error("Unexpected error handling response for url {}, connector={}", requestUrl, name);
            throw new ExternalDataRestClientException("External lookup failed. connector="
                    + name + "url=" + requestUrl);
        } catch (IOException e) {
            log.error("Unexpected error performing http request for url {}, connector={}", requestUrl, name);
            throw new ExternalDataRestClientException(e);
        }
    }

    /**
     * Given an http entity from API response, parse to a string and return so
     * the http client can be closed safely after any input streams are closed
     * @param entity the response HTTP entity
     * @return a string containing the JSON response
     * @throws IOException
     */
    private String readResultEntityToString(HttpEntity entity) throws IOException {
        String result = null;
        log.debug("Got successful (200 OK) response from {} API, content type={}, length={}",
                name, entity.getContentType(), entity.getContentLength());
        // Read the content input stream into a string, using try-with-resources to ensure stream is closed
        try (final BufferedInputStream in = new BufferedInputStream(entity.getContent())) {
            byte[] contents = new byte[1024];
            int bytesRead = 0;
            StringBuilder content = new StringBuilder();
            while ((bytesRead = in.read(contents)) != -1) {
                content.append(new String(contents, 0, bytesRead));
            }
            result = content.toString();
        }
        return result;
    }

    /**
     * Create HTTP client. If the member client is null, a new CloseableHttpClient is built, otherwise
     * this.httpClient is used. This allows tests to mock an http client, and allows for other custom client usage
     *
     * @return http client to use in actual request
     */
    private CloseableHttpClient createHttpClient() {
        if (this.httpClient != null) {
            return this.httpClient;
        } else {
            return HttpClientBuilder.create().build();
        }
    }

}
