/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.scopus;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.Logger;

/**
 * This class deals with logic management to connect to scopus external service in order to collect
 * person metrics like h-index.
 *
 * Used {@link CloseableHttpClient} can be injected. i.e. for testing purposes.
 * Please note that {@link CloseableHttpClient} instance connection is eventually closed after performing operation.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class ScopusPersonRestConnector {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ScopusPersonRestConnector.class);

    private String url;
    private String apiKey;
    private String insttoken;
    private Boolean enhanced;

    private CloseableHttpClient httpClient;


    public String get(String id) {
        try {
            return sendRequest(id);
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            return null;
        }
    }

    private String sendRequest(String id)
            throws IOException {
        StringBuilder requestUrl = new StringBuilder(url);
        requestUrl.append(id);
        if (!Objects.isNull(enhanced) && enhanced == true) {
            requestUrl.append("?view=ENHANCED");
        } else {
            System.out.println("The ENHANCED param must be valued with true");
            return null;
        }
        try (CloseableHttpClient httpClient = Optional.ofNullable(this.httpClient)
            .orElseGet(HttpClients::createDefault)) {

            HttpGet httpGet = new HttpGet(requestUrl.toString());
            httpGet.setHeader("Accept-Encoding", "gzip, deflate, br");
            httpGet.setHeader("Connection", "keep-alive");
            httpGet.setHeader("X-ELS-APIKey", apiKey);
            httpGet.setHeader("X-ELS-Insttoken", insttoken);
            httpGet.setHeader("Accept", "application/json");

            HttpResponse response = httpClient.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                return null;
            }
            return IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        }
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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

    public boolean isEnhanced() {
        return enhanced;
    }

    public void setEnhanced(Boolean enhanced) {
        this.enhanced = enhanced;
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