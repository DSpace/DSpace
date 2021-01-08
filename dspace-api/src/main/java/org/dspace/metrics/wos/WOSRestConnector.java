/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.wos;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.Logger;

/**
 * This class deals with logic management to connect to the WOS external service
 * Used {@link CloseableHttpClient} can be injected. i.e. for testing purposes.
 * Please note that {@link CloseableHttpClient} instance connection is eventually closed after performing operation.
 *
 * @author mykhaylo boychuk (mykhaylo.boychuk at 4science.it)
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class WOSRestConnector {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(WOSRestConnector.class);

    private String apiKey;
    private String wosUrl;
    private CloseableHttpClient httpClient;


    public String get(String id) {
        try {
            return sendRequestToWOS(id);
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return null;
        }
    }

    private String sendRequestToWOS(String id)
            throws IOException {
        try (CloseableHttpClient httpClient = Optional.ofNullable(this.httpClient)
            .orElseGet(HttpClients::createDefault)) {
            HttpGet httpGet = new HttpGet(wosUrl.concat("DO=(").concat(URLEncoder.encode(id, StandardCharsets.UTF_8))
                .concat(")&count=10&firstRecord=1"));
            httpGet.setHeader("Accept-Encoding", "gzip, deflate, br");
            httpGet.setHeader("Connection", "keep-alive");
            httpGet.setHeader("X-ApiKey", apiKey);
            httpGet.setHeader("Accept", "application/json");

            HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                return null;
            }
            return IOUtils.toString(response.getEntity().getContent(),
                StandardCharsets.UTF_8);
        }
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getWosUrl() {
        return wosUrl;
    }

    public void setWosUrl(String wosUrl) {
        this.wosUrl = wosUrl;
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

}