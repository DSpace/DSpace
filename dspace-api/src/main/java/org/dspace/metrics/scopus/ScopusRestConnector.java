/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.scopus;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.Logger;

/**
 * This class deals with logic management to connect to the SCOPUS external service
 * Used {@link CloseableHttpClient} can be injected. i.e. for testing purposes.
 * Please note that {@link CloseableHttpClient} instance connection is eventually closed after performing operation.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class ScopusRestConnector {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ScopusRestConnector.class);

    private String apiKey;
    private String insttoken;
    private String scopusUrl;

    private CloseableHttpClient httpClient;


    public String get(String id) {
        try {
            return sendRequestToScopus(id);
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Fetches items using a url that is used for
     * linked entities
     * 
     * @param nextItemUrl
     * @return fetched response
     */
    public String getNextItem(String nextItemUrl) {
        try {
            return sendNextItemRequestToScopus(nextItemUrl);
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return null;
        }
    }

    private String sendRequestToScopus(String id)
            throws IOException {
        try (CloseableHttpClient httpClient = Optional.ofNullable(this.httpClient)
            .orElseGet(HttpClients::createDefault)) {

            HttpGet httpGet = new HttpGet(scopusUrl + URLEncoder.encode(id, Charset.defaultCharset()));
            mapCommonHeaders(httpGet);
            httpGet.setHeader("X-ELS-APIKey", apiKey);
            httpGet.setHeader("X-ELS-Insttoken", insttoken);

            return sendRequest(httpClient, httpGet);
        }
    }

    private void mapCommonHeaders(HttpGet httpGet) {
        httpGet.setHeader("Accept-Encoding", "gzip, deflate, br");
        httpGet.setHeader("Connection", "keep-alive");
        httpGet.setHeader("Accept", "application/xml");
    }

    private String sendRequest(CloseableHttpClient httpClient, HttpGet httpGet)
            throws IOException, ClientProtocolException {
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                log.error("Error connecting to server! The Server answered with: " + statusCode);
                throw new RuntimeException();
            }
            return IOUtils.toString(response.getEntity().getContent(),
                StandardCharsets.UTF_8);
        }
    }

    private String sendNextItemRequestToScopus(String nextItemUrl)
            throws IOException {
        try (CloseableHttpClient httpClient = Optional.ofNullable(this.httpClient)
                .orElseGet(HttpClients::createDefault)) {

            HttpGet httpGet = new HttpGet(nextItemUrl);
            mapCommonHeaders(httpGet);

            return sendRequest(httpClient, httpGet);
        }
    }

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * sets a custom {@link CloseableHttpClient} instance. Please make sure that
     * this instance is not closed.
     * @param httpClient
     */
    public void setHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public String getScopusUrl() {
        return scopusUrl;
    }

    public void setScopusUrl(String scopusUrl) {
        this.scopusUrl = scopusUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getInsttoken() {
        return insttoken;
    }

    public void setInsttoken(String insttoken) {
        this.insttoken = insttoken;
    }
}
